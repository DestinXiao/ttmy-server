package com.maple.game.osee.dao.data.entity.gm;

import java.util.Date;

/**
 * 后台认证记录查询结果
 */
public class GmAuthenticationInfo {

	/**
	 * 认证记录id
	 */
	private long id;
	
	/**
	 * 认证时间
	 */
	private Date createTime;
	
	/**
	 * 玩家id
	 */
	private long playerId;
	
	/**
	 * 玩家昵称
	 */
	private String nickName;
	
	/**
	 * 真实姓名
	 */
	private String realName;

	/**
	 * 身份证号
	 */
	private String idCardNum;

	/**
	 * 手机号
	 */
	private String phoneNum;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(long playerId) {
		this.playerId = playerId;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public String getIdCardNum() {
		return idCardNum;
	}

	public void setIdCardNum(String idCardNum) {
		this.idCardNum = idCardNum;
	}

	public String getPhoneNum() {
		return phoneNum;
	}

	public void setPhoneNum(String phoneNum) {
		this.phoneNum = phoneNum;
	}

}
