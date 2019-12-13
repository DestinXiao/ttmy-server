package com.maple.game.osee.dao.log.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.maple.database.data.mapper.UserMapper;
import com.maple.game.osee.dao.log.entity.OseeExpendLogEntity;

/**
 * 支出记录接口
 */
@Mapper
public interface OseeExpendLogMapper {

	String TABLE_NAME = "tbl_osee_expend_log";

	/**
	 * 插入记录
	 */
	@Insert("INSERT INTO " + TABLE_NAME + " (`user_id`, `nickname`, `pay_type`, `diamond`, `money`, `lottery`) VALUES "
			+ "(#{log.userId}, #{log.nickname}, #{log.payType}, #{log.diamond}, #{log.money}, #{log.lottery})")
	void save(@Param("log") OseeExpendLogEntity log);

	/**
	 * 根据条件查询记录
	 */
	@Select("SELECT * FROM " + TABLE_NAME + " log LEFT JOIN app_data." + UserMapper.TABLE_NAME
			+ " user ON user.id = log.user_id ${where} ORDER BY log.id DESC ${page}")
	List<OseeExpendLogEntity> getLogList(@Param("where") String where, @Param("page") String page);

	/**
	 * 获取所有抽水金币
	 */
	@Select("SELECT COALESCE(SUM(`money`), 0) totalCut FROM " + TABLE_NAME)
	long getTotalExpendMoney();

	/**
	 * 获取当天抽水金币
	 */
	@Select("SELECT COALESCE(SUM(`money`), 0) totalCut FROM " + TABLE_NAME + " WHERE DATE(`create_time`) = CURDATE()")
	long getTodayExpendMoney();

	/**
	 * 根据条件查询记录数量
	 */
	@Select("SELECT COUNT(*) totalNum, COALESCE(SUM(`diamond`), 0) diamond, COALESCE(SUM(`money`), 0) money, "
			+ "COALESCE(SUM(`lottery`), 0) lottery FROM " + TABLE_NAME + " log LEFT JOIN app_data."
			+ UserMapper.TABLE_NAME + " `user` ON `user`.id = `log`.user_id ${where}")
	Map<String, Object> getLogCount(@Param("where") String where);

	/**
	 * 创建表
	 */
	@Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',"
			+ "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
			+ "`user_id` bigint(20) NULL COMMENT '玩家id',`nickname` varchar(32) NULL DEFAULT NULL COMMENT '昵称',"
			+ "`pay_type` int(11) NOT NULL COMMENT '支出类型',`diamond` bigint(20) NULL COMMENT '支出钻石',"
			+ "`money` bigint(20) NULL COMMENT '支出金币',`lottery` bigint(20) NULL COMMENT '支出奖券',"
			+ "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
	void createTable();

}
