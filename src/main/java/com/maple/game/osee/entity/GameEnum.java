package com.maple.game.osee.entity;

/**
 * 游戏枚举
 */
public enum GameEnum {

    FISHING(2, "捕鱼"),

    FIGHT_TEN(3, "拼十"),

    ERBA_GAME(4, "二八杠"),

    FRUIT_LABA(5, "水果拉霸"),

    GOBANG(6, "五子棋"),

    FISHING_CHALLENGE_1(7, "龙晶战场1号场"),

    FISHING_CHALLENGE_2(8, "龙晶战场2号场"),

    FISHING_CHALLENGE_3(9, "龙晶战场VIP场"),

    FISHING_GRANDPRIX(10, "大奖赛"),

    ;

    /**
     * 游戏id
     */
    private final int id;

    /**
     * 游戏名
     */
    private final String name;

    private GameEnum(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据id获取物品枚举
     */
    public static GameEnum getItemIdById(int id) {
        for (GameEnum item : GameEnum.values()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

}
