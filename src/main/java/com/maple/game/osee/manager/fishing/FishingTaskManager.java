package com.maple.game.osee.manager.fishing;

import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.container.DataContainer;
import com.maple.engine.data.ServerUser;
import com.maple.engine.manager.GsonManager;
import com.maple.engine.utils.DateUtils;
import com.maple.game.osee.dao.log.entity.OseeExpendLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeExpendLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.FishingGameRoom;
import com.maple.game.osee.entity.fishing.csv.file.TaskConfig;
import com.maple.game.osee.entity.fishing.game.FishingTaskStruct;
import com.maple.game.osee.entity.fishing.task.GoalType;
import com.maple.game.osee.entity.fishing.task.TaskType;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.OseePublicData;
import com.maple.game.osee.proto.OseePublicData.ItemDataProto;
import com.maple.game.osee.proto.fishing.OseeFishingMessage;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage;
import com.maple.game.osee.util.CommonUtil;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGameRoom;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 捕鱼相关任务管理类
 */
@Component
public class FishingTaskManager {

    private static final Logger logger = LoggerFactory.getLogger(FishingTaskManager.class);

    /**
     * 每日任务Redis命名空间
     */
    private static final String TASK_DAILY_NAMESPACE = "Fishing:Task:Daily:%s:%s";

    /**
     * 每日任务活跃度Redis保存命名空间
     */
    private static final String TASK_DAILY_ACTIVEREWARD_NAMESPACE = "Fishing:Task:Daily:ActiveReward:%s:%s";

    /**
     * 房间任务Redis命名空间
     */
    private static final String TASK_ROOM_NAMESPACE = "Fishing:Task:Room:%s:%s";

    private static OseeExpendLogMapper expendLogMapper;

    /**
     * 玩家每日任务列表
     */
    private static Map<Long, List<FishingTaskStruct>> userDailyTaskMap = new HashMap<>();

    /**
     * 玩家房间任务列表
     */
    private static Map<Long, List<FishingTaskStruct>> userRoomTaskMap = new HashMap<>();

    @Autowired
    public FishingTaskManager(OseeExpendLogMapper expendLogMapper) {
        FishingTaskManager.expendLogMapper = expendLogMapper;
    }

    /**
     * 获取玩家当日任务活跃度
     */
    public static int getDailyActiveLevel(ServerUser user) {
        List<FishingTaskStruct> dailyTaskList = getDailyTaskList(user);
        int level = 0;
        for (FishingTaskStruct taskStruct : dailyTaskList) {
//            if (taskStruct.getProgress() == taskStruct.getTarget() ||
//                    taskStruct.isReceived()) { // 任务完成了或领取了之后才算活跃度
            if (taskStruct.isReceived()) {
                level += taskStruct.getActiveLevel();
            }
        }
        return level;
    }

    /**
     * 获取每日任务列表
     */
    public static List<FishingTaskStruct> getDailyTaskList(ServerUser user) {
        // 从内存加载任务
        List<FishingTaskStruct> taskStructList = userDailyTaskMap.get(user.getId());
        if (taskStructList == null) {
            String pattern = String.format(TASK_DAILY_NAMESPACE, Long.toString(user.getId()), "*");
            Map<String, String> taskMap = RedisHelper.getPatternMap(pattern);

            taskStructList = new LinkedList<>();
            boolean makeNewTask = true;
            if (taskMap.size() > 0) {
                List<String> values = new ArrayList<>(taskMap.values());
                FishingTaskStruct fishingTaskStruct = GsonManager.gson.fromJson(values.get(0), FishingTaskStruct.class);
                if (DateUtils.isSameDay(fishingTaskStruct.getCreateTime(), new Date())) {
                    // 每日任务还未过期就不需要生成新任务
                    makeNewTask = false;
                    for (String value : values) {
                        taskStructList.add(GsonManager.gson.fromJson(value, FishingTaskStruct.class));
                    }
                }
            }
            if (makeNewTask) {
                getNewTask(user, TaskType.DAILY, taskStructList);
            }
            // 放入内存
            userDailyTaskMap.put(user.getId(), taskStructList);
        } else {
            boolean makeNewTask = true;
            if (taskStructList.size() > 0) {
                if (DateUtils.isSameDay(taskStructList.get(0).getCreateTime(), new Date())) {
                    // 每日任务还未过期就不需要生成新任务
                    makeNewTask = false;
                } else {
                    taskStructList.clear(); // 清空任务重新生成
                }
            }
            if (makeNewTask) {
                getNewTask(user, TaskType.DAILY, taskStructList);
            }
        }
        return taskStructList;
    }

    /**
     * 获取房间任务列表(暂时只有一个任务)
     */
    public static List<FishingTaskStruct> getRoomTaskList(ServerUser user) {
        List<FishingTaskStruct> taskStructList = userRoomTaskMap.get(user.getId());
        if (taskStructList == null) { // 内存没有任务数据，重新生成
            String pattern = String.format(TASK_ROOM_NAMESPACE, Long.toString(user.getId()), "*");
            Map<String, String> taskMap = RedisHelper.getPatternMap(pattern);

            taskStructList = new LinkedList<>();

            boolean makeNewTask = true;
            if (taskMap.size() > 0) {
                // 直接读取已经有的任务 房间任务只有一个
                FishingTaskStruct taskStruct = GsonManager.gson.fromJson(taskMap.values().iterator().next(), FishingTaskStruct.class);
                if (taskStruct.isReceived()) { // 房间任务已经领取了就要重新生成新的任务
                    makeNewTask = true;
                } else {
                    makeNewTask = false;
                    taskStructList.add(taskStruct);
                }
            }
            if (makeNewTask) { // 新生成任务
                getNewTask(user, TaskType.ROOM, taskStructList);
            }

            // 放入内存
            userRoomTaskMap.put(user.getId(), taskStructList);
        } else {
            boolean makeNewTask = true;
            if (taskStructList.size() > 0) {
                if (taskStructList.get(0).isReceived()) {
                    taskStructList.clear(); // 已经领取新生成任务
                } else {
                    makeNewTask = false;
                }
            }
            if (makeNewTask) { // 新生成任务
                getNewTask(user, TaskType.ROOM, taskStructList);
            }
        }
        return taskStructList;
    }

    /**
     * 获取新任务
     */
    private static void getNewTask(ServerUser user, TaskType taskType, List<FishingTaskStruct> taskList) {
        List<TaskConfig> taskConfigs = DataContainer.getDatas(TaskConfig.class)
                .stream()
                .filter(taskConfig -> taskConfig.getTaskType() == taskType.getId())
                .collect(Collectors.toList());
//        String pattern;
        if (taskType == TaskType.ROOM) {
            Collections.shuffle(taskConfigs);
            String pattern = String.format(TASK_ROOM_NAMESPACE, Long.toString(user.getId()), "*");
            RedisHelper.removePattern(pattern); // 移除旧的房间任务
        }
//        } else {
//            pattern = String.format(TASK_DAILY_NAMESPACE, Long.toString(user.getId()), "*");
//        }
        // 领取新任务时之前的要删除掉
        for (TaskConfig taskConfig : taskConfigs) {
            FishingTaskStruct taskStruct = new FishingTaskStruct();
            taskStruct.setId(taskConfig.getId());
            taskStruct.setProgress(0);
            taskStruct.setTarget(taskConfig.getGoalNum());
            taskStruct.setActiveLevel(taskConfig.getActiveLevel());
            taskStruct.setReceived(false);
            taskStruct.setCreateTime(new Date());

            List<ItemData> realRewards = taskConfig.getRealRewards();
            Collections.shuffle(realRewards);
            List<ItemData> taskRewards = new ArrayList<>();
            taskRewards.add(realRewards.get(0)); // 只随机选取一个奖励
            taskStruct.setTaskRewards(taskRewards);

            // 放入任务列表
            taskList.add(taskStruct);

            String key = String.format(
                    taskType == TaskType.ROOM ? TASK_ROOM_NAMESPACE : TASK_DAILY_NAMESPACE,
                    Long.toString(user.getId()),
                    Long.toString(taskConfig.getId())
            );
            String value = GsonManager.gson.toJson(taskStruct);
            // 存入Redis
            RedisHelper.set(key, value);

            if (taskType == TaskType.ROOM) { // 房间任务只领取一个就行了
                break;
            }
        }
    }

    /**
     * 做任务
     */
    public static void doTask(ServerUser user, TaskType taskType, GoalType goalType, int goalId, int size) {
        if (StringUtils.isEmpty(user.getConnect())) { // 机器人不做任务
            return;
        }

        List<FishingTaskStruct> taskList;
        if (taskType == TaskType.DAILY) {
            taskList = getDailyTaskList(user);
        } else {
            taskList = getRoomTaskList(user);
        }
        for (FishingTaskStruct taskStruct : taskList) {
            if (taskStruct.isReceived() || taskStruct.getProgress() >= taskStruct.getTarget()) { // 已领取或已完成就不管
                continue;
            }

            TaskConfig config = DataContainer.getData(taskStruct.getId(), TaskConfig.class);

//            logger.info("玩家[{}]做任务[{}][{}][goalId:{}]", user.getNickname(), config.getInfo(),
//                    config.getRealGoalId().contains(goalId), goalId);

            // 玩家做任务
            if (config.getGoalType() == goalType.getId() && config.getRealGoalId().contains(goalId)) {
                int target = taskStruct.getTarget();
                int nowProgress = taskStruct.getProgress() + size;
                if (nowProgress > target) {
                    nowProgress = target;
                }
                taskStruct.setProgress(nowProgress);
                String key = String.format(
                        taskType == TaskType.ROOM ? TASK_ROOM_NAMESPACE : TASK_DAILY_NAMESPACE,
                        Long.toString(user.getId()),
                        Long.toString(taskStruct.getId())
                );
                // 更新redis任务数据
                RedisHelper.set(key, GsonManager.gson.toJson(taskStruct));
                if (taskType == TaskType.ROOM) { // 房间任务要实时更新任务进度给前端
                    sendRoomTaskListResponse(user);
                }
//                logger.info("玩家[{}]做了{}任务[{}({}/{})]", user.getNickname(),
//                        taskType == TaskType.DAILY ? "每日" : "房间", config.getName(),
//                        taskStruct.getProgress(), taskStruct.getTarget()
//                );
            }
        }
    }

    /**
     * 领取任务奖励
     */
    public static void getTaskReward(ServerUser user, TaskType taskType, long taskId) {
        String key;
        Optional<FishingTaskStruct> taskList;
        if (taskType == TaskType.DAILY) {
            key = String.format(TASK_DAILY_NAMESPACE, Long.toString(user.getId()), Long.toString(taskId));
            taskList = getDailyTaskList(user).stream()
                    .filter(taskStruct -> taskStruct.getId() == taskId)
                    .findFirst();
        } else {
            key = String.format(TASK_ROOM_NAMESPACE, Long.toString(user.getId()), Long.toString(taskId));
            taskList = getRoomTaskList(user).stream()
                    .filter(taskStruct -> taskStruct.getId() == taskId)
                    .findFirst();
        }

//        String taskStr = RedisHelper.get(key);
//        if (StringUtils.isEmpty(taskStr)) {
        if (!taskList.isPresent()) {
            NetManager.sendErrorMessageToClient("该任务不存在", user);
            return;
        }

        FishingTaskStruct task = taskList.get();//GsonManager.gson.fromJson(taskStr, FishingTaskStruct.class);

        if (task.isReceived()) {
            NetManager.sendErrorMessageToClient("该任务奖励已领取", user);
            return;
        }

        if (task.getProgress() < task.getTarget()) {
            NetManager.sendErrorMessageToClient("任务未完成", user);
            return;
        }

        OseeExpendLogEntity log = new OseeExpendLogEntity();
        log.setUserId(user.getId());
        log.setNickname(user.getNickname());
        log.setPayType(2);

        int rewardMulti = 1; // 任务奖励翻倍倍数
        if (taskType == TaskType.ROOM) { // 房间任务奖励要根据对应场次解锁奖励倍数
            BaseGameRoom gameRoom = GameContainer.getGameRoomByPlayerId(user.getId());
            if (gameRoom instanceof FishingGameRoom) {
                int roomIndex = ((FishingGameRoom) gameRoom).getRoomIndex();
                if (roomIndex == 2) { // 大洋秘境场次
                    int[] multi = {20, 80};
                    rewardMulti = CommonUtil.getWheelRandom(multi) + 1;
                } else if (roomIndex == 3) { // 深海宝藏场次
                    int[] multi = {10, 20, 70};
                    rewardMulti = CommonUtil.getWheelRandom(multi) + 1;
                } else if (roomIndex == 4) { // 惊涛骇浪场次
                    int[] multi = {10, 20, 30, 40};
                    rewardMulti = CommonUtil.getWheelRandom(multi) + 1;
                }
            }
        }

        List<ItemDataProto> itemDataProtoList = new LinkedList<>();
        for (ItemData reward : task.getTaskRewards()) {
            int rewardItemId = reward.getItemId();
            long rewardCount = reward.getCount() * rewardMulti; // 乘上翻倍倍数
            PlayerManager.addItem(user, rewardItemId, rewardCount, ItemChangeReason.TASK_FINISH, true);
            itemDataProtoList.add(ItemDataProto.newBuilder()
                    .setItemId(rewardItemId)
                    .setItemNum(rewardCount)
                    .build());

            if (rewardItemId == ItemId.MONEY.getId()) {
                log.setMoney(log.getMoney() + rewardCount);
            } else if (rewardItemId == ItemId.LOTTERY.getId()) {
                log.setLottery(log.getLottery() + rewardCount);
            } else if (rewardItemId == ItemId.DIAMOND.getId()) {
                log.setDiamond(log.getLottery() + rewardCount);
            } else if (rewardItemId == ItemId.SKILL_LOCK.getId()) { // TODO 任务奖励技能支出记录
            } else if (rewardItemId == ItemId.SKILL_FROZEN.getId()) {
            } else if (rewardItemId == ItemId.SKILL_FAST.getId()) {
            } else if (rewardItemId == ItemId.SKILL_CRIT.getId()) {
            }
        }
        expendLogMapper.save(log); // 保存支出日志

        task.setReceived(true);
        RedisHelper.set(key, GsonManager.gson.toJson(task));

        if (taskType == TaskType.DAILY) {
            OseeLobbyMessage.GetDailyTaskRewardResponse.Builder builder = OseeLobbyMessage.GetDailyTaskRewardResponse.newBuilder();
            builder.addAllRewards(itemDataProtoList);
            NetManager.sendMessage(OseeMsgCode.S_C_TTMY_GET_DAILY_TASK_REWARD_RESPONSE_VALUE, builder, user);
        } else {
            // 领取房间任务奖励响应 房间任务领取之后会发送新的房间任务
            OseeFishingMessage.FishingGetRoomTaskRewardResponse.Builder builder = OseeFishingMessage.FishingGetRoomTaskRewardResponse.newBuilder();
            builder.addAllRewards(itemDataProtoList);
            builder.setRewardMulti(rewardMulti);
            NetManager.sendMessage(OseeMsgCode.S_C_OSEE_FISHING_GET_ROOM_TASK_REWARD_RESPONSE_VALUE, builder, user);
            sendRoomTaskListResponse(user);
        }
    }

    /**
     * 获取活跃度奖励
     */
    public static void getActiveReward(ServerUser user, int activeLevel) {
        int dailyActiveLevel = getDailyActiveLevel(user);
        if (dailyActiveLevel < activeLevel) {
            NetManager.sendHintMessageToClient("活跃度不够", user);
            return;
        }

        String key = String.format(TASK_DAILY_ACTIVEREWARD_NAMESPACE, Long.toString(user.getId()), Integer.toString(activeLevel));
        String value = RedisHelper.get(key);
        if (!StringUtils.isEmpty(value)) {
            if (LocalDate.parse(value).isEqual(LocalDate.now())) { // 同一天
                NetManager.sendErrorMessageToClient("你已领取该活跃度奖励", user);
                return;
            }
        }

        int itemId = 0;
        long itemNum = 0;
        if (activeLevel == 30) { // 奖励随机道具卡5张
            itemId = ThreadLocalRandom.current().nextInt(ItemId.SKILL_LOCK.getId(), ItemId.SKILL_CRIT.getId() + 1);
            itemNum = 5;
        } else if (activeLevel == 60) { // 奖励10000金币
            itemId = ItemId.MONEY.getId();
            itemNum = 10000;
        } else if (activeLevel == 100) { // 奖励20000金币
            itemId = ItemId.MONEY.getId();
            itemNum = 20000;
        } else if (activeLevel == 150) { // 奖励50000金币
            itemId = ItemId.MONEY.getId();
            itemNum = 50000;
        }
        PlayerManager.addItem(user, itemId, itemNum, ItemChangeReason.TASK_FINISH, true);

        // 保存今天的活跃度领取记录到Redis
        RedisHelper.set(key, LocalDate.now().toString());

        OseeLobbyMessage.GetDailyActiveRewardResponse.Builder builder = OseeLobbyMessage.GetDailyActiveRewardResponse.newBuilder();
        builder.addRewards(ItemDataProto.newBuilder()
                .setItemId(itemId)
                .setItemNum(itemNum)
                .build());
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_GET_DAILY_ACTIVE_REWARD_RESPONSE_VALUE, builder, user);
    }

    /**
     * 创建任务协议信息
     */
    public static OseePublicData.TaskInfoProto createTaskInfoProto(FishingTaskStruct taskStruct) {
        TaskConfig taskConfig = DataContainer.getData(taskStruct.getId(), TaskConfig.class);
        OseePublicData.TaskInfoProto.Builder builder = OseePublicData.TaskInfoProto.newBuilder();
        builder.setTaskId(taskStruct.getId());
        builder.setName(taskConfig.getName());
        builder.setInfo(taskConfig.getInfo());
        builder.setActive(taskConfig.getActiveLevel());
        builder.setProgress(taskStruct.getProgress());
        builder.setTarget(taskStruct.getTarget());
        builder.setReceive(taskStruct.isReceived());
        for (ItemData taskReward : taskStruct.getTaskRewards()) {
            builder.addRewards(ItemDataProto.newBuilder()
                    .setItemId(taskReward.getItemId())
                    .setItemNum(taskReward.getCount())
                    .build());
        }
        return builder.build();
    }

    /**
     * 发送每日任务列表响应
     */
    public static void sendDailyTaskListResponse(ServerUser user) {
        List<FishingTaskStruct> dailyTaskList = getDailyTaskList(user);
        int dailyActiveLevel = getDailyActiveLevel(user); // 当日总活跃度
        OseeLobbyMessage.DailyTaskListResponse.Builder builder = OseeLobbyMessage.DailyTaskListResponse.newBuilder();
        builder.setTotalActive(dailyActiveLevel);
        // 活跃度详情
        Integer[] activeLevel = new Integer[]{30, 60, 100, 150};
        for (Integer active : activeLevel) {
            String key = String.format(TASK_DAILY_ACTIVEREWARD_NAMESPACE, Long.toString(user.getId()), Integer.toString(active));
            String value = RedisHelper.get(key);
            boolean receive = false;
            if (!StringUtils.isEmpty(value)) {
                if (LocalDate.parse(value).isEqual(LocalDate.now())) { // 今日已领取
                    receive = true;
                }
            }
            builder.addActive(OseeLobbyMessage.ActiveLevelInfoProto.newBuilder()
                    .setActiveLevel(active)
                    .setReceive(receive)
                    .build());
        }
        // 任务详情
        for (FishingTaskStruct taskStruct : dailyTaskList) {
            builder.addTasks(createTaskInfoProto(taskStruct));
        }
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_DAILY_TASK_LIST_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送房间任务信息响应
     */
    public static void sendRoomTaskListResponse(ServerUser user) {
        List<FishingTaskStruct> taskList = getRoomTaskList(user);

        OseeFishingMessage.FishingRoomTaskListResponse.Builder builder = OseeFishingMessage.FishingRoomTaskListResponse.newBuilder();
        // 房间任务详情
        for (FishingTaskStruct taskStruct : taskList) {
            builder.addTaskInfos(createTaskInfoProto(taskStruct));
        }
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_FISHING_ROOM_TASK_LIST_RESPONSE_VALUE, builder, user);
    }

    /**
     * 一键领取所有完成的任务奖励
     */
    public static void oneKeyGetDailyTaskRewards(ServerUser user) {
        List<FishingTaskStruct> taskList = getDailyTaskList(user);
        Map<Integer, Long> rewards = new HashMap<>();
        for (FishingTaskStruct task : taskList) {
            if (task.getProgress() == task.getTarget() && !task.isReceived()) { // 已完成但还没有领取
                // 领取奖励
                String key = String.format(TASK_DAILY_NAMESPACE, Long.toString(user.getId()), Long.toString(task.getId()));

                OseeExpendLogEntity log = new OseeExpendLogEntity();
                log.setUserId(user.getId());
                log.setNickname(user.getNickname());
                log.setPayType(2);

                for (ItemData reward : task.getTaskRewards()) {
                    int rewardItemId = reward.getItemId();
                    long rewardNum = reward.getCount();
                    PlayerManager.addItem(user, rewardItemId, rewardNum, ItemChangeReason.TASK_FINISH, true);
                    rewards.put(rewardItemId, rewards.getOrDefault(rewardItemId, 0L) + rewardNum); // 合并相同的奖励物品返回给前端

                    ItemId itemId = ItemId.getItemIdById(reward.getItemId());
                    if (itemId == ItemId.MONEY) {
                        log.setMoney(log.getMoney() + rewardNum);
                    } else if (itemId == ItemId.LOTTERY) {
                        log.setLottery(log.getLottery() + rewardNum);
                    } else if (itemId == ItemId.DIAMOND) {
                        log.setDiamond(log.getLottery() + rewardNum);
                    }
//                    else if (itemId == ItemId.SKILL_LOCK) { // TODO 任务奖励技能支出记录
//                    } else if (itemId == ItemId.SKILL_FROZEN) {
//                    } else if (itemId == ItemId.SKILL_FAST) {
//                    } else if (itemId == ItemId.SKILL_CRIT) {
//                    }
                }
                expendLogMapper.save(log); // 保存支出日志

                task.setReceived(true);
                RedisHelper.set(key, GsonManager.gson.toJson(task));
            }
        }
        if (rewards.size() <= 0) {
            NetManager.sendHintMessageToClient("你还没有已完成的每日任务哦，快去做任务吧~", user);
            return;
        }
        OseeLobbyMessage.OneKeyGetDailyTaskRewardsResponse.Builder builder = OseeLobbyMessage.OneKeyGetDailyTaskRewardsResponse.newBuilder();
        for (Map.Entry entry : rewards.entrySet()) {
            builder.addRewards(ItemDataProto.newBuilder()
                    .setItemId((Integer) entry.getKey())
                    .setItemNum((Long) entry.getValue())
                    .build());
        }
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_ONE_KEY_GET_DAILY_TASK_REWARDS_RESPONSE_VALUE, builder, user);
        sendDailyTaskListResponse(user);
    }
}
