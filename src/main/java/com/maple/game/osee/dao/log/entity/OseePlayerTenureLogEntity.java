package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 用户账户变动日志
 */
public class OseePlayerTenureLogEntity extends DbEntity {

    private static final long serialVersionUID = 3928922029028562872L;

    /**
     * 用户id
     */
    private long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 变动来源
     */
    private int reason;

    /**
     * 变动前钻石
     */
    private long preDiamond;

    /**
     * 变动钻石
     */
    private long changeDiamond;

    /**
     * 变动前金币
     */
    private long preMoney;

    /**
     * 变动金币
     */
    private long changeMoney;

    /**
     * 变动前奖券
     */
    private long preLottery;

    /**
     * 变动奖券
     */
    private long changeLottery;

    /**
     * 变动前保险箱金币
     */
    private long preBankMoney;

    /**
     * 变动保险箱金币
     */
    private long changeBankMoney;

    /**
     * 变动前青铜鱼雷数量
     */
    private long preBronzeTorpedo;

    /**
     * 变动的青铜鱼雷数量
     */
    private long changeBronzeTorpedo;

    /**
     * 变动前白银鱼雷数量
     */
    private long preSilverTorpedo;

    /**
     * 变动的白银鱼雷数量
     */
    private long changeSilverTorpedo;

    /**
     * 变动前黄金鱼雷数量
     */
    private long preGoldTorpedo;

    /**
     * 变动的黄金鱼雷数量
     */
    private long changeGoldTorpedo;

    /**
     * 锁定技能变动
     */
    private long preSkillLock;
    private long changeSkillLock;

    /**
     * 冰冻技能变动
     */
    private long preSkillFrozen;
    private long changeSkillFrozen;

    /**
     * 急速技能变动
     */
    private long preSkillFast;
    private long changeSkillFast;

    /**
     * 暴击技能变动
     */
    private long preSkillCrit;
    private long changeSkillCrit;

    /**
     * boss号角变动
     */
    private long preBossBugle;
    private long changeBossBugle;

    /**
     * 附加值
     */
    private String extraData;

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

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public long getPreDiamond() {
        return preDiamond;
    }

    public void setPreDiamond(long preDiamond) {
        this.preDiamond = preDiamond;
    }

    public long getChangeDiamond() {
        return changeDiamond;
    }

    public void setChangeDiamond(long changeDiamond) {
        this.changeDiamond = changeDiamond;
    }

    public long getPreMoney() {
        return preMoney;
    }

    public void setPreMoney(long preMoney) {
        this.preMoney = preMoney;
    }

    public long getChangeMoney() {
        return changeMoney;
    }

    public void setChangeMoney(long changeMoney) {
        this.changeMoney = changeMoney;
    }

    public long getPreLottery() {
        return preLottery;
    }

    public void setPreLottery(long preLottery) {
        this.preLottery = preLottery;
    }

    public long getChangeLottery() {
        return changeLottery;
    }

    public void setChangeLottery(long changeLottery) {
        this.changeLottery = changeLottery;
    }

    public long getPreBankMoney() {
        return preBankMoney;
    }

    public void setPreBankMoney(long preBankMoney) {
        this.preBankMoney = preBankMoney;
    }

    public long getChangeBankMoney() {
        return changeBankMoney;
    }

    public void setChangeBankMoney(long changeBankMoney) {
        this.changeBankMoney = changeBankMoney;
    }

    public long getPreBronzeTorpedo() {
        return preBronzeTorpedo;
    }

    public void setPreBronzeTorpedo(long preBronzeTorpedo) {
        this.preBronzeTorpedo = preBronzeTorpedo;
    }

    public long getChangeBronzeTorpedo() {
        return changeBronzeTorpedo;
    }

    public void setChangeBronzeTorpedo(long changeBronzeTorpedo) {
        this.changeBronzeTorpedo = changeBronzeTorpedo;
    }

    public long getPreSilverTorpedo() {
        return preSilverTorpedo;
    }

    public void setPreSilverTorpedo(long preSilverTorpedo) {
        this.preSilverTorpedo = preSilverTorpedo;
    }

    public long getChangeSilverTorpedo() {
        return changeSilverTorpedo;
    }

    public void setChangeSilverTorpedo(long changeSilverTorpedo) {
        this.changeSilverTorpedo = changeSilverTorpedo;
    }

    public long getPreGoldTorpedo() {
        return preGoldTorpedo;
    }

    public void setPreGoldTorpedo(long preGoldTorpedo) {
        this.preGoldTorpedo = preGoldTorpedo;
    }

    public long getChangeGoldTorpedo() {
        return changeGoldTorpedo;
    }

    public void setChangeGoldTorpedo(long changeGoldTorpedo) {
        this.changeGoldTorpedo = changeGoldTorpedo;
    }

    public long getPreSkillLock() {
        return preSkillLock;
    }

    public void setPreSkillLock(long preSkillLock) {
        this.preSkillLock = preSkillLock;
    }

    public long getChangeSkillLock() {
        return changeSkillLock;
    }

    public void setChangeSkillLock(long changeSkillLock) {
        this.changeSkillLock = changeSkillLock;
    }

    public long getPreSkillFrozen() {
        return preSkillFrozen;
    }

    public void setPreSkillFrozen(long preSkillFrozen) {
        this.preSkillFrozen = preSkillFrozen;
    }

    public long getChangeSkillFrozen() {
        return changeSkillFrozen;
    }

    public void setChangeSkillFrozen(long changeSkillFrozen) {
        this.changeSkillFrozen = changeSkillFrozen;
    }

    public long getPreSkillFast() {
        return preSkillFast;
    }

    public void setPreSkillFast(long preSkillFast) {
        this.preSkillFast = preSkillFast;
    }

    public long getChangeSkillFast() {
        return changeSkillFast;
    }

    public void setChangeSkillFast(long changeSkillFast) {
        this.changeSkillFast = changeSkillFast;
    }

    public long getPreSkillCrit() {
        return preSkillCrit;
    }

    public void setPreSkillCrit(long preSkillCrit) {
        this.preSkillCrit = preSkillCrit;
    }

    public long getChangeSkillCrit() {
        return changeSkillCrit;
    }

    public void setChangeSkillCrit(long changeSkillCrit) {
        this.changeSkillCrit = changeSkillCrit;
    }

    public long getPreBossBugle() {
        return preBossBugle;
    }

    public void setPreBossBugle(long preBossBugle) {
        this.preBossBugle = preBossBugle;
    }

    public long getChangeBossBugle() {
        return changeBossBugle;
    }

    public void setChangeBossBugle(long changeBossBugle) {
        this.changeBossBugle = changeBossBugle;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

}
