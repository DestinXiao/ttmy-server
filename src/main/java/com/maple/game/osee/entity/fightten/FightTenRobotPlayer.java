package com.maple.game.osee.entity.fightten;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 拼十机器人
 */
public class FightTenRobotPlayer extends FightTenPlayer {

    /**
     * 机器人消失的时间
     */
    private Long leaveTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(60, 200) * 1000;

    /**
     * 机器人金币数量
     */
    private long money;

    public Long getLeaveTime() {
        return leaveTime;
    }

    public void setLeaveTime(Long leaveTime) {
        this.leaveTime = leaveTime;
    }

    @Override
    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }
}
