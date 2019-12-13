package com.maple.game.osee.entity.fishing.task;

/**
 * 任务目标类型
 *
 * @author Junlong
 */
public enum GoalType {

    FISH(1, "打鱼任务"),

    FIRE(2, "子弹任务"),

    BOSS(3, "boss任务"),

    SIGN(4, "签到任务"),

    ONLINE(5, "在线时长"),

    SKILL(5, "技能任务"),

    GET_DIAMOND(6, "获得任意数量钻石"),

    MONEY_10W(7, "单次捕鱼获得10W金币"),

    FISH_50MULTI(8, "捕获50倍以上的鱼"),

    USE_ITEM(9, "使用道具物品"),

    EMPTY(0x7FFFFFFF, "空枚举");

    /**
     * 目标类型
     */
    private int id;

    private String info;

    GoalType(int id, String info) {
        this.id = id;
        this.info = info;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
