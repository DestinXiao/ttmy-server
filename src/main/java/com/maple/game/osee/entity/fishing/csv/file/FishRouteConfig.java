package com.maple.game.osee.entity.fishing.csv.file;

import org.springframework.util.StringUtils;

import com.maple.engine.anotation.AppData;
import com.maple.engine.data.BaseCsvData;

/**
 * 捕鱼路线配置表
 */
@AppData(fileUrl = "data/fishing/cfg_fish_route.csv")
public class FishRouteConfig extends BaseCsvData {

	/**
	 * 轨迹id
	 */
	private long routeId;

	/**
	 * 持续时长
	 */
	private float time;

	/**
	 * 停顿时间
	 */
	private String stopTime;

	/**
	 * 是否读取过停顿时间
	 */
	private boolean read;

	public long getRouteId() {
		return routeId;
	}

	public void setRouteId(long routeId) {
		this.routeId = routeId;
	}

	public float getTime() {
		if (!read) { // 避免每次调取get方法都会对整个对象加锁
			synchronized (this) {
				if (!read) { // 避免出现多线程数据问题
					if (!StringUtils.isEmpty(stopTime)) {
						for (String stop : stopTime.split(",")) {
							time += Float.parseFloat(stop.split("_")[1]);
						}
					}

					read = true;
				}
			}
		}

		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public String getStopTime() {
		return stopTime;
	}

	public void setStopTime(String stopTime) {
		this.stopTime = stopTime;
	}

}
