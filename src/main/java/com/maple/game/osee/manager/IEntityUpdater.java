package com.maple.game.osee.manager;

import com.maple.game.osee.dao.data.entity.OseePlayerEntity;

/**
 * 玩家数据更新接口
 */
public interface IEntityUpdater {

	/**
	 * 玩家数据更新
	 */
	void entityUpdate(OseePlayerEntity entity);

}
