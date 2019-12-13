package com.maple.game.osee.entity.fishing.game;

/**
 * 捕鱼历史记录
 */
public class FishingHistoryStruct {

	/**
	 * 获取金币
	 */
	private long winMoney;

	/**
	 * 失去金币
	 */
	private long loseMoney;

	public FishingHistoryStruct(long winMoney, long loseMoney) {
		this.winMoney = winMoney;
		this.loseMoney = loseMoney;
	}

	public FishingHistoryStruct(String winMoneyStr, String loseMoneyStr) {
		try {
			this.winMoney = Long.parseLong(winMoneyStr);
			this.loseMoney = Long.parseLong(loseMoneyStr);
		} catch (Exception e) {
		}
	}

	public long getWinMoney() {
		return winMoney;
	}

	public void setWinMoney(long winMoney) {
		this.winMoney = winMoney;
	}

	public long getLoseMoney() {
		return loseMoney;
	}

	public void setLoseMoney(long loseMoney) {
		this.loseMoney = loseMoney;
	}

}
