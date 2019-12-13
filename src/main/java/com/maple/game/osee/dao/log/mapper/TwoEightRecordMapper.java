package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.OseeFighttenRecordLogEntity;
import com.maple.game.osee.dao.log.entity.TwoEightRecordLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface TwoEightRecordMapper {
    String TABLE_NAME = "tbl_osee_two_eight_record_log";

    /**
     * 保存数据
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`money`, `playerId`, `nickname`, `playBeforeMoney`, `playAfterMoney`, `input`,  `cardType`) "
            + "VALUES (#{entity.money}, #{entity.playerId}, #{entity.nickname}, #{entity.playBeforeMoney}, "
            + "#{entity.playAfterMoney}, #{entity.input},  #{entity.cardType})")
    void save(@Param("entity") TwoEightRecordLogEntity entity);

    /**
     * 根据条件查询记录
     */
    @Select("SELECT * FROM " + TABLE_NAME + " log ${where} ORDER BY log.id DESC ${page}")
    List<OseeFighttenRecordLogEntity> getLogList(@Param("where") String where, @Param("page") String page);


    /**
     * 从log表中 累加计算条件范围内 记录的 输赢金币总额 totalMoney 下注金币总额 totalCost 的赢取金币总额 AllTotalWin
     */
    @Select("SELECT COALESCE(SUM(money),0) totalMoney FROM " + TABLE_NAME + " log ${where}")
    Map<String, Object> getStatstic(@Param("where") String where);

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
            + "`input` bigint(20) NOT NULL COMMENT '下注金额',"
            + "`cardType` varchar(128) NULL DEFAULT NULL COMMENT '牌型',"
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();
}
