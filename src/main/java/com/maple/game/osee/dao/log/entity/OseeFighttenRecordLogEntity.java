package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;
/**
 * 拼十日志
 */
public class OseeFighttenRecordLogEntity extends DbEntity {

    /**
     * 玩家id
     */
    private long playerId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 下注金额
     */
    private long input;

    /**
     * 牌型倍数
     */
    private int rate;


    /**
     * 牌型
     */
    private String cardType;

    /**
     * 账户金币变动数额
     */
    private long money;

    /**
     * 游戏前剩余金币
     */
    private long playBeforeMoney;

    /**
     * 游戏后剩余金币
     */
    private long playAfterMoney;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getPlayBeforeMoney() {
        return playBeforeMoney;
    }

    public void setPlayBeforeMoney(long playBeforeMoney) {
        this.playBeforeMoney = playBeforeMoney;
    }

    public long getPlayAfterMoney() {
        return playAfterMoney;
    }

    public void setPlayAfterMoney(long playAfterMoney) {
        this.playAfterMoney = playAfterMoney;
    }

    public long getInput() {
        return input;
    }

    public void setInput(long input) {
        this.input = input;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }
}
