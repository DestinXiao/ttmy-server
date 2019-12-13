package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.OseeRechargeLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 充值记录接口
 */
@Mapper
public interface OseeRechargeLogMapper {

    String TABLE_NAME = "tbl_osee_recharge_log";

    /**
     * 保存数据
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`order_num`, `user_id`, `nickname`, `pay_money`, `shop_name`, "
            + "`shop_type`, `count`, `creator`, `recharge_type`, `order_state`) VALUES (#{entity.orderNum}, "
            + "#{entity.userId}, #{entity.nickname}, #{entity.payMoney}, #{entity.shopName}, #{entity.shopType}, "
            + "#{entity.count}, #{entity.creator}, #{entity.rechargeType}, #{entity.orderState})")
    void save(@Param("entity") OseeRechargeLogEntity entity);

    /**
     * 根据订单号查找订单
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE `order_num` = #{orderNum}")
    OseeRechargeLogEntity get(@Param("orderNum") String orderNum);

    /**
     * 修改订单状态
     */
    @Update("UPDATE " + TABLE_NAME + " log SET `order_state` = #{state} WHERE `id` = #{id}")
    void updateOrderState(@Param("state") int state, @Param("id") long id);

    /**
     * 根据条件查询充值记录
     */
    @Select("SELECT * FROM " + TABLE_NAME + " log ${where} ORDER BY `id` DESC ${page}")
    List<OseeRechargeLogEntity> getLogList(@Param("where") String where, @Param("page") String page);

    /**
     * 根据条件查询充值统计值
     */
    @Select("SELECT COUNT(*) totalNum FROM " + TABLE_NAME + " log ${where}")
    long getLogCount(@Param("where") String where);

    /**
     * 获取充值总额
     */
    @Select("SELECT COALESCE(SUM(`${sum}`), 0) totalMoney FROM " + TABLE_NAME + " log ${where} AND log.order_state = 1")
    long getTotalRecharge(@Param("where") String where, @Param("sum") String sum);

    /**
     * 获取玩家今日充值总额
     */
    @Select("select COALESCE(sum(pay_money), 0) total from " + TABLE_NAME + " where recharge_type != 3 and order_state = 1" +
            " and DATE(`create_time`) = CURRENT_DATE()" +
            " and user_id = #{userId}")
    long getTodayRecharge(@Param("userId") long userId);

    /**
     * 创建记录表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',"
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
            + "`order_num` varchar(32) NULL DEFAULT NULL COMMENT '订单号',`user_id` bigint(20) NULL COMMENT '玩家id',"
            + "`nickname` varchar(32) NULL DEFAULT NULL COMMENT '昵称',`pay_money` int(11) NOT NULL COMMENT '支付金额',"
            + "`shop_name` varchar(32) NULL DEFAULT NULL COMMENT '商品名',`shop_type` int(11) NOT NULL COMMENT '类型',"
            + "`count` int(11) NOT NULL COMMENT '数量',`creator` varchar(32) NULL COMMENT '创建数量',"
            + "`recharge_type` int(11) NOT NULL COMMENT '充值方式',`order_state` int(11) NOT NULL COMMENT '订单状态',"
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

}
