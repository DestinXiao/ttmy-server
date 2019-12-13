package com.maple.game.osee.dao.data.entity;

import com.maple.database.data.DbEntity;

import java.time.LocalDate;

/**
 * 玩家实体类
 */
public class OseePlayerEntity extends DbEntity {

    private static final long serialVersionUID = 6524085625364080223L;

    /**
     * 实体id
     */
    public static final String EntityId = "ttmy";

    /**
     * 玩家id
     */
    private long userId;

    /**
     * 金币
     */
    private long money;

    /**
     * 保险箱金币
     */
    private long bankMoney;

    /**
     * 保险箱密码
     */
    private String bankPassword = "e10adc3949ba59abbe56e057f20f883e";

    /**
     * 奖券
     */
    private long lottery;

    /**
     * 钻石
     */
    private long diamond;

    /**
     * vip等级
     */
    private int vipLevel;

    /**
     * 玩家等级
     */
    private int level = 1;

    /**
     * 玩家经验
     */
    private long experience;

    /**
     * 充值金额
     */
    private long rechargeMoney;

    /**
     * 必输控制标识
     */
    private int loseControl;

    /**
     * 代理标识
     */
    private int playerType;

    /**
     * 青铜鱼雷
     */
    private long bronzeTorpedo;

    /**
     * 白银鱼雷
     */
    private long silverTorpedo;

    /**
     * 黄金鱼雷
     */
    private long goldTorpedo;

    /**
     * 锁定技能
     */
    private long skillLock;

    /**
     * 冰冻技能
     */
    private long skillFrozen;

    /**
     * 急速技能
     */
    private long skillFast;

    /**
     * 暴击技能
     */
    private long skillCrit;

    /**
     * boss号角
     */
    private long bossBugle;

    /**
     * 玩家目前所拥有的最高炮台等级
     */
    private int batteryLevel;

    /**
     * 月卡到期时间
     */
    private LocalDate monthCardExpireDate = LocalDate.now();

    /**
     * 玩家剩余拼十挑战次数
     */
    private long tenChallengeTimes;

    /**
     * 骑士之誓炮台外观到期时间
     */
    private LocalDate qszsBatteryExpireDate = LocalDate.now();

    /**
     * 冰龙怒吼炮台外观到期时间
     */
    private LocalDate blnhBatteryExpireDate = LocalDate.now();

    /**
     * 莲花童子炮台外观到期时间
     */
    private LocalDate lhtzBatteryExpireDate = LocalDate.now();

    /**
     * 死亡火炮炮台外观到期时间
     */
    private LocalDate swhpBatteryExpireDate = LocalDate.now();

    /**
     * 龙晶数量
     */
    private long dragonCrystal;

    /**
     * 分身炮道具
     */
    private long fenShen;

    public long getDragonCrystal() {
        return dragonCrystal;
    }

    public void setDragonCrystal(long dragonCrystal) {
        this.dragonCrystal = dragonCrystal;
    }

    public LocalDate getQszsBatteryExpireDate() {
        return qszsBatteryExpireDate;
    }

    public void setQszsBatteryExpireDate(LocalDate qszsBatteryExpireDate) {
        this.qszsBatteryExpireDate = qszsBatteryExpireDate;
    }

    public LocalDate getBlnhBatteryExpireDate() {
        return blnhBatteryExpireDate;
    }

    public void setBlnhBatteryExpireDate(LocalDate blnhBatteryExpireDate) {
        this.blnhBatteryExpireDate = blnhBatteryExpireDate;
    }

    public LocalDate getLhtzBatteryExpireDate() {
        return lhtzBatteryExpireDate;
    }

    public void setLhtzBatteryExpireDate(LocalDate lhtzBatteryExpireDate) {
        this.lhtzBatteryExpireDate = lhtzBatteryExpireDate;
    }

    public LocalDate getSwhpBatteryExpireDate() {
        return swhpBatteryExpireDate;
    }

    public void setSwhpBatteryExpireDate(LocalDate swhpBatteryExpireDate) {
        this.swhpBatteryExpireDate = swhpBatteryExpireDate;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getBankMoney() {
        return bankMoney;
    }

    public void setBankMoney(long bankMoney) {
        this.bankMoney = bankMoney;
    }

    public String getBankPassword() {
        return bankPassword;
    }

    public void setBankPassword(String bankPassword) {
        this.bankPassword = bankPassword;
    }

    public long getLottery() {
        return lottery;
    }

    public void setLottery(long lottery) {
        this.lottery = lottery;
    }

    public long getDiamond() {
        return diamond;
    }

    public void setDiamond(long diamond) {
        this.diamond = diamond;
    }

    public int getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(int vipLevel) {
        this.vipLevel = vipLevel;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = experience;
    }

    public long getRechargeMoney() {
        return rechargeMoney;
    }

    public void setRechargeMoney(long rechargeMoney) {
        this.rechargeMoney = rechargeMoney;
    }

    public int getLoseControl() {
        return loseControl;
    }

    public void setLoseControl(int loseControl) {
        this.loseControl = loseControl;
    }

    public int getPlayerType() {
        return playerType;
    }

    public void setPlayerType(int playerType) {
        this.playerType = playerType;
    }

    public long getBronzeTorpedo() {
        return bronzeTorpedo;
    }

    public void setBronzeTorpedo(long bronzeTorpedo) {
        this.bronzeTorpedo = bronzeTorpedo;
    }

    public long getSilverTorpedo() {
        return silverTorpedo;
    }

    public void setSilverTorpedo(long silverTorpedo) {
        this.silverTorpedo = silverTorpedo;
    }

    public long getGoldTorpedo() {
        return goldTorpedo;
    }

    public void setGoldTorpedo(long goldTorpedo) {
        this.goldTorpedo = goldTorpedo;
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

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public LocalDate getMonthCardExpireDate() {
        return monthCardExpireDate;
    }

    public void setMonthCardExpireDate(LocalDate monthCardExpireDate) {
        this.monthCardExpireDate = monthCardExpireDate;
    }

    public long getTenChallengeTimes() {
        return tenChallengeTimes;
    }

    public void setTenChallengeTimes(long tenChallengeTimes) {
        this.tenChallengeTimes = tenChallengeTimes;
    }

    public long getBossBugle() {
        return bossBugle;
    }

    public void setBossBugle(long bossBugle) {
        this.bossBugle = bossBugle;
    }

    public long getFenShen() {
        return fenShen;
    }

    public void setFenShen(long fenShen) {
        this.fenShen = fenShen;
    }
}
