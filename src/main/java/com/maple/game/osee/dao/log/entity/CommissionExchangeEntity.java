package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 代理佣金兑换记录实体类
 *
 * @author Junlong
 */
public class CommissionExchangeEntity extends DbEntity {
    private static final long serialVersionUID = 6884870441737777913L;

    private Long agentId;               // 代理ID

    private String agentName;           // 代理昵称

    private Long bronzeTorpedoNum = 0L; // 青铜鱼雷数量

    private Long silverTorpedoNum = 0L; // 白银鱼雷数量

    private Long goldTorpedoNum = 0L;   // 黄金鱼雷数量

    private Long goldNum = 0L;          // 金币数量

    private Long costCommission;        // 扣除的佣金

    private Long restCommission;        // 该次兑换后剩余的佣金

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

    public Long getBronzeTorpedoNum() {
        return bronzeTorpedoNum;
    }

    public void setBronzeTorpedoNum(Long bronzeTorpedoNum) {
        this.bronzeTorpedoNum = bronzeTorpedoNum;
    }

    public Long getSilverTorpedoNum() {
        return silverTorpedoNum;
    }

    public void setSilverTorpedoNum(Long silverTorpedoNum) {
        this.silverTorpedoNum = silverTorpedoNum;
    }

    public Long getGoldTorpedoNum() {
        return goldTorpedoNum;
    }

    public void setGoldTorpedoNum(Long goldTorpedoNum) {
        this.goldTorpedoNum = goldTorpedoNum;
    }

    public Long getGoldNum() {
        return goldNum;
    }

    public void setGoldNum(Long goldNum) {
        this.goldNum = goldNum;
    }

    public Long getCostCommission() {
        return costCommission;
    }

    public void setCostCommission(Long costCommission) {
        this.costCommission = costCommission;
    }

    public Long getRestCommission() {
        return restCommission;
    }

    public void setRestCommission(Long restCommission) {
        this.restCommission = restCommission;
    }
}
