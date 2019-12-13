package com.maple.game.osee.manager.lobby;

import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.data.ServerUser;
import com.maple.engine.manager.GsonManager;
import com.maple.engine.utils.DateUtils;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.dao.log.entity.OseeLotteryDrawLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeLotteryDrawLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.LotteryDrawResponse;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.NextLotteryDrawFeeResponse;
import com.maple.game.osee.util.CommonUtil;
import com.maple.network.manager.NetManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 1688抽奖管理类
 */
@Component
public class LotteryDrawManager {

    private static OseeLotteryDrawLogMapper lotteryDrawLogMapper;

    /**
     * 抽奖概率键
     */
    private static final String probKey = "Server:Setting:LotteryProb";

    /**
     * 奖品列表
     */
    public static final int[][] prizes = {
            {ItemId.SKILL_LOCK.getId(), 10}, {ItemId.SKILL_FROZEN.getId(), 10},
            {ItemId.SKILL_FAST.getId(), 5}, {ItemId.SKILL_CRIT.getId(), 3},
            {ItemId.MONEY.getId(), 5000}, {ItemId.MONEY.getId(), 50000},
            {ItemId.DIAMOND.getId(), 5}, {ItemId.DIAMOND.getId(), 10}
    };

    /**
     * 中奖概率
     */
    public static int[] probabilities = {15, 10, 10, 10, 25, 5, 15, 10};

    /**
     * 免费抽奖次数 每日首次免费
     */
    public static final int lotteryFreeCount = 1;

    /**
     * 抽奖费用 每次进行抽奖消耗的奖券数量
     */
    public static final int lotteryFee = 100;

    /**
     * 1688抽奖记录缓存map
     */
    public static final Map<Long, List<OseeLotteryDrawLogEntity>> lotteryLogMap = new HashMap<>();

    @Autowired
    public LotteryDrawManager(OseeLotteryDrawLogMapper lotteryDrawLogMapper) {
        LotteryDrawManager.lotteryDrawLogMapper = lotteryDrawLogMapper;
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(LotteryDrawManager::readLotteryProb, 5, TimeUnit.SECONDS);
    }

    /**
     * 保存概率值
     */
    public static void saveLotteryProb() {
        RedisHelper.set(probKey, GsonManager.gson.toJson(probabilities));
    }

    /**
     * 读取概率值
     */
    public static void readLotteryProb() {
        String probStr = RedisHelper.get(probKey);
        if (!StringUtils.isEmpty(probStr)) {
            probabilities = GsonManager.gson.fromJson(probStr, int[].class);
        }
    }

    /**
     * 获取当前剩余免费抽奖次数
     */
    public static int getFreeLotteryDrawCount(ServerUser user) {
        if (!lotteryLogMap.containsKey(user.getId())) {
            synchronized (lotteryLogMap) {
                if (!lotteryLogMap.containsKey(user.getId())) {
                    lotteryLogMap.put(user.getId(), lotteryDrawLogMapper.getLotteryDrawLogs(user.getId(), 1));
                }
            }
        }

        int total = 0;
        for (OseeLotteryDrawLogEntity log : lotteryLogMap.get(user.getId())) {
            if (DateUtils.isSameDay(log.getCreateTime(), new Date())) {
                total += 1;
            }
        }

        int free = lotteryFreeCount - total;

        return free >= 0 ? free : 0;
    }

    /**
     * 抽奖任务
     */
    public static void doLotteryDraw(ServerUser user) {
        // 检测今日免费次数
        if (getFreeLotteryDrawCount(user) < 1) {
            // 免费次数用完就消耗奖券
            if (!PlayerManager.checkItem(user, ItemId.LOTTERY, lotteryFee)) {
                NetManager.sendHintMessageToClient("奖券数量不足", user);
                return;
            }
            // 扣除轮盘消耗的奖券
            PlayerManager.addItem(user, ItemId.LOTTERY, -lotteryFee, ItemChangeReason.LOTTERY_PAY, true);
        }

        // 随机轮盘算法
        int prizeIndex = CommonUtil.getWheelRandom(probabilities);

        OseeLotteryDrawLogEntity entity = new OseeLotteryDrawLogEntity();
        entity.setPlayerId(user.getId());
        entity.setItemId(prizes[prizeIndex][0]);
        entity.setItemNum(prizes[prizeIndex][1]);
        lotteryDrawLogMapper.save(entity);
        lotteryLogMap.get(user.getId()).add(entity);

        PlayerManager.addItem(user, prizes[prizeIndex][0], prizes[prizeIndex][1], ItemChangeReason.LOTTERY_WIN, true);

        LotteryDrawResponse.Builder builder = LotteryDrawResponse.newBuilder();
        builder.setIndex(prizeIndex + 1);
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_LOTTERY_DRAW_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送下次抽奖费用消息
     */
    public static void sendNextLotteryDrawFeeResponse(ServerUser user) {
        NextLotteryDrawFeeResponse.Builder builder = NextLotteryDrawFeeResponse.newBuilder();
        builder.setFreeCount(getFreeLotteryDrawCount(user));
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_NEXT_LOTTERY_DRAW_FEE_RESPONSE_VALUE, builder, user);
    }

}
