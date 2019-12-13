package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.OseeFruitRecordLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 1688水果拉霸记录日志接口
 */
@Mapper
public interface OseeFruitRecordLogMapper {

    String TABLE_NAME = "tbl_osee_fruit_record_log";

    /**
     * 保存数据
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`money`, `playerId`, `nickname`, `playBeforeMoney`, `playAfterMoney`, `cost`, `lineNum`, `totalWin`, `info`) "
            + "VALUES (#{entity.money}, #{entity.playerId}, #{entity.nickname}, #{entity.playBeforeMoney}, "
            + "#{entity.playAfterMoney}, #{entity.cost}, #{entity.lineNum}, #{entity.totalWin}, #{entity.info})")
    void save(@Param("entity") OseeFruitRecordLogEntity entity);

    /**
     * 从log表中 累加计算条件范围内 记录的 输赢金币总额 totalMoney 下注金币总额 totalCost 的赢取金币总额 AllTotalWin
     */
    @Select("SELECT COALESCE(SUM(money),0) totalMoney, COALESCE(SUM(cost),0) totalCost,COALESCE(SUM(totalWin),0) AllTotalWin  FROM " + TABLE_NAME + " log ${where}")
    Map<String, Object> getStatstic(@Param("where") String where);


    /**
     * 根据条件查询记录
     */
    @Select("SELECT * FROM " + TABLE_NAME + " log ${where} ORDER BY log.id DESC ${page}")
    List<OseeFruitRecordLogEntity> getLogList(@Param("where") String where, @Param("page") String page);

    /**
     * 根据条件查询记录数量
     */
    @Select("SELECT COUNT(*) totalNum FROM " + TABLE_NAME + " log ${where}")
    int getLogCount(@Param("where") String where);

    /**
     * 创建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',"
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
            + "`money` bigint(20) NULL COMMENT '账户金币变动数额',"
            + "`playerId` bigint(20) NULL DEFAULT NULL COMMENT '玩家id',"
            + "`nickname` varchar(32) NOT NULL COMMENT '玩家昵称',"
            + "`playBeforeMoney` bigint(20) NULL DEFAULT NULL COMMENT '游戏前剩余金币',"
            + "`playAfterMoney` bigint(20) NULL DEFAULT NULL COMMENT '游戏后剩余金币',"
            + "`cost` bigint(20) NULL DEFAULT NULL COMMENT '下注消耗',"
            + "`lineNum` int(20) NULL DEFAULT NULL COMMENT '下注条数',"
            + "`totalWin` bigint(20) NULL DEFAULT NULL COMMENT '中奖赢取',"
            + "`info` varchar(255) NULL DEFAULT NULL COMMENT '中奖详情',"
            + "`type` int(255) NULL DEFAULT NULL COMMENT '类型',"
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

    /**
     * 从抽水表中 累加计算 水果拉霸 game = 5 抽水总额 ljy
     */
    @Select("SELECT COALESCE(SUM(cut_money),0) totalCut FROM tbl_osee_cut_money_log WHERE game=${game} and type=${type}")
    long getTotalCutMoney(@Param("game") Integer game, @Param("type") Integer type);
}
