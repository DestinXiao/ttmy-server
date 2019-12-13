package com.maple.game.osee.entity.fishing;

import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.game.FireStruct;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.gamebase.data.fishing.BaseFishingPlayer;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 捕鱼游戏玩家
 */
@Data
public class FishingGamePlayer extends BaseFishingPlayer {

    /**
     * 外观序号
     */
    private int viewIndex;

    /**
     * 炮台等级
     */
    private int batteryLevel = 50;

    /**
     * 炮台倍数
     */
    private int batteryMult = 1;

    /**
     * 玩家子弹表
     */
    private Map<Long, FireStruct> fireMap = new ConcurrentHashMap<>();

    /**
     * 进入捕鱼房间时的时间 用于计算玩家在房间内的时长
     */
    private long enterRoomTime = 0;

    /**
     * 上次使用锁定的时间
     */
    private long lastLockTime;

    /**
     * 上次使用冰冻技能的时间
     */
    private long lastFrozenTime;

    /**
     * 上次使用急速的时间
     */
    private long lastFastTime;

    /**
     * 最后一次触发暴击的时间
     */
    private long lastCritTime;

    /**
     * 最后一次使用分身炮的时间
     */
    private long lastFenShenTime;

    /**
     * 最后一次开炮时间
     */
    private long lastFireTime = System.currentTimeMillis();

    /**
     * 进入房间时金币
     */
    private long enterMoney;

    /**
     * 变化金币
     */
    private long changeMoney;

    /**
     * 抽水金币
     */
    private long cutMoney;

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
    private long dropBronzeTorpedoNum = 0;

    /**
     * 掉落的白银鱼雷数量
     */
    private long dropSilverTorpedoNum = 0;

    /**
     * 掉落的黄金鱼雷数量
     */
    private long dropGoldTorpedoNum = 0;

    private long dragonCrystal = 0;

    /**
     * 鱼雷金币
     */
    private long torpedoMoney = 0;

    /**
     * 获取玩家金币数量
     */
    public long getMoney() {
        return PlayerManager.getPlayerMoney(getUser());
    }

    /**
     * 添加金币
     */
    public void addMoney(long count) {
        PlayerManager.addItem(getUser(), ItemId.MONEY, count, ItemChangeReason.FISHING_RESULT, false);
    }

    /**
     * 获取玩家vip等级
     */
    public int getVipLevel() {
        return PlayerManager.getPlayerVipLevel(getUser());
    }

    /**
     * 获取玩家等级
     */
    public int getLevel() {
        return PlayerManager.getPlayerEntity(getUser()).getLevel();
    }

    public int getViewIndex() {
        return viewIndex;
    }

    public void setViewIndex(int viewIndex) {
        this.viewIndex = viewIndex;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public int getBatteryMult() {
        return batteryMult;
    }

    public void setBatteryMult(int batteryMult) {
        this.batteryMult = batteryMult;
    }

    public Map<Long, FireStruct> getFireMap() {
        return fireMap;
    }

    public void setFireMap(Map<Long, FireStruct> fireMap) {
        this.fireMap = fireMap;
    }

    public long getEnterRoomTime() {
        return enterRoomTime;
    }

    public void setEnterRoomTime(long enterRoomTime) {
        this.enterRoomTime = enterRoomTime;
    }

    public long getLastLockTime() {
        return lastLockTime;
    }

    public void setLastLockTime(long lastLockTime) {
        this.lastLockTime = lastLockTime;
    }

    public long getLastFrozenTime() {
        return lastFrozenTime;
    }

    public void setLastFrozenTime(long lastFrozenTime) {
        this.lastFrozenTime = lastFrozenTime;
    }

    public long getLastFastTime() {
        return lastFastTime;
    }

    public void setLastFastTime(long lastFastTime) {
        this.lastFastTime = lastFastTime;
    }

    public long getLastCritTime() {
        return lastCritTime;
    }

    public void setLastCritTime(long lastCritTime) {
        this.lastCritTime = lastCritTime;
    }

    public long getLastFireTime() {
        return lastFireTime;
    }

    public void setLastFireTime(long lastFireTime) {
        this.lastFireTime = lastFireTime;
    }

    public long getEnterMoney() {
        return enterMoney;
    }

    public void setEnterMoney(long enterMoney) {
        this.enterMoney = enterMoney;
    }

    public long getChangeMoney() {
        return changeMoney;
    }

    public void setChangeMoney(long changeMoney) {
        this.changeMoney = changeMoney;
    }

    public long getCutMoney() {
        return cutMoney;
    }

    public void setCutMoney(long cutMoney) {
        this.cutMoney = cutMoney;
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

    public long getLastFenShenTime() {
        return lastFenShenTime;
    }

    public void setLastFenShenTime(long lastFenShenTime) {
        this.lastFenShenTime = lastFenShenTime;
    }

    public long getTorpedoMoney() {
        return torpedoMoney;
    }

    public void setTorpedoMoney(long torpedoMoney) {
        this.torpedoMoney = torpedoMoney;
    }
}
