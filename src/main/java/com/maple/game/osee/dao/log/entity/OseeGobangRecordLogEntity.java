package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 五子棋记录日志
 */
public class OseeGobangRecordLogEntity extends DbEntity {

	private static final long serialVersionUID = -3956934850109324770L;

	/**
	 * 学费
	 */
	private long money;

	/**
	 * 获胜者id
	 */
	private long winnerId;

	/**
	 * 获胜者昵称
	 */
	private String winnerNickname;

	/**
	 * 获胜前金币
	 */
	private long winnerBeforeMoney;

	/**
	 * 获胜后金币
	 */
	private long winnerAfterMoney;

	/**
	 * 失败者id
	 */
	private long loserId;

	/**
	 * 失败者昵称
	 */
	private String loserNickname;

	/**
	 * 失败前金币
	 */
	private long loserBeforeMoney;

	/**
	 * 失败后金币
	 */
	private long loserAfterMoney;

	public long getMoney() {
		return money;
	}

	public void setMoney(long money) {
		this.money = money;
	}

	public long getWinnerId() {
		return winnerId;
	}

	public void setWinnerId(long winnerId) {
		this.winnerId = winnerId;
	}

	public String getWinnerNickname() {
		return winnerNickname;
	}

	public void setWinnerNickname(String winnerNickname) {
		this.winnerNickname = winnerNickname;
	}

	public long getWinnerBeforeMoney() {
		return winnerBeforeMoney;
	}

	public void setWinnerBeforeMoney(long winnerBeforeMoney) {
		this.winnerBeforeMoney = winnerBeforeMoney;
	}

	public long getWinnerAfterMoney() {
		return winnerAfterMoney;
	}

	public void setWinnerAfterMoney(long winnerAfterMoney) {
		this.winnerAfterMoney = winnerAfterMoney;
	}

	public long getLoserId() {
		return loserId;
	}

	public void setLoserId(long loserId) {
		this.loserId = loserId;
	}

	public String getLoserNickname() {
		return loserNickname;
	}

	public void setLoserNickname(String loserNickname) {
		this.loserNickname = loserNickname;
	}

	public long getLoserBeforeMoney() {
		return loserBeforeMoney;
	}

	public void setLoserBeforeMoney(long loserBeforeMoney) {
		this.loserBeforeMoney = loserBeforeMoney;
	}

	public long getLoserAfterMoney() {
		return loserAfterMoney;
	}

	public void setLoserAfterMoney(long loserAfterMoney) {
		this.loserAfterMoney = loserAfterMoney;
	}

}
