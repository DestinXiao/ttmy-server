package com.maple.game.osee.entity.fightten.task;

import java.io.Serializable;
import java.util.List;

/**
 * 拼十玩家任务
 */
public class FightTenTask implements Serializable {
    private static final long serialVersionUID = -3863782952529187511L;

    /**
     * 拼十玩家所有任务
     */
    private List<TaskInfo> taskInfos;

    /**
     * 任务更新时间
     */
    private String updateTime;

    public List<TaskInfo> getTaskInfos() {
        return taskInfos;
    }

    public void setTaskInfos(List<TaskInfo> taskInfos) {
        this.taskInfos = taskInfos;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 玩家的任务详情
     */
    public static class TaskInfo {
        /**
         * 任务id
         */
        private Long taskId;

        /**
         * 当前完成进度
         */
        private Integer nowNum;

        /**
         * 任务目标数量
         */
        private Integer targetNum;

        /**
         * 任务当前状态：0-未完成，1-已完成，但是待领取，2-已领取
         */
        private Integer state;

        /**
         * 重置任务
         */
        public void reset() {
            this.nowNum = 0;
            this.state = 0;
        }

        private List<TaskReward> taskRewards;

        public Long getTaskId() {
            return taskId;
        }

        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }

        public Integer getNowNum() {
            return nowNum;
        }

        public void setNowNum(Integer nowNum) {
            this.nowNum = nowNum;
        }

        public Integer getTargetNum() {
            return targetNum;
        }

        public void setTargetNum(Integer targetNum) {
            this.targetNum = targetNum;
        }

        public Integer getState() {
            return state;
        }

        public void setState(Integer state) {
            this.state = state;
        }

        public List<TaskReward> getTaskRewards() {
            return taskRewards;
        }

        public void setTaskRewards(List<TaskReward> taskRewards) {
            this.taskRewards = taskRewards;
        }
    }

    /**
     * 任务奖励内容
     */
    public class TaskReward {
        /**
         * 物品id
         */
        private Integer itemId;

        /**
         * 物品数量
         */
        private Integer itemNum;

        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public Integer getItemNum() {
            return itemNum;
        }

        public void setItemNum(Integer itemNum) {
            this.itemNum = itemNum;
        }
    }
}
