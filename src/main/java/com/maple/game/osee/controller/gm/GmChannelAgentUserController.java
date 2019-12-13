package com.maple.game.osee.controller.gm;

import com.google.gson.Gson;
import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.mapper.UserMapper;
import com.maple.engine.anotation.GmController;
import com.maple.engine.anotation.GmHandler;
import com.maple.engine.container.UserContainer;
import com.maple.engine.data.ServerUser;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 渠道商后台控制器
 */
@GmController
public class GmChannelAgentUserController extends GmBaseController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 代理在线码
     */
    public String channelOnlineKey = "ChannelOnlineToken:%d";

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
     * GM默认检查器
     */
    @Override
    public String checker(Method taskMethod, Object param) {
        try {
            Map<String, Object> data = new HashMap<>();
            CommonResponse response = new CommonResponse(data);

            long id = ((Double) ((Map) param).get("id")).longValue();
            String token = ((Map) param).get("token").toString();
            if (StringUtils.isEmpty(token)) {
                response.setSuccess(false);
                response.setErrMsg("登录凭证有误");
            } else {
                String rToken = RedisHelper.get(String.format(channelOnlineKey, id));
                if (rToken == null || !rToken.equals(token)) {
                    response.setSuccess(false);
                    response.setErrMsg("登录凭证无效，请重新登录！");
                } else {
                    taskMethod.invoke(this, param, response, data, agentManager.getAgentInfoByPlayerId(id));
                }
            }

            return new Gson().toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 渠道推广用户列表
     */
    @GmHandler(key = "/ttmy/channel/user/list")
    public void doChannelUserList(Map<String, Object> params, CommonResponse response, Map<String, Object> data, AgentEntity agent) throws Exception {
        StringBuilder condBuilder = new StringBuilder();
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonMapUtils.JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonMapUtils.JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append(page).append(", ").append(pageSize);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        condBuilder.append(" AND player.create_time >= '").append(DATE_FORMATER.format(startTime)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        condBuilder.append(" AND player.create_time <= '").append(DATE_FORMATER.format(endTime)).append("'");
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND player.player_id = ").append(playerId);
                    }
                    break;
                case "promoterId":
                    long promoterId = JsonMapUtils.parseObject(params, "promoterId", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (promoterId > 0) {
                        condBuilder.append(" AND player.agent_player_id = ").append(promoterId);
                    }
                    break;
                case "gameState":
                    long gameState = JsonMapUtils.parseObject(params, "gameState", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (gameState > 0) {
                        if (gameState < 100) {
                            condBuilder.append(" AND userInfo.online_state = ").append(gameState - 1);
                        } else {
                            condBuilder.append(" AND userInfo.online_state > 0");
                        }
                    }
                    break;
                case "vipLevel":
                    int vipLevel = JsonMapUtils.parseObject(params, "vipLevel", JsonMapUtils.JsonInnerType.TYPE_INT);
                    if (vipLevel > 0) {
                        condBuilder.append(" AND playerInfo.vip_level = ").append(vipLevel);
                    }
                    break;
            }
        }
        List<Map<String, Object>> agentEntityList = agentMapper.getPlayerByChannelId(agent.getPlayerId(), condBuilder.toString(), pageBuilder.toString());
        int totalNum = agentMapper.getPlayerByChannelIdSize(agent.getPlayerId(), condBuilder.toString());

        List<Map<String, Object>> playerList = new LinkedList<>();
        for (Map<String, Object> player : agentEntityList) {
            Map<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("playerId", player.get("player_id"));
            playerInfo.put("nickname", player.get("nickname"));
            playerInfo.put("vipLevel", player.get("vip_level"));
            playerInfo.put("batteryLevel", player.get("battery_level"));
            playerInfo.put("gameState", ((int) player.get("online_state")) + 1);
            playerInfo.put("createTime", player.get("create_time"));
            playerInfo.put("promoterId", player.get("agent_player_id"));
            playerList.add(playerInfo);
        }
        data.put("list", playerList);
        data.put("totalNum", totalNum);
        data.put("dailyPersonNum", agentMapper.getDailyChannelPlayerSize(agent.getPlayerId()));
        data.put("dailyRecharge", agentCommissionMapper.getDailyCommissionByAgentId(agent.getPlayerId()));
    }

    /**
     * 渠道收入明细
     */
    @GmHandler(key = "/ttmy/channel/my_money/list")
    public void doChannelMyMoneyList(Map<String, Object> params, CommonResponse response, Map<String, Object> data, AgentEntity agent) throws Exception {
        StringBuilder condBuilder = new StringBuilder("where log.channel_id = " + agent.getPlayerId());
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonMapUtils.JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonMapUtils.JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append(page).append(", ").append(pageSize);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case "startTime":
                    long startTime = JsonMapUtils.parseObject(params, "startTime", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (startTime > 0) {
                        condBuilder.append(" AND log.create_time >= '").append(DATE_FORMATER.format(startTime)).append("'");
                    }
                    break;
                case "endTime":
                    long endTime = JsonMapUtils.parseObject(params, "endTime", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (endTime > 0) {
                        condBuilder.append(" AND log.create_time <= '").append(DATE_FORMATER.format(endTime)).append("'");
                    }
                    break;
                case "playerId":
                    long playerId = JsonMapUtils.parseObject(params, "playerId", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (playerId > 0) {
                        condBuilder.append(" AND log.player_id = ").append(playerId);
                    }
                    break;
                case "spreadId":
                    long promoterId = JsonMapUtils.parseObject(params, "spreadId", JsonMapUtils.JsonInnerType.TYPE_LONG);
                    if (promoterId > 0) {
                        condBuilder.append(" AND log.promoter_id = ").append(promoterId);
                    }
                    break;
            }
        }
        List<AgentCommissionInfoEntity> logs = agentCommissionInfoMapper.getList(condBuilder.toString(), pageBuilder.toString());
        int totalNum = agentCommissionInfoMapper.getCount(condBuilder.toString());
        Map<String, Object> statistics = agentCommissionInfoMapper.getStatistics(condBuilder.toString());

        List<Map<String, Object>> playerList = new LinkedList<>();
        for (AgentCommissionInfoEntity log : logs) {
            Map<String, Object> logInfo = new HashMap<>();
            logInfo.put("playerId", log.getPlayerId());
            logInfo.put("nickname", log.getPlayerName());
            logInfo.put("shopName", log.getShopName());
            logInfo.put("shopMoney", log.getMoney());
            logInfo.put("createTime", log.getCreateTime());
            logInfo.put("channelMoney", log.getCommission());
            logInfo.put("spreadMoney", log.getSecCommission());
            logInfo.put("spreadId", log.getPromoterId());
            playerList.add(logInfo);
        }
        data.put("list", playerList);
        data.put("totalNum", totalNum);
        data.put("totalRecharge", statistics == null ? 0 : statistics.get("money"));
        data.put("channelTotalMoney", statistics == null ? 0 : statistics.get("commission"));
        data.put("spreadTotalMoney", statistics == null ? 0 : statistics.get("secCommission"));
    }

    /**
     * 渠道提现明细
     */
    @GmHandler(key = "/ttmy/channel/my_withdraw/list")
    public void doChannelMyWithdrawList(Map<String, Object> params, CommonResponse response, Map<String, Object> data, AgentEntity agent) throws Exception {
        StringBuilder condBuilder = new StringBuilder("where log.agent_id = " + agent.getPlayerId());
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonMapUtils.JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonMapUtils.JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append(page).append(", ").append(pageSize);
        List<AgentWithdrawLogEntity> agentEntityList = agentWithdrawLogMapper.getList(condBuilder.toString(), pageBuilder.toString());
        int totalNum = agentWithdrawLogMapper.getCount(condBuilder.toString());
        List<Map<String, Object>> statisticsList = agentWithdrawLogMapper.getStatistics(condBuilder.toString());

        List<Map<String, Object>> playerList = new LinkedList<>();
        for (AgentWithdrawLogEntity log : agentEntityList) {
            Map<String, Object> logInfo = new HashMap<>();
            logInfo.put("drawMoney", log.getMoney());
            logInfo.put("createTime", log.getCreateTime());
            logInfo.put("bank", log.getBank());
            logInfo.put("accountName", log.getRealName());
            logInfo.put("cardNo", log.getBankNum());
            logInfo.put("bankName", log.getOpenBank());
            logInfo.put("state", log.getState() + 1);
            playerList.add(logInfo);
        }
        data.put("list", playerList);
        data.put("totalNum", totalNum);
        data.put("bank", agent.getBank());
        data.put("accountName", agent.getRealName());
        data.put("cardNo", agent.getBankNum());
        data.put("bankName", agent.getOpenBank());
        data.put("remindMoney", agent.getTotalCommission());

        data.put("drawMoney", 0);
        data.put("notDrawMoney", 0);
        statisticsList.forEach(statistics -> {
            if (statistics.get("state").equals(0)) {
                data.put("notDrawMoney", statistics.get("money"));
            } else if (statistics.get("state").equals(1)) {
                data.put("drawMoney", statistics.get("money"));
            }
        });
    }

    /**
     * 申请提现
     */
    @GmHandler(key = "/ttmy/channel/withdraw")
    public void doChannelUserWithdraw(Map<String, Object> params, CommonResponse response, Map<String, Object> data, AgentEntity agent) {
        long money = (long) (double) params.get("money");
        if (money > agent.getTotalCommission()) {
            response.setSuccess(false);
            response.setErrMsg("余额不足，无法提现");
            return;
        }

        agent.setTotalCommission(agent.getTotalCommission() - money);
        agentMapper.update(agent);
        AgentWithdrawLogEntity log = new AgentWithdrawLogEntity();
        log.setAgentId(agent.getPlayerId());
        log.setMoney(money);
        log.setBank(params.get("bank").toString());
        log.setRealName(params.get("accountName").toString());
        log.setBankNum(params.get("cardNo").toString());
        log.setOpenBank(params.get("bankName").toString());
        agentWithdrawLogMapper.save(log);
    }

    /**
     * 修改提现信息
     */
    @GmHandler(key = "/ttmy/channel/withdraw/address/update")
    public void doChannelUserWithdrawInfoUpdate(Map<String, Object> params, CommonResponse response, Map<String, Object> data, AgentEntity agent) {
        agent.setBank(params.get("bank").toString());
        agent.setRealName(params.get("accountName").toString());
        agent.setBankNum(params.get("cardNo").toString());
        agent.setOpenBank(params.get("bankName").toString());
        agentMapper.update(agent);
    }

    /**
     * 员工(推广员)列表
     */
    @GmHandler(key = "/ttmy/channel/staff/list")
    public void doChannelStaffList(Map<String, Object> params, CommonResponse response, Map<String, Object> data, AgentEntity agent) throws Exception {
        List<Map<String, Object>> agentList = new LinkedList<>();

        long channelId = agent.getPlayerId();
        // 查询销售金额和佣金的查询条件
        StringBuilder condBuilder = new StringBuilder("WHERE agent_player_id = " + channelId);
        StringBuilder pageBuilder = new StringBuilder();

        // 解析数据
        int page = JsonMapUtils.parseObject(params, "page", JsonMapUtils.JsonInnerType.TYPE_INT);
        int pageSize = JsonMapUtils.parseObject(params, "pageSize", JsonMapUtils.JsonInnerType.TYPE_INT);

        pageBuilder.append(" LIMIT ").append(page).append(", ").append(pageSize);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            switch (key) {
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
            agentItem.put("dailyNewCount", agentMapper.getDailyPlayerSize(agentEntity.getPlayerId()));
            agentItem.put("dailyMoney", agentCommissionMapper.getDailyCommissionByAgentId(agentEntity.getPlayerId()));
            agentItem.put("monthMoney", agentCommissionMapper.getTargetMonthCommissionByAgentId(agentEntity.getPlayerId(), monthDate));
            agentItem.put("historyTotalMoney", agentCommissionMapper.getTotalCommissionByAgentId(agentEntity.getPlayerId()));
            agentItem.put("rate", (agentEntity.getSecondCommissionRate() == null ? agent : agentEntity).getSecondCommissionRate());
            agentList.add(agentItem);
        }

        data.put("totalNum", totalNum);
        data.put("list", agentList);
        data.put("rate", agent.getSecondCommissionRate());
        data.put("channelRate", agent.getFirstCommissionRate());
    }

    /**
     * 更新渠道员工分成比例
     */
    @GmHandler(key = "/ttmy/channel/total_rate/update")
    public void doChannelTotalRateUpdate(Map<String, Object> params, CommonResponse response, Map<String, Object> data, AgentEntity agent) {
        double rate = (double) params.get("rate");
        int update = (int) (double) params.get("update");
        if (rate > agent.getFirstCommissionRate()) {
            response.setSuccess(false);
            response.setErrMsg("员工分成不能大于渠道分成");
            return;
        }

        updateStaffRate(agent, rate, update);
    }

    /**
     * 修改员工分成比例
     */
    public void updateStaffRate(AgentEntity agent, double rate, int update) {
        if (update == 0) {
            agentMapper.updateSameSecondRate(agent.getSecondCommissionRate(), rate, agent.getPlayerId());
        } else {
            agentMapper.updateAllSecondRate(rate, agent.getPlayerId());
        }
        agent.setSecondCommissionRate(rate);
        agentMapper.update(agent);
    }

    /**
     * 更新渠道员工分成比例
     */
    @GmHandler(key = "/ttmy/channel/user_rate/update")
    public void doChannelUserRateUpdate(Map<String, Object> params, CommonResponse response, Map<String, Object> data, AgentEntity agent) throws Exception {
        double rate = (double) params.get("rate");
        String nickname = params.get("nickname").toString();
        long playerId = (long) (double) params.get("playerId");
        if (rate > agent.getFirstCommissionRate()) {
            response.setSuccess(false);
            response.setErrMsg("员工分成不能大于渠道分成");
            return;
        }

        updateUserStaffRate(agent, rate, playerId, nickname, response);
    }

    /**
     * 更新渠道指定员工分成比例
     */
    public void updateUserStaffRate(AgentEntity agent, double rate, long playerId, String nickname, CommonResponse response) {
        if (agent.getFirstCommissionRate() != null && rate > agent.getFirstCommissionRate()) {
            response.setSuccess(false);
            response.setErrMsg("员工分成比例不可大于渠道分成比例");
            return;
        }

        AgentEntity agentEntity = agentManager.getAgentInfoByPlayerId(playerId);
        if (agentEntity.getAgentLevel() == 2 && agentEntity.getAgentPlayerId().equals(agent.getPlayerId())) {
            agentEntity.setSecondCommissionRate(rate);
            agentEntity.setPlayerName(nickname);
            agentMapper.update(agentEntity);
        } else {
            response.setSuccess(false);
            response.setErrMsg("修改失败");
        }
    }

    /**
     * 删除推广员
     */
    @GmHandler(key = "/ttmy/channel/user/delete")
    public void doChannelUserDelete(Map<String, Object> params, CommonResponse response, Map<String, Object> data, AgentEntity agent) throws Exception {
        AgentEntity agentEntity = agentManager.getAgentInfoByPlayerId((long) (double) params.get("playerId"));
        if (agentEntity.getAgentLevel() == 2 && agentEntity.getAgentPlayerId().equals(agent.getPlayerId())) {
            if (!agentManager.deleteSecAgent(agentEntity)) {
                response.setSuccess(false);
                response.setErrMsg("删除失败1");
            }
        } else {
            response.setSuccess(false);
            response.setErrMsg("删除失败");
        }
    }

    @Autowired
    private UserMapper userMapper;

    /**
     * 修改登录密码
     */
    @GmHandler(key = "/ttmy/channel/password/update")
    public void doChannelPasswordUpdate(Map<String, Object> params, CommonResponse response, Map<String, Object> data, AgentEntity agent) throws Exception {
        ServerUser user = UserContainer.getUserById(agent.getPlayerId());
        if (params.get("oldPassword").equals(user.getEntity().getPassword())) {
            user.getEntity().setPassword(params.get("newPassword").toString());
            userMapper.update(user.getEntity());
        } else {
            response.setErrMsg("原密码错误");
            response.setSuccess(false);
        }
    }
}
