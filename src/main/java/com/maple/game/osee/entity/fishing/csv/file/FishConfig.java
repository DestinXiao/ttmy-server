package com.maple.game.osee.entity.fishing.csv.file;

import com.maple.engine.anotation.AppData;
import com.maple.engine.data.BaseCsvData;

/**
 * 鱼配置
 */
@AppData(fileUrl = "data/fishing/cfg_fish.csv")
public class FishConfig extends BaseCsvData {

    /**
     * 名称
     */
    private String name;

    /**
     * 模型id
     */
    private int modelId;

    /**
     * 鱼类型 100-boss鱼 其他-暂时无关
     */
    private int fishType;

//    /**
//     * 出现场景
//     */
//    private String scene;
//
//    /**
//     * 真实出现场景
//     */
//    private int[] realScene;

    /**
     * 基础金币
     */
    private int money;

    /**
     * 最大金币
     */
    private int maxMoney;

    /**
     * 最小安全值
     */
    private int minSafe;

    /**
     * 最大安全值
     */
    private int maxSafe;

    /**
     * 攻击值
     */
    private int attack;

    /**
     * 生命值
     */
    private int health;

    /**
     * 经验值
     */
    private int exp;

    /**
     * 附带技能
     */
    private int skill;

    /**
     * 最少掉落技能数量
     */
    private int minSkillDropNum;

    /**
     * 最大掉落技能数量
     */
    private int maxSkillDropNum;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public int getFishType() {
        return fishType;
    }

    public void setFishType(int fishType) {
        this.fishType = fishType;
    }

//    public String getScene() {
//        return scene;
//    }
//
//    public void setScene(String scene) {
//        this.scene = scene;
//    }
//
//    public int[] getRealScene() {
//        if (realScene == null) {
//            realScene = GsonManager.gson.fromJson(scene, int[].class);
//        }
//
//        return realScene;
//    }
//
//    public void setRealScene(int[] realScene) {
//        this.realScene = realScene;
//    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getMaxMoney() {
        return maxMoney;
    }

    public void setMaxMoney(int maxMoney) {
        this.maxMoney = maxMoney;
    }

    public int getMinSafe() {
        return minSafe;
    }

    public void setMinSafe(int minSafe) {
        this.minSafe = minSafe;
    }

    public int getMaxSafe() {
        return maxSafe;
    }

    public void setMaxSafe(int maxSafe) {
        this.maxSafe = maxSafe;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getSkill() {
        return skill;
    }

    public void setSkill(int skill) {
        this.skill = skill;
    }

    public int getMinSkillDropNum() {
        return minSkillDropNum;
    }

    public void setMinSkillDropNum(int minSkillDropNum) {
        this.minSkillDropNum = minSkillDropNum;
    }

    public int getMaxSkillDropNum() {
        return maxSkillDropNum;
    }

    public void setMaxSkillDropNum(int maxSkillDropNum) {
        this.maxSkillDropNum = maxSkillDropNum;
    }
}
