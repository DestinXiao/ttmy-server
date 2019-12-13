package com.maple.game.osee.entity.fightten.config;

import java.io.Serializable;

/**
 * 拼十机器人配置
 */
public class RobotConfig implements Serializable {
    private static final long serialVersionUID = 3446776107426068895L;

    /**
     * 是否使用机器人 0:关闭 1:启用
     */
    private Integer useRobot;

    /**
     * 单个房间机器人数量
     */
    private Integer robotNum;

    /**
     * 机器人刷新阈值随机数前区间
     */
    private Integer refreshTimeRangeBegin;

    /**
     * 机器人刷新阈值随机数后区间
     */
    private Integer refreshTimeRangeEnd;

    /**
     * 机器人获胜金币占比(%)
     * <p>
     * 当前服务器所有机器人获胜金币总额在机器人金币数据总额（机器人获胜金币总额+机器人亏损金币总额）中占比低于60%（占比后台可控），
     * 则机器人将会控制发好牌功能，规则如下：
     * 1) 在发牌时控制自己的手牌大于玩家手牌。
     * 2) 如果单个房间存在多个机器人，则携带金币数量最少的机器人拥有当局最好牌型，该房间其他机器人以当局金币数量从低至高，从好到坏依次获得手牌。
     */
    private Integer winPercent;

    public Integer getWinPercent() {
        return winPercent;
    }

    public void setWinPercent(Integer winPercent) {
        this.winPercent = winPercent;
    }

    public Integer getUseRobot() {
        return useRobot;
    }

    public void setUseRobot(Integer useRobot) {
        this.useRobot = useRobot;
    }

    public Integer getRobotNum() {
        return robotNum;
    }

    public void setRobotNum(Integer robotNum) {
        this.robotNum = robotNum;
    }

    public Integer getRefreshTimeRangeBegin() {
        return refreshTimeRangeBegin;
    }

    public void setRefreshTimeRangeBegin(Integer refreshTimeRangeBegin) {
        this.refreshTimeRangeBegin = refreshTimeRangeBegin;
    }

    public Integer getRefreshTimeRangeEnd() {
        return refreshTimeRangeEnd;
    }

    public void setRefreshTimeRangeEnd(Integer refreshTimeRangeEnd) {
        this.refreshTimeRangeEnd = refreshTimeRangeEnd;
    }
}
