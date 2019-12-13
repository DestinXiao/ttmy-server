package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.OseeUnrealLotteryLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 实物兑换
 */
@Mapper
public interface OseeUnrealLotteryLogMapper {

    String TABLE_NAME = "tbl_osee_unreal_lottery_log";

    /**
     * 插入记录
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`order_num`, `user_id`, `nickname`, `reward_name`, `type`, `count`, "
            + "`item_id`, `cost`) VALUES (#{log.orderNum}, #{log.userId}, #{log.nickname}, #{log.rewardName}, "
            + "#{log.type}, #{log.count}, #{log.itemId}, #{log.cost})")
    void save(@Param("log") OseeUnrealLotteryLogEntity log);

    /**
     * 根据条件查询实物兑换记录
     */
    @Select("SELECT * FROM " + TABLE_NAME + " record ${where} ORDER BY `id` DESC ${page}")
    List<OseeUnrealLotteryLogEntity> getLogList(@Param("where") String where, @Param("page") String page);

    /**
     * 根据条件查询实物兑换记录数量
     */
    @Select("SELECT COUNT(*) FROM " + TABLE_NAME + " record ${where}")
    int getLogCount(@Param("where") String where);

    /**
     * 创建日志表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',"
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
            + "`order_num` varchar(32) NULL DEFAULT NULL COMMENT '订单号',"
            + "`user_id` bigint(20) NOT NULL COMMENT '兑换人id',"
            + "`nickname` varchar(32) NULL DEFAULT NULL COMMENT '昵称',"
            + "`reward_name` varchar(32) NULL DEFAULT NULL COMMENT '商品名',"
            + "`type` int(11) NOT NULL COMMENT '类型',"
            + "`count` int(11) NOT NULL COMMENT '兑换数量',"
            + "`item_id` int(11) NOT NULL COMMENT '消耗类型',"
            + "`cost` int(11) NOT NULL COMMENT '消耗数量',"
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

}
