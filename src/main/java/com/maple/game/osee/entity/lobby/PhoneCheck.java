package com.maple.game.osee.entity.lobby;

import java.util.Date;

/**
 * 实名认证手机验证
 */
public class PhoneCheck {

	/**
	 * 发送验证码时间
	 */
	private Date checkTime;

	/**
	 * 验证码
	 */
	private int checkCode;

	/**
	 * 手机号
	 */
	private String phoneNum;

	public Date getCheckTime() {
		return checkTime;
	}

	public void setCheckTime(Date checkTime) {
		this.checkTime = checkTime;
	}

	public int getCheckCode() {
		return checkCode;
	}

	public void setCheckCode(int checkCode) {
		this.checkCode = checkCode;
	}

	public String getPhoneNum() {
		return phoneNum;
	}

	public void setPhoneNum(String phoneNum) {
		this.phoneNum = phoneNum;
	}

}
