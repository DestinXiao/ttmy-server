package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 虚拟道具兑换日志
 */
public class OseeUnrealLotteryLogEntity extends DbEntity {

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
	 * 类型
	 */
	private int type;

	/**
	 * 兑换数量
	 */
	private int count;

	/**
	 * 消耗类型
	 */
	private int itemId;

	/**
	 * 消耗数量
	 */
	private long cost;

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

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	public long getCost() {
		return cost;
	}

	public void setCost(long cost) {
		this.cost = cost;
	}

}
