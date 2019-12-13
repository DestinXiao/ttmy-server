package com.maple.game.osee.manager.fishing;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.Message;
import com.maple.common.lobby.proto.LobbyMessage.LobbyMsgCode;
import com.maple.common.lobby.proto.LobbyMessage.WanderSubtitleResponse;
import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.container.DataContainer;
import com.maple.engine.data.ServerUser;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.dao.log.entity.OseeCutMoneyLogEntity;
import com.maple.game.osee.dao.log.entity.OseeFishingRecordLogEntity;
import com.maple.game.osee.dao.log.entity.OseePlayerTenureLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeCutMoneyLogMapper;
import com.maple.game.osee.dao.log.mapper.OseeFishingRecordLogMapper;
import com.maple.game.osee.dao.log.mapper.OseePlayerTenureLogMapper;
import com.maple.game.osee.entity.GameEnum;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.FishingGamePlayer;
import com.maple.game.osee.entity.fishing.FishingGameRoom;
import com.maple.game.osee.entity.fishing.csv.file.*;
import com.maple.game.osee.entity.fishing.game.FireStruct;
import com.maple.game.osee.entity.fishing.game.FishStruct;
import com.maple.game.osee.entity.fishing.task.GoalType;
import com.maple.game.osee.entity.fishing.task.TaskType;
import com.maple.game.osee.entity.robot.fishing.FishingGameRobot;
import com.maple.game.osee.manager.AgentManager;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.manager.fishing.util.FishingUtil;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.OseePublicData.ItemDataProto;
import com.maple.game.osee.proto.fishing.OseeFishingMessage;
import com.maple.game.osee.proto.fishing.OseeFishingMessage.*;
import com.maple.game.osee.timer.AutoWanderSubtitle;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.data.fishing.BaseFishingRoom;
import com.maple.gamebase.manager.fishing.BaseFishingManager;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 1688捕鱼管理类
 */
@Component
public class FishingManager extends BaseFishingManager {

    private static Logger logger = LoggerFactory.getLogger(FishingManager.class);

//    @Autowired
//    private OseeExpendLogMapper expendLogMapper;

    @Autowired
    private OseePlayerTenureLogMapper tenureLogMapper;

    @Autowired
    private OseeCutMoneyLogMapper cutMoneyLogMapper;

    @Autowired
    private OseeFishingRecordLogMapper fishingRecordLogMapper;

    @Autowired
    private OseePlayerMapper playerMapper;

    @Autowired
    private FishingRobotManager robotManager;

    @Autowired
    protected AgentManager agentManager;

    /**
     * 循环时间
     */
    private static final long LOOP_TIME = 1000;

    /**
     * 默认鱼存活时间
     */
    public static final int DEFAULT_LIFE_TIME = 120;

    /**
     * 技能锁定持续时间
     */
    public static final long SKILL_LOCK_TIME = 20000;

    /**
     * 技能急速持续时间
     */
    public static final long SKILL_FAST_TIME = 20000;

    /**
     * 技能冰冻持续时间
     */
    public static final long SKILL_FROZEN_TIME = 10000;

    /**
     * 技能暴击持续时间
     */
    public static final long SKILL_CRIT_TIME = 10000;

    /**
     * 分身炮持续时间
     */
    public static final long SKILL_FEN_SHEN_TIME = 20000;

    /**
     * 未操作踢出房间的时长
     */
    public static final long ROOM_KICK_TIME = 5 * 60 * 1000; // 五分钟

    /**
     * 房间使用boss号角的冷却时长
     */
    public static final long BOSS_BUGLE_COOL_TIME = 5 * 60 * 1000; // 五分钟

    /**
     * 炮台等级限制
     */
    public static final int[][] batteryLevelLimit = {
            {Integer.MAX_VALUE, Integer.MIN_VALUE},
            {Integer.MAX_VALUE, Integer.MIN_VALUE},
            {Integer.MAX_VALUE, Integer.MIN_VALUE},
            {Integer.MAX_VALUE, Integer.MIN_VALUE},
            {Integer.MAX_VALUE, Integer.MIN_VALUE},
            {Integer.MAX_VALUE, Integer.MIN_VALUE}
    };

    /**
     * 房间进入金币限制
     */
    public static final long[] enterLimit = {0, 100000, 2000000, 5000000, 0, 100000};

    /**
     * 各个鱼雷的金币价值：青铜、白银、黄金
     */
    public static long[] TORPEDO_VALUE = {200000, 1000000, 2000000};

    /**
     * 免费玩家掉落几率
     */
    public static double TORPEDO_DROP_FREE_RATE = -1;

    /**
     * 付费玩家每充值金钱数
     */
    public static long TORPEDO_DROP_PER_PAY_MONEY = -1;

    /**
     * 付费玩家每充值提升几率
     */
    public static double TORPEDO_DROP_PER_PAY_RATE = -1;

    /**
     * 免费玩家掉落几率
     */
    public static double CHALLENGE_DROP_FREE_RATE = -1;

    /**
     * 付费玩家每充值金钱数
     */
    public static long CHALLENGE_DROP_PER_PAY_MONEY = -1;

    /**
     * 付费玩家每充值提升几率
     */
    public static double CHALLENGE_DROP_PER_PAY_RATE = -1;

    /**
     * 记录掉落和使用的鱼雷数量的Map
     */
    public static ConcurrentHashMap<String, Long> TORPEDO_RECORD = new ConcurrentHashMap<>();

    public FishingManager() {
        super(LOOP_TIME);
        List<BatteryLevelConfig> configs = DataContainer.getDatas(BatteryLevelConfig.class);
        for (BatteryLevelConfig config : configs) {
            int scene = config.getScene() - 1;

            batteryLevelLimit[scene][0] = Math.min(batteryLevelLimit[scene][0], config.getBatteryLevel());
            batteryLevelLimit[scene][1] = Math.max(batteryLevelLimit[scene][1], config.getBatteryLevel());
        }
    }

    /**
     * 玩家加入房间
     */
    public void playerJoinRoom(ServerUser user, int roomIndex) {
        System.out.println(user + " " + roomIndex);
        if (!PlayerManager.checkItem(user, ItemId.MONEY, enterLimit[roomIndex - 1])) {
            NetManager.sendHintMessageToClient("携带金币不足，无法进入该房间", user);
            return;
        }

        if (PlayerManager.getPlayerBatteryLevel(user) < batteryLevelLimit[roomIndex - 1][0]) {
            // 炮台等级不足，发送解锁炮台提示
            unlockBatteryLevelHint(user, batteryLevelLimit[roomIndex - 1][0]);
            return;
        }

        List<FishingGameRoom> gameRooms = GameContainer.getGameRooms(FishingGameRoom.class);
        for (FishingGameRoom gameRoom : gameRooms) {
            // 加入房间条件: 1:目标房间场次与玩家所选场次相同 2:房间人数未满
            if (gameRoom.getRoomIndex() == roomIndex && gameRoom.getMaxSize() > gameRoom.getPlayerSize()) {
                synchronized (gameRoom) {
                    joinFishingRoom(user, gameRoom);
                }
                return;
            }
        }
        // 没有房间就新建一个房间
        FishingGameRoom gameRoom = createFishingRoom(roomIndex);
        synchronized (gameRoom) {
            joinFishingRoom(user, gameRoom);
        }
    }

    /**
     * 创建捕鱼房间
     */
    private FishingGameRoom createFishingRoom(int roomIndex) {
        FishingGameRoom gameRoom = GameContainer.createGameRoom(FishingGameRoom.class, 4);
        gameRoom.setRoomIndex(roomIndex);
        if (FishingRobotManager.USE_ROBOT != 0) { // 使用机器人才生成默认机器人
            ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> {
                // 加几个机器人占座
                int robotNum = ThreadLocalRandom.current().nextInt(1, FishingRobotManager.ROBOT_COUNT + 1);
                for (int i = 0; i < robotNum; i++) {
                    FishingGameRobot robotPlayer = robotManager.createRobotPlayer(gameRoom);
                    if (robotPlayer != null) {
                        // 将自己的信息发送给房间内所有玩家
                        sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_PLAYER_INFO_RESPONSE_VALUE, createPlayerInfoResponse(gameRoom, robotPlayer));
                        // 机器人自动发炮的定时任务
                        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> robotManager.robotFire(gameRoom, robotPlayer.getId()), 0, TimeUnit.SECONDS);
                    }
                }
            }, 0, TimeUnit.SECONDS);
        }
        return gameRoom;
    }

    /**
     * 加入捕鱼房间
     */
    private void joinFishingRoom(ServerUser user, FishingGameRoom gameRoom) {
        long enterMoney = PlayerManager.getPlayerEntity(user).getMoney();
        FishingGamePlayer gamePlayer = GameContainer.createGamePlayer(gameRoom, user, FishingGamePlayer.class);
        gamePlayer.setEnterMoney(enterMoney);
        gamePlayer.setEnterRoomTime(System.currentTimeMillis());

        // 设置玩家在房间内的初始炮台等级
        // 玩家拥有的最高炮台等级
        int batteryLevel = PlayerManager.getPlayerEntity(user).getBatteryLevel();
        int bMax = batteryLevelLimit[gameRoom.getRoomIndex() - 1][1];
        if (batteryLevel > bMax) { // 高于房间内最高使用炮台等级就用房间最高的；低于房间内最低使用炮台等级不能进房间了
            batteryLevel = bMax;
        }
        gamePlayer.setBatteryLevel(batteryLevel);

        sendJoinRoomResposne(gameRoom, user); // 发送加入房间消息
        sendPlayersInfoResponse(gameRoom, user); // 发送玩家数据列表消息
        sendSynchroniseResponse(gameRoom, user); // 发送同步鱼消息
        sendFrozenMessage(gameRoom, user); // 发送房间当前冰冻消息
        // 将自己的数据广播到房间所有玩家
        sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_PLAYER_INFO_RESPONSE_VALUE, createPlayerInfoResponse(gameRoom, gamePlayer));
//        for (int i = 0; i < gameRoom.getMaxSize(); i++) {
//            FishingGamePlayer player = gameRoom.getGamePlayerBySeat(i);
//            if (player == null || !player.getUser().isOnline() || player.getId() == user.getId()) {
//                continue;
//            }
//            NetManager.sendMessage(OseeMsgCode.S_C_OSEE_FISHING_PLAYER_INFO_RESPONSE_VALUE, resp, player.getUser());
//        }
    }

    /**
     * 玩家重连
     */
    public void reconnect(FishingGameRoom gameRoom, FishingGamePlayer gamePlayer) {
        logger.info("玩家[{}]重连普通捕鱼房间[{}]", gamePlayer.getUser().getNickname(), gameRoom.getCode());
        ServerUser user = gamePlayer.getUser();
        sendJoinRoomResposne(gameRoom, user); // 发送加入房间消息
        sendPlayersInfoResponse(gameRoom, user); // 发送玩家数据列表消息
        sendSynchroniseResponse(gameRoom, user); // 发送同步鱼消息
        sendFrozenMessage(gameRoom, user); // 发送房间当前冰冻消息
    }

    /**
     * 离开捕鱼房间
     */
    public void exitFishingRoom(FishingGameRoom gameRoom, ServerUser user) {
        FishingGamePlayer player = gameRoom.getGamePlayerById(user.getId());

        FishingExitRoomResponse.Builder builder = FishingExitRoomResponse.newBuilder();
        builder.setPlayerId(user.getId());
        sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_EXIT_ROOM_RESPONSE_VALUE, builder.build());

        long stayTime = (System.currentTimeMillis() - player.getEnterRoomTime()) / 1000;
        // 做累计在线任务 分钟
        FishingTaskManager.doTask(user, TaskType.DAILY, GoalType.ONLINE, 0, (int) (stayTime / 60));

        OseePlayerEntity entity = PlayerManager.getPlayerEntity(user);
        if (entity != null) {
            agentManager.addActiveMoney(user.getId(), GameEnum.FISHING, player.getCutMoney(), 0);
            // 保存玩家金币变化记录
            if (player.getChangeMoney() != 0) {
                // 保存到账户变动记录
                OseePlayerTenureLogEntity log = new OseePlayerTenureLogEntity();
                log.setUserId(user.getId());
                log.setNickname(user.getNickname());
                log.setReason(ItemChangeReason.FISHING_RESULT.getId());
                log.setPreBankMoney(entity.getBankMoney());
                log.setPreMoney(player.getEnterMoney());
                log.setPreLottery(entity.getLottery());
                log.setChangeMoney(player.getChangeMoney() - player.getTorpedoMoney());
                tenureLogMapper.save(log);
                // 保存到捕鱼记录
                OseeFishingRecordLogEntity recordLogEntity = new OseeFishingRecordLogEntity();
                recordLogEntity.setPlayerId(user.getId());
                recordLogEntity.setRoomIndex(gameRoom.getRoomIndex());
                recordLogEntity.setSpendMoney(player.getSpendMoney());
                recordLogEntity.setWinMoney(player.getWinMoney() - player.getTorpedoMoney());
                recordLogEntity.setDropBronzeTorpedoNum(player.getDropBronzeTorpedoNum());
                recordLogEntity.setDropSilverTorpedoNum(player.getDropSilverTorpedoNum());
                recordLogEntity.setDropGoldTorpedoNum(player.getDropGoldTorpedoNum());
                fishingRecordLogMapper.save(recordLogEntity);
            }
            // 保存抽水记录
            if (player.getCutMoney() != 0) {
                OseeCutMoneyLogEntity cutLog = new OseeCutMoneyLogEntity();
                cutLog.setUserId(user.getId());
                cutLog.setGame(GameEnum.FISHING.getId());
                cutLog.setCutMoney(player.getCutMoney());
                cutMoneyLogMapper.save(cutLog);
            }
            playerMapper.update(PlayerManager.getPlayerEntity(user));
        }
        GameContainer.removeGamePlayer(gameRoom, gameRoom.getGamePlayerById(user.getId()).getSeat(), true);
    }

    /**
     * 玩家发射子弹
     */
    public void playerFire(FishingGameRoom gameRoom, FishingGamePlayer player, FireStruct fire) {
        long needMoney = player.getBatteryLevel() * player.getBatteryMult();
        if (System.currentTimeMillis() - player.getLastFenShenTime() < SKILL_FEN_SHEN_TIME) { // 还在分身阶段就要扣三发子弹的钱
            int fireCount = 3;
            needMoney *= fireCount;
            fire.setCount(fireCount);
        }

        if (!PlayerManager.checkItem(player.getUser(), ItemId.MONEY, needMoney)) {
            return;
        }

        int index = gameRoom.getRoomIndex() - 1;
        FishingHitDataManager.addBlackRoom(player.getId(), index, -needMoney);

        player.setLastFireTime(System.currentTimeMillis());
        player.addMoney(-needMoney);
        FishingHitDataManager.addWin(gameRoom, player, -needMoney);

        fire.setLevel(player.getBatteryLevel());
        fire.setMult(player.getBatteryMult());
        player.getFireMap().put(fire.getId(), fire);

        FishingFireResponse.Builder builder = FishingFireResponse.newBuilder();
        builder.setFireId(fire.getId());
        builder.setFishId(fire.getFishId());
        builder.setAngle(fire.getAngle());
        builder.setRestMoney(player.getMoney());
        builder.setPlayerId(player.getId());
        sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_FIRE_RESPONSE_VALUE, builder.build());

        // 添加任务进度
        FishingTaskManager.doTask(player.getUser(), TaskType.DAILY, GoalType.FIRE, 0, fire.getCount());
        // 房间任务
        FishingTaskManager.doTask(player.getUser(), TaskType.ROOM, GoalType.FIRE, 0, fire.getCount());
    }

    /**
     * 玩家击中鱼
     */
    public List<FishConfig> playerFightFish(FishingGameRoom gameRoom, FishingGamePlayer player, long fireId,
                                            List<Long> fishIds, boolean boom) {
        try {
            List<FishConfig> configs = fightFish(gameRoom, player, fireId, fishIds, boom);

            if (configs != null) {
                for (FishConfig config : configs) {
                    // 玩家加经验
                    addExperience(player.getUser(), config.getExp());
                    // 添加任务进度
                    // 做每日任务
                    FishingTaskManager.doTask(player.getUser(), TaskType.DAILY, GoalType.FISH, 0, 1);
                    // 房间打鱼任务 任意鱼
                    FishingTaskManager.doTask(player.getUser(), TaskType.ROOM, GoalType.FISH, 0, 1);
                    // 击中指定的鱼
                    FishingTaskManager.doTask(player.getUser(), TaskType.ROOM, GoalType.FISH, config.getModelId(), 1);
                    if (config.getMoney() > 50) { // 倍数大于50倍的
                        // 添加任务进度
                        FishingTaskManager.doTask(player.getUser(), TaskType.DAILY, GoalType.FISH_50MULTI, 0, 1);
                    }
                }
            }

            return configs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 增加经验
     */
    public static void addExperience(ServerUser user, long experience) {
        OseePlayerEntity entity = PlayerManager.getPlayerEntity(user);

        synchronized (entity) {
            long exp = entity.getExperience() + experience;
            entity.setExperience(exp);

            while (entity.getExperience() > 0) {
                int level = entity.getLevel();
                List<PlayerLevelConfig> configs = DataContainer.getDatas(PlayerLevelConfig.class);
                PlayerLevelConfig cfg = null;
                for (PlayerLevelConfig config : configs) {
                    if (config.getLevel() == level) {
                        cfg = config;
                        break;
                    }
                }

                if (cfg != null && cfg.getExp() <= entity.getExperience()) {
                    entity.setExperience(entity.getExperience() - cfg.getExp());
                    entity.setLevel(entity.getLevel() + 1);

                    FishingLevelUpResponse.Builder builder = FishingLevelUpResponse.newBuilder();
                    builder.setLevel(entity.getLevel());
                    List<ItemData> rewards = cfg.getRealRewards();
                    for (ItemData reward : rewards) {
                        builder.addRewards(ItemDataProto.newBuilder()
                                .setItemId(reward.getItemId())
                                .setItemNum(reward.getCount())
                                .build());
                    }
                    // 给予奖励物品
                    PlayerManager.addItems(user, rewards, ItemChangeReason.LEVEL_UP, true);
                    NetManager.sendMessage(OseeMsgCode.S_C_OSEE_FISHING_LEVEL_UP_RESPONSE_VALUE, builder, user);
                } else {
                    break;
                }
            }
        }
    }

    /**
     * 击中鱼基础任务
     */
    public static List<FishConfig> fightFish(FishingGameRoom gameRoom, FishingGamePlayer player, long fireId,
                                             List<Long> fishIds, boolean boom) {
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

                if (config.getSkill() > 5 && config.getSkill() < 9
                        && player instanceof FishingGameRobot && config.getFishType() == 100) { // 机器人无法打死特殊技能鱼
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

                    // 暴击状态下成功命中鱼类，25%概率获得2倍金币奖励
                    if (System.currentTimeMillis() - player.getLastCritTime() < SKILL_CRIT_TIME && new Random().nextInt(100) < 25) {
                        winMoney *= 2;
                    }
                } else {
                    winMoney = randomMoney * player.getBatteryLevel() * player.getBatteryMult();
                }

                if (winMoney >= 10 * 10000) { // 获得10万金币
                    // 添加任务进度
                    FishingTaskManager.doTask(player.getUser(), TaskType.DAILY, GoalType.MONEY_10W, 0, 1);
                }

                // Boss鱼死亡
                if (config.getFishType() == 100 && player.getBatteryLevel() >= 1000) { // 炮台等级要1000倍以上才通报
                    // 进行全服通报
                    CatchBossFishResponse.Builder builder = CatchBossFishResponse.newBuilder();
                    builder.setFishName(config.getName());
                    builder.setMoney(winMoney);
                    builder.setPlayerName(player.getUser().getNickname());
                    builder.setPlayerVipLevel(player.getVipLevel());
                    builder.setBatteryLevel(player.getBatteryLevel());
                    sendCatchBossFishResponse(builder.build());
                }

                // 记录玩家金币相关数据
                if (!(player instanceof FishingGameRobot)) {
                    FishingHitDataManager.addWin(gameRoom, player, winMoney);
                }
                // 玩家小黑屋
                int greener = FishingHitDataManager.getGreener(player);
                int index = gameRoom.getRoomIndex() - 1;
                if (winMoney > FishingHitDataManager.BLACK_ROOM_LIMIT[greener][index]) {
                    FishingHitDataManager.addBlackRoom(player.getId(), index, winMoney);
                }

                // 打死鱼之后掉落的物品
                List<ItemDataProto> dropItems = new LinkedList<>();
                // Boss死亡或者可以掉鱼雷的鱼掉落鱼雷给玩家
                if ((config.getFishType() == 100 || config.getFishType() == 10) && !(player instanceof FishingGameRobot)) {
                    // 判断鱼雷掉落几率
                    getTorpedoDropRate();
                    // 掉落几率=免费几率+付费几率
                    OseePlayerEntity playerEntity = PlayerManager.getPlayerEntity(player.getUser());
                    long rechargeMoney = 0;
                    if (playerEntity != null) {
                        rechargeMoney = playerEntity.getRechargeMoney();
                    }
                    // 鱼雷掉落概率
                    double dropRate = FishingManager.TORPEDO_DROP_FREE_RATE +
                            ((int) (rechargeMoney / FishingManager.TORPEDO_DROP_PER_PAY_MONEY)) * FishingManager.TORPEDO_DROP_PER_PAY_RATE;
                    boolean bugleBoss = gameRoom.getBoss() == 2; // 是否是boss号角召唤出来的boss
                    if (bugleBoss) { // boss号角召唤出来的boss
                        gameRoom.setLastBossBugleTime(0L); // 冷却时间失效
                    }
                    gameRoom.setBoss(0); // 召唤的boss被击杀，房间无boss
                    if (ThreadLocalRandom.current().nextDouble(0, 100) < dropRate // 判断概率
                            || bugleBoss) { // 召唤出的boss必掉鱼雷
                        long goldTorpedoValue = TORPEDO_VALUE[2]; // 黄金鱼雷
//                        long silverTorpedoValue = TORPEDO_VALUE[1]; // 白银鱼雷
//                        long bronzeTorpedoValue = TORPEDO_VALUE[0]; // 青铜鱼雷
                        long itemNum;
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
                                dropItems.add(ItemDataProto.newBuilder().setItemId(ItemId.GOLD_TORPEDO.getId()).setItemNum(itemNum).build());
                                player.setDropGoldTorpedoNum(player.getDropGoldTorpedoNum() + itemNum);
                                logger.info("龙珠：" + itemNum);
                            }
                        }

                        // 掉落龙晶
                        itemNum = winMoney / 2;
                        winMoney = winMoney % 2;

                        if (itemNum > 0) {

                            dropItems.add(ItemDataProto.newBuilder().setItemId(ItemId.DRAGON_CRYSTAL.getId()).setItemNum(itemNum).build());
                            player.setDragonCrystal(player.getDragonCrystal() + itemNum);
                            logger.info("龙晶：" + itemNum);
                        }


//                        if (roomIndex >= 3 && winMoney / silverTorpedoValue > 0) { // 可以换算成白银鱼雷  第三、四个场次掉落
//                            itemNum = winMoney / silverTorpedoValue; // 鱼雷数量
//                            winMoney = winMoney % silverTorpedoValue; // 零头金币原样返回
//                            if (itemNum > 0) {
//                                // 历史掉落
//                                TORPEDO_RECORD.put("silverDropNumHistory", TORPEDO_RECORD.getOrDefault("silverDropNumHistory", 0L) + itemNum);
//                                // 今日掉落
//                                TORPEDO_RECORD.put("silverDropNumToday", TORPEDO_RECORD.getOrDefault("silverDropNumToday", 0L) + itemNum);
//                                dropItems.add(ItemDataProto.newBuilder().setItemId(ItemId.SILVER_TORPEDO.getId()).setItemNum(itemNum).build());
//                                player.setDropSilverTorpedoNum(player.getDropSilverTorpedoNum() + itemNum);
//                            }
//                        }
//                        if (roomIndex >= 2 && winMoney / bronzeTorpedoValue > 0) { // 可以换算成青铜鱼雷  第二、三、四个场次掉落
//                            itemNum = winMoney / bronzeTorpedoValue; // 鱼雷数量
//                            winMoney = winMoney % bronzeTorpedoValue; // 零头金币原样返回
//                            if (itemNum > 0) {
//                                // 历史掉落
//                                TORPEDO_RECORD.put("bronzeDropNumHistory", TORPEDO_RECORD.getOrDefault("bronzeDropNumHistory", 0L) + itemNum);
//                                // 今日掉落
//                                TORPEDO_RECORD.put("bronzeDropNumToday", TORPEDO_RECORD.getOrDefault("bronzeDropNumToday", 0L) + itemNum);
//                                dropItems.add(ItemDataProto.newBuilder().setItemId(ItemId.BRONZE_TORPEDO.getId()).setItemNum(itemNum).build());
//                                player.setDropBronzeTorpedoNum(player.getDropBronzeTorpedoNum() + itemNum);
//                            }
//                        }
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
                        FishingUseSkillResponse.Builder builder = FishingUseSkillResponse.newBuilder();
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
                        sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_USE_SKILL_RESPONSE_VALUE, builder.build());
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
                                // 做任务
                                FishingTaskManager.doTask(player.getUser(), TaskType.DAILY, GoalType.GET_DIAMOND, 0, 1);
                            }
                        }
                        if (skillId > 0) {
                            dropItems.add(ItemDataProto.newBuilder().setItemId(skillId).setItemNum(skillDropNum).build());
                        }
                    }
                }
                if (!(player instanceof FishingGameRobot)) { // 机器人不增加物品
                    // 给玩家加掉落的鱼雷或者技能
                    for (ItemDataProto item : dropItems) {
                        // 变动原因为捕鱼产出消耗 ItemChangeReason.FISHING_RESULT
                        PlayerManager.addItem(player.getUser(), item.getItemId(), item.getItemNum(), ItemChangeReason.FISHING_RESULT, true);
                    }
                }

                // 爆炸炸死的鱼不向玩家发送消息
                if (!boom) {
                    FishingFightFishResponse.Builder builder = FishingFightFishResponse.newBuilder();
                    builder.setFishId(fishId);
                    builder.setPlayerId(player.getId());
                    builder.setRestMoney(player.getMoney());
                    builder.setDropMoney(winMoney);
                    builder.addAllDropItems(dropItems);
                    sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_FIGHT_FISH_RESPONSE_VALUE, builder.build());

                    if (dropItems.size() > 0) { // 掉落了鱼雷字幕播报
                        // 掉落的鱼雷名称信息
                        StringBuilder torpedoInfo = new StringBuilder();
                        for (ItemDataProto item : dropItems) {
                            if (item.getItemId() == ItemId.BRONZE_TORPEDO.getId()) {
                                torpedoInfo.append(ItemId.BRONZE_TORPEDO.getInfo()).append(",");
                            } else if (item.getItemId() == ItemId.SILVER_TORPEDO.getId()) {
                                torpedoInfo.append(ItemId.SILVER_TORPEDO.getInfo()).append(",");
                            } else if (item.getItemId() == ItemId.GOLD_TORPEDO.getId()) {
                                torpedoInfo.append("龙珠").append(",");
                            }
                        }
                        if (torpedoInfo.length() > 0) { // 掉落的有鱼雷
                            String text = String.format(
                                    AutoWanderSubtitle.TEMPLATES[ThreadLocalRandom.current().nextInt(2, 4)],
                                    player.getUser().getNickname(), randomMoney, config.getName(),
                                    torpedoInfo.toString().substring(0, torpedoInfo.length() - 1)
                            );
                            // 给全部在线玩家推送游走字幕消息
                            PlayerManager.sendMessageToOnline(LobbyMsgCode.S_C_WANDER_SUBTITLE_RESPONSE_VALUE,
                                    WanderSubtitleResponse.newBuilder().setLevel(1).setContent(text).build());
                        }
                    } else if (randomMoney >= 60 && randomMoney <= 80 && winMoney > 1000000) { // 没有掉落鱼雷游走播报条件：60-80倍(虚拟的随机金币区间100w-400w)
                        String text = String.format(
                                AutoWanderSubtitle.TEMPLATES[ThreadLocalRandom.current().nextInt(2)],
                                player.getUser().getNickname(), randomMoney, config.getName(), winMoney / 10000
                        );
                        // 给全部在线玩家推送游走字幕消息
                        PlayerManager.sendMessageToOnline(LobbyMsgCode.S_C_WANDER_SUBTITLE_RESPONSE_VALUE,
                                WanderSubtitleResponse.newBuilder().setLevel(1).setContent(text).build());
                    }
                }
            }
        }
        return configs;
    }

    /**
     * 判断鱼是否被击中
     */
    private static boolean isHit(FishingGamePlayer gamePlayer, int roomIndex, FishStruct fish, FishConfig config) {
        if (fish.getFireTimes() < fish.getSafeTimes()) { // 安全次数判断
            // 拥有最高等级炮台为中级场最高炮台倍数以上检查安全次数 最高倍数及以下的不检查安全次数
            if (!(gamePlayer instanceof FishingGameRobot)) {
                if (PlayerManager.getPlayerBatteryLevel(gamePlayer.getUser()) > batteryLevelLimit[1][1]) {
                    return false;
                }
            } else {
                return false;
            }
        }

        double baseHit = (double) config.getAttack() / config.getHealth(); // 基础命中率 = 攻击值 / 生命值

        int index = roomIndex - 1;
        int greener = FishingHitDataManager.getGreener(gamePlayer);

        // 基础命中系数(1)加上服务器当前命中系数
        double baseCoefficient = 100D + FishingHitDataManager.getServerProb(greener, index);

        if(greener == 1) {
            baseCoefficient -= ((FishingHitDataManager.getTotalWin(gamePlayer.getId(), index) - FishingUtil.q0[index * 2]) * 1.0 / FishingUtil.apt[index * 2]) * 0.01;
        } else {
            baseCoefficient -= ((FishingHitDataManager.getTotalWin(gamePlayer.getId(), index) - FishingUtil.q0[index * 2 + 1]) * 1.0 / FishingUtil.apt[index * 2 + 1]) * 0.01;
        }

        // 当日输赢金币限制
        long dailyWin = FishingHitDataManager.getDailyWin(gamePlayer.getId(), index);
        if (dailyWin < -FishingHitDataManager
                .PLAYER_DAILY_LIMIT[greener][index][0]) {
            baseCoefficient += FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][0]; // 幸运玩家系数
        } else if (dailyWin > FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][1]) {
            baseCoefficient -= FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][1]; // 挽救玩家系数
        }

        // 小黑屋限制
        if (FishingHitDataManager.getBlackRoom(gamePlayer.getId(), index) > 0) {
            baseCoefficient -= FishingHitDataManager.BLACK_ROOM_PROB[greener][index];
        }

        // 总计输赢金币限制
        long totalWin = FishingHitDataManager.getTotalWin(gamePlayer.getId(), index);
        if (totalWin < -FishingHitDataManager.PLAYER_TOTAL_LIMIT[greener][index][0]) {
            baseCoefficient += FishingHitDataManager.PLAYER_TOTAL_PROB[greener][index][0];
        } else if (totalWin > FishingHitDataManager.PLAYER_TOTAL_LIMIT[greener][index][1]) {
            baseCoefficient -= FishingHitDataManager.PLAYER_TOTAL_PROB[greener][index][1];
        }

        baseCoefficient += FishingHitDataManager.getPlayerFishingProb(gamePlayer.getId());

        // 玩家暴击状态下命中率降低
        if ((System.currentTimeMillis() - gamePlayer.getLastCritTime()) / 1000 < SKILL_CRIT_TIME / 1000) {
            baseCoefficient *= 0.75;
        }

        // 机器人降低命中率
        if (gamePlayer instanceof FishingGameRobot) {
            baseCoefficient *= 0.5;
        }

        // 判断是否命中
        return !(baseHit * baseCoefficient / 100D < ThreadLocalRandom.current().nextDouble());
    }

    /**
     * 创建玩家信息结构
     */
    private static FishingPlayerInfoProto createPlayerInfoProto(FishingGamePlayer gamePlayer) {
        FishingPlayerInfoProto.Builder builder = FishingPlayerInfoProto.newBuilder();
        builder.setPlayerId(gamePlayer.getId());
        builder.setName(gamePlayer.getUser().getNickname());
        builder.setHeadIndex(gamePlayer.getUser().getEntity().getHeadIndex());
        builder.setHeadUrl(gamePlayer.getUser().getEntity().getHeadUrl());
        builder.setSex(gamePlayer.getUser().getEntity().getSex());
        builder.setMoney(gamePlayer.getMoney());
        builder.setSeat(gamePlayer.getSeat());
        builder.setOnline(gamePlayer.getUser().isOnline());
        builder.setVipLevel(gamePlayer.getVipLevel());
        builder.setViewIndex(gamePlayer.getViewIndex());
        builder.setBatteryLevel(gamePlayer.getBatteryLevel());
        builder.setBatteryMult(gamePlayer.getBatteryMult());
        builder.setLevel(gamePlayer.getLevel());
        return builder.build();
    }

    /**
     * 创建玩家信息
     */
    public static FishingPlayerInfoResponse createPlayerInfoResponse(FishingGameRoom gameRoom, FishingGamePlayer
            player) {
        FishingPlayerInfoResponse.Builder builder = FishingPlayerInfoResponse.newBuilder();
        builder.setPlayerInfo(createPlayerInfoProto(player));
        return builder.build();
    }

    /**
     * 创建鱼数据结构
     */
    private static FishingFishInfoProto createFishInfoProto(FishStruct struct) {
        FishingFishInfoProto.Builder builder = FishingFishInfoProto.newBuilder();
        builder.setId(struct.getId());
        builder.setFishId(struct.getConfigId());
        builder.setRouteId(struct.getRouteId());
        builder.setClientLifeTime(struct.getClientLifeTime());
        builder.setCreateTime(struct.getCreateTime());
        return builder.build();
    }

    /**
     * 发送玩家加入房间消息
     */
    private void sendJoinRoomResposne(FishingGameRoom gameRoom, ServerUser user) {
        FishingJoinRoomResponse.Builder builder = FishingJoinRoomResponse.newBuilder();
        builder.setRoomIndex(gameRoom.getRoomIndex());
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_FISHING_JOIN_ROOM_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送玩家列表消息
     */
    public void sendPlayersInfoResponse(FishingGameRoom gameRoom, ServerUser user) {
        FishingPlayersInfoResponse.Builder builder = FishingPlayersInfoResponse.newBuilder();
        for (BaseGamePlayer gamePlayer : gameRoom.getGamePlayers()) {
            if (gamePlayer != null) {
                builder.addPlayerInfos(createPlayerInfoProto((FishingGamePlayer) gamePlayer));
            }
        }
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_FISHING_PLAYERS_INFO_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送同步鱼消息
     */
    public void sendSynchroniseResponse(FishingGameRoom gameRoom, ServerUser user) {
        FishingSynchroniseResponse.Builder builder = FishingSynchroniseResponse.newBuilder();
        for (FishStruct fish : gameRoom.getFishMap().values()) {
            builder.addFishInfos(createFishInfoProto(fish));
        }
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_FISHING_SYNCHRONISE_RESPONSE_VALUE, builder, user);
    }

    /**
     * 向玩家发送房间当前的冰冻消息
     */
    public void sendFrozenMessage(FishingGameRoom gameRoom, ServerUser user) {
        long nowTime = System.currentTimeMillis();
        if (nowTime - gameRoom.getLastRoomFrozenTime() < SKILL_FROZEN_TIME) { // 房间处于冰冻状态
            FishingUseSkillResponse.Builder builder = FishingUseSkillResponse.newBuilder();
            builder.setSkillId(ItemId.SKILL_FROZEN.getId()); // 冰冻
            builder.setDuration((int) ((SKILL_FROZEN_TIME - (nowTime - gameRoom.getLastRoomFrozenTime())) / 1000));
            NetManager.sendMessage(OseeMsgCode.S_C_OSEE_FISHING_USE_SKILL_RESPONSE_VALUE, builder, user);
        }
    }

    /**
     * 发送重新激活消息
     */
    public void sendReactiveMessage(FishingGameRoom gameRoom, ServerUser user) {
        sendJoinRoomResposne(gameRoom, user); // 发送加入房间消息
        sendPlayersInfoResponse(gameRoom, user); // 发送玩家数据列表消息
        sendSynchroniseResponse(gameRoom, user); // 发送同步鱼消息
        sendFrozenMessage(gameRoom, user); // 发送房间当前冰冻消息
    }

    /**
     * 发送房间消息
     */
    public static void sendRoomMessage(BaseFishingRoom gameRoom, int msgCode, Message msg) {
        for (BaseGamePlayer gamePlayer : gameRoom.getGamePlayers()) {
            if (gamePlayer != null && gamePlayer.getUser().isOnline()) {
                NetManager.sendMessage(msgCode, msg, gamePlayer.getUser());
            }
        }
    }

    /**
     * 发送玩家捕获boss鱼响应到全服
     */
    public static void sendCatchBossFishResponse(CatchBossFishResponse response) {
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> {
            // 只发送到全服在捕鱼房间内的玩家
            List<FishingGameRoom> fishingGameRooms = GameContainer.getGameRooms(FishingGameRoom.class);
            for (FishingGameRoom gameRoom : fishingGameRooms) {
                if (gameRoom != null) {
                    sendRoomMessage(gameRoom, OseeMsgCode.S_C_TTMY_CATCH_BOSS_FISH_RESPONSE_VALUE, response);
                }
            }
        }, 0, TimeUnit.SECONDS);
    }

    /**
     * 刷新一组鱼
     */
    private void refreshFish0(FishingGameRoom gameRoom, List<Long> fishIds, long routeId, long ruleId) {
        long nowTime = System.currentTimeMillis();
        if (nowTime - gameRoom.getLastRoomFrozenTime() < SKILL_FROZEN_TIME) {
            return;
        }

        FishRefreshRule rule = DataContainer.getData(ruleId, FishRefreshRule.class);
        if (!rule.isFishTide() && gameRoom.isFishTide()) { // 如果不是鱼潮中的鱼，且房间正处于鱼潮中，中止刷此鱼
            return;
        }

        FishingRefreshFishesResponse.Builder builder = FishingRefreshFishesResponse.newBuilder();
        FishRouteConfig route = DataContainer.getData(routeId, FishRouteConfig.class);
        for (Long fishId : fishIds) {
            FishConfig fish = DataContainer.getData(fishId, FishConfig.class);

//            boolean cont = false;
//            for (int scene : fish.getRealScene()) {
//                if (scene == gameRoom.getRoomIndex()) {
//                    cont = true;
//                }
//            }
//            if (!cont) {
//                continue;
//            }

            if (fish.getFishType() == 100 && gameRoom.getBoss() != 0) { // 房间内暂时还有boss，就中止刷新该boss
                continue;
            }

            int maxSafe = fish.getMaxSafe() == 0 ? fish.getMinSafe() + 1 : fish.getMaxSafe();
            int safeTimes = ThreadLocalRandom.current().nextInt(fish.getMinSafe(), maxSafe);

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
                if (System.currentTimeMillis() - gameRoom.getLastBossBugleTime() < BOSS_BUGLE_COOL_TIME) {
                    gameRoom.setBoss(2); // 设置房间有召唤出来的boss
                } else {
                    gameRoom.setBoss(1); // 设置为普通自动刷新的boss
                }
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
        sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_REFRESH_FISHES_RESPONSE_VALUE, builder.build());
        gameRoom.setNoFishTick(0);
    }

    /**
     * 定时刷新一组鱼
     */
    private void refreshFish(FishingGameRoom gameRoom, List<Long> fishIds, long routeId, long ruleId,
                             double delay) {
        ScheduledExecutorService service = ThreadPoolUtils.TASK_SERVICE_POOL;
        service.schedule(() -> refreshFish0(gameRoom, fishIds, routeId, ruleId), (int) (delay * 1000), TimeUnit.MILLISECONDS);
    }

    /**
     * 随机刷新一群鱼
     */
    private void refreshGroupFish(FishingGameRoom gameRoom, long ruleId) {
        FishRefreshRule rule = DataContainer.getData(ruleId, FishRefreshRule.class);

//        for (int i = 0; i < 10; i++) {
        FishGroupConfig group = DataContainer.getRandomData(FishGroupConfig.class, rule.getStart(), rule.getEnd());
//            if (Arrays.asList(group.getRealScene()).contains(gameRoom.getRoomIndex())) {
        refreshGroupFish(gameRoom, ruleId, group);
//                break;
//            }
//        }
    }

    /**
     * 刷新一群鱼
     */
    private void refreshGroupFish(FishingGameRoom gameRoom, long ruleId, FishGroupConfig groupConfig) {
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
                        refreshFish(gameRoom, fishIds, routeId, ruleId, delay);
                        delay += realDelay;
                        fishIds = new LinkedList<>();
                    }

                    fishIds.add(groupConfig.getRealGroup()[i]);
                }
            }

            refreshFish(gameRoom, fishIds, routeId, ruleId, delay);
        }
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
     * 从房间创建开始就定时执行刷鱼逻辑
     */
    @Override
    protected void doFishingRoomTask0(BaseFishingRoom fishingRoom) {
        if (!(fishingRoom instanceof FishingGameRoom)) {
            return;
        }

        long nowTime = System.currentTimeMillis();
        FishingGameRoom gameRoom = (FishingGameRoom) fishingRoom;

        // 初始化房间刷新规则
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
                refreshTime.put(refreshRule, getNextRefreshTime(refreshRule, 0));
            }
            gameRoom.setNextRefreshTime(refreshTime);
        }

        if (gameRoom.getBoss() == 0 // 房间无boss
                && !gameRoom.isFishTide()
                && gameRoom.getMinFishTideDelay() > 0
                && gameRoom.getRoomTick() >= gameRoom.getNextFishTideTime()) { // 进入鱼潮
            if (gameRoom.getMinFishTideDelay() > 0) {
                // 设置下次刷新鱼潮的时长
                gameRoom.setNextFishTideTime(gameRoom.getRoomTick() +
                        ThreadLocalRandom.current().nextLong(gameRoom.getMinFishTideDelay(), gameRoom.getMaxFishTideDelay() + 1)
                );
            }

            gameRoom.setFishTide(true);

            // 发送鱼潮消息
            FishingFishTideResponse.Builder builder = FishingFishTideResponse.newBuilder();
            sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_FISH_TIDE_RESPONSE_VALUE, builder.build());
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
            if (gameRoom.getNoFishTick() > 2 && nowTime - gameRoom.getLastRoomFrozenTime() > SKILL_FROZEN_TIME) {
                gameRoom.setFishTide(false);
            } else if (gameRoom.getFishMap().size() == 0) {
                gameRoom.setNoFishTick(gameRoom.getNoFishTick() + 1);
            }
        } else { // 正常刷鱼
            // 房间冰冻期间不刷新鱼
            if (nowTime - gameRoom.getLastRoomFrozenTime() > SKILL_FROZEN_TIME) {
                // 刷新鱼
                for (Entry<FishRefreshRule, Long> refreshEntry : gameRoom.getNextRefreshTime().entrySet()) {
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
            FishingGamePlayer player = gameRoom.getGamePlayerBySeat(i);
            // 检查玩家是否长时间未操作
            if (player != null && nowTime - player.getLastFireTime() > ROOM_KICK_TIME) {
                NetManager.sendHintMessageToClient("您长时间未操作，已被移出捕鱼房间", player.getUser());
                exitFishingRoom(gameRoom, player.getUser());
            }
        }

        // 判断过期鱼，并从鱼表内移除
        List<Long> removeKey = new LinkedList<>();
        for (FishStruct fish : gameRoom.getFishMap().values()) {
            long maxLifeTime = Math.round(fish.getLifeTime() > 0 ? fish.getLifeTime() : DEFAULT_LIFE_TIME);
//             maxLifeTime += DELAY_LIFE_TIME;
            if (maxLifeTime * 1000 + fish.getCreateTime() < nowTime) {
                removeKey.add(fish.getId());
                if (fish.getFishType() == 100) { // boss鱼消失过期
                    if (gameRoom.getBoss() == 2) {
                        gameRoom.setLastBossBugleTime(0L); // boss号角召唤的boss就重置召唤冷却时间
                    }
                    gameRoom.setBoss(0); // boss消失了就置为房间无boss状态
                }
            }
        }
        for (long key : removeKey) {
            gameRoom.getFishMap().remove(key);
        }
    }

    /**
     * 玩家使用BOSS号角
     */
    public void useBossBugle(FishingGameRoom gameRoom, FishingGamePlayer player) {
        ServerUser user = player.getUser();
        if (PlayerManager.getPlayerVipLevel(user) < 3) {
            NetManager.sendHintMessageToClient("VIP3及以上才可以使用BOSS号角", user);
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - gameRoom.getLastRoomFrozenTime() < SKILL_FROZEN_TIME || gameRoom.isFishTide()) {
            // 房间冰冻中或者鱼潮中不能召唤boss
            NetManager.sendHintMessageToClient("房间冰冻中或者鱼潮中不能召唤boss", user);
            return;
        }
        if (currentTimeMillis - gameRoom.getLastBossBugleTime() < BOSS_BUGLE_COOL_TIME) {
            NetManager.sendHintMessageToClient("房间BOSS号角使用冷却中(5分钟)", user);
            return;
        }
        if (gameRoom.getBoss() != 0) {
            NetManager.sendHintMessageToClient("房间内还有BOSS存在，请稍后再使用", user);
            return;
        }
        if (!PlayerManager.checkItem(user, ItemId.BOSS_BUGLE, 1)) {
            NetManager.sendHintMessageToClient("BOSS号角数量不足", user);
            return;
        }
        gameRoom.setLastBossBugleTime(currentTimeMillis);
        // 扣除使用的号角数量
        PlayerManager.addItem(user, ItemId.BOSS_BUGLE, -1, ItemChangeReason.USE_ITEM, true);
        long[] ruleIds = {18, 36, 55, 74}; // 各个boss的刷新规则
        long ruleId = ruleIds[gameRoom.getRoomIndex() - 1]; // 获取当前场次的boss刷新规则
        // 按照boss规则刷新
        refreshGroupFish(gameRoom, ruleId);
        UseBossBugleResponse.Builder builder = UseBossBugleResponse.newBuilder();
        builder.setPlayerId(player.getId());
        sendRoomMessage(gameRoom, OseeMsgCode.S_C_TTMY_USE_BOSS_BUGLE_RESPONSE_VALUE, builder.build());
    }




    /**
     * 玩家在房间使用龙晶
     */
    public void useDragonCrystal(BaseFishingRoom gameRoom, FishingGamePlayer player, int torpedoId, int torpedoNum,
                                 float angle) {

        ServerUser user = player.getUser();
        if (!PlayerManager.checkItem(user, torpedoId, torpedoNum)) {
            return;
        }

        // 游戏中使用鱼雷可直接获得金币
        long money = 0;

        if (torpedoId == ItemId.DRAGON_CRYSTAL.getId()) {
            money = 2 * torpedoNum;

        }
        // 保存鱼雷使用数量记录
//        RedisHelper.set("Fishing:TorpedoDropNum", JSON.toJSONString(TORPEDO_RECORD));
        List<ItemData> itemDataList = new LinkedList<>();
        // 扣除玩家鱼雷
        itemDataList.add(new ItemData(torpedoId, -torpedoNum));
        // 增加玩家金币
        itemDataList.add(new ItemData(ItemId.MONEY.getId(), money));
        PlayerManager.addItems(user, itemDataList, ItemChangeReason.USE_ITEM, true);
        player.setTorpedoMoney(player.getTorpedoMoney() + money);

        OseeFishingMessage.FishingUseTorpedoResponse.Builder builder = OseeFishingMessage.FishingUseTorpedoResponse.newBuilder();
        builder.setPlayerId(player.getId());
        builder.setTorpedoId(torpedoId);
        builder.setTorpedoNum(torpedoNum);
        builder.setAngle(angle);
        builder.setMoney(money);
        sendRoomMessage(gameRoom, OseeMsgCode.S_C_TTMY_FISHING_USE_TORPEDO_RESPONSE_VALUE, builder.build());
    }

    /**
     * 房间内玩家使用技能
     */
    public void useSkill(FishingGameRoom gameRoom, FishingGamePlayer player, int skillId) {
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

        FishingUseSkillResponse.Builder builder = FishingUseSkillResponse.newBuilder();
        builder.setSkillId(skillId);
        builder.setPlayerId(player.getId());

        long nowTime = System.currentTimeMillis();
        if (skillId == ItemId.SKILL_LOCK.getId()) { // 锁定
            if (nowTime - player.getLastLockTime() < SKILL_LOCK_TIME) {
                NetManager.sendHintMessageToClient("技能冷却中", user);
                return;
            }
            builder.setDuration((int) (SKILL_LOCK_TIME / 1000));
            player.setLastLockTime(nowTime);
        } else if (skillId == ItemId.SKILL_FROZEN.getId()) { // 冰冻
            if (nowTime - player.getLastFrozenTime() < SKILL_FROZEN_TIME) {
                NetManager.sendHintMessageToClient("技能冷却中", user);
                return;
            }
            builder.setDuration((int) (SKILL_FROZEN_TIME / 1000));
            player.setLastFrozenTime(nowTime);
            gameRoom.setLastRoomFrozenTime(nowTime);

            for (Entry<FishRefreshRule, Long> refresh : gameRoom.getNextRefreshTime().entrySet()) {
                refresh.setValue(refresh.getValue() + SKILL_FROZEN_TIME / 1000); // 延迟冰冻持续时间段刷鱼
            }

            long addTime = nowTime / 1000 - gameRoom.getLastRoomFrozenTime() / 1000;
            long skillTime = SKILL_FROZEN_TIME / 1000;
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
            if (nowTime - player.getLastFastTime() < SKILL_FAST_TIME) {
                NetManager.sendHintMessageToClient("技能冷却中", user);
                return;
            }
            builder.setDuration((int) (SKILL_FAST_TIME / 1000));
            player.setLastFastTime(nowTime);
        } else if (skillId == ItemId.SKILL_CRIT.getId()) { // 暴击
            int vipLevel = PlayerManager.getPlayerVipLevel(user);
            if (vipLevel < 4) {
                NetManager.sendHintMessageToClient("VIP4及以上才可以使用暴击技能", user);
                return;
            }
            if (nowTime - player.getLastCritTime() < SKILL_CRIT_TIME) {
                NetManager.sendHintMessageToClient("技能冷却中", user);
                return;
            }
            builder.setDuration((int) (SKILL_CRIT_TIME / 1000));
            player.setLastCritTime(nowTime);
        } else if (skillId == ItemId.FEN_SHEN.getId()) { // 分身炮道具
            int vipLevel = PlayerManager.getPlayerVipLevel(user);
            if (vipLevel < 3) {
                NetManager.sendHintMessageToClient("VIP3及以上才可以使用分身炮", user);
                return;
            }
            if (nowTime - player.getLastFenShenTime() < SKILL_FEN_SHEN_TIME) {
                NetManager.sendHintMessageToClient("技能冷却中", user);
                return;
            }
            builder.setDuration((int) (SKILL_FEN_SHEN_TIME / 1000));
            player.setLastFenShenTime(nowTime);
        }

        builder.setRestMoney(player.getMoney());
        sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_USE_SKILL_RESPONSE_VALUE, builder.build());

        // 扣除使用的技能数量
        PlayerManager.addItem(user, skillId, -1, ItemChangeReason.USE_ITEM, true);
        // 做任务 使用任意道具
        FishingTaskManager.doTask(user, TaskType.DAILY, GoalType.USE_ITEM, 0, 1);
        // 房间任务
        FishingTaskManager.doTask(user, TaskType.ROOM, GoalType.USE_ITEM, skillId, 1);
    }

    /**
     * 解锁炮台等级提示消息
     */
    public void unlockBatteryLevelHint(ServerUser user, int targetLevel) {
        int batteryLevel = PlayerManager.getPlayerBatteryLevel(user);

        List<BatteryLevelConfig> configs = DataContainer.getDatas(BatteryLevelConfig.class);
        // 炮台等级从小到大排列
        configs.sort(Comparator.comparingInt(BatteryLevelConfig::getBatteryLevel));
        if (batteryLevel >= configs.get(configs.size() - 1).getBatteryLevel()) { // 已拥有最高等级的炮台
            return;
        }
        long cost = 0; // 需要消耗的钻石数量
        long gold = 0; // 解锁总共奖励的金币
        for (BatteryLevelConfig config : configs) {
            if (config != null) {
                if (targetLevel > 0) { // 需要直接升到对应的等级
                    if (config.getBatteryLevel() > batteryLevel) {
                        cost += config.getCost(); // 累计的钻石消耗
                        gold += config.getGold(); // 累计的金币奖励
                    }
                    if (config.getBatteryLevel() == targetLevel) {
                        break;
                    }
                } else if (config.getBatteryLevel() > batteryLevel) { // 找寻下一等级的炮台
                    targetLevel = config.getBatteryLevel();
                    cost = config.getCost();
                    gold = config.getGold();
                    break;
                }
            }
        }
        UnlockBatteryLevelHintResponse.Builder builder = UnlockBatteryLevelHintResponse.newBuilder();
        builder.setNextLevel(targetLevel);
        builder.setCost(cost);
        builder.setRewardGold(ItemDataProto.newBuilder().setItemId(ItemId.MONEY.getId()).setItemNum(gold).build());
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_UNLOCK_BATTERY_LEVEL_HINT_RESPONSE_VALUE, builder, user);
    }

    /**
     * 解锁炮台等级
     */
    public void unlockBatteryLevel(ServerUser user, int level) {
        OseePlayerEntity playerEntity = PlayerManager.getPlayerEntity(user);
        int batteryLevel = playerEntity.getBatteryLevel();
        if (batteryLevel >= level) {
            NetManager.sendHintMessageToClient("你已拥有更高等级的炮台！", user);
            return;
        }
        List<BatteryLevelConfig> configs = DataContainer.getDatas(BatteryLevelConfig.class);
        // 炮台等级从小到大排列
        configs.sort(Comparator.comparingInt(BatteryLevelConfig::getBatteryLevel));
        long cost = 0; // 需要消耗的钻石数量
        long gold = 0; // 解锁总共奖励的金币
        for (BatteryLevelConfig config : configs) {
            if (config != null) {
                int configBatteryLevel = config.getBatteryLevel();
                if (configBatteryLevel > batteryLevel) {
                    cost += config.getCost(); // 累计的钻石消耗
                    gold += config.getGold(); // 累计的金币奖励
                }
                if (configBatteryLevel == level) {
                    if (PlayerManager.checkItem(user, ItemId.DIAMOND, cost) && cost > 0) {
                        // 更新玩家炮台等级
                        playerEntity.setBatteryLevel(level); // 这里没有调用update方法是因为下面的addItem里面有调用就不重复调用了
                        // 扣除玩家解锁花费的钻石数量
                        PlayerManager.addItem(user, ItemId.DIAMOND, -cost, ItemChangeReason.UNLOCK_BATTERY, true);
                        // 奖励玩家升级的金币
                        PlayerManager.addItem(user, ItemId.MONEY, gold, ItemChangeReason.UNLOCK_BATTERY, true);

                        // 返回响应
                        UnlockBatteryLevelResponse.Builder builder = UnlockBatteryLevelResponse.newBuilder();
                        builder.setLevel(level);
                        builder.setRewardGold(ItemDataProto.newBuilder().setItemId(ItemId.MONEY.getId()).setItemNum(gold).build());
                        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_UNLOCK_BATTERY_LEVEL_RESPONSE_VALUE, builder, user);
                    } else {
                        NetManager.sendErrorMessageToClient("解锁失败：钻石不足！", user);
                    }
                    return;
                }
            }
        }
        NetManager.sendErrorMessageToClient("升级的炮台等级有误", user);
    }

    /**
     * 捕获特殊鱼
     */
    public void catchSpecialFish(FishingGameRoom gameRoom, long playerId, List<Long> fishIdsList, long specialFishId) {
        FishingGamePlayer player = gameRoom.getGamePlayerById(playerId);
        if (player == null) {
            return;
        }
        long beforeMoney = player.getMoney();
        List<FishConfig> configs = playerFightFish(gameRoom, player, -1, fishIdsList, true);
        if (configs != null) {
            CatchSpecialFishResponse.Builder builder = CatchSpecialFishResponse.newBuilder();
            builder.setSpecialFishId(specialFishId);
            builder.addAllFishIds(fishIdsList);
            builder.setPlayerId(player.getId());
            builder.setDropMoney(player.getMoney() - beforeMoney);
            builder.setRestMoney(player.getMoney());
            sendRoomMessage(gameRoom, OseeMsgCode.S_C_TTMY_CATCH_SPECIAL_FISH_RESPONSE_VALUE, builder.build());
        }
    }

    /**
     * 快速开始
     */
    public void quickStart(ServerUser user) {
        int playerBatteryLevel = PlayerManager.getPlayerBatteryLevel(user);
        int roomIndex = -1;
        for (int i = batteryLevelLimit.length; i > 0; i--) {
            roomIndex = i;
            // 场次最低炮台等级限制
            int minBatteryLevel = batteryLevelLimit[roomIndex - 1][0];
            if (playerBatteryLevel >= minBatteryLevel) {
                for (int j = roomIndex; j > 0; j--) { // 检测金币是否满足进入房间的限制
                    if (PlayerManager.checkItem(user, ItemId.MONEY, enterLimit[j - 1])) {
                        roomIndex = j;
                        break;
                    }
                }
                break;
            }
        }
        // 加入合适的房间
        playerJoinRoom(user, roomIndex);
    }

    /**
     * 发送场次信息
     */
    public void getFieldInfo(ServerUser user) {
        FishingGetFieldInfoResponse.Builder builder = FishingGetFieldInfoResponse.newBuilder();
        for (int i = 0; i < enterLimit.length; i++) {
            builder.addFieldInfos(FishingFieldInfoProto.newBuilder()
                    .setIndex(i + 1)
                    .setEnterLimit(enterLimit[i])
                    .setBatteryLevelLimit(batteryLevelLimit[i][0])
                    .build());
        }
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_FISHING_GET_FIELD_INFO_RESPONSE_VALUE, builder, user);
    }

    /**
     * 设置鱼雷掉落概率数据
     */
    public static void setTorpedoDropRate(double torpedoDropFreeRate, long torpedoDropPerPayMoney, double torpedoDropPerPayRate) {
        FishingManager.TORPEDO_DROP_FREE_RATE = torpedoDropFreeRate;
        FishingManager.TORPEDO_DROP_PER_PAY_MONEY = torpedoDropPerPayMoney;
        FishingManager.TORPEDO_DROP_PER_PAY_RATE = torpedoDropPerPayRate;
        RedisHelper.set("Fishing:TorpedoDropRate:FreeRate", String.valueOf(FishingManager.TORPEDO_DROP_FREE_RATE));
        RedisHelper.set("Fishing:TorpedoDropRate:PerPayMoney", String.valueOf(FishingManager.TORPEDO_DROP_PER_PAY_MONEY));
        RedisHelper.set("Fishing:TorpedoDropRate:PerPayRate", String.valueOf(FishingManager.TORPEDO_DROP_PER_PAY_RATE));
    }

    public static void setChallengeDropRate(double challengeDropFreeRate, long challengeDropPerPayMoney, double challengeDropPerPayRate) {
        FishingManager.CHALLENGE_DROP_FREE_RATE = challengeDropFreeRate;
        FishingManager.CHALLENGE_DROP_PER_PAY_MONEY = challengeDropPerPayMoney;
        FishingManager.CHALLENGE_DROP_PER_PAY_RATE = challengeDropPerPayRate;
        RedisHelper.set("Fishing:ChallengeDropRate:FreeRate", String.valueOf(FishingManager.CHALLENGE_DROP_FREE_RATE));
        RedisHelper.set("Fishing:ChallengeDropRate:PerPayMoney", String.valueOf(FishingManager.CHALLENGE_DROP_PER_PAY_MONEY));
        RedisHelper.set("Fishing:ChallengeDropRate:PerPayRate", String.valueOf(FishingManager.CHALLENGE_DROP_PER_PAY_RATE));
    }

    /**
     * 获取鱼雷掉落概率数据
     */
    public static void getTorpedoDropRate() {
        if (FishingManager.TORPEDO_DROP_FREE_RATE == -1 ||
                FishingManager.TORPEDO_DROP_PER_PAY_RATE == -1 || FishingManager.TORPEDO_DROP_PER_PAY_MONEY == -1) {
            String value_1 = RedisHelper.get("Fishing:TorpedoDropRate:FreeRate");
            if (StringUtils.isEmpty(value_1)) {
                // 默认概率5%
                FishingManager.TORPEDO_DROP_FREE_RATE = 5;
            } else {
                FishingManager.TORPEDO_DROP_FREE_RATE = Double.parseDouble(value_1);
            }
            String value_2 = RedisHelper.get("Fishing:TorpedoDropRate:PerPayMoney");
            if (StringUtils.isEmpty(value_2)) {
                // 默认值10000
                FishingManager.TORPEDO_DROP_PER_PAY_MONEY = 10000;
            } else {
                FishingManager.TORPEDO_DROP_PER_PAY_MONEY = Long.parseLong(value_2);
            }
            String value_3 = RedisHelper.get("Fishing:TorpedoDropRate:PerPayRate");
            if (StringUtils.isEmpty(value_3)) {
                // 默认值1.2
                FishingManager.TORPEDO_DROP_PER_PAY_RATE = 1.2;
            } else {
                FishingManager.TORPEDO_DROP_PER_PAY_RATE = Double.parseDouble(value_3);
            }
        }
    }
}
