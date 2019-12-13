package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.OseeCutMoneyLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 抽水记录接口
 */
@Mapper
public interface OseeCutMoneyLogMapper {

    String TABLE_NAME = "tbl_osee_cut_money_log";

    /**
     * 插入记录
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`user_id`,  `game`, `cut_money`, `type`) VALUES "
            + "(#{log.userId}, #{log.game} , #{log.cutMoney}, #{log.type})")
    void save(@Param("log") OseeCutMoneyLogEntity log);

    /**
     * 根据条件查询记录
     */
    @Select("SELECT * FROM " + TABLE_NAME + " log ${where} ORDER BY log.id DESC ${page}")
    List<OseeCutMoneyLogEntity> getLogList(@Param("where") String where, @Param("page") String page);

    /**
     * 根据条件查询记录数量
     */
    @Select("SELECT COUNT(*) totalNum, COALESCE(SUM(`cut_money`), 0) totalCut FROM " + TABLE_NAME + " log ${where}")
    Map<String, Object> getLogCount(@Param("where") String where);

    /**
     * 获取指定游戏的所有抽水
     */
    @Select("SELECT COALESCE(SUM(`cut_money`), 0) totalCut FROM " + TABLE_NAME + " WHERE `game` = #{game}")
    long getTotalCutMoney(@Param("game") int game);

    /**
     * 获取今日指定游戏的所有抽水
     */
    @Select("SELECT COALESCE(SUM(`cut_money`), 0) totalCut FROM " + TABLE_NAME + " WHERE DATE(`create_time`) = CURDATE()" +
            " AND `game` = #{game}")
    long getTodayCutMoney(@Param("game") int game);

    /**
     * 创建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',"
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
            + "`user_id` bigint(20) NULL COMMENT '玩家id',"
            + "`game` int(11) NOT NULL COMMENT '游戏',"
            + "`type` int(11) NOT NULL COMMENT '游戏',"
            + "`cut_money` bigint(20) NULL COMMENT '变动金币',"
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

}
