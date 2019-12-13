package com.maple.game.osee.entity.fishing.csv.file;

import com.maple.engine.anotation.AppData;

/**
 * 1688技能配置
 */
@AppData(fileUrl = "data/fishing/cfg_skill.csv")
public class SkillConfig {

	/**
	 * 技能持续时间
	 */
	private int duration;

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

}
