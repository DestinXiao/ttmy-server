package com.maple.game.osee.entity.fishing.csv.file;

import com.maple.engine.anotation.AppData;
import com.maple.engine.data.BaseCsvData;

/**
 * 炮台等级配置表
 */
@AppData(fileUrl = "data/fishing/cfg_battery_level.csv")
public class BatteryLevelConfig extends BaseCsvData {

    /**
     * 炮台等级
     */
    private int batteryLevel;

    /**
     * 使用场景
     */
    private int scene;

    /**
     * 解锁需要的费用
     */
    private long cost;

    /**
     * 解锁成功奖励金币数
     */
    private long gold;

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public int getScene() {
        return scene;
    }

    public void setScene(int scene) {
        this.scene = scene;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public long getGold() {
        return gold;
    }

    public void setGold(long gold) {
        this.gold = gold;
    }
}
