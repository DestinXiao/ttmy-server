package com.maple.game.osee.dao.data.entity;

import com.maple.database.data.DbEntity;

/**
 * 1688 CDK类型实体类
 */
public class OseeCdkTypeEntity extends DbEntity {

	private static final long serialVersionUID = -1796324908033907023L;

	/**
	 * 类型名
	 */
	private String name;

	/**
	 * 开头字符
	 */
	private String startWith;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStartWith() {
		return startWith;
	}

	public void setStartWith(String startWith) {
		this.startWith = startWith;
	}

}
