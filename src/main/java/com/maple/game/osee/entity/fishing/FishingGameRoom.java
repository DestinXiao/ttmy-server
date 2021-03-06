package com.maple.game.osee.entity.fishing;

import com.maple.game.osee.entity.GameEnum;
import com.maple.game.osee.entity.fishing.csv.file.FishRefreshRule;
import com.maple.game.osee.entity.fishing.game.FishStruct;
import com.maple.gamebase.data.fishing.BaseFishingRoom;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 捕鱼游戏房间
 */
public class FishingGameRoom extends BaseFishingRoom {

    /**
     * 房间序号
     */
    private int roomIndex;

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
     * 房间内是否有Boss 0-无 1-普通刷新出来的boss 2-boss号角召唤出来的
     */
    private int boss = 0;

    public long getLastBossBugleTime() {
        return lastBossBugleTime;
    }

    public void setLastBossBugleTime(long lastBossBugleTime) {
        this.lastBossBugleTime = lastBossBugleTime;
    }

    public int getRoomIndex() {
        return roomIndex;
    }

    public void setRoomIndex(int roomIndex) {
        this.roomIndex = roomIndex;
    }

    public long getNextId() {
        return roomIdCreator.getAndIncrement();
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

    public int getBoss() {
        return boss;
    }

    public void setBoss(int boss) {
        this.boss = boss;
    }

    @Override
    public int getGameId() {
        return GameEnum.FISHING.getId();
    }

}
