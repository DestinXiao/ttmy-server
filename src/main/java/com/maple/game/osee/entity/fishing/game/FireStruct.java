package com.maple.game.osee.entity.fishing.game;

/**
 * 子弹数据
 */
public class FireStruct {

    /**
     * id
     */
    private long id;

    /**
     * 鱼id
     */
    private long fishId;

    /**
     * 角度
     */
    private float angle;

    /**
     * 子弹等级
     */
    private int level;

    /**
     * 子弹倍数
     */
    private int mult;

    /**
     * 单次发射的子弹数量
     */
    private int count = 1;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFishId() {
        return fishId;
    }

    public void setFishId(long fishId) {
        this.fishId = fishId;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getMult() {
        return mult;
    }

    public void setMult(int mult) {
        this.mult = mult;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
