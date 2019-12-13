package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 捕鱼记录实体类
 */
public class OseeFishingRecordLogEntity extends DbEntity {
    private static final long serialVersionUID = 5711596587043216673L;

    /**
     * 玩家id
     */
    private long playerId;

    /**
     * 捕鱼场次
     */
    private int roomIndex;

    /**
     * 花费金币
     */
    private long spendMoney;

    /**
     * 赢取的金币
     */
    private long winMoney;

    /**
     * 掉落的青铜鱼雷数量
     */
    private long dropBronzeTorpedoNum;

    /**
     * 掉落的白银鱼雷数量
     */
    private long dropSilverTorpedoNum;

    /**
     * 掉落的黄金鱼雷数量
     */
    private long dropGoldTorpedoNum;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getRoomIndex() {
        return roomIndex;
    }

    public void setRoomIndex(int roomIndex) {
        this.roomIndex = roomIndex;
    }

    public long getSpendMoney() {
        return spendMoney;
    }

    public void setSpendMoney(long spendMoney) {
        this.spendMoney = spendMoney;
    }

    public long getWinMoney() {
        return winMoney;
    }

    public void setWinMoney(long winMoney) {
        this.winMoney = winMoney;
    }

    public long getDropBronzeTorpedoNum() {
        return dropBronzeTorpedoNum;
    }

    public void setDropBronzeTorpedoNum(long dropBronzeTorpedoNum) {
        this.dropBronzeTorpedoNum = dropBronzeTorpedoNum;
    }

    public long getDropSilverTorpedoNum() {
        return dropSilverTorpedoNum;
    }

    public void setDropSilverTorpedoNum(long dropSilverTorpedoNum) {
        this.dropSilverTorpedoNum = dropSilverTorpedoNum;
    }

    public long getDropGoldTorpedoNum() {
        return dropGoldTorpedoNum;
    }

    public void setDropGoldTorpedoNum(long dropGoldTorpedoNum) {
        this.dropGoldTorpedoNum = dropGoldTorpedoNum;
    }
}
