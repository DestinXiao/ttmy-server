package com.maple.game.osee.entity.fightten.challenge;

import com.maple.game.osee.entity.fightten.FightTenRoom;

import java.time.LocalDateTime;

/**
 * 拼十挑战赛房间
 *
 * @author Junlong
 */
public class FightTenChallengeRoom extends FightTenRoom {

    /**
     * 房间创建时间
     */
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 加入房间金币限制
     */
    private long enterMoneyLimit;

    /**
     * 是否玩过
     */
    private boolean played = false;

    /**
     * 房间创建者id
     */
    private long ownerId;

    /**
     * 房费支付类型 0-房主承包：房主支付钻石，有固定局数 1-挑战模式：每次扣除挑战次数
     */
    private int feeType;

    // 以下属性只有只有房主支付才生效

    /**
     * 当前局数
     */
    private int roundNow;

    /**
     * 总局数
     */
    private int roundTotal;

    /**
     * 局数序号
     */
    private int roundIndex;

    /**
     * 是否是私有亲友房间
     */
    private boolean privateRoom;

    // *************


    public int getRoundIndex() {
        return roundIndex;
    }

    public void setRoundIndex(int roundIndex) {
        this.roundIndex = roundIndex;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public long getEnterMoneyLimit() {
        return enterMoneyLimit;
    }

    public void setEnterMoneyLimit(long enterMoneyLimit) {
        this.enterMoneyLimit = enterMoneyLimit;
    }

    public int getFeeType() {
        return feeType;
    }

    public void setFeeType(int feeType) {
        this.feeType = feeType;
    }

    public int getRoundNow() {
        return roundNow;
    }

    public void setRoundNow(int roundNow) {
        this.roundNow = roundNow;
    }

    public int getRoundTotal() {
        return roundTotal;
    }

    public void setRoundTotal(int roundTotal) {
        this.roundTotal = roundTotal;
    }

    public boolean isPrivateRoom() {
        return privateRoom;
    }

    public void setPrivateRoom(boolean privateRoom) {
        this.privateRoom = privateRoom;
    }

    public boolean isPlayed() {
        return played;
    }

    public void setPlayed(boolean played) {
        this.played = played;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    // *************************************

    @Override
    public void roundReset() {
        super.roundReset();
    }
}
