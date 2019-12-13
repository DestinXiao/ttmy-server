package com.maple.game.osee.controller.gm;

import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.entity.UserEntity;
import com.maple.database.data.mapper.UserMapper;
import com.maple.engine.anotation.GmController;
import com.maple.engine.anotation.GmHandler;
import com.maple.engine.utils.JsonMapUtils;
import com.maple.game.osee.controller.gm.base.GmBaseController;
import com.maple.game.osee.dao.data.entity.AgentEntity;
import com.maple.game.osee.dao.data.mapper.AgentMapper;
import com.maple.game.osee.dao.log.entity.AgentCommissionInfoEntity;
import com.maple.game.osee.dao.log.entity.AgentWithdrawLogEntity;
import com.maple.game.osee.dao.log.mapper.AgentCommissionInfoMapper;
import com.maple.game.osee.dao.log.mapper.AgentCommissionMapper;
import com.maple.game.osee.dao.log.mapper.AgentWithdrawLogMapper;
import com.maple.game.osee.entity.gm.CommonResponse;
import com.maple.game.osee.manager.AgentManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 渠道商代理控制器
 */
@GmController
public class GmChannelAgentController extends GmBaseController {

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private AgentCommissionMapper agentCommissionMapper;

    @Autowired
    private AgentCommissionInfoMapper agentCommissionInfoMapper;

    @Autowired
    private AgentWithdrawLogMapper agentWithdrawLogMapper;

    /**
     * 初始分成比例关键字
     */
    private static final String INIT_RATE = "Ttmy:Agent:FirstRate";

    /**
     * 提现申请周期
     */
    private static final String FLUSH_TIME = "Ttmy:Agent:FlushTime";

    /**
     * 获取初始分成比例
     */
    private static double getInitRate() {
        String rateStr = RedisHelper.get(INIT_RATE);
        return StringUtils.isEmpty(rateStr) ? 0.01 : Double.parseDouble(rateStr);
    }

    /**
     * 设置初始分成比例
     */
    private static void setInitRate(Double initRate) {
        RedisHelper.set(INIT_RATE, initRate.toString());
    }

    /**
     * 获取提现周期
     */
    public static int getFlushTime() {
        String rateStr = RedisHelper.get(FLUSH_TIME);
        return StringUtils.isEmpty(rateStr) ? 7 : Integer.parseInt(rateStr);
    }

    /**
     * 设置提现周期
     */
    public static void setFlushTime(Integer flushTime) {
        RedisHelper.set(FLUSH_TIME, flushTime.toString());
    }

    /**
     * 渠道列表
     */
    @GmHandler(key = "/ttmy/channel/list")
    public void doChannelAgentList(Map<String, Object> params, CommonResponse response) throws Exception {
        List<Map<String, Object>> agentList = new LinkedList<>();

        // 查询销售金额和佣金的查询条件
        StringBuilder condBuilder = new StringBuilder("WHERE 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonMapUtils.JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonMapUtils.JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        condBuilder.append(" AND create_time >= '").append(DATE_FORMATER.format(startTime)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        condBuilder.append(" AND create_time <= '").append(DATE_FORMATER.format(endTime)).append("'");
                    }
                    break;
                case "playerId":
                    long id = JsonMapUtils.parseObject(params, "playerId", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (id > 0) {
                        condBuilder.append(" AND player_id = ").append(id);
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonMapUtils.JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND player_name = '").append(nickname).append("'");
                    }
                    break;
            }
        }
        condBuilder.append(" AND agent_level = 1"); // 只查询一级代理
        List<AgentEntity> agentEntityList = agentMapper.getAgentList(condBuilder.toString(), pageBuilder.toString());
        int totalNum = agentMapper.getAgentCount(condBuilder.toString());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (AgentEntity agentEntity : agentEntityList) {
            Map<String, Object> agentItem = new HashMap<>();

            int totalPerson = 0, dailyPerson = 0;
            List<AgentEntity> entities = agentMapper.getByAgentPlayerId(agentEntity.getPlayerId());
            totalPerson += entities.size();
            for (AgentEntity entity : entities) {
                if (sdf.format(new Date()).equals(sdf.format(entity.getCreateTime()))) {
                    dailyPerson += 1;
                }

                totalPerson += agentMapper.getAgentNextLevelCount(entity.getPlayerId());
                dailyPerson += agentMapper.getDailyPlayerSize(entity.getPlayerId());
            }

            long month;
            if (params.containsKey("month")) {
                month = JsonMapUtils.parseObject(params, "month", JsonMapUtils.JsonInnerType.TYPE_LONG);
            } else {
                month = System.currentTimeMillis();
            }
            String monthDate = new SimpleDateFormat("yyyy-MM").format(new Date(month));

            agentItem.put("playerId", agentEntity.getPlayerId());
            agentItem.put("nickname", agentEntity.getPlayerName());
            agentItem.put("totalPersonNum", totalPerson);
            agentItem.put("dailyPersonNum", dailyPerson);
            agentItem.put("dailyRecharge", agentCommissionMapper.getDailyCommissionByAgentId(agentEntity.getPlayerId()));
            agentItem.put("monthRecharge", agentCommissionMapper.getTargetMonthCommissionByAgentId(agentEntity.getPlayerId(), monthDate));
            agentItem.put("totalRecharge", agentCommissionMapper.getTotalCommissionByAgentId(agentEntity.getPlayerId()));
            agentItem.put("currentMoney", agentEntity.getTotalCommission());
            agentItem.put("createTime", agentEntity.getCreateTime());
            agentItem.put("rate", agentEntity.getFirstCommissionRate());
            agentList.add(agentItem);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalNum", totalNum);
        resultMap.put("list", agentList);
        resultMap.put("rate", getInitRate());
        response.setData(resultMap);
    }

    /**
     * 删除渠道商
     */
    @GmHandler(key = "/ttmy/channel/delete")
    public void doChannelAgentDelete(Map<String, Object> params, CommonResponse response) throws Exception {
        AgentEntity agentEntity = agentManager.getAgentInfoByPlayerId((long) (double) params.get("playerId"));

        if (agentEntity.getAgentLevel() == 1) {
            if (!agentManager.deleteFirAgent(agentEntity)) {
                response.setSuccess(false);
                response.setErrMsg("删除失败1");
            }
        } else {
            response.setSuccess(false);
            response.setErrMsg("删除失败");
        }
    }

    /**
     * 更新渠道商
     */
    @GmHandler(key = "/ttmy/channel/update")
    public void doChannelAgentUpdate(Map<String, Object> params, CommonResponse response) throws Exception {
        long playerId = (long) (double) params.get("playerId");
        double rate = (double) params.get("rate");

        response.setSuccess(false);
        if (rate > 100D) {
            response.setErrMsg("分成比例不可大于100%");
            return;
        }

        List<AgentEntity> secAgents = agentMapper.getByAgentPlayerId(playerId);
        for (AgentEntity agent : secAgents) {
            if (agent.getSecondCommissionRate() != null && agent.getSecondCommissionRate() > rate) {
                response.setErrMsg("员工:" + agent.getPlayerName() + "分成比例" + agent.getSecondCommissionRate() +
                        "大于" + rate + "，无法修改");
            }
        }

        AgentEntity agentEntity = agentManager.getAgentInfoByPlayerId(playerId);
        if (agentEntity.getAgentLevel() == 1) {
            if (rate < agentEntity.getFirstCommissionRate()) {
                response.setErrMsg("不可小于原分成比例");
                return;
            }

            agentEntity.setFirstCommissionRate(rate);
            agentMapper.update(agentEntity);
        } else {
            response.setErrMsg("渠道商id有误");
        }
        response.setSuccess(true);
    }

    /**
     * 修改渠道总分成比例
     */
    @GmHandler(key = "/ttmy/channel/rate/update")
    public void doChannelRateUpdate(Map<String, Object> params, CommonResponse response) throws Exception {
        double rate = (double) params.get("rate");
        int update = (int) (double) params.get("update");

        if (update == 0) {
            agentMapper.updateSameFirstRate(getInitRate(), rate);
        } else {
            agentMapper.updateAllFirstRate(rate);
        }
        setInitRate(rate);
    }

    /**
     * 获取渠道员工列表
     */
    @GmHandler(key = "/ttmy/channel/promoter/list")
    public void doChannelPromoterList(Map<String, Object> params, CommonResponse response) throws Exception {
        List<Map<String, Object>> agentList = new LinkedList<>();

        long channelId = (long) (double) params.get("channelId");
        // 查询销售金额和佣金的查询条件
        StringBuilder condBuilder = new StringBuilder("WHERE agent_player_id = " + channelId);
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonMapUtils.JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonMapUtils.JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "promoterId":
                    long id = JsonMapUtils.parseObject(params, "promoterId", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (id > 0) {
                        condBuilder.append(" AND player_id = ").append(id);
                    }
                    break;
                case "promoterName":
                    String nickname = JsonMapUtils.parseObject(params, "promoterName", JsonMapUtils.JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND player_name = '").append(nickname).append("'");
                    }
                    break;
            }
        }
        AgentEntity channelAgent = agentManager.getAgentInfoByPlayerId(channelId);
        List<AgentEntity> agentEntityList = agentMapper.getAgentList(condBuilder.toString(), pageBuilder.toString());
        int totalNum = agentMapper.getAgentCount(condBuilder.toString());
        for (AgentEntity agentEntity : agentEntityList) {
            Map<String, Object> agentItem = new HashMap<>();
            long month;
            if (params.containsKey("month")) {
                month = JsonMapUtils.parseObject(params, "month", JsonMapUtils.JsonInnerType.TYPE_LONG);
            } else {
                month = System.currentTimeMillis();
            }
            String monthDate = new SimpleDateFormat("yyyy-MM").format(new Date(month));

            agentItem.put("playerId", agentEntity.getPlayerId());
            agentItem.put("nickname", agentEntity.getPlayerName());
            agentItem.put("dailyPersonNum", agentMapper.getDailyPlayerSize(agentEntity.getPlayerId()));
            agentItem.put("dailyRecharge", agentCommissionMapper.getDailyCommissionByAgentId(agentEntity.getPlayerId()));
            agentItem.put("monthRecharge", agentCommissionMapper.getTargetMonthCommissionByAgentId(agentEntity.getPlayerId(), monthDate));
            agentItem.put("totalRecharge", agentCommissionMapper.getTotalCommissionByAgentId(agentEntity.getPlayerId()));
            agentItem.put("rate", agentEntity.getSecondCommissionRate() == null ? channelAgent.getSecondCommissionRate() : agentEntity.getSecondCommissionRate());
            agentList.add(agentItem);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalNum", totalNum);
        resultMap.put("list", agentList);
        resultMap.put("rate", channelAgent.getSecondCommissionRate());
        resultMap.put("channelRate", channelAgent.getFirstCommissionRate());
        response.setData(resultMap);
    }

    /**
     * 删除渠道员工
     */
    @GmHandler(key = "/ttmy/channel/staff/delete")
    public void doChannelStaffDelete(Map<String, Object> params, CommonResponse response) throws Exception {
        long playerId = (long) (double) params.get("playerId");
        AgentEntity agentEntity = agentManager.getAgentInfoByPlayerId(playerId);
        if (agentEntity.getAgentLevel() == 2) {
            if (!agentManager.deleteSecAgent(agentEntity)) {
                response.setSuccess(false);
                response.setErrMsg("删除失败");
            }
        } else {
            response.setSuccess(false);
            response.setErrMsg("该用户不为推广员");
        }
    }

    /**
     * 修改渠道员工总分成比例
     */
    @GmHandler(key = "/ttmy/channel/staff/rate/update")
    public void doChannelStaffRateUpdate(Map<String, Object> params, CommonResponse response) {
        AgentEntity agent = agentManager.getAgentInfoByPlayerId((long) (double) params.get("channelId"));
        double rate = (double) params.get("rate");
        int update = (int) (double) params.get("update");
        if (rate > agent.getFirstCommissionRate()) {
            response.setSuccess(false);
            response.setErrMsg("员工分成不能大于渠道分成");
            return;
        }

        gmChannelAgentUserController.updateStaffRate(agent, rate, update);
    }

    /**
     * 修改指定渠道员工分成比例
     */
    @GmHandler(key = "/ttmy/channel/staff/user_rate/update")
    public void doChannelStaffUserRateUpdate(Map<String, Object> params, CommonResponse response) {
        AgentEntity agent = agentManager.getAgentInfoByPlayerId((long) (double) params.get("channelId"));
        long playerId = (long) (double) params.get("playerId");
        String nickname = params.get("nickname").toString();
        double rate = (double) params.get("rate");
        if (agent.getFirstCommissionRate() != null && rate > agent.getFirstCommissionRate()) {
            response.setSuccess(false);
            response.setErrMsg("员工分成不能大于渠道分成");
            return;
        }

        gmChannelAgentUserController.updateUserStaffRate(agent, rate, playerId, nickname, response);
    }

    /**
     * 渠道提现申请列表
     */
    @GmHandler(key = "/ttmy/channel/withdraw/list")
    public void doChannelWithdrawList(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("and agent.agent_level = 1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonMapUtils.JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonMapUtils.JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        Date date = new Date(startTime);
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(date)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        Date date = new Date(endTime);
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(date)).append("'");
                    }
                    break;
                case "channelId":
                    long id = JsonMapUtils.parseObject(params, "channelId", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (id > 0) {
                        condBuilder.append(" AND log.channel_id = ").append(id);
                    }
                    break;
                case "nickname":
                    String nickname = JsonMapUtils.parseObject(params, "nickname", JsonMapUtils.JsonInnerType.TYPE_STRING);
                    if (!StringUtils.isEmpty(nickname)) {
                        condBuilder.append(" AND agent.player_name = '").append(nickname).append("'");
                    }
                    break;
                case "state":
                    long state = JsonMapUtils.parseObject(params, "state", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    condBuilder.append(" AND log.state = ").append(state - 1);
                    break;
            }
        }
        List<Map<String, Object>> withdrawInfoList = agentWithdrawLogMapper.getMapList(condBuilder.toString(), pageBuilder.toString());
        int totalNum = agentWithdrawLogMapper.getMapCount(condBuilder.toString());

        List<Map<String, Object>> withdrawDatas = new ArrayList<>();
        for (Map<String, Object> withdrawInfo : withdrawInfoList) {
            Map<String, Object> withdrawData = new HashMap<>();
            withdrawData.put("id", withdrawInfo.get("id"));
            withdrawData.put("channelId", withdrawInfo.get("agent_id"));
            withdrawData.put("nickname", withdrawInfo.get("player_name"));
            withdrawData.put("money", withdrawInfo.get("money"));
            withdrawData.put("bank", withdrawInfo.get("bank"));
            withdrawData.put("accountName", withdrawInfo.get("real_name"));
            withdrawData.put("cardNo", withdrawInfo.get("bank_num"));
            withdrawData.put("bankName", withdrawInfo.get("open_bank"));
            withdrawData.put("creator", withdrawInfo.get("creator"));
            withdrawData.put("state", ((int) withdrawInfo.get("state")) + 1);
            withdrawData.put("createTime", ((Date) withdrawInfo.get("create_time")).getTime());
            withdrawDatas.add(withdrawData);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", withdrawDatas);
        data.put("totalNum", totalNum);
        data.put("time", getFlushTime());
        response.setData(data);
    }

    /**
     * 修改提现申请周期
     */
    @GmHandler(key = "/ttmy/channel/withdraw/time/update")
    public void doChannelWithdrawTimeUpdate(Map<String, Object> params, CommonResponse response) throws Exception {
        setFlushTime(((Double) params.get("time")).intValue());
    }

    /**
     * 修改提现状态
     */
    @GmHandler(key = "/ttmy/channel/withdraw/state/update")
    public void doChannelWithdrawStateUpdate(Map<String, Object> params, CommonResponse response) throws Exception {
        AgentWithdrawLogEntity withdrawLogEntity = agentWithdrawLogMapper.get((long) (double) params.get("id"));
        if (withdrawLogEntity.getState() == 0) {
            int state = (int) (double) params.get("state");
            withdrawLogEntity.setCreator(params.get("creator").toString());
            if (state == 3) { // 拒绝返佣金
                AgentEntity agentEntity = agentManager.getAgentInfoByPlayerId(withdrawLogEntity.getAgentId());
                agentEntity.setTotalCommission(agentEntity.getTotalCommission() + withdrawLogEntity.getMoney());
                agentMapper.update(agentEntity);
            }
            withdrawLogEntity.setState(state - 1);
            agentWithdrawLogMapper.updateState(withdrawLogEntity);
        } else {
            response.setSuccess(false);
            response.setErrMsg("该订单已处理");
        }
    }

    /**
     * 财务明细
     */
    @GmHandler(key = "/ttmy/channel/money/list")
    public void doChannelMoneyList(Map<String, Object> params, CommonResponse response) throws Exception {
        StringBuilder condBuilder = new StringBuilder("where 1=1");
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonMapUtils.JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonMapUtils.JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append((page - 1) * pageSize).append(", ").append(pageSize);

        long channelId = 0;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            if ("channelId".equals(key)) {
                channelId = JsonMapUtils.parseObject(params, "startTime", JsonMapUtils.JsonInnerType.TYPE_LONG);
                if (channelId > 0) {
                    condBuilder.append(" AND log.channel_id >= ").append(channelId);
                }
            }
        }
        List<AgentCommissionInfoEntity> commissions = agentCommissionInfoMapper.getList(condBuilder.toString(), pageBuilder.toString());
        int totalNum = agentCommissionInfoMapper.getCount(condBuilder.toString());

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (AgentCommissionInfoEntity commissionInfo : commissions) {
            Map<String, Object> data = new HashMap<>();
            data.put("playerId", commissionInfo.getPlayerId());
            data.put("nickname", commissionInfo.getPlayerName());
            data.put("shopName", commissionInfo.getShopName());
            data.put("shopMoney", commissionInfo.getMoney());
            data.put("channelMoney", commissionInfo.getCommission());
            data.put("channelId", commissionInfo.getChannelId());
            data.put("createTime", commissionInfo.getCreateTime());
            dataList.add(data);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", dataList);
        data.put("totalNum", totalNum);
        if (channelId > 0) {
            data.put("dailyNewCount", agentMapper.getDailyChannelPlayerSize(channelId));
            data.put("dailyTotalMoney", agentCommissionMapper.getDailyCommissionByAgentId(channelId));
        } else {
            data.put("dailyNewCount", agentMapper.getDailyTotalChannelPlayerSize());
            data.put("dailyTotalMoney", agentCommissionInfoMapper.getDailyTotalCommission());
        }
        response.setData(data);
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GmChannelAgentUserController gmChannelAgentUserController;

    /**
     * 渠道商用户登录
     */
    @GmHandler(key = "/ttmy/channel/user/login")
    public void doChannelUserLogin(Map<String, Object> params, CommonResponse response) throws Exception {
        String userName = params.get("username").toString();
        String password = params.get("password").toString();

        response.setSuccess(false);
        UserEntity userEntity = userMapper.findByUsername(userName);
        if (!userEntity.getPassword().equals(password)) {
            response.setErrMsg("密码错误");
            return;
        }

        if (userEntity.getUserState() != 0) {
            response.setErrMsg("您的账号已被冻结，无法登录渠道商后台");
            return;
        }
        AgentEntity agentEntity = agentManager.getAgentInfoByPlayerId(userEntity.getId());
        if (agentEntity == null || agentEntity.getAgentLevel() != 1) {
            response.setErrMsg("账号类型错误，无法登录");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        int token = ThreadLocalRandom.current().nextInt(10000000, 100000000);
        RedisHelper.set(String.format(gmChannelAgentUserController.channelOnlineKey, userEntity.getId()), Integer.toString(token), 2 * 3600);
        data.put("id", userEntity.getId());
        data.put("token", Integer.toString(token));
        data.put("nickname", userEntity.getNickname());
        response.setData(data);
        response.setSuccess(true);
    }
}
