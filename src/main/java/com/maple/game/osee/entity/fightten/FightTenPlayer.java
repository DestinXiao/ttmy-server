package com.maple.game.osee.entity.fightten;

import com.maple.game.osee.manager.PlayerManager;
import com.maple.gamebase.data.BaseGamePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼十玩家
 */
public class FightTenPlayer extends BaseGamePlayer {

    /**
     * 玩家进入房间的时间
     */
    public final Long enterTime = System.currentTimeMillis();

    /**
     * 玩家在拼十中上次操作时间
     */
    private Long lastOptionTime = System.currentTimeMillis();

    /**
     * 准备类型：0-已准备，1-未准备
     */
    private Integer readyType = 1;

    /**
     * 玩家手牌数据
     */
    private List<Integer> cards = new ArrayList<>(5);

    /**
     * 抢庄倍数，默认为1倍
     * -1 = 不抢
     */
    private Integer fightMultiple;

    /**
     * 玩家下注金额列表
     */
    private List<Long> betMoneyList = new ArrayList<>(4);

    /**
     * 玩家下注金额
     */
    private Long betMoney;

    /**
     * 是否看牌或搓牌了
     */
    private Boolean seeOrRubCard = false;

    /**
     * 玩家牌类型
     */
    private Integer cardType = 0;

    /**
     * 输赢情况：1-赢，0-输
     */
    private Integer win = -1;

    /**
     * 玩家单局输赢的钱
     */
    private Long winMoney = 0L;

    /**
     * 获取玩家的金币数量
     *
     * @return 金币数量
     */
    public long getMoney() {
        return PlayerManager.getPlayerMoney(getUser());
    }

    public Long getLastOptionTime() {
        return lastOptionTime;
    }

    public void setLastOptionTime(Long lastOptionTime) {
        this.lastOptionTime = lastOptionTime;
    }

    public Integer getReadyType() {
        return readyType;
    }

    public void setReadyType(Integer readyType) {
        this.readyType = readyType;
    }

    public List<Integer> getCards() {
        return cards;
    }

    public void setCards(List<Integer> cards) {
        this.cards = cards;
    }

    public Integer getFightMultiple() {
        return fightMultiple;
    }

    public void setFightMultiple(Integer fightMultiple) {
        this.fightMultiple = fightMultiple;
    }

    public List<Long> getBetMoneyList() {
        return betMoneyList;
    }

    public void setBetMoneyList(List<Long> betMoneyList) {
        this.betMoneyList = betMoneyList;
    }

    public Long getBetMoney() {
        return betMoney;
    }

    public void setBetMoney(Long betMoney) {
        this.betMoney = betMoney;
    }

    public Boolean getSeeOrRubCard() {
        return seeOrRubCard;
    }

    public void setSeeOrRubCard(Boolean seeOrRubCard) {
        this.seeOrRubCard = seeOrRubCard;
    }

    public Integer getCardType() {
        return cardType;
    }

    public void setCardType(Integer cardType) {
        this.cardType = cardType;
    }

    public Integer getWin() {
        return win;
    }

    public void setWin(Integer win) {
        this.win = win;
    }

    public Long getWinMoney() {
        return winMoney;
    }

    public void setWinMoney(Long winMoney) {
        this.winMoney = winMoney;
    }

    /**
     * 玩家对局数据重置
     */
    public void roundReset() {
        readyType = 1;
        cardType = 0;
        win = -1;
        winMoney = 0L;
        cards.clear();
        fightMultiple = null;
        betMoney = null;
        seeOrRubCard = false;
    }
}
