package com.maple.game.osee.manager.fishing;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.GeneratedMessage;
import com.maple.common.lobby.proto.LobbyMessage;
import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.entity.UserEntity;
import com.maple.engine.container.DataContainer;
import com.maple.engine.data.ServerUser;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.dao.log.entity.OseeCutMoneyLogEntity;
import com.maple.game.osee.dao.log.entity.OseeFishingRecordLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeCutMoneyLogMapper;
import com.maple.game.osee.dao.log.mapper.OseeFishingRecordLogMapper;
import com.maple.game.osee.entity.GameEnum;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.FishingGamePlayer;
import com.maple.game.osee.entity.fishing.challenge.FishingChallengePlayer;
import com.maple.game.osee.entity.fishing.challenge.FishingChallengeRoom;
import com.maple.game.osee.entity.fishing.csv.file.FishConfig;
import com.maple.game.osee.entity.fishing.csv.file.FishGroupConfig;
import com.maple.game.osee.entity.fishing.csv.file.FishRefreshRule;
import com.maple.game.osee.entity.fishing.csv.file.FishRouteConfig;
import com.maple.game.osee.entity.fishing.game.FireStruct;
import com.maple.game.osee.entity.fishing.game.FishStruct;
import com.maple.game.osee.manager.AgentManager;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.manager.fishing.util.FishingUtil;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.OseePublicData;
import com.maple.game.osee.proto.fishing.TtmyFishingChallengeMessage;
import com.maple.game.osee.timer.AutoWanderSubtitle;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.data.BaseGameRoom;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.Boolean.compare;

/**
 * 捕鱼挑战赛管理类
 *
 * @author Junlong
 */
@Component
public class FishingChallengeManager {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OseePlayerMapper playerMapper;

    @Autowired
    private OseeFishingRecordLogMapper fishingRecordLogMapper;

    @Autowired
    private OseeCutMoneyLogMapper cutMoneyLogMapper;

    /**
     * 各个鱼雷的金币价值：青铜、白银、黄金
     */
    public static long[] TORPEDO_VALUE = {200000, 1000000, 2000000};

    @Autowired
    private AgentManager agentManager;
    private AtomicInteger count = new AtomicInteger();
    /**
     * 最多可建房间数
     */
    private static final int MAX_ROOM_NUM = 100;

    /**
     * 进入房间最小炮台等级
     */
    private static final int MIN_BATTERY_LEVEL = 500;

    /**
     * 房间最大炮台倍数等级
     */
    private static final int MAX_BATTERY_LEVEL = 5000;

    ArrayList<String> imageList = new ArrayList<>();

    /**
     * 记录掉落和使用的鱼雷数量的Map
     */
    public static ConcurrentHashMap<String, Long> TORPEDO_RECORD = new ConcurrentHashMap<>();

    public ConcurrentHashMap<ServerUser, Integer> room1 = new ConcurrentHashMap<>();
    public ConcurrentHashMap<ServerUser, Integer> room2 = new ConcurrentHashMap<>();
    public ConcurrentHashMap<ServerUser, Integer> room3 = new ConcurrentHashMap<>();

    public ConcurrentHashMap<Integer, FishingChallengeRoom> robotRoom = new ConcurrentHashMap<>();

    /**
     * 目前所有的boss数量
     */
    private static long bossNum;

    @Scheduled(cron = "0 30 * * * ?")
    public void updateRobotRoom() {
        robotRoom.forEachValue(1, this::generateRobot);
        logger.info("Robot房间更新头像");
    }

    public FishingChallengeManager() {
        FileInputStream inputStream = null;

        try {
            inputStream = new FileInputStream("data/img");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String str;
            while( (str = reader.readLine()) != null) {
                imageList.add(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert inputStream != null;
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 每一秒循环执行房间任务
        long loopTime = 1000;
        ThreadPoolUtils.TIMER_SERVICE_POOL.scheduleAtFixedRate(() -> {
            try {
                List<FishingChallengeRoom> gameRooms = GameContainer.getGameRooms(FishingChallengeRoom.class);
                for (FishingChallengeRoom gameRoom : gameRooms) {
                    if (robotRoom.containsKey(gameRoom.getCode())) continue;
                    if (gameRoom != null) {
                        if (gameRoom.getPlayerSize() > 0) { // 有玩家才刷鱼
                            gameRoom.addRoomTick();
                            doFishingRoomTask(gameRoom);
                        } else {
                            gameRoom.reset();
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("捕鱼挑战赛:执行房间循环任务时出现异常:[{}][{}]", e.getMessage(), e);
            }
        }, 1000, loopTime, TimeUnit.MILLISECONDS);
        // 房间boss刷新间隔时间 秒
        long bossRefreshTime = 20 * 60;
        ThreadPoolUtils.TIMER_SERVICE_POOL.scheduleAtFixedRate(() -> {
            List<FishingChallengeRoom> gameRooms = GameContainer.getGameRooms(FishingChallengeRoom.class).stream()
                    .filter(room -> room.getPlayerSize() > 0)
                    .collect(Collectors.toList());
            int roomNum = gameRooms.size();
            if (roomNum > 0) {
                // 有boss的鱼房
                bossNum = gameRooms.stream().filter(room -> room.getBoss() == 1).count();
                // 期望的boss数量
                int i = roomNum / 10;
                i = i <= 0 ? 1 : i;
                int count = 1000; // 循环最多次数，如果超出该次数就结束循环
                while (bossNum < i && count > 0) { // 需要刷boss
                    FishingChallengeRoom room = gameRooms.get(ThreadLocalRandom.current().nextInt(0, roomNum));
                    long millis = System.currentTimeMillis();
                    if (room != null &&
                            room.getBoss() == 0 && // 房间无boss
                            !room.isFishTide() && // 房间无鱼潮
                            millis - room.getLastRoomFrozenTime() > FishingManager.SKILL_FROZEN_TIME // 房间未冰冻
                    ) {
                        // 房间置为有系统boss的状态
                        room.setBoss(1);
                        // 发送刷新boss响应
                        TtmyFishingChallengeMessage.FishingChallengeRefreshBossResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeRefreshBossResponse.newBuilder();
                        sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_REFRESH_BOSS_RESPONSE_VALUE, builder);
                        // 清空鱼
                        room.getFishMap().clear();

                        List<FishRefreshRule> rules = room.getBossRefreshRules();
                        // 随机一个刷新规则
                        FishRefreshRule refreshRule = rules.get(ThreadLocalRandom.current().nextInt(0, rules.size()));
                        // 按照boss规则刷新
                        refreshGroupFish(room, refreshRule.getId());
                        bossNum++;
//                        logger.info("捕鱼挑战赛房间[{}]刷新了一条BOSS", room.getCode());
                    }
                    count--;
                }
            }
        }, 1, bossRefreshTime, TimeUnit.SECONDS);
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> {
            // 创建40个空房间
            int createRoomNum = 40;
            int odd = 7;
            int even = 7;
            for (int i = 0; i < createRoomNum; i++) {
                FishingChallengeRoom gameRoom = GameContainer.createGameRoom(FishingChallengeRoom.class, 4);
                if (gameRoom.getCode() % 2 == 0 && odd > 0) {
                    generateRobot(gameRoom);
                    robotRoom.put(gameRoom.getCode(), gameRoom);
                    odd--;
                } else if (gameRoom.getCode() % 2 == 1 && even > 0) {
                    generateRobot(gameRoom);
                    robotRoom.put(gameRoom.getCode(), gameRoom);
                    even--;
                }
            }

            for (int i = 0; i < 7; i++) {
                FishingChallengeRoom gameRoom = GameContainer.createGameRoom(FishingChallengeRoom.class, 4);

                gameRoom.setVip(true);
                gameRoom.setVerify(i % 2 == 0);
                gameRoom.setRoomPassword(UUID.randomUUID().toString());
                generateRobot(gameRoom);
                robotRoom.put(gameRoom.getCode(), gameRoom);
            }

        }, 5, TimeUnit.SECONDS);
    }

    private void generateRobot(FishingChallengeRoom gameRoom) {
        BaseGamePlayer[] gamePlayers = new BaseGamePlayer[]{new FishingGamePlayer(), new FishingGamePlayer(), new FishingGamePlayer(), new FishingGamePlayer()};
        for (BaseGamePlayer gamePlayer : gamePlayers) {
            int pos = new Random().nextInt(imageList.size());

            ServerUser serverUser = new ServerUser();
            UserEntity userEntity = new UserEntity();
            serverUser.setEntity(userEntity);
            userEntity.setHeadIndex(0);
            userEntity.setHeadUrl(imageList.get(pos));
            gamePlayer.setUser(serverUser);
        }
        gameRoom.setGamePlayers(gamePlayers);
    }

    /**
     * 房间循环任务,刷鱼等
     */
    private void doFishingRoomTask(FishingChallengeRoom gameRoom) {
        long nowTime = System.currentTimeMillis();

        if (gameRoom.getNextRefreshTime() == null) {
            Map<FishRefreshRule, Long> refreshTime = new HashMap<>();
            // 获取该房间场次的所有刷鱼规则
            List<FishRefreshRule> allRefreshRules = DataContainer.getDatas(FishRefreshRule.class)
                    .stream()
                    .filter(rule -> Arrays.asList(rule.getRealScene()).contains(gameRoom.getRoomIndex()))
                    .sorted()
                    .collect(Collectors.toList());
            // 规则放入房间内存放
            gameRoom.getRefreshRules().addAll(allRefreshRules);

            for (FishRefreshRule refreshRule : gameRoom.getRefreshRules()) {
                // 若为鱼潮，则重置房间鱼潮时间
                if (refreshRule.isFishTide()) {
                    gameRoom.setMinFishTideDelay(refreshRule.getMinDelay());
                    int max = refreshRule.getMaxDelay() == 0 ? refreshRule.getMinDelay() : refreshRule.getMaxDelay();
                    gameRoom.setMaxFishTideDelay(max);

                    if (gameRoom.getMinFishTideDelay() > 0) {
                        // 设置距离下次刷新鱼潮的时长
                        gameRoom.setNextFishTideTime(ThreadLocalRandom.current().nextLong(
                                gameRoom.getMinFishTideDelay(), gameRoom.getMaxFishTideDelay() + 1
                        ));
                    }
                    continue;
                }
                // 将Boss鱼配置单独提出来
                if (refreshRule.isBoss()) {
                    gameRoom.getBossRefreshRules().add(refreshRule);
                    // boss鱼不自动刷，由系统控制刷新
                    continue;
                }
                refreshTime.put(refreshRule, getNextRefreshTime(refreshRule, 0));
            }
            gameRoom.setNextRefreshTime(refreshTime);
        }

        if (gameRoom.getBoss() == 0 // 房间无Boss
                && !gameRoom.isFishTide()
                && gameRoom.getMinFishTideDelay() > 0
                && gameRoom.getRoomTick() >= gameRoom.getNextFishTideTime()) { // 进入鱼潮
            if (gameRoom.getMinFishTideDelay() > 0) {
                // 设置下次刷新鱼潮的时长
                gameRoom.setNextFishTideTime(gameRoom.getRoomTick() +
                        ThreadLocalRandom.current().nextLong(gameRoom.getMinFishTideDelay(), gameRoom.getMaxFishTideDelay() + 1)
                );
            }
            // 房间置为鱼潮中
            gameRoom.setFishTide(true);
            // 发送鱼潮消息
            TtmyFishingChallengeMessage.FishingChallengeFishTideResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeFishTideResponse.newBuilder();
            sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_FISH_TIDE_RESPONSE_VALUE, builder);
            gameRoom.getFishMap().clear();

            // 获取该房间内刷新规则
            List<FishRefreshRule> allRefreshRules = gameRoom.getRefreshRules();
            Collections.shuffle(allRefreshRules);
            for (FishRefreshRule rule : allRefreshRules) {
                if (rule.isFishTide()) {
                    // 该鱼潮规则下的所有鱼群刷新出来
                    List<FishGroupConfig> allConfigs = DataContainer.getDatas(FishGroupConfig.class, rule.getStart(), rule.getEnd());
                    for (FishGroupConfig config : allConfigs) {
                        refreshGroupFish(gameRoom, rule.getId(), config);
                    }
                    break;
                }
            }
            gameRoom.setNoFishTick(0);
        } else if (gameRoom.isFishTide()) { // 鱼潮中
            // 如果房间还在冰冻中就后延鱼潮持续时间，不在冰冻才判断是否结束鱼潮
            if (gameRoom.getNoFishTick() > 2 &&
                    nowTime - gameRoom.getLastRoomFrozenTime() > FishingManager.SKILL_FROZEN_TIME) {
                gameRoom.setFishTide(false);
            } else if (gameRoom.getFishMap().size() == 0) {
                gameRoom.setNoFishTick(gameRoom.getNoFishTick() + 1);
            }
        } else { // 正常刷鱼
            // 房间有自动刷新的boss就不刷其他鱼，房间冰冻期间不刷新鱼
            if (gameRoom.getBoss() != 1 && nowTime - gameRoom.getLastRoomFrozenTime() > FishingManager.SKILL_FROZEN_TIME) {
                // 刷新鱼
                for (Map.Entry<FishRefreshRule, Long> refreshEntry : gameRoom.getNextRefreshTime().entrySet()) {
                    FishRefreshRule key = refreshEntry.getKey();

                    if (refreshEntry.getValue() <= gameRoom.getRoomTick()) { // 刷新时间到
                        refreshEntry.setValue(getNextRefreshTime(key, gameRoom.getRoomTick()));
                        refreshGroupFish(gameRoom, key.getId());
                    } else if (key.isDynamicRefresh() && gameRoom.getFishMap().values().stream()
                            .noneMatch(fish -> fish.getRuleId() == key.getId())) { // 无此鱼且为动态刷新配置
                        refreshEntry.setValue(getNextRefreshTime(key, gameRoom.getRoomTick()));
                        refreshGroupFish(gameRoom, key.getId());
                    }
                }
            }
        }

        // 判断玩家操作时间
        for (int i = 0; i < gameRoom.getMaxSize(); i++) {
            FishingChallengePlayer player = gameRoom.getGamePlayerBySeat(i);
            // 检查玩家是否长时间未操作
            if (player != null && nowTime - player.getLastFireTime() > FishingManager.ROOM_KICK_TIME) {
                NetManager.sendHintMessageToClient("您长时间未操作，已被移出捕鱼房间", player.getUser());
                exitRoom(player, gameRoom);
            }
        }

        // 判断过期鱼，并从鱼表内移除
        List<Long> removeKey = new LinkedList<>();
        for (FishStruct fish : gameRoom.getFishMap().values()) {
            long maxLifeTime = Math.round(fish.getLifeTime() > 0 ? fish.getLifeTime() : FishingManager.DEFAULT_LIFE_TIME);
//             maxLifeTime += DELAY_LIFE_TIME;
            if (maxLifeTime * 1000 + fish.getCreateTime() < nowTime) {
                removeKey.add(fish.getId());
                if (fish.getFishType() == 100) { // boss鱼消失了就重置房间boss不存在
                    gameRoom.setBoss(0);
                }
            }
        }
        for (long key : removeKey) {
            gameRoom.getFishMap().remove(key);
        }
    }

    /**
     * 刷新随机的一群鱼
     */
    private void refreshGroupFish(FishingChallengeRoom gameRoom, long ruleId) {
        FishRefreshRule rule = DataContainer.getData(ruleId, FishRefreshRule.class);
        FishGroupConfig group = DataContainer.getRandomData(FishGroupConfig.class, rule.getStart(), rule.getEnd());
        refreshGroupFish(gameRoom, ruleId, group);
    }

    /**
     * 刷新指定的一群鱼
     */
    private void refreshGroupFish(FishingChallengeRoom gameRoom, long ruleId, FishGroupConfig groupConfig) {
        if (groupConfig != null) {
            double delay = 0;
            List<Long> fishIds = new LinkedList<>();
            Long[] routeIds = groupConfig.getRealRouteId();
            long routeId = routeIds.length > 1 ? routeIds[ThreadLocalRandom.current().nextInt(routeIds.length)] : routeIds[0];

            if (StringUtils.isEmpty(groupConfig.getDelay())) { // 未设置延迟，默认同时刷出
                fishIds.addAll(Arrays.asList(groupConfig.getRealGroup()));
            } else { // 设置延迟则分组刷新鱼
                fishIds.add(groupConfig.getRealGroup()[0]);

                for (int i = 1; i < groupConfig.getRealGroup().length; i++) {
                    double realDelay = groupConfig.getRealDelay().length > i - 1 ? groupConfig.getRealDelay()[i - 1]
                            : groupConfig.getRealDelay()[0];

                    if (realDelay > 0) {
                        refreshFishWithDelay(gameRoom, fishIds, routeId, ruleId, delay);
                        delay += realDelay;
                        fishIds = new LinkedList<>();
                    }

                    fishIds.add(groupConfig.getRealGroup()[i]);
                }
            }

            refreshFishWithDelay(gameRoom, fishIds, routeId, ruleId, delay);
        }
    }

    /**
     * 定时刷新一组鱼
     */
    private void refreshFishWithDelay(FishingChallengeRoom gameRoom, List<Long> fishIds, long routeId, long ruleId, double delay) {
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> refreshFish(gameRoom, fishIds, routeId, ruleId),
                (int) (delay * 1000), TimeUnit.MILLISECONDS);
    }

    /**
     * 刷新一组鱼
     */
    private void refreshFish(FishingChallengeRoom gameRoom, List<Long> fishIds, long routeId, long ruleId) {
        long nowTime = System.currentTimeMillis();
        if (nowTime - gameRoom.getLastRoomFrozenTime() < FishingManager.SKILL_FROZEN_TIME) {
            return;
        }

        FishRefreshRule rule = DataContainer.getData(ruleId, FishRefreshRule.class);
        if (!rule.isFishTide() && gameRoom.isFishTide()) { // 如果不是鱼潮中的鱼，且房间正处于鱼潮中，中止刷此鱼
            return;
        }

        TtmyFishingChallengeMessage.FishingChallengeRefreshFishesResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeRefreshFishesResponse.newBuilder();
        FishRouteConfig route = DataContainer.getData(routeId, FishRouteConfig.class);
        for (Long fishId : fishIds) {
            FishConfig fish = DataContainer.getData(fishId, FishConfig.class);

            // 鱼不为boss并且房间刷新了挑战赛boss就终止刷此鱼
            if (fish.getFishType() != 100 && gameRoom.getBoss() == 1) {
                return;
            }

            int maxSafe = fish.getMaxSafe() == 0 ? fish.getMinSafe() + 1 : fish.getMaxSafe();
            int safeTimes = ThreadLocalRandom.current().nextInt(fish.getMinSafe(), maxSafe);
            // 生成一条鱼
            FishStruct fishStruct = new FishStruct();
            fishStruct.setId(gameRoom.getNextId());
            fishStruct.setRuleId(ruleId);
            fishStruct.setConfigId(fish.getId());
            fishStruct.setRouteId(route.getId());
            fishStruct.setLifeTime(route.getTime());
            fishStruct.setSafeTimes(safeTimes);
            fishStruct.setCreateTime(nowTime);
            fishStruct.setFishType(fish.getFishType());
            gameRoom.getFishMap().put(fishStruct.getId(), fishStruct);
            builder.addFishInfos(createFishInfoProto(fishStruct));

            // Boss鱼刷新出来就延后鱼潮的刷新时间
            // 这样做就是防止鱼潮出来之后把boss赶跑了
            if (fish.getFishType() == 100) {
                long nextFishTideTime = gameRoom.getNextFishTideTime();
                long roomTick = gameRoom.getRoomTick();
                float lifeTime = fishStruct.getLifeTime();
                if (roomTick + lifeTime >= nextFishTideTime) {
                    // Boss消失时间在鱼潮之后就要将鱼潮延时，避免赶走boss
                    nextFishTideTime += (long) (roomTick + lifeTime - nextFishTideTime + 30);
                    gameRoom.setNextFishTideTime(nextFishTideTime);
                }
            }
        }
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_REFRESH_FISHES_RESPONSE_VALUE, builder);
        gameRoom.setNoFishTick(0);
    }

    /**
     * 获取下次刷新时间
     */
    private long getNextRefreshTime(FishRefreshRule refreshRule, long nowRefreshTime) {
        long minTime = refreshRule.getMinDelay();
        long maxTime = refreshRule.getMaxDelay() == 0 ? minTime : refreshRule.getMaxDelay();
        return nowRefreshTime + ThreadLocalRandom.current().nextLong(minTime, maxTime + 1);
    }

    /**
     * 判断鱼是否被击中
     */
    public boolean isHit(FishingChallengePlayer gamePlayer, int roomIndex, FishStruct fish, FishConfig config) {
        if (fish.getFireTimes() < fish.getSafeTimes()) { // 安全次数判断
//            // 拥有最高等级炮台为中级场最高炮台倍数以上检查安全次数 最高倍数及以下的不检查安全次数
//            if (PlayerManager.getPlayerBatteryLevel(gamePlayer.getUser()) > batteryLevelLimit[1][1]) {
//                return false;
//            }
            return false;
        }

        double baseHit = (double) config.getAttack() / config.getHealth(); // 基础命中率 = 攻击值 / 生命值

        // TODO 判断鱼命中内参数
        int index = roomIndex - 1;
        int greener = 1; // 挑战赛不区分新老玩家 FishingHitDataManager.getGreener(gamePlayer);

        // 基础命中系数(1)加上服务器当前命中系数
        double baseCoefficient = 100D + FishingHitDataManager.getServerProb(greener, index);


        // 当日输赢金币限制
        long dailyWin = FishingHitDataManager.getChallengeDailyWin(gamePlayer.getId(), index);
        if (dailyWin < -FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][0]) {
            baseCoefficient += FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][0]; // 幸运玩家系数
        } else if (dailyWin > FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][1]) {
            baseCoefficient -= FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][1]; // 挽救玩家系数
        }

        // 小黑屋限制
        if (FishingHitDataManager.getChallengeBlackRoom(gamePlayer.getId(), index) > 0) {
            baseCoefficient -= FishingHitDataManager.BLACK_ROOM_PROB[greener][index];
        }

        // 总计输赢金币限制
        long totalWin = FishingHitDataManager.getChallengeTotalWin(gamePlayer.getId(), index);
        if (totalWin < -FishingHitDataManager.PLAYER_TOTAL_LIMIT[greener][index][0]) {
            baseCoefficient += FishingHitDataManager.PLAYER_TOTAL_PROB[greener][index][0];
        } else if (totalWin > FishingHitDataManager.PLAYER_TOTAL_LIMIT[greener][index][1]) {
            baseCoefficient -= FishingHitDataManager.PLAYER_TOTAL_PROB[greener][index][1];
        }

        // 公用玩家AP值
        baseCoefficient += FishingHitDataManager.getPlayerFishingProb(gamePlayer.getId());

        if(room1.containsKey(gamePlayer.getUser())) {
            baseCoefficient -= ((FishingHitDataManager.getTotalWin(gamePlayer.getId(), index) - FishingUtil.q0[8]) * 1.0 / FishingUtil.apt[8] * 0.01);
        } else if (room2.containsKey(gamePlayer.getUser())) {
            baseCoefficient -= ((FishingHitDataManager.getTotalWin(gamePlayer.getId(), index) - FishingUtil.q0[9]) * 1.0 / FishingUtil.apt[9] * 0.01);
        } else {
            baseCoefficient -= ((FishingHitDataManager.getTotalWin(gamePlayer.getId(), index) - FishingUtil.q0[10]) * 1.0 / FishingUtil.apt[10] * 0.01);
        }



        // 玩家暴击状态下命中率降低
        if ((System.currentTimeMillis() - gamePlayer.getLastCritTime()) / 1000 < FishingManager.SKILL_CRIT_TIME / 1000) {
            baseCoefficient *= 0.75;
        }

        // 判断是否命中
        return !(baseHit * baseCoefficient / 100D < ThreadLocalRandom.current().nextDouble());
    }

    // ******************************************

    /**
     * 创建房间信息协议
     */
    public TtmyFishingChallengeMessage.FishingChallengeRoomInfoProto.Builder createRoomInfoProto(FishingChallengeRoom room) {
        TtmyFishingChallengeMessage.FishingChallengeRoomInfoProto.Builder builder = TtmyFishingChallengeMessage.FishingChallengeRoomInfoProto.newBuilder();
        builder.setRoomCode(room.getCode());
        builder.setBoss(room.getBoss());
        builder.setVip(room.isVip());
        builder.setVerify(room.isVerify());
        for (BaseGamePlayer gamePlayer : room.getGamePlayers()) {
            if (gamePlayer != null) {
                UserEntity userEntity = gamePlayer.getUser().getEntity();
                if (userEntity.getHeadIndex() == 0) {
                    builder.addHeadImg(userEntity.getHeadUrl());
                } else {
                    builder.addHeadImg(String.valueOf(userEntity.getHeadIndex()));
                }
            }
        }
        return builder;
    }

    /**
     * 创建玩家信息协议
     */
    public TtmyFishingChallengeMessage.FishingChallengePlayerInfoProto.Builder createPlayerInfoProto(FishingChallengePlayer player) {
        TtmyFishingChallengeMessage.FishingChallengePlayerInfoProto.Builder builder = TtmyFishingChallengeMessage.FishingChallengePlayerInfoProto.newBuilder();
        builder.setPlayerId(player.getId());
        builder.setName(player.getUser().getNickname());
        builder.setHeadIndex(player.getUser().getEntity().getHeadIndex());
        builder.setHeadUrl(player.getUser().getEntity().getHeadUrl());
        builder.setSex(player.getUser().getEntity().getSex());
        builder.setMoney(player.getMoney());
        builder.setSeat(player.getSeat());
        builder.setOnline(player.getUser().isOnline());
        builder.setVipLevel(player.getVipLevel());
        builder.setViewIndex(player.getViewIndex());
        builder.setBatteryLevel(player.getBatteryLevel());
        builder.setBatteryMult(player.getBatteryMult());
        builder.setLevel(player.getLevel());
        return builder;
    }

    /**
     * 创建鱼数据结构
     */
    public TtmyFishingChallengeMessage.FishingChallengeFishInfoProto createFishInfoProto(FishStruct struct) {
        TtmyFishingChallengeMessage.FishingChallengeFishInfoProto.Builder builder = TtmyFishingChallengeMessage.FishingChallengeFishInfoProto.newBuilder();
        builder.setId(struct.getId());
        builder.setFishId(struct.getConfigId());
        builder.setRouteId(struct.getRouteId());
        builder.setClientLifeTime(struct.getClientLifeTime());
        builder.setCreateTime(struct.getCreateTime());
        return builder.build();
    }

    // ******************************************

    /**
     * 发送信息给房间所有玩家
     */
    public void sendRoomMessage(FishingChallengeRoom room, int msgCode, GeneratedMessage.Builder<?> message) {
        if (room != null) {
            for (BaseGamePlayer gamePlayer : room.getGamePlayers()) {
                if (gamePlayer != null) {
                    NetManager.sendMessage(msgCode, message, gamePlayer.getUser());
                }
            }
        }
    }

    /**
     * 发送加入房间响应
     */
    public void sendJoinRoomResponse(FishingChallengeRoom room, FishingChallengePlayer player, int roomType) {
        TtmyFishingChallengeMessage.FishingChallengeJoinRoomResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeJoinRoomResponse.newBuilder();
        builder.setRoomCode(room.getCode());
        builder.setVip(room.isVip());
        builder.setRoomType(roomType);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_JOIN_ROOM_RESPONSE_VALUE, builder, player.getUser());
    }

    /**
     * 给房间内所有玩家发送某玩家信息
     */
    public void sendRoomPlayerInfoResponse(FishingChallengeRoom room, FishingChallengePlayer player) {
        TtmyFishingChallengeMessage.FishingChallengeRoomPlayerInfoResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeRoomPlayerInfoResponse.newBuilder();
        builder.setPlayerInfo(createPlayerInfoProto(player));
        sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_ROOM_PLAYER_INFO_RESPONSE_VALUE, builder);
    }

    /**
     * 发送房间内所有玩家的信息给某玩家
     */
    public void sendRoomPlayerInfoListResponse(FishingChallengeRoom room, FishingChallengePlayer player) {
        if (room != null) {
            TtmyFishingChallengeMessage.FishingChallengeRoomPlayerInfoListResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeRoomPlayerInfoListResponse.newBuilder();
            for (BaseGamePlayer gamePlayer : room.getGamePlayers()) {
                if (gamePlayer != null) {
                    builder.addPlayerInfos(createPlayerInfoProto((FishingChallengePlayer) gamePlayer));
                }
            }
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_ROOM_PLAYER_INFO_LIST_RESPONSE_VALUE, builder, player.getUser());
        }
    }

    /**
     * 向玩家发送房间当前的冰冻消息
     */
    public void sendFrozenMessage(FishingChallengeRoom gameRoom, FishingChallengePlayer player) {
        long nowTime = System.currentTimeMillis();
        if (nowTime - gameRoom.getLastRoomFrozenTime() < FishingManager.SKILL_FROZEN_TIME) { // 房间处于冰冻状态
            TtmyFishingChallengeMessage.FishingChallengeUseSkillResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeUseSkillResponse.newBuilder();
            builder.setSkillId(ItemId.SKILL_FROZEN.getId()); // 冰冻
            builder.setDuration((int) ((FishingManager.SKILL_FROZEN_TIME - (nowTime - gameRoom.getLastRoomFrozenTime())) / 1000));
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_USE_SKILL_RESPONSE_VALUE, builder, player.getUser());
        }
    }

    /**
     * 发送房间内鱼同步的响应
     */
    public void sendSynchroniseResponse(FishingChallengeRoom gameRoom, FishingChallengePlayer player) {
        TtmyFishingChallengeMessage.FishingChallengeSynchroniseResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeSynchroniseResponse.newBuilder();
        for (FishStruct fish : gameRoom.getFishMap().values()) {
            builder.addFishInfos(createFishInfoProto(fish));
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_SYNCHRONISE_RESPONSE_VALUE, builder, player.getUser());
    }

    /**
     * 为房间创建加入一名玩家
     */
    public void addRoomPlayer(FishingChallengeRoom gameRoom, ServerUser user, int roomType) {
        synchronized (gameRoom) {
            long enterMoney = PlayerManager.getPlayerEntity(user).getMoney();
            FishingChallengePlayer gamePlayer = GameContainer.createGamePlayer(gameRoom, user, FishingChallengePlayer.class);
            gamePlayer.setEnterMoney(enterMoney);
            gamePlayer.setEnterRoomTime(System.currentTimeMillis());
            int level = 1;
            if (roomType == 2) level = 100;
            if (roomType == 3) level = 1000;
            // 玩家初始炮台倍数
            gamePlayer.setBatteryLevel(level);

            sendJoinRoomResponse(gameRoom, gamePlayer, roomType); // 发送加入房间响应
            sendRoomPlayerInfoListResponse(gameRoom, gamePlayer); // 发送当前房间内玩家的信息
            sendRoomPlayerInfoResponse(gameRoom, gamePlayer); // 发送自己的信息给房间内所有玩家
            sendSynchroniseResponse(gameRoom, gamePlayer); // 发送同步鱼消息
            sendFrozenMessage(gameRoom, gamePlayer); // 发送房间当前冰冻消息
        }
    }

    /**
     * 发送捕获到boss鱼的广播响应
     */
    private void sendCatchBossFishResponse(TtmyFishingChallengeMessage.FishingChallengeCatchBossFishResponse.Builder response) {
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> {
            // 只发送到全服在捕鱼房间内的玩家
            List<FishingChallengeRoom> fishingGameRooms = GameContainer.getGameRooms(FishingChallengeRoom.class);
            for (FishingChallengeRoom gameRoom : fishingGameRooms) {
                if (gameRoom != null) {
                    sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_CATCH_BOSS_FISH_RESPONSE_VALUE, response);
                }
            }
        }, 0, TimeUnit.SECONDS);
    }

    // ******************************************

    /**
     * 挑战赛房间列表
     */
    public void roomList(ServerUser user, int roomType) {
//        logger.info("获取房间列表");

        TtmyFishingChallengeMessage.FishingChallengeRoomListResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeRoomListResponse.newBuilder();

        List<FishingChallengeRoom> gameRooms = GameContainer.getGameRooms(FishingChallengeRoom.class).
                stream().
                filter(room -> {
                    if(roomType == 1 || roomType == 2) {
                        return (room.getCode() % 2 == roomType - 1) && !room.isVip();
                    } else if (roomType == 3) {
                        return room.isVip();
                    }
                    return false;
                }).
                collect(Collectors.toList());
        gameRooms = gameRooms.stream().sorted((o1, o2) -> compare(o1.isVerify(), o2.isVerify())).collect(Collectors.toList());


        gameRooms.forEach(fishingChallengeRoom -> builder.addRoomList(createRoomInfoProto(fishingChallengeRoom)));

        builder.setRoomType(roomType);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_ROOM_LIST_RESPONSE_VALUE, builder, user);
    }

    /**
     * 创建房间
     */
    public void createRoom(ServerUser user, String roomPassword) {
        if (GameContainer.getGameRooms(FishingChallengeRoom.class).size() >= MAX_ROOM_NUM) {
            NetManager.sendErrorMessageToClient("房间数量已达上限", user);
            return;
        }
        if (PlayerManager.getPlayerBatteryLevel(user) < 1000) {
            NetManager.sendErrorMessageToClient("炮台倍数" + 1000 + "以上才能创建房间", user);
            return;
        }
        int vipLevel = PlayerManager.getPlayerVipLevel(user);
        if (!StringUtils.isEmpty(roomPassword)) {
            if (vipLevel < 4) {
                NetManager.sendErrorMessageToClient("VIP4及以上玩家才可以设置房间密码", user);
                return;
            }
            if (roomPassword.length() > 20) {
                NetManager.sendErrorMessageToClient("密码长度必须在1-20位之间", user);
                return;
            }
        }
        FishingChallengeRoom gameRoom = GameContainer.createGameRoom(FishingChallengeRoom.class, 4);
        if (!StringUtils.isEmpty(roomPassword)) { // 密码不为空就是VIP房间
            gameRoom.setVip(true);
            gameRoom.setVerify(true);
            gameRoom.setRoomPassword(roomPassword);
        } else {
            gameRoom.setVip(true);
            gameRoom.setVerify(false);
        }

        // 加入房间
        addRoomPlayer(gameRoom, user, 3);
    }

    /**
     * 玩家加入指定房间
     */
    public void joinRoom(ServerUser user, int roomCode, String roomPassword, int rootType) {
        logger.info("加入指定房间：" + user + " roomCode:" +  roomCode + " roomPassword:" + roomPassword + " " + rootType);
        BaseGameRoom gameRoom = GameContainer.getGameRoomByCode(roomCode);
        if (!(gameRoom instanceof FishingChallengeRoom)) {
            NetManager.sendErrorMessageToClient("房间不存在", user);
            return;
        }
        FishingChallengeRoom room = (FishingChallengeRoom) gameRoom;
        if (room.getPlayerSize() >= room.getMaxSize()) {
            NetManager.sendErrorMessageToClient("房间人数已满", user);
            return;
        }
        if (room.isVip() && PlayerManager.getPlayerVipLevel(user) < 4) {
            NetManager.sendErrorMessageToClient("VIP4及以上玩家才能加入VIP房间", user);
            return;
        }
        int batteryLevel = PlayerManager.getPlayerBatteryLevel(user);

        if (rootType == 1) {
            if (batteryLevel < 1) {
                NetManager.sendErrorMessageToClient("炮台等级不足：最小需要" + 1 + "倍炮", user);
                return;
            }
        } else if (rootType == 2) {
            if (batteryLevel < 100) {
                NetManager.sendErrorMessageToClient("炮台等级不足：最小需要" + 100 + "倍炮", user);
                return;
            }
        } else if (rootType == 3) {
            if (batteryLevel < 1000) {
                NetManager.sendErrorMessageToClient("炮台等级不足：最小需要" + 1000 + "倍炮", user);
                return;
            }
        }

        if (!roomPassword.equals(room.getRoomPassword())) {
            NetManager.sendErrorMessageToClient("房间密码错误", user);
            return;
        }

        switch (rootType) {
            case 1: room1.put(user, 1); break;
            case 2: room2.put(user, 2); break;
            case 3: room3.put(user, 2); break;
        }

        // 加入房间
        addRoomPlayer(room, user, rootType);
    }

    /**
     * 快速加入挑战赛房间
     */
    public void quickJoin(ServerUser user) {
        logger.info("加入挑战赛：" + user);
        int batteryLevel = PlayerManager.getPlayerBatteryLevel(user);
        if (batteryLevel < MIN_BATTERY_LEVEL) {
            NetManager.sendErrorMessageToClient("炮台等级不足：最小需要" + MIN_BATTERY_LEVEL + "倍炮", user);
            return;
        }
        List<FishingChallengeRoom> gameRooms = GameContainer.getGameRooms(FishingChallengeRoom.class)
                .stream()
                .sorted(Comparator.comparingInt(FishingChallengeRoom::getPlayerSize).reversed())
                .collect(Collectors.toList());
        for (FishingChallengeRoom room : gameRooms) {
            if (room != null) {
                if (room.getPlayerSize() >= room.getMaxSize()) {
                    continue;
                }
                if (room.isVip() || !room.getRoomPassword().equals("")) { // VIP和有密码的房间不能快速加入
                    continue;
                }
                // 加入房间
                addRoomPlayer(room, user, 1);
                return;
            }
        }
        NetManager.sendHintMessageToClient("暂无合适房间加入", user);
    }

    /**
     * 退出捕鱼挑战赛房间
     */
    public void exitRoom(FishingChallengePlayer player, FishingChallengeRoom room) {
        TtmyFishingChallengeMessage.FishingChallengeExitRoomResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeExitRoomResponse.newBuilder();
        builder.setPlayerId(player.getId());
        sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_EXIT_ROOM_RESPONSE_VALUE, builder);
        // 退出房间后的操作，比如日志记录等
        ServerUser user = player.getUser();
        OseePlayerEntity entity = PlayerManager.getPlayerEntity(user);
        if (entity != null) {
            agentManager.addActiveMoney(user.getId(), GameEnum.FISHING, 0, player.getCutMoney());
            // 保存玩家金币变化记录
            if (player.getChangeMoney() != 0) {
                // 保存到捕鱼记录
                OseeFishingRecordLogEntity recordLogEntity = new OseeFishingRecordLogEntity();
                recordLogEntity.setPlayerId(user.getId());
                recordLogEntity.setRoomIndex(room.getRoomIndex());
                recordLogEntity.setSpendMoney(player.getSpendMoney());
                recordLogEntity.setWinMoney(player.getWinMoney());
                recordLogEntity.setDropBronzeTorpedoNum(player.getDropBronzeTorpedoNum());
                recordLogEntity.setDropSilverTorpedoNum(player.getDropSilverTorpedoNum());
                recordLogEntity.setDropGoldTorpedoNum(player.getDropGoldTorpedoNum());
                fishingRecordLogMapper.save(recordLogEntity);
            }
            int roomType = 0;
            if(room1.containsKey(user)) roomType = 1;
            if(room2.containsKey(user)) roomType = 2;
            if(room3.containsKey(user)) roomType = 3;
            // 保存抽水记录
            if (player.getCutMoney() != 0) {
                OseeCutMoneyLogEntity cutLog = new OseeCutMoneyLogEntity();
                cutLog.setUserId(user.getId());
                cutLog.setGame(calcId(roomType));
                cutLog.setType(calcId(roomType));
                cutLog.setCutMoney(player.getCutMoney());
                cutMoneyLogMapper.save(cutLog);
            }

            playerMapper.update(PlayerManager.getPlayerEntity(user));
        }
        // 把玩家从房间移除 VIP房间如果没人就删除
        GameContainer.removeGamePlayer(room, player.getSeat(), room.isVip());
    }

    private int calcId(int type) {
        if(type == 1) return GameEnum.FISHING_CHALLENGE_1.getId();
        if(type == 2) return GameEnum.FISHING_CHALLENGE_2.getId();
        if(type == 3) return GameEnum.FISHING_CHALLENGE_3.getId();
        return 0;
    }

    /**
     * 更改炮台外观
     */
    public void changeBatteryView(FishingChallengeRoom gameRoom, FishingChallengePlayer player, int viewIndex) {
        if (viewIndex >= ItemId.QSZS_BATTERY_VIEW.getId() && viewIndex <= ItemId.SWHP_BATTERY_VIEW.getId()) { // 切换到自己购买的炮台外观
            if (PlayerManager.getItemNum(player.getUser(), ItemId.getItemIdById(viewIndex)) <= 0) {
                NetManager.sendHintMessageToClient("该炮台外观已到期", player.getUser());
                return;
            }
            player.setViewIndex(viewIndex); // 设置外观
            TtmyFishingChallengeMessage.FishingChallengeChangeBatteryViewResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeChangeBatteryViewResponse.newBuilder();
            builder.setPlayerId(player.getId());
            builder.setViewIndex(player.getViewIndex());
            sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_CHANGE_BATTERY_VIEW_RESPONSE_VALUE, builder);
        } else if (PlayerManager.getPlayerVipLevel(player.getUser()) >= viewIndex) {
            player.setViewIndex(viewIndex); // 设置外观
            TtmyFishingChallengeMessage.FishingChallengeChangeBatteryViewResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeChangeBatteryViewResponse.newBuilder();
            builder.setPlayerId(player.getId());
            builder.setViewIndex(player.getViewIndex());
            sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_CHANGE_BATTERY_VIEW_RESPONSE_VALUE, builder);
        } else {
            NetManager.sendHintMessageToClient("您的VIP等级不足，无法更改该炮台外观", player.getUser());
        }
    }

    /**
     * 更换炮台等级
     */
    public void changeBatteryLevel(FishingChallengeRoom gameRoom, FishingChallengePlayer player, int targetLevel) {
        // 判断炮台等级数值是否有误
        if (targetLevel < 0 || targetLevel > 100000) {
            NetManager.sendErrorMessageToClient("更换的炮台等级有误", player.getUser());
            return;
        }
        // 改变玩家当前炮台等级
        player.setBatteryLevel(targetLevel);

        TtmyFishingChallengeMessage.FishingChallengeChangeBatteryLevelResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeChangeBatteryLevelResponse.newBuilder();
        builder.setPlayerId(player.getId());
        builder.setLevel(player.getBatteryLevel());
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_CHANGE_BATTERY_LEVEL_RESPONSE_VALUE, builder);
    }

    /**
     * 玩家发射子弹
     */
    public void playerFire(FishingChallengeRoom gameRoom, FishingChallengePlayer player, FireStruct fire) {
        long needMoney = player.getBatteryLevel() * player.getBatteryMult();
        if (System.currentTimeMillis() - player.getLastFenShenTime() < FishingManager.SKILL_FEN_SHEN_TIME) { // 还在分身阶段就要扣三发子弹的钱
            int fireCount = 3;
            needMoney *= fireCount;
            fire.setCount(fireCount);
        }

        // 检查龙晶是否足够
        if (!PlayerManager.checkItem(player.getUser(), ItemId.DRAGON_CRYSTAL, needMoney)) {
            return;
        }
        int index = gameRoom.getRoomIndex() - 1;
        // 小黑屋
        FishingHitDataManager.addChallengeBlackRoom(player.getId(), index, -needMoney);

        player.setLastFireTime(System.currentTimeMillis());
        player.addMoney(-needMoney);
        FishingHitDataManager.addChallengeWin(gameRoom, player, -needMoney);

        fire.setLevel(player.getBatteryLevel());
        fire.setMult(player.getBatteryMult());
        player.getFireMap().put(fire.getId(), fire);

        // 广播玩家发送子弹响应
        TtmyFishingChallengeMessage.FishingChallengeFireResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeFireResponse.newBuilder();
        builder.setFireId(fire.getId());
        builder.setFishId(fire.getFishId());
        builder.setAngle(fire.getAngle());
        builder.setRestMoney(player.getMoney());
        builder.setPlayerId(player.getId());
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_FIRE_RESPONSE_VALUE, builder);
    }

    /**
     * 玩家击中鱼
     */
    public List<FishConfig> playerFightFish(FishingChallengeRoom gameRoom, FishingChallengePlayer player, long fireId, List<Long> fishIds, boolean boom) {
        try {
            List<FishConfig> configs = fightFish(gameRoom, player, fireId, fishIds, boom);
            if (configs != null) {
                for (FishConfig config : configs) {
                    // 玩家加经验
                    FishingManager.addExperience(player.getUser(), config.getExp());
                }
            }
            return configs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 玩家击中鱼的各种逻辑判断
     */
    private List<FishConfig> fightFish(FishingChallengeRoom gameRoom, FishingChallengePlayer player,
                                       long fireId, List<Long> fishIds, boolean boom) {
        List<FishConfig> configs = new LinkedList<>();

        synchronized (gameRoom) {
            // 非爆炸状态，且房间内不存在指定子弹
            FireStruct fire = null;
            if (!boom) {
                if (!player.getFireMap().containsKey(fireId)) {
                    return null;
                }

                fire = player.getFireMap().get(fireId);
                fire.setCount(fire.getCount() - 1);
                if (fire.getCount() <= 0) { // 该发子弹是否打完了
                    player.getFireMap().remove(fireId);
                }
            }

            for (Long fishId : fishIds) {
                // 鱼id不存在
                if (!gameRoom.getFishMap().containsKey(fishId)) {
                    continue;
                }

                FishStruct fish = gameRoom.getFishMap().get(fishId);
                FishConfig config = DataContainer.getData(fish.getConfigId(), FishConfig.class);

                // boss鱼无法因爆炸死亡
                if (boom && config.getFishType() == 100) {
                    continue;
                }

                if (!boom && !isHit(player, gameRoom.getRoomIndex(), fish, config)) {
                    fish.setFireTimes(fish.getFireTimes() + 1);
                    continue;
                }

                fish = gameRoom.getFishMap().remove(fishId);
                configs.add(config);

                long winMoney;

                long randomMoney = config.getMaxMoney() > config.getMoney()
                        ? ThreadLocalRandom.current().nextLong(config.getMoney(), config.getMaxMoney() + 1)
                        : config.getMoney();
                if (fire != null) {
                    winMoney = randomMoney * fire.getLevel() * fire.getMult();
                    // 暴击状态下成功命中鱼类，则获得1.5倍金币奖励
                    if (System.currentTimeMillis() - player.getLastCritTime() < FishingManager.SKILL_CRIT_TIME && new Random().nextInt(100) < 25) {
                        winMoney *= 2;
                    }
                } else {
                    winMoney = randomMoney * player.getBatteryLevel() * player.getBatteryMult();
                }

                // Boss鱼死亡
                if (config.getFishType() == 100) {
                    // 房间置为boss鱼不存在了
                    gameRoom.setBoss(0);
                    if (player.getBatteryLevel() >= 1000) { // 炮台等级要1000倍以上才通报
                        // 进行全服通报
                        TtmyFishingChallengeMessage.FishingChallengeCatchBossFishResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeCatchBossFishResponse.newBuilder();
                        builder.setFishName(config.getName());
                        builder.setMoney(winMoney);
                        builder.setPlayerName(player.getUser().getNickname());
                        builder.setPlayerVipLevel(player.getVipLevel());
                        builder.setBatteryLevel(player.getBatteryLevel());
                        sendCatchBossFishResponse(builder);
                    }
                }

                // TODO 记录玩家金币和小黑屋相关数据
                FishingHitDataManager.addChallengeWin(gameRoom, player, winMoney);
                int greener = 1;
                int index = gameRoom.getRoomIndex() - 1;
                if (winMoney > FishingHitDataManager.BLACK_ROOM_LIMIT[greener][index]) {
                    FishingHitDataManager.addChallengeBlackRoom(player.getId(), index, winMoney);
                }

                // 玩家收获金币
                player.addMoney(winMoney);

                // 打死鱼之后掉落的物品
                List<OseePublicData.ItemDataProto> dropItems = new LinkedList<>();
                long itemNum2 = 0;
                long itemNum = 0;
                // Boss死亡或者可以掉鱼雷的鱼掉落鱼雷给玩家
                if ((config.getFishType() == 100 || config.getFishType() == 10) ) {
                    // 判断鱼雷掉落几率
//                    getTorpedoDropRate();
                    // 掉落几率=免费几率+付费几率
                    OseePlayerEntity playerEntity = PlayerManager.getPlayerEntity(player.getUser());
                    long rechargeMoney = 0;
                    if (playerEntity != null) {
                        rechargeMoney = playerEntity.getRechargeMoney();
                    }
                    // 鱼雷掉落概率
                    double dropRate = FishingManager.CHALLENGE_DROP_FREE_RATE +
                            ((int) (rechargeMoney / FishingManager.CHALLENGE_DROP_PER_PAY_MONEY)) * FishingManager.CHALLENGE_DROP_PER_PAY_RATE;
                    boolean bugleBoss = gameRoom.getBoss() == 2; // 是否是boss号角召唤出来的boss
                    if (bugleBoss) { // boss号角召唤出来的boss
                        gameRoom.setLastBossBugleTime(0L); // 冷却时间失效
                    }
                    gameRoom.setBoss(0); // 召唤的boss被击杀，房间无boss
                    if (ThreadLocalRandom.current().nextDouble(0, 100) < dropRate // 判断概率
                            || bugleBoss) { // 召唤出的boss必掉鱼雷
                        long goldTorpedoValue = TORPEDO_VALUE[2]; // 黄金鱼雷
                        int roomIndex = gameRoom.getRoomIndex(); // 房间场次信息，第一个场次不掉鱼雷
                        logger.info("winMoney:" + winMoney);
                        if (winMoney / goldTorpedoValue > 0) { // 可以换算成黄金鱼雷  第四个场次掉落
                            itemNum = winMoney / goldTorpedoValue; // 鱼雷数量
                            winMoney = winMoney % goldTorpedoValue; // 零头金币原样返回
                            if (itemNum > 0) {
                                // 历史掉落
                                TORPEDO_RECORD.put("goldDropNumHistory", TORPEDO_RECORD.getOrDefault("goldDropNumHistory", 0L) + itemNum);
                                // 今日掉落
                                TORPEDO_RECORD.put("goldDropNumToday", TORPEDO_RECORD.getOrDefault("goldDropNumToday", 0L) + itemNum);
                                dropItems.add(OseePublicData.ItemDataProto.newBuilder().setItemId(ItemId.GOLD_TORPEDO.getId()).setItemNum(itemNum).build());
                                player.setDropGoldTorpedoNum(player.getDropGoldTorpedoNum() + itemNum);
                                logger.info("龙珠：" + itemNum);
                            }
                        }

                        // 掉落龙晶
                        itemNum2 = winMoney / 2;
                        winMoney = winMoney % 2;

                        if (itemNum2 > 0) {

                            dropItems.add(OseePublicData.ItemDataProto.newBuilder().setItemId(ItemId.DRAGON_CRYSTAL.getId()).setItemNum(itemNum).build());
                            player.setDragonCrystal(player.getDragonCrystal() + itemNum2);
                            logger.info("龙晶：" + itemNum2);
                        }
                        // 保存鱼雷掉落数量记录
                        RedisHelper.set("Fishing:TorpedoDropNum", JSON.toJSONString(TORPEDO_RECORD));
                    }
                }

                if (winMoney > 0) {
                    // 玩家实时金币变化
                    player.addMoney(winMoney);
                }


                if (config.getSkill() > 0) {
                    if (config.getSkill() > 5 && config.getSkill() < 9) { // 特殊技能鱼
                        TtmyFishingChallengeMessage.FishingChallengeUseSkillResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeUseSkillResponse.newBuilder();
                        builder.setPlayerId(player.getId());
                        if (config.getSkill() == 6) { // 局部爆炸鱼
                            builder.setSkillId(101);
                            builder.setSkillFishId(fishId);
                        } else if (config.getSkill() == 7) { // 闪电鱼
                            builder.setSkillId(102);
                            builder.setSkillFishId(fishId);
                        } else if (config.getSkill() == 8) { // 黑洞鱼
                            builder.setSkillId(103);
                            builder.setSkillFishId(fishId);
                        }
                        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_USE_SKILL_RESPONSE_VALUE, builder);
                    } else if (config.getSkill() != 5) { // 会掉落物品的鱼
                        // 随机掉落的技能数量
                        int skillDropNum = ThreadLocalRandom.current().nextInt(config.getMinSkillDropNum(), config.getMaxSkillDropNum() + 1);
                        int skillId = 0;
                        // 1-锁定，2-冰冻，3-急速，4-暴击，5-爆炸鱼
                        if (config.getSkill() == 1) { // 锁定
                            if (skillDropNum > 0) {
                                skillId = ItemId.SKILL_LOCK.getId();
                            }
                        } else if (config.getSkill() == 2) { // 冰冻
                            if (skillDropNum > 0) {
                                skillId = ItemId.SKILL_FROZEN.getId();
                            }
                        } else if (config.getSkill() == 3) { // 急速
                            if (skillDropNum > 0) {
                                skillId = ItemId.SKILL_FAST.getId();
                            }
                        } else if (config.getSkill() == 4) { // 暴击
                            if (skillDropNum > 0) {
                                skillId = ItemId.SKILL_CRIT.getId();
                            }
                        } else if (config.getSkill() == 9) { // 奖券
                            if (skillDropNum > 0) {
                                skillId = ItemId.LOTTERY.getId();
                            }
                        } else if (config.getSkill() == 10) { // 钻石
                            if (skillDropNum > 0) {
                                skillId = ItemId.DIAMOND.getId();
                            }
                        }
                        if (skillId > 0) {
                            dropItems.add(OseePublicData.ItemDataProto.newBuilder().setItemId(skillId).setItemNum(skillDropNum).build());
                        }
                    }
                }
                // 给玩家加掉落的鱼雷或者技能
                for (OseePublicData.ItemDataProto item : dropItems) {
                    // 变动原因为捕鱼产出消耗 ItemChangeReason.FISHING_RESULT
                    PlayerManager.addItem(player.getUser(), item.getItemId(), item.getItemNum(), ItemChangeReason.FISHING_RESULT, true);
                }



                // 爆炸炸死的鱼不向玩家发送消息
                if (!boom) {
                    TtmyFishingChallengeMessage.FishingChallengeFightFishResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeFightFishResponse.newBuilder();
                    builder.setFishId(fishId);
                    builder.setPlayerId(player.getId());
                    builder.setRestMoney(player.getMoney());
                    builder.setDropMoney(winMoney);
                    builder.addAllDropItems(dropItems);
                    builder.setMultiple(randomMoney); // 鱼倍数
                    sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_FIGHT_FISH_RESPONSE_VALUE, builder);

                    // 游走字幕播报
                    if (fish.getFishType() == 100) { // boss鱼才播报
                        String text;
                        if (itemNum != 0)
                            text = String.format(
                                AutoWanderSubtitle.TEMPLATES[10],
                                player.getUser().getNickname(), randomMoney, config.getName(), itemNum
                            );
                        else
                            text = String.format(
                                    AutoWanderSubtitle.TEMPLATES[9],
                                    player.getUser().getNickname(), randomMoney, config.getName(), itemNum2 / 10000
                            );

                        // 给全部在线玩家推送游走字幕消息
                        PlayerManager.sendMessageToOnline(LobbyMessage.LobbyMsgCode.S_C_WANDER_SUBTITLE_RESPONSE_VALUE,
                                LobbyMessage.WanderSubtitleResponse.newBuilder().setLevel(1).setContent(text).build());
                    }
                }
            }
        }
        return configs;
    }

    /**
     * 发送重新激活消息相关消息
     */
    public void sendReactiveMessage(FishingChallengeRoom gameRoom, FishingChallengePlayer gamePlayer) {

        int roomType = 0;
        if(room1.containsKey(gamePlayer.getUser())) {
            roomType = 1;
        } else if (room2.containsKey(gamePlayer.getUser())) {
            roomType = 2;
        } else if (room3.containsKey(gamePlayer.getUser())) {
            roomType = 3;
        }
        sendJoinRoomResponse(gameRoom, gamePlayer, roomType); // 发送加入房间消息
        sendRoomPlayerInfoListResponse(gameRoom, gamePlayer); // 发送玩家数据列表消息
        sendSynchroniseResponse(gameRoom, gamePlayer); // 发送同步鱼消息
        sendFrozenMessage(gameRoom, gamePlayer); // 发送房间当前冰冻消息
    }

    /**
     * 玩家使用技能
     */
    public void useSkill(FishingChallengeRoom gameRoom, FishingChallengePlayer player, int skillId) {
        // 技能id有误
        if ((skillId < ItemId.SKILL_LOCK.getId() || skillId > ItemId.SKILL_CRIT.getId())
                && skillId != ItemId.FEN_SHEN.getId()) {
            return;
        }

        ServerUser user = player.getUser();
        // 技能数量不足
        if (!PlayerManager.checkItem(user, skillId, 1)) {
            return;
        }

        TtmyFishingChallengeMessage.FishingChallengeUseSkillResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeUseSkillResponse.newBuilder();
        builder.setSkillId(skillId);
        builder.setPlayerId(player.getId());

        long nowTime = System.currentTimeMillis();
        if (skillId == ItemId.SKILL_LOCK.getId()) { // 锁定
            if (nowTime - player.getLastLockTime() < FishingManager.SKILL_LOCK_TIME) {
                NetManager.sendHintMessageToClient("技能冷却中", user);
                return;
            }
            builder.setDuration((int) (FishingManager.SKILL_LOCK_TIME / 1000));
            player.setLastLockTime(nowTime);
        } else if (skillId == ItemId.SKILL_FROZEN.getId()) { // 冰冻
            if (nowTime - player.getLastFrozenTime() < FishingManager.SKILL_FROZEN_TIME) {
                NetManager.sendHintMessageToClient("技能冷却中", user);
                return;
            }
            builder.setDuration((int) (FishingManager.SKILL_FROZEN_TIME / 1000));
            player.setLastFrozenTime(nowTime);
            gameRoom.setLastRoomFrozenTime(nowTime);

            for (Map.Entry<FishRefreshRule, Long> refresh : gameRoom.getNextRefreshTime().entrySet()) {
                refresh.setValue(refresh.getValue() + FishingManager.SKILL_FROZEN_TIME / 1000); // 延迟冰冻持续时间段刷鱼
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
        } else if (skillId == ItemId.SKILL_FAST.getId()) { // 急速
            if (nowTime - player.getLastFastTime() < FishingManager.SKILL_FAST_TIME) {
                NetManager.sendHintMessageToClient("技能冷却中", user);
                return;
            }
            builder.setDuration((int) (FishingManager.SKILL_FAST_TIME / 1000));
            player.setLastFastTime(nowTime);
        } else if (skillId == ItemId.SKILL_CRIT.getId()) { // 暴击
            int vipLevel = PlayerManager.getPlayerVipLevel(user);
            if (vipLevel < 4) {
                NetManager.sendHintMessageToClient("VIP4及以上才可以使用暴击技能", user);
                return;
            }
            if (nowTime - player.getLastCritTime() < FishingManager.SKILL_CRIT_TIME) {
                NetManager.sendHintMessageToClient("技能冷却中", user);
                return;
            }
            builder.setDuration((int) (FishingManager.SKILL_CRIT_TIME / 1000));
            player.setLastCritTime(nowTime);
        } else if (skillId == ItemId.FEN_SHEN.getId()) { // 分身炮道具
            int vipLevel = PlayerManager.getPlayerVipLevel(user);
            if (vipLevel < 3) {
                NetManager.sendHintMessageToClient("VIP3及以上才可以使用分身炮", user);
                return;
            }
            if (nowTime - player.getLastFenShenTime() < FishingManager.SKILL_FEN_SHEN_TIME) {
                NetManager.sendHintMessageToClient("技能冷却中", user);
                return;
            }
            builder.setDuration((int) (FishingManager.SKILL_FEN_SHEN_TIME / 1000));
            player.setLastFenShenTime(nowTime);
        }

        // 扣除使用的技能数量
        PlayerManager.addItem(user, skillId, -1, ItemChangeReason.USE_ITEM, true);

        builder.setRestMoney(player.getMoney());
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_USE_SKILL_RESPONSE_VALUE, builder);
    }

    /**
     * 捕捉到特殊鱼
     */
    public void catchSpecialFish(FishingChallengeRoom gameRoom, long playerId, List<Long> fishIdsList, long specialFishId) {
        FishingChallengePlayer player = gameRoom.getGamePlayerById(playerId);
        if (player == null) {
            return;
        }
        long beforeMoney = player.getMoney();
        List<FishConfig> configs = playerFightFish(gameRoom, player, -1, fishIdsList, true);
        if (configs != null) {
            TtmyFishingChallengeMessage.FishingChallengeCatchSpecialFishResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeCatchSpecialFishResponse.newBuilder();
            builder.setSpecialFishId(specialFishId);
            builder.addAllFishIds(fishIdsList);
            builder.setPlayerId(player.getId());
            builder.setDropMoney(player.getMoney() - beforeMoney);
            builder.setRestMoney(player.getMoney());
            sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_CATCH_SPECIAL_FISH_RESPONSE_VALUE, builder);
        }
    }

    /**
     * 玩家使用BOSS号角
     */
    public void useBossBugle(FishingChallengeRoom gameRoom, FishingChallengePlayer player) {
        ServerUser user = player.getUser();
        if (PlayerManager.getPlayerVipLevel(user) < 3) {
            NetManager.sendHintMessageToClient("VIP3及以上才可以使用BOSS号角", user);
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - gameRoom.getLastRoomFrozenTime() < FishingManager.SKILL_FROZEN_TIME || gameRoom.isFishTide()) {
            NetManager.sendHintMessageToClient("房间冰冻中或者鱼潮中不能召唤boss", user);
            return;
        }
        if (currentTimeMillis - gameRoom.getLastBossBugleTime() < FishingManager.BOSS_BUGLE_COOL_TIME) {
            NetManager.sendHintMessageToClient("房间BOSS号角使用冷却中(5分钟)", user);
            return;
        }
        if (!PlayerManager.checkItem(user, ItemId.BOSS_BUGLE, 1)) {
            NetManager.sendHintMessageToClient("BOSS号角数量不足", user);
            return;
        }
        if (gameRoom.getBoss() != 0) {
            NetManager.sendHintMessageToClient("房间内还存在BOSS，不能召唤", user);
            return;
        }
        gameRoom.setLastBossBugleTime(currentTimeMillis);
        // 房间置为有boss的状态
        gameRoom.setBoss(2);
        // 扣除使用的号角数量
        PlayerManager.addItem(user, ItemId.BOSS_BUGLE, -1, ItemChangeReason.USE_ITEM, true);
        // 获取当前场次的boss刷新规则
        List<FishRefreshRule> bossRefreshRules = gameRoom.getBossRefreshRules();
        // 随机一个刷新规则
        FishRefreshRule refreshRule = bossRefreshRules.get(ThreadLocalRandom.current().nextInt(0, bossRefreshRules.size()));
        // 按照boss规则刷新
        refreshGroupFish(gameRoom, refreshRule.getId());
        // 发送响应
        TtmyFishingChallengeMessage.FishingChallengeUseBossBugleResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeUseBossBugleResponse.newBuilder();
        builder.setPlayerId(player.getId());
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_CHALLENGE_USE_BOSS_BUGLE_RESPONSE_VALUE, builder);
    }

    /**
     * 玩家重连
     */
    public void reconnect(FishingChallengeRoom gameRoom, FishingChallengePlayer gamePlayer) {
        logger.info("玩家[{}]重连捕鱼挑战赛房间[{}]", gamePlayer.getUser().getNickname(), gameRoom.getCode());

        int roomType = 0;
        if(room1.containsKey(gamePlayer.getUser())) {
            roomType = 1;
        } else if (room2.containsKey(gamePlayer.getUser())) {
            roomType = 2;
        } else if (room3.containsKey(gamePlayer.getUser())) {
            roomType = 3;
        }

        sendJoinRoomResponse(gameRoom, gamePlayer, roomType); // 发送加入房间响应
        sendRoomPlayerInfoListResponse(gameRoom, gamePlayer); // 发送房间所有玩家信息
        sendSynchroniseResponse(gameRoom, gamePlayer); // 发送同步鱼响应
        sendFrozenMessage(gameRoom, gamePlayer); // 发送房间冰冻响应
    }

    /**
     * VIP换座
     */
    public void changeSeat(FishingChallengeRoom gameRoom, FishingChallengePlayer player, int seat) {
        if (seat < 0 || seat >= gameRoom.getMaxSize()) {
            NetManager.sendErrorMessageToClient("座位序号有误", player.getUser());
            return;
        }
        if (!gameRoom.isVip()) {
            NetManager.sendErrorMessageToClient("VIP房间才能换座", player.getUser());
            return;
        }
        synchronized (gameRoom) {
            if (gameRoom.getGamePlayerBySeat(seat) != null) {
                NetManager.sendErrorMessageToClient("该座位已经有玩家了哦", player.getUser());
                return;
            }
            // 当前座位号
            int nowSeat = player.getSeat();
            gameRoom.getGamePlayers()[nowSeat] = null; // 清空之前座位的玩家信息
            gameRoom.getGamePlayers()[seat] = player; // 将自己的信息移到新座位
            gameRoom.getGamePlayers()[seat].setSeat(seat); // 玩家设置新座位号
            sendRoomPlayerInfoResponse(gameRoom, player);
        }
    }

    /**
     * 玩家在房间使用鱼雷
     */
    public void useTorpedo(FishingChallengeRoom gameRoom, FishingGamePlayer player, int torpedoId, int torpedoNum,
                           float angle) {
        // 鱼雷id有误
        if (torpedoId < ItemId.BRONZE_TORPEDO.getId() || torpedoId > ItemId.GOLD_TORPEDO.getId()) {
            return;
        }

        ServerUser user = player.getUser();
        if (!PlayerManager.checkItem(user, torpedoId, torpedoNum)) {
            return;
        }

        // 游戏中使用鱼雷可直接获得金币
        long money = 0;

        if (torpedoId == ItemId.GOLD_TORPEDO.getId()) {
            money = 100 * 10000 * torpedoNum;
            // 历史使用
            FishingManager.TORPEDO_RECORD.put("goldUseNumHistory", FishingManager.TORPEDO_RECORD.getOrDefault("goldUseNumHistory", 0L) + torpedoNum);
            // 今日使用
            FishingManager.TORPEDO_RECORD.put("goldUseNumToday", FishingManager.TORPEDO_RECORD.getOrDefault("goldUseNumToday", 0L) + torpedoNum);
        }
        // 保存鱼雷使用数量记录
        RedisHelper.set("Fishing:TorpedoDropNum", JSON.toJSONString(FishingManager.TORPEDO_RECORD));
        List<ItemData> itemDataList = new LinkedList<>();
        // 扣除玩家鱼雷
        itemDataList.add(new ItemData(torpedoId, -torpedoNum));
        // 增加玩家金币
        itemDataList.add(new ItemData(ItemId.DRAGON_CRYSTAL.getId(), money));
        PlayerManager.addItems(user, itemDataList, ItemChangeReason.USE_ITEM, true);
        //player.setTorpedoMoney(player.getTorpedoMoney() + money);

        TtmyFishingChallengeMessage.FishingChallengeUseTorpedoResponse.Builder builder = TtmyFishingChallengeMessage.FishingChallengeUseTorpedoResponse.newBuilder();
        builder.setPlayerId(player.getId());
        builder.setTorpedoId(torpedoId);
        builder.setTorpedoNum(torpedoNum);
        builder.setAngle(angle);
        builder.setMoney(money);
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_Challenge_USE_TORPEDO_RESPONSE_VALUE, builder);
    }

}
