package com.maple.game.osee.dao.data.entity;

import com.maple.database.data.DbEntity;

/**
 * 天天摸鱼代理
 *
 * @author Junlong
 */
public class AgentEntity extends DbEntity {
    private static final long serialVersionUID = -7167632950616321841L;

    /**
     * 玩家ID
     */
    private Long playerId;

    /**
     * 玩家昵称
     */
    private String playerName;

    /**
     * 玩家代理级别: 1-一级代理 2-二级代理 3-普通会员
     */
    private Integer agentLevel;

    /**
     * 直属上级代理玩家ID
     */
    private Long agentPlayerId;

    /**
     * 上级玩家id
     */
    private Long upperPlayerId;

    /**
     * 一级代理佣金比例 0-100 整数
     */
    private Double firstCommissionRate;

    /**
     * 二级代理佣金比例 0-100 整数
     */
    private Double secondCommissionRate;

    /**
     * 总共赚的佣金
     */
    private Double totalCommission;

    /**
     * 总活跃金币
     */
    private Long totalActiveMoney = 0L;

    /**
     * 总活跃龙晶
     */
    private Long totalActiveDragonCrystal = 0L;

    /**
     * 邀请二维码
     */
    private String inviteQrCodeImg;

    /**
     * 邀请链接
     */
    private String inviteUrl;

    /**
     * 身份状态：0-正常，1-禁用
     */
    private Integer state;

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

    public Integer getAgentLevel() {
        return agentLevel;
    }

    public void setAgentLevel(Integer agentLevel) {
        this.agentLevel = agentLevel;
    }

    public Long getAgentPlayerId() {
        return agentPlayerId;
    }

    public void setAgentPlayerId(Long agentPlayerId) {
        this.agentPlayerId = agentPlayerId;
    }

    public Long getUpperPlayerId() {
        return upperPlayerId;
    }

    public void setUpperPlayerId(Long upperPlayerId) {
        this.upperPlayerId = upperPlayerId;
    }

    public Double getFirstCommissionRate() {
        return firstCommissionRate;
    }

    public void setFirstCommissionRate(Double firstCommissionRate) {
        this.firstCommissionRate = firstCommissionRate;
    }

    public Double getSecondCommissionRate() {
        return secondCommissionRate;
    }

    public void setSecondCommissionRate(Double secondCommissionRate) {
        this.secondCommissionRate = secondCommissionRate;
    }

    public Double getTotalCommission() {
        return totalCommission;
    }

    public void setTotalCommission(Double totalCommission) {
        this.totalCommission = totalCommission;
    }

    public Long getTotalActiveMoney() {
        return totalActiveMoney;
    }

    public void setTotalActiveMoney(Long totalActiveMoney) {
        this.totalActiveMoney = totalActiveMoney;
    }

    public Long getTotalActiveDragonCrystal() {
        return totalActiveDragonCrystal;
    }

    public void setTotalActiveDragonCrystal(Long totalActiveDragonCrystal) {
        this.totalActiveDragonCrystal = totalActiveDragonCrystal;
    }

    public String getInviteQrCodeImg() {
        return inviteQrCodeImg;
    }

    public void setInviteQrCodeImg(String inviteQrCodeImg) {
        this.inviteQrCodeImg = inviteQrCodeImg;
    }

    public String getInviteUrl() {
        return inviteUrl;
    }

    public void setInviteUrl(String inviteUrl) {
        this.inviteUrl = inviteUrl;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
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
}
