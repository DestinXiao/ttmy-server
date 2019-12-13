package com.maple.game.osee.dao.data.entity;

import com.maple.database.data.DbEntity;

/**
 * 1688 CDK实体类
 */
public class OseeCdkEntity extends DbEntity {

	private static final long serialVersionUID = -739784491987993967L;

	/**
	 * cdk
	 */
	private String cdk;

	/**
	 * 类型id
	 */
	private long typeId;

	/**
	 * cdk奖励
	 */
	private String rewards;

	/**
	 * 兑换人id
	 */
	private long userId;

	/**
	 * 兑换人昵称
	 */
	private String nickname;

	public String getCdk() {
		return cdk;
	}

	public void setCdk(String cdk) {
		this.cdk = cdk;
	}

	public long getTypeId() {
		return typeId;
	}

	public void setTypeId(long typeId) {
		this.typeId = typeId;
	}

	public String getRewards() {
		return rewards;
	}

	public void setRewards(String rewards) {
		this.rewards = rewards;
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

}
