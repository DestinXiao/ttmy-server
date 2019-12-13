package com.maple.game.osee.controller.gm;

import com.google.gson.Gson;
import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.mapper.UserMapper;
import com.maple.engine.anotation.GmController;
import com.maple.engine.anotation.GmHandler;
import com.maple.engine.manager.GsonManager;
import com.maple.game.osee.common.RedisUtil;
import com.maple.game.osee.controller.gm.base.GmBaseController;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.dao.log.entity.AppGameLogEntity;
import com.maple.game.osee.dao.log.entity.AppRankLogEntity;
import com.maple.game.osee.dao.log.mapper.*;
import com.maple.game.osee.entity.GameEnum;
import com.maple.game.osee.entity.fightten.FightTenRobotPlayer;
import com.maple.game.osee.entity.fightten.FightTenRoom;
import com.maple.game.osee.entity.fightten.challenge.FightTenChallengeRoom;
import com.maple.game.osee.entity.fishing.FishingGameRoom;
import com.maple.game.osee.entity.fishing.challenge.FishingChallengeRoom;
import com.maple.game.osee.entity.fruitlaba.FruitlabaPlayer;
import com.maple.game.osee.entity.gm.CommonResponse;
import com.maple.game.osee.entity.gobang.GobangGameRoom;
import com.maple.game.osee.entity.robot.fishing.FishingGameRobot;
import com.maple.game.osee.entity.two_eight.TwoEightConfig;
import com.maple.game.osee.entity.two_eight.TwoEightRoom;
import com.maple.game.osee.manager.AgentManager;
import com.maple.game.osee.manager.fightten.FightTenManager;
import com.maple.game.osee.manager.fishing.FishingGrandPrixManager;
import com.maple.game.osee.manager.fishing.FishingHitDataManager;
import com.maple.game.osee.manager.fishing.FishingManager;
import com.maple.game.osee.manager.fishing.FishingRobotManager;
import com.maple.game.osee.manager.fishing.util.FishingUtil;
import com.maple.game.osee.manager.fruitlaba.FruitLaBaManager;
import com.maple.game.osee.manager.two_eight.TwoEightManager;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.data.BaseGameRoom;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 后台游戏设置控制器
 */
@GmController
public class GmGameConfigController extends GmBaseController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OseePlayerMapper playerMapper;

    @Autowired
    private FishingRobotManager fishingRobotManager;

    @Autowired
    private OseeCutMoneyLogMapper cutMoneyLogMapper;

    @Autowired
    private OseeExpendLogMapper expendLogMapper;

    @Autowired
    private AppRankLogMapper rankLogMapper;

    @Autowired
    private AppGameLogMapper gameLogMapper;

    @Autowired
    private OseeFruitRecordLogMapper fruitRecordLogMapper;

    /**
     * 获取服务器ai设置
     */
    @SuppressWarnings("unchecked")
    @GmHandler(key = "/osee/game/ai/info")
    public void doGameAiInfoTask(Map<String, Object> params, CommonResponse response) {
        Map<String, Object> resultMap = (Map<String, Object>) response.getData();

        Map<String, Object> fishConfig = new HashMap<>();
        fishConfig.put("useRobot", fishingRobotManager.USE_ROBOT);
        fishConfig.put("robotCount", fishingRobotManager.ROBOT_COUNT);
        fishConfig.put("minRefresh", fishingRobotManager.REFRESH_TIME[0]);
        fishConfig.put("maxRefresh", fishingRobotManager.REFRESH_TIME[1]);
        fishConfig.put("minDisappear", fishingRobotManager.DISAPPEAR_TIME[0]);
        fishConfig.put("maxDisappear", fishingRobotManager.DISAPPEAR_TIME[1]);
        resultMap.put("fishing", fishConfig);

        // 拼十机器人设置信息
        resultMap.put("fightten", FightTenManager.getRobotConfig());
        resultMap.put("tenRobotTotalWinMoney", FightTenManager.getRobotWinMoney());
        resultMap.put("tenRobotTotalLoseMoney", -FightTenManager.getRobotLoseMoney());

        // 二八杠机器人信息
        Map<String, Object> erbaConfig = new HashMap<>();

        erbaConfig.put("useRobot", 1);
        //盈利金额
        erbaConfig.put("TwoEightRobotCurrentWinMoney", TwoEightManager.getTERobotMoney(TwoEightConfig.RedisTwoEightRobotWinKey));
        //历史总盈利盈利金额
        erbaConfig.put("TwoEightRobotHistoryWinMoney", TwoEightManager.getTERobotMoney(TwoEightConfig.RedisTwoEightRobotHistoryWinKey));
        //未达到盈利发牌概率
        erbaConfig.put("toWinFirstCardProbably", TwoEightConfig.toWinFirstCardProbably);//最大牌型概率
        erbaConfig.put("toWinSecondCardProbably", TwoEightConfig.toWinSecondCardProbably);//第二大牌型概率
        erbaConfig.put("toWinThirdCardProbably", TwoEightConfig.toWinThirdCardProbably);//第三大牌型概率
        erbaConfig.put("toWinLastCardProbably", TwoEightConfig.toWinLastCardProbably);//最小牌型概率

        //达到盈利后发牌概率
        erbaConfig.put("toLoseFirstCardProbably", TwoEightConfig.toLoseFirstCardProbably);//最大牌型概率
        erbaConfig.put("toLoseSecondCardProbably", TwoEightConfig.toLoseSecondCardProbably);//第二大牌型概率
        erbaConfig.put("toLoseThirdCardProbably", TwoEightConfig.toLoseThirdCardProbably);//第三大牌型概率
        erbaConfig.put("toLoseLastCardProbably", TwoEightConfig.toLoseLastCardProbably);//最小牌型概率

        erbaConfig.put("RobotMinMoney", TwoEightConfig.robotMinMoney);
        resultMap.put("erba", erbaConfig);
    }

    /**
     * 修改捕鱼ai设置
     */
    @GmHandler(key = "/osee/game/ai/fishing/update")
    public void doGameAiFishingUpdateTask(Map<String, Object> params, CommonResponse response) {
        fishingRobotManager.USE_ROBOT = (int) (double) params.get("useRobot");
        fishingRobotManager.ROBOT_COUNT = (int) (double) params.get("robotCount");

        int minRefresh = (int) (double) params.get("minRefresh");
        int maxRefresh = (int) (double) params.get("maxRefresh");
        int minDisappear = (int) (double) params.get("minDisappear");
        int maxDisappear = (int) (double) params.get("maxDisappear");
        if (maxRefresh < minRefresh || maxDisappear < minDisappear) {
            response.setSuccess(false);
            response.setErrMsg("数据不正确");
            return;
        }
        fishingRobotManager.REFRESH_TIME[0] = minRefresh;
        fishingRobotManager.REFRESH_TIME[1] = maxRefresh;
        fishingRobotManager.DISAPPEAR_TIME[0] = minDisappear;
        fishingRobotManager.DISAPPEAR_TIME[1] = maxDisappear;

        RedisHelper.set("Fishing:Robot:UseRobot", String.valueOf(fishingRobotManager.USE_ROBOT));
        RedisHelper.set("Fishing:Robot:RobotCount", String.valueOf(fishingRobotManager.ROBOT_COUNT));
        RedisHelper.set("Fishing:Robot:RefreshTime", GsonManager.gson.toJson(fishingRobotManager.REFRESH_TIME));
        RedisHelper.set("Fishing:Robot:DisappearTime", GsonManager.gson.toJson(fishingRobotManager.DISAPPEAR_TIME));
    }

    /**
     * 修改二八ai设置
     */
    @GmHandler(key = "/osee/game/ai/twoeight/update")
    public void doGameAiTwoEightUpdateTask(Map<String, Object> params, CommonResponse response) {
        //盈利金额
        TwoEightConfig.robotMinMoney = (long) (double) params.get("robotMinMoney");
        //盈利概率
        TwoEightConfig.toWinFirstCardProbably = (double) params.get("toWinFirstCardProbably");
        TwoEightConfig.toWinSecondCardProbably = (double) params.get("toWinSecondCardProbably");
        TwoEightConfig.toWinThirdCardProbably = (double) params.get("toWinThirdCardProbably");
        TwoEightConfig.toWinLastCardProbably = (double) params.get("toWinLastCardProbably");

        //输钱概率
        TwoEightConfig.toLoseFirstCardProbably = (double) params.get("toLoseFirstCardProbably");
        TwoEightConfig.toLoseSecondCardProbably = (double) params.get("toLoseSecondCardProbably");
        TwoEightConfig.toLoseThirdCardProbably = (double) params.get("toLoseThirdCardProbably");
        TwoEightConfig.toLoseLastCardProbably = (double) params.get("toLoseLastCardProbably");
    }

    /**
     * 机器人金币记录清空
     */
    @GmHandler(key = "/osee/twoeight/robot/money/reset")
    public void resetRobotMoney(Map<String, Object> params, CommonResponse response) {
        RedisHelper.set(TwoEightConfig.RedisTwoEightRobotWinKey, "0");
    }

    /**
     * 获取服务器统计记录
     */
    @SuppressWarnings("unchecked")
    @GmHandler(key = "/osee/server/statistics")
    public void doServerStatisticsTask(Map<String, Object> params, CommonResponse response) {
        Map<String, Object> resultMap = (Map<String, Object>) response.getData();

        Map<String, Object> statisticsMap = playerMapper.getGmStatistics();
        resultMap.put("playerMoney", statisticsMap.get("money")); // 全服携带金币统计
        resultMap.put("bankMoney", statisticsMap.get("bankMoney")); // 全服保险箱金币统计

        long fishPoolAll = 0;//累加计算全服捕鱼库存
        for (long[] pools : FishingHitDataManager.FISHING_POOL) {
            // 除去龙晶战场的库存
            for (int i = 0; i < pools.length - 3; i++) {
                long pool = pools[i];
                fishPoolAll += pool;
            }
        }
        // 捕鱼抽水总额
        long fishCutAll = cutMoneyLogMapper.getTotalCutMoney(GameEnum.FISHING.getId());

        // 龙晶战场的库存总额
        long fishChallengePoolAll = 0;
        for (long[] pools : FishingHitDataManager.FISHING_POOL) {
            // 除去龙晶战场的库存
            for (int i = 4; i < pools.length; i++) {
                long pool = pools[i];
                fishChallengePoolAll += pool;
            }
        }
        // 龙晶战场总抽水值
        // long fishChallengeCutAll = cutMoneyLogMapper.getTotalCutMoney(GameEnum.FISHING_CHALLENGE.getId());

        // 水果拉霸抽水总额
        long fruitCutMoney = cutMoneyLogMapper.getTotalCutMoney(GameEnum.FRUIT_LABA.getId());

        // 水果拉霸总库存
        long fruitPool1 = FruitLaBaManager.getAllPoolGold(1);
        long fruitPool2 = FruitLaBaManager.getAllPoolGold(2);
        long fruitPool3 = FruitLaBaManager.getAllPoolGold(3);
        long poolMoney = fruitPool1 + fruitPool2 + fruitPool3 + fishPoolAll;

        resultMap.put("nowTime", System.currentTimeMillis());//当前时间
        resultMap.put("poolMoney", poolMoney);//全服金币库存总额 （捕鱼）
        resultMap.put("expendMoney", expendLogMapper.getTotalExpendMoney()); // 支出总数
        resultMap.put("cutMoney", fishCutAll
                + cutMoneyLogMapper.getTotalCutMoney(GameEnum.ERBA_GAME.getId())
                + cutMoneyLogMapper.getTotalCutMoney(GameEnum.GOBANG.getId())); // 金币抽水总数(捕鱼+二八杠+五子棋)
        resultMap.put("totalPlayer", userMapper.getUserCount()); // 注册人数
        resultMap.put("onlinePlayer", userMapper.getOnlineUserCount()); // 在线人数

        //捕鱼库存
        resultMap.put("fishPoolAll", fishPoolAll); // 捕鱼全服库存统计
        //捕鱼抽水总额
        resultMap.put("fishCutAll", fishCutAll);
        List<Object> fishPools = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int index = i % 4;
            int greener = i / 4;
            Map<String, Object> fishPool = new HashMap<>();
            fishPool.put("currentPool", FishingHitDataManager.FISHING_POOL[greener][index]);
            fishPools.add(fishPool);
        }
        resultMap.put("fishPool", fishPools);

        //水果拉霸 ljy
        resultMap.put("fruitPoolMoney1", fruitPool1);//水果拉霸库存总和
        resultMap.put("fruitCutMoney1", fruitRecordLogMapper.getTotalCutMoney(5,1));//水果拉霸抽水总额

        resultMap.put("fruitPoolMoney2", fruitPool2);//水果拉霸库存总和
        resultMap.put("fruitCutMoney2", fruitRecordLogMapper.getTotalCutMoney(5,2));//水果拉霸抽水总额

        resultMap.put("fruitPoolMoney3", fruitPool3);//水果拉霸库存总和
        resultMap.put("fruitCutMoney3", fruitRecordLogMapper.getTotalCutMoney(5,3));//水果拉霸抽水总额

        // 捕鱼鱼雷掉落/使用数据
        resultMap.putAll(FishingManager.TORPEDO_RECORD);

        // 龙晶战场库存总额
        resultMap.put("fishChallengePoolAll", fishChallengePoolAll);
        resultMap.put("fishChallengePool1", FishingHitDataManager.FISHING_POOL[1][4]);
        resultMap.put("fishChallengePool2", FishingHitDataManager.FISHING_POOL[1][5]);
        resultMap.put("fishChallengePool3", FishingHitDataManager.FISHING_POOL[1][6]);
        // 龙晶战场抽水总额
        long fishChallengeCut1 = fruitRecordLogMapper.getTotalCutMoney(7, 7);
        long fishChallengeCut2 = fruitRecordLogMapper.getTotalCutMoney(8, 8);
        long fishChallengeCut3 = fruitRecordLogMapper.getTotalCutMoney(9, 9);
        resultMap.put("fishChallengeCut1", fishChallengeCut1);
        resultMap.put("fishChallengeCut2", fishChallengeCut2);
        resultMap.put("fishChallengeCut3", fishChallengeCut3);
        resultMap.put("fishChallengeCutAll", fishChallengeCut1 + fishChallengeCut2 + fishChallengeCut3);
        //水果拉霸

       // resultMap.put("fruitCutMoney", fruitCutMoney);//水果拉霸抽水总额

        List<Object> fruitPoolMoneys = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            fruitPoolMoneys.add(FruitLaBaManager.getLineContollerGold(i));
        }
        resultMap.put("fruitPoolMoneyLine", fruitPoolMoneys);
        //分别统计各处在线人数
        for (BaseGameRoom gameRoom : GameContainer.getGameRooms()) {
            if (gameRoom instanceof FishingGameRoom) { // 捕鱼房间
                String key = "fishingOnline_" + ((FishingGameRoom) gameRoom).getRoomIndex();

                for (BaseGamePlayer player : gameRoom.getGamePlayers()) {
                    if (player != null && player.getUser().isOnline() && !(player instanceof FishingGameRobot)) {
                        resultMap.put(key, ((int) resultMap.getOrDefault(key, 0)) + 1);
                    }
                }
            } else if (gameRoom instanceof FishingChallengeRoom) { // 捕鱼挑战赛房间人数
                String key = "fishingChallengeOnline";
                for (BaseGamePlayer player : gameRoom.getGamePlayers()) {
                    if (player != null && player.getUser().isOnline()) {
                        resultMap.put(key, ((int) resultMap.getOrDefault(key, 0)) + 1);
                    }
                }
            } else if (gameRoom instanceof FightTenChallengeRoom) { // 拼十挑战赛房间人数
                String key = "fightTenChallengeOnline";
                for (BaseGamePlayer player : gameRoom.getGamePlayers()) {
                    if (player != null && player.getUser().isOnline()) {
                        resultMap.put(key, ((int) resultMap.getOrDefault(key, 0)) + 1);
                    }
                }
            } else if (gameRoom instanceof FightTenRoom) { // 拼十房间
                String key = "fightTenOnline_" + (((FightTenRoom) gameRoom).getFieldType() + 1);

                for (BaseGamePlayer player : gameRoom.getGamePlayers()) {
                    if (player != null && player.getUser().isOnline() && !(player instanceof FightTenRobotPlayer)) {
                        // 房间在线玩家
                        resultMap.put(key, ((int) resultMap.getOrDefault(key, 0)) + 1);
                    }
                }
            } else if (gameRoom instanceof GobangGameRoom) { // 五子棋
                String key = "goBangOnline";
                resultMap.put(key, (int) resultMap.getOrDefault(key, 0) + gameRoom.getPlayerSize());
            } else if (gameRoom instanceof TwoEightRoom) { // 二八杠
                String key = "erbaOnline";
                for (BaseGamePlayer player : gameRoom.getGamePlayers()) {
                    if (player != null && player.getUser().isOnline()) {
                        // 二八杠的在线人数
                        resultMap.put(key, (int) resultMap.getOrDefault(key, 0) + 1);
                    }
                }
            }
        }
        // 水果拉霸在线人数
        int labaOnline = FruitLaBaManager.fruitRoomUser.size();
        resultMap.put("labaOnline", labaOnline);

        int labaOnline1 = 0;
        int labaOnline2 = 0;
        int labaOnline3 = 0;
        for (Map.Entry<Long, FruitlabaPlayer> entry : FruitLaBaManager.fruitplayers.entrySet()) {
            if(entry.getValue().getRoomType() == 1) labaOnline1++;
            else if(entry.getValue().getRoomType() == 2) labaOnline2++;
            else if(entry.getValue().getRoomType() == 3) labaOnline3++;
        }
        resultMap.put("labaOnline1", labaOnline1);
        resultMap.put("labaOnline2", labaOnline2);
        resultMap.put("labaOnline3", labaOnline3);
        resultMap.put("rewardTotal1", FruitLaBaManager.rewardTotal1);
        resultMap.put("rewardTotal2", FruitLaBaManager.rewardTotal2);
        resultMap.put("rewardTotal3", FruitLaBaManager.rewardTotal3);

        //当前金币：
        resultMap.put("erbaDailyMoney", TwoEightManager.getTERobotMoney(TwoEightConfig.RedisTwoEightDailyMoney));

        //当日总金币：
        long allMoney = TwoEightManager.getTERobotMoney(TwoEightConfig.RedisTwoEightDailyMoney);
        //TODO 加上其他游戏金币
    }

    /**
     * 获取服务器监控数据
     */
    @SuppressWarnings("unchecked")
    @GmHandler(key = "/osee/server/monitor")
    public void doServerMonitorTask(Map<String, Object> params, CommonResponse response) {
        Map<String, Object> resultMap = (Map<String, Object>) response.getData();
        resultMap.put("dailyExpend", expendLogMapper.getTodayExpendMoney());
        resultMap.put("dailyCutMoney", cutMoneyLogMapper.getTodayCutMoney(GameEnum.FISHING.getId()));
        long fishChallengeCut1 = fruitRecordLogMapper.getTotalCutMoney(7, 7);
        long fishChallengeCut2 = fruitRecordLogMapper.getTotalCutMoney(8, 8);
        long fishChallengeCut3 = fruitRecordLogMapper.getTotalCutMoney(9, 9);
        resultMap.put("dailyCutCrystal", fishChallengeCut1 + fishChallengeCut2 + fishChallengeCut3);
        resultMap.put("nowTime", System.currentTimeMillis());

        List<Object> monitors = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int index = i % 4;
            int greener = i / 4;

            Map<String, Object> monitor = new HashMap<>();
            monitor.put("initPool", FishingHitDataManager.FISHING_INIT_POOL[greener][index]);
            monitor.put("currentPool", FishingHitDataManager.FISHING_POOL[greener][index]);
            monitor.put("fishingProb", String.format("%.2f", FishingHitDataManager.getServerProb(greener, index)));
            monitors.add(monitor);
        }

        for(int index = 4; index < 7; index++) {
            // 捕鱼挑战赛
            Map<String, Object> monitor = new HashMap<>();
            monitor.put("initPool", FishingHitDataManager.FISHING_INIT_POOL[1][index]);
            monitor.put("currentPool", FishingHitDataManager.FISHING_POOL[1][index]);
            monitor.put("fishingProb", String.format("%.2f", FishingHitDataManager.getServerProb(1, index)));
            monitors.add(monitor);
        }

        // 大奖赛
        Map<String, Object> monitor = new HashMap<>();
        monitor.put("initPool", FishingGrandPrixManager.initPool);
        monitor.put("currentPool", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, 0L));
        Double val = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_AP_KEY, 0.0D);
        Long val1 = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, 0L);
        Long val2 = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_APT_KEY, 100L);

        monitor.put("fishingProb", String.format("%.2f", val + (val1 / val2) * 0.01));
        monitors.add(monitor);

        resultMap.put("monitors", monitors);
    }

    /**
     * 获取服务器游戏设置
     */
    @GmHandler(key = "/osee/game/config/info")
    public void doGameConfigInfoTask(Map<String, Object> params, CommonResponse response) {
        Map<String, Object> resultMap = new HashMap<>();
        FishingManager.getTorpedoDropRate();
        resultMap.put("torpedoDropFreeRate", FishingManager.TORPEDO_DROP_FREE_RATE);
        resultMap.put("torpedoDropPerPayMoney", FishingManager.TORPEDO_DROP_PER_PAY_MONEY);
        resultMap.put("torpedoDropPerPayRate", FishingManager.TORPEDO_DROP_PER_PAY_RATE);

        // 大奖赛
        resultMap.put("challengeDropFreeRate", FishingManager.CHALLENGE_DROP_FREE_RATE);
        resultMap.put("challengeDropPerPayMoney", FishingManager.CHALLENGE_DROP_PER_PAY_MONEY);
        resultMap.put("challengeDropPerPayRate", FishingManager.CHALLENGE_DROP_PER_PAY_RATE);

        resultMap.put("fishingGreener", FishingHitDataManager.GREENER_LIMIT);
        List<Object> fishConfigList = new ArrayList<>(6);

        resultMap.put("q0", FishingUtil.q0);
        resultMap.put("ap", FishingUtil.ap);
        resultMap.put("apt", FishingUtil.apt);
        for (int i = 0; i < 8; i++) {
            int index = i % 4;
            int greener = i / 4;

//            Map<String, Object> fishConfig = new HashMap<>();
//            fishConfig.put("fishingProb", FishingHitDataManager.FISHING_PROB[greener][index]);
//            fishConfig.put("unitMoney", FishingHitDataManager.FISHING_PER_UNIT_MONEY[greener][index]);
//            fishConfig.put("blackRoomProb", FishingHitDataManager.BLACK_ROOM_PROB[greener][index]);
//            fishConfig.put("blackRoomLimit", FishingHitDataManager.BLACK_ROOM_LIMIT[greener][index]);
//            fishConfig.put("totalWinProb", FishingHitDataManager.PLAYER_TOTAL_PROB[greener][index][1]);
//            fishConfig.put("totalWinLimit", FishingHitDataManager.PLAYER_TOTAL_LIMIT[greener][index][1]);
//            fishConfig.put("totalLoseProb", FishingHitDataManager.PLAYER_TOTAL_PROB[greener][index][0]);
//            fishConfig.put("totalLoseLimit", FishingHitDataManager.PLAYER_TOTAL_LIMIT[greener][index][0]);
//            fishConfig.put("dailyWinProb", FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][1]);
//            fishConfig.put("dailyWinLimit", FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][1]);
//            fishConfig.put("dailyLoseProb", FishingHitDataManager.PLAYER_DAILY_PROB[greener][index][0]);
//            fishConfig.put("dailyLoseLimit", FishingHitDataManager.PLAYER_DAILY_LIMIT[greener][index][0]);
//            fishConfig.put("cutProb", FishingHitDataManager.FISHING_CUT_PROB[greener][index]);
//            fishConfig.put("initPoolMoney", FishingHitDataManager.FISHING_INIT_POOL[greener][index]);
//            fishConfigList.add(fishConfig);
            fishConfigList.add(FishingHitDataManager.getFishingConfig(greener, index));
        }
        // 捕鱼挑战赛配置
        fishConfigList.add(FishingHitDataManager.getFishingConfig(1, 4));
        fishConfigList.add(FishingHitDataManager.getFishingConfig(1, 5));
        fishConfigList.add(FishingHitDataManager.getFishingConfig(1, 6));

        resultMap.put("fishing", fishConfigList);

        // 拼十游戏设置
        resultMap.put("fightten", FightTenManager.getFieldConfigList());

        // 二八杠抽水百分点(1-100)
        resultMap.put("erbaDrawPercent", 1);

        // 水果拉霸抽水百分点(1-100)
        resultMap.put("labaDrawPercent1", FruitLaBaManager.drawPercent1);
        resultMap.put("labaDrawPercent2", FruitLaBaManager.drawPercent2);
        resultMap.put("labaDrawPercent3", FruitLaBaManager.drawPercent3);

        // 水果拉霸入场最低金币限制
        resultMap.put("enterMoneyLimit1", FruitLaBaManager.enterMoneyLimit1);
        resultMap.put("enterMoneyLimit2", FruitLaBaManager.enterMoneyLimit2);
        resultMap.put("enterMoneyLimit3", FruitLaBaManager.enterMoneyLimit3);

        //水果拉霸概率设置
        resultMap.put("lineRate", FruitLaBaManager.lineRate); // 线条概率
        resultMap.put("multipleLevel", FruitLaBaManager.multipleLevel); // 线条概率
        resultMap.put("linePoolGold", FruitLaBaManager.linePoolGold); // 每条线的库存 1-9
        resultMap.put("activeRate", AgentManager.activeRate);

        resultMap.put("drawPercent1", FruitLaBaManager.drawPercent1);
        resultMap.put("fruitLabaBlackRoomTP1", RedisHelper.get(FruitLaBaManager.BLACK_ROOM_TP_KEY + "1"));
        resultMap.put("fruitLabaBlackRoomEP1", RedisHelper.get(FruitLaBaManager.BLACK_ROOM_EP_KEY + "1"));
        resultMap.put("fruitLabaBlackRoomBP1", RedisHelper.get(FruitLaBaManager.BLACK_ROOM_BP_KEY + "1"));
        resultMap.put("fruitLabaStockK01", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K0_KEY + "1"));
        resultMap.put("fruitLabaStockK11", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K1_KEY + "1"));
        resultMap.put("fruitLabaStockK21", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K2_KEY + "1"));
        resultMap.put("fruitLabaStockK31", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K3_KEY + "1"));
        resultMap.put("fruitLabaStockK41", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K4_KEY + "1"));
        resultMap.put("fruitLabaPrizeDXJ1", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_DXJ_KEY + "1"));
        resultMap.put("fruitLabaPrizeLXJ1", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_LXJ_KEY + "1"));
        resultMap.put("fruitLabaPrizeFLXJ1", RedisHelper.get(FruitLaBaManager.REDIS_PLAYER_FLXJ_KEY + "1"));
        resultMap.put("fruitLabaPrizeFDXJ1", RedisHelper.get(FruitLaBaManager.REDIS_PLAYER_FDXJ_KEY + "1"));
        resultMap.put("fruitLabaPrizeXS1", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_XS_KEY + "1"));
        resultMap.put("fruitLabaPrizeFXS1", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_FXS_KEY + "1"));
        resultMap.put("fruitLabaPrizeDX1", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_DX_KEY + "1"));
        resultMap.put("fruitLabaPrizeLX1", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_LX_KEY + "1"));
        resultMap.put("fruitLabaPrizeLW1", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_LW_KEY + "1"));
        resultMap.put("fruitLabaPrizeDW1", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_DW_KEY + "1"));
        resultMap.put("percent1", RedisHelper.get(FruitLaBaManager.REDIS_LABA_PERCENT_KEY + "1"));

        resultMap.put("drawPercent2", FruitLaBaManager.drawPercent2);
        resultMap.put("fruitLabaBlackRoomTP2", RedisHelper.get(FruitLaBaManager.BLACK_ROOM_TP_KEY + "2"));
        resultMap.put("fruitLabaBlackRoomEP2", RedisHelper.get(FruitLaBaManager.BLACK_ROOM_EP_KEY + "2"));
        resultMap.put("fruitLabaBlackRoomBP2", RedisHelper.get(FruitLaBaManager.BLACK_ROOM_BP_KEY + "2"));
        resultMap.put("fruitLabaStockK02", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K0_KEY + "2"));
        resultMap.put("fruitLabaStockK12", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K1_KEY + "2"));
        resultMap.put("fruitLabaStockK22", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K2_KEY + "2"));
        resultMap.put("fruitLabaStockK32", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K3_KEY + "2"));
        resultMap.put("fruitLabaStockK42", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K4_KEY + "2"));
        resultMap.put("fruitLabaPrizeDXJ2", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_DXJ_KEY + "2"));
        resultMap.put("fruitLabaPrizeLXJ2", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_LXJ_KEY + "2"));
        resultMap.put("fruitLabaPrizeFLXJ2", RedisHelper.get(FruitLaBaManager.REDIS_PLAYER_FLXJ_KEY + "2"));
        resultMap.put("fruitLabaPrizeFDXJ2", RedisHelper.get(FruitLaBaManager.REDIS_PLAYER_FDXJ_KEY + "2"));
        resultMap.put("fruitLabaPrizeXS2", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_XS_KEY + "2"));
        resultMap.put("fruitLabaPrizeFXS2", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_FXS_KEY + "2"));
        resultMap.put("fruitLabaPrizeDX2", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_DX_KEY + "2"));
        resultMap.put("fruitLabaPrizeLX2", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_LX_KEY + "2"));
        resultMap.put("fruitLabaPrizeLW2", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_LW_KEY + "2"));
        resultMap.put("fruitLabaPrizeDW2", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_DW_KEY + "2"));
        resultMap.put("percent2", RedisHelper.get(FruitLaBaManager.REDIS_LABA_PERCENT_KEY + "2"));

        resultMap.put("drawPercent3", FruitLaBaManager.drawPercent3);
        resultMap.put("fruitLabaBlackRoomTP3", RedisHelper.get(FruitLaBaManager.BLACK_ROOM_TP_KEY + "3"));
        resultMap.put("fruitLabaBlackRoomEP3", RedisHelper.get(FruitLaBaManager.BLACK_ROOM_EP_KEY + "3"));
        resultMap.put("fruitLabaBlackRoomBP3", RedisHelper.get(FruitLaBaManager.BLACK_ROOM_BP_KEY + "3"));
        resultMap.put("fruitLabaStockK03", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K0_KEY + "3"));
        resultMap.put("fruitLabaStockK13", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K1_KEY + "3"));
        resultMap.put("fruitLabaStockK23", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K2_KEY + "3"));
        resultMap.put("fruitLabaStockK33", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K3_KEY + "3"));
        resultMap.put("fruitLabaStockK43", RedisHelper.get(FruitLaBaManager.REDIS_Stock_K4_KEY + "3"));
        resultMap.put("fruitLabaPrizeDXJ3", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_DXJ_KEY + "3"));
        resultMap.put("fruitLabaPrizeLXJ3", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_LXJ_KEY + "3"));
        resultMap.put("fruitLabaPrizeFLXJ3", RedisHelper.get(FruitLaBaManager.REDIS_PLAYER_FLXJ_KEY + "3"));
        resultMap.put("fruitLabaPrizeFDXJ3", RedisHelper.get(FruitLaBaManager.REDIS_PLAYER_FDXJ_KEY + "3"));
        resultMap.put("fruitLabaPrizeXS3", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_XS_KEY + "3"));
        resultMap.put("fruitLabaPrizeFXS3", RedisHelper.get(FruitLaBaManager.REDIS_PRIZE_FXS_KEY + "3"));
        resultMap.put("fruitLabaPrizeDX3", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_DX_KEY + "3"));
        resultMap.put("fruitLabaPrizeLX3", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_LX_KEY + "3"));
        resultMap.put("fruitLabaPrizeLW3", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_LW_KEY + "3"));
        resultMap.put("fruitLabaPrizeDW3", RedisHelper.get(FruitLaBaManager.REDIS_LUCKY_DW_KEY + "3"));
        resultMap.put("percent3", RedisHelper.get(FruitLaBaManager.REDIS_LABA_PERCENT_KEY + "3"));

        resultMap.put("fishingGrandPrixInitPool", FishingGrandPrixManager.initPool);
        resultMap.put("fishingGrandPrixAP", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_AP_KEY, 0D));
        resultMap.put("fishingGrandPrixAPT", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_APT_KEY, 100L));
        resultMap.put("fishingGrandPrixBP", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_BP_KEY, 0D));
        resultMap.put("fishingGrandPrixQZ", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QZ_KEY, 0L));
        resultMap.put("fishingGrandPrixPQ", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_PQ_KEY, 0D));
        resultMap.put("fishingGrandPrixQY", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QY_KEY, 0L));
        resultMap.put("fishingGrandPrixPA", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_PA_KEY, 0D));
        resultMap.put("fishingGrandPrixQS", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QS_KEY, 0L));
        resultMap.put("fishingGrandPrixPW", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_PW_KEY, 0D));
        resultMap.put("fishingGrandPrixQX", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QX_KEY, 0L));
        resultMap.put("fishingGrandPrixPY", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_PY_KEY, 0D));
        resultMap.put("fishingGrandPrixQW", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QW_KEY, 0L));
        resultMap.put("fishingGrandPrixTP", RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_TP_KEY, 0D));

        response.setData(resultMap);
    }

    /**
     * 修改捕鱼游戏设置
     */
    @SuppressWarnings("unchecked")
    @GmHandler(key = "/osee/game/config/fishing/update")
    public void doGameConfigFishingUpdateTask(Map<String, Object> params, CommonResponse response) {
        FishingManager.setTorpedoDropRate((double) params.get("torpedoDropFreeRate"),
                (long) (double) params.get("torpedoDropPerPayMoney"), (double) params.get("torpedoDropPerPayRate"));
        FishingHitDataManager.GREENER_LIMIT = (int) (double) params.get("fishingGreener");

        FishingManager.setChallengeDropRate((double) params.get("challengeDropFreeRate"),
                (long) (double) params.get("challengeDropPerPayMoney"), (double) params.get("challengeDropPerPayRate"));
        FishingHitDataManager.GREENER_LIMIT = (int) (double) params.get("fishingGreener");

        Double fishingGrandPrixInitPool = (double) params.get("fishingGrandPrixInitPool");
        Double fishingGrandPrixAP = (double) params.get("fishingGrandPrixAP");
        Double fishingGrandPrixAPT = (double) params.get("fishingGrandPrixAPT");
        Double fishingGrandPrixBP = (double) params.get("fishingGrandPrixBP");
        Double fishingGrandPrixQZ = (double) params.get("fishingGrandPrixQZ");
        Double fishingGrandPrixPQ = (double) params.get("fishingGrandPrixPQ");
        Double fishingGrandPrixQY = (double) params.get("fishingGrandPrixQY");
        Double fishingGrandPrixPA = (double) params.get("fishingGrandPrixPA");
        Double fishingGrandPrixQS = (double) params.get("fishingGrandPrixQS");
        Double fishingGrandPrixPW = (double) params.get("fishingGrandPrixPW");
        Double fishingGrandPrixQX = (double) params.get("fishingGrandPrixQX");
        Double fishingGrandPrixPY = (double) params.get("fishingGrandPrixPY");
        Double fishingGrandPrixQW = (double) params.get("fishingGrandPrixQW");
        Double fishingGrandPrixTP = (double) params.get("fishingGrandPrixTP");

        ArrayList<Double> obj = (ArrayList<Double>) params.get("q0");
        List<Long> list = new ArrayList<>();
        for (Double aDouble : obj) {
            System.out.println(aDouble);
            list.add(aDouble.longValue());
        }
        FishingUtil.q0 =list.toArray(new Long[0]);

        ArrayList<Double> ap_obj = (ArrayList<Double>) params.get("ap");
        FishingUtil.ap =ap_obj.toArray(new Double[0]);

        ArrayList<Double> apt_obj = (ArrayList<Double>) params.get("apt");
        List<Long> apt = new ArrayList<>();
        for (Double aDouble : apt_obj) {
            apt.add(aDouble.longValue());
        }
        FishingUtil.apt =apt.toArray(new Long[0]);

        if(fishingGrandPrixInitPool.longValue() != FishingGrandPrixManager.initPool) {
            FishingGrandPrixManager.initPool = fishingGrandPrixInitPool.longValue();
            RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, String.valueOf(FishingGrandPrixManager.initPool));
        }
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_AP_KEY, String.valueOf(fishingGrandPrixAP));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_APT_KEY, String.valueOf(fishingGrandPrixAPT.longValue()));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_BP_KEY, String.valueOf(fishingGrandPrixBP));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QZ_KEY, String.valueOf(fishingGrandPrixQZ.longValue()));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_PQ_KEY, String.valueOf(fishingGrandPrixPQ));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QY_KEY, String.valueOf(fishingGrandPrixQY.longValue()));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_PA_KEY, String.valueOf(fishingGrandPrixPA));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QS_KEY, String.valueOf(fishingGrandPrixQS.longValue()));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_PW_KEY, String.valueOf(fishingGrandPrixPW));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QX_KEY, String.valueOf(fishingGrandPrixQX.longValue()));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_PY_KEY, String.valueOf(fishingGrandPrixPY));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_QW_KEY, String.valueOf(fishingGrandPrixQW.longValue()));
        RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_TP_KEY, String.valueOf(fishingGrandPrixTP));




        List<Map<String, Object>> configList = (List<Map<String, Object>>) params.get("fishing");
        for (int i = 0; i < 8; i++) {
            Map<String, Object> config = configList.get(i);

            int index = i % 4;
            int greener = i / 4;

            FishingHitDataManager.setFishingConfig(greener, index, config);
        }
        System.out.println(" --> " + configList.size());
        // 捕鱼挑战赛配置
        FishingHitDataManager.setFishingConfig(1, 4, configList.get(8));
        FishingHitDataManager.setFishingConfig(1, 5, configList.get(9));
        FishingHitDataManager.setFishingConfig(1, 6, configList.get(10));

//        Map<String, Object> grandPrix = (Map<String, Object>) params.get("grandPrix");
//
//        long initPool = (long) grandPrix.get("initPool");


//        if(initPool != FishingGrandPrixManager.initPool) {
//            FishingGrandPrixManager.initPool = initPool;
//            RedisHelper.set(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, String.valueOf(initPool));
//        }
    }

    /**
     * 修改抽水游戏设置
     */
    @GmHandler(key = "/osee/game/config/draw/update")
    public void doDrawUpdateTask(Map<String, Object> params, CommonResponse response) {
        // 抽水百分比：1-100

        // 修改水果拉霸抽水设置
        FruitLaBaManager.drawPercent1 = ((double) params.get("labaDrawPercent1"));
        RedisHelper.set("Osee:FruitLaba:Config:drawPercent1", new Gson().toJson(FruitLaBaManager.drawPercent1));
        FruitLaBaManager.drawPercent2 = ((double) params.get("labaDrawPercent2"));
        RedisHelper.set("Osee:FruitLaba:Config:drawPercent2", new Gson().toJson(FruitLaBaManager.drawPercent1));
        FruitLaBaManager.drawPercent3 = ((double) params.get("labaDrawPercent3"));
        RedisHelper.set("Osee:FruitLaba:Config:drawPercent3", new Gson().toJson(FruitLaBaManager.drawPercent3));

        // 修改水果拉霸入场金币要求
        FruitLaBaManager.enterMoneyLimit1 = new Gson().fromJson(new Gson().toJson(params.get("enterMoneyLimit1")), long.class);
        RedisHelper.set("Osee:FruitLaba:EnterLimit:money1", new Gson().toJson(FruitLaBaManager.enterMoneyLimit1));
        FruitLaBaManager.enterMoneyLimit2 = new Gson().fromJson(new Gson().toJson(params.get("enterMoneyLimit2")), long.class);
        RedisHelper.set("Osee:FruitLaba:EnterLimit:money2", new Gson().toJson(FruitLaBaManager.enterMoneyLimit2));
        FruitLaBaManager.enterMoneyLimit3 = new Gson().fromJson(new Gson().toJson(params.get("enterMoneyLimit3")), long.class);
        RedisHelper.set("Osee:FruitLaba:EnterLimit:money3", new Gson().toJson(FruitLaBaManager.enterMoneyLimit3));

        // 修改水果拉霸中奖几率
        // 线条个数概率
        FruitLaBaManager.lineRate = new Gson().fromJson(new Gson().toJson(params.get("lineRate")), Double[].class);
        RedisHelper.set("Osee:FruitLaba:Config:LineRate", new Gson().toJson(FruitLaBaManager.lineRate));
        // 倍数概率
        FruitLaBaManager.multipleLevel = new Gson().fromJson(new Gson().toJson(params.get("multipleLevel")), Double[].class);
        RedisHelper.set("Osee:FruitLaba:Config:MultipleLevelRate", new Gson().toJson(FruitLaBaManager.multipleLevel));
        // 保存每条线的库存
//        Long[] linePoolGolds = new Gson().fromJson(new Gson().toJson(params.get("linePoolGold")), Long[].class);
//        for (int i = 1; i < linePoolGolds.length; i++) {
//            FruitLaBaManager.linePoolGold[i] = linePoolGolds[i];
//        }
        AgentManager.activeRate = (Double) params.get("activeRate");
        RedisHelper.set(AgentManager.activeRateKey, AgentManager.activeRate.toString());

        //Double tenDrawPercent = (Double) params.get("tenDrawPercent");
        //FightTenManager.winDrawPercent = tenDrawPercent;
        //RedisHelper.set("Osee:FightTen:Config:WinDrawPercent", tenDrawPercent.toString());

        Long fruitLabaBlackRoomTP1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaBlackRoomTP1")), Long.class);
        RedisHelper.set(FruitLaBaManager.BLACK_ROOM_TP_KEY + 1, fruitLabaBlackRoomTP1.toString());
        Integer fruitLabaBlackRoomEP1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaBlackRoomEP1")), Integer.class);
        RedisHelper.set(FruitLaBaManager.BLACK_ROOM_EP_KEY + 1, fruitLabaBlackRoomEP1.toString());
        Integer fruitLabaBlackRoomBP1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaBlackRoomBP1")), Integer.class);
        RedisHelper.set(FruitLaBaManager.BLACK_ROOM_BP_KEY + 1, fruitLabaBlackRoomBP1.toString());
        Long fruitLabaStockK01 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK01")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K0_KEY + 1, fruitLabaStockK01.toString());
        Long fruitLabaStockK11 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK11")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K1_KEY + 1, fruitLabaStockK11.toString());
        Long fruitLabaStockK21 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK21")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K2_KEY + 1, fruitLabaStockK21.toString());
        Long fruitLabaStockK31 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK31")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K3_KEY + 1, fruitLabaStockK31.toString());
        Long fruitLabaStockK41 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK41")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K4_KEY + 1, fruitLabaStockK41.toString());
        Long fruitLabaPrizeDXJ1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeDXJ1")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_DXJ_KEY + 1, fruitLabaPrizeDXJ1.toString());
        Long fruitLabaPrizeFDXJ1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeFDXJ1")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PLAYER_FDXJ_KEY + 1, fruitLabaPrizeFDXJ1.toString());
        Long fruitLabaPrizeLXJ1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeLXJ1")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_LXJ_KEY + 1, fruitLabaPrizeLXJ1.toString());
        Long fruitLabaPrizeFLXJ1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeFLXJ1")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PLAYER_FLXJ_KEY + 1, fruitLabaPrizeFLXJ1.toString());
        Integer fruitLabaPrizeXS1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeXS1")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_XS_KEY + 1, fruitLabaPrizeXS1.toString());
        Integer fruitLabaPrizeFXS1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeFXS1")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_FXS_KEY + 1, fruitLabaPrizeFXS1.toString());
        Integer fruitLabaPrizeDX1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeDX1")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_DX_KEY + 1, fruitLabaPrizeDX1.toString());
        Integer fruitLabaPrizeLX1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeLX1")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_LX_KEY + 1, fruitLabaPrizeLX1.toString());
        Integer fruitLabaPrizeDW1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeDW1")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_DW_KEY + 1, fruitLabaPrizeDW1.toString());
        Integer fruitLabaPrizeLW1 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeLW1")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_LW_KEY + 1, fruitLabaPrizeLW1.toString());
        Double percent1 = new Gson().fromJson(new Gson().toJson(params.get("percent1")), Double.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LABA_PERCENT_KEY + 1, percent1.toString());

        Long fruitLabaBlackRoomTP2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaBlackRoomTP2")), Long.class);
        RedisHelper.set(FruitLaBaManager.BLACK_ROOM_TP_KEY + 2, fruitLabaBlackRoomTP2.toString());
        Integer fruitLabaBlackRoomEP2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaBlackRoomEP2")), Integer.class);
        RedisHelper.set(FruitLaBaManager.BLACK_ROOM_EP_KEY + 2, fruitLabaBlackRoomEP2.toString());
        Integer fruitLabaBlackRoomBP2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaBlackRoomBP2")), Integer.class);
        RedisHelper.set(FruitLaBaManager.BLACK_ROOM_BP_KEY + 2, fruitLabaBlackRoomBP2.toString());
        Long fruitLabaStockK02 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK02")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K0_KEY + 2, fruitLabaStockK02.toString());
        Long fruitLabaStockK12 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK12")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K2_KEY + 2, fruitLabaStockK12.toString());
        Long fruitLabaStockK22 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK22")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K2_KEY + 2, fruitLabaStockK22.toString());
        Long fruitLabaStockK32 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK32")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K3_KEY + 2, fruitLabaStockK32.toString());
        Long fruitLabaStockK42 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK42")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K4_KEY + 2, fruitLabaStockK42.toString());
        Long fruitLabaPrizeDXJ2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeDXJ2")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_DXJ_KEY + 2, fruitLabaPrizeDXJ2.toString());
        Long fruitLabaPrizeFDXJ2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeFDXJ2")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PLAYER_FDXJ_KEY + 2, fruitLabaPrizeFDXJ2.toString());
        Long fruitLabaPrizeLXJ2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeLXJ2")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_LXJ_KEY + 2, fruitLabaPrizeLXJ2.toString());
        Long fruitLabaPrizeFLXJ2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeFLXJ2")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PLAYER_FLXJ_KEY + 2, fruitLabaPrizeFLXJ2.toString());
        Integer fruitLabaPrizeXS2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeXS2")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_XS_KEY + 2, fruitLabaPrizeXS2.toString());
        Integer fruitLabaPrizeFXS2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeFXS2")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_FXS_KEY + 2, fruitLabaPrizeFXS2.toString());
        Integer fruitLabaPrizeDX2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeDX2")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_DX_KEY + 2, fruitLabaPrizeDX2.toString());
        Integer fruitLabaPrizeLX2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeLX2")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_LX_KEY + 2, fruitLabaPrizeLX2.toString());
        Integer fruitLabaPrizeDW2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeDW2")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_DW_KEY + 2, fruitLabaPrizeDW2.toString());
        Integer fruitLabaPrizeLW2 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeLW2")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_LW_KEY + 2, fruitLabaPrizeLW2.toString());
        Double percent2 = new Gson().fromJson(new Gson().toJson(params.get("percent2")), Double.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LABA_PERCENT_KEY + 2, percent2.toString());

        Long fruitLabaBlackRoomTP3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaBlackRoomTP3")), Long.class);
        RedisHelper.set(FruitLaBaManager.BLACK_ROOM_TP_KEY + 3, fruitLabaBlackRoomTP3.toString());
        Integer fruitLabaBlackRoomEP3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaBlackRoomEP3")), Integer.class);
        RedisHelper.set(FruitLaBaManager.BLACK_ROOM_EP_KEY + 3, fruitLabaBlackRoomEP3.toString());
        Integer fruitLabaBlackRoomBP3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaBlackRoomBP3")), Integer.class);
        RedisHelper.set(FruitLaBaManager.BLACK_ROOM_BP_KEY + 3, fruitLabaBlackRoomBP3.toString());
        Long fruitLabaStockK03 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK03")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K0_KEY + 3, fruitLabaStockK03.toString());
        Long fruitLabaStockK13 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK13")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K3_KEY + 3, fruitLabaStockK13.toString());
        Long fruitLabaStockK23 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK23")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K3_KEY + 3, fruitLabaStockK23.toString());
        Long fruitLabaStockK33 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK33")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K3_KEY + 3, fruitLabaStockK33.toString());
        Long fruitLabaStockK43 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaStockK43")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_Stock_K4_KEY + 3, fruitLabaStockK43.toString());
        Long fruitLabaPrizeDXJ3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeDXJ3")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_DXJ_KEY + 3, fruitLabaPrizeDXJ3.toString());
        Long fruitLabaPrizeFDXJ3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeFDXJ3")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PLAYER_FDXJ_KEY + 3, fruitLabaPrizeFDXJ3.toString());
        Long fruitLabaPrizeLXJ3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeLXJ3")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_LXJ_KEY + 3, fruitLabaPrizeLXJ3.toString());
        Long fruitLabaPrizeFLXJ3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeFLXJ3")), Long.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PLAYER_FLXJ_KEY + 3, fruitLabaPrizeFLXJ3.toString());
        Integer fruitLabaPrizeXS3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeXS3")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_XS_KEY + 3, fruitLabaPrizeXS3.toString());
        Integer fruitLabaPrizeFXS3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeFXS3")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_PRIZE_FXS_KEY + 3, fruitLabaPrizeFXS3.toString());
        Integer fruitLabaPrizeDX3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeDX3")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_DX_KEY + 3, fruitLabaPrizeDX3.toString());
        Integer fruitLabaPrizeLX3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeLX3")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_LX_KEY + 3, fruitLabaPrizeLX3.toString());
        Integer fruitLabaPrizeDW3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeDW3")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_DW_KEY + 3, fruitLabaPrizeDW3.toString());
        Integer fruitLabaPrizeLW3 = new Gson().fromJson(new Gson().toJson(params.get("fruitLabaPrizeLW3")), Integer.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LUCKY_LW_KEY + 3, fruitLabaPrizeLW3.toString());
        Double percent3 = new Gson().fromJson(new Gson().toJson(params.get("percent3")), Double.class);
        RedisHelper.set(FruitLaBaManager.REDIS_LABA_PERCENT_KEY + 3, percent3.toString());
    }

    @GmHandler(key = "/osee/server/games")
    public void doServerGamesData(Map<String, Object> params, CommonResponse response) {
        Double page = Double.parseDouble(params.get("page").toString());
        Double limit = Double.parseDouble(params.get("limit").toString());
        Double startTime = null;
        Double endTime = null;
        Double mode = null;
        Double type = null;
        String startDate = null;
        String endDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd");
        if(params.get("startDate") != null && params.get("endDate") != null) {
            startTime = Double.parseDouble(params.get("startDate").toString());
            endTime = Double.parseDouble(params.get("endDate").toString());
            startDate = sdf.format(startTime);
            endDate = sdf.format(endTime);
        }
        if(params.get("mode") != null) {
            mode = Double.parseDouble(params.get("mode").toString());
        }
        if(params.get("type") != null) {
            type = Double.parseDouble(params.get("type").toString());
        }


        List<AppGameLogEntity> entities = gameLogMapper.find(startDate, endDate, mode == null ? null : mode.intValue(), limit.intValue() * (page.intValue() - 1), limit.intValue(), type == null ? null : type.intValue());
        int count = gameLogMapper.count(startDate, endDate, mode == null ? null : mode.intValue(),  type == null ? null : type.intValue());
        response.setData(entities);
        Map<String, Object> obj = new HashMap<>();
        obj.put("count", count);
        obj.put("data", entities);
        response.setData(obj);
        response.setSuccess(true);
    }

    @GmHandler(key = "/osee/server/ranks")
    public void doServerRanksData(Map<String, Object> params, CommonResponse response) throws ParseException {
        Double page = Double.parseDouble(params.get("page").toString());
        Double limit = Double.parseDouble(params.get("limit").toString());
        Double time = Double.parseDouble(params.get("date").toString());
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd");
        String date = sdf.format(time);
        Double type = Double.parseDouble(params.get("type").toString());
        List<AppRankLogEntity> entities = rankLogMapper.find(limit.intValue() * (page.intValue() - 1), limit.intValue(), date, type.intValue());
        int count = rankLogMapper.count(date, type.intValue());
        Map<String, Object> obj = new HashMap<>();
        obj.put("count", count);
        obj.put("data", entities);
        response.setData(obj);
        response.setSuccess(true);
    }
}
