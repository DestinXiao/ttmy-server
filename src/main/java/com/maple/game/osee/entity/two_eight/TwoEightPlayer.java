package com.maple.game.osee.entity.two_eight;

import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.gamebase.data.BaseGamePlayer;
import org.springframework.beans.factory.annotation.Autowired;

public class TwoEightPlayer extends BaseGamePlayer {


    //0-表示闲家,1-表示庄家，2-表示等待上庄
    private int role;

    //下注金币
    private Long[] betMoney;//0-顺，1-天，2-地

    private boolean isRobot =false;

    //金币数量
    private OseePlayerEntity entity ;

    //座位号（排序号）从0开始金币依次递减
    private int seatIndex;

    private long currentScore=0;

    private boolean inRoom =true;

    //玩家未下注次数,超过两次自动移除
    private int notBetNum =0;

    //赢钱数量(减去抽水金额)
    private long returnScore=0;

    public long getReturnScore() {
        return returnScore;
    }

    public void setReturnScore(long returnScore) {
        this.returnScore = returnScore;
    }

    public int getNotBetNum() {
        return notBetNum;
    }

    public void setNotBetNum(int notBetNum) {
        this.notBetNum = notBetNum;
    }

    public boolean isRobot() {
        return isRobot;
    }

    public void setRobot(boolean robot) {
        isRobot = robot;
    }

    public boolean isInRoom() {
        return inRoom;
    }

    public void setInRoom(boolean inRoom) {
        this.inRoom = inRoom;
    }

    public long getCurrentScore() {
        return currentScore;
    }

    public void setCurrentScore(long currentScore) {
        this.currentScore = currentScore;
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public void setSeatIndex(int seatIndex) {
        this.seatIndex = seatIndex;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }



    public OseePlayerEntity getEntity() {
        return entity;
    }

    public void setEntity(OseePlayerEntity entity) {
        this.entity = entity;
    }

    public synchronized void addBetMoney(long moneyNum, int type){
        betMoney[type]= betMoney[type]+moneyNum;
    }

    public synchronized long getBetMoney(int type){
        return betMoney[type];
    }

    public void roundSet(){
        betMoney=new Long[3];
        betMoney[0]=0L;
        betMoney[1]=0L;
        betMoney[2]=0L;
        this.currentScore=0;
    }
}
