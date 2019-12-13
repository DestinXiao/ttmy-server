package com.maple.game.osee.entity.gobang;

import com.maple.gamebase.data.gobang.BaseGobangPlayer;

/**
 * 1688五子棋玩家
 */
public class GobangGamePlayer extends BaseGobangPlayer {

	/**
	 * 是否已准备
	 */
	private boolean ready;

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}
}
