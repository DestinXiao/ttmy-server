package com.maple.game.osee.entity.lobby.csv;

import com.maple.engine.anotation.AppData;
import com.maple.engine.data.BaseCsvData;

/**
 * 机器人名配置
 */
@AppData(fileUrl = "data/cfg_robot_name.csv")
public class RobotNameConfig extends BaseCsvData {

	/**
	 * 昵称
	 */
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
