package com.maple.game.osee.entity.fishing.task;

/**
 * 任务类型
 */
public enum TaskType {

    /**
     * 每日任务
     */
    DAILY(1),

    /**
     * 房间任务
     */
    ROOM(2),

    /**
     * 空枚举
     */
    EMPTY(0x7FFFFFFF);

    /**
     * 任务类型
     */
    private int id;

    private TaskType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * 根据id获取物品枚举
     */
    public static TaskType getTaskTypeById(int id) {
        for (TaskType type : TaskType.class.getEnumConstants()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }

}
