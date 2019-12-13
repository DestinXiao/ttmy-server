package com.maple.game.osee.entity.fishing.csv.file;

import com.maple.engine.anotation.AppData;
import com.maple.engine.data.BaseCsvData;
import com.maple.engine.manager.GsonManager;
import org.springframework.util.StringUtils;

/**
 * 捕鱼刷新规则
 */
@AppData(fileUrl = "data/fishing/cfg_fish_refresh_rule.csv")
public class FishRefreshRule extends BaseCsvData implements Comparable<FishRefreshRule> {

    /**
     * 起始鱼群
     */
    private long start;

    /**
     * 结束鱼群
     */
    private long end;

    /**
     * 最小间隔
     */
    private int minDelay;

    /**
     * 最大间隔
     */
    private int maxDelay;

    /**
     * 是否为鱼潮
     */
    private boolean fishTide;

    /**
     * 是否动态刷新
     */
    private boolean dynamicRefresh;

    /**
     * 是否是boss刷新规则
     */
    private boolean boss;

    /**
     * 出现场次
     */
    private String scene;

    /**
     * 真实出现场次
     */
    private Integer[] realScene;

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getMinDelay() {
        return minDelay;
    }

    public void setMinDelay(int minDelay) {
        this.minDelay = minDelay;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(int maxDelay) {
        this.maxDelay = maxDelay;
    }

    public boolean isFishTide() {
        return fishTide;
    }

    public void setFishTide(boolean fishTide) {
        this.fishTide = fishTide;
    }

    public boolean isDynamicRefresh() {
        return dynamicRefresh;
    }

    public void setDynamicRefresh(boolean dynamicRefresh) {
        this.dynamicRefresh = dynamicRefresh;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public Integer[] getRealScene() {
        if (realScene == null) {
            realScene = StringUtils.isEmpty(scene) ? new Integer[0] : GsonManager.gson.fromJson(scene, Integer[].class);
        }
        return realScene;
    }

    public void setRealScene(Integer[] realScene) {
        this.realScene = realScene;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }

    @Override
    public int compareTo(FishRefreshRule o) {
        return Long.compare(start, o.start);
    }

}
