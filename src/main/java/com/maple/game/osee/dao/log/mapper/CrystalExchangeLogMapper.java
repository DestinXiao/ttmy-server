package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.CrystalExchangeLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 龙晶兑换记录数据操作接口
 *
 * @author Junlong
 */
@Mapper
public interface CrystalExchangeLogMapper {
    String TABLE_NAME = "tbl_dragon_crystal_exchange_log";

    /**
     * 建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +

            "  `player_id` bigint(20) NOT NULL COMMENT '玩家ID'," +
            "  `exchange_type` int(11) NOT NULL COMMENT '兑换类型 0-兑换龙晶 1-兑换鱼雷'," +

            "  `bronze_torpedo_before` bigint(20) NULL COMMENT '青铜鱼雷数量前'," +
            "  `silver_torpedo_before` bigint(20) NULL COMMENT '白银鱼雷数量前'," +
            "  `gold_torpedo_before` bigint(20) NULL COMMENT '黄金鱼雷数量前'," +

            "  `bronze_torpedo_change` bigint(20) NULL COMMENT '青铜鱼雷数量变'," +
            "  `silver_torpedo_change` bigint(20) NULL COMMENT '白银鱼雷数量变'," +
            "  `gold_torpedo_change` bigint(20) NULL COMMENT '黄金鱼雷数量变'," +

            "  `dragon_crystal_before` bigint(20) NULL COMMENT '龙晶数量前'," +
            "  `dragon_crystal_change` bigint(20) NULL COMMENT '龙晶数量变'," +

            "  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据创建时间'," +
            "  PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 保存数据
     */
    @Insert("insert into " + TABLE_NAME + " (" +
            "player_id, exchange_type, " +
            "bronze_torpedo_before, silver_torpedo_before, gold_torpedo_before," +
            "bronze_torpedo_change, silver_torpedo_change, gold_torpedo_change," +
            "dragon_crystal_before, dragon_crystal_change" +
            ") values (" +
            "#{entity.playerId}, #{entity.exchangeType}," +
            "#{entity.bronzeTorpedoBefore}, #{entity.silverTorpedoBefore}, #{entity.goldTorpedoBefore}," +
            "#{entity.bronzeTorpedoChange}, #{entity.silverTorpedoChange}, #{entity.goldTorpedoChange}," +
            "#{entity.dragonCrystalBefore}, #{entity.dragonCrystalChange}" +
            ")")
    void save(@Param("entity") CrystalExchangeLogEntity entity);

    /**
     * 获取数据列表
     */
    @Select("select * from " + TABLE_NAME + " ${query}")
    List<CrystalExchangeLogEntity> getList(@Param("query") String query);

    /**
     * 获取指定查询条件数据的总条数
     */
    @Select("select count(*) from " + TABLE_NAME + " ${query}")
    long getCount(@Param("query") String query);
}
