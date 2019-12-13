package com.maple.game.osee.controller.gm;

import com.alibaba.fastjson.JSON;
import com.maple.common.lobby.manager.LobbyManager;
import com.maple.common.lobby.manager.WanderSubtitleManager;
import com.maple.common.login.proto.LoginMessage.LoginMsgCode;
import com.maple.common.login.proto.LoginMessage.LogoutResponse;
import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.entity.WanderSubtitleEntity;
import com.maple.database.data.mapper.UserAuthenticationMapper;
import com.maple.database.data.mapper.UserMapper;
import com.maple.database.data.mapper.WanderSubtitleMapper;
import com.maple.engine.anotation.GmController;
import com.maple.engine.anotation.GmHandler;
import com.maple.engine.container.UserContainer;
import com.maple.engine.data.ServerUser;
import com.maple.engine.manager.GsonManager;
import com.maple.engine.utils.JsonMapUtils;
import com.maple.engine.utils.JsonMapUtils.JsonInnerType;
import com.maple.game.osee.controller.gm.base.GmBaseController;
import com.maple.game.osee.dao.data.entity.*;
import com.maple.game.osee.dao.data.entity.gm.GmAuthenticationInfo;
import com.maple.game.osee.dao.data.entity.gm.GmCdkInfo;
import com.maple.game.osee.dao.data.entity.gm.GmCdkTypeInfo;
import com.maple.game.osee.dao.data.mapper.*;
import com.maple.game.osee.dao.data.mapper.gm.GmCommonMapper;
import com.maple.game.osee.dao.log.entity.*;
import com.maple.game.osee.dao.log.mapper.*;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.gm.CommonResponse;
import com.maple.game.osee.manager.AgentManager;
import com.maple.game.osee.manager.MessageManager;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.manager.fishing.FishingHitDataManager;
import com.maple.game.osee.manager.fruitlaba.FruitLaBaManager;
import com.maple.game.osee.manager.lobby.CdkManager;
import com.maple.game.osee.manager.lobby.CommonLobbyManager;
import com.maple.game.osee.manager.lobby.LotteryDrawManager;
import com.maple.game.osee.manager.lobby.ShoppingManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.OseePublicData;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 后台基础控制器
 */
@GmController
public class GmCommonController extends GmBaseController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OseePlayerMapper playerMapper;

    @Autowired
    private GmCommonMapper gmCommonMapper;

    @Autowired
    private UserAuthenticationMapper authenticationMapper;

    @Autowired
    private OseeNoticeMapper noticeMapper;

    @Autowired
    private WanderSubtitleMapper subtitleMapper;

    @Autowired
    private OseeLotteryShopMapper lotteryShopMapper;

    @Autowired
    private OseeRealLotteryLogMapper realLotteryMapper;

    @Autowired
    private OseeUnrealLotteryLogMapper unrealLotteryMapper;

    @Autowired
    private OseeRechargeLogMapper rechargeLogMapper;

    @Autowired
    private OseePlayerTenureLogMapper tenureLogMapper;

    @Autowired
    private OseeCutMoneyLogMapper cutMoneyLogMapper;

    @Autowired
    private OseeExpendLogMapper expendLogMapper;

    @Autowired
    private OseeGobangRecordLogMapper gobangRecordMapper;

    @Autowired
    private OseeFruitRecordLogMapper fruitRecordMapper;

    @Autowired
    private OseeFighttenRecordLogMapper fighttenRecordMapper;

    @Autowired
    private OseeFishingRecordLogMapper fishingRecordMapper;

    @Autowired
    private LobbyManager lobbyManager;

    @Autowired
    private CommonLobbyManager commonLobbyManager;

    @Autowired
    private CdkManager cdkManager;

    @Autowired
    private WanderSubtitleManager subtitleManager;

    @Autowired
    private ShoppingManager shoppingManager;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private AgentCommissionMapper agentCommissionMapper;

    @Autowired
    private CommissionExchangeMapper commissionExchangeMapper;

    @Autowired
    private GiveGiftLogMapper giftLogMapper;

    @Autowired
    private MessageManager messageManager;

    @Autowired
    private TwoEightRecordMapper twoEightRecordMapper;

    @Autowired
    private CrystalExchangeLogMapper crystalExchangeLogMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AgentCommissionInfoMapper agentCommissionInfoMapper;

    @Autowired
    private AppRewardRankMapper rewardRankMapper;

    @Autowired
    private AppRewardLogMapper rewardLogMapper;

    /**
     * 获取用户列表
     */
    @GmHandler(key = "/osee/player/list")
    public void doPlayerListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder();
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date date = new Date(startTime);
                        condBuilder.append(" AND user.create_time >= '").append(DATE_FORMATER.format(date)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date date = new Date(endTime);
                        condBuilder.append(" AND user.create_time <= '").append(DATE_FORMATER.format(date)).append("'");
                    }
                    break;
                case "playerId":
                    long id = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (id > 0) {
                        condBuilder.append(" AND user.id = ").append(id);
                    }
                    break;
                case "username":
                    String username = JsonMapUtils.parseObject(params, "username", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(username)) {
                        condBuilder.append(" AND user.username = '").append(username).append("'");
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND user.nickname = '").append(nickname).append("'");
                    }
                    break;
                case "userState":
                    long userState = JsonMapUtils.parseObject(params, "userState", JsonInnerType.TYPE_LONG);
                    if (userState > 0) {
                        condBuilder.append(" AND user.user_state = ").append(userState - 1);
                    }
                    break;
                case "gameState":
                    long gameState = JsonMapUtils.parseObject(params, "gameState", JsonInnerType.TYPE_LONG);
                    if (gameState > 0) {
                        if (gameState < 100) {
                            condBuilder.append(" AND user.online_state = ").append(gameState - 1);
                        } else {
                            condBuilder.append(" AND user.online_state > 0");
                        }
                    }
                    break;
                case "vipLevel":
                    int vipLevel = JsonMapUtils.parseObject(params, "vipLevel", JsonInnerType.TYPE_INT);
                    if (vipLevel > 0) {
                        condBuilder.append(" AND player.vip_level = ").append(vipLevel);
                    }
                    break;
                case "loginType":
                    int loginType = JsonMapUtils.parseObject(params, "loginType", JsonInnerType.TYPE_INT);
                    if (loginType > 0) {
                        if (loginType == 1) { // 微信登录
                            condBuilder.append(" AND TRIM(user.openid) != ''");
                        } else if (loginType == 2) { // 账号登录
                            condBuilder.append(" AND TRIM(user.openid) = ''");
                        }
                    }
                    break;
                case "loseControl":
                    int loseControl = JsonMapUtils.parseObject(params, "loseControl", JsonInnerType.TYPE_INT);
                    if (loseControl > 0) {
                        condBuilder.append(" AND player.lose_control = ").append(loseControl - 1);
                    }
                    break;
                case "playerType":
                    int playerType = JsonMapUtils.parseObject(params, "playerType", JsonInnerType.TYPE_INT);
                    if (playerType == 1) { // 非代理
                        condBuilder.append(" AND (agent.agent_level is null or agent.agent_level = 3)");
                    } else if (playerType == 2) { // 代理
                        condBuilder.append(" AND (agent.agent_level < 3)");
                    }
                    break;
            }
        }
        List<Long> idList = playerMapper.getGmPlayerIdList(condBuilder.toString(), pageBuilder.toString());
        int idCount = playerMapper.getGmPlayerCount(condBuilder.toString());

        List<Map<String, Object>> playerList = new LinkedList<>();
        for (long id : idList) {
            ServerUser user = UserContainer.getUserById(id);
            OseePlayerEntity entity = PlayerManager.getPlayerEntity(user);
            Map<String, Object> playerInfoMap = new HashMap<>();
            playerInfoMap.put("playerId", user.getId());
            playerInfoMap.put("vipLevel", entity.getVipLevel());
            playerInfoMap.put("level", entity.getLevel()); // 玩家等级
            playerInfoMap.put("batteryLevel", entity.getBatteryLevel()); // 最高拥有炮台等级
            playerInfoMap.put("playerType", entity.getPlayerType() + 1);
            playerInfoMap.put("nickname", user.getNickname());
            playerInfoMap.put("loginType", StringUtils.isEmpty(user.getOpenid()) ? 2 : 1);
            // 天天摸鱼手机号即是账号
            playerInfoMap.put("username", StringUtils.isEmpty(user.getPhonenum()) ? "" : user.getUsername());
            playerInfoMap.put("diamond", entity.getDiamond());
            playerInfoMap.put("lottery", entity.getLottery());
            playerInfoMap.put("money", entity.getMoney());
            playerInfoMap.put("bankMoney", entity.getBankMoney());
            playerInfoMap.put("createTime", user.getEntity().getCreateTime());
            playerInfoMap.put("gameState", user.getEntity().getOnlineState() + 1);
            playerInfoMap.put("loseControl", entity.getLoseControl() + 1);
            playerInfoMap.put("userState", user.getEntity().getUserState() + 1);

            String ag_ = RedisHelper.get(FruitLaBaManager.REDIS_PLAYER_AG_KEY + user.getId());
            Integer ag = Integer.valueOf(ag_.equals("") ? "63" : ag_);
            StringBuilder ag_str = new StringBuilder("");
            if((ag & 0x1) == 0x1) ag_str.append("1");
            if((ag & 0x2) == 0x2) ag_str.append("2");
            if((ag & 0x4) == 0x4) ag_str.append("3");
            if((ag & 0x8) == 0x8) ag_str.append("4");
            if((ag & 0x10) == 0x10) ag_str.append("5");
            if((ag & 0x20) == 0x20) ag_str.append("6");
            playerInfoMap.put("ag_str", ag_str);

            playerList.add(playerInfoMap);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", playerList);
        resultMap.put("totalNum", idCount);
        response.setData(resultMap);
    }

    /**
     * 获取用户详细信息
     */
    @GmHandler(key = "/osee/player/info")
    public void doPlayerInfoTask(Map<String, Object> params, CommonResponse response) {
        long playerId = (long) (double) params.get("playerId");

        ServerUser user = UserContainer.getUserById(playerId);
        if (user != null) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("loseControl", PlayerManager.getPlayerEntity(user).getLoseControl() + 1);
            resultMap.put("playerType", PlayerManager.getPlayerEntity(user).getPlayerType() + 1);
            AgentEntity agentEntity = agentManager.getAgentInfoByPlayerId(playerId); // 玩家代理信息
            if (agentEntity != null) {
                resultMap.put("firstCommissionRate", agentEntity.getFirstCommissionRate());
                resultMap.put("secondCommissionRate", agentEntity.getSecondCommissionRate());
                long openChessCards = 0;
                String openChessCardsStr = RedisHelper.get("Agent:OpenChessCards:" + agentEntity.getPlayerId());
                if (!StringUtils.isEmpty(openChessCardsStr)) {
                    openChessCards = Long.parseLong(openChessCardsStr);
                }
                resultMap.put("openChessCards", openChessCards);
                if (agentEntity.getAgentLevel() == 1) {
                    resultMap.put("rate", agentEntity.getFirstCommissionRate());
                    resultMap.put("playerName", agentEntity.getPlayerName());
                }
            } else {
                // 代理默认设置为关闭棋牌
                resultMap.put("openChessCards", 0);
            }
            resultMap.put("fishingProb", FishingHitDataManager.getPlayerFishingProb(playerId));
            resultMap.put("nickname", user.getNickname());
            resultMap.put("lottery", PlayerManager.getPlayerEntity(user).getLottery());

            String ag_str = RedisHelper.get(FruitLaBaManager.REDIS_PLAYER_AG_KEY + playerId);
            String one_str = RedisHelper.get(FruitLaBaManager.REDIS_PLAYER_RATE_ONE_KEY + playerId);
            resultMap.put("ag", Integer.parseInt(ag_str == "" ? "63" : ag_str));
            resultMap.put("one", Integer.parseInt(one_str == "" ? "511" : one_str));

            response.setData(resultMap);
        } else {
            response.setSuccess(false);
            response.setErrMsg("玩家不存在");
        }
    }

    /**
     * 修改用户金币
     */
    @GmHandler(key = "/osee/player/tenure/update")
    public void doPlayerTenureUpdateTask(Map<String, Object> params, CommonResponse response) {
        long playerId = (long) (double) params.get("playerId");
        int tenureType = (int) (double) params.get("tenureType");
        long number = (long) (double) params.get("number");
        String creator = params.get("creator").toString();

        ServerUser user = UserContainer.getUserById(playerId);
        if (PlayerManager.checkItem(user, tenureType, -number)) {
            ItemChangeReason reason = number >= 0 ? ItemChangeReason.GM_RECHARGE : ItemChangeReason.GM_DEDUCT;
            PlayerManager.addItem(user, tenureType, number, reason, true);

            OseeRechargeLogEntity log = new OseeRechargeLogEntity();
            log.setUserId(user.getId());
            log.setNickname(user.getNickname());
            log.setShopType(tenureType);
            log.setCount((int) number);
            log.setCreator(creator);
            log.setRechargeType(3); // 后台充值
            log.setOrderState(1); // 充值成功
            if (number > 0) { // 充值
                log.setOrderNum("R" + System.currentTimeMillis() / 1000 + ThreadLocalRandom.current().nextInt(1000));
            } else { // 扣除
                log.setOrderNum("D" + System.currentTimeMillis() / 1000 + ThreadLocalRandom.current().nextInt(1000));
            }
            rechargeLogMapper.save(log);
        } else if (tenureType == ItemId.MONEY.getId() && PlayerManager.getRealPlayerMoney(user) + number >= 0) {
            List<ItemData> itemData = new ArrayList<>();
            itemData.add(new ItemData(ItemId.MONEY.getId(), -PlayerManager.getItemNum(user, ItemId.MONEY)));
            itemData.add(new ItemData(ItemId.BANK_MONEY.getId(), number - itemData.get(0).getCount()));
            PlayerManager.addItems(user, itemData, ItemChangeReason.GM_DEDUCT, true);

            OseeRechargeLogEntity log = new OseeRechargeLogEntity();
            log.setUserId(user.getId());
            log.setNickname(user.getNickname());
            log.setShopType(tenureType);
            log.setCount((int) number);
            log.setCreator(creator);
            log.setRechargeType(3); // 后台充值
            log.setOrderState(1); // 充值成功
            log.setOrderNum("D" + System.currentTimeMillis() / 1000 + ThreadLocalRandom.current().nextInt(1000));
            rechargeLogMapper.save(log);
        } else {
            response.setSuccess(false);
            response.setErrMsg("用户余额不足");
        }
    }

    /**
     * 冻结/解冻
     */
    @SuppressWarnings("unchecked")
    @GmHandler(key = "/osee/player/frozen")
    public void doPlayerFrozenTask(Map<String, Object> params, CommonResponse response) {
        List<Double> ids = (List<Double>) params.get("list");
        int type = (int) (double) params.get("type");

        type = type == 1 ? 1 : 0;
        for (double id : ids) {
            ServerUser user = UserContainer.getUserById(Math.round(id));
            user.getEntity().setUserState(type);
            userMapper.update(user.getEntity());
            if (type == 1) {
                NetManager.closeClientConnect(user);
            }
        }
    }

    /**
     * 强制下线
     */
    @SuppressWarnings("unchecked")
    @GmHandler(key = "/osee/player/offline")
    public void doPlayerOfflineTask(Map<String, Object> params, CommonResponse response) {
        List<Double> ids = (List<Double>) params.get("list");

        LogoutResponse resp = LogoutResponse.newBuilder().setResult(2).build();

        if (ids == null || ids.size() == 0) { // 全员下线
            List<ServerUser> users = UserContainer.getActiveServerUsers();
            for (ServerUser user : users) {
                NetManager.sendMessage(LoginMsgCode.S_C_LOGOUT_RESPONSE_VALUE, resp, user);
                NetManager.closeClientConnect(user);
            }
        } else {
            for (double id : ids) { // 选定玩家下线
                ServerUser user = UserContainer.getUserById(Math.round(id));
                NetManager.sendMessage(LoginMsgCode.S_C_LOGOUT_RESPONSE_VALUE, resp, user);
                NetManager.closeClientConnect(user);
            }
        }
    }

    /**
     * 初始化全服捕鱼系数
     */
    @GmHandler(key = "/osee/fishing_prob/init")
    public void doFishingProbInitTask(Map<String, Object> params, CommonResponse response) {
        FishingHitDataManager.resetAllPlayerFishingProb();
    }

    /**
     * 修改用户数据
     */
    @GmHandler(key = "/osee/player/update")
    public void doPlayerUpdateTask(Map<String, Object> params, CommonResponse response) {

        Long playerId = ((Number)params.get("playerId")).longValue();
        Integer ag = ((Number) params.get("ag")).intValue();
        Integer one = ((Number) params.get("one")).intValue();
        System.out.println(playerId);
        System.out.println(ag.toString());
        System.out.println(one.toString());
        RedisHelper.set(FruitLaBaManager.REDIS_PLAYER_AG_KEY + playerId, String.valueOf(ag));
        RedisHelper.set(FruitLaBaManager.REDIS_PLAYER_RATE_ONE_KEY + playerId, String.valueOf(one));

        ServerUser user = UserContainer.getUserById((long) (double) params.get("playerId"));
        if (user != null) {
            int playerType = (int) (double) params.get("playerType") - 1; // 1~2 - 1 ：0~1 0表示玩家 1表示代理
            if (playerType == 1) { // 如果设置为代理了
                AgentEntity agent = agentManager.getAgentInfoByPlayerId(user.getId());
                if (agent != null && agent.getAgentLevel() == 2) {
                    response.setSuccess(false);
                    response.setErrMsg("账号已为推广员，不可设置为渠道商！");
                    return;
                }

                double first = (double) params.get("rate");
                String playerName = params.get("playerName").toString();
                boolean success = agentManager.setAgent(user, first, first / 2, playerName);
                String openChessCards = params.get("openChessCards").toString();
                Double occ = Double.parseDouble(openChessCards);

                RedisHelper.set("Agent:OpenChessCards:" + user.getId(), String.valueOf(occ.intValue()));
                if (!success) {
                    response.setSuccess(false);
                    response.setErrMsg("设置渠道商失败！");
                    return;
                }
            }
            if (!StringUtils.isEmpty(params.get("password"))) {
                user.getEntity().setPassword(params.get("password").toString());
            }
            OseePlayerEntity entity = PlayerManager.getPlayerEntity(user);
            entity.setLoseControl((int) (double) params.get("loseControl") - 1);// 1~2 - 1 ：0~1 0表示正常 1表示控制必输 2表示必赢
            entity.setPlayerType(playerType);
            if (!StringUtils.isEmpty(params.get("bankpassword"))) {
                entity.setBankPassword((String) params.get("bankpassword"));
            }
            FishingHitDataManager.setPlayerFishingProb(user.getId(), (double) params.get("fishingProb"));

            userMapper.update(user.getEntity());
            PlayerManager.updateEntities.add(entity);
        }
    }

    /**
     * 获取实名认证列表
     */
    @GmHandler(key = "/osee/authentication/list")
    public void doAuthenticationListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND record.create_time >= '").append(DATE_FORMATER.format(startDate))
                                .append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND record.create_time <= '").append(DATE_FORMATER.format(endDate))
                                .append("'");
                    }
                    break;
                case "playerId":
                    long id = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (id > 0) {
                        condBuilder.append(" AND record.user_id = ").append(id);
                    }
                    break;
                case "realName":
                    String realName = JsonMapUtils.parseObject(params, "realName", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(realName)) {
                        condBuilder.append(" AND record.name = '").append(realName).append("'");
                    }
                    break;
                case "nickName":
                    String nickName = JsonMapUtils.parseObject(params, "nickName", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickName)) {
                        condBuilder.append(" AND user.nickname = '").append(nickName).append("'");
                    }
                    break;
                case "phoneNum":
                    String phoneNum = JsonMapUtils.parseObject(params, "phoneNum", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(phoneNum)) {
                        condBuilder.append(" AND record.phoneNum = '").append(phoneNum).append("'");
                    }
                    break;
            }
        }

        List<GmAuthenticationInfo> authentications = gmCommonMapper.getAuthenticationList(condBuilder.toString(),
                pageBuilder.toString());
        int total = gmCommonMapper.getAuthenticationCount(condBuilder.toString());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalNum", total);
        resultMap.put("list", JsonMapUtils.objectsToMaps(authentications));
        response.setData(resultMap);
    }

    /**
     * 删除实名认证记录
     */
    @GmHandler(key = "/osee/authentication/delete")
    public void doAuthenticationDeleteTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long recordId = JsonMapUtils.parseObject(params, "id", JsonInnerType.TYPE_LONG);
        authenticationMapper.delete(recordId);
    }

    /**
     * 获取商品奖品数据
     */
    @GmHandler(key = "/osee/shop/list")
    public void doShopListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "name":
                    String name = JsonMapUtils.parseObject(params, "name", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(name)) {
                        condBuilder.append(" AND name = '").append(name).append("'");
                    }
                    break;
                case "type":
                    int type = JsonMapUtils.parseObject(params, "type", JsonInnerType.TYPE_INT);
                    if (type > 0) {
                        condBuilder.append(" AND type = ").append(type);
                    }
                    break;
            }
        }

        List<OseeLotteryShopEntity> entities = lotteryShopMapper.selectList(condBuilder.toString(),
                pageBuilder.toString());
        int count = lotteryShopMapper.selectCount(condBuilder.toString());

        List<Map<String, Object>> itemList = new LinkedList<>();
        for (OseeLotteryShopEntity entity : entities) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", entity.getId());
            itemMap.put("type", entity.getType());
            itemMap.put("count", entity.getCount());
            itemMap.put("name", entity.getName());
            itemMap.put("cost", entity.getCost());
            itemMap.put("img", entity.getImg());
            itemMap.put("size", entity.getSize());
            itemMap.put("restSize", entity.getSize() - entity.getUsedSize());
            itemMap.put("refreshType", entity.getRefreshType());
            itemMap.put("sendType", entity.getSendType());
            long stock;
            if (entity.getType() == 1 && entity.getSendType() == 3) { // 自动发卡的实物读取库存
                stock = stockMapper.getUnusedCount(entity.getId());
            } else {
                stock = entity.getStock();
            }
            itemMap.put("stock", stock);
            itemList.add(itemMap);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalNum", count);
        resultMap.put("list", itemList);
        response.setData(resultMap);
    }

    /**
     * 添加商品奖品数据
     */
    @GmHandler(key = "/osee/shop/add")
    public void doShopAddTask(Map<String, Object> params, CommonResponse response) throws Exception {
        // 限购次数
        int size = params.containsKey("size") ? (int) (double) params.get("size") : 0;

        int type = (int) (double) params.get("type");
        int sendType = (int) (double) params.get("sendType");
        if (type == 1 && sendType == 1) { // 实物不能选择实时兑换
            response.setSuccess(false);
            response.setErrMsg("实物奖品不能选择实时兑换");
            return;
        } else if (type != 1 && sendType != 1) {
            response.setSuccess(false);
            response.setErrMsg("虚拟奖品请选择实时兑换");
            return;
        }
        // 库存
        int stock = params.containsKey("stock") ? (int) (double) params.get("stock") : 0;
        if (sendType == 3) { // 自动发卡库存不可设置，手动添加
            stock = 0;
        } else if (size > stock) { // 当限购次数大于库存时，则使用库存作为本轮可购次数
            size = stock;
        }

        OseeLotteryShopEntity shop = new OseeLotteryShopEntity();
        shop.setType(type);
        shop.setCount((long) (double) params.get("count"));
        shop.setName(params.get("name").toString());
        shop.setImg(params.get("img").toString());
        shop.setCost((int) (double) params.get("cost"));
        shop.setRefreshType((int) (double) params.get("refreshType"));
        shop.setSize(size);
        shop.setSendType(sendType);
        shop.setStock(stock);
        lotteryShopMapper.save(shop);
        shoppingManager.refreshLottery();
    }

    /**
     * 删除商品奖品数据
     */
    @GmHandler(key = "/osee/shop/delete")
    public void doShopDeleteTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long id = (long) (double) params.get("id");
        lotteryShopMapper.deleteById(id);
        shoppingManager.refreshLottery();
    }

    /**
     * 修改商城奖品数据
     */
    @GmHandler(key = "/osee/shop/update")
    public void doShopUpdateTask(Map<String, Object> params, CommonResponse response) throws Exception {
        OseeLotteryShopEntity shop = lotteryShopMapper.getById((long) (double) params.get("id"));
        if (shop != null) {
            // 限购次数
            int size = params.containsKey("size") ? (int) (double) params.get("size") : 0;

            int type = (int) (double) params.get("type");
            int sendType = (int) (double) params.get("sendType");
            if (type == 1 && sendType == 1) { // 实物不能选择实时兑换
                response.setSuccess(false);
                response.setErrMsg("实物奖品不能选择实时兑换");
                return;
            } else if (type != 1 && sendType != 1) {
                response.setSuccess(false);
                response.setErrMsg("虚拟奖品请选择实时兑换");
                return;
            }
            // 库存
            int stock = params.containsKey("stock") ? (int) (double) params.get("stock") : 0;
            if (sendType == 3) { // 自动发卡库存不可设置，手动添加
                stock = 0;
            } else if (size > stock) { // 当限购次数大于库存时，则使用库存作为本轮可购次数
                size = stock;
            }
            shop.setType((int) (double) params.get("type"));
            shop.setCount((long) (double) params.get("count"));
            shop.setName(params.get("name").toString());
            shop.setImg(params.get("img").toString());
            shop.setCost((int) (double) params.get("cost"));
            shop.setSize(size);
            shop.setRefreshType((int) (double) params.get("refreshType"));
            shop.setSendType(sendType);
            shop.setStock(stock);
            lotteryShopMapper.update(shop);
            shoppingManager.refreshLottery();
        } else {
            response.setSuccess(false);
            response.setErrMsg("奖品不存在");
        }
    }

    /**
     * 获取指定商城奖品数据
     */
    @GmHandler(key = "/osee/shop/search")
    public void doShopSearchTask(Map<String, Object> params, CommonResponse response) throws Exception {
        OseeLotteryShopEntity entity = lotteryShopMapper.getById((long) (double) params.get("id"));
        if (entity != null) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", entity.getId());
            itemMap.put("type", entity.getType());
            itemMap.put("count", entity.getCount());
            itemMap.put("name", entity.getName());
            itemMap.put("cost", entity.getCost());
            itemMap.put("img", entity.getImg());
            itemMap.put("size", entity.getSize());
            itemMap.put("restSize", entity.getSize() - entity.getUsedSize());
            itemMap.put("refreshType", entity.getRefreshType());
            itemMap.put("sendType", entity.getSendType());
            long stock;
            if (entity.getType() == 1 && entity.getSendType() == 3) { // 自动发卡的实物读取库存
                stock = stockMapper.getUnusedCount(entity.getId());
            } else {
                stock = entity.getStock();
            }
            itemMap.put("stock", stock);
            response.setData(itemMap);
        } else {
            response.setSuccess(false);
            response.setErrMsg("商品不存在");
        }
    }

    /**
     * 获取商品库存列表
     */
    @GmHandler(key = "/osee/shop/stock/list")
    public void doShopStockListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long shopId = JsonMapUtils.parseObject(params, "shopId", JsonInnerType.TYPE_LONG);
        OseeLotteryShopEntity shopEntity = lotteryShopMapper.getById(shopId);
        if (shopEntity == null) {
            response.setSuccess(false);
            response.setErrMsg("商品不存在");
            return;
        }

        // 查询条件构造
        StringBuilder query = new StringBuilder("where `shop_id` = ").append(shopId);
        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "number":
                    String number = JsonMapUtils.parseObject(params, "number", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(number)) {
                        query.append(" and `number` = '").append(number).append("'");
                    }
                    break;
                case "userId":
                    long userId = JsonMapUtils.parseObject(params, "userId", JsonInnerType.TYPE_LONG);
                    if (userId > 0) {
                        query.append(" and `user_id` = ").append(userId);
                    }
                    break;
                case "state":
                    int state = JsonMapUtils.parseObject(params, "state", JsonInnerType.TYPE_INT);
                    // 1-未兑换 2-已兑换
                    if (state == 1) {
                        query.append(" and `user_id` is null");
                    } else if (state == 2) {
                        query.append(" and `user_id` is not null");
                    }
                    break;
            }
        }
        // 获取数据的总条数
        long count = stockMapper.getCount(query.toString());
        query.append(" order by create_time desc");
        // 解析分页数据
        int pageNo = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);
        query.append(" limit ").append((pageNo - 1) * pageSize).append(",").append(pageSize);
        // 获取数据
        List<StockEntity> stockEntityList = stockMapper.getList(query.toString());
        LinkedList<Map<String, Object>> list = new LinkedList<>();
        for (StockEntity entity : stockEntityList) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", entity.getId());
            item.put("number", entity.getNumber());
            item.put("password", "******"); // entity.getPassword()); // 密码不传输
            item.put("userId", entity.getUserId());
            item.put("state", entity.getUserId() == null ? 1 : 2);
            list.add(item);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalNum", count);
        resultMap.put("list", list);

        // 获取库存总数
        long stock = stockMapper.getCount("where `shop_id` = " + shopId);
        // 获取已经兑换的数量
        long usedNum = stockMapper.getCount("where `user_id` is not null and `shop_id` =" + shopId);
        resultMap.put("shopName", shopEntity.getName());
        resultMap.put("stock", stock);
        resultMap.put("usedNum", usedNum);
        response.setData(resultMap);
    }

    /**
     * 添加奖券商城商品库存任务
     */
    @GmHandler(key = "/osee/shop/stock/add")
    public void doShopStockAddTask(Map<String, Object> params, CommonResponse response) {
        StockEntity stockEntity = JSON.parseObject(JSON.toJSONString(params), StockEntity.class);
        if (stockEntity == null) {
            response.setSuccess(false);
            response.setErrMsg("数据为空");
            return;
        }
        stockMapper.save(stockEntity);
    }

    /**
     * 提交实物兑换订单
     */
    @GmHandler(key = "/osee/shop/submit")
    public void doShopSubmitTask(Map<String, Object> params, CommonResponse response) throws Exception {
        OseeLotteryShopEntity shop = lotteryShopMapper.getById((long) (double) params.get("shopId"));
        ServerUser user = UserContainer.getUserById((long) (double) params.get("playerId"));
        response.setSuccess(false);
        if (shop == null) {
            response.setErrMsg("商品不存在");
        } else if (user == null) {
            response.setErrMsg("用户不存在");
        } else if (shop.getType() != 1) {
            response.setErrMsg("目标商品不为实物");
        } else {
            int count = (int) (double) params.get("count");
            long price = count * shop.getCost();
            if (shop.getSize() != 0 && shop.getUsedSize() + count > shop.getSize()) {
                response.setErrMsg("商品剩余数量不足");
                return;
            }

            if (!PlayerManager.checkItem(user, ItemId.LOTTERY, price)) {
                response.setErrMsg("用户奖券不足");
                return;
            }
            PlayerManager.addItem(user, ItemId.LOTTERY, -price, ItemChangeReason.SHOPPING, true);

            shop.setUsedSize(shop.getUsedSize() + count);
            lotteryShopMapper.update(shop);

            OseeRealLotteryLogEntity entity = new OseeRealLotteryLogEntity();
            entity.setOrderNum("R" + System.currentTimeMillis() / 1000 + ThreadLocalRandom.current().nextInt(1000));
            entity.setUserId(user.getId());
            entity.setNickname(user.getNickname());
            entity.setRewardName(shop.getName());
            entity.setCount(count);
            entity.setCost(price);
            entity.setCreator(params.get("creator").toString());
            entity.setConsignee(params.get("consignee").toString());
            entity.setPhoneNum(params.get("phoneNum").toString());
            entity.setAddress(params.get("address").toString());
            realLotteryMapper.save(entity);
            response.setSuccess(true);
        }
    }

    /**
     * 获取实物数据
     */
    @GmHandler(key = "/osee/shop/entity/list")
    public void doShopEntityListTask(Map<String, Object> params, CommonResponse response) {
        List<OseeLotteryShopEntity> lotteryShops = lotteryShopMapper.getAllEntity();
        List<Map<String, Object>> shopMaps = new ArrayList<>();
        for (OseeLotteryShopEntity shopEntity : lotteryShops) {
            Map<String, Object> shopMap = new HashMap<>();
            shopMap.put("id", shopEntity.getId());
            shopMap.put("name", shopEntity.getName());
            shopMaps.add(shopMap);
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", shopMaps);
        response.setData(resultMap);
    }

    /**
     * 获取虚拟道具兑换记录
     */
    @GmHandler(key = "/osee/shop/unreal_log/list")
    public void doShopUnrealLogListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND record.create_time >= '").append(DATE_FORMATER.format(startDate))
                                .append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND record.create_time <= '").append(DATE_FORMATER.format(endDate))
                                .append("'");
                    }
                    break;
                case "orderNum":
                    String orderNum = JsonMapUtils.parseObject(params, "orderNum", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(orderNum)) {
                        condBuilder.append(" AND record.order_num = '").append(orderNum).append("'");
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (!StringUtils.isEmpty(playerId)) {
                        condBuilder.append(" AND record.user_id = ").append(playerId);
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND record.nickname = '").append(nickname).append("'");
                    }
                    break;
            }
        }
        List<OseeUnrealLotteryLogEntity> logList = unrealLotteryMapper.getLogList(condBuilder.toString(),
                pageBuilder.toString());
        int logCount = unrealLotteryMapper.getLogCount(condBuilder.toString());

        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeUnrealLotteryLogEntity log : logList) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("orderNum", log.getOrderNum());
            dataMap.put("playerId", log.getUserId());
            dataMap.put("nickname", log.getNickname());
            dataMap.put("name", log.getRewardName());
            dataMap.put("type", log.getType());
            dataMap.put("count", log.getCount());
            dataMap.put("costType", log.getItemId());
            dataMap.put("cost", log.getCost());
            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalNum", logCount);
        result.put("list", dataList);
        response.setData(result);
    }

    /**
     * 获取实物道具兑换记录
     */
    @GmHandler(key = "/osee/shop/real_log/list")
    public void doShopRealLogListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND record.create_time >= '").append(DATE_FORMATER.format(startDate))
                                .append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND record.create_time <= '").append(DATE_FORMATER.format(endDate))
                                .append("'");
                    }
                    break;
                case "orderNum":
                    String orderNum = JsonMapUtils.parseObject(params, "orderNum", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(orderNum)) {
                        condBuilder.append(" AND record.order_num = '").append(orderNum).append("'");
                    }
                    break;
                case "orderState":
                    int orderState = JsonMapUtils.parseObject(params, "orderState", JsonInnerType.TYPE_INT);
                    if (orderState > 0) {
                        condBuilder.append(" AND record.order_state = ").append(orderState - 1);
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (!StringUtils.isEmpty(playerId)) {
                        condBuilder.append(" AND record.user_id = ").append(playerId);
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND record.nickname = '").append(nickname).append("'");
                    }
                    break;
            }
        }
        List<OseeRealLotteryLogEntity> logList = realLotteryMapper.getLogList(condBuilder.toString(),
                pageBuilder.toString());
        int logCount = realLotteryMapper.getLogCount(condBuilder.toString());
        List<Map<String, Object>> groupList = realLotteryMapper.getGroupCount("order_state");

        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeRealLotteryLogEntity log : logList) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("orderId", log.getId());
            dataMap.put("createTime", log.getCreateTime());
//            dataMap.put("orderNum", log.getOrderNum());
            dataMap.put("playerId", log.getUserId());
            dataMap.put("nickname", log.getNickname());
            dataMap.put("name", log.getRewardName());
            dataMap.put("count", log.getCount());
            dataMap.put("cost", log.getCost());
//            dataMap.put("creator", log.getCreator());
            dataMap.put("consignee", log.getConsignee());
            dataMap.put("phoneNum", log.getPhoneNum());
            dataMap.put("address", log.getAddress());
            dataMap.put("orderState", log.getOrderState() + 1);
            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();

        for (int i = 0, k = 0; i < 3; i++) {
            String key = "state_" + i;
            if (groupList.size() > 0 && k < groupList.size() && (int) groupList.get(k).get("key") == i) {
                result.put(key, groupList.get(k).get("count"));
                k++;
            } else {
                result.put(key, 0);
            }
        }

        result.put("totalNum", logCount);
        result.put("list", dataList);
        response.setData(result);
    }

    /**
     * 修改订单状态
     */
    @GmHandler(key = "/osee/shop/real_log/state/update")
    public void doShopRealLogStateUpdateTask(Map<String, Object> params, CommonResponse response) {
        long id = (long) (double) params.get("id");
        int state = (int) (double) params.get("state");

        OseeRealLotteryLogEntity entity = realLotteryMapper.getById(id);
        if (state != 2 && state != 3) {
            response.setSuccess(false);
            response.setErrMsg("无效的订单状态:" + state);
        } else if (entity == null) {
            response.setSuccess(false);
            response.setErrMsg("订单不存在");
        } else if (entity.getOrderState() != 0) {
            response.setSuccess(false);
            response.setErrMsg("订单当前状态为" + (entity.getOrderState() == 1 ? "已发货" : "已拒绝") + "，无法修改订单状态");
        } else {
            entity.setOrderState(state - 1);
            if (state == 3) {
                // 拒绝发货退回奖券
                ServerUser user = UserContainer.getUserById(entity.getUserId());
                PlayerManager.addItem(user, ItemId.LOTTERY, entity.getCost(), ItemChangeReason.SHOPPING, true);
            }
            realLotteryMapper.update(entity);
        }
    }

    /**
     * 获取充值记录
     */
    @GmHandler(key = "/osee/recharge/list")
    public void doRechargeListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE log.count > 0");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "orderNum":
                    String orderNum = JsonMapUtils.parseObject(params, "orderNum", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(orderNum)) {
                        condBuilder.append(" AND log.order_num = '").append(orderNum).append("'");
                    }
                    break;
                case "rechargeType":
                    int rechargeType = JsonMapUtils.parseObject(params, "rechargeType", JsonInnerType.TYPE_INT);
                    condBuilder.append(" AND log.recharge_type = ").append(rechargeType);
                    break;
                case "orderState":
                    int orderState = JsonMapUtils.parseObject(params, "orderState", JsonInnerType.TYPE_INT);
                    condBuilder.append(" AND log.order_state = ").append(orderState);
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (!StringUtils.isEmpty(playerId)) {
                        condBuilder.append(" AND log.user_id = ").append(playerId);
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND log.nickname = '").append(nickname).append("'");
                    }
                    break;
                case "creator":
                    String creator = JsonMapUtils.parseObject(params, "creator", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(creator)) {
                        condBuilder.append(" AND log.creator = '").append(creator).append("'");
                    }
                    break;
                case "shopType":
                    int shopType = JsonMapUtils.parseObject(params, "shopType", JsonInnerType.TYPE_INT);
                    if (shopType > 0) {
                        condBuilder.append(" AND log.shop_type = ").append(shopType);
                    }
                    break;
            }
        }

        List<OseeRechargeLogEntity> logList = rechargeLogMapper.getLogList(condBuilder.toString(),
                pageBuilder.toString());
        long logCount = rechargeLogMapper.getLogCount(condBuilder.toString());
        // 记录的金币是分为单位，所以最后要换算成元为单位
        long recharge = rechargeLogMapper.getTotalRecharge(condBuilder.toString(), "pay_money") / 100;

        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeRechargeLogEntity log : logList) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("orderNum", log.getOrderNum());
            dataMap.put("playerId", log.getUserId());
            dataMap.put("nickname", log.getNickname());
            dataMap.put("payMoney", log.getPayMoney());
            dataMap.put("shopName", StringUtils.isEmpty(log.getShopName()) ? "--" : log.getShopName());
            dataMap.put("shopType", log.getShopType());
            dataMap.put("count", log.getCount());
            dataMap.put("creator", log.getCreator());
            dataMap.put("rechargeType", log.getRechargeType());
            dataMap.put("orderState", log.getOrderState());
            dataList.add(dataMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalNum", logCount);
        result.put("totalMoney", recharge);
        result.put("list", dataList);
        response.setData(result);
    }

    /**
     * 获取扣除记录
     */
    @GmHandler(key = "/osee/deduct/list")
    public void doDeductListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE log.count < 0");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "orderNum":
                    String orderNum = JsonMapUtils.parseObject(params, "orderNum", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(orderNum)) {
                        condBuilder.append(" AND log.order_num = '").append(orderNum).append("'");
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (!StringUtils.isEmpty(playerId)) {
                        condBuilder.append(" AND log.user_id = ").append(playerId);
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND log.nickname = '").append(nickname).append("'");
                    }
                    break;
                case "creator":
                    String creator = JsonMapUtils.parseObject(params, "creator", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(creator)) {
                        condBuilder.append(" AND log.creator = '").append(creator).append("'");
                    }
                    break;
                case "deductType":
                    int shopType = JsonMapUtils.parseObject(params, "deductType", JsonInnerType.TYPE_INT);
                    if (shopType > 0) {
                        condBuilder.append(" AND log.shop_type = ").append(shopType);
                    }
                    break;
            }
        }

        List<OseeRechargeLogEntity> logList = rechargeLogMapper.getLogList(condBuilder.toString(),
                pageBuilder.toString());
        long logCount = rechargeLogMapper.getLogCount(condBuilder.toString());
        long deduct = rechargeLogMapper.getTotalRecharge(condBuilder.toString(), "count");

        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeRechargeLogEntity log : logList) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("orderNum", log.getOrderNum());
            dataMap.put("playerId", log.getUserId());
            dataMap.put("nickname", log.getNickname());
            dataMap.put("deductType", log.getShopType());
            dataMap.put("count", log.getCount());
            dataMap.put("creator", log.getCreator());
            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();

        result.put("totalNum", logCount);
        result.put("totalMoney", deduct);
        result.put("list", dataList);
        response.setData(result);
    }

    /**
     * 获取玩家账户变动原因
     */
    @GmHandler(key = "/osee/player/tenure/change_reason")
    public void doPlayerTenureChangeReasonTask(Map<String, Object> params, CommonResponse response) {
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) response.getData();
        List<Object> reasonList = new LinkedList<>();
        for (ItemChangeReason reason : ItemChangeReason.values()) {
            Map<String, Object> reasonMap = new HashMap<>();
            reasonMap.put("id", reason.getId());
            reasonMap.put("info", reason.getInfo());
            reasonList.add(reasonMap);
        }
        resultMap.put("list", reasonList);
    }

    /**
     * 获取玩家账户变动记录
     */
    @GmHandler(key = "/osee/player/tenure/log")
    public void doPlayerTenureLogTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (!StringUtils.isEmpty(playerId)) {
                        condBuilder.append(" AND log.user_id = ").append(playerId);
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND user.nickname = '").append(nickname).append("'");
                    }
                    break;
                case "type":
                    int type = JsonMapUtils.parseObject(params, "type", JsonInnerType.TYPE_INT);
                    if (type > 0) {
                        condBuilder.append(" AND log.reason = ").append(type);
                    }
                    break;
            }
        }

        List<OseePlayerTenureLogEntity> logs = tenureLogMapper.getLogList(condBuilder.toString(),
                pageBuilder.toString());
        int count = tenureLogMapper.getLogCount(condBuilder.toString());

        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseePlayerTenureLogEntity log : logs) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("playerId", log.getUserId());
            dataMap.put("nickname", log.getNickname());
            dataMap.put("reason", log.getReason());
            dataMap.put("preDiamond", log.getPreDiamond());
            dataMap.put("changeDiamond", log.getChangeDiamond());
            dataMap.put("afterDiamond", log.getPreDiamond() + log.getChangeDiamond());
            dataMap.put("preMoney", log.getPreMoney());
            dataMap.put("changeMoney", log.getChangeMoney());
            dataMap.put("afterMoney", log.getPreMoney() + log.getChangeMoney());
            dataMap.put("preLottery", log.getPreLottery());
            dataMap.put("changeLottery", log.getChangeLottery());
            dataMap.put("afterLottery", log.getPreLottery() + log.getChangeLottery());
            dataMap.put("preBankMoney", log.getPreBankMoney());
            dataMap.put("changeBankMoney", log.getChangeBankMoney());
            dataMap.put("afterBankMoney", log.getPreBankMoney() + log.getChangeBankMoney());

            // 变动前鱼雷数量明细
            String preTorpedoStr = "青*" + log.getPreBronzeTorpedo() + "\n" +
                    "银*" + log.getPreSilverTorpedo() + "\n" +
                    "金*" + log.getPreGoldTorpedo();
            dataMap.put("preTorpedo", preTorpedoStr);
            // 变动的鱼雷数量
            String changeTorpedoStr = "青 " + log.getChangeBronzeTorpedo() + "\n" +
                    "银 " + log.getChangeSilverTorpedo() + "\n" +
                    "金 " + log.getChangeGoldTorpedo();
            dataMap.put("changeTorpedo", changeTorpedoStr);
            // 变动后的鱼雷数量明细
            String afterTorpedoStr = "青*" + (log.getPreBronzeTorpedo() + log.getChangeBronzeTorpedo()) + "\n" +
                    "银*" + (log.getPreSilverTorpedo() + log.getChangeSilverTorpedo()) + "\n" +
                    "金*" + (log.getPreGoldTorpedo() + log.getChangeGoldTorpedo());
            dataMap.put("afterTorpedo", afterTorpedoStr);

            // 变动前技能数量明细
            String preSkillStr = "锁定*" + log.getPreSkillLock() + "\n" +
                    "冰冻*" + log.getPreSkillFrozen() + "\n" +
                    "急速*" + log.getPreSkillFast() + "\n" +
                    "暴击*" + log.getPreSkillCrit();
            dataMap.put("preSkill", preSkillStr);
            // 变动的技能数量
            String changeSkillStr = "锁定 " + log.getChangeSkillLock() + "\n" +
                    "冰冻 " + log.getChangeSkillFrozen() + "\n" +
                    "急速 " + log.getChangeSkillFast() + "\n" +
                    "暴击 " + log.getChangeSkillCrit();
            dataMap.put("changeSkill", changeSkillStr);
            // 变动后的技能数量明细
            String afterSkillStr = "锁定*" + (log.getPreSkillLock() + log.getChangeSkillLock()) + "\n" +
                    "冰冻*" + (log.getPreSkillFrozen() + log.getChangeSkillFrozen()) + "\n" +
                    "急速*" + (log.getPreSkillFast() + log.getChangeSkillFast()) + "\n" +
                    "暴击*" + (log.getPreSkillCrit() + log.getChangeSkillCrit());
            dataMap.put("afterSkill", afterSkillStr);

            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();

        result.put("totalNum", count);
        result.put("list", dataList);
        response.setData(result);
    }

    /**
     * 获取抽水明细
     */
    @GmHandler(key = "/osee/cut_money/list")
    public void doCutMoneyListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "game":
                    int game = JsonMapUtils.parseObject(params, "game", JsonInnerType.TYPE_INT);
                    if (game > 0) {
                        condBuilder.append(" AND log.game = ").append(game);
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND log.user_id = ").append(playerId);
                    }
                    break;
//                case "nickname":
//                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonInnerType.TYPE_STRING);
//                    if (!StringUtils.isEmpty(nickname)) {
//                        condBuilder.append(" AND user.nickname = '").append(nickname).append("'");
//                    }
//                    break;
            }
        }

        List<OseeCutMoneyLogEntity> logs = cutMoneyLogMapper.getLogList(condBuilder.toString(), pageBuilder.toString());
        Map<String, Object> count = cutMoneyLogMapper.getLogCount(condBuilder.toString());

        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeCutMoneyLogEntity log : logs) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("playerId", log.getUserId());
            dataMap.put("nickname", UserContainer.getUserById(log.getUserId()).getNickname());
            dataMap.put("game", log.getGame());
            dataMap.put("cutMoney", log.getCutMoney());
            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();

        result.put("totalNum", count.get("totalNum"));
        result.put("totalCut", count.get("totalCut"));
        result.put("list", dataList);
        response.setData(result);
    }

    /**
     * 获取支出明细
     */
    @GmHandler(key = "/osee/pay_money/list")
    public void doPayMoneyListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "payType":
                    int payType = JsonMapUtils.parseObject(params, "payType", JsonInnerType.TYPE_INT);
                    if (payType > 0) {
                        condBuilder.append(" AND log.pay_type = ").append(payType);
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND log.user_id = ").append(playerId);
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND user.nickname = '").append(nickname).append("'");
                    }
                    break;
            }
        }

        List<OseeExpendLogEntity> logs = expendLogMapper.getLogList(condBuilder.toString(), pageBuilder.toString());
        Map<String, Object> count = expendLogMapper.getLogCount(condBuilder.toString());

        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeExpendLogEntity log : logs) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("playerId", log.getUserId());
            dataMap.put("nickname", log.getNickname());
            dataMap.put("payType", log.getPayType());
            dataMap.put("diamond", log.getDiamond());
            dataMap.put("money", log.getMoney());
            dataMap.put("lottery", log.getLottery());
            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();

        result.put("totalNum", count.get("totalNum"));
        result.put("totalDiamond", count.get("diamond"));
        result.put("totalMoney", count.get("money"));
        result.put("totalLottery", count.get("lottery"));
        result.put("list", dataList);
        response.setData(result);
    }

    /**
     * 调换商城奖品顺序
     */
    @GmHandler(key = "/osee/shop/change")
    public void doShopChangeTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long id = JsonMapUtils.parseObject(params, "id", JsonInnerType.TYPE_LONG);
        int type = JsonMapUtils.parseObject(params, "type", JsonInnerType.TYPE_INT);

        if (!shoppingManager.changeLottery(id, type)) {
            response.setSuccess(false);
            response.setErrMsg("该项无法继续移动");
        }
    }

    /**
     * 获取轮盘概率
     */
    @GmHandler(key = "/osee/lottery_probability")
    public void doLotteryProbabilityTask(Map<String, Object> params, CommonResponse response) throws Exception {
        Map<String, Object> probMap = new HashMap<>();
        probMap.put("lock10", LotteryDrawManager.probabilities[0]);
        probMap.put("frozen8", LotteryDrawManager.probabilities[1]);
        probMap.put("fast5", LotteryDrawManager.probabilities[2]);
        probMap.put("crit3", LotteryDrawManager.probabilities[3]);
        probMap.put("money5000", LotteryDrawManager.probabilities[4]);
        probMap.put("money50000", LotteryDrawManager.probabilities[5]);
        probMap.put("diamond5", LotteryDrawManager.probabilities[6]);
        probMap.put("diamond10", LotteryDrawManager.probabilities[7]);
        response.setData(probMap);
    }

    /**
     * 修改轮盘概率
     */
    @GmHandler(key = "/osee/lottery_probability/update")
    public void doLotteryProbabilityUpdateTask(Map<String, Object> params, CommonResponse response) throws Exception {
        int totalProb = 0;
        for (Object prob : params.values()) {
            totalProb += (double) prob;
        }
        if (totalProb != 100) {
            response.setSuccess(false);
            response.setErrCode("TOTAL PROB ERROR");
            response.setErrMsg("中奖概率总和必须等于100");
            return;
        }
        // 设置概率
        LotteryDrawManager.probabilities[0] = (int) (double) params.get("lock10");
        LotteryDrawManager.probabilities[1] = (int) (double) params.get("frozen8");
        LotteryDrawManager.probabilities[2] = (int) (double) params.get("fast5");
        LotteryDrawManager.probabilities[3] = (int) (double) params.get("crit3");
        LotteryDrawManager.probabilities[4] = (int) (double) params.get("money5000");
        LotteryDrawManager.probabilities[5] = (int) (double) params.get("money50000");
        LotteryDrawManager.probabilities[6] = (int) (double) params.get("diamond5");
        LotteryDrawManager.probabilities[7] = (int) (double) params.get("diamond10");
        // 保存概率
        LotteryDrawManager.saveLotteryProb();
    }

    /**
     * 获取游走字幕列表
     */
    @GmHandler(key = "/osee/wander_subtitle/list")
    public void doWanderSubtitleListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        int page = JsonMapUtils.parseObject(params, "page", JsonMapUtils.JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonMapUtils.JsonInnerType.TYPE_INT);

        int skip = (page - 1) * pageSize;

        List<WanderSubtitleEntity> subtitles = subtitleMapper.getPage(skip, pageSize);
        int totalCount = subtitleMapper.getTotolCount();

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalNum", totalCount);

        long nowTime = System.currentTimeMillis();
        List<Map<String, Object>> mapResultsList = new LinkedList<>();
        for (int i = 0; i < subtitles.size(); i++) {
            WanderSubtitleEntity subtitle = subtitles.get(i);
            Map<String, Object> mapResults = new HashMap<>();
            mapResults.put("subtitleId", subtitle.getId());
            mapResults.put("content", subtitle.getContent());
            mapResults.put("intervalTime", subtitle.getIntervalTime());
            mapResults.put("effectiveTime", subtitle.getStartTime().getTime());
            mapResults.put("failureTime", subtitle.getEndTime().getTime());

            int state = 0;
            if (subtitle.getStartTime().getTime() > nowTime || subtitle.getEndTime().getTime() < nowTime) {
                state = 1;
            }
            mapResults.put("state", state);
            mapResultsList.add(mapResults);
        }
        resultMap.put("list", mapResultsList);
        response.setData(resultMap);
    }

    /**
     * 查询游走字幕
     */
    @GmHandler(key = "/osee/wander_subtitle/query")
    public void doWanderSubtitleQueryTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long id = JsonMapUtils.parseObject(params, "subtitleId", JsonMapUtils.JsonInnerType.TYPE_LONG);
        WanderSubtitleEntity subtitle = subtitleMapper.getById(id);
        if (subtitle == null) {
            response.setSuccess(false);
            response.setErrMsg("游走字幕不存在");
            return;
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("id", subtitle.getId());
        resultMap.put("content", subtitle.getContent());
        resultMap.put("intervalTime", subtitle.getIntervalTime());
//        resultMap.put("effectiveTime", subtitle.getStartTime().getTime());
//        resultMap.put("failureTime", subtitle.getEndTime().getTime());
        response.setData(resultMap);
    }

    /**
     * 更新游走字幕数据
     */
    @GmHandler(key = "/osee/wander_subtitle/update")
    public void doWanderSubtitleUpdateTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long id = (long) (double) params.get("id");
        WanderSubtitleEntity subtitle = subtitleManager.getWanderSubtitleById(id);
        if (subtitle == null) {
            response.setSuccess(false);
            response.setErrMsg("游走字幕不存在");
            return;
        }
        subtitle.setContent(JsonMapUtils.parseObject(params, "content", JsonMapUtils.JsonInnerType.TYPE_STRING));
        subtitle.setIntervalTime(JsonMapUtils.parseObject(params, "intervalTime", JsonMapUtils.JsonInnerType.TYPE_INT));
        subtitle.setStartTime(JsonMapUtils.parseObject(params, "effectiveTime", JsonMapUtils.JsonInnerType.TYPE_DATE));
        subtitle.setEndTime(JsonMapUtils.parseObject(params, "failureTime", JsonMapUtils.JsonInnerType.TYPE_DATE));
        subtitleMapper.update(subtitle);
        subtitleManager.addWanderSubtitle(subtitle);
    }

    /**
     * 删除游走字幕
     */
    @GmHandler(key = "/osee/wander_subtitle/delete")
    public void doWanderSubtitleDeleteTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long id = JsonMapUtils.parseObject(params, "subtitleId", JsonMapUtils.JsonInnerType.TYPE_LONG);
        subtitleMapper.delete(id);
        subtitleManager.removeWanderSubtitle(id);
    }

    /**
     * 添加游走字幕
     */
    @GmHandler(key = "/osee/wander_subtitle/add")
    public void doWanderSubtitleAddTask(Map<String, Object> params, CommonResponse response) throws Exception {
        WanderSubtitleEntity subtitle = new WanderSubtitleEntity();
        subtitle.setContent(JsonMapUtils.parseObject(params, "content", JsonMapUtils.JsonInnerType.TYPE_STRING));
        subtitle.setIntervalTime(JsonMapUtils.parseObject(params, "intervalTime", JsonMapUtils.JsonInnerType.TYPE_INT));
        subtitle.setStartTime(JsonMapUtils.parseObject(params, "effectiveTime", JsonMapUtils.JsonInnerType.TYPE_DATE));
        subtitle.setEndTime(JsonMapUtils.parseObject(params, "failureTime", JsonMapUtils.JsonInnerType.TYPE_DATE));
        subtitleMapper.save(subtitle);

        if (subtitle.getId() > 0) {
            subtitleManager.addWanderSubtitle(subtitle);
        } else {
            response.setSuccess(false);
        }
    }

    /**
     * 获取公告列表
     */
    @GmHandler(key = "/osee/notice/list")
    public void doNoticeListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        List<OseeNoticeEntity> notices = noticeMapper.getAll();
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, Object>> noticeItemMap = JsonMapUtils.objectsToMaps(notices);
        long nowTime = System.currentTimeMillis();
        for (int i = 0; i < notices.size(); i++) {
            int state = 0;
            OseeNoticeEntity notice = notices.get(i);
            if (notice.getStartTime().getTime() > nowTime || notice.getEndTime().getTime() < nowTime) {
                state = 1;
            }
            noticeItemMap.get(i).put("state", state);
        }

        resultMap.put("list", noticeItemMap);
        response.setData(resultMap);
    }

    /**
     * 根据ID查找公告
     */
    @GmHandler(key = "/osee/notice/query")
    public void doNoticeQueryTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long id = JsonMapUtils.parseObject(params, "noticeId", JsonInnerType.TYPE_LONG);
        OseeNoticeEntity notice = noticeMapper.getById(id);
        if (notice == null) {
            response.setSuccess(false);
            response.setErrMsg("公告不存在");
            return;
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("id", notice.getId());
        resultMap.put("title", notice.getTitle());
        resultMap.put("content", notice.getContent());
//        resultMap.put("startTime", notice.getStartTime().getTime());
//        resultMap.put("endTime", notice.getEndTime().getTime());
        response.setData(resultMap);
    }

    /**
     * 更新公告
     */
    @GmHandler(key = "/osee/notice/update")
    public void doNoticeUpdateTask(Map<String, Object> params, CommonResponse response) throws Exception {
        OseeNoticeEntity notice = noticeMapper.getById((long) (double) params.get("id"));
        if (notice == null) {
            response.setSuccess(false);
            response.setErrMsg("公告不存在");
            return;
        }
        notice.setTitle((String) params.get("title"));
        notice.setContent((String) params.get("content"));
        notice.setStartTime(JsonMapUtils.parseObject(params, "startTime", JsonMapUtils.JsonInnerType.TYPE_DATE));
        notice.setEndTime(JsonMapUtils.parseObject(params, "endTime", JsonMapUtils.JsonInnerType.TYPE_DATE));
        noticeMapper.update(notice);
        commonLobbyManager.refreshNotice();
    }

    /**
     * 添加公告
     */
    @GmHandler(key = "/osee/notice/add")
    public void doNoticeAddTask(Map<String, Object> params, CommonResponse response) throws Exception {
        OseeNoticeEntity notice = new OseeNoticeEntity();
        notice.setIndex(Integer.MAX_VALUE);
        notice.setTitle(JsonMapUtils.parseObject(params, "title", JsonInnerType.TYPE_STRING));
        notice.setContent(JsonMapUtils.parseObject(params, "content", JsonInnerType.TYPE_STRING));
        notice.setStartTime(JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_DATE));
        notice.setEndTime(JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_DATE));

        noticeMapper.save(notice);
        if (notice.getId() <= 0) {
            response.setSuccess(false);
            return;
        }

        commonLobbyManager.refreshNotice();
    }

    /**
     * 删除公告
     */
    @GmHandler(key = "/osee/notice/delete")
    public void doNoticeDeleteTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long id = JsonMapUtils.parseObject(params, "id", JsonInnerType.TYPE_LONG);
        noticeMapper.deleteById(id);
        commonLobbyManager.refreshNotice();
    }

    /**
     * 交换公告顺序
     */
    @GmHandler(key = "/osee/notice/change")
    public void doNoticeChangeTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long id = JsonMapUtils.parseObject(params, "id", JsonInnerType.TYPE_LONG);
        int type = JsonMapUtils.parseObject(params, "type", JsonInnerType.TYPE_INT);

        if (!commonLobbyManager.changeNotice(id, type)) {
            response.setSuccess(false);
            response.setErrMsg("该项无法继续移动");
        }
    }

    /**
     * 获取游戏版本信息
     */
    @GmHandler(key = "/osee/game_version")
    public void doGameVersionTask(Map<String, Object> params, CommonResponse response) {
        Map<String, Object> resultMap = new HashMap<>();
        String version = lobbyManager.getServerVersion();
        resultMap.put("version", version);
        response.setData(resultMap);
    }

    /**
     * 修改游戏版本信息
     */
    @GmHandler(key = "/osee/game_version/update")
    public void doGameVersionUpdateTask(Map<String, Object> params, CommonResponse response) throws Exception {
        String version = JsonMapUtils.parseObject(params, "version", JsonInnerType.TYPE_STRING);
        lobbyManager.setServerVersion(version);
    }

    /**
     * 获取cdk列表任务
     */
    @GmHandler(key = "/osee/cdk/list")
    public void doCdkListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "cdkey":
                    String cdkey = JsonMapUtils.parseObject(params, "cdkey", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(cdkey)) {
                        condBuilder.append(" AND cdkey = '").append(cdkey).append("'");
                    }
                    break;
                case "typeId":
                    long typeId = JsonMapUtils.parseObject(params, "typeId", JsonInnerType.TYPE_LONG);
                    if (typeId > 0) {
                        condBuilder.append(" AND type_id = ").append(typeId);
                    }
                    break;
                case "used":
                    int used = JsonMapUtils.parseObject(params, "used", JsonInnerType.TYPE_INT);
                    if (used == 1) {
                        condBuilder.append(" AND user_id > 0");
                    } else if (used == 2) {
                        condBuilder.append(" AND user_id = 0");
                    }
                    break;
                case "page":
                case "pageSize":
                    if (pageBuilder.length() <= 0) {
                        // 解析分页数据
                        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
                        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);
                        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(",").append(pageSize);
                    }
                    break;
            }
        }

        List<GmCdkInfo> cdkInfos = gmCommonMapper.getCdkInfoList(condBuilder.toString(), pageBuilder.toString());
        int total = gmCommonMapper.getCdkInfoCount(condBuilder.toString());

        List<Map<String, Object>> resultList = JsonMapUtils.objectsToMaps(cdkInfos);
        for (Map<String, Object> resultObj : resultList) {
            @SuppressWarnings("unchecked")
            Map<String, Object>[] rewards = GsonManager.gson.fromJson(resultObj.get("rewards").toString(), Map[].class);
            StringBuilder rewardsBuilder = new StringBuilder();
            for (Map<String, Object> reward : rewards) {
                String name = ItemId.getItemIdById((int) Double.parseDouble(reward.get("itemId").toString())).getInfo();
                rewardsBuilder
                        .append(name)
                        .append("*")
                        .append((int) Double.parseDouble(reward.get("count").toString()))
                        .append(" ");
            }

            resultObj.put("rewards", rewardsBuilder.toString());
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalNum", total);
        resultMap.put("list", resultList);
        response.setData(resultMap);
    }

    /**
     * 添加cdk任务
     */
    @GmHandler(key = "/osee/cdk/add")
    public void doCdkAddTask(Map<String, Object> params, CommonResponse response) throws Exception {
        String rewards = JsonMapUtils.parseObject(params, "rewards", JsonInnerType.TYPE_STRING);
        long typeId = JsonMapUtils.parseObject(params, "typeId", JsonInnerType.TYPE_LONG);
        int count = JsonMapUtils.parseObject(params, "count", JsonInnerType.TYPE_INT);
        if (!cdkManager.createCdk(typeId, count, rewards)) {
            response.setSuccess(false);
            response.setErrCode("ERROR_UNKNOWN");
            response.setErrMsg("未知错误");
        }
    }

    /**
     * 删除cdk任务
     */
    @GmHandler(key = "/osee/cdk/delete")
    public void doCdkDeleteTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long typeId = JsonMapUtils.parseObject(params, "typeId", JsonInnerType.TYPE_LONG);
        cdkManager.deleteCdk(typeId);
    }

    /**
     * 获取cdk类型列表任务
     */
    @GmHandler(key = "/osee/cdk_type/list")
    public void doCdkTypeListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        List<OseeCdkTypeEntity> entities = cdkManager.getCdkTypes();
        List<GmCdkTypeInfo> typeInfos = new LinkedList<>();
        for (OseeCdkTypeEntity entity : entities) {
            GmCdkTypeInfo typeInfo = new GmCdkTypeInfo();
            typeInfo.setId(entity.getId());
            typeInfo.setName(entity.getName());
            typeInfos.add(typeInfo);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", typeInfos);
        response.setData(resultMap);
    }

    /**
     * 添加cdk类型任务
     */
    @GmHandler(key = "/osee/cdk_type/add")
    public void doCdkTypeAddTask(Map<String, Object> params, CommonResponse response) throws Exception {
        String name = JsonMapUtils.parseObject(params, "name", JsonInnerType.TYPE_STRING);
        String errMsg = cdkManager.createCdkType(name);
        if (!StringUtils.isEmpty(errMsg)) {
            response.setSuccess(false);
            response.setErrMsg(errMsg);
        }
    }

    /**
     * 添加支付订单
     */
    @GmHandler(key = "/osee/pay_order/add")
    public void doPayOrderAddTask(Map<String, Object> params, CommonResponse response) {
        String orderNum = params.get("orderNum").toString();
        OseeRechargeLogEntity log = rechargeLogMapper.get(orderNum);
        if (log == null) {
            log = new OseeRechargeLogEntity();
            log.setOrderNum(orderNum);
            log.setUserId((long) (double) params.get("playerId"));
            log.setPayMoney((long) (double) params.get("payMoney"));
            log.setShopName(params.get("shopName").toString());
            log.setShopType((int) (double) params.get("shopType"));
            log.setCount((int) (double) params.get("shopCount"));
            log.setCreator("第三方");
            log.setRechargeType((int) (double) params.get("rechargeType"));
            ServerUser user = UserContainer.getUserById(log.getUserId());
            log.setNickname(user.getNickname());

            rechargeLogMapper.save(log);
        } else {
            response.setSuccess(false);
            response.setErrMsg("订单号已存在");
        }
    }

    /**
     * 修改支付订单状态
     */
    @GmHandler(key = "/osee/pay_order/update")
    public void doPayOrderUpdateTask(Map<String, Object> params, CommonResponse response) {
        String orderNum = params.get("orderNum").toString();
        int orderState = (int) (double) params.get("orderState");
        OseeRechargeLogEntity log = rechargeLogMapper.get(orderNum);

        if (log == null) {
            response.setSuccess(false);
            response.setErrMsg("订单号不存在");
        } else if (log.getOrderState() != 0) {
            if (log.getOrderState() != orderState) {
                response.setSuccess(false);
                response.setErrMsg("订单当前状态不可编辑:" + log.getOrderState());
            }
        } else {
            // 支付的金额：分
            long payMoney = (long) (double) params.get("payMoney");

            if (log.getPayMoney() != payMoney) {
                response.setSuccess(false);
                response.setErrMsg("订单金额不匹配");
                return;
            }

            if (orderState == 1) { // 支付成功
                ServerUser user = UserContainer.getUserById(log.getUserId());

                OseePlayerEntity entity = PlayerManager.getPlayerEntity(user);

                // VIP5及以上充值金币会额外赠送10%
                if (log.getShopType() == ItemId.MONEY.getId() && entity.getVipLevel() >= 5) {
                    log.setCount((int) (log.getCount() + log.getCount() * 0.1));
                }

                // 记录玩家充值的金钱数
                entity.setRechargeMoney(entity.getRechargeMoney() + log.getPayMoney() / 100);
                // 根据充值的金钱计算玩家的vip等级
                entity.setVipLevel(PlayerManager.getPlayerVipLevel(entity));

                // 给玩家加对应购买的物品
                PlayerManager.addItem(user, log.getShopType(), log.getCount(), ItemChangeReason.THIRD_PARTY_RECHARGE, true);

                // 如果是购买的月卡
                if (log.getShopType() == ItemId.MONTH_CARD.getId()) {
                    // 赠送购买礼包 30颗钻石、10万金币、自动开炮30天
                    List<ItemData> itemDataList = Arrays.asList(
                            new ItemData(ItemId.DIAMOND.getId(), 30),
                            new ItemData(ItemId.MONEY.getId(), 100000)
                    );
                    PlayerManager.addItems(user, itemDataList, null, true);

                    if (user.isOnline()) { // 通知给用户
                        // 发送礼包赠送响应
                        OseeLobbyMessage.BuyMonthCardRewardsResponse.Builder builder = OseeLobbyMessage.BuyMonthCardRewardsResponse.newBuilder();
                        for (ItemData itemData : itemDataList) {
                            builder.addRewards(OseePublicData.ItemDataProto.newBuilder()
                                    .setItemId(itemData.getItemId())
                                    .setItemNum(itemData.getCount())
                                    .build());
                        }
                        // 放入自动开炮 自定义自动开炮物品的ID为100
                        builder.addRewards(OseePublicData.ItemDataProto.newBuilder()
                                .setItemId(100)
                                .setItemNum(30)
                                .build());
                        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_BUY_MONTH_CARD_REWARDS_RESPONSE_VALUE, builder, user);
                    }
                    // 发送月卡每日奖励
                    commonLobbyManager.sendDailyMonthCardRewards(user);
                }

                if (payMoney / 100 >= 6) {
                    // 判断6元首充是否有效
                    String key = String.format(CommonLobbyManager.FIRST_CHARGE_KEY_NAMESPACE, user.getId());
                    String value = RedisHelper.get(key);
                    if (StringUtils.isEmpty(value)) { // 还没有首充
                        // 首充赠送的大礼包 12颗钻石、2万金币、30张锁定卡、20张冰冻卡、20张急速卡、10张暴击卡、3天月卡体验
                        List<ItemData> itemDataList = Arrays.asList(
                                new ItemData(ItemId.DIAMOND.getId(), 12),
                                new ItemData(ItemId.MONEY.getId(), 20000),
                                new ItemData(ItemId.SKILL_LOCK.getId(), 30),
                                new ItemData(ItemId.SKILL_FROZEN.getId(), 20),
                                new ItemData(ItemId.SKILL_FAST.getId(), 20),
                                new ItemData(ItemId.SKILL_CRIT.getId(), 10),
                                new ItemData(ItemId.MONTH_CARD.getId(), 3)
                        );
                        PlayerManager.addItems(user, itemDataList, null, true);
                        // 保存首充记录
                        RedisHelper.set(key, "￥" + payMoney / 100);

                        if (user.isOnline()) { // 通知给用户
                            // 发送礼包赠送响应
                            OseeLobbyMessage.FirstChargeRewardsResponse.Builder builder = OseeLobbyMessage.FirstChargeRewardsResponse.newBuilder();
                            for (ItemData itemData : itemDataList) {
                                builder.addRewards(OseePublicData.ItemDataProto.newBuilder()
                                        .setItemId(itemData.getItemId())
                                        .setItemNum(itemData.getCount())
                                        .build());
                            }
                            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FIRST_CHARGE_REWARDS_RESPONSE_VALUE, builder, user);
                        }
                    }
                }
                rechargeLogMapper.updateOrderState(orderState, log.getId());

                try {
                    // 计算玩家是否有代理，然后要计算代理佣金，被禁用的代理无法获取返利
                    long userId = user.getId();
                    AgentEntity agentEntity = agentMapper.getByPlayerId(userId);
                    if (agentEntity != null) {
                        // 现金要转换为金币去计算佣金 1元=2万金币
                        long payMoneyYuan = payMoney / 100; // 换算成元
                        Integer agentLevel = agentEntity.getAgentLevel();
                        if (agentLevel == 2) { // 二级代理只给一级代理返利
                            Long firAgentPlayerId = agentEntity.getAgentPlayerId();
                            // 一级代理
                            AgentEntity firAgent = agentMapper.getByPlayerId(firAgentPlayerId);
                            if (firAgent == null) {
                                return;
                            }
                            if (firAgent.getState() != 1) { // 代理权限未被禁用
                                Double firstCommissionRate = firAgent.getFirstCommissionRate();
                                // 佣金
                                Double commission = payMoneyYuan * firstCommissionRate / 100;

                                AgentCommissionEntity firCommissionEntity = new AgentCommissionEntity();
                                firCommissionEntity.setPlayerId(userId);
                                firCommissionEntity.setPlayerName(user.getNickname());
                                firCommissionEntity.setMoney(payMoneyYuan);
                                firCommissionEntity.setAgentPlayerId(firAgentPlayerId);
                                firCommissionEntity.setAgentPlayerName(firAgent.getPlayerName());
                                firCommissionEntity.setCommission(commission);
                                firCommissionEntity.setCommissionRate(firstCommissionRate);
                                agentCommissionMapper.save(firCommissionEntity);
                                saveCommissionInfo(user, firAgentPlayerId, null, log.getShopName(), commission, null, (double) payMoneyYuan);
                                // 更新代理的佣金总收入
                                firAgent.setTotalCommission(firAgent.getTotalCommission() + commission);
                                agentMapper.update(firAgent);
                            }
                        } else if (agentLevel == 3) { // 会员玩家要给二级、一级返利
                            Long secAgentPlayerId = agentEntity.getAgentPlayerId();
                            // 二级代理
                            AgentEntity secAgent = agentMapper.getByPlayerId(secAgentPlayerId);
                            if (secAgent == null) {
                                return;
                            }
                            Long firAgentPlayerId = secAgent.getAgentPlayerId();
                            // 一级代理
                            AgentEntity firAgent = agentMapper.getByPlayerId(firAgentPlayerId);
                            if (firAgent == null) {
                                return;
                            }
                            Double firstCommissionRate = firAgent.getFirstCommissionRate();
                            Double secondCommissionRate = (secAgent.getSecondCommissionRate() == null ? firAgent : secAgent).getSecondCommissionRate();
                            // 一级佣金
                            Double firCommission = payMoneyYuan * firstCommissionRate / 100;
                            // 二级佣金
                            Double secCommission = payMoneyYuan * secondCommissionRate / 100;
                            if (secAgent.getState() != 1) { // 二级代理权限未被禁用
                                AgentCommissionEntity secCommissionEntity = new AgentCommissionEntity();
                                secCommissionEntity.setPlayerId(userId);
                                secCommissionEntity.setPlayerName(user.getNickname());
                                secCommissionEntity.setMoney(payMoneyYuan);
                                secCommissionEntity.setAgentPlayerId(secAgentPlayerId);
                                secCommissionEntity.setAgentPlayerName(secAgent.getPlayerName());
                                secCommissionEntity.setCommission(secCommission);
                                secCommissionEntity.setCommissionRate(secondCommissionRate);
                                agentCommissionMapper.save(secCommissionEntity);
                                // 更新代理的佣金总收入
                                secAgent.setTotalCommission(secAgent.getTotalCommission() + secCommission);
                                agentMapper.update(secAgent);
                            }
                            if (firAgent.getState() != 1) { // 一级代理权限未被禁用
                                AgentCommissionEntity firCommissionEntity = new AgentCommissionEntity();
                                firCommissionEntity.setPlayerId(userId);
                                firCommissionEntity.setPlayerName(user.getNickname());
                                firCommissionEntity.setMoney(payMoneyYuan);
                                firCommissionEntity.setAgentPlayerId(firAgentPlayerId);
                                firCommissionEntity.setAgentPlayerName(firAgent.getPlayerName());
                                firCommissionEntity.setCommission(firCommission);
                                firCommissionEntity.setCommissionRate(firstCommissionRate);
                                agentCommissionMapper.save(firCommissionEntity);
                                // 更新代理的佣金总收入
                                firAgent.setTotalCommission(firAgent.getTotalCommission() + firCommission);
                                agentMapper.update(firAgent);
                            }
                            saveCommissionInfo(user, firAgentPlayerId, secAgentPlayerId, log.getShopName(), firCommission, secCommission, (double) payMoneyYuan);
                        }
                    }
                } catch (Exception e) {
                    logger.error("玩家重置处理代理佣金出错");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 保存代理佣金记录
     */
    private void saveCommissionInfo(ServerUser user, Long agentPlayerId, Long secAgentPlayerId, String shopName, Double first, Double second, Double money) {
        AgentCommissionInfoEntity log = new AgentCommissionInfoEntity();
        log.setPlayerId(user.getId());
        log.setPlayerName(user.getNickname());
        log.setChannelId(agentPlayerId);
        log.setPromoterId(secAgentPlayerId);
        log.setShopName(shopName);
        log.setCommission(first);
        log.setSecCommission(second);
        log.setMoney(money);
        agentCommissionInfoMapper.save(log);
    }

    /**
     * 客服信息
     */
    @GmHandler(key = "/osee/support")
    public void doSupportTask(Map<String, Object> params, CommonResponse response) {
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) response.getData();
        resultMap.put("wechat", commonLobbyManager.getSupportWechat());
        resultMap.put("qrcode", commonLobbyManager.getSupportQRCode());
    }

    /**
     * 设置客服信息
     */
    @GmHandler(key = "/osee/support/update")
    public void doSupportUpdateTask(Map<String, Object> params, CommonResponse response) {
        commonLobbyManager.setSupportWechat(params.get("wechat").toString());
        commonLobbyManager.setSupportQRCode(params.get("qrcode").toString());
    }

    /**
     * 查询五子棋战绩
     */
    @GmHandler(key = "/osee/game/gobang/record")
    public void doGameGobangRecord(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();
        StringBuilder condBuilderWin = new StringBuilder("WHERE 1=1");//赢取总金币统计
        StringBuilder condBuilderLose = new StringBuilder("WHERE 1=1");//输掉总金币统计
        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);
        long playerId = 0;
        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                        condBuilderWin.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                        condBuilderLose.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                        condBuilderWin.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                        condBuilderLose.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "playerId":
                    playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND (log.winner_id = " + playerId + " OR log.loser_id = " + playerId + ")");
                        condBuilderWin.append(" AND (log.winner_id = " + playerId + ")");
                        condBuilderLose.append(" AND (log.loser_id = " + playerId + ")");
                    }
                    break;
            }
        }

        List<OseeGobangRecordLogEntity> logs = gobangRecordMapper.getLogList(condBuilder.toString(),
                pageBuilder.toString());
        int count = gobangRecordMapper.getLogCount(condBuilder.toString());
        //按查询条件统计
        long totalWin = gobangRecordMapper.getTotalWin(condBuilderWin.toString());
        long totalLose = gobangRecordMapper.getTotalLose(condBuilderLose.toString());
        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeGobangRecordLogEntity log : logs) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("money", log.getMoney());
            dataMap.put("winnerId", log.getWinnerId());
            dataMap.put("winnerNickname", log.getWinnerNickname());
            dataMap.put("winnerBeforeMoney", log.getWinnerBeforeMoney());
            dataMap.put("winnerAfterMoney", log.getWinnerAfterMoney());
            dataMap.put("loserId", log.getLoserId());
            dataMap.put("loserNickname", log.getLoserNickname());
            dataMap.put("loserBeforeMoney", log.getLoserBeforeMoney());
            dataMap.put("loserAfterMoney", log.getLoserAfterMoney());
            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalNum", count);
        result.put("list", dataList);
        result.put("totalWin", totalWin);
        result.put("totalLose", totalLose);
        result.put("totalMoney", totalWin - totalLose);
        response.setData(result);
    }

    /**
     * 查询水果拉霸战绩
     */
    @GmHandler(key = "/osee/game/fruit/record")
    public void doGameFruitRecord(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND (log.playerId = " + playerId + ")");
                    }
                    break;
            }
        }

        List<OseeFruitRecordLogEntity> logs = fruitRecordMapper.getLogList(condBuilder.toString(),
                pageBuilder.toString());
        int count = fruitRecordMapper.getLogCount(condBuilder.toString());
        //按查询条件统计
        Map<String, Object> statistic = fruitRecordMapper.getStatstic(condBuilder.toString());
        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeFruitRecordLogEntity log : logs) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("info", log.getInfo());//中奖详情
            dataMap.put("totalWin", log.getTotalWin());//中奖总金额
            dataMap.put("lineNum", log.getLineNum());//下注条数
            dataMap.put("cost", log.getCost());//下注金额
            dataMap.put("money", log.getMoney());//账户金额变动
            dataMap.put("playerId", log.getPlayerId());
            dataMap.put("nickname", log.getNickname());
            dataMap.put("playBeforeMoney", log.getPlayBeforeMoney());
            dataMap.put("playAfterMoney", log.getPlayAfterMoney());
            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalNum", count);
        result.put("list", dataList);
        result.put("totalMoney", statistic.get("totalMoney"));
        result.put("totalCost", statistic.get("totalCost"));
        result.put("AllTotalWin", statistic.get("AllTotalWin"));
        response.setData(result);
    }

    /**
     * 查询拼十战绩
     */
    @GmHandler(key = "/osee/game/fightten/record")
    public void doGameFighttenRecord(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND (log.playerId = " + playerId + ")");
                    }
                    break;
            }
        }

        List<OseeFighttenRecordLogEntity> logs = fighttenRecordMapper.getLogList(condBuilder.toString(),
                pageBuilder.toString());
        int count = fighttenRecordMapper.getLogCount(condBuilder.toString());
        //按查询条件统计
        Map<String, Object> statistic = fighttenRecordMapper.getStatstic(condBuilder.toString());
        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeFighttenRecordLogEntity log : logs) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("money", log.getMoney());
            dataMap.put("playerId", log.getPlayerId());
            dataMap.put("nickname", log.getNickname());
            dataMap.put("input", log.getInput());
            dataMap.put("rate", log.getRate());
            dataMap.put("cardType", log.getCardType());
            dataMap.put("playBeforeMoney", log.getPlayBeforeMoney());
            dataMap.put("playAfterMoney", log.getPlayAfterMoney());
            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalNum", count);
        result.put("totalMoney", statistic.get("totalMoney"));//按条件统计账户金币变动总额
        result.put("list", dataList);
        response.setData(result);
    }

    /**
     * 二八杠玩家输赢明细记录查询
     */
    @GmHandler(key = "/osee/game/twoeight/record")
    public void doTwoEightRecord(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND (log.playerId = ").append(playerId).append(")");
                    }
                    break;
            }
        }

        List<OseeFighttenRecordLogEntity> logs = twoEightRecordMapper.getLogList(condBuilder.toString(),
                pageBuilder.toString());
        int count = twoEightRecordMapper.getLogCount(condBuilder.toString());
        //按查询条件统计
        Map<String, Object> statistic = twoEightRecordMapper.getStatstic(condBuilder.toString());
        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeFighttenRecordLogEntity log : logs) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("money", log.getMoney());
            dataMap.put("playerId", log.getPlayerId());
            dataMap.put("nickname", log.getNickname());
            dataMap.put("input", log.getInput());
            dataMap.put("cardType", log.getCardType());
            dataMap.put("playBeforeMoney", log.getPlayBeforeMoney());
            dataMap.put("playAfterMoney", log.getPlayAfterMoney());
            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalNum", count);
        result.put("totalMoney", statistic.get("totalMoney"));//按条件统计账户金币变动总额
        result.put("list", dataList);
        response.setData(result);
    }

    /**
     * 查询捕鱼战绩
     */
    @GmHandler(key = "/osee/game/fishing/record")
    public void doGameFishingRecord(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND log.player_id = ").append(playerId);
                    }
                    break;
                case "roomIndex":
                    int roomIndex = JsonMapUtils.parseObject(params, "roomIndex", JsonInnerType.TYPE_INT);
                    if (roomIndex > 0) {
                        condBuilder.append(" AND log.room_index = ").append(roomIndex);
                    }
                    break;
            }
        }

        List<OseeFishingRecordLogEntity> logs = fishingRecordMapper.getLogList(condBuilder.toString(), pageBuilder.toString());
        int count = fishingRecordMapper.getLogCount(condBuilder.toString());
        // 获取总共赢取的金币数量
        long totalWin = fishingRecordMapper.getSum("win_money", condBuilder.toString());

        List<Map<String, Object>> dataList = new LinkedList<>();
        for (OseeFishingRecordLogEntity log : logs) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("createTime", log.getCreateTime());
            dataMap.put("playerId", log.getPlayerId());
            dataMap.put("nickname", UserContainer.getUserById(log.getPlayerId()).getNickname());
            dataMap.put("roomIndex", log.getRoomIndex());
            dataMap.put("spendMoney", log.getSpendMoney());
            dataMap.put("winMoney", log.getWinMoney());
            dataMap.put("dropTorpedo", String.format("青*%d 银*%d 金*%d",
                    log.getDropBronzeTorpedoNum(), log.getDropSilverTorpedoNum(), log.getDropGoldTorpedoNum()));
            dataList.add(dataMap);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalNum", count);
        result.put("list", dataList);
        result.put("totalWin", totalWin);
        response.setData(result);
    }

    /**
     * 获取游戏礼物赠送记录
     */
    @GmHandler(key = "/ttmy/game/give/record")
    public void doGiveGiftRecordTask(Map<String, Object> params, CommonResponse response) throws Exception {
        // 条件语句构建
        StringBuilder condBuilder = new StringBuilder(" where 1=1");

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
//                case "fromId":
//                    long fromId = JsonMapUtils.parseObject(params, "fromId", JsonInnerType.TYPE_LONG);
//                    if (fromId > 0) {
//                        condBuilder.append(" AND from_id = ").append(fromId);
//                    }
//                    break;
//                case "fromName":
//                    String fromName = JsonMapUtils.parseObject(params, "fromName", JsonInnerType.TYPE_STRING);
//                    if (!StringUtils.isEmpty(fromName)) {
//                        condBuilder.append(" AND from_name = ").append(fromName);
//                    }
//                    break;
//                case "toId":
//                    long toId = JsonMapUtils.parseObject(params, "toId", JsonInnerType.TYPE_LONG);
//                    if (toId > 0) {
//                        condBuilder.append(" AND to_id = ").append(toId);
//                    }
//                    break;
//                case "toName":
//                    String toName = JsonMapUtils.parseObject(params, "toName", JsonInnerType.TYPE_STRING);
//                    if (!StringUtils.isEmpty(toName)) {
//                        condBuilder.append(" AND to_name = ").append(toName);
//                    }
//                    break;
                case "playerId": // 查询玩家送出的和收到的
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND from_id = ").append(playerId).append(" OR to_id = ").append(playerId);
                    }
                    break;
            }
        }
        condBuilder.append(" order by create_time desc");

        // 数据总条数
        long count = giftLogMapper.getCount(condBuilder.toString());
        // 礼物总数
        long giftTotalNum = giftLogMapper.getGiftTotalNum(condBuilder.toString());

        // 解析分页数据
        int pageNo = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);
        condBuilder.append(" limit ").append((pageNo - 1) * pageSize).append(",").append(pageSize);

        // 开始获取数据
        List<GiveGiftLogEntity> list = giftLogMapper.getList(condBuilder.toString());

        Map<String, Object> result = new HashMap<>();
        result.put("totalNum", count);
        result.put("list", list);
        result.put("giftTotalNum", giftTotalNum);
        response.setData(result);
    }

    /**
     * 代理绑定
     */
    @GmHandler(key = "/ttmy/agent/bind")
    public void doAgentBindTask(Map<String, Object> params, CommonResponse response) throws UnsupportedEncodingException {
        long playerId = Long.parseLong((String) params.get("playerId"));
        String wxUrl = agentManager.agentBind(playerId);
        response.setData(wxUrl);
    }

    /**
     * 绑定代理微信回调处理
     */
    @GmHandler(key = "/ttmy/agent/bind/callback")
    public void doAgentBindCallbackTask(Map<String, Object> params, CommonResponse response) {
        String code = (String) params.get("code");
        String state = (String) params.get("state");
        CommonResponse commonResponse = agentManager.agentBindCallback(code, state);
        if (commonResponse.getSuccess()) {
            response.setData(commonResponse.getData());
        } else {
            response.setSuccess(false);
            response.setErrCode(commonResponse.getErrCode());
            response.setErrMsg(commonResponse.getErrMsg());
        }
    }

    /**
     * 获取玩家绑定的上级代理信息
     */
    @GmHandler(key = "/ttmy/player/agent/info")
    public void doAgentInfoTask(Map<String, Object> params, CommonResponse response) {
        long playerId = (long) (double) params.get("playerId");

        Map<String, Object> resultMap = new HashMap<>();
        String firstInfo = "无"; // 一级代理信息
        String secondInfo = "无"; // 二级代理信息

        AgentEntity agentEntity = agentManager.getAgentInfoByPlayerId(playerId);
        if (agentEntity != null) {
            if (agentEntity.getAgentLevel() == 2) { // 二级代理有上级一级
                if (agentEntity.getAgentPlayerId() != null) {
                    AgentEntity firstAgent = agentManager.getAgentInfoByPlayerId(agentEntity.getAgentPlayerId());
                    firstInfo = firstAgent.getPlayerName() + "(ID:" + firstAgent.getPlayerId() + ")";
                }
            } else if (agentEntity.getAgentLevel() == 3) { // 会员上级有两级代理
                if (agentEntity.getAgentPlayerId() != null) {
                    AgentEntity secondAgent = agentManager.getAgentInfoByPlayerId(agentEntity.getAgentPlayerId());
                    AgentEntity firstAgent = agentManager.getAgentInfoByPlayerId(secondAgent.getAgentPlayerId());
                    firstInfo = firstAgent.getPlayerName() + "(ID:" + firstAgent.getPlayerId() + ")";
                    secondInfo = secondAgent.getPlayerName() + "(ID:" + secondAgent.getPlayerId() + ")";
                }
            }
        }
        resultMap.put("first", firstInfo);
        resultMap.put("second", secondInfo);
        response.setData(resultMap);
    }

    /**
     * 获取代理玩家列表
     */
    @GmHandler(key = "/ttmy/agent/list")
    public void doAgentListTask(Map<String, Object> params, CommonResponse response) throws Exception {
        List<Map<String, Object>> agentList = new LinkedList<>();

        // 查询销售金额和佣金的查询条件
        StringBuilder queryBuilder = new StringBuilder();
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        queryBuilder.append(" AND create_time >= '").append(DATE_FORMATER.format(startTime)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        queryBuilder.append(" AND create_time <= '").append(DATE_FORMATER.format(endTime)).append("'");
                    }
                    break;
                case "playerId":
                    long id = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (id > 0) {
                        condBuilder.append(" AND player_id = ").append(id);
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND player_name = '").append(nickname).append("'");
                    }
                    break;
                case "state":
                    long state = JsonMapUtils.parseObject(params, "state", JsonInnerType.TYPE_LONG);
                    if (state > 0) {
                        condBuilder.append(" AND state = ").append(state - 1);
                    }
                    break;
                case "level":
                    int level = JsonMapUtils.parseObject(params, "level", JsonInnerType.TYPE_INT);
                    if (level > 0) {
                        condBuilder.append(" AND agent_level = ").append(level);
                    }
                    break;
            }
        }
        // 添加只查询代理的条件，不选择普通会员
        condBuilder.append(" AND agent_level < 3");
        List<AgentEntity> agentEntityList = agentMapper.getAgentList(condBuilder.toString(), pageBuilder.toString());
        int totalNum = agentMapper.getAgentCount(condBuilder.toString());

        for (AgentEntity agentEntity : agentEntityList) {
            Map<String, Object> agentItem = new HashMap<>();
            Integer agentLevel = agentEntity.getAgentLevel();
            agentItem.put("level", agentLevel);
            agentItem.put("playerId", agentEntity.getPlayerId());
            agentItem.put("nickname", agentEntity.getPlayerName());

            // 代理收到的所有销售金
            long money = agentCommissionMapper.getMoneyByAgentId(agentEntity.getPlayerId(), queryBuilder.toString());
            // 代理收到的佣金
            long commission = agentCommissionMapper.getCommissionByAgentId(agentEntity.getPlayerId(), queryBuilder.toString());
            agentItem.put("money", money);
            agentItem.put("commission", commission);

            // 一级代理下团队人数就是二级代理人数
            if (agentLevel == 1) {
                agentItem.put("teamPersonNum", agentMapper.getAgentNextLevelCount(agentEntity.getPlayerId()));
            } else {
                agentItem.put("teamPersonNum", 0);
            }
            // 二级代理下会员人数
            if (agentLevel == 2) {
                agentItem.put("memberPersonNum", agentMapper.getAgentNextLevelCount(agentEntity.getPlayerId()));
            } else {
                // 一级代理就显示他下面所有二级代理招募的会员数量
                long personNum = 0;
                // 查询出所有二级代理
                List<AgentEntity> secAgentList = agentMapper.getByAgentPlayerId(agentEntity.getPlayerId());
                for (AgentEntity secAgent : secAgentList) {
                    personNum += agentMapper.getAgentNextLevelCount(secAgent.getPlayerId());
                }
                agentItem.put("memberPersonNum", personNum);
            }
            agentItem.put("createTime", agentEntity.getCreateTime());
            agentItem.put("state", agentEntity.getState() + 1);
            agentList.add(agentItem);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalNum", totalNum);
        resultMap.put("list", agentList);
        response.setData(resultMap);
    }

    /**
     * 代理玩家冻结/解冻
     */
    @GmHandler(key = "/ttmy/agent/frozen")
    public void doAgentFrozenTask(Map<String, Object> params, CommonResponse response) {
        List<Double> idList = (List<Double>) params.get("idList");
        // 操作类别 1-冻结 0-解冻
        int option = (int) (double) params.get("option");

        for (Double id : idList) {
            long playerId = Math.round(id);
            AgentEntity agentEntity = agentMapper.getByPlayerId(playerId);
            agentEntity.setState(option == 0 ? 0 : 1);
            agentMapper.update(agentEntity);
        }
    }

    /**
     * 获取代理贡献明细列表
     */
    @GmHandler(key = "/ttmy/agent/commission")
    public void doAgentCommissionTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        // 代理玩家ID
        long agentId = 0;
        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "agentId":
                    agentId = JsonMapUtils.parseObject(params, "agentId", JsonInnerType.TYPE_LONG);
                    if (agentId > 0) {
                        condBuilder.append(" AND agent_player_id = ").append(agentId);
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND player_id = ").append(playerId);
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND player_name = '").append(nickname).append("'");
                    }
                    break;
            }
        }
        // 查找贡献明细数据
        List<AgentCommissionEntity> commissionEntityList = agentCommissionMapper.getCommissionList(condBuilder.toString(), pageBuilder.toString());
        // 指定代理收到的贡献数据总条数
        int commissionCount = agentCommissionMapper.getCommissionCount(condBuilder.toString());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalNum", commissionCount);
        resultMap.put("list", commissionEntityList);
        response.setData(resultMap);
    }

    /**
     * 代理佣金兑换明细
     */
    @GmHandler(key = "/ttmy/agent/commission/exchange")
    public void doCommissionExchangeLogTask(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("where 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);
        pageBuilder.append(" limit ").append((page - 1) * pageSize).append(",").append(pageSize);

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" and create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" and create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "agentId":
                    long agentId = JsonMapUtils.parseObject(params, "agentId", JsonInnerType.TYPE_LONG);
                    if (agentId > 0) {
                        condBuilder.append(" AND agent_id = ").append(agentId);
                    }
                    break;
            }
        }
        // 查找数据
        List<CommissionExchangeEntity> exchangeEntityList = commissionExchangeMapper.getList(condBuilder.toString(), pageBuilder.toString());
        // 数据总条数
        int count = commissionExchangeMapper.getCount(condBuilder.toString());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalNum", count);
        resultMap.put("list", exchangeEntityList);
        response.setData(resultMap);
    }

    /**
     * 删除代理身份
     */
    @GmHandler(key = "/ttmy/agent/delete")
    public void doAgentDeleteTask(Map<String, Object> params, CommonResponse response) throws Exception {
        long agentId = JsonMapUtils.parseObject(params, "agentId", JsonInnerType.TYPE_LONG);
        AgentEntity agentEntity = agentMapper.getByPlayerId(agentId);
        if (agentEntity == null) {
            response.setSuccess(false);
            response.setErrMsg("该玩家不是代理！");
            return;
        }
        if (agentEntity.getAgentLevel() == 1) { // 一级代理
            ServerUser agentUser = UserContainer.getUserById(agentId);
            OseePlayerEntity playerEntity = PlayerManager.getPlayerEntity(agentUser);
            playerEntity.setPlayerType(0); // 玩家身份设置为普通玩家
            playerMapper.update(playerEntity);
            // 获取下面所有的二级代理
            List<AgentEntity> agentEntityList = agentMapper.getByAgentPlayerId(agentId);
            for (AgentEntity entity : agentEntityList) {
                // 删除二级代理及他下面被代理玩家的代理信息
                agentMapper.deleteByAgentId(entity.getPlayerId());
                // 删除二级代理收到的所有贡献佣金数据
                agentCommissionMapper.deleteByAgentId(entity.getPlayerId());
                // 删除二级代理所有的兑换数据
                commissionExchangeMapper.deleteByAgentId(entity.getPlayerId());
            }
        }
        // 删除自己的代理信息和绑定了该代理的其他玩家代理信息
        agentMapper.deleteByAgentId(agentId);
        // 删除代理收到的所有贡献佣金数据
        agentCommissionMapper.deleteByAgentId(agentId);
        // 删除代理玩家所有的兑换数据
        commissionExchangeMapper.deleteByAgentId(agentId);
        // 删除棋牌模块开启状态
        RedisHelper.remove("Agent:OpenChessCards:" + agentId);
    }

    /**
     * 获取玩家背包物品信息
     */
    @GmHandler(key = "/ttmy/player/package/info")
    public void doPlayerPackageInfoTask(Map<String, Object> params, CommonResponse response) {
        long playerId = (long) (double) params.get("playerId");
        ServerUser user = UserContainer.getUserById(playerId);
        if (user == null) {
            response.setSuccess(false);
            response.setErrMsg("玩家不存在！");
            return;
        }

        List<String> data = new LinkedList<>();
        for (ItemId itemId : ItemId.values()) {
            if ((itemId.getId() >= ItemId.BRONZE_TORPEDO.getId() && itemId.getId() <= ItemId.SKILL_CRIT.getId()) ||
                    itemId.getId() == ItemId.BOSS_BUGLE.getId() ||
                    itemId.getId() == ItemId.FEN_SHEN.getId()) {
                data.add(itemId.getInfo() + "*" + PlayerManager.getItemNum(user, itemId));
            }
        }
        response.setData(data);
    }

    /**
     * 龙晶兑换记录明细
     */
    @GmHandler(key = "/ttmy/money/log/crystal/exchange")
    public void doDragonCrystalExchangeLogTask(Map<String, Object> params, CommonResponse response) throws Exception {
        // 条件语句构建
        StringBuilder condBuilder = new StringBuilder(" where 1=1");

        for (Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date startDate = new Date(startTime);
                        condBuilder.append(" AND create_time >= '").append(DATE_FORMATER.format(startDate)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date endDate = new Date(endTime);
                        condBuilder.append(" AND create_time <= '").append(DATE_FORMATER.format(endDate)).append("'");
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND player_id = ").append(playerId);
                    }
                    break;
                case "exchangeType":
                    int exchangeType = JsonMapUtils.parseObject(params, "exchangeType", JsonInnerType.TYPE_INT);
                    if (exchangeType >= 0) {
                        condBuilder.append(" AND exchange_type = ").append(exchangeType);
                    }
                    break;
            }
        }
        condBuilder.append(" order by create_time desc");

        // 数据总条数
        long count = crystalExchangeLogMapper.getCount(condBuilder.toString());
        // 解析分页数据
        int pageNo = JsonMapUtils.parseObject(params, "page", JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonInnerType.TYPE_INT);
        condBuilder.append(" limit ").append((pageNo - 1) * pageSize).append(",").append(pageSize);
        // 开始获取数据
        List<CrystalExchangeLogEntity> list = crystalExchangeLogMapper.getList(condBuilder.toString());

        List<Map> dataList = new LinkedList<>();
        for (CrystalExchangeLogEntity entity : list) {
            Map<String, Object> data = new HashMap<>();
            data.put("playerId", entity.getPlayerId());
            data.put("playerName", UserContainer.getUserById(entity.getPlayerId()).getNickname());
            data.put("exchangeType", entity.getExchangeType());
            String torpedo = "青*%d 银*%d 金*%d";
            data.put("torpedoBefore", String.format(torpedo, entity.getBronzeTorpedoBefore(), entity.getSilverTorpedoBefore(), entity.getGoldTorpedoBefore()));
            data.put("torpedoChange", String.format(torpedo, entity.getBronzeTorpedoChange(), entity.getSilverTorpedoChange(), entity.getGoldTorpedoChange()));
            data.put("torpedoAfter", String.format(torpedo,
                    entity.getBronzeTorpedoBefore() + entity.getBronzeTorpedoChange(),
                    entity.getSilverTorpedoBefore() + entity.getSilverTorpedoChange(),
                    entity.getGoldTorpedoBefore() + entity.getGoldTorpedoChange()));
            data.put("crystalBefore", entity.getDragonCrystalBefore());
            data.put("crystalChange", entity.getDragonCrystalChange());
            data.put("crystalAfter", entity.getDragonCrystalBefore() + entity.getDragonCrystalChange());
            data.put("createTime", entity.getCreateTime());
            dataList.add(data);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalNum", count);
        result.put("list", dataList);
        response.setData(result);
    }

    /**
     * 更改玩家vip等级
     */
    @GmHandler(key = "/ttmy/player/vip/change")
    public void doChangePlayerVipLevelTask(Map<String, Object> params, CommonResponse response) {
        long playerId = (long) (double) params.get("playerId");
        int vipLevel = (int) (double) params.get("vipLevel");
        ServerUser user = UserContainer.getUserById(playerId);
        if (user == null) {
            response.setSuccess(false);
            response.setErrMsg("玩家ID有误");
            return;
        }
        long[] vipMoney = PlayerManager.VIP_MONEY;
        if (vipLevel < 0 || vipLevel > vipMoney.length) {
            response.setSuccess(false);
            response.setErrMsg("输入的VIP等级有误");
            return;
        }
        OseePlayerEntity playerEntity = PlayerManager.getPlayerEntity(user);
        playerEntity.setVipLevel(vipLevel); // 设置等级
        playerEntity.setRechargeMoney(vipLevel == 0 ? 0 : vipMoney[vipLevel - 1] + 1); // 设置充值的金额为对应等级之上
        playerMapper.update(playerEntity);
        PlayerManager.sendVipLevelResponse(user);
    }

    /**
     * 发送系统邮件
     */
    @GmHandler(key = "/ttmy/send_mail")
    public void sendMail(Map<String, Object> params, CommonResponse response) {
        // 有值就是个人发送，否在全服发送
        long toId = params.containsKey("receiverId") ? (long) (double) params.get("receiverId") : -1;

        MessageEntity messageEntity = new MessageEntity();
        // 系统邮件发送ID为-1
        messageEntity.setFromId(-1L);
        if (toId > 0) { // 指定发送玩家
            if (UserContainer.getUserById(toId) == null) {
                response.setSuccess(false);
                response.setErrMsg("玩家ID不存在");
                return;
            }
            messageEntity.setToId(toId);
        }

        messageEntity.setTitle(params.get("title").toString());
        messageEntity.setContent(params.get("content").toString());

        if (params.containsKey("itemId") && params.containsKey("itemCount")) {
            Integer[] itemIds = JSON.parseObject(params.get("itemId").toString(), Integer[].class);
            Integer[] itemCounts = JSON.parseObject(params.get("itemCount").toString(), Integer[].class);
            ItemData[] itemData = new ItemData[itemIds.length];
            for (int i = 0; i < itemData.length; i++) {
                itemData[i] = new ItemData(itemIds[i], itemCounts[i]);
            }
            messageEntity.setItems(itemData);
        }
        messageManager.sendMessage(messageEntity); // 发送邮件
    }


    @GmHandler(key = "/osee/shop/rewardSetting")
    public void rewardRank(Map<String, Object> params, CommonResponse response) {
        int type = JSON.parseObject(params.get("type").toString(), Integer.class);
        List<AppRewardRankEntity> appRewardRankEntities = rewardRankMapper.findByType(type);
        response.setData(appRewardRankEntities);
        response.setSuccess(true);
    }

    @GmHandler(key = "/osee/shop/updateRewardSetting")
    public void updateRewardSetting(Map<String, Object> params, CommonResponse response) throws Exception {
        int diamond = JSON.parseObject(params.get("diamond").toString(), Integer.class);
        int gold = JSON.parseObject(params.get("gold").toString(), Integer.class);
        int highBall = JSON.parseObject(params.get("highBall").toString(), Integer.class);
        int id = JSON.parseObject(params.get("id").toString(), Integer.class);
        int lowerBall = JSON.parseObject(params.get("lowerBall").toString(), Integer.class);
        int middleBall = JSON.parseObject(params.get("middleBall").toString(), Integer.class);
        int bossBugle = JSON.parseObject(params.get("bossBugle").toString(), Integer.class);
        int skillCrit = JSON.parseObject(params.get("skillCrit").toString(), Integer.class);
        int skillFast = JSON.parseObject(params.get("skillFast").toString(), Integer.class);
        int skillFrozen = JSON.parseObject(params.get("skillFrozen").toString(), Integer.class);
        int skillLock = JSON.parseObject(params.get("skillLock").toString(), Integer.class);
        AppRewardLogEntity entity = new AppRewardLogEntity();
        entity.setId(id);
        entity.setDiamond(diamond);
        entity.setGold(gold);
        entity.setHighBall(highBall);
        entity.setLowerBall(lowerBall);
        entity.setMiddleBall(middleBall);
        entity.setBossBugle(bossBugle);
        entity.setSkillCrit(skillCrit);
        entity.setSkillFast(skillFast);
        entity.setSkillFrozen(skillFrozen);
        entity.setSkillLock(skillLock);
        int a = rewardLogMapper.update(entity);

    }

    @GmHandler(key = "/osee/shop/saveRewardSetting")
    public void saveRewardSetting(Map<String, Object> params, CommonResponse response) {
        Integer rank = JSON.parseObject(params.get("rank").toString(), Integer.class);
        Integer type = JSON.parseObject(params.get("type").toString(), Integer.class);
        Integer status = JSON.parseObject(params.get("status").toString(), Integer.class);
        AppRewardLogEntity rewardLogEntity = new AppRewardLogEntity();
        rewardLogMapper.save(rewardLogEntity);
        AppRewardRankEntity entity = new AppRewardRankEntity();
        entity.setRank(rank);
        entity.setType(type);
        entity.setStatus(status);
        entity.setReward(rewardLogEntity);
        entity.setUpdateTime(new Date());
        rewardRankMapper.save(entity);
        AppRewardRankEntity result = rewardRankMapper.findById(entity.getId());
        response.setData(result);
        response.setSuccess(true);
    }

    @GmHandler(key = "/osee/shop/deleteRewardSetting")
    public void deleteRewardSetting(Map<String, Object> params, CommonResponse response) {
        Integer id = JSON.parseObject(params.get("id").toString(), Integer.class);
        AppRewardRankEntity entity = rewardRankMapper.findById(id);
        int rewardId = entity.getReward().getId();
        rewardLogMapper.delete(rewardId);
        rewardRankMapper.delete(id);
        response.setSuccess(true);
    }

    @GmHandler(key = "/osee/shop/updateRewardRank")
    public void updateRewardRank(Map<String, Object> params, CommonResponse response) {
        Integer id = JSON.parseObject(params.get("id").toString(), Integer.class);
        Integer rank = JSON.parseObject(params.get("rank").toString(), Integer.class);
        AppRewardRankEntity entity = new AppRewardRankEntity();
        entity.setId(id);
        entity.setRank(rank);
        rewardRankMapper.update(entity);
    }
}