package com.maple.game.osee.entity.lobby;

import java.util.Date;

/**
 * 微信分享
 */
public class WechatShare {

	/**
	 * 分享日期
	 */
	private Date shareDate;

	public WechatShare(Date shareDate) {
		this.shareDate = shareDate;
	}

	public Date getShareDate() {
		return shareDate;
	}

	public void setShareDate(Date shareDate) {
		this.shareDate = shareDate;
	}

}
