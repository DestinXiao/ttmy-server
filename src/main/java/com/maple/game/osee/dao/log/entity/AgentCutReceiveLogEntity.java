package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 领取全民推广奖励记录
 */
public class AgentCutReceiveLogEntity extends DbEntity {

    /**
     * 代理id
     */
    private Long agentId;

    /**
     * 兑换时昵称
     */
    private String agentName;

    /**
     * 兑换金币数量
     */
    private Long money;

    /**
     * 兑换龙晶数量
     */
    private Long dragonCrystal;

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public Long getMoney() {
        return money;
    }

    public void setMoney(Long money) {
        this.money = money;
    }

    public Long getDragonCrystal() {
        return dragonCrystal;
    }

    public void setDragonCrystal(Long dragonCrystal) {
        this.dragonCrystal = dragonCrystal;
    }
}
