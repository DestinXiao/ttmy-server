package com.maple.game.osee.manager.fishing;

import com.google.protobuf.GeneratedMessage;
import com.maple.common.lobby.proto.LobbyMessage;
import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.entity.UserEntity;
import com.maple.database.data.mapper.UserMapper;
import com.maple.engine.container.DataContainer;
import com.maple.engine.data.ServerUser;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.common.RedisUtil;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.entity.GameEnum;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.csv.file.FishConfig;
import com.maple.game.osee.entity.fishing.csv.file.FishGroupConfig;
import com.maple.game.osee.entity.fishing.csv.file.FishRefreshRule;
import com.maple.game.osee.entity.fishing.csv.file.FishRouteConfig;
import com.maple.game.osee.entity.fishing.game.FireStruct;
import com.maple.game.osee.entity.fishing.game.FishStruct;
import com.maple.game.osee.entity.fishing.grandprix.FishingGrandPrixPlayer;
import com.maple.game.osee.entity.fishing.grandprix.FishingGrandPrixRoom;
import com.maple.game.osee.manager.AgentManager;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.manager.fishing.util.FishingUtil;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.OseePublicData;
import com.maple.game.osee.timer.AutoWanderSubtitle;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.maple.game.osee.proto.TtmyFishingGrandPrixMessage.*;

@Component
public class FishingGrandPrixManager {

    private Logger logger = LoggerFactory.getLogger(FishingGrandPrixManager.class);

    @Autowired
    UserMapper userMapper;

    @Autowired
    FishingUtil fishingUtil;

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

//    private Logger logger = LoggerFactory.getLogger(this.getClass());


    @Autowired
    FishingRobotManager robotManager;

    /**
     * 玩家大奖赛剩余子弹数量 + playerId
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_BULLET_KEY = "player:grandprix:config:bullet:";

    /**
     * 周排行榜
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_RANK_WEEK_KEY = "player:grandprix:config:rank:week";

    /**
     * 日排行榜
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_RANK_DAY_KEY = "player:grandprix:config:rank:day";

    /**
     * 玩家大奖赛今日积分 + playerId
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_POINT_DAY_KEY = "player:grandprix:config:point:day:";

    /**
     * 玩家大奖赛周积分 + playerId
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_POINT_WEEK_KEY = "player:grandprix:config:point:week:";

    /**
     * 玩家大奖赛今日游戏局数 + playerId
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_GAMES_KEY = "player:grandprix:config:games:";

    /**
     * 玩家大奖赛入场金币数
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_ENTRY_GOLD_KEY = "player:grandprix:config:entry:gold";

    /**
     * 玩家大奖赛入场钻石数
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_ENTRY_DIAMOND_KEY = "player:grandprix:config:entry:diamond";

    /**
     * 目前所有的boss数量
     */
    private static long bossNum;

    /**
     * 大奖赛总库存
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_STOCK_KEY = "player:grandprix:config:stock";

    /**
     * 玩家总输赢
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_POINT_TOTAL_KEY = "player:grandprix:config:point:total:";

    /**
     * 玩家今日输赢
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_POINT_DAY_TOTAL_KEY = "player:grandprix:config:point:day:total:";

    /**
     * 玩家本周输赢
     */
    public static final String PLAYER_GRANDPRIX_CONFIG_POINT_WEEK_TOTAL_KEY = "player:grandprix:config:point:week:total:";


    //-------------------- 以下为玩家参数控制变量 -----------------
    public static final String PLAYER_GRANDPRIX_CONFIG_BLACK_ROOM_KEY = "player:grandprix:config:blackroom";

    // 实时AP值
    public static final String PLAYER_GRANDPRIX_CONFIG_AP_KEY = "player:grandprix:config:ap";

    // AP变化0.01库存变化量
    public static final String PLAYER_GRANDPRIX_CONFIG_APT_KEY = "player:grandprix:config:apt";

    // 小黑屋参数
    public static final String PLAYER_GRANDPRIX_CONFIG_BP_KEY = "player:grandprix:config:bp";

    // 小黑屋金币
    public static final String PLAYER_GRANDPRIX_CONFIG_QZ_KEY = "player:grandprix:config:qz";

    // 赢上限参数
    public static final String PLAYER_GRANDPRIX_CONFIG_PQ_KEY = "player:grandprix:config:pq";

    // （历史）玩家累积赢取金币
    public static final String PLAYER_GRANDPRIX_CONFIG_QY_KEY = "player:grandprix:config:qy";

    // 输下限参数
    public static final String PLAYER_GRANDPRIX_CONFIG_PA_KEY = "player:grandprix:config:pa";

    // （历史）玩家累积输掉金币
    public static final String PLAYER_GRANDPRIX_CONFIG_QS_KEY = "player:grandprix:config:qs";

    // 幸运参数
    public static final String PLAYER_GRANDPRIX_CONFIG_PW_KEY = "player:grandprix:config:pw";

    // （当天）玩家累积赢取金币
    public static final String PLAYER_GRANDPRIX_CONFIG_QX_KEY = "player:grandprix:config:qx";

    // 挽救参数
    public static final String PLAYER_GRANDPRIX_CONFIG_PY_KEY = "player:grandprix:config:py";

    // （当天）玩家累计输掉金币
    public static final String PLAYER_GRANDPRIX_CONFIG_QW_KEY = "player:grandprix:config:qw";

    // 抽水参数
    public static final String PLAYER_GRANDPRIX_CONFIG_TP_KEY = "player:grandprix:config:tp";

    // 预留金币
    public static long initPool = 0;

    @Autowired
    private AgentManager agentManager;


    public FishingGrandPrixManager() {
        // 每一秒循环执行房间任务
        long loopTime = 1000;
        ThreadPoolUtils.TIMER_SERVICE_POOL.scheduleAtFixedRate(() -> {
            try {
                List<FishingGrandPrixRoom> gameRooms = GameContainer.getGameRooms(FishingGrandPrixRoom.class);
                for (FishingGrandPrixRoom gameRoom : gameRooms) {
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
            List<FishingGrandPrixRoom> gameRooms = GameContainer.getGameRooms(FishingGrandPrixRoom.class).stream()
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
                    FishingGrandPrixRoom room = gameRooms.get(ThreadLocalRandom.current().nextInt(0, roomNum));
                    long millis = System.currentTimeMillis();
                    if (room != null &&
                            room.getBoss() == 0 && // 房间无boss
                            !room.isFishTide() && // 房间无鱼潮
                            millis - room.getLastRoomFrozenTime() > FishingManager.SKILL_FROZEN_TIME // 房间未冰冻
                    ) {
                        // 房间置为有系统boss的状态
                        room.setBoss(1);
                        // 发送刷新boss响应
                        FishingGrandPrixRefreshBossResponse.Builder builder = FishingGrandPrixRefreshBossResponse.newBuilder();
                        sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_REFRESH_BOSS_RESPONSE_VALUE, builder);
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
    }

    /**
     * 向客户端响应大奖赛是否开始
     * 如果大奖赛已经开始，则返回true 和 玩家子弹剩余数量
     * 否则 返回false
     * @param playerId 玩家Id
     */
    public void start(Long playerId, ServerUser user) {
        new Date();
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        FishingGrandPrixStartResponse.Builder builder = FishingGrandPrixStartResponse.newBuilder();

        int bullet = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_BULLET_KEY + playerId, 2000);
        int games = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_GAMES_KEY + playerId, 0);

        //开始时间
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        builder.setStartTime(calendar.getTimeInMillis());

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        builder.setEndTime(calendar.getTimeInMillis());
        builder.setGames(games);

        if(hour >= 7 && hour < 23) {
            builder.setProgress(true);
            builder.setBullet(bullet);
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_START_RESPONSE_VALUE, builder, user);
        } else {
            builder.setProgress(false);
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_START_RESPONSE_VALUE, builder, user);
        }
    }

    /**
     * 响应用户排名
     * @param rankType
     * @param pageCurrent
     * @param pageSize
     * @param total
     * @param user
     */
    public  void rank(int rankType, int pageCurrent, int pageSize, int total, ServerUser user) {
        String key = null;
        if(rankType == 1)
            key = PLAYER_GRANDPRIX_CONFIG_RANK_WEEK_KEY;
        else
            key = PLAYER_GRANDPRIX_CONFIG_RANK_DAY_KEY;
        int i = total / pageSize;
        int start = i * (pageCurrent - 1);
        int end = (pageCurrent + 1) * i;


        FishingGrandPrixRankResponse.Builder builder = FishingGrandPrixRankResponse.newBuilder();
        builder.setRankType(rankType);

        Set<String> rankIds = RedisUtil.values(key, start, end - 1);
        int index = 0;
        for (String rankId : rankIds) {
            long playerId = Long.parseLong(rankId);
            FishingGrandPrixPlayerInfoMessage.Builder b = FishingGrandPrixPlayerInfoMessage.newBuilder();
            b.setPlayerId(playerId);
            b.setDayPoint(getPointDay(playerId));
            b.setWeekPoint(getPointWeek(playerId));
            b.setRank(++index);

            UserEntity userEntity = userMapper.findById(playerId);
            b.setName(userEntity.getNickname());
            b.setHeadIndex(userEntity.getHeadIndex());
            b.setHeadUrl(userEntity.getHeadUrl());
            b.setSex(userEntity.getSex());

            builder.addPlayerInfos(b);
        }

        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FINSHING_GRAND_PRIX_RANK_RESPONSE_VALUE, builder, user);

    }

    /**
     * 大奖赛 玩家加入房间
     * @param user
     */
    public void joinRoom(ServerUser user) {

        int batteryLevel = PlayerManager.getPlayerBatteryLevel(user);
        logger.info("玩家炮台等级：" + batteryLevel);
        if (batteryLevel < 1000) {
            NetManager.sendHintMessageToClient("炮台等级不足，请先升级", user);
            return;
        }
        // 是否开赛检查
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        if(hourOfDay >= 23 || hourOfDay < 9) {
            return ;
        }




        // 金币检查
        if (!PlayerManager.checkItem(user, ItemId.MONEY, RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_ENTRY_GOLD_KEY, 100000L))) {
            NetManager.sendHintMessageToClient("携带金币不足，无法进入该房间", user);
            return;
        }

        // 获取玩家子弹剩余数量与局数 （默认2000发，第一局）
        int bullet = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_BULLET_KEY + user.getId(), 2000);
        int games = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_GAMES_KEY + user.getId(), 0);

        List<String> weekPoint = RedisUtil.getList(PLAYER_GRANDPRIX_CONFIG_POINT_WEEK_KEY + user.getId());
        if(weekPoint == null || weekPoint.size() == 0) {
            RedisUtil.rightPush(PLAYER_GRANDPRIX_CONFIG_POINT_WEEK_KEY + user.getId(), "0", "0", "0", "0", "0", "0", "0");
        }

        // 玩家当日第一次加入游戏初始化
        if(games == 0 && bullet == 2000) {
            RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_GAMES_KEY + user.getId(), "0");
            RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_BULLET_KEY + user.getId(), "2000");
            RedisUtil.rightPush(PLAYER_GRANDPRIX_CONFIG_POINT_DAY_KEY + user.getId(), "0");
        }

        // 玩家打完第一局游戏
        if(games >= 0 && bullet == 0) {
            int diamond = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_ENTRY_DIAMOND_KEY, 20);

            if(!PlayerManager.checkItem(user, ItemId.DIAMOND, diamond)) {
                NetManager.sendHintMessageToClient(String.format("钻石不足%s，无法进入该房间!", diamond), user);
                return ;
            }
            PlayerManager.addItem(user, ItemId.DIAMOND, -20L, ItemChangeReason.FISHING_GRANDPRIX_JOIN_ROOM, true);

            bullet = 2000;
            games++;

            // 重新初始化用户子弹和游戏局数数据 添加新一局的积分信息
            RedisUtil.rightPush(PLAYER_GRANDPRIX_CONFIG_POINT_DAY_KEY + user.getId(), "0");
            RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_GAMES_KEY + user.getId(), String.valueOf(games));
            RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_BULLET_KEY + user.getId(), String.valueOf(bullet));
        }


        List<FishingGrandPrixRoom> gameRooms = GameContainer.getGameRooms(FishingGrandPrixRoom.class);
        for (FishingGrandPrixRoom gameRoom : gameRooms) {
            // 加入房间条件: 1:目标房间场次与玩家所选场次相同 2:房间人数未满
            if ( gameRoom.getMaxSize() > gameRoom.getPlayerSize()) {
                synchronized (gameRoom) {
                    joinFishingRoom(user, gameRoom);
                }
                return;
            }
        }
        // 没有房间就新建一个房间
        FishingGrandPrixRoom gameRoom = createFishingRoom();
        synchronized (gameRoom) {
            joinFishingRoom(user, gameRoom);
        }
    }

    /**
     * 创建一个房间
     * @return
     */
    private FishingGrandPrixRoom createFishingRoom() {
        FishingGrandPrixRoom gameRoom = GameContainer.createGameRoom(FishingGrandPrixRoom.class, 4);
        return gameRoom;
    }

    private void joinFishingRoom(ServerUser user, FishingGrandPrixRoom room) {
        long enterMoney = PlayerManager.getPlayerEntity(user).getMoney();
        FishingGrandPrixPlayer gamePlayer = GameContainer.createGamePlayer(room, user, FishingGrandPrixPlayer.class);

        gamePlayer.setEnterMoney(enterMoney);
        gamePlayer.setEnterRoomTime(System.currentTimeMillis());

        // 设置玩家在房间内的初始炮台等级
        // 玩家拥有的最高炮台等级
        //int batteryLevel = PlayerManager.getPlayerEntity(user).getBatteryLevel();

        gamePlayer.setBatteryLevel(1000);

        sendJoinRoomResponse(room, gamePlayer); // 发送加入房间消息
        sendPlayersInfoResponse(room, gamePlayer); // 发送玩家数据列表消息
        sendSynchroniseResponse(room, gamePlayer); // 发送同步鱼消息
        sendFrozenMessage(room, gamePlayer); // 发送房间当前冰冻消息
//        // 将自己的数据广播到房间所有玩家
        sendRoomPlayerInfoResponse(room, gamePlayer);
        sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_ROOM_PLAYER_INFO_RESPONSE_VALUE, createGrandPrixRoomPlayerInfoResponse(gamePlayer));
    }

    /**
     * 给房间内所有玩家发送某玩家信息
     */
    public void sendRoomPlayerInfoResponse(FishingGrandPrixRoom room, FishingGrandPrixPlayer player) {
        FishingGrandPrixRoomPlayerInfoResponse.Builder builder = FishingGrandPrixRoomPlayerInfoResponse.newBuilder();
        builder.setPlayerInfo(createGrandPrixRoomPlayerInfoResponse(player));
        sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_ROOM_PLAYER_INFO_RESPONSE_VALUE, builder);
        logger.info(builder + " ");
    }

    private FishingPlayerInfoMessage.Builder createGrandPrixRoomPlayerInfoResponse(FishingGrandPrixPlayer gamePlayer) {
        FishingPlayerInfoMessage.Builder builder = FishingPlayerInfoMessage.newBuilder();
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
        return builder;
    }

    /**
     * 向玩家发送房间当前的冰冻消息
     */
    public void sendFrozenMessage(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player) {
        long nowTime = System.currentTimeMillis();
        if (nowTime - gameRoom.getLastRoomFrozenTime() < FishingManager.SKILL_FROZEN_TIME) { // 房间处于冰冻状态
            FishingGrandPrixUseSkillResponse.Builder builder = FishingGrandPrixUseSkillResponse.newBuilder();
            builder.setSkillId(ItemId.SKILL_FROZEN.getId()); // 冰冻
            builder.setDuration((int) ((FishingManager.SKILL_FROZEN_TIME - (nowTime - gameRoom.getLastRoomFrozenTime())) / 1000));
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_USE_SKILL_RESPONSE_VALUE, builder, player.getUser());
        }
    }
    /**
     * 发送信息给房间所有玩家
     */
    public void sendRoomMessage(FishingGrandPrixRoom room, int msgCode, GeneratedMessage.Builder<?> message) {
        if (room != null) {

            for (BaseGamePlayer gamePlayer : room.getGamePlayers()) {
                if (gamePlayer != null) {
                    NetManager.sendMessage(msgCode, message, gamePlayer.getUser());
                }
            }
        }
    }

    /**
     * 发送玩家加入房间消息
     */
    private void sendJoinRoomResponse(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player) {
        FishingGrandPrixJoinRoomResponse.Builder builder = FishingGrandPrixJoinRoomResponse.newBuilder();


        Integer games = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_GAMES_KEY + player.getId(), 0);
        builder.setRoomCode(gameRoom.getCode());
        builder.setBullet(RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_BULLET_KEY + player.getId(), 2000));
        builder.setDayPoint(RedisUtil.get(PLAYER_GRANDPRIX_CONFIG_POINT_DAY_KEY, games));
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_JOIN_ROOM_RESPONSE_VALUE, builder, player.getUser());
    }

    /**
     * 发送玩家列表消息
     */
    private void sendPlayersInfoResponse(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player) {
        FishingGrandPrixRoomPlayerInfoListResponse.Builder builder = FishingGrandPrixRoomPlayerInfoListResponse.newBuilder();
        for (BaseGamePlayer gamePlayer : gameRoom.getGamePlayers()) {
            if (gamePlayer != null) {

                long enterMoney = PlayerManager.getPlayerEntity(gamePlayer.getUser()).getMoney();
                FishingGrandPrixPlayer fishingGrandPrixPlayer = (FishingGrandPrixPlayer) gamePlayer;
                fishingGrandPrixPlayer.setEnterMoney(enterMoney);
                builder.addPlayerInfos(createPlayerInfoMessage(fishingGrandPrixPlayer));
                logger.info(gamePlayer.getUser().getNickname());
            }
        }
        if(gameRoom.getGamePlayers().length == 0) return;
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_ROOM_PLAYER_INFO_LIST_RESPONSE_VALUE, builder, player.getUser());
    }


    private FishingPlayerInfoMessage.Builder createPlayerInfoMessage(FishingGrandPrixPlayer player) {
        FishingPlayerInfoMessage.Builder builder = FishingPlayerInfoMessage.newBuilder();
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
     * 发送同步鱼消息
     */
    public void sendSynchroniseResponse(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player) {
        FishingGrandPrixSynchroniseResponse.Builder builder = FishingGrandPrixSynchroniseResponse.newBuilder();
        for (FishStruct fish : gameRoom.getFishMap().values()) {
            builder.addFishInfos(createFishInfoProto(fish));
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_SYNCHRONISE_RESPONSE_VALUE, builder, player.getUser());
    }

    /**
     * 创建鱼结构
     * @param fish
     * @return
     */
    private FishingGrandPrixFishInfoMessage.Builder createFishInfoProto(FishStruct fish) {
        FishingGrandPrixFishInfoMessage.Builder builder = FishingGrandPrixFishInfoMessage.newBuilder();
        builder.setId(fish.getId());
        builder.setFishId(fish.getConfigId());
        builder.setRouteId(fish.getRouteId());
        builder.setClientLifeTime(fish.getClientLifeTime());
        builder.setCreateTime(fish.getCreateTime());
        return builder;
    }

    public void getPlayerInfo(String playerId, ServerUser user) {

        FishingGrandPrixPlayerInfoResponse.Builder builder = FishingGrandPrixPlayerInfoResponse.newBuilder();

        builder.setPlayerInfo(createGrandPrixPlayerInfoMessage(Long.parseLong(playerId)));
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_FINSHING_GRAND_PRIX_PLAYER_INFO_RESPONSE_VALUE, builder, user);
    }

    /**
     * 获取周积分
     * @param playerId
     * @return
     */
    private int getPointWeek(long playerId) {
        return getPoint(PLAYER_GRANDPRIX_CONFIG_RANK_WEEK_KEY, String.valueOf(playerId));
    }

    /**
     * 获取今日最高积分
     * @param playerId
     * @return
     */
    private int getPointDay(long playerId) {
        return getPoint(PLAYER_GRANDPRIX_CONFIG_RANK_DAY_KEY, String.valueOf(playerId));
    }

    private int getPoint(String key, String playerId) {
//        List<String> list = RedisUtil.getList(key);
//        if(list == null || list.size() == 0) return 0;
//        Optional<String> max = list.stream().max(Comparator.comparingInt(Integer::valueOf));
//        return Integer.valueOf(max.get());
        Double point = RedisUtil.zScore(key, playerId);
        return point == null ? 0 : point.intValue();
    }

    private FishingGrandPrixPlayerInfoMessage.Builder createGrandPrixPlayerInfoMessage(long playerId) {
        FishingGrandPrixPlayerInfoMessage.Builder builder = FishingGrandPrixPlayerInfoMessage.newBuilder();
        builder.setPlayerId(playerId);
        builder.setDayPoint(getPointDay(playerId));
        builder.setWeekPoint(getPointWeek(playerId));
        Set<String> ranksId = RedisUtil.values(PLAYER_GRANDPRIX_CONFIG_RANK_DAY_KEY, 0, 50 - 1);
        int index = 0;

        boolean contains = ranksId.contains(String.valueOf(playerId));
        if(contains) {
            for (String s : ranksId) {
                index++;
                if(s.equals(String.valueOf(playerId))) {
                    builder.setRank(index);
                    break;
                }

            }
        } else {
            builder.setRank(0);
        }


        // 玩家奖励

        return builder;

    }


    /**
     * 玩家发射子弹处理
     * @param gameRoom
     * @param player
     * @param fire
     */
    public void playerFire(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player, FireStruct fire) {
        long needMoney = player.getBatteryLevel() * player.getBatteryMult();
        int bullet = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_BULLET_KEY + player.getId(), 2000);
        int games = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_GAMES_KEY + player.getId(), 0);
        if (System.currentTimeMillis() - player.getLastFenShenTime() < SKILL_FEN_SHEN_TIME) { // 还在分身阶段就要扣三发子弹的钱
            int fireCount = 3;
            needMoney *= fireCount;
            fire.setCount(fireCount);
        }

        // 子弹发射完 不对其进行处理
        if(bullet <= 0 || needMoney <= 0) {
            return;
        }

        bullet--;
        RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_BULLET_KEY + player.getId(),String.valueOf(bullet));
//
//        // 检查龙晶是否足够
//        if (!PlayerManager.checkItem(player.getUser(), ItemId.DRAGON_CRYSTAL, needMoney)) {
//            return;
//        }
//        int index = gameRoom.getRoomIndex() - 1;
//        // 小黑屋
//        FishingHitDataManager.addChallengeBlackRoom(player.getId(), index, -needMoney);
//
        player.setLastFireTime(System.currentTimeMillis());
        player.addMoney(-needMoney);

        // 用户消耗金币存入大奖赛游戏库存
        long stock = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, 0L);
        stock += needMoney;
        RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, String.valueOf(stock));

        // 用户消耗金币存入用户自身库存
        Long pointTotal = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_POINT_TOTAL_KEY + player.getId(), 0L);
        pointTotal -= needMoney;
        RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_POINT_TOTAL_KEY + player.getId(), String.valueOf(pointTotal));

        Long pointDayTotal = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_POINT_DAY_TOTAL_KEY + player.getId(), 0L);
        pointDayTotal -= needMoney;
        RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_POINT_DAY_TOTAL_KEY + player.getId(), String.valueOf(pointDayTotal));

        Long pointWeekTotal = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_POINT_WEEK_TOTAL_KEY + player.getId(), 0L);
        pointWeekTotal -= needMoney;
        RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_POINT_WEEK_TOTAL_KEY + player.getId(), String.valueOf(pointWeekTotal));
//        FishingHitDataManager.addChallengeWin(gameRoom, player, -needMoney);
//
        fire.setLevel(player.getBatteryLevel());
        fire.setMult(player.getBatteryMult());
        player.getFireMap().put(fire.getId(), fire);
//
        // 广播玩家发送子弹响应
        FishingGrandPrixFireResponse.Builder builder = FishingGrandPrixFireResponse.newBuilder();
        builder.setFireId(fire.getId());
        builder.setFishId(fire.getFishId());
        builder.setAngle(fire.getAngle());
        builder.setRestMoney(player.getMoney());
        builder.setPlayerId(player.getId());
        builder.setBullet(bullet);

        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_FIRE_RESPONSE_VALUE, builder);
    }

    public List<FishConfig> playerFightFish(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player, long fireId, List<Long> fishIds, boolean b) {
        try {
            List<FishConfig> configs = fightFish(gameRoom, player, fireId, fishIds, b);
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
    private List<FishConfig> fightFish(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player,
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
                boolean hit = isHit(player, gameRoom.getRoomIndex(), fish, config);
                if (!boom && !hit) {
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
                    if (System.currentTimeMillis() - player.getLastCritTime() < SKILL_CRIT_TIME) {
                        winMoney *= 1.5;
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
                        FishingGrandPrixCatchBossFishResponse.Builder builder = FishingGrandPrixCatchBossFishResponse.newBuilder();
                        builder.setFishName(config.getName());
                        builder.setMoney(winMoney);
                        builder.setPlayerName(player.getUser().getNickname());
                        builder.setPlayerVipLevel(player.getVipLevel());
                        builder.setBatteryLevel(player.getBatteryLevel());
                        sendCatchBossFishResponse(builder);
                    }
                }

                // TODO 记录玩家金币和小黑屋相关数据
//                FishingHitDataManager.addChallengeWin(gameRoom, player, winMoney);
//                int greener = 1;
//                int index = gameRoom.getRoomIndex() - 1;
//                if (winMoney > FishingHitDataManager.BLACK_ROOM_LIMIT[greener][index]) {
//                    FishingHitDataManager.addChallengeBlackRoom(player.getId(), index, winMoney);
//                }

                // 玩家收获金币
                player.addMoney(winMoney);

                // 打死鱼之后掉落的物品
                List<OseePublicData.ItemDataProto> dropItems = new LinkedList<>();
                if (config.getSkill() > 0) {
                    if (config.getSkill() > 5 && config.getSkill() < 9) { // 特殊技能鱼
                        FishingGrandPrixUseSkillResponse.Builder builder = FishingGrandPrixUseSkillResponse.newBuilder();
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
                        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_USE_SKILL_RESPONSE_VALUE, builder);
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

                int games = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_GAMES_KEY + player.getId(), 0);
                int dayPoint = RedisUtil.get(PLAYER_GRANDPRIX_CONFIG_POINT_DAY_KEY + player.getId(), games);

                // 爆炸炸死的鱼不向玩家发送消息
                if (!boom) {

                    FishingGrandPrixFightFishResponse.Builder builder = FishingGrandPrixFightFishResponse.newBuilder();
                    builder.setFishId(fishId);
                    builder.setPlayerId(player.getId());
                    builder.setRestMoney(player.getMoney());
                    builder.setDropMoney(winMoney);
                    builder.setDayPoint((int) (dayPoint + winMoney));
                    builder.addAllDropItems(dropItems);
                    builder.setMultiple(randomMoney); // 鱼倍数
                    sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_FIGHT_FISH_RESPONSE_VALUE, builder);
                    long stock = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, 0L);
                    stock -= winMoney;
                    RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, String.valueOf(stock));


                    // 用户消耗金币存入用户自身库存
                    Long pointTotal = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_POINT_TOTAL_KEY + player.getId(), 0L);
                    pointTotal += winMoney;
                    RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_POINT_TOTAL_KEY + player.getId(), String.valueOf(pointTotal));

                    Long pointDayTotal = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_POINT_DAY_TOTAL_KEY + player.getId(), 0L);
                    pointDayTotal += winMoney;
                    RedisHelper.set(PLAYER_GRANDPRIX_CONFIG_POINT_DAY_TOTAL_KEY + player.getId(), String.valueOf(pointDayTotal));

                    RedisUtil.set(PLAYER_GRANDPRIX_CONFIG_POINT_DAY_KEY + player.getId(), String.valueOf(dayPoint + winMoney), games);
                    logger.info("击中 " + builder);
                    // 游走字幕播报
                    if (fish.getFishType() == 100) { // boss鱼才播报
                        String text = String.format(
                                AutoWanderSubtitle.TEMPLATES[ThreadLocalRandom.current().nextInt(8, 10)],
                                player.getUser().getNickname(), randomMoney, config.getName(), winMoney / 10000
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
    private void sendCatchBossFishResponse(FishingGrandPrixCatchBossFishResponse.Builder builder) {
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> {
            // 只发送到全服在捕鱼房间内的玩家
            List<FishingGrandPrixRoom> fishingGameRooms = GameContainer.getGameRooms(FishingGrandPrixRoom.class);
            for (FishingGrandPrixRoom gameRoom : fishingGameRooms) {
                if (gameRoom != null) {
                    sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_CATCH_BOSS_FISH_RESPONSE_VALUE, builder);
                }
            }
        }, 0, TimeUnit.SECONDS);
    }


    /**
     * this code like shit, i don't want to change it, good luck
     * @param gamePlayer
     * @param roomIndex
     * @param fish
     * @param config
     * @return
     */
    private boolean isHit(FishingGrandPrixPlayer gamePlayer, int roomIndex, FishStruct fish, FishConfig config) {
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


        Double val = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_AP_KEY, 0.0D);
        Long val1 = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, 0L);
        Long val2 = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_APT_KEY, 100L);
        double baseCoefficient = 100D + val + val1 / (val2 * 0.01);

        long blackRoom = RedisUtil.val(PLAYER_GRANDPRIX_CONFIG_BLACK_ROOM_KEY, 0L);


//        if(blackRoom )



        // 当日输赢金币限制
//        long dailyWin = FishingHitDataManager.getChallengeDailyWin(gamePlayer.getId(), index);
//        if (dailyWin < -FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][0]) {
//            baseCoefficient += FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][0]; // 幸运玩家系数
//        } else if (dailyWin > FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][1]) {
//            baseCoefficient -= FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][1]; // 挽救玩家系数
//        }



        // 小黑屋限制
        if (RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QZ_KEY, Long.MAX_VALUE) < RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_POINT_TOTAL_KEY + gamePlayer.getId(), 0L)) {
            baseCoefficient -= RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_BP_KEY, 0.0D);
        }

        // 总计输赢金币限制
        long totalWin = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QZ_KEY, 0L);
        long totalWinToday = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QX_KEY, 0L);
        long stock = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_POINT_TOTAL_KEY + gamePlayer.getId(), 0L);

        // 玩家历史累积赢取金币 判断
        if (totalWin < stock) {
            baseCoefficient -= RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_PQ_KEY, 0.0);
        }

        // 玩家当天累积赢取金币 设置
        if (totalWinToday < RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_POINT_DAY_TOTAL_KEY + gamePlayer.getId(), 0L) ) {
            baseCoefficient -= RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_PY_KEY, 0.0);
        }

        baseCoefficient -= ((stock - FishingUtil.q0[11]) * 1.0 / FishingUtil.apt[11]) * 0.01;

        // 公用玩家AP值
        baseCoefficient += FishingHitDataManager.getPlayerFishingProb(gamePlayer.getId());
        baseCoefficient += (stock / 100.0 * 0.01);

        // 玩家暴击状态下命中率降低
        if ((System.currentTimeMillis() - gamePlayer.getLastCritTime()) / 1000 < FishingManager.SKILL_CRIT_TIME / 1000) {
            baseCoefficient *= 0.75;
        }



        // 判断是否命中
        return !(baseHit * baseCoefficient / 100D < ThreadLocalRandom.current().nextDouble());
    }

    public void exitRoom(FishingGrandPrixPlayer player, FishingGrandPrixRoom room) {
        FishingGrandPrixQuitResponse.Builder builder = FishingGrandPrixQuitResponse.newBuilder();
        builder.setPlayerId(player.getId());
        sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_TTMY_FINSHING_GRAND_PRIX_QUIT_RESPONSE_VALUE, builder);
        // 退出房间后的操作，比如日志记录等
        ServerUser user = player.getUser();
        OseePlayerEntity entity = PlayerManager.getPlayerEntity(user);
        agentManager.addActiveMoney(user.getId(), GameEnum.FISHING, 0, player.getCutMoney());


        // 保存本局抽水记录
        fishingUtil.saveCutProb(player, GameEnum.FISHING_GRANDPRIX.getId());


        // 把玩家从房间移除 VIP房间如果没人就删除
        GameContainer.removeGamePlayer(room, player.getSeat(), room.isVip());

        // 保存今日最高积分到每周积分
        List<String> dayPoint = RedisUtil.getList(PLAYER_GRANDPRIX_CONFIG_POINT_DAY_KEY + user.getId());
        String maxDayPint = dayPoint.stream().max(Comparator.comparingInt(Integer::parseInt)).get();
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        RedisUtil.set(PLAYER_GRANDPRIX_CONFIG_POINT_WEEK_KEY + user.getId(), maxDayPint, dayOfWeek - 1);

        List<String> weekPoint = RedisUtil.getList(PLAYER_GRANDPRIX_CONFIG_POINT_WEEK_KEY + user.getId())
                .stream()
                .sorted((Comparator.comparing(Integer::parseInt)))
                .sorted(Comparator.reverseOrder())
                .limit(3)
                .collect(Collectors.toList());

        long totalWeekPoint = 0;
        for(int i = 0; i < weekPoint.size(); i++) {
            totalWeekPoint += Long.parseLong(weekPoint.get(i));
        }

        // 刷新日排行榜
        RedisUtil.zadd(PLAYER_GRANDPRIX_CONFIG_RANK_DAY_KEY, String.valueOf(user.getId()), Double.parseDouble(maxDayPint));
        // 刷新周排行榜
        RedisUtil.zadd(PLAYER_GRANDPRIX_CONFIG_RANK_WEEK_KEY, String.valueOf(user.getId()), (double) totalWeekPoint);
    }

    /**
     * 使用技能
     * @param gameRoom
     * @param player
     * @param skillId
     */
    public void useSkill(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player, int skillId) {
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

        FishingGrandPrixUseSkillResponse.Builder builder = FishingGrandPrixUseSkillResponse.newBuilder();
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

        // 扣除使用的技能数量
        PlayerManager.addItem(user, skillId, -1, ItemChangeReason.USE_ITEM, true);

        builder.setRestMoney(player.getMoney());
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_USE_SKILL_RESPONSE_VALUE, builder);
    }


    /**
     * 房间循环任务,刷鱼等
     */
    private void doFishingRoomTask(FishingGrandPrixRoom gameRoom) {
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
            FishingGrandPrixFishTideResponse.Builder builder = FishingGrandPrixFishTideResponse.newBuilder();
            sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_FISH_TIDE_RESPONSE_VALUE, builder);
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
                    nowTime - gameRoom.getLastRoomFrozenTime() > SKILL_FROZEN_TIME) {
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
            FishingGrandPrixPlayer player = gameRoom.getGamePlayerBySeat(i);
            // 检查玩家是否长时间未操作
            if (player != null && nowTime - player.getLastFireTime() > ROOM_KICK_TIME) {
                NetManager.sendHintMessageToClient("您长时间未操作，已被移出捕鱼房间", player.getUser());
                exitRoom(player, gameRoom);
            }
        }

        // 判断过期鱼，并从鱼表内移除
        List<Long> removeKey = new LinkedList<>();
        for (FishStruct fish : gameRoom.getFishMap().values()) {
            long maxLifeTime = Math.round(fish.getLifeTime() > 0 ? fish.getLifeTime() : DEFAULT_LIFE_TIME);
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
    private void refreshGroupFish(FishingGrandPrixRoom gameRoom, long ruleId) {
        FishRefreshRule rule = DataContainer.getData(ruleId, FishRefreshRule.class);
        FishGroupConfig group = DataContainer.getRandomData(FishGroupConfig.class, rule.getStart(), rule.getEnd());
        refreshGroupFish(gameRoom, ruleId, group);
    }

    /**
     * 刷新指定的一群鱼
     */
    private void refreshGroupFish(FishingGrandPrixRoom gameRoom, long ruleId, FishGroupConfig groupConfig) {
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
    private void refreshFishWithDelay(FishingGrandPrixRoom gameRoom, List<Long> fishIds, long routeId, long ruleId, double delay) {
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> refreshFish(gameRoom, fishIds, routeId, ruleId),
                (int) (delay * 1000), TimeUnit.MILLISECONDS);
    }

    /**
     * 刷新一组鱼
     */
    private void refreshFish(FishingGrandPrixRoom gameRoom, List<Long> fishIds, long routeId, long ruleId) {
        long nowTime = System.currentTimeMillis();
        if (nowTime - gameRoom.getLastRoomFrozenTime() < FishingManager.SKILL_FROZEN_TIME) {
            return;
        }

        FishRefreshRule rule = DataContainer.getData(ruleId, FishRefreshRule.class);
        if (!rule.isFishTide() && gameRoom.isFishTide()) { // 如果不是鱼潮中的鱼，且房间正处于鱼潮中，中止刷此鱼
            return;
        }

        FishingGrandPrixRefreshFishesResponse.Builder builder = FishingGrandPrixRefreshFishesResponse.newBuilder();
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
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_REFRESH_FISHES_RESPONSE_VALUE, builder);
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
     * 更换炮台等级
     */
    public void changeBatteryLevel(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player, int targetLevel) {
        // 判断炮台等级数值是否有误
        if (targetLevel < 1000 || targetLevel > 9500) {
            NetManager.sendErrorMessageToClient("更换的炮台等级有误", player.getUser());
            return;
        }


        // 改变玩家当前炮台等级
        player.setBatteryLevel(targetLevel);

        FishingGrandPrixChangeBatteryLevelResponse.Builder builder =FishingGrandPrixChangeBatteryLevelResponse.newBuilder();
        builder.setPlayerId(player.getId());
        builder.setLevel(player.getBatteryLevel());
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_CHANGE_BATTERY_LEVEL_RESPONSE_VALUE, builder);
    }

    public void changeBatteryView(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player, int viewIndex) {
        int msgCode = OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_CHANGE_BATTERY_VIEW_RESPONSE_VALUE;
        if (viewIndex >= ItemId.QSZS_BATTERY_VIEW.getId() && viewIndex <= ItemId.SWHP_BATTERY_VIEW.getId()) { // 切换到自己购买的炮台外观
            if (PlayerManager.getItemNum(player.getUser(), ItemId.getItemIdById(viewIndex)) <= 0) {
                NetManager.sendHintMessageToClient("该炮台外观已到期", player.getUser());
                return;
            }
            player.setViewIndex(viewIndex);
            FishingGrandPrixChangeBatteryViewResponse.Builder builder = FishingGrandPrixChangeBatteryViewResponse.newBuilder();
            builder.setPlayerId(player.getId());
            builder.setViewIndex(player.getViewIndex());
            sendRoomMessage(gameRoom, msgCode, builder);
        } else if (PlayerManager.getPlayerVipLevel(player.getUser()) >= viewIndex) {
            player.setViewIndex(viewIndex);
            FishingGrandPrixChangeBatteryViewResponse.Builder builder = FishingGrandPrixChangeBatteryViewResponse.newBuilder();
            builder.setPlayerId(player.getId());
            builder.setViewIndex(player.getViewIndex());
            sendRoomMessage(gameRoom, msgCode, builder);
        } else {
            NetManager.sendHintMessageToClient("您的vip等级不足，无法更改该炮台外观", player.getUser());
        }
    }

    public void useBossBugle(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player) {
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
//        long ruleId = ruleIds[gameRoom.getRoomIndex() - 1]; // 获取当前场次的boss刷新规则

        // 按照boss规则刷新
        refreshGroupFish(gameRoom, 18);
        FishingGrandPrixUseBossBugleResponse.Builder builder = FishingGrandPrixUseBossBugleResponse.newBuilder();
        builder.setPlayerId(player.getId());
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_TTMY_FISHING_GRAND_PRIX_USE_BOSS_BUGLE_RESPONSE_VALUE, builder);
    }

    /**
     * 发送重新激活消息相关消息
     */
    public void sendReactiveMessage(FishingGrandPrixRoom gameRoom, FishingGrandPrixPlayer player) {
        sendJoinRoomResponse(gameRoom, player); // 发送加入房间消息
        sendPlayersInfoResponse(gameRoom, player); // 发送玩家数据列表消息
        sendSynchroniseResponse(gameRoom, player); // 发送同步鱼消息
        sendFrozenMessage(gameRoom, player); // 发送房间当前冰冻消息
    }
}
