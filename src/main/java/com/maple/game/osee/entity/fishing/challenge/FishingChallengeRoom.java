package com.maple.game.osee.entity.fishing.challenge;

import com.maple.game.osee.entity.GameEnum;
import com.maple.game.osee.entity.fishing.csv.file.FishRefreshRule;
import com.maple.game.osee.entity.fishing.game.FishStruct;
import com.maple.gamebase.data.BaseGameRoom;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 捕鱼挑战赛房间
 *
 * @author Junlong
 */
public class FishingChallengeRoom extends BaseGameRoom {

    /**
     * 房间时间
     */
    private long roomTick;

    private boolean verify;

    /**
     * 实体id生成类
     */
    private AtomicLong roomIdCreator = new AtomicLong(1L);

    /**
     * 房间鱼表
     */
    private Map<Long, FishStruct> fishMap = new ConcurrentHashMap<>();

    /**
     * 下次刷新鱼时间
     */
    private Map<FishRefreshRule, Long> nextRefreshTime;

    /**
     * 最小鱼潮间隔时间
     */
    private int minFishTideDelay;

    /**
     * 最大鱼潮间隔时间
     */
    private int maxFishTideDelay;

    /**
     * 是否正在刷新鱼潮
     */
    private boolean fishTide;

    /**
     * 房间无鱼持续时间
     */
    private int noFishTick;

    /**
     * 下次鱼潮时间
     */
    private long nextFishTideTime;

    /**
     * 房间最后冰冻时间
     */
    private long lastRoomFrozenTime;

    /**
     * 房间上次使用Boss号角时间
     */
    private long lastBossBugleTime;

    /**
     * 最后刷新机器人时间
     */
    private long lastRefreshRobotTime = System.currentTimeMillis();

    /**
     * 该房间所有的鱼群刷新规则
     */
    private List<FishRefreshRule> refreshRules = new LinkedList<>();

    /**
     * 房间内boss刷新规则
     */
    private List<FishRefreshRule> bossRefreshRules = new LinkedList<>();

    /**
     * 房间序号 挑战赛固定为5
     */
    private int roomIndex = 5;

    // **************************************************
    // 挑战赛房间属性

    /**
     * 是否是vip玩家创建的房间
     */
    private boolean vip = false;

    /**
     * 房间密码,VIP才能设置
     */
    private String roomPassword = "";

    /**
     * 房间内是否有Boss 0-无 1-挑战赛的boss 2-boss号角召唤出来的
     */
    private int boss = 0;

    // **************************************************


    public AtomicLong getRoomIdCreator() {
        return roomIdCreator;
    }

    public boolean isVerify() {
        return verify;
    }

    public void setVerify(boolean verify) {
        this.verify = verify;
    }

    public long getRoomTick() {
        return roomTick;
    }

    public void setRoomTick(long roomTick) {
        this.roomTick = roomTick;
    }

    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    public String getRoomPassword() {
        return roomPassword;
    }

    public void setRoomPassword(String roomPassword) {
        this.roomPassword = roomPassword;
    }

    public int getBoss() {
        return boss;
    }

    public void setBoss(int boss) {
        this.boss = boss;
    }

    public Map<Long, FishStruct> getFishMap() {
        return fishMap;
    }

    public void setFishMap(Map<Long, FishStruct> fishMap) {
        this.fishMap = fishMap;
    }

    public Map<FishRefreshRule, Long> getNextRefreshTime() {
        return nextRefreshTime;
    }

    public void setNextRefreshTime(Map<FishRefreshRule, Long> nextRefreshTime) {
        this.nextRefreshTime = nextRefreshTime;
    }

    public int getMinFishTideDelay() {
        return minFishTideDelay;
    }

    public void setMinFishTideDelay(int minFishTideDelay) {
        this.minFishTideDelay = minFishTideDelay;
    }

    public int getMaxFishTideDelay() {
        return maxFishTideDelay;
    }

    public void setMaxFishTideDelay(int maxFishTideDelay) {
        this.maxFishTideDelay = maxFishTideDelay;
    }

    public boolean isFishTide() {
        return fishTide;
    }

    public void setFishTide(boolean fishTide) {
        this.fishTide = fishTide;
    }

    public int getNoFishTick() {
        return noFishTick;
    }

    public void setNoFishTick(int noFishTick) {
        this.noFishTick = noFishTick;
    }

    public long getNextFishTideTime() {
        return nextFishTideTime;
    }

    public void setNextFishTideTime(long nextFishTideTime) {
        this.nextFishTideTime = nextFishTideTime;
    }

    public long getLastRoomFrozenTime() {
        return lastRoomFrozenTime;
    }

    public void setLastRoomFrozenTime(long lastRoomFrozenTime) {
        this.lastRoomFrozenTime = lastRoomFrozenTime;
    }

    public long getLastBossBugleTime() {
        return lastBossBugleTime;
    }

    public void setLastBossBugleTime(long lastBossBugleTime) {
        this.lastBossBugleTime = lastBossBugleTime;
    }

    public long getLastRefreshRobotTime() {
        return lastRefreshRobotTime;
    }

    public void setLastRefreshRobotTime(long lastRefreshRobotTime) {
        this.lastRefreshRobotTime = lastRefreshRobotTime;
    }

    public List<FishRefreshRule> getRefreshRules() {
        return refreshRules;
    }

    public void setRefreshRules(List<FishRefreshRule> refreshRules) {
        this.refreshRules = refreshRules;
    }

    public int getRoomIndex() {
        return roomIndex;
    }

    public void setRoomIndex(int roomIndex) {
        this.roomIndex = roomIndex;
    }

    public List<FishRefreshRule> getBossRefreshRules() {
        return bossRefreshRules;
    }

    public void setBossRefreshRules(List<FishRefreshRule> bossRefreshRules) {
        this.bossRefreshRules = bossRefreshRules;
    }

    // *********************************

    /**
     * 房间时钟递增
     */
    public void addRoomTick() {
        roomTick++;
    }

    public long getNextId() {
        return roomIdCreator.getAndIncrement();
    }

    @Override
    public int getGameId() {
        return GameEnum.FISHING.getId();
    }

    public void reset() {
        this.roomTick = 0;
        this.roomIdCreator.set(1L);
        this.fishMap.clear();
        this.nextRefreshTime = null;
        this.minFishTideDelay = 0;
        this.maxFishTideDelay = 0;
        this.fishTide = false;
        this.noFishTick = 0;
        this.nextFishTideTime = 0;
        this.lastRoomFrozenTime = 0;
        this.lastBossBugleTime = 0;
        this.lastRefreshRobotTime = 0;
        this.refreshRules.clear();
        this.bossRefreshRules.clear();
        this.boss = 0;
    }

}
