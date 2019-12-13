package com.maple.game.osee.dao.data.entity;

import java.util.Date;

import com.maple.database.data.DbEntity;

/**
 * 1688 公告实体类
 */
public class OseeNoticeEntity extends DbEntity {

	private static final long serialVersionUID = 6454770930028030394L;

	/**
	 * 序号
	 */
	private int index;

	/**
	 * 标题
	 */
	private String title;

	/**
	 * 内容
	 */
	private String content;

	/**
	 * 生效时间
	 */
	private Date startTime;

	/**
	 * 失效时间
	 */
	private Date endTime;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

}
