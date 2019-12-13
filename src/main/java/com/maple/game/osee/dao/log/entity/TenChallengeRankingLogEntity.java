package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

import java.util.Date;

/**
 * 拼十挑战赛玩家排行榜数据实体
 *
 * @author Junlong
 */
public class TenChallengeRankingLogEntity extends DbEntity {
    private static final long serialVersionUID = 50115657406893134L;

    private long userId;

    private String nickname;

    private int headIndex;

    private String headUrl;

    /**
     * 单局获胜金币
     */
    private long score;

    /**
     * 记录更新时间
     */
    private Date updateTime = new Date();

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getHeadIndex() {
        return headIndex;
    }

    public void setHeadIndex(int headIndex) {
        this.headIndex = headIndex;
    }

    public String getHeadUrl() {
        return headUrl;
    }

    public void setHeadUrl(String headUrl) {
        this.headUrl = headUrl;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
