package com.maple.game.osee.dao.log.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AppRankLogEntity {

    private int id; // 竞技模式玩家比赛排名表

    private long playerId;  // 玩家id

    private String nickname;    // 玩家昵称

    private int score;  // 玩家积分

    private int rank;   // 玩家排名

    private int mode;   // 竞技模式类型（1：大奖赛，2：全民赛，3：满人赛，4：道具赛）

    private int cost;   // 报名消耗钻石数

    private int games;

    private int change;   // 字段详情，请看表字段注释

//    private int reward_id;
    private AppRewardLogEntity reward;

    private int receive;

    private Date receiveTime;

    private Date createTime;

    private long emailId;

    private int type;

}
