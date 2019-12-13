package com.maple.game.osee.entity;

/**
 * 账户变动原因
 */
public enum ItemChangeReason {

    USE_CDK(1, "CDK兑换"),

    THIRD_PARTY_RECHARGE(2, "第三方充值"),

    GM_RECHARGE(3, "后台充值"),

    GM_DEDUCT(4, "后台扣除"),

    SHOPPING(5, "商城兑换"),

    LOTTERY_PAY(6, "轮盘支付"),

    LOTTERY_WIN(7, "轮盘中奖"),

    SIGN_IN(8, "签到"),

    TASK_FINISH(9, "任务奖励"),

    AUTHENTICATION(10, "实名认证"),

    FIGHT_TEN_WIN(11, "真人拼十获胜"),

    FIGHT_TEN_LOSE(12, "真人拼十失败"),

    ERBA_GANG_WIN(13, "二八杠胜利"),

    ERBA_GANG_LOSE(14, "二八杠失败"),

    GOBANG_RESULT(15, "五子棋结算"),

//    GOBANG_LOSE(16, "五子棋失败"),

    GOBANG_FEE(17, "五子棋房费"),

    FISHING_RESULT(18, "捕鱼产出消耗"),

    FRUIT_LABA_FEE(19, "水果拉霸筹码消耗"),

    FRUIT_LABA_WIN(20, "水果拉霸中奖"),

    BANK_IN(21, "保险箱存入"),

    BANK_OUT(22, "保险箱取出"),

    WECHAT_SHARE(23, "微信分享"),

    ACCOUNT_SET(24, "账号设置"),

    AGENT_COMMISSION_EXCHANGE(25, "佣金兑换"),

    GIVE_GIFT(26, "礼物赠送"),

    USE_ITEM(27, "物品使用"),

    UNLOCK_BATTERY(28, "炮台解锁"),

    CHANGE_NICKNAME(29, "更改昵称"),

    LEVEL_UP(30, "升级奖励"),

    FIGHT_TEN_CHALLENGE_WIN(31, "拼十挑战赛获胜"),

    FIGHT_TEN_CHALLENGE_LOSE(32, "拼十挑战赛失败"),

    ACTIVE_AGENT(33, "全民推广奖励"),

    FISHING_GRANDPRIX_JOIN_ROOM(34, "捕鱼大奖赛入场"),
    ;

    /**
     * 原因id
     */
    private int id;

    /**
     * 原因说明
     */
    private String info;

    ItemChangeReason(int id, String info) {
        this.id = id;
        this.info = info;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
