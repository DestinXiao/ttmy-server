package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 实物兑换日志
 */
public class OseeRealLotteryLogEntity extends DbEntity {

    private static final long serialVersionUID = -2132481856793149572L;

    /**
     * 订单号
     */
    private String orderNum;

    /**
     * 兑换人id
     */
    private long userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 商品名
     */
    private String rewardName;

    /**
     * 兑换数量
     */
    private int count;

    /**
     * 消耗数量
     */
    private long cost;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 收货人
     */
    private String consignee;

    /**
     * 手机号
     */
    private String phoneNum;

    /**
     * 收货地址
     */
    private String address;

    /**
     * 订单状态 0-待发货 1-已发货 2-已拒绝
     */
    private int orderState;

    /**
     * 实物对应的库存物品ID
     */
    private long stockId;

    public String getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(String orderNum) {
        this.orderNum = orderNum;
    }

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

    public String getRewardName() {
        return rewardName;
    }

    public void setRewardName(String rewardName) {
        this.rewardName = rewardName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getConsignee() {
        return consignee;
    }

    public void setConsignee(String consignee) {
        this.consignee = consignee;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getOrderState() {
        return orderState;
    }

    public void setOrderState(int orderState) {
        this.orderState = orderState;
    }

    public long getStockId() {
        return stockId;
    }

    public void setStockId(long stockId) {
        this.stockId = stockId;
    }
}
