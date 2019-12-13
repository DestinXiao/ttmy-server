package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.AgentCommissionEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 代理佣金记录数据接口
 */
@Mapper
public interface AgentCommissionMapper {

    String TABLE_NAME = "tbl_ttmy_agent_commission_log";

    /**
     * 建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            "  `player_id` bigint(20) NOT NULL COMMENT '充值玩家ID'," +
            "  `player_name` varchar(100) NOT NULL COMMENT '充值玩家昵称'," +
            "  `agent_player_id` bigint(20) NULL COMMENT '玩家上级代理ID(不一定是直属)'," +
            "  `agent_player_name` varchar(100) NULL COMMENT '玩家上级代理昵称(不一定是直属)'," +
            "  `commission_rate` decimal(8,4) NOT NULL DEFAULT 0.05 NULL COMMENT '计算的佣金比例'," +
            "  `commission` decimal(8,4) NULL COMMENT '计算出的佣金数量(取整)'," +
            "  `money` bigint(20) NULL COMMENT '充值金额'," +
            "  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据创建时间'," +
            "  PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 获取指定玩家最近一周所贡献出的佣金
     */
    @Select("select COALESCE(sum(commission), 0) from " + TABLE_NAME + " " +
            "where DATE_SUB(CURDATE(), INTERVAL 7 DAY) <= date(create_time) and player_id = #{playerId} and agent_player_id = #{agentId}")
    double getWeeklyCommissionByPlayerId(@Param("playerId") Long playerId, @Param("agentId") Long agentPlayerId);

    /**
     * 获取指定代理玩家最近一周所收获的佣金
     */
    @Select("select COALESCE(sum(commission), 0) from " + TABLE_NAME + " " +
            "where DATE_SUB(CURDATE(), INTERVAL 7 DAY) <= date(create_time) and agent_player_id = #{agentId}")
    double getWeeklyCommissionByAgentId(@Param("agentId") Long agentId);

    /**
     * 获取指定代理玩家当天所收获的佣金
     */
    @Select("select COALESCE(sum(commission), 0) from " + TABLE_NAME + " " +
            "where date(create_time) = curdate() and agent_player_id = #{agentId}")
    double getDailyCommissionByAgentId(@Param("agentId") Long agentId);

    /**
     * 获取指定代理玩家当月所收获的佣金
     */
    @Select("select COALESCE(sum(commission), 0) from " + TABLE_NAME + " " +
            "where date_format(create_time, '%Y-%m') = date_format(curdate(), '%Y-%m') and agent_player_id = #{agentId}")
    double getMonthCommissionByAgentId(@Param("agentId") Long agentId);

    /**
     * 获取指定代理玩家指定月度所收获的佣金
     */
    @Select("select COALESCE(sum(commission), 0) from " + TABLE_NAME + " " +
            "where date_format(create_time, '%Y-%m') = #{month} and agent_player_id = #{agentId}")
    double getTargetMonthCommissionByAgentId(@Param("agentId") Long agentId, @Param("month") String month);

    /**
     * 获取指定代理玩家所有收获的佣金
     */
    @Select("select COALESCE(sum(commission), 0) from " + TABLE_NAME + " where agent_player_id = #{agentId}")
    double getTotalCommissionByAgentId(@Param("agentId") Long agentId);

    /**
     * 根据年份获取收获佣金
     */
    @Select("select MONTH(create_time) month, COALESCE(sum(commission), 0) money from " + TABLE_NAME + " where agent_player_id = #{agentPlayerId}" +
            " and YEAR(create_time) = #{year} GROUP BY MONTH(create_time)")
    List<Map<Object, Object>> getPerMonthCommissionByAgentId(@Param("agentPlayerId") long agentPlayerId, @Param("year") int year);

    /**
     * 获取指定代理玩家每日赚取的贡献佣金
     */
    @Select("select DATE(create_time) date, COALESCE(sum(commission), 0) commission from" +
            " " + TABLE_NAME + " " +
            "where agent_player_id = #{agentId} " +
            "group by create_time " +
            "order by create_time " +
            "limit #{offset},#{size}")
    List<Map<String, Object>> getDailyCommissionListByAgentId(@Param("agentId") Long agentId, @Param("offset") int offset, @Param("size") int size);

    /**
     * 获取指定玩家共有多少天贡献了佣金
     */
    @Select("select count(*) from (select create_time from " + TABLE_NAME + " where player_id = #{playerId} group by create_time) tac")
    long getDaysByPlayerId(@Param("playerId") Long playerId);

    /**
     * 获取指定代理共有多少天赚取到了佣金
     */
    @Select("select count(*) from (select create_time from " + TABLE_NAME + " where agent_player_id = #{agentId} group by create_time) tac")
    long getDaysByAgentId(@Param("agentId") Long agentId);

    /**
     * 获取指定代理下级使用的销售金
     */
    @Select("select COALESCE(sum(money), 0) from " + TABLE_NAME + " where agent_player_id = #{agentId}" +
            "${condition}")
    long getMoneyByAgentId(@Param("agentId") Long agentPlayerId, @Param("condition") String queryCondition);

    /**
     * 获取指定代理收获的佣金
     */
    @Select("select COALESCE(sum(commission), 0) from " + TABLE_NAME + " where agent_player_id = #{agentId}" +
            "${condition}")
    long getCommissionByAgentId(@Param("agentId") Long agentPlayerId, @Param("condition") String queryCondition);

    /**
     * 根据指定条件查找
     */
    @Select("select * from " + TABLE_NAME + " ${condition} ORDER BY create_time desc ${page}")
    List<AgentCommissionEntity> getCommissionList(@Param("condition") String condition, @Param("page") String page);

    /**
     * 获取指定代理总共收到贡献数据条数
     */
    @Select("select count(*) from " + TABLE_NAME + " ${condition}")
    int getCommissionCount(@Param("condition") String condition);

    /**
     * 插入数据
     */
    @Insert("insert into " + TABLE_NAME + " " +
            "(player_id, player_name, agent_player_id, agent_player_name," +
            " commission_rate, commission, money)" +
            " values " +
            "(#{entity.playerId}, #{entity.playerName}, #{entity.agentPlayerId}, #{entity.agentPlayerName}," +
            " #{entity.commissionRate}, #{entity.commission}, #{entity.money})")
    int save(@Param("entity") AgentCommissionEntity commissionEntity);

    /**
     * 删除指定代理玩家所有的佣金信息
     */
    @Delete("delete from " + TABLE_NAME + " where agent_player_id = #{agentId}")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
