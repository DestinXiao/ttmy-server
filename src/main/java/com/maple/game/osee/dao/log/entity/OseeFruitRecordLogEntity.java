package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;
/**
 * 水果拉霸日志
 */
public class OseeFruitRecordLogEntity extends DbEntity {
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
    private long cost;

    /**
     * 下注条数
     */
    private int lineNum;

    /**
     * 账户金额变动 （旧）
     */

    private long money;
    /**
     * 中奖详情
     */
    private String info;

    /**
     * 总计中奖金额
     */
    private long totalWin;

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

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public long getTotalWin() {
        return totalWin;
    }

    public void setTotalWin(long totalWin) {
        this.totalWin = totalWin;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }
}
