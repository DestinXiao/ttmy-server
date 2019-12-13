package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 代理活跃抽水日志
 */
public class AgentCutLogEntity extends DbEntity {

    /**
     * 玩家id
     */
    private Long playerId;

    /**
     * 代理id
     */
    private long agentId;

    /**
     * 具体游戏
     */
    private int game;

    /**
     * 抽水金币
     */
    private long cutMoney;

    /**
     * 抽水龙晶
     */
    private long cutDragonCrystal;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public long getAgentId() {
        return agentId;
    }

    public void setAgentId(long agentId) {
        this.agentId = agentId;
    }

    public int getGame() {
        return game;
    }

    public void setGame(int game) {
        this.game = game;
    }

    public long getCutMoney() {
        return cutMoney;
    }

    public void setCutMoney(long cutMoney) {
        this.cutMoney = cutMoney;
    }

    public long getCutDragonCrystal() {
        return cutDragonCrystal;
    }

    public void setCutDragonCrystal(long cutDragonCrystal) {
        this.cutDragonCrystal = cutDragonCrystal;
    }
}
