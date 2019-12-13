package com.maple.game.osee.dao.data.entity;

import com.maple.database.data.DbEntity;

/**
 * 水果拉霸转满N次所能得到的奖励
 * @author lzr
 *
 * 2018年12月27日
 */
public class FruitLaBaRewardInfo extends DbEntity{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8636834567467231197L;
	
	/**
	 * 用户id
	 */
	private long userId;
	
	/**
	 * 转满的次数
	 */
	private int achieveNum=0;
	
	/**
	 * 奖励的金币
	 */
	private int rewardGold=0;
	
	/**
	 * 奖励的点券
	 */
	private int rewardLottery=0;
	
	/**
	 * 是否领取
	 */
	private boolean weatherReceive=false;

	public int getAchieveNum() {
		return achieveNum;
	}

	public void setAchieveNum(int achieveNum) {
		this.achieveNum = achieveNum;
	}

	public int getRewardGold() {
		return rewardGold;
	}

	public void setRewardGold(int rewardGold) {
		this.rewardGold = rewardGold;
	}

	public int getRewardLottery() {
		return rewardLottery;
	}

	public void setRewardLottery(int rewardLottery) {
		this.rewardLottery = rewardLottery;
	}

	public boolean isWeatherReceive() {
		return weatherReceive;
	}

	public void setWeatherReceive(boolean weatherReceive) {
		this.weatherReceive = weatherReceive;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}
	
}
