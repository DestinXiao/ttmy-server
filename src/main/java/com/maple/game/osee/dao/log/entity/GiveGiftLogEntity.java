package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 礼物赠送记录数据实体类
 *
 * @author Junlong
 */
public class GiveGiftLogEntity extends DbEntity {
    private static final long serialVersionUID = 3644454256677699237L;

    private Long fromId;        // 赠送人id

    private String fromName;    // 赠送人昵称

    private Long toId;          // 被赠人id

    private String toName;      // 被赠人昵称

    private String giftName;    // 赠送的礼物名称

    private Long giftNum;       // 赠送的礼物数量

    public Long getFromId() {
        return fromId;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public Long getToId() {
        return toId;
    }

    public void setToId(Long toId) {
        this.toId = toId;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getGiftName() {
        return giftName;
    }

    public void setGiftName(String giftName) {
        this.giftName = giftName;
    }

    public Long getGiftNum() {
        return giftNum;
    }

    public void setGiftNum(Long giftNum) {
        this.giftNum = giftNum;
    }
}
