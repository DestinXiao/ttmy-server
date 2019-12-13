package com.maple.game.osee.manager.fruitlaba;

import com.google.gson.Gson;
import com.maple.common.lobby.proto.LobbyMessage;
import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.mapper.UserMapper;
import com.maple.engine.data.ServerUser;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.dao.data.entity.FruitLaBaRewardInfo;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.FruitLaBaRewardInfoMapper;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.dao.log.entity.OseeCutMoneyLogEntity;
import com.maple.game.osee.dao.log.entity.OseeExpendLogEntity;
import com.maple.game.osee.dao.log.entity.OseeFruitRecordLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeCutMoneyLogMapper;
import com.maple.game.osee.dao.log.mapper.OseeExpendLogMapper;
import com.maple.game.osee.dao.log.mapper.OseeFruitRecordLogMapper;
import com.maple.game.osee.entity.GameEnum;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fruitlaba.FruitlabaPlayer;
import com.maple.game.osee.manager.AgentManager;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.OseePublicData.FruitLaBaRewardInfoProto;
import com.maple.game.osee.proto.OseePublicData.FruitLaBaSpotProto;
import com.maple.game.osee.proto.OseePublicData.FruitLaBaWinLineDataProto;
import com.maple.game.osee.proto.OseePublicData.FruitLaBaWinSpecialRewardProto;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.FruitLaBaStartRunResponse;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.FruitLabaReceiveTaskResponse;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.FruitLabaTaskInfoResponse;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.PlayerEnterFruitLaBaRoomResponse;
import com.maple.game.osee.timer.AutoWanderSubtitle;
import com.maple.network.manager.NetManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 水果拉霸管理类
 *
 * @author lzr
 * <p>
 * 2018年12月27日
 */
@Component
public class FruitLaBaManager {

    @Autowired
    private OseeExpendLogMapper expendLogMapper;

    @Autowired
    private FruitLaBaRewardInfoMapper fruitLaBaRewardInfoMapper;

    @Autowired
    private OseeFruitRecordLogMapper fruitRecordLogMapper;

    @Autowired
    private FruitLaBaSendDataManager fruitLaBaSendDataManager;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private OseeCutMoneyLogMapper cutMoneyLogMapper;

    private static OseePlayerMapper playerMapper;

    @Autowired
    private UserMapper userMapper;

    public static final String REDIS_LABA_PERCENT_KEY = "Osee:FruitLaba:Config:Percent";

    /**
     * 水果拉霸 线概率 保存在redis中的key
     */
    public static final String REDIS_LINERATE_CONFIG_KEYNAME = "Osee:FruitLaba:Config:LineRate";

    /**
     * 水果拉霸 线概率 保存在redis中的key
     */
    public static final String REDIS_MULTIPLE_LEVEL_CONFIG_KEYNAME = "Osee:FruitLaba:Config:MultipleLevelRate";

    /**
     * 水果拉霸 线概率 保存在redis中的key
     */
    public static final String REDIS_DRAWPERCENT = "Osee:FruitLaba:Config:drawPercent";

    /**
     * 水果拉霸 小黑屋 保存在redis中的key
     */
    public static final String BLACK_ROOM_KEY = "Osee:FruitLaba:Blackroom:player";

    /**
     * 水果拉霸 小黑屋奖金 TP 金币数，
     * 后台可设置 取值范围：[1， 500000000（5亿）]
     * 如果玩家累计中奖达到该金额则进入小黑屋
     */
    public static final String BLACK_ROOM_TP_KEY = "Osee:FruitLaba:Blackroom:tp";
    /**
     * 水果拉霸 小黑屋倍率 EP
     * 后台可设置
     * 取值范围：[2，5]
     * 如果玩家进入小黑屋后，则设置该玩家只中EP级倍率以下
     */
    public static final String BLACK_ROOM_EP_KEY = "Osee:FruitLaba:Blackroom:ep";
    /**
     * 水果拉霸 小黑屋中奖线数 BP
     * 后台可设置
     * 取值范围：[2, 9]
     * 如果玩家进入小黑屋后，则将BP级以上中奖线的概率增加至0条中奖线上
     */
    public static final String BLACK_ROOM_BP_KEY = "Osee:FruitLaba:Blackroom:bp";

    /**
     * 入场金币限制 保存在redis中的key
     */
    public static final String ENTER_LIMIT_KEY = "Osee:FruitLaba:EnterLimit:money";

    public static final String ENTER_LIMIT_ROOM1_KEY = "Osee:FruitLaba:EnterLimit:money1";
    public static final String ENTER_LIMIT_ROOM2_KEY = "Osee:FruitLaba:EnterLimit:money2";
    public static final String ENTER_LIMIT_ROOM3_KEY = "Osee:FruitLaba:EnterLimit:money3";

    /**
     *   K3 (DEFAULT: 10亿) > K2（DEFAULT：5亿） > K1（DEFAULT：2亿） > k0（5000万）
     *   可通过后台进行调节
     */
    /**
     *  库存阀值K0 保存再redis中的key
     */
    public static final String REDIS_Stock_K0_KEY = "Osee:FruitLaba:Config:Stock:k0";
    /**
     *  库存阀值K1 保存再redis中的key
     */
    public static final String REDIS_Stock_K1_KEY = "Osee:FruitLaba:Config:Stock:k1";
    /**
     *  库存阀值K2 保存再redis中的key
     */
    public static final String REDIS_Stock_K2_KEY = "Osee:FruitLaba:Config:Stock:k2";
    /**
     *  库存阀值K3 保存再redis中的key
     */
    public static final String REDIS_Stock_K3_KEY = "Osee:FruitLaba:Config:Stock:k3";
    /**
     *  库存阀值K4 关闭2级及以上倍率
     */
    public static final String REDIS_Stock_K4_KEY = "Osee:FruitLaba:Config:Stock:k4";
    /**
     *  dxj 当天累计中奖上限
     *  如果大于该值时，则启动幸运参数dx
     *  默认值 1亿
     */
    public static final String REDIS_PRIZE_DXJ_KEY = "Osee:FruitLaba:Config:Prize:dxj";
    /**
     *  lxj 历史累计中奖上限
     *  如果大于该值时，则启动幸运参数lx
     *  默认值 10亿
     */
    public static final String REDIS_PRIZE_LXJ_KEY = "Osee:FruitLaba:Config:Prize:lxj";
    /**
     *  玩家历史累计中奖值
     * {REDIS_HISTORY_PRIZE_KEY + playerId}
     */
    public static final String REDIS_PLAYER_HISTORY_PRIZE_KEY = "Osee:FruitLaba:Player:History:Prize";
    /**
     *  玩家今日累计中奖值
     *  {REDIS_PRIZE_DXJ_KEY + playerId}
     */
    public static final String REDIS_PLAYER_TODAY_PRIZE_KEY = "Osee:FruitLaba:Player:Today:Prize";
    /**
     *  fdxj
     *  当天累计获奖金币数 < fdxj 启动幸运参数 DW
     *
     *  DEFAULT: -1亿
     */
    public static final String REDIS_PLAYER_FDXJ_KEY = "Osee:FruitLaba:Player:fdxj";
    /**
     * flxj
     * 历史累计获奖金币数 < flxj 启动幸运参数 LW
     *
     * DEFAULT: -10亿
     */
    public static final String REDIS_PLAYER_FLXJ_KEY = "Osee:FruitLaba:Player:flxj";
    /**
     *  xs 中奖线参数
     *  取值范围：[2,9]
     *  default: 4
     */
    public static final String REDIS_PRIZE_XS_KEY = "Osee:FruitLaba:Config:Prize:xs";

    public static final String REDIS_PRIZE_FXS_KEY = "Osee:FruitLaba:Config:Prize:fxs";
    /**
     *
     */
    public static final String REDIS_LUCKY_DX_KEY = "Osee:FruitLaba:Config:Lucky:dx";
    /**
     *
     */
    public static final String REDIS_LUCKY_LX_KEY = "Osee:FruitLaba:Config:Lucky:lx";
    public static final String REDIS_LUCKY_LW_KEY = "Osee:FruitLaba:Config:Lucky:lw";
    public static final String REDIS_LUCKY_DW_KEY = "Osee:FruitLaba:Config:Lucky:dw";
    /**
     * 调整指定用户中奖参数 {REDIS_PLAYER_AG_KEY + playerId}
     *
     * <pre>
     * +-----------------------------------+
     * |  32 |  16 |  8  |  4  |  2  |  1  |
     * +-----------------------------------+
     * |  6  |  5  |  4  |  3  |  2  |  1  |
     * +-----------------------------------+
     * </pre>
     *
     * 使用不同的数字代表指定不同的倍率<br/>
     * eg: 5 = 1 + 4 (指定 1倍 和 3倍)<br/>
     * eg: 21 = 1 + 4 + 16 (指定 1倍、3倍 和 5倍)<br/>
     *
     * DEFAULT: 63 = 1 + 2 + 4 + 8 + 16 + 32
     *
     */
    public static final String REDIS_PLAYER_AG_KEY = "Osee:FruitLaba:Player:ag";
    /**
     * 当指定玩家倍率被指定为只为1时，使用此参数<br/>
     *
     *
     */
    public static final String REDIS_PLAYER_RATE_ONE_KEY = "Osee:FruitLaba:Player:Rate:one";

    /**
     * 水果拉霸 出现线的条数的各概率
     */
    public static Double[] lineRate = new Double[10];

    /**
     * 水果拉霸 生成倍数的等级概率
     */
    public static Double[] multipleLevel = new Double[10];

    /**
     * 水果拉霸 每条线的库存
     */
    public static long[] linePoolGold = new long[28];


    /**
     * 玩家保存到redis中的 <strong>免费抽奖次数</strong> 的数据key的标识头 (标识头 + playerId)就是key
     */
    public static final String FruitDrawSign = "FruitLaBa:FreeDrawNum:";

    /**
     * 玩家保存到redis中的已经 <strong>抽奖次数</strong> 的数据key的标识头 (标识头 + playerId)就是key
     */
    public static final String FruitTotalRotateSign = "FruitLaBa:TotalRotateNum:";

    /**
     * 保存到redis中的 <strong>彩金池金币数量</strong> 的数据key的标识
     */
    public static final String FruitLaBaRewardPoolSign = "FruitLaBa:RewardPoolNum";

    /**
     * 玩家保存到redis中的 <strong>抽奖幸运值</strong> 的数据key的标识头 (标识头 + playerId)就是key
     */
    public static final String FruitLaBaLukeyNumSign = "FruitLaBa:LukeyNumSign:";

    /**
     * 保存进redis中的 <strong>每条线的初始金币</strong> 的数据key的标识头 (标识头+线的id)就是key
     */
    public static final String FruitLaBaInitGoldNumSign = "FruitLaBa:InitGoldNumSign:";

    /**
     * 保存进redis中的 <strong>免费抽奖时的金币和线数量</strong> 的数据key的标识头 (标识头+player的id)就是key
     */
    public static final String FruitLaBaPlayerFreeLineAndGoldSign = "FruitLaBa:PlayerFreeLineAndGoldSign:";

    /**
     * 玩家抽奖时的金币,进入奖池的比例
     */
    public static double percent = 1.0;// 千分之四

    /**
     * 玩家抽奖时的金币,进入奖池的比例
     */
    public static int rate5addFromPoolLine = 200000000;// 库存达到2亿开启

    /**
     * 抽水百分比
     */
    public static double drawPercent = 20.0;// 百分之20

    public static double drawPercent1 = 20.0;// 百分之20
    public static double drawPercent2 = 20.0;// 百分之20
    public static double drawPercent3 = 20.0;// 百分之20

    /**
     * 任务旋转次数奖励以这个数据为一个单位
     */
    public final static int roateRewardUnit = 100;

    /**
     * 当全服库存达到2亿金币时，每多1亿都将使五级倍率奖励几率提高1%，从一级倍率中扣取，最高提升3%。同时随着库存的变化而降低倍率奖励。
     */
    public static double probabilityFromPoll = 0;

    /**
     * 一个单位奖励金币的数量
     */
    public final static int rotateRewardGoldUnit = 2000;

    /**
     * 数字7 限制检查特殊奖励金币(单注金币大于这个限制金币，才能进行数字7的特殊奖励判断)
     */
    public final static int numSevenLimitCheckGold = 50000;

    /**
     * 进入房间金币要求
     */
    public static long enterMoneyLimit1 = 10000;
    public static long enterMoneyLimit2 = 100000;
    public static long enterMoneyLimit3 = 1000000;

    /**
     * 初始化彩金池金币
     */
    public static long rewardTotal = 0;
    public static long rewardTotal1 = 0;
    public static long rewardTotal2 = 0;
    public static long rewardTotal3 = 0;

    /**
     * 启用返水参数的金币
     **/
    private static long ENABLE_REBACK = 25000;

    /**
     * 一个单位奖励点券的数量
     */
    public static final int rotateRewardLotteryUnit = 2;

     /**
     * 平面上鱼的个数(根据这个随机鱼的个数，进行判断线的输赢)
     */
    public static final int planeFishNum = 15;

    /**
     * 平面上横排条数(注意：横排*竖排=平面上鱼的个数) 即有多少行
     */
    public static final int rankNum = 3;

    /**
     * 平面上竖排条数(注意：横排*竖排=平面上鱼的个数) 即有多少列
     */
    public static final int verticalNum = 5;

    /**
     * 所有可赢的线的集合 => 第几条与线的位置
     */
    public static final Integer[][][] spotLines = {
            {{0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}}, // 第1条线
            {{0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2}}, // 第2条线
            {{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}}, // 第3条线
            {{0, 2}, {1, 1}, {2, 0}, {3, 1}, {4, 2}}, // 第4条线
            {{0, 0}, {1, 1}, {2, 2}, {3, 1}, {4, 0}}, // 第5条线
            {{0, 2}, {1, 2}, {2, 1}, {3, 0}, {4, 0}}, // 第6条线
            {{0, 0}, {1, 0}, {2, 1}, {3, 2}, {4, 2}}, // 第7条线
            {{0, 1}, {1, 0}, {2, 1}, {3, 2}, {4, 1}}, // 第8条线
            {{0, 1}, {1, 2}, {2, 1}, {3, 0}, {4, 1}} // 第9条线
    };

    /**
     * 倍率表
     */
    public static final Integer[][] rateMap = {
            {1, 3, 5, 7, 10, 15, 20, 25, 30},
            {40, 45, 50, 60},
            {65, 70, 85, 90, 95},
            {100, 150, 200, 350},
            {400, 450, 500, 600, 700},
            {1000, 3000, 5000}
//            {1, 3},//一级倍率
//            {10, 15},//二级倍率
//            {25, 30, 35, 40, 45, 50, 70, 75, 80, 85},//三级倍率
//            {100, 175, 200, 250},//四级倍率
//            {400, 550, 650, 800},//五级倍率
//            {1250, 1750},//六级倍率
    };


    /**
     *      Edit
     *     ===== 新规则 =====
     *
     * 所有水果id
     * 1：wild：wild可以代替任何元素除了bonus和7，其余都可以和它组合成为该元素之一，如：2个香蕉500分加上wild组合成3个为1500分。
     * 2：bonus：连续3列（竖列）出现bonus可以获得免费的摇奖（每中一次可以免费摇5次奖）除此之外，3个连着为单线投入的20倍（连线x单线投入x20=最终奖励金币）4个为65倍，5个为500倍
     * 3：数字7：如果单线投入金币为50000，则该玩家获得彩金池全部奖金；此外3个横着为单线投入的350倍（连线x单线投入x350=最终奖励金币）4个为450倍，5个为5000倍
     * 4：香蕉：3个相连的为1倍，4个相连为单线投入的3倍（连线x单线投入x3=最终奖励金币）5个为70倍
     * 5：西瓜：3个相连为单线投入的5倍（连线x单线投入x3=最终奖励金币）4个为10倍，5个为85倍
     * 6：柠檬：3个相连为单线投入的15倍（连线x单线投入x15=最终奖励金币）4个为40倍，5个为100倍
     * 7：葡萄：3个相连为单线投入的25倍（连线x单线投入x25=最终奖励金币）4个为50倍，5个为400倍
     * 8：气球：3个相连为单线投入的30倍（连线x单线投入x30=最终奖励金币）4个为90倍，5个为600倍
     * 9：铃铛：3个相连为单线投入的35倍（连线x单线投入x35=最终奖励金币）4个为95倍，5个为700倍
     * 10：樱桃：3个相连为单线投入的45倍（连线x单线投入x45=最终奖励金币）4个为150倍，5个为1000倍
     * 11：bar：3个相连为单线投入的60倍（连线x单线投入x75=最终奖励金币）4个为200倍，5个为3000倍
     */
    public static final List<Integer> fruitIds = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));

    /**
     * 普通水果对应的倍数,(注意：上面修改，则下面有可能也要修改)
     */
    public static final Map<Integer, List<Integer>> fruitMultiple = new HashMap<Integer, List<Integer>>() {
        private static final long serialVersionUID = 1L;

        {
            // 将水果倍数数据，保存进这里 key=水果id,value=水果 连续个数(1~5)的倍数
            put(fruitIds.get(0), new ArrayList<>(Arrays.asList(0, 0, 60, 200, 3000)));// wild(变成 能变成的最大倍数，bar)
            put(fruitIds.get(1), new ArrayList<>(Arrays.asList(0, 0, 20, 65, 500)));// bonus
            put(fruitIds.get(2), new ArrayList<>(Arrays.asList(0, 0, 350, 450, 5000)));// 数字7
            put(fruitIds.get(3), new ArrayList<>(Arrays.asList(0, 0, 1, 3, 70)));// 香蕉
            put(fruitIds.get(4), new ArrayList<>(Arrays.asList(0, 0, 5, 10, 85)));// 西瓜
            put(fruitIds.get(5), new ArrayList<>(Arrays.asList(0, 0, 7, 40, 100)));// 柠檬
            put(fruitIds.get(6), new ArrayList<>(Arrays.asList(0, 0, 15, 50, 400)));// 葡萄
            put(fruitIds.get(7), new ArrayList<>(Arrays.asList(0, 0, 25, 90, 600)));// 气球
            put(fruitIds.get(8), new ArrayList<>(Arrays.asList(0, 0, 30, 95, 700)));// 铃铛
            put(fruitIds.get(9), new ArrayList<>(Arrays.asList(0, 0, 45, 150, 1000)));// 樱桃
            put(fruitIds.get(10), new ArrayList<>(Arrays.asList(0, 0, 60, 200, 3000)));// bar
        }
    };

    /**
     * 进入了水果拉霸房间的玩家
     */
    public static final CopyOnWriteArrayList<ServerUser> fruitRoomUser = new CopyOnWriteArrayList<>();
    public static final CopyOnWriteArrayList<ServerUser> fruitRoom1User = new CopyOnWriteArrayList<>();
    public static final CopyOnWriteArrayList<ServerUser> fruitRoom2User = new CopyOnWriteArrayList<>();
    public static final CopyOnWriteArrayList<ServerUser> fruitRoom3User = new CopyOnWriteArrayList<>();
    /**
     * 进入了水果拉霸房间的玩家
     */
    public static final Map<Long, FruitlabaPlayer> fruitplayers = new HashMap<>();

    @Autowired
    public FruitLaBaManager(OseePlayerMapper playerMapper) {
        FruitLaBaManager.playerMapper = playerMapper;
    }

    /**
     * 水果拉霸初始化  need
     */
    public void init() {
        //从redis中读出 线的分布概率 或 初始化
        String LineRateConfigJson = RedisHelper.get(REDIS_LINERATE_CONFIG_KEYNAME);
        Double[] tempLineRate = new Gson().fromJson(LineRateConfigJson, Double[].class);
        if (tempLineRate == null) {
            lineRate[0] = 50.0;
            lineRate[1] = 12.0;
            lineRate[2] = 10.0;
            lineRate[3] = 7.0;
            lineRate[4] = 6.0;
            lineRate[5] = 5.0;
            lineRate[6] = 4.0;
            lineRate[7] = 3.0;
            lineRate[8] = 2.0;
            lineRate[9] = 1.0;
            RedisHelper.set(REDIS_LINERATE_CONFIG_KEYNAME, new Gson().toJson(lineRate));
        } else {
            lineRate = tempLineRate;
        }

        for (int i = 1; i <= 3; i++) {
            String key = BLACK_ROOM_TP_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "5000000");
            key = BLACK_ROOM_BP_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "3");
            key = BLACK_ROOM_EP_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "2");
            key = REDIS_Stock_K0_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "80000000");
            key = REDIS_Stock_K1_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "200000000");
            key = REDIS_Stock_K2_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "300000000");
            key = REDIS_Stock_K3_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "50000000000");
            key = REDIS_Stock_K4_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "1000000");
            key = REDIS_PRIZE_DXJ_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "20000000");
            key = REDIS_LUCKY_DX_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "2");
            key = REDIS_PRIZE_LXJ_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "100000000");
            key = REDIS_LUCKY_LX_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "2");
            key = REDIS_PRIZE_XS_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "2");
            key = REDIS_PRIZE_FXS_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "2");
            key = REDIS_PLAYER_FDXJ_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "-200000000");
            key = REDIS_PLAYER_FLXJ_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "-900000000");
            key = REDIS_LUCKY_LW_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "2");
            key = REDIS_LUCKY_DW_KEY + i;
            if(RedisHelper.get(key).equals("")) RedisHelper.set(key, "2");
            key = REDIS_LABA_PERCENT_KEY + i;
            if (RedisHelper.get(key).equals("")) RedisHelper.set(key, "1");
        }


        //从redis中读出 倍率级别的概率 或 初始化
        String MultipleRateConfigJson = RedisHelper.get(REDIS_MULTIPLE_LEVEL_CONFIG_KEYNAME);
        Double[] tempMultipleRate = new Gson().fromJson(MultipleRateConfigJson, Double[].class);
        if (tempMultipleRate == null) {
            multipleLevel[0] = 0.0;
            multipleLevel[1] = 90.0;
            multipleLevel[2] = 7.0;
            multipleLevel[3] = 1.89;
            multipleLevel[4] = 1.0;
            multipleLevel[5] = 0.1;
            multipleLevel[6] = 0.01;
            RedisHelper.set(REDIS_MULTIPLE_LEVEL_CONFIG_KEYNAME, new Gson().toJson(multipleLevel));
        } else {
            multipleLevel = tempMultipleRate;
        }

        String drawPercentStr = RedisHelper.get(REDIS_DRAWPERCENT);
        drawPercent = StringUtils.isEmpty(drawPercentStr) ? 10D : new Gson().fromJson(drawPercentStr, double.class);


        //从Redis中 读取奖池比例
        String percent_str = RedisHelper.get(REDIS_LABA_PERCENT_KEY);
        percent = Double.parseDouble(percent_str.equals("") ? "1.0" : percent_str);

        // 对进入水果拉霸的玩家进行检测，清除掉线的玩家
        ThreadPoolUtils.createSingleThread().scheduleAtFixedRate(() -> {
            try {
                for (ServerUser user : fruitRoomUser) {
                    if (user == null ||
                            !user.isOnline() ||
                            user.getEntity().getOnlineState() != GameEnum.FRUIT_LABA.getId()) {
                        fruitRoomUser.remove(user);
                        fruitplayers.remove(user.getId());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1000, 3000, TimeUnit.MILLISECONDS);

        //从redis中读取彩金池初始金币
        String goldStr2= RedisHelper.get(FruitLaBaRewardPoolSign + 2);
        if (!StringUtils.isEmpty(goldStr2)) {
            rewardTotal2 = Long.parseLong(RedisHelper.get(FruitLaBaRewardPoolSign + 2));
        }

        String goldStr3= RedisHelper.get(FruitLaBaRewardPoolSign + 3);
        if (!StringUtils.isEmpty(goldStr3)) {
            rewardTotal3 = Long.parseLong(RedisHelper.get(FruitLaBaRewardPoolSign + 3));
        }

        String goldStr1= RedisHelper.get(FruitLaBaRewardPoolSign + 1);
        if (!StringUtils.isEmpty(goldStr1)) {
            rewardTotal1 = Long.parseLong(RedisHelper.get(FruitLaBaRewardPoolSign + 1));
        }

        //从redis中读取库存
        for (int i = 1; i <= 27; i++) {
            linePoolGold[i] = getLinePoolGold(i);
        }

        try {
            enterMoneyLimit1 = Long.parseLong(RedisHelper.get(ENTER_LIMIT_ROOM1_KEY));
        } catch (Exception e) {
            RedisHelper.set(ENTER_LIMIT_ROOM1_KEY, "10000");
        }

        try {
            enterMoneyLimit2 = Long.parseLong(RedisHelper.get(ENTER_LIMIT_ROOM2_KEY));
        } catch (Exception e) {
            RedisHelper.set(ENTER_LIMIT_ROOM2_KEY, "100000");
        }

        try {
            enterMoneyLimit3 = Long.parseLong(RedisHelper.get(ENTER_LIMIT_ROOM3_KEY));
        } catch (Exception e) {
            RedisHelper.set(ENTER_LIMIT_ROOM3_KEY, "1000000");
        }
        // 初始化水果拉霸中每条线的初始金币(如果该字段存在，就不初始化)
        // cleanUserRotateTask();
    }

    /**
     * 玩家进入水果拉霸房间(主要是给玩家发送数据) need
     */
    public void playerEnterFruitLaBaRoom(ServerUser user, int roomType) {
//        if (PlayerManager.getPlayerLevel(user) < 20) {
//            NetManager.sendErrorMessageToClient("等级不足，进入失败！", user);
//            return;
//        }
        // 加入到房间玩家列表中


        boolean flag = false;
        for (ServerUser serverUser : fruitRoomUser) {
            if (user.getId() == serverUser.getId()) {
                flag = true;
                break;
            }
        }

        long money = 0L;
        if(roomType == 1) {
            money = enterMoneyLimit1;
        } else if(roomType == 2) {
            money = enterMoneyLimit2;
        } else if(roomType == 3) {
            money = enterMoneyLimit3;
        }

        if(!checkCondition(user, money)) {
            return ;
        }

        if (!flag) {
            fruitRoomUser.add(user);
            if(roomType == 1) fruitRoom1User.add(user);
            else if(roomType == 2) fruitRoom2User.add(user);
            else if(roomType == 3) fruitRoom3User.add(user);
        }
        // 置为水果拉霸房间内
        user.getEntity().setOnlineState(GameEnum.FRUIT_LABA.getId());
        // 更新数据库数据
        userMapper.update(user.getEntity());
        sendFruitLaBaRoomInfo(user, roomType);

        FruitlabaPlayer fruitlabaPlayer = new FruitlabaPlayer();//新建一个玩家实体类
        fruitlabaPlayer.setUser(user);
        fruitlabaPlayer.setRoomType(roomType);
        fruitlabaPlayer.setEnterMoney(PlayerManager.getPlayerEntity(user).getMoney());
        fruitplayers.put(user.getId(), fruitlabaPlayer);//放入MAP
        String key = BLACK_ROOM_KEY + roomType + user.getId();
        try {
//            int rest = Integer.parseInt(RedisHelper.get(key));
            long rest = Long.parseLong(RedisHelper.get(key));
        } catch (Exception e) {
            RedisHelper.set(key, "0");
        }
    }

    public boolean checkCondition(ServerUser user, Long enterMoneyLimit) {
        if (!PlayerManager.checkItem(user, ItemId.MONEY, enterMoneyLimit)) {
            if (enterMoneyLimit >= 10000) {
                NetManager.sendHintMessageToClient("携带金币不足" + enterMoneyLimit / 10000 + "万，无法进入该房间", user);
            } else {
                NetManager.sendHintMessageToClient("携带金币不足" + enterMoneyLimit + "，无法进入该房间", user);
            }
            return false;
        }
        return true;
    }

    /**
     * I know the code sucks like shit, but please don't fuck me
     */

    /**
     * 玩家离开水果拉霸房间
     * @param user sb
     */
    public void playerLeaveRoom(ServerUser user) {

        // 从房间玩家列表中移除该玩家
        boolean result = fruitRoomUser.remove(user);
        fruitplayers.remove(user.getId());

        int roomType = 0;

        if(fruitRoom1User.contains(user)) roomType = 1;
        else if(fruitRoom2User.contains(user)) roomType = 2;
        else if(fruitRoom3User.contains(user)) roomType = 3;

        if(roomType == 1) fruitRoom1User.remove(user);
        else if(roomType == 2) fruitRoom2User.remove(user);
        else if(roomType == 3) fruitRoom3User.remove(user);
        if (user.isOnline() && result) {
            // 置为游戏大厅内
            user.getEntity().setOnlineState(1);
            // 更新数据库数
            userMapper.update(user.getEntity());
        }
        fruitLaBaSendDataManager.sendLeaveRoomResponse(OseeMsgCode.S_C_FRUITLABA_LEAVE_ROOM_RESPONSE_VALUE, user);
    }

    /**
     * 发送水果拉霸房间的数据 need
     */
    private void sendFruitLaBaRoomInfo(ServerUser user, int roomType) {
        PlayerEnterFruitLaBaRoomResponse.Builder builder = PlayerEnterFruitLaBaRoomResponse.newBuilder();
        // 彩金池中的金币总量
        builder.setPoolGoldNum(getFruitLaBaRewardPoolNum(roomType));
        List<String> list = getPlayerFreeLineAndGold(user.getId());
        builder.setRemaindFreeDrawLineSize(Integer.parseInt(list.get(0)));
        builder.setRemaindFreeDrawGold(Long.parseLong(list.get(1)));
        builder.setRemaindFreeDrawNum(getFreeDrawNum(user.getId()));
        // 发送给连接的玩家
        fruitLaBaSendDataManager.sendRoomInfo(OseeMsgCode.S_C_PLAYER_ENTER_FRUITLABA_ROOM_RESPONSE_VALUE, builder, user);
        // 发送任务信息
        sendTaskInfo(user);
    }

    /**
     * 发送任务奖励信息
     */
    public void sendTaskInfo(ServerUser user) {
        FruitLabaTaskInfoResponse.Builder builder = FruitLabaTaskInfoResponse.newBuilder();
        // 任务完成奖励信息
        List<FruitLaBaRewardInfo> findByUserId = fruitLaBaRewardInfoMapper.findByUserId(user.getId());
        for (FruitLaBaRewardInfo fruitLaBaRewardInfo : findByUserId) {
            FruitLaBaRewardInfoProto.Builder proto = FruitLaBaRewardInfoProto.newBuilder();
            proto.setAchieveNum(fruitLaBaRewardInfo.getAchieveNum());// 达成的数量
            proto.setRewardGold(fruitLaBaRewardInfo.getRewardGold());// 奖励的金币
//            proto.setRewardLottery(fruitLaBaRewardInfo.getRewardLottery());// 奖励的点券
            proto.setWeatherReceive(fruitLaBaRewardInfo.isWeatherReceive());// 是否已经领取
            proto.setRewardId(fruitLaBaRewardInfo.getId());

            builder.addRewardInfos(proto);
        }
//        logger.info("水果拉霸奖励数量：" + findByUserId.size());
        if (getPlayerTotalRotateNum(user.getId()) < 3000) {
            // 下一等级任务奖励信息
            FruitLaBaRewardInfoProto.Builder nextRewardInfo = FruitLaBaRewardInfoProto.newBuilder();
            int nextIndex = getPlayerTotalRotateNum(user.getId()) / roateRewardUnit + 1;
            int nextRotateNum = nextIndex * roateRewardUnit;// 下一等级旋转次数
            int nextRewardGold = nextIndex * rotateRewardGoldUnit;// 下一等级金币
//            int nextRewardLottery = nextIndex * rotateRewardLotteryUnit;// 下一等级点券
            nextRewardInfo.setAchieveNum(nextRotateNum);
            nextRewardInfo.setRewardGold(nextRewardGold);
//            nextRewardInfo.setRewardLottery(nextRewardLottery);
            nextRewardInfo.setWeatherReceive(false);

            builder.setNextReward(nextRewardInfo);
        }

        // 发送给连接的玩家
        fruitLaBaSendDataManager.sendTaskInfo(OseeMsgCode.S_C_FRUITLABA_TASK_INFO_RESPONSE_VALUE, builder, user);
    }

    /**
     * 领取任务奖励
     */
    public void receiveTaskReward(long taskId, ServerUser user) {
        FruitLaBaRewardInfo findById = fruitLaBaRewardInfoMapper.findById(taskId);
        if (findById == null) {
            NetManager.sendErrorMessageToClient("领取奖励失败", user);
            sendTaskInfo(user);
            return;
        }
        // 更新玩家的金币奖券数量
        List<ItemData> itemDatas = new LinkedList<>();
        itemDatas.add(new ItemData(ItemId.MONEY.getId(), findById.getRewardGold()));
//        itemDatas.add(new ItemData(ItemId.LOTTERY.getId(), findById.getRewardLottery()));
        PlayerManager.addItems(user, itemDatas, ItemChangeReason.TASK_FINISH, true);

        OseeExpendLogEntity log = new OseeExpendLogEntity();
        log.setPayType(2);
        log.setUserId(user.getId());
        log.setNickname(user.getNickname());
        log.setMoney(findById.getRewardGold());
//        log.setLottery(findById.getRewardLottery());
        expendLogMapper.save(log);

        OseePlayerEntity player = PlayerManager.getPlayerEntity(user);
        if (player != null) {
            playerMapper.update(player);
        }

        fruitLaBaRewardInfoMapper.delete(findById.getId());

        // 回包
        FruitLabaReceiveTaskResponse.Builder builder = FruitLabaReceiveTaskResponse.newBuilder();
        builder.setRewardId(taskId);
        NetManager.sendMessage(OseeMsgCode.S_C_FRUITLABA_RECEIVE_TASK_RESPONSE_VALUE, builder.build(), user);

        // sendTaskInfo(user);

//        NetManager.sendHintMessageToClient("领取任务奖励：金币*<color=\"red\">" + findById.getRewardGold()
//                + "</color>,点券*<color=\"red\">" + findById.getRewardLottery() + "</color>,成功!", user);
        NetManager.sendHintMessageToClient("领取任务奖励：金币*<color=\"red\">" + findById.getRewardGold()
                + "</color>,成功!", user);
    }

    /**
     * 开始旋转水果机 处理玩家下注结果逻辑
     */
    public void startFruitLaBaRotateDraw(List<Integer> lines, int singleGold, ServerUser user) {
        //检查player是否为空
        OseePlayerEntity player = PlayerManager.getPlayerEntity(user);
        int roomType = 1;
        if(fruitRoom1User.contains(user)) roomType = 1;
        else if(fruitRoom2User.contains(user)) roomType = 2;
        else if(fruitRoom3User.contains(user)) roomType = 3;
        if (player == null) {
            return;
        }
        // 防止出现线不足，钱不足的意外情况
        if (getFreeDrawNum(user.getId()) <= 0) {
//            if (lines.size() <= 0 || singleGold <= 0 || PlayerManager.getPlayerEntity(user).getMoney() < singleGold * lines.size()) {
            if (lines.size() <= 0 || singleGold <= 0 || player.getDragonCrystal() < singleGold * lines.size()) {
                NetManager.sendErrorMessageToClient("下注条件不符，不能开始！", user);
//                logger.info("玩家[{}]下注条件不符，不能开始", user.getNickname());
                return;
            }
        }

        //如果上次玩的金额比本次投注金额更低 重新计算返水参数
//        if(fruitplayers.get(user.getId()).getLastSingleGold()<singleGold){
//            resetReback(fruitplayers.get(user.getId()).getEnterMoney(),singleGold,user.getId());
//        }

        //记录本次投注金额 作为下一次计算的 上次投注金额
        fruitplayers.get(user.getId()).setLastSingleGold(singleGold);
        //记录玩家本次玩之前账户金币余额 用于写进日志记录
//        fruitplayers.get(user.getId()).setPlayBeforeMoney(player.getMoney());
        fruitplayers.get(user.getId()).setPlayBeforeMoney(player.getDragonCrystal());
        //记录玩家投注金币总消耗
        fruitplayers.get(user.getId()).setFruitCost(singleGold * lines.size());
//        logger.info("初始金币：" + player.getMoney() + ",线的条数：" + lines.size() + ",单注金币:" + singleGold);
//        logger.info("初始金币：" + player.getDragonCrystal() + ",线的条数：" + lines.size() + ",单注金币:" + singleGold);

        //旋转前 默认此次不中奖或只中了一级倍率 保底参数需要 -1
        fruitplayers.get(user.getId()).setNeedMinimum(true);

        // 旋转前减少金币
        // 更新玩家的金币数量
        //如果玩家没有免费次数
        if (getFreeDrawNum(user.getId()) <= 0) {
            //玩家账户扣钱
            PlayerManager.addItem(user, ItemId.DRAGON_CRYSTAL, -singleGold * lines.size(), ItemChangeReason.FRUIT_LABA_FEE, true);
            // 保存抽水记录
            OseeCutMoneyLogEntity log = new OseeCutMoneyLogEntity();
            log.setUserId(user.getId());
            log.setGame(GameEnum.FRUIT_LABA.getId());
            log.setType(roomType);
            log.setCutMoney((long) (singleGold * lines.size() * drawPercent / 100));
            agentManager.addActiveMoney(user.getId(), GameEnum.FRUIT_LABA, 0, log.getCutMoney());
            cutMoneyLogMapper.save(log);
            playerMapper.update(player);
            // 更新彩金池币量
            handleReward((long) (singleGold * lines.size() * percent / 1000), roomType);
        } else { //如果有免费次数
            List<String> list = getPlayerFreeLineAndGold(user.getId());
            //获得免费抽奖下注条件 不符合则返回
            if (lines.size() != Integer.parseInt(list.get(0)) || singleGold != Long.parseLong(list.get(1))) {
                NetManager.sendErrorMessageToClient("您有免费次数，下注条件不符，不能开始！", user);
                return;
            }
            //减少免费抽奖次数
            reduceFreeDrawNum(user.getId(), 1);
            fruitplayers.get(user.getId()).setFree(true);
        }

//        long  REBACK_LINE = fruitplayers.get(user.getId()).getREBACK_LINE();
//        //  花掉的钱 已经大于 返水参数线 则返水
//        if((fruitplayers.get(player.getUserId()).getEnterMoney()-player.getMoney())> REBACK_LINE && fruitplayers.get(player.getUserId()).getReback()>0)//如果玩家扣除下注的钱之后 低于返水线 且 返水次数大于 0
//        {
//            logger.info("玩家下注投入金币"+(fruitplayers.get(player.getUserId()).getEnterMoney()-player.getMoney())+">返水线"+REBACK_LINE);
//            fruitplayers.get(user.getId()).setREBACK_FLAG(true);//对应玩家设为必赢
//        }

        // 服务器根据输赢中奖几率生成水果点
        List<Integer> fishSpots = getControllerFishSpot(lines, singleGold, player, roomType);

        // 对得到的水果进行数据处理
        FruitLaBaStartRunResponse.Builder builder = getCheckResult(fishSpots, lines, singleGold, user, player, roomType);

       fruitplayers.get(user.getId()).setFree(false);
        //保底参数是否需要-1
        if (fruitplayers.get(user.getId()).getNeedMinimum() && lines.size() >= 5) {
            fruitplayers.get(user.getId()).mininumConsume(); // 保底参数 -1 （未命中二级以上倍率）
        }

        // 免费抽奖次数为0时就不赋值
        if (getFreeDrawNum(user.getId()) > 0) {
            List<String> list = getPlayerFreeLineAndGold(user.getId());
            builder.setRemaindFreeDrawLineSize(Integer.parseInt(list.get(0)));
            builder.setRemaindFreeDrawGold(Long.parseLong(list.get(1)));
        }

        // 给玩家发送旋转结果
        fruitLaBaSendDataManager.sendRotateResultData(OseeMsgCode.S_C_FRUITLABA_START_RUN_RESPONSE_VALUE, builder, user);

        // 向所有玩家发送奖池数量
        // List<ServerUser> users=UserContainer.getActiveServerUsers();
        // 向在水果拉霸房间内的玩家发送奖池数量
        List<ServerUser> users = new ArrayList<>();
        if(roomType == 1) {
            rewardTotal = rewardTotal1;
            users = fruitRoom1User;
        } else if(roomType == 2) {
            rewardTotal = rewardTotal2;
            users = fruitRoom2User;
        } else if(roomType == 3) {
            rewardTotal = rewardTotal3;
            users = fruitRoom3User;
        }
        fruitLaBaSendDataManager.sendFruitLaBaRewardPoolNum(OseeMsgCode.S_C_FRUITLABA_REWARD_POOL_GOLDNUM_RESPONSE_VALUE, users, rewardTotal);

        // 更新任务
        long rotateTotalNum = getPlayerTotalRotateNum(user.getId());
        if (rotateTotalNum % roateRewardUnit == 0) {// 为100的整数倍才处理并发送
            if (getPlayerTotalRotateNum(user.getId()) <= 3000) {
                FruitLaBaRewardInfo fruitLaBaRewardInfo = new FruitLaBaRewardInfo();
                int index = getPlayerTotalRotateNum(user.getId()) / roateRewardUnit;
                int rotateNum = index * roateRewardUnit;// 这一等级旋转次数
                int rewardGold = index * rotateRewardGoldUnit;// 这一等级金币
//                int rewardLottery = index * rotateRewardLotteryUnit;// 这一等级点券
                fruitLaBaRewardInfo.setUserId(user.getId());
                fruitLaBaRewardInfo.setAchieveNum(rotateNum);
                fruitLaBaRewardInfo.setRewardGold(rewardGold);
//                fruitLaBaRewardInfo.setRewardLottery(rewardLottery);
                fruitLaBaRewardInfo.setWeatherReceive(false);
                fruitLaBaRewardInfoMapper.save(fruitLaBaRewardInfo);
            }
            // 发送任务信息
            sendTaskInfo(user);
        }
    }

    /**
     * 对得到的鱼进行数据处理 取得的鱼+下注的线+单注金币+玩家
     */
    public FruitLaBaStartRunResponse.Builder getCheckResult(List<Integer> fishSpots, List<Integer> lines, int singleGold, ServerUser user, OseePlayerEntity player, int roomType) {
        StringBuilder winInfo = new StringBuilder();//记录中奖详细信息
        long winTotalGold = 0;// 输赢总金币
        boolean weatherNumSeven = false;
        List<FruitLaBaSpotProto> numSevenLinePoints = new ArrayList<>();// 数字7 点
        FruitLaBaStartRunResponse.Builder builder = FruitLaBaStartRunResponse.newBuilder();
//        builder.setBeforTotalGoldNum(player.getMoney());// 旋转之前的金币
        builder.setBeforTotalGoldNum(player.getDragonCrystal());// 旋转之前的龙晶
        builder.addAllSpots(fishSpots);// 鱼的点
        // 进行下注线的判断(注意：线是从1开始数，不是从0开始!!)
        for (Integer line : lines) {
            int winFishId = 0;// 如果赢，则保存连续鱼的id 注意：鱼的id是从1开始
            int winFishNum = 0;// 如果赢，则保存 连续出现鱼的数量 如：(1,1,1,1,2)则鱼连续的数量为4，从1开始，有4个连续的1
            Integer[][] thisSpots = spotLines[line - 1];// 当前一条下注线的点
            for (Integer[] localSpot : thisSpots) {// 对每个 点 顺序进行检测
                int infishSpotsSeat = localSpot[0] * rankNum + localSpot[1];// 得到在得到 鱼的点(在界面上显示的鱼) 中的位置
                int fishId = fishSpots.get(infishSpotsSeat);// 得到在得到 鱼的点(在界面上显示的鱼) 中的鱼的id

                if (winFishId == fishId) {// id与上一个记录的鱼相同
                    winFishNum++;
                } else if (fishId == fruitIds.get(0)) {// wild
                    if (winFishId == fruitIds.get(1) || winFishId == fruitIds.get(2)) {// bonus和数字7，wild不能变
                        break;
                    }
                    winFishNum++;
                } else if (winFishId == 0) {// 说明还没有开始 鱼id 记录数据,让检测wild在前面的原因是，防止第一个就是wild
                    if (winFishNum > 0 && (fishId == fruitIds.get(1) || fishId == fruitIds.get(2))) {// bonus和数字7，wild不能变
                        break;
                    }
                    winFishId = fishId;
                    winFishNum++;
                } else {// 不能继续下去了，在这里断片
                    break;
                }
            }

            // 如果条数满足奖励条件,则增加返回的数据
            if (winFishId == fruitIds.get(3) && winFishNum >= 2) {// 是香蕉
                FruitLaBaWinLineDataProto.Builder proto = FruitLaBaWinLineDataProto.newBuilder();
                Integer multiple = fruitMultiple.get(winFishId).get(winFishNum - 1);// 获得当前线赢取的倍数
                long currentWinGold = multiple * singleGold;// 当前线赢取的金币
                winTotalGold += currentWinGold;// 加入总金币赢取数量
                //test
                winInfo.append(line).append("线").append(multiple).append("倍 ");
                proto.setWinNum(multiple);// 当前线赢取的倍数
                proto.setWinType(0);// 中奖类型
                proto.setLineGold(currentWinGold);// 赢取金币
                proto.setLineMultiple(multiple);// 中奖倍数
                List<FruitLaBaSpotProto> winLinePoints = new ArrayList<>();// 点
                for (int i = 0; i < thisSpots.length && i < winFishNum; i++) {
                    FruitLaBaSpotProto.Builder protoSpot = FruitLaBaSpotProto.newBuilder();
                    protoSpot.setRankSpot(thisSpots[i][0]);
                    protoSpot.setVerticalSpot(thisSpots[i][1]);
                    winLinePoints.add(protoSpot.build());
                }
                proto.addAllWinLinePoints(winLinePoints);
                proto.setLineName(line);
                builder.addWinLines(proto);
                //如果是免费摇奖 则没有下注金额和抽水
                if (fruitplayers.get(user.getId()).getFree()) {
                    handleLineGold((roomType - 1) * 9 + line, -currentWinGold);// 更新redis中这条线输赢金币
                } else {
                    handleLineGold((roomType - 1) * 9 + line, (long) singleGold - (long) (singleGold * (percent + drawPercent * 10) / 1000) - currentWinGold);
                }
            } else if (winFishNum >= 3) {// 其他水果

                FruitLaBaWinLineDataProto.Builder proto = FruitLaBaWinLineDataProto.newBuilder();
                Integer multiple = fruitMultiple.get(winFishId).get(winFishNum - 1);// 获得当前线赢取的倍数
                long currentWinGold = (long)multiple * singleGold;// 当前线赢取的金币
                winTotalGold += currentWinGold;// 加入总金币赢取数量
                winInfo.append(line).append("线").append(multiple).append("倍 ");
                //test
                //小黑屋
                String key = BLACK_ROOM_KEY + roomType + player.getUserId();
                long rest = Long.parseLong(RedisHelper.get(key));

                // tp 从Redis中获取金币数
                String tp_str = RedisHelper.get(BLACK_ROOM_TP_KEY + roomType);
                long tp = Long.parseLong(tp_str.equals("") ? "50000000" : tp_str);

                if (multiple >= 70) { // 命中四级倍率以上就播报游走字幕
                    if (currentWinGold >= 1000000) {
                        String info = String.format(
                                AutoWanderSubtitle.TEMPLATES[ThreadLocalRandom.current().nextInt(6, 8)],
                                user.getNickname(), multiple, currentWinGold / 10000
                        );
                        sendWanderSubtitle(info);
                    }
                    if(winTotalGold >= tp) {
                        long value = winTotalGold;
                        if(rest > 0) value += rest;
                        RedisHelper.set(key, value + "");//设置小黑屋参数 为 中奖金额
                    }
                }
                proto.setWinNum(multiple);// 当前线赢取的倍数
                proto.setWinType(0);// 中奖类型
                proto.setLineGold(currentWinGold);// 赢取金币
                proto.setLineMultiple(multiple);// 中奖倍数
                List<FruitLaBaSpotProto> winLinePoints = new ArrayList<>();// 点
                for (int i = 0; i < thisSpots.length && i < winFishNum; i++) {
                    FruitLaBaSpotProto.Builder protoSpot = FruitLaBaSpotProto.newBuilder();
                    protoSpot.setRankSpot(thisSpots[i][0]);
                    protoSpot.setVerticalSpot(thisSpots[i][1]);
                    winLinePoints.add(protoSpot.build());
                }
                proto.addAllWinLinePoints(winLinePoints);
                proto.setLineName(line);
                builder.addWinLines(proto);

                if (winFishId == fruitIds.get(2) && winFishNum >= 3) { // 数字7
                    weatherNumSeven = true;// 代表有连续的三个7
                    numSevenLinePoints.addAll(winLinePoints);
                }
                // 更新redis中这条线输赢金币
                if (fruitplayers.get(user.getId()).getFree()) {
                    handleLineGold((roomType - 1) * 9 + line, -currentWinGold);
                } else {
                    handleLineGold((roomType - 1) * 9 + line, (long) singleGold - (long) (singleGold * (percent + drawPercent * 10) / 1000) - currentWinGold);
                }
            } else {
                // 这条线没有赢得金币
                if (!fruitplayers.get(user.getId()).getFree()) { // 免费抽奖 没有抽水 也没有进库存
                    handleLineGold((roomType - 1) * 9 + line, (long) singleGold - (long) (singleGold * (percent + drawPercent * 10) / 1000));
                }
                String key = BLACK_ROOM_KEY + roomType + player.getUserId();
                long rest = Long.parseLong(RedisHelper.get(key));
                if (rest > 0) {//否则 如果小黑屋参数不为0 则从中扣除下注的金额
                    rest = rest - singleGold;
                    rest = rest < 0 ? 0 : rest;
                    RedisHelper.set(key, rest + "");
                }
            }
        }
        //小黑屋参数提示

        // 对fishSpots进行检测，检测特殊鱼带来的奖励
        FruitLaBaWinSpecialRewardProto.Builder specialReward_bonus = checkSpecialReward_bonus(fishSpots, lines,
                singleGold, user);
        if (specialReward_bonus != null) {
            winInfo.append("免费+5 ");
            builder.addSpecialRewards(specialReward_bonus);
        }
        FruitLaBaWinSpecialRewardProto.Builder specialReward_NumSeven = checkSpecialReward_NumSeven(numSevenLinePoints,
                singleGold, lines.size(), weatherNumSeven, roomType);
        if (specialReward_NumSeven != null) {
            builder.addSpecialRewards(specialReward_NumSeven);
            winInfo.append("彩金池").append(specialReward_NumSeven.getWinNum());
            winTotalGold += specialReward_NumSeven.getWinNum();
        }


        /**
         *  Edit by 2019-09-28
         */
        // 当前中奖总数（扣除押注）
        long total = winTotalGold - lines.size() * singleGold;
        // 添加历史累计赢奖总数
        String historyPrize_str = RedisHelper.get(REDIS_PLAYER_HISTORY_PRIZE_KEY + roomType + user.getId());
        long historyPrize = Long.parseLong(historyPrize_str.equals("") ? "0" : historyPrize_str);
        historyPrize += total;
        RedisHelper.set(REDIS_PLAYER_HISTORY_PRIZE_KEY + roomType + user.getId(), String.valueOf(historyPrize));
        // 添加当天累计赢奖总数
        String todayPrize_str = RedisHelper.get(REDIS_PLAYER_TODAY_PRIZE_KEY + roomType + user.getId());
        long todayPrize = Long.parseLong(todayPrize_str.equals("") ? "0" : todayPrize_str);
        todayPrize += total;
        RedisHelper.set(REDIS_PLAYER_TODAY_PRIZE_KEY + roomType + user.getId(), String.valueOf(todayPrize));


        // 特殊线赢取金币+普通线赢取金币
        builder.setWinGold(winTotalGold);

        // 增加一次旋转次数,获得总旋转次数和免费旋转次数
        addPlayerRotateNum(user.getId(), 1);
        builder.setLocalRotateNum(getPlayerTotalRotateNum(user.getId()));// 当前总旋转次数
        builder.setRemaindFreeDrawNum(getFreeDrawNum(user.getId()));// 剩余免费旋转次数

        // 水果拉霸日志 ljy
        OseeFruitRecordLogEntity fruitlog = new OseeFruitRecordLogEntity();
        fruitlog.setPlayBeforeMoney(fruitplayers.get(user.getId()).getPlayBeforeMoney());//玩之前的金币
        fruitlog.setCost(-1 * fruitplayers.get(user.getId()).getFruitCost());//下注金额
        fruitlog.setTotalWin(winTotalGold);//中奖金额
        fruitlog.setInfo(winInfo.toString());//中奖详情
        fruitlog.setLineNum(lines.size());//下注条数
        fruitlog.setMoney(-1 * fruitplayers.get(user.getId()).getFruitCost() + winTotalGold);//金额变动
//        fruitlog.setPlayAfterMoney(PlayerManager.getPlayerEntity(user).getMoney() + winTotalGold);//玩之后的金币
        fruitlog.setPlayAfterMoney(PlayerManager.getPlayerEntity(user).getDragonCrystal() + winTotalGold);//玩之后的龙晶
        fruitlog.setPlayerId(player.getUserId());
        fruitlog.setNickname(user.getEntity().getNickname());
        fruitRecordLogMapper.save(fruitlog);

        // 更新玩家的金币数量
//        PlayerManager.addItem(user, ItemId.MONEY, winTotalGold, ItemChangeReason.FRUIT_LABA_WIN, true);
        // 更新玩家的龙晶数量
        PlayerManager.addItem(user, ItemId.DRAGON_CRYSTAL, winTotalGold, ItemChangeReason.FRUIT_LABA_WIN, true);
        playerMapper.update(player);

//        builder.setAfterTotalGoldNum(player.getMoney());// 旋转逻辑完成后的金币
        builder.setAfterTotalGoldNum(player.getDragonCrystal());// 旋转逻辑完成后的龙晶

        return builder;
    }

    /**
     * 检查 bonus 特殊奖励
     */
    public FruitLaBaWinSpecialRewardProto.Builder checkSpecialReward_bonus(List<Integer> fishSpots, List<Integer> lines, int singleGold, ServerUser user) {
        int winNum = 0;
        boolean weatherWin = false;
        List<FruitLaBaSpotProto> winLinePoints = new ArrayList<>();// 特殊奖励点

        // 对所有点进行顺序检查
        for (int startCheckNum = 0; startCheckNum <= verticalNum - 3; startCheckNum++) {
            int seriesNum = 0;// 连续列存在的数量
            for (int i = startCheckNum; i < verticalNum; i++) {// 连续竖列查找
                int num = 0;// 用于检查一竖是否有bonus
                for (int j = 0; j < rankNum; j++) {// 横
                    Integer fishId = fishSpots.get(i * rankNum + j);
                    if (fishId.equals(fruitIds.get(1))) { // bonus
                        seriesNum++;
                        break;
                    }
                    num++;
                }
                // 这一竖,没有bonus,开始下一个
                if (num >= rankNum) {
                    break;
                }
            }
            if (seriesNum >= 3) {// 连续三个及以上
                winNum += (seriesNum - 2);
                weatherWin = true;
                break;// 说明有特殊奖励点，不再查找
            }
        }

        if (weatherWin) {
            // 取得所有特殊奖励点
            for (int i = 0; i < fishSpots.size(); i++) {
                if (fishSpots.get(i).equals(fruitIds.get(1))) {// 如果是bonus，就加入进去
                    int rankSeat = i % rankNum;// 横坐标
                    int verticalSeat = i / rankNum;// 纵坐标
                    FruitLaBaSpotProto.Builder protoSpot = FruitLaBaSpotProto.newBuilder();
                    protoSpot.setRankSpot(verticalSeat);
                    protoSpot.setVerticalSpot(rankSeat);
                    winLinePoints.add(protoSpot.build());
                }
            }

            FruitLaBaWinSpecialRewardProto.Builder proto = FruitLaBaWinSpecialRewardProto.newBuilder();
            proto.setSpecialType(1);// bonus，获得奖励
            //proto.setWinNum(winNum * 5);// (特殊奖励数量,每中一个*5)
            proto.setWinNum(5);// 需求改为只加五次
            proto.addAllSpecialSpots(winLinePoints);
            // 增加免费抽奖次数
            //addFreeDrawNum(user.getId(), winNum * 5);
            addFreeDrawNum(user.getId(), 5);// 需求改为只加五次
            // 保存抽奖时的线与金币
            setPlayerFreeLineAndGold(lines.size(), singleGold, user.getId());
            return proto;

        }

        return null;
    }

    /**
     * 检查 数字7 特殊奖励
     */
    public FruitLaBaWinSpecialRewardProto.Builder checkSpecialReward_NumSeven(
            List<FruitLaBaSpotProto> numSevenLinePoints, int singleGold, int lineSize, boolean weatherNumSeven, int roomType) {
        if (weatherNumSeven) {// 有三个7以上
            if (singleGold * lineSize >= numSevenLimitCheckGold) {// 下注金币足够50000
                FruitLaBaWinSpecialRewardProto.Builder proto = FruitLaBaWinSpecialRewardProto.newBuilder();
                proto.addAllSpecialSpots(numSevenLinePoints);
                proto.setSpecialType(2);// 类型: 数字7

                switch (roomType) {
                    case 1:rewardTotal = rewardTotal1;break;
                    case 2:rewardTotal = rewardTotal2;break;
                    case 3:rewardTotal = rewardTotal3;break;
                }
                // 彩金池中的金币数量
                long fruitLaBaRewardPoolNum = rewardTotal * 2 / 100;// 彩金池2%的金币
                proto.setWinNum(fruitLaBaRewardPoolNum);
                // 减少彩金池中的金币
                handleReward(-fruitLaBaRewardPoolNum, roomType);
                return proto;
            }
        }
        return null;
    }

    /**
     * 随机取得planeFishNum个点(每个点从所有水果id中取)
     */
    public List<Integer> getRandomFishSpot() {
        List<Integer> spots = new ArrayList<>();
        for (int i = 0; i < planeFishNum; i++) {
            spots.add(fruitIds.get(new Random().nextInt(fruitIds.size())));
        }
        spots.set(1, 3);
        spots.set(4, 3);// 7
        spots.set(7, 3);
        spots.set(10, 3);
        spots.set(13, 3);
        spots.set(0, 2);// bouns
        spots.set(3, 2);
        spots.set(6, 2);
        return spots;
    }

    /**
     * <strong>服务器控制</strong>取得<strong>planeFishNum</strong>个点(每个点从所有水果id中取)
     * 根据[线]与[单注金币]和[玩家] 来控制
     */
    public List<Integer> getControllerFishSpot(List<Integer> lines, int singleGold, OseePlayerEntity player, int roomType) {
        Integer[] fruitSpot = new Integer[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        List<Integer> fruitSet = new ArrayList<>(Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        //根据概率随机生成中奖线的条数
        int WinLinesNum = randomGenerateLine();
        WinLinesNum = checkWinLinesNum(WinLinesNum, player.getUserId(), roomType);
//        if(fruitplayers.get(player.getUserId()).isREBACK_FLAG()==true)//返水本次必定中奖 所以一定要有条线
//        {
//            while(WinLinesNum==0)
//                WinLinesNum = randomGenerateLine();
//        }
//        else
        if (fruitplayers.get(player.getUserId()).getMinimum() == 0) { //如果保底参数为0 则需要至少有一条线
            while (WinLinesNum == 0)
                WinLinesNum = randomGenerateLine();
        }
        if (WinLinesNum == 0) {//如果一条线都没有中 则提升中奖率
            //低于五条线的时候的保底
            if (lines.size() < 5) {
                long baodi5 = fruitplayers.get(player.getUserId()).getBaodi5();
                fruitplayers.get(player.getUserId()).setBaodi5(baodi5 + lines.size() * singleGold);//累计
            }
            increaseRateAdd(lines.size(), player.getUserId());
        } else {
            fruitplayers.get(player.getUserId()).setBaodi5(0);
        }

        //定义数组 分别代表九条线
        Integer[] Lines = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        WinLinesNum = checkWinLinesNum(WinLinesNum, player.getUserId(), roomType);
        //随机从这九条线中选择WinLinesNum条
        List<Integer> winLines = Arrays.asList(randomSelect(Lines, WinLinesNum));
        //累加计算全服水果拉霸库存
        long fruitPool = getAllPoolGold(roomType);
        //如果总库存为正数 且该次不为免费抽奖 启用命中率提升参数
        if (fruitPool > 0 && !fruitplayers.get(player.getUserId()).getFree()) {
            for (Integer winLine : winLines) {
                //分别为选中的中奖线生成倍率 并放入Map中
                int lineRate = randomGenerateRateByAddRate(lines.size(), player.getUserId(), singleGold, roomType);
                int count = 0;
                while (lineRate == fruitplayers.get(player.getUserId()).getLastLineRate()) {//尽量使连续两条线的中奖倍率不同  避免同屏水果一样 多出意外中奖
                    lineRate = randomGenerateRateDefault(lines.size(), player.getUserId(), singleGold, roomType);
                    count++;
                    if(count > 2) break;
                }
                fruitplayers.get(player.getUserId()).setLastLineRate(lineRate);
                fruitplayers.get(player.getUserId()).getWin_Multiple_MAP().put(winLine, lineRate);
            }
        } else {//否则 不使用命中提升参数
            for (Integer winLine : winLines) {
                int lineRate = randomGenerateRateDefault(lines.size(), player.getUserId(), singleGold, roomType);
                int count = 0;
                while (lineRate == fruitplayers.get(player.getUserId()).getLastLineRate()) {//尽量使连续两条线的中奖倍率不同 避免同屏水果一样 多出意外中奖
                    lineRate = randomGenerateRateDefault(lines.size(), player.getUserId(), singleGold, roomType);
                    count++;
                    if(count > 2) break;
                }
                fruitplayers.get(player.getUserId()).setLastLineRate(lineRate);
                fruitplayers.get(player.getUserId()).getWin_Multiple_MAP().put(winLine, lineRate);
            }
        }

        for (Integer winLine : winLines) {
            Integer[][] thisSpots = spotLines[winLine - 1];// 当前一条下注线的点
            Integer fruitNum = randomFruitId(fruitplayers.get(player.getUserId()).getWin_Multiple_MAP().get(winLine), player.getUserId());//传入当前线设定的倍率 得到返回的水果个数 以及将中奖水果id存入player实体中 从实体中取值
            int fruitId = fruitplayers.get(player.getUserId()).getFruitId();//从实体中取值 得到中奖水果的Id
            for (int i = 0; i < fruitNum; i++) {
                int fruitSpotSeat = thisSpots[i][0] * rankNum + thisSpots[i][1]; // 按原来的坐标算法
//                int fruitSpotSeat = (2 - thisSpots[i][1]) * verticalNum + thisSpots[i][0]; // 即 位置 = 坐标 比如第8条线第一个点 (0,1) 序列中坐标为5 即(2-y)*5+x =5 (0开始)
                if (fruitSpot[fruitSpotSeat] == 0) {
                    fruitSpot[fruitSpotSeat] = fruitId; // 将该位置标记为fruitId
                    fruitSet.remove(Integer.valueOf(fruitId));
                } else if (fruitSpotSeat > 2) { // 第一列不出现wild 策划规则制定 否则wild太多 不该出现的中奖概率极大提升
                    fruitSpot[fruitSpotSeat] = 1; //否则设置为wild
                    fruitSet.remove(Integer.valueOf(1));
                } // 否则就不改

            }
        }
//        System.out.println(fruitSet);//fruitSet 中 移除中奖的水果 即剩下随机生成的水果不能与已经中奖的水果相同 否则可能出现中奖倍数意外增大

        //test
//        logger.info("转换前：");
//        for (int i = rankNum - 1; i >= 0; i--) {
//            for (int j = 0; j < verticalNum; j++) {
//                System.out.print("  " + fruitSpot[j * rankNum + i]);
//            }
//            System.out.println();
//        }

        //随机填充0
        int k = 0;
        Integer[][] temp = new Integer[6][6];
        Integer[] numlist = new Integer[16];
        for (int i = rankNum - 1; i >= 0; i--) {
            for (int j = 0; j < verticalNum; j++) {
                temp[i][j] = fruitSpot[j * rankNum + i];
            }
        }

        for (int i = rankNum - 1; i >= 0; i--) {
            for (int j = 0; j < verticalNum; j++) {
                if (temp[i][j] == 0) {
                    temp[i][j] = fruitSet.get(new Random().nextInt(fruitSet.size()));
                    if (i == 0) {
                        if (j != 0) {
                            while (temp[i][j].equals(temp[i][j - 1]) || temp[i][j].equals(temp[i + 1][j - 1]) ||
                                    temp[i][j].equals(temp[i + 1][j + 1])) { //从下往上 第一行 除了第一个的其他位置 需要考虑 左边 左上 和 右上
                                temp[i][j] = fruitSet.get(new Random().nextInt(fruitSet.size()));
                            }
                        } else {
                            while (temp[i][j].equals(temp[i + 1][j]) || temp[i][j].equals(temp[i + 1][j + 1]) ||
                                    temp[i][j].equals(temp[i][j + 1])) { // (0,0)处 考虑 上面那个 和右上角 和 右边的
                                temp[i][j] = fruitSet.get(new Random().nextInt(fruitSet.size()));
                            }
                        }
                    } else {
                        if (j != 0) {
                            while (temp[i][j].equals(temp[i][j - 1]) || temp[i][j].equals(temp[i - 1][j - 1]) ||
                                    temp[i][j].equals(temp[i + 1][j - 1]) || temp[i][j].equals(temp[i][j + 1]) ||
                                    temp[i][j].equals(temp[i - 1][j + 1]) || temp[i][j].equals(temp[i + 1][j + 1])) { //第二行第三行的除了第一列其他位置 考虑左 左上左下 右边右上 和 右下
                                temp[i][j] = fruitSet.get(new Random().nextInt(fruitSet.size()));
                            }
                        } else {
                            while (temp[i][j].equals(temp[i][j + 1]) || temp[i][j].equals(temp[i + 1][j + 1]) ||
                                    temp[i][j].equals(temp[i - 1][j + 1])) { //第二行第三行 的 第一列 只考虑 右边 右上 和 右下
                                temp[i][j] = fruitSet.get(new Random().nextInt(fruitSet.size()));
                            }
                        }
                    }
                }
            }
        }
        for (int i = rankNum - 1; i >= 0; i--) {
            for (int j = 0; j < verticalNum; j++) {
                fruitSpot[j * rankNum + i] = temp[i][j];
            }
        }

//        logger.info("转换后：");
//        for (int i = rankNum - 1; i >= 0; i--) {
//            for (int j = 0; j < verticalNum; j++) {
//                System.out.print("  " + fruitSpot[j * rankNum + i]);
//            }
//            System.out.println();
//        }

        // 随机填充
        return Arrays.asList(fruitSpot);
    }

    /**
     *
     * 检查玩家是否进入小黑屋，
     * 如果进入小黑屋且中奖线数量大于等于bp，则将其中奖线数量设置为0；
     * 否则不对中奖线数量进行改变
     *
     * @param linesNum 生成的中奖线数量
     * @param id 玩家的id
     * @return 返回中奖线数量
     */
    public int checkWinLinesNum(int linesNum, long id, int roomType) {
        String key = BLACK_ROOM_KEY + roomType + id;
        long rest = Long.parseLong(RedisHelper.get(key));

        /**
         *  玩家中奖上限处理 达到上限 dxj OR lxj 则禁止高倍率
         */
        String todayPrize_str = RedisHelper.get(REDIS_PLAYER_TODAY_PRIZE_KEY + roomType + id);
        String historyPrize_str = RedisHelper.get(REDIS_PLAYER_HISTORY_PRIZE_KEY + roomType+ id);
        String dxj_str = RedisHelper.get(REDIS_PRIZE_DXJ_KEY + roomType);
        String lxj_str = RedisHelper.get(REDIS_PRIZE_LXJ_KEY + roomType);
        String xs_str = RedisHelper.get(REDIS_PRIZE_XS_KEY + roomType);
        String fxs_str = RedisHelper.get(REDIS_PRIZE_FXS_KEY + roomType);
        long todayPrize = Long.parseLong(todayPrize_str.equals("") ? "0" : todayPrize_str);
        long historyPrize = Long.parseLong(historyPrize_str.equals("") ? "0" : historyPrize_str);
        long dxj = Long.parseLong(dxj_str.equals("") ? "100000000" : dxj_str);
        long lxj = Long.parseLong(lxj_str.equals("") ? "1000000000" : lxj_str);
        int xs = Integer.parseInt(xs_str.equals("") ? "2" : xs_str);
        int fxs = Integer.parseInt(fxs_str.equals("") ? "2" : fxs_str);

        if((todayPrize > dxj ||  historyPrize > lxj)&& linesNum >= xs) {
            linesNum = nonAverageRandom(xs);
            if (linesNum == 0) linesNum = 1;
        }
        String fdxj_str = RedisHelper.get(REDIS_PLAYER_FDXJ_KEY + roomType);
        String flxj_str = RedisHelper.get(REDIS_PLAYER_FLXJ_KEY + roomType);
        long fdxj = Long.parseLong(fdxj_str.equals("") ? "-50000000" : fdxj_str);
        long flxj = Long.parseLong(flxj_str.equals("") ? "-500000000" : flxj_str);
        if((todayPrize < fdxj || historyPrize < flxj) && linesNum >= fxs) {
            linesNum = nonAverageRandom(fxs);
            if(linesNum == 0) linesNum = 1;
        }

        // 判断用户是否进入小黑屋
        String bp_str = RedisHelper.get(BLACK_ROOM_BP_KEY + roomType);
        int bp = Integer.parseInt(bp_str.equals("") ? "4" : bp_str);
        if(rest > 0 && linesNum >= bp) {
            // logger.info("en");
            linesNum = 0;
        }

        String ag_str = RedisHelper.get(REDIS_PLAYER_AG_KEY + id);
        int ag = Integer.parseInt(ag_str.equals("") ? "63" : ag_str);
        if(ag == 1) {
            String rateOne_str = RedisHelper.get(REDIS_PLAYER_RATE_ONE_KEY + id);
            int rateOne = Integer.parseInt(rateOne_str.equals("") ? "511" : rateOne_str);
            if(rateOne == 0) return 0;
        }

        return linesNum;
    }

    /**
     * 根据概率表生成中奖线的倍数 原版 无提升参数  保底还是有的
     */
    public int randomGenerateRateDefault(int linesSize, long id, long singleGold, int roomType) {
        double[] base = new double[10];
        int rate = 1;//定义倍率 默认为 1
        int index;//随机选择某个倍率等级中的一种倍率 即下标
        //首先判断保底时应该生成的倍率 当保底参数为0时，则本次至少刷新有一条中奖线获得二级倍率中任意一个倍数奖励，并重置保底参数
        if (fruitplayers.get(id).getMinimum() == 0) {
            rate = 1;
            // index = ThreadLocalRandom.current().nextInt(rateMap[rate - 1].length); //下标从0开始 所以rate-1 从等级倍率的个数n中随机一个 即 0-n-1
            index = nonAverageRandom(rateMap[rate - 1].length);
            resetMinimum(id);//重置保底参数
            fruitplayers.get(id).setNeedMinimum(false);
            return rateMap[rate - 1][index];//直接返回奖励倍数
        } else if (fruitplayers.get(id).getBaodi5() >= fruitplayers.get(id).getLastSingleGold() * 30) { //5条线以下的保底
            rate = 1;
            // index = ThreadLocalRandom.current().nextInt(rateMap[rate - 1].length); //下标从0开始 所以rate-1 从等级倍率的个数n中随机一个 即 0-n-1
            index = nonAverageRandom(rateMap[rate - 1].length);
            fruitplayers.get(id).setBaodi5(0);//重置为0
            return rateMap[rate - 1][index];//直接返回奖励倍数
        }
        double random = new Random().nextDouble() * 100;
        //一级倍率 75% 二级倍率 20% 三级倍率 4% 四级倍率0.6% 五级倍率0.3% 六级倍率 0.1%
        if (random >= base[6 - 1]) { //一级倍率
            rate = 1;
        } else if (random >= base[6 - 2]) {//二级倍率
            rate = 2;
            fruitplayers.get(id).setNeedMinimum(false);
            resetMinimum(id);//重置保底参数
        } else if (random >= base[6 - 3]) {//三级倍率
            rate = 3;
            fruitplayers.get(id).setNeedMinimum(false);
            resetMinimum(id);//重置保底参数
        } else if (random >= base[6 - 4]) { //四级倍率
            rate = 4;
            fruitplayers.get(id).setNeedMinimum(false);
            resetMinimum(id);//重置保底参数
        } else if (random >= base[6 - 5]) {//五级倍率
            rate = 5;
            fruitplayers.get(id).setNeedMinimum(false);
            resetMinimum(id);//重置保底参数
        } else {  //六级倍率
            rate = 6;
            fruitplayers.get(id).setNeedMinimum(false);
            resetMinimum(id);//重置保底参数
        }

        rate = checkRate(rate, id, roomType);

        index = nonAverageRandom(rateMap[rate - 1].length);

        String rateOne_str = RedisHelper.get(REDIS_PLAYER_RATE_ONE_KEY + id);
        String ag_str = RedisHelper.get(REDIS_PLAYER_AG_KEY + id);
        int rateOne = Integer.parseInt(rateOne_str.equals("") ? "511" : rateOne_str);
        int ag = Integer.parseInt(ag_str.equals("") ? "63" : ag_str);
        if(ag == 1 && rate == 1 && (rateOne & (1 << index)) == 0) {
            for(int i = 0; i < rateMap[0].length; i++) {
                if((rateOne & 1 << i) != 0) {
                    return rateMap[rate - 1][i];
                }
            }
        }
        return rateMap[rate - 1][index];
    }


    public int checkRate(int rate, long id, int roomType) {
        /**
         * Edit 对库存进行判断
         */
        String k0_str = RedisHelper.get(REDIS_Stock_K0_KEY + roomType);
        String k1_str = RedisHelper.get(REDIS_Stock_K1_KEY + roomType);
        String k2_str = RedisHelper.get(REDIS_Stock_K2_KEY + roomType);
        String k3_str = RedisHelper.get(REDIS_Stock_K3_KEY + roomType);
        String k4_str = RedisHelper.get(REDIS_Stock_K4_KEY + roomType);
        long k0 = Long.parseLong(k0_str.equals("") ? "50000000" : k0_str);
        long k1 = Long.parseLong(k1_str.equals("") ? "200000000" : k1_str);
        long k2 = Long.parseLong(k2_str.equals("") ? "500000000" : k2_str);
        long k3 = Long.parseLong(k3_str.equals("") ? "1000000000" : k3_str);
        long k4 = Long.parseLong(k4_str.equals("") ? "0" : k4_str);
        long allPoolGold = getAllPoolGold(roomType);
        if(allPoolGold < k4 && rate >= 2) {
            rate = 1;       // 库存小于k4为负 禁用2级及以上倍率
        } else if(allPoolGold < k0 && rate >= 3) {
            rate = nonAverageRandom(3);
            if (rate == 0) rate = 1; // 库存小于k0 且 倍率>=3级，则将倍率降为 1 OR 2
        } else if(allPoolGold < k1 && rate >= 4) {
            rate = nonAverageRandom(4);
            if (rate == 0) rate = 1; // 库存小于k1 且 倍率>=4级，则将倍率降为 1 OR 2 OR 3
        } else if(allPoolGold < k2 && rate >= 5) {
            rate = nonAverageRandom(5);
            if (rate == 0) rate = 1; // 库存小于k2 且 倍率>=5级，则将倍率降为 1 OR 2 OR 3 OR 4
        } else if(allPoolGold < k3 && rate >= 6) {
            rate = nonAverageRandom(6);
            if (rate == 0) rate = 1; // 库存小于k3 且 倍率>=6级，则将倍率降为 1 OR 2 OR 3 OR 4 OR 5
        }

        /**
         *  玩家中奖上限处理 达到上限 dxj OR lxj 则禁止高倍率
         */
        String todayPrize_str = RedisHelper.get(REDIS_PLAYER_TODAY_PRIZE_KEY + roomType + id);
        String historyPrize_str = RedisHelper.get(REDIS_PLAYER_HISTORY_PRIZE_KEY + roomType+ id);
        String dxj_str = RedisHelper.get(REDIS_PRIZE_DXJ_KEY + roomType);
        String lxj_str = RedisHelper.get(REDIS_PRIZE_LXJ_KEY + roomType);
        String dx_str = RedisHelper.get(REDIS_LUCKY_DX_KEY + roomType);
        String lx_str = RedisHelper.get(REDIS_LUCKY_LX_KEY + roomType);
        long todayPrize = Long.parseLong(todayPrize_str.equals("") ? "0" : todayPrize_str);
        long historyPrize = Long.parseLong(historyPrize_str.equals("") ? "0" : historyPrize_str);
        long dxj = Long.parseLong(dxj_str.equals("") ? "100000000" : dxj_str);
        long lxj = Long.parseLong(lxj_str.equals("") ? "1000000000" : lxj_str);
        int dx = Integer.parseInt(dx_str.equals("") ? "4" : dx_str);
        int lx = Integer.parseInt(lx_str.equals("") ? "4" : lx_str);

        if(todayPrize > dxj && rate >= dx) {

            rate = rate % dx;
            if (rate == 0) rate+=1;
        }
        if(historyPrize > lxj && rate >= lx) {
            rate = rate % lx;
            if(rate == 0) rate+=1;
        }

        /**
         * 玩家中奖下限处理，达到下限则提升倍率
         */
        String fdxj_str = RedisHelper.get(REDIS_PLAYER_FDXJ_KEY + roomType);
        String flxj_str = RedisHelper.get(REDIS_PLAYER_FLXJ_KEY + roomType);
        String lw_str = RedisHelper.get(REDIS_LUCKY_LW_KEY + roomType);
        String dw_str = RedisHelper.get(REDIS_LUCKY_DW_KEY + roomType);
        long fdxj = Long.parseLong(fdxj_str.equals("") ? "-50000000" : fdxj_str);
        long flxj = Long.parseLong(flxj_str.equals("") ? "-500000000" : flxj_str);
        int lw = Integer.parseInt(lw_str.equals("") ? "4" : lw_str);
        int dw = Integer.parseInt(dw_str.equals("") ? "4" : dw_str);
        if(todayPrize < fdxj) rate = dw;
        if(historyPrize < flxj) rate = lw;
        /**
         * 对每个用户检查设定的倍率控制 AG
         */
        String ag_str = RedisHelper.get(REDIS_PLAYER_AG_KEY + id);
        int ag = Integer.parseInt(ag_str.equals("") ? "63" : ag_str);
        int tmp = rate;
        if((ag & 1 << rate - 1) == 0) {
            for(int i = 0; i < rateMap.length; i++) {
                if((ag & 1 << i) != 0) {
                    rate = i + 1;
                    break;
                }
            }
        }
        //index = ThreadLocalRandom.current().nextInt(rateMap[rate - 1].length); //map的下标从0开始 所以rate-1 从等级倍率的个数n中随机一个 即 0-n-1
        return rate;
    }

    /**
     * 根据概率表生成中奖线的倍数 提升参数版本
     */
    public int randomGenerateRateByAddRate(int linesSize, long id, long singleGold, int roomType) {
        double rate3Add = fruitplayers.get(id).getRate3Add();
        double rate4Add = fruitplayers.get(id).getRate4Add();

        //当全服库存达到2亿金币时，每多1亿都将使五级倍率奖励几率提高1%，从二级倍率中扣取，最高提升3%。同时随着库存的变化而降低倍率奖励。
        if (getAllPoolGold(roomType) > rate5addFromPoolLine)
            probabilityFromPoll = getAllPoolGold(roomType) % 1000000000;
        if (probabilityFromPoll > 3)
            probabilityFromPoll = 3;

        double[] base = new double[10];
        for (int i = 1; i <= 6; i++) {
            for (int j = 6; j >= 7 - i; j--) {
                base[i] += multipleLevel[j];
            }
        }
        int rate = 1;//定义倍率 默认为 1
        int index;//随机选择某个倍率等级中的一种倍率 即下标
        //首先判断保底时应该生成的倍率 当保底参数为0时，则本次至少刷新有一条中奖线获得二级倍率中任意一个倍数奖励，并重置保底参数

        if (fruitplayers.get(id).getMinimum() == 0) {
            rate = 1;
            //index = ThreadLocalRandom.current().nextInt(rateMap[rate - 1].length); //下标从0开始 所以rate-1 从等级倍率的个数n中随机一个 即 0-n-1
            index = nonAverageRandom(rateMap[rate - 1].length);
            if (linesSize >= 5) {
                resetMinimum(id);//重置保底参数
            }
            fruitplayers.get(id).setNeedMinimum(false);
            return rateMap[rate - 1][index];//直接返回奖励倍数
        } else if (fruitplayers.get(id).getBaodi5() >= fruitplayers.get(id).getLastSingleGold() * 30) { //5条线以下的保底
            rate = 1;
            // index = ThreadLocalRandom.current().nextInt(rateMap[rate - 1].length); //下标从0开始 所以rate-1 从等级倍率的个数n中随机一个 即 0-n-1
            index = nonAverageRandom(rateMap[rate - 1].length);
            fruitplayers.get(id).setBaodi5(0);//重置为0
            return rateMap[rate - 1][index];//直接返回奖励倍数
        }
        double random = new Random().nextDouble() * 100; //百分比

        //一级倍率 75% 二级倍率 20% 三级倍率 4% 四级倍率0.6% 五级倍率0.3% 六级倍率 0.1%

        if (random >= (base[6 - 1] + rate3Add + rate4Add + probabilityFromPoll)) { //一级倍率  [250,1000)  占比75%  +rate3Add（三级倍率提升命中参数） 则 一级倍率减少 变成(250+rate3Add)-999 四级倍率提升命中参数 同理
            rate = 1;
        } else if (random >= (base[6 - 2] + rate3Add + rate4Add + probabilityFromPoll)) {//二级倍率
            rate = 2;
            fruitplayers.get(id).setNeedMinimum(false);
            resetMinimum(id);//重置保底参数
        } else if (random >= (base[6 - 3] + rate4Add + probabilityFromPoll)) {     //三级倍率
            rate = 3;
            fruitplayers.get(id).setNeedMinimum(false);
            fruitplayers.get(id).resetRateAdd();//命中三级及以上倍率 则重置命中提升参数
            resetMinimum(id);//重置保底参数
        } else if (random >= base[6 - 4] + probabilityFromPoll) {                 //四级倍率
            rate = 4;
            fruitplayers.get(id).setNeedMinimum(false);
            fruitplayers.get(id).resetRateAdd();//命中三级及以上倍率 则重置命中提升参数
            resetMinimum(id);//重置保底参数
        } else if (random >= base[6 - 5]) {                 //五级倍率
            rate = 5;
            fruitplayers.get(id).setNeedMinimum(false);
            fruitplayers.get(id).resetRateAdd();//命中三级及以上倍率 则重置命中提升参数
            resetMinimum(id);//重置保底参数
        } else {  //六级倍率
            rate = 6;
            fruitplayers.get(id).setNeedMinimum(false);
            fruitplayers.get(id).resetRateAdd();//命中三级及以上倍率 则重置命中提升参数
            resetMinimum(id);//重置保底参数
        }

        rate = checkRate(rate, id, roomType);
        String key = BLACK_ROOM_KEY + roomType + id;
        int rest;
        rest = Integer.parseInt(RedisHelper.get(key));
        String ep_str = RedisHelper.get(BLACK_ROOM_EP_KEY);
        int ep = Integer.parseInt(ep_str.equals("") ? "4" : ep_str);

        if (rate >= ep) {
            if (rest > 0) { //如果小黑屋金额尚未清零
                rate %= ep; //则不允许中ep级倍率及以上
                if(rate == 0) rate = 1;
            }
        }

        index = nonAverageRandom(rateMap[rate - 1].length);

        /**
         * 设置用户被指定只中一级倍率时，对一级倍率中9中倍数得选择
         *
         */
        String rateOne_str = RedisHelper.get(REDIS_PLAYER_RATE_ONE_KEY + id);
        String ag_str = RedisHelper.get(REDIS_PLAYER_AG_KEY + id);
        int rateOne = Integer.parseInt(rateOne_str.equals("") ? "511" : rateOne_str);
        int ag = Integer.parseInt(ag_str.equals("") ? "63" : ag_str);
        if(ag == 1 && rate == 1 && (rateOne & (1 << index)) == 0) {
            for(int i = 0; i < rateMap[0].length; i++) {
                if((rateOne & 1 << i) != 0) {
                    return rateMap[rate - 1][i];
                }
            }
        }

        return rateMap[rate - 1][index];
    }

    /**
     * 非平均生成随机数
     * @param size
     * @return
     */
    public int nonAverageRandom(int size) {
        double[] base = new double[size];
        double probability = 100.0;
        for (int i = 0; i < size - 1; i++) {
            base[i] = probability / 2;
            probability = probability / 2;
        }
        base[size - 1] = 0;
        double random = new Random().nextDouble() * 100;
        for (int i = 0; i < base.length; i++) {
            if(base[i] < random) return i;
        }
        return 0;
    }

    /**
     * 重置返水参数
     * <p>
     * 可能出现BUG 玩家入场时因为上次投注金额为0 本次至少500 所以会执行一次重置返水参数 同时随机一个返水次数，如果返水次数使用到
     */
    public void resetReback(long money, long singleGold, long id) {
        //如果玩家携带金钱 大于 返水生效线 则启用返水参数 否则不启用
        if (money >= ENABLE_REBACK) {
            fruitplayers.get(id).setREBACK_LINE(Math.min(money / 2, singleGold * 100));
            fruitplayers.get(id).setReback(new Random().nextInt(3) + 2); //随机重置为2-4
        } else {
            fruitplayers.get(id).setReback(-1); //-1表示不启用
        }
    }

    /**
     * 重置保底参数
     */
    public static void resetMinimum(long id) {
        int k = new Random().nextInt(4) + 5;
        fruitplayers.get(id).setMinimum(k);//随机重置为5-8
    }

    /**
     * 增加命中提升参数
     */
    public static void increaseRateAdd(int linesNum, long id) {
        double rate3Add = fruitplayers.get(id).getRate3Add();
        double rate4Add = fruitplayers.get(id).getRate4Add();
        rate3Add += linesNum * 0.05; //千分之一 即 0.1%
        //三级倍率提升参数 最多提升20%
        rate3Add = rate3Add > 9 ? 9 : rate3Add;
        rate4Add += linesNum * 0.005; //千分之0.1 即 0.01%
        //四级倍率提升参数 最多提升9%
        rate4Add = rate4Add > 4.5 ? 4.5 : rate4Add;
        fruitplayers.get(id).setRate3Add(rate3Add);
        fruitplayers.get(id).setRate4Add(rate4Add);
    }


    /**
     * 根据传入的倍率 返回生成的水果id及个数  fruitId存放到player实体中
     */
    public Integer randomFruitId(int rate, long id) {
        long lastSingleGold = fruitplayers.get(id).getLastSingleGold();
        int fruitNum = 0;
        switch (rate) {
            case 1:
                fruitplayers.get(id).setFruitId(4);
                fruitNum = 3;
                break;  //香蕉 2 个
            case 3:
                fruitplayers.get(id).setFruitId(4);
                fruitNum = 4;
                break;     //香蕉4个
            case 5:
                fruitplayers.get(id).setFruitId(5);
                fruitNum = 3;
                break;    //西瓜 3个
            case 7:
                fruitplayers.get(id).setFruitId(6);
                fruitNum = 3;
                break;    // 柠檬3个
            case 10:
                fruitplayers.get(id).setFruitId(5);
                fruitNum = 4;
                break;   //西瓜 4个
            case 15:
                fruitplayers.get(id).setFruitId(7);
                fruitNum = 3;
                break;   // 葡萄 3个
            case 20:
                fruitplayers.get(id).setFruitId(2);
                fruitNum = 3;
                break;    // bouns 3个
            case 25:
                fruitplayers.get(id).setFruitId(8);
                fruitNum = 3;
                break; //气球 3个
            case 30:
                fruitplayers.get(id).setFruitId(9);
                fruitNum = 3;
                break; //铃铛 3个
            case 45:
                fruitplayers.get(id).setFruitId(10);
                fruitNum = 3;
                break;    // 樱桃 3个
            case 50:
                fruitplayers.get(id).setFruitId(7);
                fruitNum = 4;
                break;    // 葡萄 4个
            case 60:
                fruitplayers.get(id).setFruitId(11);
                fruitNum = 3;
                break;    // bar 3个
            case 65:
                fruitplayers.get(id).setFruitId(2);
                fruitNum = 4;
                break;    //bonus 4个
            case 70:
                fruitplayers.get(id).setFruitId(4);
                fruitNum = 5;
                break;    // 香蕉 5个
            case 85:
                fruitplayers.get(id).setFruitId(5);
                fruitNum = 5;
                break;
            case 90:
                fruitplayers.get(id).setFruitId(8);
                fruitNum = 4;
                break;    //气球 4个
            case 95:// 单条线押注5W以上 才可以中7
                fruitplayers.get(id).setFruitId(9);
                fruitNum = 4;
//                if (lastSingleGold >= 50000) {
//                    fruitplayers.get(id).setFruitId(random2to1(3, 10));
//                    fruitNum = (fruitplayers.get(id).getFruitId() == 3 ? 3 : 4);  // 7 3个  樱桃 4个
//                } else {
//                    fruitplayers.get(id).setFruitId(10);
//                    fruitNum = 4;
//                }
                break;    // 铃铛 4个
            case 100:
                fruitplayers.get(id).setFruitId(6);
                fruitNum = 5;
                break;   //柠檬 5个
            case 150: // 单条线押注5W以上 才可以中7
                fruitplayers.get(id).setFruitId(10);
                fruitNum = 4;
                break;   //bar 4个
            case 200: // 单条线押注5W以上 才可以中7
                fruitplayers.get(id).setFruitId(11);
                fruitNum = 4;  //bar 4个
                break;
            case 350:
                if(lastSingleGold > 50000) {
                    fruitplayers.get(id).setFruitId(3);
                    fruitNum = 3;    // 7 3个
                }  else {
                    fruitplayers.get(id).setFruitId(11);
                    fruitNum = 4;   // 柠檬 5个
                }
                break;
            case 400:
                fruitplayers.get(id).setFruitId(7);
                fruitNum = 5;
                break;   //葡萄 5个
            case 450:
                if(lastSingleGold > 50000) {
                    fruitplayers.get(id).setFruitId(3);
                    fruitNum = 4;    // 7 4个
                } else {
                    fruitplayers.get(id).setFruitId(7);
                    fruitNum = 5;    // 葡萄 5个
                }
                break;
            case 500:
                fruitplayers.get(id).setFruitId(2);
                fruitNum = 5;
                break;   //bonus 5个
            case 600:
                fruitplayers.get(id).setFruitId(8);
                fruitNum = 5;
                break;  //气球 5个
            case 700:
                fruitplayers.get(id).setFruitId(9);
                fruitNum = 5;
                break;   // 铃铛5个
            case 1000:
                fruitplayers.get(id).setFruitId(10);
                fruitNum = 5;
                break;   // 樱桃 5个
            case 3000:
                fruitplayers.get(id).setFruitId(11);
                fruitNum= 5;
                break;   // bar 5个
            case 5000:
                if(lastSingleGold > 50000) {
                    fruitplayers.get(id).setFruitId(3);
                    fruitNum = 5;   // 7 5个
                } else {
                    fruitplayers.get(id).setFruitId(11);
                    fruitNum = 5;   // bar 5个
                }
                break;
        }

        return fruitNum;
    }

    /**
     * 根据概率表生成中奖线的条数
     */
    public static int randomGenerateLine() {
        double[] base = new double[15];
        for (int i = 1; i <= 10; i++) {
            for (int j = 0; j < i; j++) {
                base[i] += lineRate[j] * 10;
            }
            System.out.println(base[i]);
        }
        int random = new Random().nextInt(1000);
        //random = 0;
        System.out.println(random);
        int linesNum = 0;
        for(int i = 1; i <= 10; i++) {
            if(random > base[i]) {
                linesNum = i;
            } else {
                return linesNum;
            }
        }
        return linesNum;
    }

    /**
     * 随机二选一
     */
    public static int random2to1(int x, int y) {
        if (new Random().nextInt(2) == 0) {
            return x;
        }
        return y;
    }

    /**
     * 随机从数组中选取M个，实现从中随机选择中奖线  蓄水池算法
     */
    public static Integer[] randomSelect(Integer[] array, int m) {
        Integer[] result = new Integer[m];
        int n = array.length;
        for (int i = 0; i < n; i++) {
            int current_num = array[i];
            if (i < m) {
                result[i] = current_num;
            } else {
                int tmp = new Random().nextInt(i + 1);
                if (tmp < m) {
                    result[tmp] = current_num;
                }
            }
        }
        return result;
    }

    /**
     * 获取玩家剩余免费抽奖次数
     */
    public int getFreeDrawNum(long userId) {
        String num = RedisHelper.get(FruitDrawSign + userId);
        return (num == null || num.equals("")) ? 0 : Integer.parseInt(num);
    }

    /**
     * 增加玩家剩余免费抽奖次数
     */
    public void addFreeDrawNum(long userId, int addNum) {
        String str = RedisHelper.get(FruitDrawSign + userId);
        int num = ((str == null || str.equals("")) ? 0 : Integer.parseInt(str)) + addNum;
        RedisHelper.set(FruitDrawSign + userId, String.valueOf(num));
    }

    /**
     * 减少玩家剩余免费抽奖次数
     */
    public void reduceFreeDrawNum(long userId, int reduceNum) {
        String str = RedisHelper.get(FruitDrawSign + userId);
        int num = ((str == null || str.equals("")) ? 0 : Integer.parseInt(str)) - reduceNum;
        RedisHelper.set(FruitDrawSign + userId, String.valueOf(num < 0 ? 0 : num));
    }

    /**
     * 增加玩家旋转次数
     */
    public void addPlayerRotateNum(long userId, int addNum) {
        String str = RedisHelper.get(FruitTotalRotateSign + userId);
        int num = ((str == null || str.equals("")) ? 0 : Integer.parseInt(str)) + addNum;
        RedisHelper.set(FruitTotalRotateSign + userId, String.valueOf(num));
    }

    /**
     * 得到玩家旋转总次数
     */
    public int getPlayerTotalRotateNum(long userId) {
        String num = RedisHelper.get(FruitTotalRotateSign + userId);
        return (num == null || num.equals("")) ? 0 : Integer.parseInt(num);
    }

    /**
     * 获得redis彩金池金币数量
     */
    public long getFruitLaBaRewardPoolNum(int roomType) {
        String str = RedisHelper.get(FruitLaBaRewardPoolSign + roomType);
        return (str == null || str.equals("")) ? 0 : Long.parseLong(str);
    }

    /**
     * 设定redis彩金池金币数量 5秒一次储存任务
     */
    @Scheduled(initialDelay = 8000, fixedRate = 5000)
    public void setFruitLaBaRewardPoolNum() {
        RedisHelper.set(FruitLaBaRewardPoolSign + 1, String.valueOf(rewardTotal1));
        RedisHelper.set(FruitLaBaRewardPoolSign + 2, String.valueOf(rewardTotal2));
        RedisHelper.set(FruitLaBaRewardPoolSign + 3, String.valueOf(rewardTotal3));
    }

    /**
     * 增加旋转幸运值
     */
    public void addFruitLaBaLukeyNum(long userId, long addNum) {
        String str = RedisHelper.get(FruitLaBaLukeyNumSign + userId);
        long totalLukeyNum = ((str == null || str.equals("")) ? 0 : Long.parseLong(str)) + addNum;
        RedisHelper.set(FruitLaBaLukeyNumSign + userId, String.valueOf(totalLukeyNum));
    }

    /**
     * 取得旋转幸运值
     */
    public long getFruitLaBaLukeyNum(long userId) {
        String str = RedisHelper.get(FruitLaBaLukeyNumSign + userId);
        return (str == null || str.equals("")) ? 0 : Long.parseLong(str);
    }

    /**
     * 减少旋转幸运值
     */
    public void reduceFruitLaBaLukeyNum(long userId, long reduceNum) {
        String str = RedisHelper.get(FruitLaBaLukeyNumSign + userId);
        long totalLukeyNum = ((str == null || str.equals("")) ? 0 : Long.parseLong(str)) - reduceNum;
        RedisHelper.set(FruitLaBaLukeyNumSign + userId, String.valueOf(totalLukeyNum < 0 ? 0 : totalLukeyNum));
    }

    /**
     * 获得水果拉霸总库存
     */
    public static long getAllPoolGold(int roomType) {
        long fruitPool = 0; // 累加计算全服水果拉霸库存
        for (int i = 1; i <= 9; i++) {

            fruitPool += getLineContollerGold(i + (roomType - 1) * 9);
        }

        return fruitPool;
    }

    /**
     * 从redis读取某条线的库存
     */
    public static long getLinePoolGold(int lineId) {
        String key = FruitLaBaInitGoldNumSign + lineId;
        String str = RedisHelper.get(key);
        return (str == null || str.equals("")) ? 0 : Long.parseLong(str);
    }

    /**
     * 获得该条线的库存 lineId为：1-9(9条线)
     */
    public static long getLineContollerGold(int lineId) {
        return linePoolGold[lineId];
    }

    /**
     * 处理这条线库存的变化
     */
    public void handleLineGold(int lineId, long handleGold) {
        synchronized (linePoolGold) {
            linePoolGold[lineId] += handleGold;
        }
    }

    /**
     * 处理这条线库存的变化
     */
    public void handleReward(long num, int roomType) {
        synchronized (linePoolGold) {
            switch (roomType) {
                case 1: rewardTotal1 += num;break;
                case 2: rewardTotal2 += num;break;
                case 3: rewardTotal3 += num;break;
            }
        }
    }

    /**
     * 定时保存倍数等级概率
     */
    @Scheduled(initialDelay = 8000, fixedRate = 5000)
    public void setLinePoolGold() {
        for (int i = 1; i <= 27; i++) {
            String key = FruitLaBaInitGoldNumSign + i;
            RedisHelper.set(key, String.valueOf(linePoolGold[i]));
        }
    }

    /**
     * 保存免费抽奖时的下注金币和线数量
     */
    public void setPlayerFreeLineAndGold(int lineNum, long goldNum, long playerId) {
        String key = FruitLaBaPlayerFreeLineAndGoldSign + playerId;
        RedisHelper.set(key, lineNum + ":" + goldNum);
    }

    /**
     * 取得免费抽奖时的下注金币和线数量
     */
    public List<String> getPlayerFreeLineAndGold(long playerId) {
        List<String> list = new ArrayList<>();
        String key = FruitLaBaPlayerFreeLineAndGoldSign + playerId;
        String string = RedisHelper.get(key);
        if (string == null || string.equals("")) {
            list.add("1");
            list.add("500");
            return list;
        }
        String[] split = string.split(":");
        list.addAll(Arrays.asList(split));
        if (list.size() <= 1) {
            list.add("1");
            list.add("500");
            return list;
        } else {
            return list;
        }
    }

    /**
     * 0点清除旋转数据
     */
    public void cleanUserRotateTask() {
        List<FruitLaBaRewardInfo> findAllReward = fruitLaBaRewardInfoMapper.findAllReward();
        for (FruitLaBaRewardInfo fruitLaBaRewardInfo : findAllReward) {
            fruitLaBaRewardInfoMapper.delete(fruitLaBaRewardInfo.getId());
        }
        RedisHelper.removePattern(FruitTotalRotateSign + "*");
    }

    /**
     * 发送游走字幕
     */
    private void sendWanderSubtitle(String info) {
        LobbyMessage.WanderSubtitleResponse.Builder builder = LobbyMessage.WanderSubtitleResponse.newBuilder();
        builder.setLevel(1);
        builder.setContent(info);
        PlayerManager.sendMessageToOnline(LobbyMessage.LobbyMsgCode.S_C_WANDER_SUBTITLE_RESPONSE_VALUE, builder.build());
    }

}
