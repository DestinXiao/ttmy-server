package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 代理佣金收入记录
 *
 * @author Junlong
 */
public class AgentCommissionEntity extends DbEntity {
    private static final long serialVersionUID = 263808844889012331L;

    /**
     * 充值玩家ID
     */
    private Long playerId;

    /**
     * 充值玩家昵称
     */
    private String playerName;

    /**
     * 玩家的上级代理玩家ID
     */
    private Long agentPlayerId;

    /**
     * 代理玩家昵称
     */
    private String agentPlayerName;

    /**
     * 计算该佣金时的比例
     */
    private Double commissionRate;

    /**
     * 该笔单赚的佣金(充值金额换算成金币数量*佣金比例)
     */
    private Double commission;

    /**
     * 玩家该笔交易充值的RMB数量(元)
     */
    private Long money;

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

    public Long getAgentPlayerId() {
        return agentPlayerId;
    }

    public void setAgentPlayerId(Long agentPlayerId) {
        this.agentPlayerId = agentPlayerId;
    }

    public String getAgentPlayerName() {
        return agentPlayerName;
    }

    public void setAgentPlayerName(String agentPlayerName) {
        this.agentPlayerName = agentPlayerName;
    }

    public Double getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(Double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public Double getCommission() {
        return commission;
    }

    public void setCommission(Double commission) {
        this.commission = commission;
    }

    public Long getMoney() {
        return money;
    }

    public void setMoney(Long money) {
        this.money = money;
    }
}
