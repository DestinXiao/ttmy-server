package com.maple.game.osee.entity.fishing.csv.file;

import org.springframework.util.StringUtils;

import com.google.gson.Gson;
import com.maple.engine.anotation.AppData;
import com.maple.engine.data.BaseCsvData;
import com.maple.engine.manager.GsonManager;

/**
 * 捕鱼鱼群表
 */
@AppData(fileUrl = "data/fishing/cfg_fish_group.csv")
public class FishGroupConfig extends BaseCsvData {

	/**
	 * 鱼群组合
	 */
	private String group;

	/**
	 * 真实鱼群组合
	 */
	private Long[] realGroup;

	/**
	 * 刷新延迟
	 */
	private String delay;

	/**
	 * 真实刷新延迟
	 */
	private Double[] realDelay;

	/**
	 * 鱼群属性
	 */
	private int type;

//	/**
//	 * 出现场次
//	 */
//	private String scene;
//
//	/**
//	 * 真实出现场次
//	 */
//	private Integer[] realScene;

	/**
	 * 路线id
	 */
	private String routeId;

	/**
	 * 真实路线id
	 */
	private Long[] realRouteId;

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public Long[] getRealGroup() {
		if (realGroup == null) {
			realGroup = StringUtils.isEmpty(group) ? new Long[0] : GsonManager.gson.fromJson(group, Long[].class);
		}
		return realGroup;
	}

	public void setRealGroup(Long[] realGroup) {
		this.realGroup = realGroup;
	}

	public String getDelay() {
		return delay;
	}

	public void setDelay(String delay) {
		this.delay = delay;
	}

	public Double[] getRealDelay() {
		if (realDelay == null) {
			realDelay = StringUtils.isEmpty(delay) ? new Double[0] : new Gson().fromJson(delay, Double[].class);
		}
		return realDelay;
	}

	public void setRealDelay(Double[] realDelay) {
		this.realDelay = realDelay;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

//	public String getScene() {
//		return scene;
//	}
//
//	public void setScene(String scene) {
//		this.scene = scene;
//	}
//
//	public Integer[] getRealScene() {
//		if (realScene == null) {
//			realScene = StringUtils.isEmpty(scene) ? new Integer[0] : GsonManager.gson.fromJson(scene, Integer[].class);
//		}
//		return realScene;
//	}
//
//	public void setRealScene(Integer[] realScene) {
//		this.realScene = realScene;
//	}

	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public Long[] getRealRouteId() {
		if (realRouteId == null) {
			realRouteId = StringUtils.isEmpty(routeId) ? new Long[0] : GsonManager.gson.fromJson(routeId, Long[].class);
		}
		return realRouteId;
	}

	public void setRealRouteId(Long[] realRouteId) {
		this.realRouteId = realRouteId;
	}

}
