package com.maple.game.osee.dao.data.entity;

import com.maple.database.data.DbEntity;

/**
 * 奖券商城物品库存-京东卡
 *
 * @author Junlong
 */
public class StockEntity extends DbEntity {
    private static final long serialVersionUID = -2659024045790209348L;

    /**
     * 该道具库存数据属于的商品ID
     */
    private Long shopId;

    /**
     * 兑换人ID
     */
    private Long userId;

    /**
     * 卡号
     */
    private String number;

    /**
     * 卡密
     */
    private String password;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
