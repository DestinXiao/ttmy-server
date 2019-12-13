package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 充值记录实体
 */
public class OseeRechargeLogEntity extends DbEntity {

	private static final long serialVersionUID = -1892095262961184920L;

	/**
	 * 订单号
	 */
	private String orderNum;

	/**
	 * 玩家id
	 */
	private long userId;

	/**
	 * 玩家昵称
	 */
	private String nickname;

	/**
	 * 支付金额
	 */
	private long payMoney;

	/**
	 * 商品名
	 */
	private String shopName;

	/**
	 * 商品类型
	 */
	private int shopType;

	/**
	 * 商品数量
	 */
	private int count;

	/**
	 * 创建人
	 */
	private String creator;

	/**
	 * 充值方式
	 */
	private int rechargeType;

	/**
	 * 订单状态
	 */
	private int orderState;

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

	public long getPayMoney() {
		return payMoney;
	}

	public void setPayMoney(long payMoney) {
		this.payMoney = payMoney;
	}

	public String getShopName() {
		return shopName;
	}

	public void setShopName(String shopName) {
		this.shopName = shopName;
	}

	public int getShopType() {
		return shopType;
	}

	public void setShopType(int shopType) {
		this.shopType = shopType;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public int getRechargeType() {
		return rechargeType;
	}

	public void setRechargeType(int rechargeType) {
		this.rechargeType = rechargeType;
	}

	public int getOrderState() {
		return orderState;
	}

	public void setOrderState(int orderState) {
		this.orderState = orderState;
	}

}
