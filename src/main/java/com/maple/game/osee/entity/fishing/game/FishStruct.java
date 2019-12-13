package com.maple.game.osee.entity.fishing.game;

/**
 * 鱼类数据
 */
public class FishStruct {

    /**
     * 实体id
     */
    private long id;

    /**
     * 刷新规则id
     */
    private long ruleId;

    /**
     * 配置id
     */
    private long configId;

    /**
     * 路线id
     */
    private long routeId;

    /**
     * 存活时间
     */
    private float lifeTime;

    /**
     * 当前存活时间
     */
    private float nowLifeTime;

    /**
     * 安全次数
     */
    private int safeTimes;

    /**
     * 命中次数
     */
    private int fireTimes;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 鱼类型
     */
    private int fishType;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public long getConfigId() {
        return configId;
    }

    public void setConfigId(long configId) {
        this.configId = configId;
    }

    public long getRouteId() {
        return routeId;
    }

    public void setRouteId(long routeId) {
        this.routeId = routeId;
    }

    public float getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(float lifeTime) {
        this.lifeTime = lifeTime;
    }

    public float getNowLifeTime() {
        return nowLifeTime;
    }

    public void setNowLifeTime(float nowLifeTime) {
        this.nowLifeTime = nowLifeTime;
    }

    public int getSafeTimes() {
        return safeTimes;
    }

    public void setSafeTimes(int safeTimes) {
        this.safeTimes = safeTimes;
    }

    public int getFireTimes() {
        return fireTimes;
    }

    public void setFireTimes(int fireTimes) {
        this.fireTimes = fireTimes;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getFishType() {
        return fishType;
    }

    public void setFishType(int fishType) {
        this.fishType = fishType;
    }

    /**
     * 客户端生存时间
     */
    public float getClientLifeTime() {
        float realClientLifeTime = (System.currentTimeMillis() - getCreateTime()) / 1000F;
        return Math.max(realClientLifeTime, nowLifeTime);
    }

}
