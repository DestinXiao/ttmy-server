package com.maple.game.osee.dao.log.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.maple.game.osee.dao.log.entity.OseeGobangRecordLogEntity;

/**
 * 1688五子棋记录日志接口
 */
@Mapper
public interface OseeGobangRecordLogMapper {

	String TABLE_NAME = "tbl_osee_gobang_record_log";

	/**
	 * 保存数据
	 */
	@Insert("INSERT INTO " + TABLE_NAME + " (`money`, `winner_id`, `winner_nickname`, `winner_before_money`, "
			+ "`winner_after_money`, `loser_id`, `loser_nickname`, `loser_before_money`, `loser_after_money`) "
			+ "VALUES (#{entity.money}, #{entity.winnerId}, #{entity.winnerNickname}, #{entity.winnerBeforeMoney}, "
			+ "#{entity.winnerAfterMoney}, #{entity.loserId}, #{entity.loserNickname}, #{entity.loserBeforeMoney}, "
			+ "#{entity.loserAfterMoney})")
	void save(@Param("entity") OseeGobangRecordLogEntity entity);

	/**
	 * 根据条件查询记录
	 */
	@Select("SELECT * FROM " + TABLE_NAME + " log ${where} ORDER BY log.id DESC ${page}")
	List<OseeGobangRecordLogEntity> getLogList(@Param("where") String where, @Param("page") String page);

	/**
	 * 根据条件查询记录数量
	 */
	@Select("SELECT COUNT(*) totalNum FROM " + TABLE_NAME + " log ${where}")
	int getLogCount(@Param("where") String where);


	/**
	 * 从log表中 累加计算条件范围内 记录的 获胜赢取总额 totalMoney
	 */
	@Select("SELECT COALESCE(SUM(money),0) total FROM " + TABLE_NAME + " log ${where}")
	long getTotalWin(@Param("where") String where);

	/**
	 * 从log表中 累加计算条件范围内 记录的 失败输掉总额 totalLose
	 */
	@Select("SELECT COALESCE(SUM(money),0) total FROM " + TABLE_NAME + " log ${where}")
	long getTotalLose(@Param("where") String where);



	/**
	 * 创建表
	 */
	@Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',"
			+ "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
			+ "`money` bigint(20) NULL COMMENT '学费',"
			+ "`winner_id` bigint(20) NULL DEFAULT NULL COMMENT '获胜者id',"
			+ "`winner_nickname` varchar(32) NOT NULL COMMENT '获胜者昵称',"
			+ "`winner_before_money` bigint(20) NULL DEFAULT NULL COMMENT '获胜前金币',"
			+ "`winner_after_money` bigint(20) NULL DEFAULT NULL COMMENT '获胜后金币',"
			+ "`loser_id` bigint(20) NULL DEFAULT NULL COMMENT '失败者id',"
			+ "`loser_nickname` varchar(32) NOT NULL COMMENT '失败者昵称',"
			+ "`loser_before_money` bigint(20) NULL DEFAULT NULL COMMENT '失败前金币',"
			+ "`loser_after_money` bigint(20) NULL DEFAULT NULL COMMENT '失败后金币',"
			+ "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
	void createTable();

}
