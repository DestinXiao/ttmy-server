package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.OseeRealLotteryLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 实物兑换
 */
@Mapper
public interface OseeRealLotteryLogMapper {

    String TABLE_NAME = "tbl_osee_real_lottery_log";

    /**
     * 插入记录
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`order_num`, `user_id`, `nickname`, `reward_name`, `count`, "
            + "`cost`, `creator`, `consignee`, `phone_num`, `address`, `order_state`, `stock_id`)"
            + " VALUES (#{log.orderNum}, "
            + "#{log.userId}, #{log.nickname}, #{log.rewardName}, #{log.count}, #{log.cost}, #{log.creator}, "
            + "#{log.consignee}, #{log.phoneNum}, #{log.address}, #{log.orderState}, #{log.stockId})")
    void save(@Param("log") OseeRealLotteryLogEntity log);

    /**
     * 根据id查询数据
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE `id` = #{id}")
    OseeRealLotteryLogEntity getById(@Param("id") long id);

    /**
     * 根据条件查询实物兑换记录
     */
    @Select("SELECT * FROM " + TABLE_NAME + " record ${where} ORDER BY `id` DESC ${page}")
    List<OseeRealLotteryLogEntity> getLogList(@Param("where") String where, @Param("page") String page);

    /**
     * 根据条件查询实物兑换记录数量
     */
    @Select("SELECT COUNT(*) FROM " + TABLE_NAME + " record ${where}")
    int getLogCount(@Param("where") String where);

    /**
     * 根据字段查询统计值
     */
    @Select("SELECT `${group}` `key`, COUNT(*) count FROM " + TABLE_NAME + " GROUP BY `${group}` ORDER BY `${group}`")
    List<Map<String, Object>> getGroupCount(@Param("group") String group);

    /**
     * 更新数据
     */
    @Update("UPDATE " + TABLE_NAME + " SET `order_state` = #{entity.orderState} WHERE `id` = #{entity.id}")
    void update(@Param("entity") OseeRealLotteryLogEntity entity);

    /**
     * 创建日志表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',"
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
            + "`order_num` varchar(32) NULL DEFAULT NULL COMMENT '订单号',"
            + "`user_id` bigint(20) NOT NULL COMMENT '兑换人id',"
            + "`nickname` varchar(32) NULL DEFAULT NULL COMMENT '昵称',"
            + "`reward_name` varchar(32) NULL DEFAULT NULL COMMENT '商品名',"
            + "`count` int(11) NOT NULL COMMENT '兑换数量',"
            + "`cost` int(11) NOT NULL COMMENT '消耗数量',"
            + "`creator` varchar(32) NULL DEFAULT NULL COMMENT '创建人',"
            + "`consignee` varchar(32) NULL DEFAULT NULL COMMENT '收货人',"
            + "`phone_num` varchar(32) NULL DEFAULT NULL COMMENT '手机号',"
            + "`address` varchar(32) NULL DEFAULT NULL COMMENT '收货地址',"
            + "`order_state` int(11) NOT NULL COMMENT '状态',"
            + "`stock_id` bigint(20) NULL DEFAULT NULL COMMENT '对应兑换的库存物品ID',"
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

}
