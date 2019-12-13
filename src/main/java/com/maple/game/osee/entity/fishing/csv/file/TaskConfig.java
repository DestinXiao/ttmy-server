package com.maple.game.osee.entity.fishing.csv.file;

import com.google.gson.Gson;
import com.maple.engine.anotation.AppData;
import com.maple.engine.data.BaseCsvData;
import com.maple.game.osee.entity.ItemData;

import java.util.Arrays;
import java.util.List;

/**
 * 捕鱼任务配置
 */
@AppData(fileUrl = "data/fishing/cfg_task.csv")
public class TaskConfig extends BaseCsvData {

    /**
     * 任务名
     */
    private String name;

    /**
     * 任务详细说明
     */
    private String info;

    /**
     * 任务类型
     */
    private int taskType;

    /**
     * 目标类型
     */
    private int goalType;

    /**
     * 目标鱼id，如果是打鱼任务
     */
    private String goalId;

    /**
     * 目标鱼ID列表
     */
    private List<Integer> realGoalId;

    /**
     * 目标数量
     */
    private int goalNum;

    /**
     * 活跃度
     */
    private int activeLevel;

    /**
     * 任务奖励
     */
    private String rewards;

    /**
     * 任务奖励列表
     */
    private List<ItemData> realRewards;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGoalType() {
        return goalType;
    }

    public void setGoalType(int goalType) {
        this.goalType = goalType;
    }

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public List<Integer> getRealGoalId() {
        if (realGoalId == null) {
            Integer[] goldIds = new Gson().fromJson(goalId, Integer[].class);
            realGoalId = Arrays.asList(goldIds);
        }
        return realGoalId;
    }

    public void setRealGoalId(List<Integer> realGoalId) {
        this.realGoalId = realGoalId;
    }

    public int getGoalNum() {
        return goalNum;
    }

    public void setGoalNum(int goalNum) {
        this.goalNum = goalNum;
    }

    public String getRewards() {
        return rewards;
    }

    public void setRewards(String rewards) {
        this.rewards = rewards;
    }

    public List<ItemData> getRealRewards() {
        if (realRewards == null) {
            ItemData[] rewardArray = new Gson().fromJson(rewards, ItemData[].class);
            realRewards = Arrays.asList(rewardArray);
        }

        return realRewards;
    }

    public void setRealRewards(List<ItemData> realRewards) {
        this.realRewards = realRewards;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getActiveLevel() {
        return activeLevel;
    }

    public void setActiveLevel(int activeLevel) {
        this.activeLevel = activeLevel;
    }

    public int getTaskType() {
        return taskType;
    }

    public void setTaskType(int taskType) {
        this.taskType = taskType;
    }
}
