package com.maple.game.osee.manager;

import com.google.gson.Gson;
import com.google.zxing.WriterException;
import com.maple.common.login.util.WeChatUtil;
import com.maple.common.login.util.wxentity.WxAccessToken;
import com.maple.common.login.util.wxentity.WxUserInfo;
import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.entity.UserEntity;
import com.maple.database.data.mapper.UserMapper;
import com.maple.engine.container.DataContainer;
import com.maple.engine.container.UserContainer;
import com.maple.engine.data.ServerUser;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.config.Constants;
import com.maple.game.osee.config.WeChatMPConfig;
import com.maple.game.osee.dao.data.entity.AgentEntity;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.AgentMapper;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.dao.log.entity.AgentCutLogEntity;
import com.maple.game.osee.dao.log.entity.AgentCutReceiveLogEntity;
import com.maple.game.osee.dao.log.entity.CommissionExchangeEntity;
import com.maple.game.osee.dao.log.mapper.*;
import com.maple.game.osee.entity.GameEnum;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.csv.file.BatteryLevelConfig;
import com.maple.game.osee.entity.gm.CommonResponse;
import com.maple.game.osee.manager.fishing.FishingManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.OseePublicData;
import com.maple.game.osee.proto.agent.TtmyAgentMessage;
import com.maple.game.osee.util.QRCodeUtil;
import com.maple.game.osee.util.ValidateUtil;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 代理管理类
 *
 * @author Junlong
 */
@Component
public class AgentManager implements CommandLineRunner {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OseePlayerMapper playerMapper;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private AgentCommissionMapper agentCommissionMapper;

    @Autowired
    private AgentCommissionInfoMapper agentCommissionInfoMapper;

    @Autowired
    private AgentCutLogMapper agentCutLogMapper;

    @Autowired
    private CommissionExchangeMapper commissionExchangeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AgentCutReceiveLogMapper agentCutReceiveLogMapper;

    @Autowired
    private AgentWithdrawLogMapper agentWithdrawLogMapper;

    @Autowired
    private WeChatMPConfig weChatMPConfig;

    public static final String activeRateKey = "Agent:ActiveRate";

    /**
     * 活跃推广奖励比例
     */
    public static Double activeRate = 0.05;

    @Override
    public void run(String... args) {
        agentMapper.createTable();
        agentCommissionMapper.createTable();
        agentCommissionInfoMapper.createTable();
        commissionExchangeMapper.createTable();
        agentCutLogMapper.createTable();
        agentCutReceiveLogMapper.createTable();
        agentWithdrawLogMapper.createTable();

        String activeRateStr = RedisHelper.get("Agent:ActiveRate");
        if (!StringUtils.isEmpty(activeRateStr)) {
            activeRate = Double.parseDouble(activeRateStr);
        }
    }

    /**
     * 通过玩家ID查询玩家代理信息
     */
    public AgentEntity getAgentInfoByPlayerId(long playerId) {
        ServerUser user = UserContainer.getUserById(playerId);
        if (user == null) {
            return null;
        }
        AgentEntity agent = agentMapper.getByPlayerId(playerId);
        if (agent == null) {
            agent = new AgentEntity();
            agent.setPlayerId(playerId);
            agent.setPlayerName(user.getNickname());
            agent.setAgentLevel(3);
            agent.setState(0);
            agentMapper.save(agent);
        }

        return agent;
    }

    /**
     * 升级为一级代理
     */
    private boolean agentEnable(ServerUser user, double first, double second, String nickname) {
        try {
            AgentEntity agentEntity = agentMapper.getByPlayerId(user.getId());
            if (agentEntity == null || agentEntity.getAgentLevel() != 1) {
                if (agentEntity != null) { // 在代理里面有记录
                    loopChangeAgent(agentEntity, null);
                    agentMapper.removeByUpperPlayer(agentEntity.getPlayerId());
                    agentCutReceiveLogMapper.deleteByAgentId(agentEntity.getPlayerId());
                    agentCutLogMapper.deleteByAgentId(agentEntity.getPlayerId());
                    agentMapper.delete(agentEntity);
                }
                agentEntity = new AgentEntity();
                agentEntity.setPlayerId(user.getId());
                agentEntity.setPlayerName(user.getNickname());
                agentEntity.setAgentLevel(1); // 一级代理级别
                agentEntity.setFirstCommissionRate(first);
                agentEntity.setSecondCommissionRate(second);
                agentEntity.setTotalCommission(0D);
                agentEntity.setState(0);
                if (!StringUtils.isEmpty(nickname)) {
                    agentEntity.setPlayerName(nickname);
                }
                if (agentMapper.save(agentEntity) > 0) {
                    // 邀请链接及图片生成
                    generateInviteInfo(agentEntity);
                    return true;
                }
            } else {
                agentEntity.setFirstCommissionRate(first);
                agentEntity.setSecondCommissionRate(second);
                if (!StringUtils.isEmpty(nickname)) {
                    agentEntity.setPlayerName(nickname);
                }
                return agentMapper.update(agentEntity) > 0;
            }
            return false;
        } catch (Exception e) {
            logger.error("设置一级代理出错：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 微信用户绑定代理
     */
    public CommonResponse agentBindCallback(String code, String state) {
        try {
            long agentPlayerId = Long.parseLong(state.substring(1));
            AgentEntity agentEntity = agentMapper.getByPlayerId(agentPlayerId);
            if (agentEntity == null) {
                return new CommonResponse("ERROR_NOT_AGENT", "该玩家暂时无法绑定！");
            }
            if (agentEntity.getState() == 1) { // 代理被禁用
                return new CommonResponse("ERROR_AGENT_DISABLED", "绑定的玩家已被冻结！绑定无效！");
            }

            // 开始获取微信用户信息
            WxAccessToken accessToken = WeChatUtil.getAccessToken(code, weChatMPConfig.getAppid(), weChatMPConfig.getSecret());
            if (accessToken.getErrcode() != null) { // 错误码不为空就代表返回错误信息
                logger.error("绑定代理出错：微信code换取token失败：code【{}】,errMsg【{}】", code, accessToken.getErrmsg());
                return new CommonResponse("ERROR_CODE_GET_TOKEN", "获取微信用户信息出错！请重试！");
            }
            WxUserInfo userInfo = WeChatUtil.getUserInfo(accessToken);
            if (userInfo.getErrcode() != null) { // 错误码不为空就代表返回错误信息
                return new CommonResponse("ERROR_GET_WX_USERINFO", "获取微信用户信息出错！请重试！");
            }
            UserEntity userEntity;
            if (userInfo.getUnionid() != null) { // 通过唯一id查询用户数据
                userEntity = userMapper.findByUnionid(userInfo.getUnionid());
            } else { // 通过openid查询用户数据
                userEntity = userMapper.findByOpenid(userInfo.getOpenid());
            }
            if (userEntity == null) { // 如果为空就要创建新的用户数据
                userEntity = new UserEntity();
                userEntity.setOpenid(userInfo.getOpenid());
                userEntity.setUnionid(userInfo.getUnionid());
                userEntity.setSex(userInfo.getSex() - 1);
                userEntity.setHeadIndex(0);
                userEntity.setHeadUrl(userInfo.getHeadimgurl());
                userEntity.setNickname(userInfo.getNickname());
                // 赋予初始用户名
                userEntity.setUsername(userInfo.getOpenid());
                // 保存新用户数据到数据库
                userMapper.save(userEntity);

                // 初始化游戏数据
                OseePlayerEntity playerEntity = new OseePlayerEntity();
                playerEntity.setUserId(userEntity.getId());
                playerEntity.setMoney(1000);
                // 初始炮台等级为最低等级
                playerEntity.setBatteryLevel(DataContainer.getData(1, BatteryLevelConfig.class).getBatteryLevel());
                playerMapper.save(playerEntity);
            }
            CommonResponse commonResponse = bindPlayer(userEntity, agentEntity);
            if (commonResponse == null) {
                // 返回下载页面地址
                commonResponse = new CommonResponse(true);
                commonResponse.setData(Constants.GAME_DOWNLOAD_URL);
                return commonResponse;
            }
            return commonResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResponse("ERROR_EXCEPTION", "操作异常！");
        }
    }

    /**
     * 玩家绑定
     */
    public CommonResponse bindPlayer(UserEntity userEntity, AgentEntity agentEntity) {
        long playerId = userEntity.getId();
        // 检查玩家是否已经有绑定或者是代理
        AgentEntity playerAgent = agentMapper.getByPlayerId(playerId);
        // 无法修改代理的情况：自己为代理、自己不为1级玩家、所属代理并没有被删除
        if (playerAgent != null && (playerAgent.getAgentLevel() != 3 || playerAgent.getUpperPlayerId() != null
                || playerAgent.getAgentPlayerId() != null)) { // 已经是代理或者已经绑定代理
            logger.error("绑定代理无效：{}已经是代理或已绑定，代理等级为{}", playerId, playerAgent.getAgentLevel());
            // 返回下载页面地址
            CommonResponse commonResponse = new CommonResponse(true);
            commonResponse.setData(Constants.GAME_DOWNLOAD_URL);
            return commonResponse;
        } else {
            if (playerAgent != null) {
                agentMapper.delete(playerAgent);
                if (agentEntity.getAgentLevel() == 1) { // 如果玩家直接绑定一级代理，需要先解绑现有关系
                    loopChangeAgent(playerAgent, null);
                    agentMapper.removeByUpperPlayer(playerAgent.getPlayerId());
                    agentCutReceiveLogMapper.deleteByAgentId(playerAgent.getPlayerId());
                    agentCutLogMapper.deleteByAgentId(playerAgent.getPlayerId());
                }
            }
            // 保存用户代理关系数据
            AgentEntity entity = new AgentEntity();
            entity.setPlayerId(playerId);
            entity.setPlayerName(userEntity.getNickname());
            if (agentEntity.getAgentLevel() == 1) { // 绑定一级代理
                entity.setAgentLevel(2); // 如果绑定的代理是一级代理就设置该玩家为二级代理
                entity.setAgentPlayerId(agentEntity.getPlayerId());
            } else if (agentEntity.getAgentLevel() == 2) { // 绑定二级代理
                entity.setAgentLevel(3); // 设置为会员级别
                entity.setAgentPlayerId(agentEntity.getPlayerId());
            } else { // 绑定其他玩家
                entity.setAgentLevel(3);
                entity.setAgentPlayerId(agentEntity.getAgentPlayerId());
                entity.setUpperPlayerId(agentEntity.getPlayerId());
            }
            entity.setTotalCommission(0D);
            entity.setState(0);
            if (agentMapper.save(entity) > 0) { // 保存数据
                generateInviteInfo(entity);
                logger.info("绑定代理成功：{}绑定了代理{}", playerId, agentEntity.getPlayerId());
            }
        }
        return null;
    }

    /**
     * 循环修改代理id
     */
    private void loopChangeAgent(AgentEntity playerAgent, Long agentId) {
        if (playerAgent != null) {
            playerAgent.setAgentPlayerId(agentId);
            agentMapper.update(playerAgent);
            List<AgentEntity> myPlayers = agentMapper.getByUpperPlayerId(playerAgent.getPlayerId());
            myPlayers.forEach(myPlayer -> loopChangeAgent(myPlayer, agentId));
        }
    }

    /**
     * 新用户绑定代理玩家
     */
    public String agentBind(long playerId) throws UnsupportedEncodingException {
        return "https://open.weixin.qq.com/connect/oauth2/authorize?" +
                "appid=" + weChatMPConfig.getAppid() +
                "&redirect_uri=" + URLEncoder.encode(Constants.GM_BASE_URL + "/api/agent/bind/callback", "utf-8") +
                "&response_type=code" +
                "&scope=snsapi_userinfo" +
                "&state=_" + playerId +
                "#wechat_redirect";
    }

    /**
     * 生成玩家专属的邀请链接
     */
    private String generateInviteUrl(long playerId) {
        return Constants.GM_BASE_URL + "/api/agent/bind/" + playerId;
    }

    /**
     * 后台新线程创建玩家的邀请信息(邀请链接，二维码等，因为是耗时的操作)
     */
    private void generateInviteInfo(AgentEntity agentEntity) {
        if (agentEntity == null) {
            return;
        }
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> {
            try {
                // 玩家自己专属的邀请链接及图片生成
                String inviteUrl = generateInviteUrl(agentEntity.getPlayerId());
                agentEntity.setInviteUrl(inviteUrl);
                String qrCodeBase64 = QRCodeUtil.createQRCodeBase64(inviteUrl);
                agentEntity.setInviteQrCodeImg(qrCodeBase64);
                // 更新信息
                agentMapper.update(agentEntity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, TimeUnit.SECONDS);
    }

    //**************************** 游戏逻辑相关 ******************************

    /**
     * 检查玩家是否为代理
     */
    public void agentCheck(long playerId, ServerUser user) {
        AgentEntity agentEntity = agentMapper.getByPlayerId(playerId);
        TtmyAgentMessage.AgentCheckResponse.Builder builder = TtmyAgentMessage.AgentCheckResponse.newBuilder();
        if (agentEntity == null) { // 不是代理
            builder.setAgentType(3);
        } else if (agentEntity.getState() == 1) { // 是代理但被禁用了
            builder.setAgentType(4);
        } else {
            builder.setAgentType(agentEntity.getAgentLevel());
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_AGENT_CHECK_RESPONSE_VALUE, builder, user);
    }

    /**
     * 获取代理的分享信息
     */
    public void agentShareInfo(long playerId, ServerUser user) {
        AgentEntity agentEntity = agentMapper.getByPlayerId(playerId);
        if (agentEntity == null) {
            agentEntity = new AgentEntity();
            agentEntity.setPlayerId(user.getId());
            agentEntity.setPlayerName(user.getNickname());
            agentEntity.setAgentLevel(3); // 一级代理级别
            agentEntity.setState(0);
            if (agentMapper.save(agentEntity) > 0) {
                // 邀请链接及图片生成
                generateInviteInfo(agentEntity);
            }
        }
        if (agentEntity.getState() == 1) {
            NetManager.sendErrorMessageToClient("你的代理权限已被禁用！", user);
            return;
        }
        // 检查玩家是否已经绑定账号
        String username = user.getUsername();
        if (ValidateUtil.isEmpty(username)) {
            NetManager.sendErrorMessageToClient("你还未绑定账号！", user);
            return;
        }
        String inviteUrl = agentEntity.getInviteUrl();
        String inviteQrCodeImg = agentEntity.getInviteQrCodeImg();
        if (StringUtils.isEmpty(inviteUrl) || StringUtils.isEmpty(inviteQrCodeImg) ||
                inviteUrl.startsWith("http://suo.im/") // 使用该短网址的地址变回来，该网址有被劫持的风险
        ) {
            try {
                // 玩家自己专属的邀请链接及图片生成
                inviteUrl = generateInviteUrl(agentEntity.getPlayerId());
                agentEntity.setInviteUrl(inviteUrl);
                inviteQrCodeImg = QRCodeUtil.createQRCodeBase64(inviteUrl);
                agentEntity.setInviteQrCodeImg(inviteQrCodeImg);
                // 更新信息
                agentMapper.update(agentEntity);
            } catch (WriterException | IOException e) {
                e.printStackTrace();
            }
        }
        TtmyAgentMessage.AgentShareInfoResponse.Builder builder = TtmyAgentMessage.AgentShareInfoResponse.newBuilder();
        builder.setQrcode(inviteQrCodeImg);
        builder.setUrl(inviteUrl);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_AGENT_SHARE_INFO_RESPONSE_VALUE, builder, user);
    }

    /**
     * 一级代理下面的团队信息
     */
    public void agentTeamInfo(TtmyAgentMessage.AgentTeamInfoRequest request, ServerUser user) {
        long playerId = request.getPlayerId();
        int pageNo = request.getPageNo();
        int pageSize = request.getPageSize();

        TtmyAgentMessage.AgentTeamInfoResponse.Builder builder = TtmyAgentMessage.AgentTeamInfoResponse.newBuilder();

        int count = agentMapper.getAgentNextLevelCount(playerId);
        // 二级代理团队总数，即二级代理人数
        builder.setMemberTotalCount(count);
        builder.setPageNo(pageNo);

        // 查询该代理下面的所有二级代理
        List<AgentEntity> agentEntityList = agentMapper.getByAgentPlayerIdWithPage(playerId,
                (pageNo - 1) * pageSize, pageSize);
        for (AgentEntity agentEntity : agentEntityList) {
            TtmyAgentMessage.AgentTeamInfoProto.Builder teamInfo = TtmyAgentMessage.AgentTeamInfoProto.newBuilder();
            teamInfo.setSecAgentId(agentEntity.getPlayerId());
            teamInfo.setNickname(agentEntity.getPlayerName());
            int nextLevelCount = agentMapper.getAgentNextLevelCount(agentEntity.getPlayerId());
            teamInfo.setMemberCount(nextLevelCount);
            // 二级代理最近一周的贡献
            double commission = agentCommissionMapper.getWeeklyCommissionByPlayerId(agentEntity.getPlayerId(), playerId);
            // 二级代理所属下级会员最近一周的贡献佣金
            double commission_1 = agentCommissionMapper.getWeeklyCommissionByAgentId(agentEntity.getPlayerId());
            // 团队总贡献 = 二级代理贡献 + 玩家总贡献
            teamInfo.setContribution(commission + commission_1);

            builder.addTeamInfo(teamInfo);
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_AGENT_TEAM_INFO_RESPONSE_VALUE, builder, user);
    }

    /**
     * 二级代理下的会员详情
     */
    public void agentMemberInfo(TtmyAgentMessage.AgentMemberInfoRequest request, ServerUser user) {
        long playerId = request.getPlayerId();
        int pageNo = request.getPageNo();
        int pageSize = request.getPageSize();

        TtmyAgentMessage.AgentMemberInfoResponse.Builder builder = TtmyAgentMessage.AgentMemberInfoResponse.newBuilder();
        // 二级代理下的会员数量
        builder.setMemberTotalCount(agentMapper.getAgentNextLevelCount(playerId));
        builder.setPageNo(pageNo);

        // 查询该二级代理下的会员数量
        List<AgentEntity> agentEntityList = agentMapper.getByAgentPlayerIdWithPage(playerId,
                (pageNo - 1) * pageSize, pageSize);
        for (AgentEntity entity : agentEntityList) {
            TtmyAgentMessage.AgentMemberInfoProto.Builder memberInfo = TtmyAgentMessage.AgentMemberInfoProto.newBuilder();
            memberInfo.setMemberId(entity.getPlayerId());
            memberInfo.setNickname(entity.getPlayerName());
            // 二级代理下会员最近一周内的贡献
            double commission = agentCommissionMapper.getWeeklyCommissionByPlayerId(entity.getPlayerId(), playerId);
            memberInfo.setContribution(commission);

            builder.addMemberInfo(memberInfo);
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_AGENT_MEMBER_INFO_RESPONSE_VALUE, builder, user);
    }

    /**
     * 代理佣金
     */
    public void agentCommission(TtmyAgentMessage.AgentCommissionRequest request, ServerUser user) {
        long playerId = request.getPlayerId();
        int pageNo = request.getPageNo();
        int pageSize = request.getPageSize();

        TtmyAgentMessage.AgentCommissionResponse.Builder builder = TtmyAgentMessage.AgentCommissionResponse.newBuilder();
        builder.setPageNo(pageNo);
        // 查询代理玩家的代理信息
        AgentEntity agentEntity = agentMapper.getByPlayerId(playerId);
        // 本周总业绩
        double weeklyTotalCommission = agentCommissionMapper.getWeeklyCommissionByAgentId(agentEntity.getPlayerId());
        // 代理玩家收到了佣金贡献的总天数
        long days = agentCommissionMapper.getDaysByAgentId(agentEntity.getPlayerId());

        // 指定代理每日赚取的贡献佣金
        List<Map<String, Object>> list = agentCommissionMapper.getDailyCommissionListByAgentId(
                agentEntity.getPlayerId(), (pageNo - 1) * pageSize, pageSize);
        for (Map<String, Object> map : list) {
            TtmyAgentMessage.AgentCommissionProto.Builder commissionProto = TtmyAgentMessage.AgentCommissionProto.newBuilder();
            commissionProto.setDate((String) map.get("date"));
            commissionProto.setTotalCommission(Long.parseLong(String.valueOf(map.get("commission"))));
            builder.addCommission(commissionProto);
        }
        builder.setTotalCommission(weeklyTotalCommission);
        builder.setTotalNum(days);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_AGENT_COMMISSION_RESPONSE_VALUE, builder, user);
    }

    /**
     * 代理佣金兑换
     */
    public void agentCommissionExchange(TtmyAgentMessage.AgentCommissionExchangeRequest request, ServerUser user) {
        long playerId = request.getPlayerId();
        int pageNo = request.getPageNo();
        int pageSize = request.getPageSize();

        TtmyAgentMessage.AgentCommissionExchangeResponse.Builder builder = TtmyAgentMessage.AgentCommissionExchangeResponse.newBuilder();
        builder.setPageNo(pageNo);
        builder.setPlayerId(playerId);

        AgentEntity agentEntity = agentMapper.getByPlayerId(playerId);
        // 玩家总可用佣金
        builder.setCommission(agentEntity.getTotalCommission());
        // 代理玩家佣金兑换记录查询,可分页
        StringBuilder conBuilder = new StringBuilder("where 1=1");
        conBuilder.append(" and agent_id = ").append(playerId);

        List<CommissionExchangeEntity> exchangeEntityList = commissionExchangeMapper.getList(conBuilder.toString(),
                " limit " + (pageNo - 1) * pageSize + "," + pageSize);
        int count = commissionExchangeMapper.getCount(conBuilder.toString());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (CommissionExchangeEntity exchangeEntity : exchangeEntityList) {
            TtmyAgentMessage.AgentExchangeLogProto.Builder exchangeLog = TtmyAgentMessage.AgentExchangeLogProto.newBuilder();
            exchangeLog.setDate(simpleDateFormat.format(exchangeEntity.getCreateTime()));
            exchangeLog.setBronzeTorpedoNum(exchangeEntity.getBronzeTorpedoNum());
            exchangeLog.setSilverTorpedoNum(exchangeEntity.getSilverTorpedoNum());
            exchangeLog.setGoldTorpedoNum(exchangeEntity.getGoldTorpedoNum());
            exchangeLog.setGoldNum(exchangeEntity.getGoldNum());
            exchangeLog.setCostCommission(exchangeEntity.getCostCommission());
            builder.addExchangeLog(exchangeLog);
        }

        builder.setTotalNum(count);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_AGENT_COMMISSION_EXCHANGE_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送代理信息消息
     */
    public void sendAgentInfoResponse(int year, ServerUser user) {
        AgentEntity agent = getAgentInfoByPlayerId(user.getId());
        if (agent == null || agent.getAgentLevel() == 3) { // 普通用户
            return;
        }

        TtmyAgentMessage.AgentInfoResponse.Builder builder = TtmyAgentMessage.AgentInfoResponse.newBuilder();
        builder.setOnlineCount(agentMapper.getOnlineSize(user.getId()));
        builder.setDailyCount(agentMapper.getDailyPlayerSize(user.getId()));
        builder.setMonthCount(agentMapper.getMonthPlayerSize(user.getId()));
        builder.setDailyRecharge(agentCommissionMapper.getDailyCommissionByAgentId(user.getId()));
        builder.setMonthRecharge(agentCommissionMapper.getMonthCommissionByAgentId(user.getId()));
        builder.setTotalCount(agentMapper.getAgentNextLevelCount(user.getId()));

        builder.setYear(year);
        List<Map<Object, Object>> monthlyCounts = agentMapper.getPerMonthPlayerSize(user.getId(), year);
        List<Map<Object, Object>> monthlyRecharges = agentCommissionMapper.getPerMonthCommissionByAgentId(user.getId(), year);
        Map<Integer, TtmyAgentMessage.AgentInfoResponse.MonthDataProto.Builder> monthDataMap = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            TtmyAgentMessage.AgentInfoResponse.MonthDataProto.Builder monthData = TtmyAgentMessage.AgentInfoResponse.MonthDataProto.newBuilder();
            monthData.setMonth(i);
            monthDataMap.put(i, monthData);
        }
        monthlyCounts.forEach(monthlyCount -> monthDataMap.get(monthlyCount.get("month")).setCount((int) (long) monthlyCount.get("count")));
        monthlyRecharges.forEach(monthlyRecharge -> monthDataMap.get(monthlyRecharge.get("month")).setRecharge((long) ((BigDecimal) monthlyRecharge.get("money")).longValue()));

        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_AGENT_INFO_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送玩家全民代理信息
     */
    public void sendPlayerAgentInfoResponse(ServerUser user) {
        AgentEntity agent = getAgentInfoByPlayerId(user.getId());
        if (agent == null) {
            return;
        }

        TtmyAgentMessage.PlayerAgentInfoResponse.Builder builder = TtmyAgentMessage.PlayerAgentInfoResponse.newBuilder();
        builder.setDailyCount(agentMapper.getDailyPlayerSizeByUpper(user.getId()));
        builder.setTotalCount(agentMapper.getAgentNextUpperCount(user.getId()));

        Map<String, BigDecimal> daily = agentCutLogMapper.getDailyActiveByAgentId(user.getId());
        builder.setDailyMoney(daily.get("money").longValue());
        builder.setDailyDragonCrystal(daily.get("dragon").longValue());

        Map<String, BigDecimal> total = agentCutLogMapper.getTotalActiveByAgentId(user.getId());
        builder.setTotalMoney(total.get("money").longValue());
        builder.setTotalDragonCrystal(total.get("dragon").longValue());

        builder.setCurrentMoney(agent.getTotalActiveMoney());
        builder.setCurrentDragonCrystal(agent.getTotalActiveDragonCrystal());
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_PLAYER_AGENT_INFO_RESPONSE_VALUE, builder, user);
    }

    /**
     * 领取全民推广奖励
     */
    public void receiveActive(ServerUser user) {
        AgentEntity agent = getAgentInfoByPlayerId(user.getId());
        if (agent == null) {
            return;
        }

        if (agent.getTotalActiveMoney() == 0 && agent.getTotalActiveDragonCrystal() == 0) {
            return;
        }

        // 保存日志
        AgentCutReceiveLogEntity log = new AgentCutReceiveLogEntity();
        log.setAgentId(user.getId());
        log.setAgentName(user.getNickname());
        log.setMoney(agent.getTotalActiveMoney());
        log.setDragonCrystal(agent.getTotalActiveDragonCrystal());
        agentCutReceiveLogMapper.save(log);

        List<ItemData> itemDataList = new ArrayList<>();
        itemDataList.add(new ItemData(ItemId.MONEY.getId(), agent.getTotalActiveMoney()));
        itemDataList.add(new ItemData(ItemId.DRAGON_CRYSTAL.getId(), agent.getTotalActiveDragonCrystal()));

        // 发送领取成功消息
        TtmyAgentMessage.PlayerAgentReceiveResponse.Builder builder = TtmyAgentMessage.PlayerAgentReceiveResponse.newBuilder();
        builder.setMoney(agent.getTotalActiveMoney());
        builder.setDragonCrystal(agent.getTotalActiveDragonCrystal());
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_PLAYER_AGENT_RECEIVE_RESPONSE_VALUE, builder, user);

        // 更新数据
        agent.setTotalActiveMoney(0L);
        agent.setTotalActiveDragonCrystal(0L);
        agentMapper.update(agent);

        // 添加道具
        PlayerManager.addItems(user, itemDataList, ItemChangeReason.ACTIVE_AGENT, true);
    }

    /**
     * 代理佣金兑换
     */
    public void agentActiveExchangeLog(TtmyAgentMessage.PlayerAgentActiveExchangeRequest request, ServerUser user) {
        int pageNo = request.getPageNo();
        int pageSize = request.getPageSize();

        TtmyAgentMessage.PlayerAgentActiveExchangeResponse.Builder builder = TtmyAgentMessage.PlayerAgentActiveExchangeResponse.newBuilder();
        builder.setPageNo(pageNo);

        // 代理玩家佣金兑换记录查询,可分页
        StringBuilder conBuilder = new StringBuilder();
        conBuilder.append("where agent_id = ").append(user.getId());

        List<AgentCutReceiveLogEntity> exchangeEntityList = agentCutReceiveLogMapper.getList(conBuilder.toString(),
                " limit " + (pageNo - 1) * pageSize + "," + pageSize);
        int count = agentCutReceiveLogMapper.getCount(conBuilder.toString());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (AgentCutReceiveLogEntity exchangeEntity : exchangeEntityList) {
            TtmyAgentMessage.PlayerAgentActiveExchangeResponse.PlayerAgentActiveExchangeProto.Builder logBuilder =
                    TtmyAgentMessage.PlayerAgentActiveExchangeResponse.PlayerAgentActiveExchangeProto.newBuilder();
            logBuilder.setReceiveTime(simpleDateFormat.format(exchangeEntity.getCreateTime()));
            logBuilder.setMoney(exchangeEntity.getMoney());
            logBuilder.setDragonCrystal(exchangeEntity.getDragonCrystal());
            builder.addReceiveLogs(logBuilder);
        }

        builder.setTotalNum(count);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_PLAYER_AGENT_ACTIVE_EXCHANGE_RESPONSE_VALUE, builder, user);
    }

    /**
     * 添加代理活跃奖励
     */
    public void addActiveMoney(long playerId, GameEnum game, long money, long dragonCrystal) {
        if (money > 0 || dragonCrystal > 0) {
            AgentEntity playerAgent = getAgentInfoByPlayerId(playerId);

            if (playerAgent != null && playerAgent.getAgentLevel() != 1) {
                Long agentId = playerAgent.getUpperPlayerId() == null ? playerAgent.getAgentPlayerId() : playerAgent.getUpperPlayerId();
                if (agentId != null) {
                    AgentEntity agent = getAgentInfoByPlayerId(agentId);
                    long cutMoney = (long) (money * activeRate / 100D);
                    long cutDragonCrystal = (long) (dragonCrystal * activeRate / 100D);
                    addActiveMoney(agent, playerId, game, cutMoney, cutDragonCrystal); // 直属代理
                }
            }
        }
    }

    /**
     * 添加活跃奖励
     */
    private void addActiveMoney(AgentEntity agent, long playerId, GameEnum game, long money, long dragonCrystal) {
        if ((money > 0 || dragonCrystal > 0) && agent != null) {
            AgentCutLogEntity log = new AgentCutLogEntity();
            log.setPlayerId(playerId);
            log.setAgentId(agent.getPlayerId());
            log.setGame(game.getId());
            log.setCutMoney(money);
            log.setCutDragonCrystal(dragonCrystal);
            agentCutLogMapper.save(log);

            agent.setTotalActiveMoney(agent.getTotalActiveMoney() + money);
            agent.setTotalActiveDragonCrystal(agent.getTotalActiveDragonCrystal() + dragonCrystal);
            agentMapper.update(agent);
        }
    }

    /**
     * 设置玩家为代理
     */
    public boolean setAgent(ServerUser user, double first, double second, String nickname) {
        if (!agentEnable(user, first, second, nickname)) {
            return false;
        }
        // 通知在线用户被升级为代理
        if (user.isOnline()) {
            TtmyAgentMessage.AgentCheckResponse.Builder builder = TtmyAgentMessage.AgentCheckResponse.newBuilder();
            builder.setAgentType(1);
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_AGENT_CHECK_RESPONSE_VALUE, builder, user);
        }
        return true;
    }

    /**
     * 删除一级代理
     */
    public boolean deleteFirAgent(AgentEntity agentEntity) {
        if (agentEntity.getAgentLevel() != 1) {
            return false;
        }

        List<AgentEntity> agents = agentMapper.getByAgentPlayerId(agentEntity.getPlayerId());
        for (AgentEntity agent : agents) {
            deleteSecAgent(agent);
            deleteAgentData(agent);
        }
        setPlayerNoAgent(agentEntity);
        return true;
    }

    /**
     * 删除二级代理
     */
    public boolean deleteSecAgent(AgentEntity agentEntity) {
        if (agentEntity.getAgentLevel() != 2) {
            return false;
        }

        List<AgentEntity> agents = agentMapper.getByAgentPlayerId(agentEntity.getPlayerId());
        for (AgentEntity agent : agents) {
            deleteAgentData(agent);
        }
        setPlayerNoAgent(agentEntity);
        return true;
    }

    /**
     * 删除代理数据
     */
    private void deleteAgentData(AgentEntity agent) {
        agent.setAgentPlayerId(null);
        agent.setAgentLevel(3);
        agent.setFirstCommissionRate(0D);
        agent.setSecondCommissionRate(0D);
        agent.setTotalCommission(0D);
        agent.setTotalActiveMoney(0L);
        agent.setTotalActiveDragonCrystal(0L);
        agentMapper.update(agent);
    }

    /**
     * 移除玩家代理权限
     */
    private void setPlayerNoAgent(AgentEntity agentEntity) {
        agentMapper.delete(agentEntity);
        agentCommissionMapper.deleteByAgentId(agentEntity.getPlayerId());
        agentCutLogMapper.deleteByAgentId(agentEntity.getPlayerId());
        agentCutReceiveLogMapper.deleteByAgentId(agentEntity.getPlayerId());
        agentWithdrawLogMapper.deleteByAgentId(agentEntity.getPlayerId());

        ServerUser user = UserContainer.getUserById(agentEntity.getPlayerId());
        PlayerManager.getPlayerEntity(user).setPlayerType(0);
        playerMapper.update(PlayerManager.getPlayerEntity(user));
    }
}
