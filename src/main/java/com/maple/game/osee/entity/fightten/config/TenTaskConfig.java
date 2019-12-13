package com.maple.game.osee.entity.fightten.config;

import com.maple.engine.anotation.AppData;
import com.maple.engine.data.BaseCsvData;

/**
 * 拼十任务配置
 */
@AppData(fileUrl = "data/fightten/cfg_ten_task.csv")
public class TenTaskConfig extends BaseCsvData {

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务说明
     */
    private String info;

    /**
     * 目标次数
     */
    private Integer targetNum;

    /**
     * 奖励(json格式)
     */
    private String rewards;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Integer getTargetNum() {
        return targetNum;
    }

    public void setTargetNum(Integer targetNum) {
        this.targetNum = targetNum;
    }

    public String getRewards() {
        return rewards;
    }

    public void setRewards(String rewards) {
        this.rewards = rewards;
    }
}
