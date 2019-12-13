package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 代理提现记录
 */
public class AgentWithdrawLogEntity extends DbEntity {

    /**
     * 代理id
     */
    private Long agentId;

    /**
     * 提现金额
     */
    private Long money;

    /**
     * 提现银行
     */
    private String bank;

    /**
     * 户名
     */
    private String realName;

    /**
     * 卡号
     */
    private String bankNum;

    /**
     * 开户行
     */
    private String openBank;

    /**
     * 状态 0:待处理 1:已处理 2:已拒绝
     */
    private Integer state = 0;

    /**
     * 操作管理员
     */
    private String creator;

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getMoney() {
        return money;
    }

    public void setMoney(Long money) {
        this.money = money;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getBankNum() {
        return bankNum;
    }

    public void setBankNum(String bankNum) {
        this.bankNum = bankNum;
    }

    public String getOpenBank() {
        return openBank;
    }

    public void setOpenBank(String openBank) {
        this.openBank = openBank;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
}
