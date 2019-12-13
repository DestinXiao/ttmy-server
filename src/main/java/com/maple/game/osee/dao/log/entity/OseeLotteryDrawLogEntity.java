package com.maple.game.osee.dao.log.entity;

import com.maple.database.data.DbEntity;

/**
 * 抽奖记录日志
 */
public class OseeLotteryDrawLogEntity extends DbEntity {

	private static final long serialVersionUID = 8343343478965684189L;

	/**
	 * 玩家id
	 */
	private long playerId;

	/**
	 * 奖品序号
	 */
	private int itemId;

	/**
	 * 奖品数量
	 */
	private int itemNum;

	public long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(long playerId) {
		this.playerId = playerId;
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	public int getItemNum() {
		return itemNum;
	}

	public void setItemNum(int itemNum) {
		this.itemNum = itemNum;
	}

}
