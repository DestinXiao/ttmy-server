package com.maple.game.osee.entity;

/**
 * 物品id
 */
public enum ItemId {

    /**
     * 金币
     */
    MONEY(1, "金币"),

    /**
     * 银行金币
     */
    BANK_MONEY(2, "银行金币"),

    /**
     * 奖券
     */
    LOTTERY(3, "奖券"),

    /**
     * 钻石
     */
    DIAMOND(4, "钻石"),

    BRONZE_TORPEDO(5, "低阶龙珠"), //"青铜鱼雷"),

    SILVER_TORPEDO(6, "中阶龙珠"), //"白银鱼雷"),

    GOLD_TORPEDO(7, "高阶龙珠"), //"黄金鱼雷"),

    SKILL_LOCK(8, "锁定技能"),

    SKILL_FROZEN(9, "冰冻技能"),

    SKILL_FAST(10, "急速技能"),

    SKILL_CRIT(11, "暴击技能"),

    MONTH_CARD(12, "月卡"), // 单位为天

    BOSS_BUGLE(13, "BOSS号角"),

    QSZS_BATTERY_VIEW(14, "骑士之誓炮台外观"), // 单位为天

    BLNH_BATTERY_VIEW(15, "冰龙怒吼炮台外观"), // 单位为天

    LHTZ_BATTERY_VIEW(16, "莲花童子炮台外观"), // 单位为天

    SWHP_BATTERY_VIEW(17, "死亡火炮炮台外观"), // 单位为天

    DRAGON_CRYSTAL(18, "龙晶"),

    FEN_SHEN(19, "分身炮"),

    ;

    /**
     * 物品id
     */
    private int id;

    /**
     * 信息
     */
    private String info;

    ItemId(int id, String info) {
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

    /**
     * 根据id获取物品枚举
     */
    public static ItemId getItemIdById(int id) {
        for (ItemId item : ItemId.values()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

}
