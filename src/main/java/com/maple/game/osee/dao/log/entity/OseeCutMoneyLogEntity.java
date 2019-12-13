package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 抽水记录
 */
public class OseeCutMoneyLogEntity extends DbEntity {

    private static final long serialVersionUID = 4648992166890925961L;

    /**
     * 用户id
     */
    private long userId;

//    /**
//     * 用户昵称
//     */
//    private String nickname;

    /**
     * 游戏
     */
    private int game;

//    /**
//     * 抽水前收入
//     */
//    private int beforeMoney;

    /**
     * 抽水金币
     */
    private long cutMoney;

    private Integer type;

    public Integer getType() {
        return type;
    }
    public void setType(Integer type) {
        this.type = type;
    }


    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

//    public String getNickname() {
//        return nickname;
//    }
//
//    public void setNickname(String nickname) {
//        this.nickname = nickname;
//    }

    public int getGame() {
        return game;
    }

    public void setGame(int game) {
        this.game = game;
    }

//    public int getBeforeMoney() {
//        return beforeMoney;
//    }
//
//    public void setBeforeMoney(int beforeMoney) {
//        this.beforeMoney = beforeMoney;
//    }

    public long getCutMoney() {
        return cutMoney;
    }

    public void setCutMoney(long cutMoney) {
        this.cutMoney = cutMoney;
    }

}
