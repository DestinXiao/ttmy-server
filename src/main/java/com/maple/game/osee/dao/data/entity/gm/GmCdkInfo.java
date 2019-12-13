package com.maple.game.osee.dao.data.entity.gm;

/**
 * 后台CDK数据
 */
public class GmCdkInfo {

	/**
	 * cdk
	 */
	private String cdkey;

	/**
	 * 类型id
	 */
	private String typeName;

	/**
	 * 奖励
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

	public String getCdkey() {
		return cdkey;
	}

	public void setCdkey(String cdkey) {
		this.cdkey = cdkey;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
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
