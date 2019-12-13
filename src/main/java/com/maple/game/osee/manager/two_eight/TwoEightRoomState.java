package com.maple.game.osee.manager.two_eight;

//二八杠房间状态
public class TwoEightRoomState {
    /**
     * 未开始阶段
     */
    public static final int NOTBEGIN =0;

    /**
     * 下注阶段
     */
    public static final int DOBET = 1;

    /**
     * 摇骰子阶段
     */
    public static final int SHAKEDICE =2;

    /**
     * 发牌阶段
     */
    public static final int GETCARDS=3;

    /**
     * 结算阶段
     */
    public static final int ROUNDOVER=4;

    /**
     * 下一局开始前需要等待一会
     */
    public static final int NEXTROUND=5;


}
