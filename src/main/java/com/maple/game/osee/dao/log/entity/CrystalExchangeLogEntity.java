package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 龙晶兑换记录实体类
 *
 * @author Junlong
 */
public class CrystalExchangeLogEntity extends DbEntity {
    private static final long serialVersionUID = 7543381269839472105L;

    private long playerId;

    private int exchangeType; // 兑换类型 0-兑换龙晶 1-兑换鱼雷

    private long bronzeTorpedoBefore; // 青铜鱼雷变前
    private long silverTorpedoBefore; // 白银鱼雷变前
    private long goldTorpedoBefore; // 黄金鱼雷变前

    private long bronzeTorpedoChange; // 青铜鱼雷变化量
    private long silverTorpedoChange; // 白银鱼雷变化量
    private long goldTorpedoChange; // 黄金鱼雷变化量

    private long dragonCrystalBefore; // 龙晶变前
    private long dragonCrystalChange; // 龙晶变化量

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(int exchangeType) {
        this.exchangeType = exchangeType;
    }

    public long getBronzeTorpedoBefore() {
        return bronzeTorpedoBefore;
    }

    public void setBronzeTorpedoBefore(long bronzeTorpedoBefore) {
        this.bronzeTorpedoBefore = bronzeTorpedoBefore;
    }

    public long getSilverTorpedoBefore() {
        return silverTorpedoBefore;
    }

    public void setSilverTorpedoBefore(long silverTorpedoBefore) {
        this.silverTorpedoBefore = silverTorpedoBefore;
    }

    public long getGoldTorpedoBefore() {
        return goldTorpedoBefore;
    }

    public void setGoldTorpedoBefore(long goldTorpedoBefore) {
        this.goldTorpedoBefore = goldTorpedoBefore;
    }

    public long getBronzeTorpedoChange() {
        return bronzeTorpedoChange;
    }

    public void setBronzeTorpedoChange(long bronzeTorpedoChange) {
        this.bronzeTorpedoChange = bronzeTorpedoChange;
    }

    public long getSilverTorpedoChange() {
        return silverTorpedoChange;
    }

    public void setSilverTorpedoChange(long silverTorpedoChange) {
        this.silverTorpedoChange = silverTorpedoChange;
    }

    public long getGoldTorpedoChange() {
        return goldTorpedoChange;
    }

    public void setGoldTorpedoChange(long goldTorpedoChange) {
        this.goldTorpedoChange = goldTorpedoChange;
    }

    public long getDragonCrystalBefore() {
        return dragonCrystalBefore;
    }

    public void setDragonCrystalBefore(long dragonCrystalBefore) {
        this.dragonCrystalBefore = dragonCrystalBefore;
    }

    public long getDragonCrystalChange() {
        return dragonCrystalChange;
    }

    public void setDragonCrystalChange(long dragonCrystalChange) {
        this.dragonCrystalChange = dragonCrystalChange;
    }
}
