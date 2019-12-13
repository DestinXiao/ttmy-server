package com.maple.game.osee.manager.fishing;

import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.entity.UserEntity;
import com.maple.engine.container.DataContainer;
import com.maple.engine.data.ServerUser;
import com.maple.engine.manager.GsonManager;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.FishingGameRoom;
import com.maple.game.osee.entity.fishing.csv.file.BatteryLevelConfig;
import com.maple.game.osee.entity.fishing.csv.file.FishConfig;
import com.maple.game.osee.entity.fishing.csv.file.FishRefreshRule;
import com.maple.game.osee.entity.fishing.csv.file.PlayerLevelConfig;
import com.maple.game.osee.entity.fishing.game.FireStruct;
import com.maple.game.osee.entity.fishing.game.FishStruct;
import com.maple.game.osee.entity.robot.fishing.FishingGameRobot;
import com.maple.game.osee.manager.BaseRobotManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.fishing.OseeFishingMessage;
import com.maple.game.osee.proto.fishing.OseeFishingMessage.FishingChangeBatteryLevelResponse;
import com.maple.game.osee.proto.fishing.OseeFishingMessage.FishingRobotFireResponse;
import com.maple.game.osee.proto.fishing.OseeFishingMessage.FishingUseSkillResponse;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 1688捕鱼机器人管理类
 */
@Component
public class FishingRobotManager extends BaseRobotManager {

    /**
     * 机器人生成金币限制
     */
    private final long[][] MONEY_LIMIT = {{100000, 1000000}, {500000, 2000000}, {3000000, 10000000}, {8000000, 30000000}};

    /**
     * 机器人开关
     */
    public static int USE_ROBOT = 0;

    /**
     * 机器人数量
     */
    public static int ROBOT_COUNT = 1;

    /**
     * 刷新机器人阈值
     */
    public int[] REFRESH_TIME = {10, 10};

    /**
     * 机器人消失阈值
     */
    public int[] DISAPPEAR_TIME = {10, 10};

    @Autowired
    private FishingManager fishingManager;

    /**
     * 创建捕鱼机器人
     */
    public void createFishingRobot(FishingGameRoom gameRoom) {
        FishingGameRobot robotPlayer = createRobotPlayer(gameRoom);
        if (robotPlayer != null) {
            FishingManager.sendRoomMessage(gameRoom,
                    OseeMsgCode.S_C_OSEE_FISHING_PLAYER_INFO_RESPONSE_VALUE,
                    FishingManager.createPlayerInfoResponse(gameRoom, robotPlayer));
            // 机器人自动发炮的定时任务
            ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> robotFire(gameRoom, robotPlayer.getId()), 3, TimeUnit.SECONDS);
        }
    }

    /**
     * 创建一个机器人游戏玩家
     */
    public FishingGameRobot createRobotPlayer(FishingGameRoom gameRoom) {
        if (gameRoom.getMaxSize() - gameRoom.getPlayerSize() < 1) { // 人员已满
            return null;
        }
        ServerUser user = new ServerUser();
        user.setEntity(new UserEntity());
        user.getEntity().setId(getNewRobotId());
        user.getEntity().setNickname(getRobotName());
        String headUrl = getRandomHeadUrl();
        if (StringUtils.isEmpty(headUrl)) {
            user.getEntity().setHeadIndex(ThreadLocalRandom.current().nextInt(1, 21));
        } else {
            user.getEntity().setHeadIndex(0);
            user.getEntity().setHeadUrl(headUrl);
        }

        FishingGameRobot robot = GameContainer.createGamePlayer(gameRoom, user, FishingGameRobot.class, true);
        long[] limit = MONEY_LIMIT[gameRoom.getRoomIndex() - 1];
        robot.setMoney(ThreadLocalRandom.current().nextLong(limit[0], limit[1]));
        robot.setBatteryLevel(FishingManager.batteryLevelLimit[gameRoom.getRoomIndex() - 1][0]);
        // 机器人随机等级
        PlayerLevelConfig levelConfig = DataContainer.getRandomData(PlayerLevelConfig.class);
        robot.setLevel(levelConfig.getLevel());
        return robot;
    }

    /**
     * 移除捕鱼机器人
     */
    public void removeFishingRobot(FishingGameRoom gameRoom, FishingGameRobot robot) {
        if (gameRoom.getLastRefreshRobotTime() + REFRESH_TIME[0] < System.currentTimeMillis()) {
            gameRoom.setLastRefreshRobotTime(System.currentTimeMillis() - (REFRESH_TIME[0] + 10) * 1000);
        }
        fishingManager.exitFishingRoom(gameRoom, robot.getUser());
    }

    /**
     * 机器人发射子弹
     */
    public void robotFire(FishingGameRoom gameRoom, long robotId) {
        // 判断房间是否存在
        if (GameContainer.getGameRoomByCode(gameRoom.getCode()) == null) {
            return;
        }
        FishingGameRobot robot = gameRoom.getGamePlayerById(robotId);

        if (ThreadLocalRandom.current().nextInt(1000) < robot.getChangeBatteryLevelProb()) {
            // 更换机器人炮台外观
            if (ThreadLocalRandom.current().nextBoolean()) {
                int roomIndex = gameRoom.getRoomIndex();
                int viewIndex = ThreadLocalRandom.current().nextInt(0, (roomIndex <= 2 ? 2 : (roomIndex == 3 ? 3 : 4)) + 1);
                robot.setViewIndex(viewIndex);
                OseeFishingMessage.FishingChangeBatteryViewResponse.Builder builder1 = OseeFishingMessage.FishingChangeBatteryViewResponse.newBuilder();
                builder1.setPlayerId(robotId);
                builder1.setViewIndex(viewIndex);
                FishingManager.sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_CHANGE_BATTERY_VIEW_RESPONSE_VALUE, builder1.build());
            }

            // 更换炮台倍数
            List<Integer> levels = new LinkedList<>();
            for (BatteryLevelConfig config : DataContainer.getDatas(BatteryLevelConfig.class)) {
                if (config.getScene() == gameRoom.getRoomIndex()) {
                    levels.add(config.getBatteryLevel());
                }
            }
            Collections.shuffle(levels);
            robot.setBatteryLevel(levels.get(0));

            FishingChangeBatteryLevelResponse.Builder builder = FishingChangeBatteryLevelResponse.newBuilder();
            builder.setPlayerId(robot.getId());
            builder.setLevel(robot.getBatteryLevel());
            int msgCode = OseeMsgCode.S_C_OSEE_FISHING_CHANGE_BATTERY_LEVEL_RESPONSE_VALUE;
            FishingManager.sendRoomMessage(gameRoom, msgCode, builder.build());

            robot.setChangeBatteryLevelProb(0);
        } else {
            robot.setChangeBatteryLevelProb(robot.getChangeBatteryLevelProb() + 1);
        }

        List<FishStruct> fishes = new LinkedList<>();

        FishStruct targetFish = gameRoom.getFishMap().get(robot.getLastFireFishId());

        // 10%的更换目标概率
        boolean changeTarget = ThreadLocalRandom.current().nextInt(1, 10 + 1) > 9;
        if (targetFish != null && !changeTarget) {
            fishes.add(targetFish);
        } else {
            // 选择房间分数最高的三条鱼，随机攻击其中一条
            for (FishStruct fish : gameRoom.getFishMap().values()) {
                if (fish.getClientLifeTime() > 15F) {
                    continue;
                }

                if (fishes.size() >= 3) {
                    int lastIndex = fishes.size() - 1;
                    FishConfig config = DataContainer.getData(fish.getConfigId(), FishConfig.class);
                    FishConfig lastConfig = DataContainer.getData(fishes.get(lastIndex).getConfigId(),
                            FishConfig.class);

                    if (config.getMoney() > lastConfig.getMoney()) { // 当前鱼比目标鱼表最后一条分高，替换目标鱼
                        fishes.remove(lastIndex);
                    } else {
                        continue;
                    }
                }
                addFish(fishes, fish);
            }
        }

        if (fishes.size() > 0) {
            long needMoney = robot.getBatteryLevel() * robot.getBatteryMult();
            if (robot.getMoney() < needMoney) {
                removeFishingRobot(gameRoom, robot);
                return;
            }
            // 随机一条鱼去攻击
            long fishId = fishes.get(ThreadLocalRandom.current().nextInt(fishes.size())).getId();
            robot.setLastFireFishId(fishId); // 记录之前攻击的那条鱼
            robot.setMoney(robot.getMoney() - needMoney);
            FireStruct fire = new FireStruct();
            fire.setFishId(fishId);
            fire.setId(gameRoom.getNextId());
            fire.setLevel(robot.getBatteryLevel());
            fire.setMult(robot.getBatteryMult());
            robot.getFireMap().put(fire.getId(), fire);

            FishingRobotFireResponse.Builder builder = FishingRobotFireResponse.newBuilder();
            builder.setRobotId(robotId);
            builder.setFireId(fire.getId());
            builder.setFishId(fire.getFishId());
            builder.setRestMoney(robot.getMoney());
            int msgCode = OseeMsgCode.S_C_OSEE_FISHING_ROBOT_FIRE_RESPONSE_VALUE;
            FishingManager.sendRoomMessage(gameRoom, msgCode, builder.build());
        }
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> robotFire(gameRoom, robotId), 300, TimeUnit.MILLISECONDS);
    }

    /**
     * 添加鱼到鱼列表中，并按金币排序
     */
    public void addFish(List<FishStruct> fishes, FishStruct fish) {
        for (int i = 0; i < fishes.size(); i++) {
            FishConfig config = DataContainer.getData(fish.getConfigId(), FishConfig.class);
            FishConfig innerConfig = DataContainer.getData(fishes.get(i).getConfigId(), FishConfig.class);

            if (config.getMoney() > innerConfig.getMoney()) {
                fishes.add(i, fish);
                return;
            }
        }
        fishes.add(fish);
    }

    /**
     * 机器人击中鱼
     */
    public void robotFightFish(FishingGameRoom gameRoom, long robotId, long fireId, List<Long> fishId, boolean boom) {
        FishingGameRobot robot = gameRoom.getGamePlayerById(robotId);
        if (robot != null) {
            List<FishConfig> configs = FishingManager.fightFish(gameRoom, robot, fireId, fishId, boom);

            if (configs != null) {
                for (FishConfig config : configs) {
                    if (config.getSkill() > 0 && config.getSkill() < 5) {
                        // 机器人打死技能鱼就直接使用技能了，不掉落物品给机器人
                        long nowTime = System.currentTimeMillis();

                        FishingUseSkillResponse.Builder builder = FishingUseSkillResponse.newBuilder();
                        builder.setPlayerId(robot.getId());

                        int skillId = 0;
                        if (config.getSkill() == 1) { // 锁定
                            skillId = ItemId.SKILL_LOCK.getId();

                            builder.setDuration((int) (FishingManager.SKILL_LOCK_TIME / 1000));
                        } else if (config.getSkill() == 2) { // 冰冻
                            skillId = ItemId.SKILL_FROZEN.getId();

                            builder.setDuration((int) (FishingManager.SKILL_FROZEN_TIME / 1000));
                            gameRoom.setLastRoomFrozenTime(nowTime);

                            for (Entry<FishRefreshRule, Long> refresh : gameRoom.getNextRefreshTime().entrySet()) {
                                refresh.setValue(refresh.getValue() + FishingManager.SKILL_FROZEN_TIME / 1000); // 延迟10秒刷鱼
                            }

                            long addTime = nowTime / 1000 - gameRoom.getLastRoomFrozenTime() / 1000;
                            long skillTime = FishingManager.SKILL_FROZEN_TIME / 1000;
                            addTime = addTime > skillTime ? skillTime : addTime;

                            for (FishStruct fish : gameRoom.getFishMap().values()) {
                                // 多加的5秒时为了防止过早的刷掉鱼
                                fish.setLifeTime(fish.getLifeTime() + addTime + 5); // 延长鱼的存在时间
                                fish.setNowLifeTime(fish.getClientLifeTime()); // 记录冰冻时鱼的存活时间
                                fish.setCreateTime(fish.getCreateTime() + (addTime + 5) * 1000); // 出生时间往后延迟
                            }
                            // 延迟鱼潮刷新时间 秒
                            gameRoom.setNextFishTideTime(gameRoom.getNextFishTideTime() + addTime + 5);
                        } else if (config.getSkill() == 3) { // 急速
                            skillId = ItemId.SKILL_FAST.getId();

                            builder.setDuration((int) (FishingManager.SKILL_FAST_TIME / 1000));
                        } else if (config.getSkill() == 4) { // 暴击
                            skillId = ItemId.SKILL_CRIT.getId();

                            builder.setDuration((int) (FishingManager.SKILL_CRIT_TIME / 1000));
                            robot.setLastCritTime(nowTime);
                        }
//                        else if (config.getSkill() == 5) { // 全屏炸弹鱼
//                            skillId = 100;
//
//                            long beforeMoney = robot.getMoney();
//
//                            List<FishStruct> fishes = new ArrayList<>(gameRoom.getFishMap().values());
//                            for (FishStruct fish : fishes) {
//                                robotFightFish(gameRoom, robot.getId(), fireId, Collections.singletonList(fish.getId()), true);
//                                builder.addFishIds(fish.getId());
//                            }
//
//                            builder.setDropMoney(robot.getMoney() - beforeMoney);
//                        }

                        builder.setSkillId(skillId);
                        builder.setRestMoney(robot.getMoney());

                        int msgCode = OseeMsgCode.S_C_OSEE_FISHING_USE_SKILL_RESPONSE_VALUE;
                        FishingManager.sendRoomMessage(gameRoom, msgCode, builder.build());
                    }
                }
            }
        }
    }

    /**
     * 初始化数据
     */
    public void init() {
        Map<String, String> settings = RedisHelper.getPatternMap("Fishing:Robot:*");

        for (Entry<String, String> setting : settings.entrySet()) {
            switch (setting.getKey()) {
                case "Fishing:Robot:UseRobot":
                    USE_ROBOT = GsonManager.gson.fromJson(setting.getValue(), int.class);
                    break;
                case "Fishing:Robot:RobotCount":
                    ROBOT_COUNT = GsonManager.gson.fromJson(setting.getValue(), int.class);
                    break;
                case "Fishing:Robot:RefreshTime":
                    REFRESH_TIME = GsonManager.gson.fromJson(setting.getValue(), int[].class);
                    break;
                case "Fishing:Robot:DisappearTime":
                    DISAPPEAR_TIME = GsonManager.gson.fromJson(setting.getValue(), int[].class);
                    break;
            }
        }
    }

    /**
     * 循环检查房间状态
     */
    @Scheduled(initialDelay = 8000, fixedRate = 1000)
    public void loopCheckRoom() {
        long nowTime = System.currentTimeMillis();
        List<FishingGameRoom> gameRooms = GameContainer.getGameRooms(FishingGameRoom.class);
        for (FishingGameRoom gameRoom : gameRooms) {
            List<BaseGamePlayer> players = Arrays.asList(gameRoom.getGamePlayers());

            players.forEach(player -> {
                if (player instanceof FishingGameRobot) {
                    FishingGameRobot robot = (FishingGameRobot) player;
                    int disappearTime = ThreadLocalRandom.current().nextInt(DISAPPEAR_TIME[0], DISAPPEAR_TIME[1] + 1);
                    if (robot.getRefreshTime() + disappearTime * 1000 < nowTime) {
                        removeFishingRobot(gameRoom, robot);
                    }
                }
            });

            if (players.stream().allMatch(player -> player == null || player instanceof FishingGameRobot)) { // 房间内全是机器人，删除房间
                GameContainer.removeGameRoom(gameRoom);
            } else if (gameRoom.getPlayerSize() < gameRoom.getMaxSize()) { // 房间人数小于最大人数，加入机器人
                if (USE_ROBOT == 0) { // 不使用机器人
                    return;
                }

                if (gameRoom.getPlayerSize() >= gameRoom.getMaxSize()) { // 房间人数已满
                    return;
                }

                long robotCount = players.stream().filter(player -> player instanceof FishingGameRobot).count();
                if (robotCount >= ROBOT_COUNT) { // 机器人刷新数量已达上限
                    return;
                }

                int waitTime = ThreadLocalRandom.current().nextInt(REFRESH_TIME[0], REFRESH_TIME[1] + 1);
                if (gameRoom.getLastRefreshRobotTime() + waitTime * 1000 > nowTime) {
                    return;
                }
                gameRoom.setLastRefreshRobotTime(nowTime);
                createFishingRobot(gameRoom);
            }
        }
    }

}
