package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 代理佣金收入详情记录
 */
public class AgentCommissionInfoEntity extends DbEntity {

    /**
     * 充值玩家ID
     */
    private Long playerId;

    /**
     * 充值玩家昵称
     */
    private String playerName;

    /**
     * 商品名称
     */
    private String shopName;

    /**
     * 渠道id
     */
    private Long channelId;

    /**
     * 玩家的上级代理玩家ID
     */
    private Long promoterId;

    /**
     * 渠道商赚的佣金(充值金额换算成金币数量*佣金比例)
     */
    private Double commission;

    /**
     * 推广员赚的佣金
     */
    private Double secCommission;

    /**
     * 玩家该笔交易充值的RMB数量(元)
     */
    private Double money;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getPromoterId() {
        return promoterId;
    }

    public void setPromoterId(Long promoterId) {
        this.promoterId = promoterId;
    }

    public Double getCommission() {
        return commission;
    }

    public void setCommission(Double commission) {
        this.commission = commission;
    }

    public Double getSecCommission() {
        return secCommission;
    }

    public void setSecCommission(Double secCommission) {
        this.secCommission = secCommission;
    }

    public Double getMoney() {
        return money;
    }

    public void setMoney(Double money) {
        this.money = money;
    }
}
