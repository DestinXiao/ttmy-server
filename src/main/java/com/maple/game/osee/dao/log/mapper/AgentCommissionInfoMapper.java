package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.AgentCommissionEntity;
import com.maple.game.osee.dao.log.entity.AgentCommissionInfoEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 代理佣金记录数据接口
 */
@Mapper
public interface AgentCommissionInfoMapper {

    String TABLE_NAME = "tbl_ttmy_agent_commission_info_log";

    /**
     * 建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            "  `player_id` bigint(20) NOT NULL COMMENT '充值玩家ID'," +
            "  `player_name` varchar(100) NOT NULL COMMENT '充值玩家昵称'," +
            "  `shop_name` varchar(100) NOT NULL COMMENT '商品名'," +
            "  `channel_id` bigint(20) NULL COMMENT '渠道ID'," +
            "  `promoter_id` bigint(20) NULL COMMENT '推广员ID'," +
            "  `commission` decimal(8,4) NULL COMMENT '计算出的渠道商佣金数量(取整)'," +
            "  `sec_commission` decimal(8,4) NULL COMMENT '计算出的推广员佣金数量(取整)'," +
            "  `money` decimal(8,4) NULL COMMENT '充值金额'," +
            "  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据创建时间'," +
            "  PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 保存数据
     */
    @Insert("insert into " + TABLE_NAME + " (player_id, player_name, shop_name, channel_id, promoter_id, commission, sec_commission, money) " +
            "values (#{log.playerId}, #{log.playerName}, #{log.shopName}, #{log.channelId}, #{log.promoterId}, #{log.commission}, #{log.secCommission}, #{log.money})")
    void save(@Param("log") AgentCommissionInfoEntity log);

    /**
     * 根据条件查找数据
     */
    @Select("select * from " + TABLE_NAME + " log ${condition} order by id desc ${page}")
    List<AgentCommissionInfoEntity> getList(@Param("condition") String condition, @Param("page") String page);

    /**
     * 根据条件获取数据总条数
     */
    @Select("select count(*) from " + TABLE_NAME + " log ${condition}")
    int getCount(@Param("condition") String condition);

    /**
     * 根据条件获取统计数据
     */
    @Select("select sum(money) money, sum(commission) commission, sum(sec_commission) secCommission from " + TABLE_NAME + " log ${condition}")
    Map<String, Object> getStatistics(@Param("condition") String condition);

    /**
     * 获取所有代理玩家当天所收获的佣金
     */
    @Select("select COALESCE(sum(commission), 0) from " + TABLE_NAME + " " +
            "where date(create_time) = curdate()")
    double getDailyTotalCommission();
}
