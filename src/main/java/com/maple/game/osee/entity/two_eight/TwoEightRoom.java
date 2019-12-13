package com.maple.game.osee.entity.two_eight;

import com.maple.game.osee.entity.GameEnum;
import com.maple.game.osee.entity.fightten.FightTenRoom;
import com.maple.game.osee.manager.two_eight.MoneyComparator;
import com.maple.game.osee.manager.two_eight.TwoEightRoomState;
import com.maple.gamebase.data.BaseGameRoom;

import java.util.*;

public class TwoEightRoom extends BaseGameRoom {


    public static final int[] TWOEIGHT_CARDS_DATA_ARRAY = {
            0,  1,  2,  3,  4,  5,  6,  7,  8,  9,//幺鸡-9筒
            0,  1,  2,  3,  4,  5,  6,  7,  8,  9,//
            0,  1,  2,  3,  4,  5,  6,  7,  8,  9,//
            0,  1,  2,  3,  4,  5,  6,  7,  8,  9//

    };


    public static Long toBeBankerMinMoney=10000000L;

    public static final int begainMinPlayers=2;//游戏开始最少玩家


    private int roomStatus;//0-不够九人，等待其他玩家加入，1-押注，2-摇骰子发牌，3-开牌


    //房间所有玩家
    private List<TwoEightPlayer> allPlayers = new ArrayList<>();

    //申请上庄的玩家列表
    private List<TwoEightPlayer> applyForBanker = new ArrayList<>();

    //下注玩家
    private Set<TwoEightPlayer> betPlayers = new HashSet<>();

    //顺门输赢情况0-赢 1-输
    private List<Integer> shunWinInfoList = new ArrayList<>();

    //天门输赢情况 0-赢 1-输
    private List<Integer> tianWinInfoList = new ArrayList<>();

    //地门输赢情况 0-赢 1-输
    private List<Integer> diWinInfoList = new ArrayList<>();



    //房间庄家
    private TwoEightPlayer banker;

    //顺天地 各个门所压得金币数量
    private long[] betMoney ;

    //顺天地 各个门下注详情
    private ArrayList<Long>[] betInfo;

    //顺天地庄各个牌 第一行是顺依次排列
    private int[][] cards =new int[3][2] ;

    /**
     * 进入状态时的时间:毫秒
     */
    private long enterStateTime;

    public List<Integer> getWinInfoList(int type) {
        if (type==0)
            return shunWinInfoList;
        else if (type==1)
            return tianWinInfoList;
        return diWinInfoList;
    }


    //骰子情况
    private List<Integer> bankerDice = new ArrayList<>();

    public long getEnterStateTime() {
        return enterStateTime;
    }

    public void setEnterStateTime(long enterStateTime) {
        this.enterStateTime = enterStateTime;
    }

    public Set<TwoEightPlayer> getBetPlayers() {
        return betPlayers;
    }

    public List<Integer> getBankerDice() {
        return bankerDice;
    }

    public void setBankerDice(List<Integer> bankerDice) {
        this.bankerDice = bankerDice;
    }

    public List<TwoEightPlayer> getAllPlayers() {
        return allPlayers;
    }

    public void setAllPlayers(List<TwoEightPlayer> allPlayers) {
        this.allPlayers = allPlayers;
    }


    public List<TwoEightPlayer> getApplyForBanker() {
        return applyForBanker;
    }

    public void setApplyForBanker(List<TwoEightPlayer> applyForBanker) {
        this.applyForBanker = applyForBanker;
    }

    public int getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(int roomStatus) {
        this.roomStatus = roomStatus;
    }


    public TwoEightPlayer getBanker() {
        return banker;
    }

    public void setBanker(TwoEightPlayer banker) {
        this.banker = banker;
    }

    public  synchronized Long getBetMoney(int type) {
        return betMoney[type];
    }

    public synchronized void addBetMoney(long money,int type) {
        this.betMoney[type]=this.betMoney[type]+money;
    }

    public synchronized Long getTotalMoney(){
        return betMoney[0]+betMoney[1]+betMoney[2];
    }

    public void addBetInfo(long betNum,int betType){
        betInfo[betType].add(betNum);
    }

    public int[][] getCards() {
        return cards;
    }

    public ArrayList<Long>[] getBetInfo() {
        return betInfo;
    }

    public void addWinInfo(int type,int win){
        if (type==0){
            if (shunWinInfoList.size()>=20){
                shunWinInfoList.remove(0);
            }
            shunWinInfoList.add(win);
        }else if (type==1){
            if (tianWinInfoList.size()>=20){
                tianWinInfoList.remove(0);
            }
            tianWinInfoList.add(win);
        }else {
            if (diWinInfoList.size()>=20){
                diWinInfoList.remove(0);
            }
            diWinInfoList.add(win);
        }
    }

    @Override
    public int getGameId() {
        return GameEnum.ERBA_GAME.getId();
    }

    public int getSeatIndex(Long playerId){
        Collections.sort(allPlayers,new MoneyComparator());
        int i=0;
        for (TwoEightPlayer player:allPlayers){
            if (player.getId()==playerId){
                return i;
            }
            i++;
        }
        return i;
    }

    public void roundSet() {
        this.betMoney = new long[3];
        this.betInfo = new ArrayList[3];
        betInfo[0]= new ArrayList<>();
        betInfo[1]= new ArrayList<>();
        betInfo[2]= new ArrayList<>();
        this.bankerDice.clear();
        this.betPlayers.clear();
        cards=new int[4][2];


    }
}
