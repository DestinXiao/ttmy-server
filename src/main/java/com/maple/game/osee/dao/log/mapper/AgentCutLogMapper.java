package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.AgentCutLogEntity;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 代理活跃抽水数据接口
 */
@Mapper
public interface AgentCutLogMapper {

    String TABLE_NAME = "tbl_agent_cut_log";

    /**
     * 建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            "  `player_id` bigint(20) NOT NULL COMMENT '玩家ID'," +
            "  `agent_id` bigint(20) NOT NULL COMMENT '代理ID'," +
            "  `game` int(8) NOT NULL COMMENT '游戏'," +
            "  `cut_money` bigint(20) NULL COMMENT '金币'," +
            "  `cut_dragon_crystal` bigint(20) NULL COMMENT '龙晶'," +
            "  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据创建时间'," +
            "  PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 插入数据
     */
    @Insert("insert into " + TABLE_NAME + " (player_id, agent_id, game, cut_money, cut_dragon_crystal) " +
            "values (#{log.playerId}, #{log.agentId}, #{log.game}, #{log.cutMoney}, #{log.cutDragonCrystal})")
    int save(@Param("log") AgentCutLogEntity log);

    /**
     * 获取指定代理玩家当天所收获的活跃金
     */
    @Select("select COALESCE(sum(cut_money), 0) money, COALESCE(sum(cut_dragon_crystal), 0) dragon from " + TABLE_NAME + " " +
            "where date(create_time) = curdate() and agent_id = #{agentId}")
    Map<String, BigDecimal> getDailyActiveByAgentId(@Param("agentId") Long agentId);

    /**
     * 获取指定代理玩家所收获的活跃金
     */
    @Select("select COALESCE(sum(cut_money), 0) money, COALESCE(sum(cut_dragon_crystal), 0) dragon from " + TABLE_NAME + " " +
            "where agent_id = #{agentId}")
    Map<String, BigDecimal> getTotalActiveByAgentId(@Param("agentId") Long agentId);

    /**
     * 删除指定代理玩家所有的佣金信息
     */
    @Delete("delete from " + TABLE_NAME + " where agent_id = #{agentId}")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
