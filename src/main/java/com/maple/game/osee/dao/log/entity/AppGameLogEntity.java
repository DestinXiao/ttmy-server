package com.maple.game.osee.dao.log.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AppGameLogEntity {

    private int id;

    private int number;

    private int income;

    private AppRewardLogEntity reward;

    private long stock;

    private int mode;

    private Date createTime;

    private int type;
}
