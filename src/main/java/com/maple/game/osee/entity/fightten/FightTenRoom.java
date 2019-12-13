package com.maple.game.osee.entity.fightten;

import com.maple.game.osee.entity.GameEnum;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.data.BaseGameRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼十房间
 */
public class FightTenRoom extends BaseGameRoom {

    /**
     * 房间状态枚举
     */
    public enum RoomState {
        NONE(0, 0), // 无
        READY(1, 15), // 准备
        DISPATCH_CARD(2, 5), // 发牌
        FIGHT_BANKER(3, 10), // 抢庄
        FIGHT_BANKER_ANI(7, 5), // 抢庄随机庄家动画阶段
        BET_MONEY(4, 5), // 下注
        SEE_CARD(5, 10), // 看牌
        OVER(6, 8) // 结算 还要加上人数*1秒
        ;
        private int index;

        /**
         * 阶段持续时间：秒
         */
        private int time;

        RoomState(int index, int time) {
            this.index = index;
            this.time = time;
        }

        /**
         * 根据状态索引获取状态的持续时间
         */
        public static int getTimeByIndex(int index) {
            for (RoomState value : RoomState.values()) {
                if (index == value.getIndex()) {
                    return value.getTime();
                }
            }
            return 0;
        }

        /**
         * 根据序号获取枚举值
         */
        public static RoomState getEnumByIndex(int index) {
            for (RoomState value : RoomState.values()) {
                if (index == value.getIndex()) {
                    return value;
                }
            }
            return NONE;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }
    }

    /**
     * 最少玩家数
     */
    public static final int MIN_PLAYER_NUM = 2;

    /**
     * 最大玩家数
     */
    public static final int MAX_PLAYER_NUM = 5;

    /**
     * 房间状态
     */
    private Integer roomState;

    /**
     * 房间所属场次：0-初、1-中、2-高
     */
    private Integer fieldType;

    /**
     * 进入某个状态的时间：秒为单位
     */
    private Long enterStateTime;

    /**
     * 庄家
     */
    private FightTenPlayer banker;

    /**
     * 房间最高庄家倍数
     */
    private Integer maxFightMultiple = 1;

    /**
     * 随机选择庄家的玩家列表
     */
    private List<Long> fightBankerRandomPlayerIdList = new ArrayList<>(MAX_PLAYER_NUM);

    /**
     * 最高下注上限
     */
    private Long maxBetMoney;

    /**
     * 上次创建机器人时间
     */
    private Long lastCreateRobotTime = System.currentTimeMillis();

    public Integer getFieldType() {
        return fieldType;
    }

    public void setFieldType(Integer fieldType) {
        this.fieldType = fieldType;
    }

    public Integer getRoomState() {
        return roomState;
    }

    public void setRoomState(Integer roomState) {
        this.roomState = roomState;
        // 改变状态时就给进入改状态的时间赋值
        this.enterStateTime = System.currentTimeMillis() / 1000;
    }

    public Long getEnterStateTime() {
        return enterStateTime;
    }

    public void setEnterStateTime(Long enterStateTime) {
        this.enterStateTime = enterStateTime;
    }

    public FightTenPlayer getBanker() {
        return banker;
    }

    public void setBanker(FightTenPlayer banker) {
        this.banker = banker;
    }

    public Integer getMaxFightMultiple() {
        return maxFightMultiple;
    }

    public void setMaxFightMultiple(Integer maxFightMultiple) {
        this.maxFightMultiple = maxFightMultiple;
    }

    public List<Long> getFightBankerRandomPlayerIdList() {
        return fightBankerRandomPlayerIdList;
    }

    public void setFightBankerRandomPlayerIdList(List<Long> fightBankerRandomPlayerIdList) {
        this.fightBankerRandomPlayerIdList = fightBankerRandomPlayerIdList;
    }

    public Long getMaxBetMoney() {
        return maxBetMoney;
    }

    public void setMaxBetMoney(Long maxBetMoney) {
        this.maxBetMoney = maxBetMoney;
    }

    public Long getLastCreateRobotTime() {
        return lastCreateRobotTime;
    }

    public void setLastCreateRobotTime(Long lastCreateRobotTime) {
        this.lastCreateRobotTime = lastCreateRobotTime;
    }

    /**
     * 对局数据重置
     */
    public void roundReset() {
        banker = null;
        maxFightMultiple = 1;
        fightBankerRandomPlayerIdList.clear();
        for (BaseGamePlayer baseGamePlayer : getGamePlayers()) {
            if (baseGamePlayer != null) {
                // 玩家数据重置
                ((FightTenPlayer) baseGamePlayer).roundReset();
            }
        }
    }

    @Override
    public int getGameId() {
        return GameEnum.FIGHT_TEN.getId();
    }

}
