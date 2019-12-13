package com.maple.game.osee.entity.fruitlaba;

import com.maple.game.osee.manager.PlayerManager;
import com.maple.gamebase.data.BaseGamePlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FruitlabaPlayer extends BaseGamePlayer {

    private int roomType;
    private boolean REBACK_FLAG = false; //本次游戏是否返水

    private int minimum = 5;//保底参数

    private Boolean needMinimum;// 本次摇奖是否命中二级倍率及以上 即用来判断保底参数是否需要-1

    private Double rate3Add = 0.0; //三级倍率提升

    private Double rate4Add = 0.0; //四级倍率提升

    private long lastSingleGold = 0;//上次投注金币

    private long EnterMoney = 0; //玩家进入房间的金币账户金额

    private long REBACK_LINE; //返水参数线 花费高于此线则返水

    private Integer reback = -1 ;//返水次数 默认 -1 不返水

    private long fruitCost; //玩家抽奖消耗金币数量

    private long playBeforeMoney; //玩家抽奖之前账户金币余额

    private Boolean isFree=false;

    private long baodi5 = 0;//低于五条线的保底累计金额

    private int fruitId;//中奖的水果id

    private int lastLineRate=0;//上次中奖线的倍数

    private Map<Integer, Integer> Win_Multiple_MAP =  new ConcurrentHashMap<>();

    /**
     * 获取玩家的金币数量
     *
     * @return 金币数量
     */
    public long getMoney() {
        return PlayerManager.getPlayerMoney(getUser());
    }
    /**
     * 清零命中提升参数
     */
    public void resetRateAdd(){
        this.rate3Add = 0.0;
        this.rate4Add = 0.0;
    }

    public void mininumConsume(){//减少 1
        this.minimum--;
    }
    public void rebackConsume(){//减少 1
        this.reback--;
    }

    public boolean isREBACK_FLAG() {
        return REBACK_FLAG;
    }
    public void setREBACK_FLAG(boolean REBACK_FLAG) {
        this.REBACK_FLAG = REBACK_FLAG;
    }
    public int getRoomType() {
        return roomType;
    }
    public void setRoomType(int roomType) {
        this.roomType = roomType;
    }

    public int getMinimum() {
        return minimum;
    }

    public void setMinimum(int minimum) {
        this.minimum = minimum;
    }

    public Double getRate3Add() {
        return rate3Add;
    }

    public void setRate3Add(Double rate3Add) {
        this.rate3Add = rate3Add;
    }

    public Double getRate4Add() {
        return rate4Add;
    }

    public void setRate4Add(Double rate4Add) {
        this.rate4Add = rate4Add;
    }

    public long getLastSingleGold() {
        return lastSingleGold;
    }

    public void setLastSingleGold(long lastSingleGold) {
        this.lastSingleGold = lastSingleGold;
    }

    public long getEnterMoney() {
        return EnterMoney;
    }

    public void setEnterMoney(long enterMoney) {
        EnterMoney = enterMoney;
    }

    public long getREBACK_LINE() {
        return REBACK_LINE;
    }

    public void setREBACK_LINE(long REBACK_LINE) {
        this.REBACK_LINE = REBACK_LINE;
    }

    public Integer getReback() {
        return reback;
    }

    public void setReback(Integer reback) {
        this.reback = reback;
    }

    public long getFruitCost() {
        return fruitCost;
    }

    public void setFruitCost(long fruitCost) {
        this.fruitCost = fruitCost;
    }

    public long getPlayBeforeMoney() {
        return playBeforeMoney;
    }

    public void setPlayBeforeMoney(long playBeforeMoney) {
        this.playBeforeMoney = playBeforeMoney;
    }

    public Boolean getFree() {
        return isFree;
    }

    public void setFree(Boolean free) {
        isFree = free;
    }

    public long getBaodi5() {
        return baodi5;
    }

    public void setBaodi5(long baodi5) {
        this.baodi5 = baodi5;
    }

    public Boolean getNeedMinimum() {
        return needMinimum;
    }

    public void setNeedMinimum(Boolean needMinimum) {
        this.needMinimum = needMinimum;
    }

    public Map<Integer, Integer> getWin_Multiple_MAP() {
        return Win_Multiple_MAP;
    }

    public int getFruitId() {
        return fruitId;
    }

    public void setFruitId(int fruitId) {
        this.fruitId = fruitId;
    }

    public int getLastLineRate() {
        return lastLineRate;
    }

    public void setLastLineRate(int lastLineRate) {
        this.lastLineRate = lastLineRate;
    }

    public void setWin_Multiple_MAP(Map<Integer, Integer> win_Multiple_MAP) {
        Win_Multiple_MAP = win_Multiple_MAP;
    }
}
