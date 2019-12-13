package com.maple.game.osee.dao.log.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AppRewardLogEntity {

    private int id;

    private long playerId;

    private int gold;

    private int diamond;

    private int lowerBall;

    private int middleBall;

    private int highBall;

    private int skillLock;

    private int skillFast;

    private int skillCrit;

    private int skillFrozen;

    private int bossBugle;

    public void add(int gold, int diamond, int lowerBall, int middleBall, int highBall, int skillLock, int skillFast, int skillCrit, int skillFrozen, int bossBugle) {
        this.gold += gold;
        this.diamond += diamond;
        this.lowerBall += lowerBall;
        this.middleBall += middleBall;
        this.highBall += highBall;
        this.skillLock += skillLock;
        this.skillFast += skillFast;
        this.skillCrit += skillCrit;
        this.skillFrozen += skillFrozen;
        this.bossBugle += bossBugle;
    }

}
