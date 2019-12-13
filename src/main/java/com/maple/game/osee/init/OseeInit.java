package com.maple.game.osee.init;

import com.alibaba.fastjson.JSONObject;
import com.maple.common.login.event.login.LoginEventManager;
import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.anotation.AppInit;
import com.maple.engine.event.userinit.UserInitEventManager;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.dao.data.mapper.*;
import com.maple.game.osee.dao.log.mapper.*;
import com.maple.game.osee.listener.OseeExitListener;
import com.maple.game.osee.listener.OseeLoginListener;
import com.maple.game.osee.listener.OseeUserInitListener;
import com.maple.game.osee.manager.AgentManager;
import com.maple.game.osee.manager.fishing.FishingHitDataManager;
import com.maple.game.osee.manager.fishing.FishingManager;
import com.maple.game.osee.manager.fishing.FishingRobotManager;
import com.maple.game.osee.manager.fruitlaba.FruitLaBaManager;
import com.maple.network.event.exit.ExitEventManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 1688初始化
 */
@AppInit
public class OseeInit {
    @Autowired
    private OseeFruitRecordLogMapper fruitRecordLogMapper;

    @Autowired
    private OseePlayerMapper playerMapper;

    @Autowired
    private OseeLotteryDrawLogMapper lotteryDrawLogMapper;

    @Autowired
    private OseeRealLotteryLogMapper realLotteryLogMapper;

    @Autowired
    private OseeUnrealLotteryLogMapper unrealLotteryLogMapper;

    @Autowired
    private OseeCdkMapper cdkMapper;

    @Autowired
    private OseeCdkTypeMapper cdkTypeMapper;

    @Autowired
    private OseeLotteryShopMapper lotteryShopMapper;

    @Autowired
    private OseeNoticeMapper noticeMapper;

    @Autowired
    private OseeRechargeLogMapper rechargeLogMapper;

    @Autowired
    private OseePlayerTenureLogMapper playerTenureLogMapper;

    @Autowired
    private OseeCutMoneyLogMapper cutMoneyLogMapper;

    @Autowired
    private OseeExpendLogMapper expendLogMapper;

    @Autowired
    private OseeGobangRecordLogMapper gobangRecordLogMapper;

    @Autowired
    private OseeFishingRecordLogMapper fishingRecordLogMapper;

    @Autowired
    private LoginEventManager loginEventManager;

    @Autowired
    private OseeLoginListener loginListener;

    @Autowired
    private ExitEventManager exitEventManager;

    @Autowired
    private OseeExitListener exitListener;

    @Autowired
    private OseeUserInitListener userInitListener;

    @Autowired
    private UserInitEventManager userInitEventManager;

    @Autowired
    private FruitLaBaRewardInfoMapper fruitLaBaRewardInfoMapper;

    @Autowired
    private FruitLaBaManager fruitLaBaManager;

    @Autowired
    private FishingRobotManager fishingRobotManager;

    @Autowired
    private OseeFighttenRecordLogMapper fighttenRecordLogMapper;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private GiveGiftLogMapper giftLogMapper;

    @Autowired
    private TenChallengeRankingLogMapper tenChallengeRankingLogMapper;

    @Autowired
    private TwoEightRecordMapper twoEightRecordMapper;

    @Autowired
    private CrystalExchangeLogMapper crystalExchangeLogMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AppRankLogMapper rankLogMapper;

    @Autowired
    private AppRewardLogMapper rewardLogMapper;

    @Autowired
    AppRewardRankMapper rewardRankMapper;

    @Autowired
    AppGameLogMapper gameLogMapper;

    /**
     * 1688初始化方法
     */
    public void init() {
        playerMapper.createTable();
        lotteryDrawLogMapper.createTable();
        fruitLaBaRewardInfoMapper.createTable();
        cdkMapper.createTable();
        cdkTypeMapper.createTable();
        lotteryShopMapper.createTable();
        noticeMapper.createTable();
        gameLogMapper.createTable();
        realLotteryLogMapper.createTable();
        unrealLotteryLogMapper.createTable();
        rechargeLogMapper.createTable();
        playerTenureLogMapper.createTable();
        cutMoneyLogMapper.createTable();
        expendLogMapper.createTable();
        gobangRecordLogMapper.createTable();
        fruitRecordLogMapper.createTable();
        fighttenRecordLogMapper.createTable();
        fishingRecordLogMapper.createTable();
        messageMapper.createTable();
        giftLogMapper.createTable();
        tenChallengeRankingLogMapper.createTable();
        crystalExchangeLogMapper.createTable();
        addressMapper.createTable();
        stockMapper.createTable();
        rewardLogMapper.createTable();
        rewardRankMapper.create();
        gameLogMapper.createTable();

        //二八杠表创建
        twoEightRecordMapper.createTable();

        loginEventManager.addEventListener(loginListener);
        exitEventManager.addEventListener(exitListener);
        userInitEventManager.addEventListener(userInitListener);
        //初始化水果拉霸
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(fruitLaBaManager::init, 5, TimeUnit.SECONDS);
        // 初始化捕鱼数据管理类
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(fishingRobotManager::init, 5, TimeUnit.SECONDS);
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(FishingHitDataManager::init, 5, TimeUnit.SECONDS);
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(() -> {
            // 从Redis内读取鱼雷掉落数量记录
            String value = RedisHelper.get("Fishing:TorpedoDropNum");
            if (!StringUtils.isEmpty(value)) {
                JSONObject jsonObject = JSONObject.parseObject(value);
                for (Map.Entry entry : jsonObject.entrySet()) {
                    FishingManager.TORPEDO_RECORD.put(
                            String.valueOf(entry.getKey()),
                            Long.parseLong(String.valueOf(entry.getValue()))
                    );
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

}
