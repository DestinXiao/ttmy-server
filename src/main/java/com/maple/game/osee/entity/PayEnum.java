package com.maple.game.osee.entity;

/**
 * 支出类型
 */
public enum PayEnum {

	AUTHENTICATION(1, "实名认证"),
	TASK_FINISH(2, "完成任务"),
	SIGN_IN(3, "每日签到"),
	USE_CDK(4, "CDK"),

	;

	/**
	 * id
	 */
	private final int id;

	/**
	 * 枚举名
	 */
	private final String name;

	private PayEnum(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/**
	 * 根据id获取枚举
	 */
	public static PayEnum getItemIdById(int id) {
		for (PayEnum item : PayEnum.values()) {
			if (item.getId() == id) {
				return item;
			}
		}
		return null;
	}

}
