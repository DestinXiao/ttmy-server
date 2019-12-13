package com.maple.game.osee.entity.fishing.game;

import com.maple.game.osee.entity.ItemData;

import java.util.Date;
import java.util.List;

/**
 * 捕鱼任务结构
 */
public class FishingTaskStruct implements Comparable<FishingTaskStruct> {

    /**
     * 任务配置id
     */
    private long id;

    /**
     * 进度
     */
    private int progress;

    /**
     * 目标数量
     */
    private int target;

    /**
     * 是否被领取
     */
    private boolean received;

    /**
     * 活跃度
     */
    private int activeLevel;

    /**
     * 接受任务时间
     */
    private Date createTime;

    /**
     * 任务奖励
     */
    private List<ItemData> taskRewards;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getActiveLevel() {
        return activeLevel;
    }

    public void setActiveLevel(int activeLevel) {
        this.activeLevel = activeLevel;
    }

    public List<ItemData> getTaskRewards() {
        return taskRewards;
    }

    public void setTaskRewards(List<ItemData> taskRewards) {
        this.taskRewards = taskRewards;
    }

    @Override
    public int compareTo(FishingTaskStruct other) {
        return other.createTime.compareTo(createTime);
    }

}
