package com.maple.game.osee.manager.lobby;

import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.data.ServerUser;
import com.maple.engine.manager.GsonManager;
import com.maple.engine.utils.DateUtils;
import com.maple.game.osee.dao.log.entity.OseeExpendLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeExpendLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.task.GoalType;
import com.maple.game.osee.entity.fishing.task.TaskType;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.manager.fishing.FishingTaskManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.SignedTimesResponse;
import com.maple.network.manager.NetManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 1688签到管理类
 */
@Component
public class DailySignManager {

    /**
     * 签到键构造字符串
     */
    private static final String SIGN_KEY_BUILDER = "WeekSignData:%d";

    /**
     * 签到奖励
     */
    private static final long[] SIGN_REWARDS = {1000, 2000, 3000, 4000, 5000, 8000, 10000};

    private static OseeExpendLogMapper expendLogMapper;

    @Autowired
    public DailySignManager(OseeExpendLogMapper expendLogMapper) {
        DailySignManager.expendLogMapper = expendLogMapper;
    }

    /**
     * 获取玩家已签到次数
     */
    public static PlayerSignData getSignTimes(ServerUser user) {
        String key = String.format(SIGN_KEY_BUILDER, user.getId());

        String value = RedisHelper.get(key);

        if (!StringUtils.isEmpty(value)) {
            return GsonManager.gson.fromJson(value, PlayerSignData.class);
        }

        return new PlayerSignData(new Date(DateUtils.DEFAULT_TIME), 0);
    }

    /**
     * 更新玩家签到数据到redis
     */
    public static void updateSignTimes(ServerUser user, PlayerSignData data) {
        String key = String.format(SIGN_KEY_BUILDER, user.getId());
        RedisHelper.set(key, GsonManager.gson.toJson(data));
    }

    /**
     * 玩家签到
     */
    public static void playerDailySign(ServerUser user) {
        PlayerSignData signData = getSignTimes(user);

        if (DateUtils.isSameDay(signData.lastSignTime, new Date())) {
            return;
        }

        if (DateUtils.isSameWeek(signData.lastSignTime, new Date())) { // 同一周内签到
            signData.setWeekSignTimes(signData.getWeekSignTimes() + 1);
        } else {
            signData.setWeekSignTimes(1);
        }
        signData.setLastSignTime(new Date());
        updateSignTimes(user, signData);

        long money = SIGN_REWARDS[signData.getWeekSignTimes() - 1];
        int vipLevel = PlayerManager.getPlayerVipLevel(user);
        if (vipLevel > 0) { // 签到奖励金币翻VIP等级+1倍
            money *= vipLevel + 1;
        }
        PlayerManager.addItem(user, ItemId.MONEY, money, ItemChangeReason.SIGN_IN, true);

        OseeExpendLogEntity log = new OseeExpendLogEntity();
        log.setUserId(user.getId());
        log.setNickname(user.getNickname());
        log.setPayType(3);
        log.setMoney(money);
        expendLogMapper.save(log);

        // 做任务
        FishingTaskManager.doTask(user, TaskType.DAILY, GoalType.SIGN, 0, 1);

        sendSignTimes(user, true);
    }

    /**
     * 发送玩家签到信息
     */
    public static void sendSignTimes(ServerUser user, boolean nowSign) {
        PlayerSignData signData = getSignTimes(user);

        SignedTimesResponse.Builder builder = SignedTimesResponse.newBuilder();
        builder.setNowSign(nowSign);
        builder.setSigned(DateUtils.isSameDay(signData.lastSignTime, new Date()));

        if (builder.getSigned() || DateUtils.isSameWeek(signData.lastSignTime, new Date())) { // 当天/本周签到
            builder.setTimes(signData.getWeekSignTimes());
        }

        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_SIGNED_TIMES_RESPONSE_VALUE, builder, user);
    }

    /**
     * 玩家签到数据
     */
    public static class PlayerSignData {

        /**
         * 最后签到时间
         */
        private Date lastSignTime;

        /**
         * 周签到次数
         */
        private int weekSignTimes;

        public PlayerSignData(Date lastSignTime, int weekSignTimes) {
            this.lastSignTime = lastSignTime;
            this.weekSignTimes = weekSignTimes;
        }

        public Date getLastSignTime() {
            return lastSignTime;
        }

        public void setLastSignTime(Date lastSignTime) {
            this.lastSignTime = lastSignTime;
        }

        public int getWeekSignTimes() {
            return weekSignTimes;
        }

        public void setWeekSignTimes(int weekSignTimes) {
            this.weekSignTimes = weekSignTimes;
        }

    }

}
