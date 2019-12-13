package com.maple.game.osee.entity.robot.fishing;

import com.maple.game.osee.entity.fishing.FishingGamePlayer;

import io.netty.util.internal.ThreadLocalRandom;

/**
 * 捕鱼机器人
 */
public class FishingGameRobot extends FishingGamePlayer {

	/**
	 * 机器人金币
	 */
	private long money;

	/**
	 * vip等级
	 */
	private int vipLevel;

	/**
	 * 等级
	 */
	private int level = ThreadLocalRandom.current().nextInt(1, 9);

	/**
	 * 最后开火指向鱼id
	 */
	private long lastFireFishId;

	/**
	 * 刷新时间
	 */
	private long refreshTime = System.currentTimeMillis();

	/**
	 * 机器人更换炮台机率
	 */
	private int changeBatteryLevelProb = 50;

	@Override
	public long getMoney() {
		return money;
	}

	@Override
	public void addMoney(long count) {
		money += count;
	}

	public void setMoney(long money) {
		this.money = money;
	}

	@Override
	public int getVipLevel() {
		return vipLevel;
	}

	@Override
	public int getLevel() {
		return level;
	}

	public void setVipLevel(int vipLevel) {
		this.vipLevel = vipLevel;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public long getLastFireFishId() {
		return lastFireFishId;
	}

	public void setLastFireFishId(long lastFireFishId) {
		this.lastFireFishId = lastFireFishId;
	}

	public long getRefreshTime() {
		return refreshTime;
	}

	public void setRefreshTime(long refreshTime) {
		this.refreshTime = refreshTime;
	}

	public int getChangeBatteryLevelProb() {
		return changeBatteryLevelProb;
	}

	public void setChangeBatteryLevelProb(int changeBatteryLevelProb) {
		this.changeBatteryLevelProb = changeBatteryLevelProb;
	}

}
