package com.maple.game.osee.manager.fishing;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.manager.GsonManager;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.entity.fishing.FishingGamePlayer;
import com.maple.game.osee.entity.fishing.FishingGameRoom;
import com.maple.game.osee.entity.fishing.challenge.FishingChallengePlayer;
import com.maple.game.osee.entity.fishing.challenge.FishingChallengeRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 捕鱼命中数据管理类
 */
@Component
public class FishingHitDataManager {

    private static Logger logger = LoggerFactory.getLogger(FishingHitDataManager.class);

    /**
     * 新手等级限制
     */
    public static int GREENER_LIMIT = 15;

    /**
     * 每日赢取金币表
     */
    private static Map<Long, Map<Integer, Long>> DAILY_WIN_MAP = new ConcurrentHashMap<>();

    /**
     * 总赢取金币表
     */
    private static Map<Long, Map<Integer, Long>> TOTAL_WIN_MAP = new ConcurrentHashMap<>();

    /**
     * 小黑屋玩家表
     */
    private static Map<Long, Map<Integer, Long>> BLACK_ROOM_MAP = new ConcurrentHashMap<>();

    /**
     * 玩家系数表
     */
    private static Map<Long, Double> PLAYER_FISHING_PROB_MAP = new ConcurrentHashMap<>();

    /**
     * 初始库存
     */
    public static long[][] FISHING_INIT_POOL = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0}};

    /**
     * 捕鱼库存
     */
    public static long[][] FISHING_POOL = {
            {0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0}};

    /**
     * 全服捕鱼参数
     */
    public static double[][] FISHING_PROB = {
            {0D, 0D, 0D, 0D, 0D, 0D, 0D},
            {0D, 0D, 0D, 0D, 0D, 0D, 0D}};

    /**
     * 全服捕鱼参数单位变化量
     */
    public static double[][] FISHING_UNIT_PROB = {
            {0.01D, 0.01D, 0.01D, 0.01D, 0.01D, 0.01D, 0.01D},
            {0.01D, 0.01D, 0.01D, 0.01D, 0.01D, 0.01D, 0.01D}};

    /**
     * 单位变化量所需库存金币
     */
    public static long[][] FISHING_PER_UNIT_MONEY = {
            {100L, 100L, 100L, 100L, 100L, 100L, 100L},
            {100L, 100L, 100L, 100L, 100L, 100L, 100L}
    };

    /**
     * 捕鱼抽水参数
     */
    public static double[][] FISHING_CUT_PROB = {
            {0D, 0D, 0D, 0D, 0D, 0D, 0D},
            {0D, 0D, 0D, 0D, 0D, 0D, 0D}
    };

    /**
     * 玩家当日输赢影响概率(输、赢)
     */
    public static double[][][] PLAYER_DAILY_PROB = {
            {{0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}},
            {{0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}}
    };

    /**
     * 玩家当日输赢限制值(输、赢)
     */
    public static long[][][] PLAYER_DAILY_LIMIT = {
            {{0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}},
            {{0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}}
    };

    /**
     * 玩家总计输赢影响概率(输、赢)
     */
    public static double[][][] PLAYER_TOTAL_PROB = {
            {{0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}},
            {{0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}, {0D, 0D}}
    };

    /**
     * 玩家总计输赢限制值(输、赢)
     */
    public static long[][][] PLAYER_TOTAL_LIMIT = {
            {{0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}},
            {{0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}, {0L, 0L}}
    };

    /**
     * 小黑屋影响概率
     */
    public static double[][] BLACK_ROOM_PROB = {
            {0D, 0D, 0D, 0D, 0D, 0D, 0D},
            {0D, 0D, 0D, 0D, 0D, 0D, 0D}
    };

    /**
     * 小黑屋限制值
     */
    public static long[][] BLACK_ROOM_LIMIT = {
            {0L, 0L, 0L, 0L, 0L, 0L, 0L},
            {0L, 0L, 0L, 0L, 0L, 0L, 0L}
    };

    public static Type getType() {
        return new TypeToken<Map<Integer, Long>>() {
        }.getType();
    }

    /**
     * 获取新人标识
     */
    public static int getGreener(FishingGamePlayer player) {
        return player.getLevel() >= GREENER_LIMIT ? 1 : 0;
    }

    /**
     * 获取玩家日常输赢表
     */
    public static Map<Integer, Long> getPlayerDailyMap(long playerId) {
        if (!DAILY_WIN_MAP.containsKey(playerId)) {
            DAILY_WIN_MAP.put(playerId, new HashMap<>());
        }
        return DAILY_WIN_MAP.get(playerId);
    }

    /**
     * 获取玩家总输赢表
     */
    public static Map<Integer, Long> getPlayerTotalMap(long playerId) {
        if (!TOTAL_WIN_MAP.containsKey(playerId)) {
            String key = String.format("Fishing:TotalWin:%d", playerId);
            String value = RedisHelper.get(key);
            if (!StringUtils.isEmpty(value)) {
                Map<Integer, Long> blackMap = new Gson().fromJson(value, getType());
                TOTAL_WIN_MAP.put(playerId, blackMap);
            } else {
                TOTAL_WIN_MAP.put(playerId, new HashMap<>());
            }
        }
        return TOTAL_WIN_MAP.get(playerId);
    }

    /**
     * 获取玩家小黑屋表
     */
    public static Map<Integer, Long> getPlayerBlackMap(long playerId) {
        if (!BLACK_ROOM_MAP.containsKey(playerId)) {
            String key = String.format("Fishing:BlackRoom:%d", playerId);
            String value = RedisHelper.get(key);
            if (!StringUtils.isEmpty(value)) {
                Map<Integer, Long> blackMap = new Gson().fromJson(value, getType());
                BLACK_ROOM_MAP.put(playerId, blackMap);
            } else {
                BLACK_ROOM_MAP.put(playerId, new HashMap<>());
            }
        }
        return BLACK_ROOM_MAP.get(playerId);
    }

    /**
     * 获取每日输赢金币
     */
    public static long getDailyWin(long playerId, int index) {
        return getPlayerDailyMap(playerId).getOrDefault(index, 0L);
    }

    /**
     * 增加每日输赢金币
     */
    public static void addDailyWin(long playerId, int index, long addMoney) {
        Map<Integer, Long> dailyMap = getPlayerDailyMap(playerId);
        synchronized (dailyMap) {
            dailyMap.put(index, dailyMap.getOrDefault(index, 0L) + addMoney);
        }
    }

    /**
     * 获取小黑屋金币
     */
    public static long getBlackRoom(long playerId, int index) {
        return getPlayerBlackMap(playerId).getOrDefault(index, 0L);
    }

    /**
     * 增加小黑屋金币
     */
    public static void addBlackRoom(long playerId, int index, long addMoney) {
        Map<Integer, Long> blackMap = getPlayerBlackMap(playerId);
        // synchronized (blackMap) {
        // long black = blackMap.getOrDefault(index, 0L) + addMoney;
        // black = Math.max(0L, black);
        // blackMap.put(index, black);
        // }
        blackMap.put(index, blackMap.getOrDefault(index, 0L));
    }

    /**
     * 获取总输赢金币
     */
    public static long getTotalWin(long playerId, int index) {
        return getPlayerTotalMap(playerId).getOrDefault(index, 0L);
    }

    /**
     * 增加总输赢金币
     */
    public static void addTotalWin(long playerId, int index, long addMoney) {
        Map<Integer, Long> totalMap = getPlayerTotalMap(playerId);
        synchronized (totalMap) {
            totalMap.put(index, totalMap.getOrDefault(index, 0L) + addMoney);
        }
    }

    /**
     * 增加输赢金币
     */
    public static void addWin(FishingGameRoom gameRoom, FishingGamePlayer player, long addMoney) {
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> {
            long money = addMoney;
            int index = gameRoom.getRoomIndex() - 1;
            //logger.info("index: " + index);
            addDailyWin(player.getId(), index, money); // 每日输赢
            addTotalWin(player.getId(), index, money); // 总输赢
            player.setChangeMoney(player.getChangeMoney() + money);
            if (money < 0) {
                player.setSpendMoney(player.getSpendMoney() + money);
            } else {
                player.setWinMoney(player.getWinMoney() + money);
            }

            int greener = getGreener(player);
            if (money < 0) { // 发射子弹消耗
                long cutMoney = (long) (money * (FISHING_CUT_PROB[greener][index] / 100D));
                money -= cutMoney;
                // 设置总抽水金币
                player.setCutMoney(player.getCutMoney() - cutMoney);
            }
            synchronized (FISHING_POOL) {
                // 服务器库存
                FISHING_POOL[greener][index] -= money;
            }
        }, 0, TimeUnit.SECONDS);
    }

    /**
     * 获取玩家捕鱼系数
     */
    public static double getPlayerFishingProb(long playerId) {
        if (PLAYER_FISHING_PROB_MAP.containsKey(playerId)) {
            return PLAYER_FISHING_PROB_MAP.get(playerId);
        }

        String key = String.format("Fishing:PlayerFishingProb:%d", playerId);
        String value = RedisHelper.get(key);
        if (!StringUtils.isEmpty(value)) {
            double result = Double.parseDouble(value);
            PLAYER_FISHING_PROB_MAP.put(playerId, result);
            return result;
        }

        PLAYER_FISHING_PROB_MAP.put(playerId, 0D);
        return 0;
    }

    /**
     * 设置玩家捕鱼参数
     */
    public static void setPlayerFishingProb(long playerId, double prob) {
        PLAYER_FISHING_PROB_MAP.put(playerId, prob);
    }

    /**
     * 重置全服玩家捕鱼系数
     */
    public static void resetAllPlayerFishingProb() {
        PLAYER_FISHING_PROB_MAP.clear();
        RedisHelper.removePattern("Fishing:PlayerFishingProb:*");
    }

    /**
     * 获取库存金币
     */
    public static long getPool(int greener, int roomIndex) {
        return FISHING_POOL[greener][roomIndex - 1];
    }

    /**
     * 获取服务器捕鱼系数
     */
    public static double getServerProb(int greener, int index) {
        if (FISHING_PER_UNIT_MONEY[greener][index] <= 0) {
            return Double.MIN_VALUE;
        }
        return FISHING_PROB[greener][index] + (FISHING_POOL[greener][index] - FISHING_INIT_POOL[greener][index])
                / (double) FISHING_PER_UNIT_MONEY[greener][index] * FISHING_UNIT_PROB[greener][index];
    }

    /**
     * 初始化数据
     */
    public static void init() {
        Map<String, String> settings = RedisHelper.getPatternMap("Fishing:Setting:*");

        for (Entry<String, String> setting : settings.entrySet()) {
            switch (setting.getKey()) {
                case "Fishing:Setting:Pool:InitPool":
                    FISHING_INIT_POOL = GsonManager.gson.fromJson(setting.getValue(), long[][].class);
                    break;
                case "Fishing:Setting:Pool:Pool":
                    FISHING_POOL = GsonManager.gson.fromJson(setting.getValue(), long[][].class);
                    break;
                case "Fishing:Setting:Pool:Prob":
                    FISHING_PROB = GsonManager.gson.fromJson(setting.getValue(), double[][].class);
                    break;
                case "Fishing:Setting:Pool:UnitProb":
                    FISHING_UNIT_PROB = GsonManager.gson.fromJson(setting.getValue(), double[][].class);
                    break;
                case "Fishing:Setting:Pool:PerUnitMoney":
                    FISHING_PER_UNIT_MONEY = GsonManager.gson.fromJson(setting.getValue(), long[][].class);
                    break;
                case "Fishing:Setting:Daily:Prob":
                    PLAYER_DAILY_PROB = GsonManager.gson.fromJson(setting.getValue(), double[][][].class);
                    break;
                case "Fishing:Setting:Daily:Limit":
                    PLAYER_DAILY_LIMIT = GsonManager.gson.fromJson(setting.getValue(), long[][][].class);
                    break;
                case "Fishing:Setting:Total:Prob":
                    PLAYER_TOTAL_PROB = GsonManager.gson.fromJson(setting.getValue(), double[][][].class);
                    break;
                case "Fishing:Setting:Total:Limit":
                    PLAYER_TOTAL_LIMIT = GsonManager.gson.fromJson(setting.getValue(), long[][][].class);
                    break;
                case "Fishing:Setting:BlackRoom:Prob":
                    BLACK_ROOM_PROB = GsonManager.gson.fromJson(setting.getValue(), double[][].class);
                    break;
                case "Fishing:Setting:BlackRoom:Limit":
                    BLACK_ROOM_LIMIT = GsonManager.gson.fromJson(setting.getValue(), long[][].class);
                    break;
                case "Fishing:Setting:Greener:Limit":
                    GREENER_LIMIT = Integer.parseInt(setting.getValue());
                    break;
            }
        }
    }

    // *****************************************************************************************

    /**
     * 设置捕鱼配置
     */
    public static void setFishingConfig(int greener, int index, Map<String, Object> config) {
        FishingHitDataManager.FISHING_PROB[greener][index] = (double) config.get("fishingProb");
        FishingHitDataManager.FISHING_PER_UNIT_MONEY[greener][index] = (long) (double) config.get("unitMoney");
        FishingHitDataManager.BLACK_ROOM_PROB[greener][index] = (double) config.get("blackRoomProb");
        FishingHitDataManager.BLACK_ROOM_LIMIT[greener][index] = (long) (double) config.get("blackRoomLimit");
        FishingHitDataManager.PLAYER_TOTAL_PROB[greener][index][1] = (double) config.get("totalWinProb");
        FishingHitDataManager.PLAYER_TOTAL_LIMIT[greener][index][1] = (long) (double) config.get("totalWinLimit");
        FishingHitDataManager.PLAYER_TOTAL_PROB[greener][index][0] = (double) config.get("totalLoseProb");
        FishingHitDataManager.PLAYER_TOTAL_LIMIT[greener][index][0] = (long) (double) config.get("totalLoseLimit");
        FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][1] = (double) config.get("dailyWinProb");
        FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][1] = (long) (double) config.get("dailyWinLimit");
        FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][0] = (double) config.get("dailyLoseProb");
        FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][0] = (long) (double) config.get("dailyLoseLimit");
        FishingHitDataManager.FISHING_CUT_PROB[greener][index] = (double) config.get("cutProb");

        long initPoolMoney = Math.round((double) config.get("initPoolMoney"));
        if (FishingHitDataManager.FISHING_INIT_POOL[greener][index] != initPoolMoney) {
            FishingHitDataManager.FISHING_POOL[greener][index] = initPoolMoney;
        }
        FishingHitDataManager.FISHING_INIT_POOL[greener][index] = initPoolMoney;
    }

    /**
     * 设置捕鱼配置
     */
    public static Map<String, Object> getFishingConfig(int greener, int index) {
        Map<String, Object> fishConfig = new HashMap<>();
        fishConfig.put("fishingProb", FishingHitDataManager.FISHING_PROB[greener][index]);
        fishConfig.put("unitMoney", FishingHitDataManager.FISHING_PER_UNIT_MONEY[greener][index]);
        fishConfig.put("blackRoomProb", FishingHitDataManager.BLACK_ROOM_PROB[greener][index]);
        fishConfig.put("blackRoomLimit", FishingHitDataManager.BLACK_ROOM_LIMIT[greener][index]);
        fishConfig.put("totalWinProb", FishingHitDataManager.PLAYER_TOTAL_PROB[greener][index][1]);
        fishConfig.put("totalWinLimit", FishingHitDataManager.PLAYER_TOTAL_LIMIT[greener][index][1]);
        fishConfig.put("totalLoseProb", FishingHitDataManager.PLAYER_TOTAL_PROB[greener][index][0]);
        fishConfig.put("totalLoseLimit", FishingHitDataManager.PLAYER_TOTAL_LIMIT[greener][index][0]);
        fishConfig.put("dailyWinProb", FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][1]);
        fishConfig.put("dailyWinLimit", FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][1]);
        fishConfig.put("dailyLoseProb", FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][0]);
        fishConfig.put("dailyLoseLimit", FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][0]);
        fishConfig.put("cutProb", FishingHitDataManager.FISHING_CUT_PROB[greener][index]);
        fishConfig.put("initPoolMoney", FishingHitDataManager.FISHING_INIT_POOL[greener][index]);
        return fishConfig;
    }

    // *****************************************************************************************

    /**
     * 5秒一次储存任务
     */
    @Scheduled(initialDelay = 8000, fixedRate = 5000)
    public void hitDataSaver() {
        RedisHelper.set("Fishing:Setting:Greener:Limit", Integer.toString(GREENER_LIMIT));
        for (Entry<Long, Map<Integer, Long>> totalWinEntry : TOTAL_WIN_MAP.entrySet()) {
            String key = String.format("Fishing:TotalWin:%d", totalWinEntry.getKey());
            RedisHelper.set(key, new Gson().toJson(totalWinEntry.getValue()));
        }
        for (Entry<Long, Map<Integer, Long>> blackRoomEntry : BLACK_ROOM_MAP.entrySet()) {
            String key = String.format("Fishing:BlackRoom:%d", blackRoomEntry.getKey());
            RedisHelper.set(key, new Gson().toJson(blackRoomEntry.getValue()));
        }
        for (Entry<Long, Double> playerFishingProb : PLAYER_FISHING_PROB_MAP.entrySet()) {
            String key = String.format("Fishing:PlayerFishingProb:%d", playerFishingProb.getKey());
            RedisHelper.set(key, Double.toString(playerFishingProb.getValue()));
        }
        // 捕鱼挑战赛参数
        for (Entry<Long, Map<Integer, Long>> totalWinEntry : CHALLENGE_TOTAL_WIN_MAP.entrySet()) {
            String key = String.format("Fishing:TotalWinChallenge:%d", totalWinEntry.getKey());
            RedisHelper.set(key, new Gson().toJson(totalWinEntry.getValue()));
        }
        for (Entry<Long, Map<Integer, Long>> blackRoomEntry : CHALLENGE_BLACK_ROOM_MAP.entrySet()) {
            String key = String.format("Fishing:BlackRoomChallenge:%d", blackRoomEntry.getKey());
            RedisHelper.set(key, new Gson().toJson(blackRoomEntry.getValue()));
        }
//        for (Entry<Long, Double> playerFishingProb : CHALLENGE_PLAYER_FISHING_PROB_MAP.entrySet()) {
//            String key = String.format("Fishing:PlayerFishingProbChallenge:%d", playerFishingProb.getKey());
//            RedisHelper.set(key, Double.toString(playerFishingProb.getValue()));
//        }

        RedisHelper.set("Fishing:Setting:Pool:InitPool", GsonManager.gson.toJson(FISHING_INIT_POOL));
        RedisHelper.set("Fishing:Setting:Pool:Pool", GsonManager.gson.toJson(FISHING_POOL));
        RedisHelper.set("Fishing:Setting:Pool:Prob", GsonManager.gson.toJson(FISHING_PROB));
        RedisHelper.set("Fishing:Setting:Pool:UnitProb", GsonManager.gson.toJson(FISHING_UNIT_PROB));
        RedisHelper.set("Fishing:Setting:Pool:PerUnitMoney", GsonManager.gson.toJson(FISHING_PER_UNIT_MONEY));
        RedisHelper.set("Fishing:Setting:Daily:Prob", GsonManager.gson.toJson(PLAYER_DAILY_PROB));
        RedisHelper.set("Fishing:Setting:Daily:Limit", GsonManager.gson.toJson(PLAYER_DAILY_LIMIT));
        RedisHelper.set("Fishing:Setting:Total:Prob", GsonManager.gson.toJson(PLAYER_TOTAL_PROB));
        RedisHelper.set("Fishing:Setting:Total:Limit", GsonManager.gson.toJson(PLAYER_TOTAL_LIMIT));
        RedisHelper.set("Fishing:Setting:BlackRoom:Prob", GsonManager.gson.toJson(BLACK_ROOM_PROB));
        RedisHelper.set("Fishing:Setting:BlackRoom:Limit", GsonManager.gson.toJson(BLACK_ROOM_LIMIT));
    }

    /**
     * 每天0点一次清空任务
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void zeroTimeWorker() {
        logger.info("执行零点捕鱼数据清空任务");
        DAILY_WIN_MAP = new ConcurrentHashMap<>();
        TOTAL_WIN_MAP = new ConcurrentHashMap<>();
        BLACK_ROOM_MAP = new ConcurrentHashMap<>();
        PLAYER_FISHING_PROB_MAP = new ConcurrentHashMap<>();
        // 捕鱼挑战赛
        CHALLENGE_DAILY_WIN_MAP = new ConcurrentHashMap<>();
        CHALLENGE_TOTAL_WIN_MAP = new ConcurrentHashMap<>();
        CHALLENGE_BLACK_ROOM_MAP = new ConcurrentHashMap<>();
//        CHALLENGE_PLAYER_FISHING_PROB_MAP = new ConcurrentHashMap<>();
    }

    // ******************************** 挑战赛相关参数操作 ********************************

    /**
     * 捕鱼挑战赛每日赢取金币表
     */
    private static Map<Long, Map<Integer, Long>> CHALLENGE_DAILY_WIN_MAP = new ConcurrentHashMap<>();

    /**
     * 捕鱼挑战赛总赢取金币表
     */
    private static Map<Long, Map<Integer, Long>> CHALLENGE_TOTAL_WIN_MAP = new ConcurrentHashMap<>();

    /**
     * 捕鱼挑战赛小黑屋玩家表
     */
    private static Map<Long, Map<Integer, Long>> CHALLENGE_BLACK_ROOM_MAP = new ConcurrentHashMap<>();

//    /**
//     * 捕鱼挑战赛玩家系数表
//     */
//    private static Map<Long, Double> CHALLENGE_PLAYER_FISHING_PROB_MAP = new ConcurrentHashMap<>();

    /**
     * 获取捕鱼挑战赛玩家日常输赢表
     */
    public static Map<Integer, Long> getChallengePlayerDailyMap(long playerId) {
        if (!CHALLENGE_DAILY_WIN_MAP.containsKey(playerId)) {
            CHALLENGE_DAILY_WIN_MAP.put(playerId, new HashMap<>());
        }
        return CHALLENGE_DAILY_WIN_MAP.get(playerId);
    }

    /**
     * 获取捕鱼挑战赛玩家总输赢表
     */
    public static Map<Integer, Long> getChallengePlayerTotalMap(long playerId) {
        if (!CHALLENGE_TOTAL_WIN_MAP.containsKey(playerId)) {
            String key = String.format("Fishing:TotalWinChallenge:%d", playerId);
            String value = RedisHelper.get(key);
            if (!StringUtils.isEmpty(value)) {
                Map<Integer, Long> blackMap = new Gson().fromJson(value, getType());
                CHALLENGE_TOTAL_WIN_MAP.put(playerId, blackMap);
            } else {
                CHALLENGE_TOTAL_WIN_MAP.put(playerId, new HashMap<>());
            }
        }
        return CHALLENGE_TOTAL_WIN_MAP.get(playerId);
    }

    /**
     * 获取捕鱼挑战赛玩家小黑屋表
     */
    public static Map<Integer, Long> getChallengePlayerBlackMap(long playerId) {
        if (!CHALLENGE_BLACK_ROOM_MAP.containsKey(playerId)) {
            String key = String.format("Fishing:BlackRoomChallenge:%d", playerId);
            String value = RedisHelper.get(key);
            if (!StringUtils.isEmpty(value)) {
                Map<Integer, Long> blackMap = new Gson().fromJson(value, getType());
                CHALLENGE_BLACK_ROOM_MAP.put(playerId, blackMap);
            } else {
                CHALLENGE_BLACK_ROOM_MAP.put(playerId, new HashMap<>());
            }
        }
        return CHALLENGE_BLACK_ROOM_MAP.get(playerId);
    }


    /**
     * 获取捕鱼挑战赛每日输赢金币
     */
    public static long getChallengeDailyWin(long playerId, int index) {
        return getChallengePlayerDailyMap(playerId).getOrDefault(index, 0L);
    }

    /**
     * 增加捕鱼挑战赛每日输赢金币
     */
    public static void addChallengeDailyWin(long playerId, int index, long addMoney) {
        Map<Integer, Long> dailyMap = getChallengePlayerDailyMap(playerId);
        synchronized (dailyMap) {
            dailyMap.put(index, dailyMap.getOrDefault(index, 0L) + addMoney);
        }
    }

    /**
     * 获取捕鱼挑战赛小黑屋金币
     */
    public static long getChallengeBlackRoom(long playerId, int index) {
        return getChallengePlayerBlackMap(playerId).getOrDefault(index, 0L);
    }

    /**
     * 增加捕鱼挑战赛小黑屋金币
     */
    public static void addChallengeBlackRoom(long playerId, int index, long addMoney) {
        Map<Integer, Long> blackMap = getChallengePlayerBlackMap(playerId);
        // synchronized (blackMap) {
        // long black = blackMap.getOrDefault(index, 0L) + addMoney;
        // black = Math.max(0L, black);
        // blackMap.put(index, black);
        // }
        blackMap.put(index, blackMap.getOrDefault(index, 0L));
    }

    /**
     * 获取捕鱼挑战赛总输赢金币
     */
    public static long getChallengeTotalWin(long playerId, int index) {
        return getChallengePlayerTotalMap(playerId).getOrDefault(index, 0L);
    }

    /**
     * 增加捕鱼挑战赛总输赢金币
     */
    public static void addChallengeTotalWin(long playerId, int index, long addMoney) {
        Map<Integer, Long> totalMap = getChallengePlayerTotalMap(playerId);
        synchronized (totalMap) {
            totalMap.put(index, totalMap.getOrDefault(index, 0L) + addMoney);
        }
    }

    /**
     * 增加捕鱼挑战赛输赢金币
     */
    public static void addChallengeWin(FishingChallengeRoom gameRoom, FishingChallengePlayer player, long addMoney) {
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> {
            long money = addMoney;
            int index = gameRoom.getRoomIndex() - 1;
//            logger.info("-index:" + index);
            if(gameRoom.isVip()) {
                index = 6;
            } else if (gameRoom.getCode() % 2 == 0) {
                index = 4;
            } else if (gameRoom.getCode() % 2 == 1) {
                index = 5;
            }
//            logger.info("->index:" + index + " " + money);
            addChallengeDailyWin(player.getId(), index, money); // 每日输赢
            addChallengeTotalWin(player.getId(), index, money); // 总输赢
            player.setChangeMoney(player.getChangeMoney() + money);
            if (money < 0) {
                player.setSpendMoney(player.getSpendMoney() + money);
            } else {
                player.setWinMoney(player.getWinMoney() + money);
            }

            int greener = 1; // 挑战赛不区分新老玩家
            if (money < 0) { // 发射子弹消耗
                long cutMoney = (long) (money * (FISHING_CUT_PROB[greener][index] / 100D));
                money -= cutMoney;
                // 设置总抽水金币
                player.setCutMoney(player.getCutMoney() - cutMoney);
            }
//            logger.info("---> " + money);
            synchronized (FISHING_POOL) {
                // 服务器库存
                FISHING_POOL[greener][index] -= money;
            }
        }, 0, TimeUnit.SECONDS);
    }

//    /**
//     * 获取捕鱼挑战赛玩家捕鱼系数
//     */
//    public static double getChallengePlayerFishingProb(long playerId) {
//        if (CHALLENGE_PLAYER_FISHING_PROB_MAP.containsKey(playerId)) {
//            return CHALLENGE_PLAYER_FISHING_PROB_MAP.get(playerId);
//        }
//
//        String key = String.format("Fishing:PlayerFishingProbChallenge:%d", playerId);
//        String value = RedisHelper.get(key);
//        if (!StringUtils.isEmpty(value)) {
//            double result = Double.parseDouble(value);
//            CHALLENGE_PLAYER_FISHING_PROB_MAP.put(playerId, result);
//            return result;
//        }
//
//        CHALLENGE_PLAYER_FISHING_PROB_MAP.put(playerId, 0D);
//        return 0;
//    }
}
