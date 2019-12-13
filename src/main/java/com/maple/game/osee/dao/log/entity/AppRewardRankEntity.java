package com.maple.game.osee.dao.log.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AppRewardRankEntity {

    private int id;
    private String name;
    private int rank;
    private int type;
    private int status;
    private AppRewardLogEntity reward;
    private Date updateTime;
}
