package com.maple.game.osee.entity.fishing.csv.file;

import com.google.gson.Gson;
import com.maple.engine.anotation.AppData;
import com.maple.engine.data.BaseCsvData;
import com.maple.game.osee.entity.ItemData;

import java.util.Arrays;
import java.util.List;

/**
 * 玩家等级配置表
 */
@AppData(fileUrl = "data/fishing/cfg_player_level.csv")
public class PlayerLevelConfig extends BaseCsvData {

    /**
     * 等级
     */
    private int level;

    /**
     * 升级所需经验
     */
    private long exp;

    /**
     * 升级奖励
     */
    private String rewards;

    private List<ItemData> realRewards;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public String getRewards() {
        return rewards;
    }

    public void setRewards(String rewards) {
        this.rewards = rewards;
    }

    public List<ItemData> getRealRewards() {
        if (realRewards == null) {
            ItemData[] itemData = new Gson().fromJson(rewards, ItemData[].class);
            realRewards = Arrays.asList(itemData);
        }
        return realRewards;
    }

    public void setRealRewards(List<ItemData> realRewards) {
        this.realRewards = realRewards;
    }
}
