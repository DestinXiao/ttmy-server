package com.maple.game.osee.dao.log.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.maple.game.osee.dao.log.entity.OseeLotteryDrawLogEntity;

/**
 * 抽奖日志
 */
@Mapper
public interface OseeLotteryDrawLogMapper {

	String TABLE_NAME = "tbl_osee_lottery_draw_log";

	/**
	 * 插入记录
	 */
	@Insert("INSERT INTO " + TABLE_NAME + " (`player_id`, `item_id`, `item_num`) VALUES (#{log.playerId}, "
			+ "#{log.itemId}, #{log.itemNum})")
	void save(@Param("log") OseeLotteryDrawLogEntity log);

	/**
	 * 查询记录
	 */
	@Select("SELECT * FROM " + TABLE_NAME + " WHERE `player_id` = #{id} ORDER BY `id` DESC LIMIT 0, ${size}")
	List<OseeLotteryDrawLogEntity> getLotteryDrawLogs(@Param("id") long playerId, @Param("size") int size);

	/**
	 * 创建日志表
	 */
	@Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',"
			+ "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
			+ "`player_id` bigint(20) NOT NULL COMMENT '玩家id',`item_id` int(10) NOT NULL COMMENT '物品id',"
			+ "`item_num` bigint(20) NOT NULL COMMENT '物品数量',"
			+ "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
	void createTable();

}
