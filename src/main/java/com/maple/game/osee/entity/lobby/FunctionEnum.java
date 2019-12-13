package com.maple.game.osee.entity.lobby;

/**
 * 游戏内可控制显示与隐藏的模块
 *
 * @author Junlong
 */
public enum FunctionEnum {

    BUY_SKILL(1, "购买技能"),

    FIRST_CHARGE(2, "首充"),

    CHESS_CARDS(3, "棋牌：拼十、二八杠"),
    ;

    private int id;

    private String name;

    FunctionEnum(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
