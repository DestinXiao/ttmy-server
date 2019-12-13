package com.maple.game.osee.manager.fightten;

import com.google.gson.Gson;
import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.container.DataContainer;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.log.entity.OseeExpendLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeExpendLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fightten.FightTenPlayer;
import com.maple.game.osee.entity.fightten.FightTenRobotPlayer;
import com.maple.game.osee.entity.fightten.config.TenTaskConfig;
import com.maple.game.osee.entity.fightten.task.FightTenTask;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.OseePublicData;
import com.maple.game.osee.proto.fightten.OseeFightTenMessage;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 拼十任务管理类
 */
@Component
public class FightTenTaskManager {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OseeExpendLogMapper expendLogMapper;

    /**
     * 任务redis保存的键前缀
     */
    public static final String TASK_REDIS_KEY_PREFIX = "Osee:FightTen:Task:";

    /**
     * 获取用户的任务
     *
     * @param userId 用户id
     */
    private FightTenTask getUserTask(long userId) {
        String key = TASK_REDIS_KEY_PREFIX + userId;
        String value = RedisHelper.get(key);
        Gson gson = new Gson();
        FightTenTask fightTenTask = gson.fromJson(value, FightTenTask.class);
        if (fightTenTask == null) { // 需要新生成玩家任务
            fightTenTask = new FightTenTask();
            fightTenTask.setTaskInfos(new ArrayList<>());
            List<TenTaskConfig> tenTaskConfigs = DataContainer.getDatas(TenTaskConfig.class);
            for (TenTaskConfig tenTaskConfig : tenTaskConfigs) {
                if (tenTaskConfig != null) {
                    FightTenTask.TaskInfo taskInfo = new FightTenTask.TaskInfo();
                    taskInfo.setTaskId(tenTaskConfig.getId());
                    taskInfo.setTargetNum(tenTaskConfig.getTargetNum());
                    taskInfo.setNowNum(0);
                    taskInfo.setState(0);

                    // 任务对应的奖励
                    FightTenTask.TaskInfo taskRewardInfo = gson.fromJson(tenTaskConfig.getRewards(),
                            FightTenTask.TaskInfo.class);
                    if (taskRewardInfo != null) {
                        taskInfo.setTaskRewards(new ArrayList<>());
                        taskInfo.getTaskRewards().addAll(taskRewardInfo.getTaskRewards());
                    }

                    fightTenTask.getTaskInfos().add(taskInfo);
                }
            }
            // 设置任务更新时间
            fightTenTask.setUpdateTime(LocalDate.now().toString());
            // 保存到redis
            RedisHelper.set(key, gson.toJson(fightTenTask));
        } else { // 判断任务是否是当天的，不是就要重置任务状态
            try {
                LocalDate updateDate = LocalDate.parse(fightTenTask.getUpdateTime());
                LocalDate localDate = LocalDate.now();
                if (localDate.compareTo(updateDate) != 0) { // 不是当天日期
                    for (FightTenTask.TaskInfo taskInfo : fightTenTask.getTaskInfos()) {
                        // 重置任务数据
                        taskInfo.reset();
                    }
                    fightTenTask.setUpdateTime(localDate.toString());
                    // 更新后的数据更新到redis
                    RedisHelper.set(key, gson.toJson(fightTenTask));
                }
            } catch (Exception e) {
                return fightTenTask;
            }
        }
        return fightTenTask;
    }

    /**
     * 获取玩家任务列表
     *
     * @param user 玩家
     */
    public void getTaskList(ServerUser user) {
        FightTenTask fightTenTask = getUserTask(user.getId());
        sendTaskListResponse(fightTenTask, user);
    }

    /**
     * 玩家获取对应任务奖励
     *
     * @param taskId 任务id
     * @param user   玩家
     */
    public void getTaskReward(long taskId, ServerUser user) {
        FightTenTask userTask = getUserTask(user.getId());
        List<FightTenTask.TaskInfo> taskInfos = userTask.getTaskInfos();
        for (FightTenTask.TaskInfo taskInfo : taskInfos) {
            if (taskInfo.getTaskId() == taskId) {
                if (taskInfo.getState() == 0) { // 未完成
                    NetManager.sendErrorMessageToClient("该任务还未完成！", user);
                } else if (taskInfo.getState() == 1) { // 已完成还未领取
                    taskInfo.setState(2); // 设置为已领取
                    List<FightTenTask.TaskReward> taskRewards = taskInfo.getTaskRewards();

                    OseeExpendLogEntity log = new OseeExpendLogEntity();
                    log.setUserId(user.getId());
                    log.setNickname(user.getNickname());
                    log.setPayType(2);
                    for (FightTenTask.TaskReward taskReward : taskRewards) {
                        // 为玩家增加奖励物品
                        PlayerManager.addItem(user, taskReward.getItemId(), taskReward.getItemNum(),
                                ItemChangeReason.TASK_FINISH, true);

                        ItemId itemId = ItemId.getItemIdById(taskReward.getItemId());
                        if (itemId == ItemId.MONEY) {
                            log.setMoney(log.getMoney() + taskReward.getItemNum());
                        } else if (itemId == ItemId.LOTTERY) {
                            log.setLottery(log.getLottery() + taskReward.getItemNum());
                        } else if (itemId == ItemId.DIAMOND) {
                            log.setDiamond(log.getLottery() + taskReward.getItemNum());
                        }
                    }
                    expendLogMapper.save(log); // 保存支出日志
                    // 给玩家发送金币等物品数量响应
                    PlayerManager.sendPlayerMoneyResponse(user);
                    // 发送更新后的任务列表
                    sendTaskListResponse(userTask, user);
                    // 发送领取响应
                    NetManager.sendHintMessageToClient("领取任务奖励成功！", user);
                    // 更新到redis
                    RedisHelper.set(TASK_REDIS_KEY_PREFIX + user.getId(), new Gson().toJson(userTask));

                    logger.info("拼十任务:玩家[{}]领取了任务[{}]的奖励", user.getId(), taskId);
                } else { // 已领取
                    NetManager.sendErrorMessageToClient("你已领取该任务奖励！", user);
                }
                break;
            }
        }
    }

    /**
     * 做任务
     *
     * @param cardType       牌型
     * @param fightTenPlayer 拼十玩家
     */
    public void doTask(int cardType, FightTenPlayer fightTenPlayer) {
        if (fightTenPlayer instanceof FightTenRobotPlayer) { // 机器人不做任务
            return;
        }
        long playerId = fightTenPlayer.getId();
        FightTenTask fightTenTask = getUserTask(playerId);
        List<FightTenTask.TaskInfo> taskInfos = fightTenTask.getTaskInfos();
        for (FightTenTask.TaskInfo taskInfo : taskInfos) {
            Long taskId = taskInfo.getTaskId();
            if ((cardType == FightTenCardManager.CARD_TYPE_ST2 && taskId == 1) || // 十带二，二龙戏珠
                    (cardType == FightTenCardManager.CARD_TYPE_ST9 && taskId == 2) || // 十带九，九九同心
                    (cardType == FightTenCardManager.CARD_TYPE_ST7 && taskId == 3) || // 十带七，七星高照
                    (cardType == FightTenCardManager.CARD_TYPE_ST1 && taskId == 4) // 十带一，一帆风顺
            ) {
                Integer targetNum = taskInfo.getTargetNum();
                Integer nowNum = taskInfo.getNowNum();
                if (nowNum < targetNum) { // 任务未完成
                    nowNum++;
                    taskInfo.setNowNum(nowNum);
                    if (nowNum >= targetNum) {
                        taskInfo.setNowNum(targetNum);
                        // 设置任务为已完成但未领取
                        taskInfo.setState(1);
                    }
                    logger.info("拼十任务:玩家[{}]的进行了任务[{}],进度为[{}/{}]", playerId, taskId, nowNum, targetNum);
                    // 更新到redis
                    String key = TASK_REDIS_KEY_PREFIX + playerId;
                    RedisHelper.set(key, new Gson().toJson(fightTenTask));
                }
                break;
            }
        }
    }

    /**
     * 发送任务列表响应
     *
     * @param fightTenTask 任务
     * @param user         玩家
     */
    private void sendTaskListResponse(FightTenTask fightTenTask, ServerUser user) {
        OseeFightTenMessage.TenTaskListResponse.Builder builder = OseeFightTenMessage.TenTaskListResponse.newBuilder();
        for (FightTenTask.TaskInfo taskInfo : fightTenTask.getTaskInfos()) {
            // 获取id对应任务的信息
            TenTaskConfig tenTaskConfig = DataContainer.getData(taskInfo.getTaskId(), TenTaskConfig.class);
            if (tenTaskConfig != null) {
                OseeFightTenMessage.TenTaskInfoProto.Builder taskInfoProto = OseeFightTenMessage.TenTaskInfoProto
                        .newBuilder();
                taskInfoProto.setTaskId(tenTaskConfig.getId());
                taskInfoProto.setTaskName(tenTaskConfig.getName());
                taskInfoProto.setTaskInfo(tenTaskConfig.getInfo());
                taskInfoProto.setTargetNum(taskInfo.getTargetNum());
                taskInfoProto.setNowNum(taskInfo.getNowNum());
                taskInfoProto.setTaskState(taskInfo.getState());
                // 获取任务奖励详情
                List<FightTenTask.TaskReward> taskRewards = taskInfo.getTaskRewards();
                if (taskRewards != null) {
                    for (FightTenTask.TaskReward taskReward : taskRewards) {
                        OseePublicData.ItemDataProto.Builder itemDataProto = OseePublicData.ItemDataProto.newBuilder();
                        itemDataProto.setItemId(taskReward.getItemId());
                        itemDataProto.setItemNum(taskReward.getItemNum());
                        taskInfoProto.addRewards(itemDataProto);
                    }
                }
                builder.addTaskInfos(taskInfoProto);
            }
        }
        // 发送给用户
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_OSEE_TEN_TASK_LIST_RESPONSE_VALUE, builder, user);
    }

}
