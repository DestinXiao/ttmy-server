package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 支出记录
 */
public class OseeExpendLogEntity extends DbEntity {

    private static final long serialVersionUID = 6610858581714447896L;

    /**
     * 用户id
     */
    private long userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 支出类型
     */
    private int payType;

    /**
     * 钻石
     */
    private long diamond;

    /**
     * 金币
     */
    private long money;

    /**
     * 奖券
     */
    private long lottery;

    private long skillLock; // 锁定
    private long skillFrozen; // 冰冻
    private long skillFast; // 急速
    private long skillCrit; // 暴击

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getPayType() {
        return payType;
    }

    public void setPayType(int payType) {
        this.payType = payType;
    }

    public long getDiamond() {
        return diamond;
    }

    public void setDiamond(long diamond) {
        this.diamond = diamond;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getLottery() {
        return lottery;
    }

    public void setLottery(long lottery) {
        this.lottery = lottery;
    }

    public long getSkillLock() {
        return skillLock;
    }

    public void setSkillLock(long skillLock) {
        this.skillLock = skillLock;
    }

    public long getSkillFrozen() {
        return skillFrozen;
    }

    public void setSkillFrozen(long skillFrozen) {
        this.skillFrozen = skillFrozen;
    }

    public long getSkillFast() {
        return skillFast;
    }

    public void setSkillFast(long skillFast) {
        this.skillFast = skillFast;
    }

    public long getSkillCrit() {
        return skillCrit;
    }

    public void setSkillCrit(long skillCrit) {
        this.skillCrit = skillCrit;
    }
}
