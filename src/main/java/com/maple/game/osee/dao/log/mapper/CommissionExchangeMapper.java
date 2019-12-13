package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.CommissionExchangeEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 代理佣金兑换数据库操作接口
 *
 * @author Junlong
 */
@Mapper
public interface CommissionExchangeMapper {

    String TABLE_NAME = "tbl_ttmy_commission_exchange_log";

    /**
     * 建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            "  `agent_id` bigint(20) NOT NULL COMMENT '代理玩家ID'," +
            "  `agent_name` varchar(100) NOT NULL COMMENT '代理玩家昵称'," +
            "  `bronze_torpedo_num` bigint(20) NULL COMMENT '青铜鱼雷数量'," +
            "  `silver_torpedo_num` bigint(20) NULL COMMENT '白银鱼雷数量'," +
            "  `gold_torpedo_num` bigint(20) NULL COMMENT '黄金鱼雷数量'," +
            "  `gold_num` bigint(20) NULL COMMENT '金币数量'," +
            "  `cost_commission` bigint(20) NULL COMMENT '花费的佣金'," +
            "  `rest_commission` bigint(20) NULL COMMENT '该次兑换后剩余的佣金'," +
            "  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据创建时间'," +
            "  PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 保存数据
     */
    @Insert("insert into " + TABLE_NAME + " (agent_id, agent_name," +
            " bronze_torpedo_num, silver_torpedo_num, gold_torpedo_num, gold_num," +
            " cost_commission, rest_commission)" +
            " values " +
            "(#{entity.agentId}, #{entity.agentName}," +
            " #{entity.bronzeTorpedoNum}, #{entity.silverTorpedoNum}, #{entity.goldTorpedoNum}, #{entity.goldNum}," +
            " #{entity.costCommission}, #{entity.restCommission})")
    void save(@Param("entity") CommissionExchangeEntity exchangeEntity);

    /**
     * 根据条件查找数据
     */
    @Select("select * from " + TABLE_NAME + " ${condition} order by id desc ${page}")
    List<CommissionExchangeEntity> getList(@Param("condition") String condition, @Param("page") String page);

    /**
     * 根据条件获取数据总条数
     */
    @Select("select count(*) from " + TABLE_NAME + " ${condition}")
    int getCount(@Param("condition") String condition);

    /**
     * 删除指定代理兑换明细数据
     */
    @Delete("delete from " + TABLE_NAME + " where agent_id = #{agentId}")
    int deleteByAgentId(@Param("agentId") Long agentId);
}
