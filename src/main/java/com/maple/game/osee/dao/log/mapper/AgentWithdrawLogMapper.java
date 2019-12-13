package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.data.mapper.AgentMapper;
import com.maple.game.osee.dao.log.entity.AgentWithdrawLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 代理提现记录
 */
public interface AgentWithdrawLogMapper {
    String TABLE_NAME = "tbl_ttmy_agent_withdraw_log";

    /**
     * 建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            "  `agent_id` bigint(20) NOT NULL COMMENT '代理ID'," +
            "  `bank` varchar(32) NOT NULL COMMENT '银行'," +
            "  `real_name` varchar(32) NOT NULL COMMENT '户名'," +
            "  `bank_num` varchar(32) NOT NULL COMMENT '卡号'," +
            "  `open_bank` varchar(64) NOT NULL COMMENT '开户行'," +
            "  `money` bigint(20) NULL COMMENT '提现金额'," +
            "  `state` int(2) NULL COMMENT '状态'," +
            "  `creator` varchar(32) NULL COMMENT '操作管理员'," +
            "  PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 保存数据
     */
    @Insert("insert into " + TABLE_NAME + " (agent_id, bank, real_name, bank_num, open_bank, money, state, creator) " +
            "values (#{log.agentId}, #{log.bank}, #{log.realName}, #{log.bankNum}, #{log.openBank}, #{log.money}, #{log.state}, #{log.creator})")
    void save(@Param("log") AgentWithdrawLogEntity log);

    /**
     * 根据id查找记录
     */
    @Select("select * from " + TABLE_NAME + " where id = #{id}")
    AgentWithdrawLogEntity get(@Param("id") Long id);

    /**
     * 更新记录状态
     */
    @Update("update " + TABLE_NAME + " set creator = #{log.creator}, state = #{log.state} where id = #{log.id}")
    void updateState(@Param("log") AgentWithdrawLogEntity log);

    /**
     * 根据条件查找数据
     */
    @Select("select * from " + TABLE_NAME + " log ${condition} order by state = 0 DESC, id DESC ${page}")
    List<AgentWithdrawLogEntity> getList(@Param("condition") String condition, @Param("page") String page);

    /**
     * 根据条件获取数据总条数
     */
    @Select("select count(*) from " + TABLE_NAME + " log ${condition}")
    int getCount(@Param("condition") String condition);

    /**
     * 根据条件查询各状态数据统计值
     */
    @Select("select state, sum(money) money from " + TABLE_NAME + " log ${condition} group by state;")
    List<Map<String, Object>> getStatistics(@Param("condition") String condition);

    /**
     * 根据条件查找数据
     */
    @Select("select * from " + TABLE_NAME + " log, app_data." + AgentMapper.TABLE_NAME + " agent where log.agent_id = agent.player_id " +
            "${condition} order by log.state = 0 DESC, log.id DESC ${page}")
    List<Map<String, Object>> getMapList(@Param("condition") String condition, @Param("page") String page);

    /**
     * 根据条件获取数据总条数
     */
    @Select("select count(*) from " + TABLE_NAME + " log, app_data." + AgentMapper.TABLE_NAME + " agent where log.agent_id = agent.player_id ${condition}")
    int getMapCount(@Param("condition") String condition);

    /**
     * 删除指定代理玩家所有的佣金信息
     */
    @Delete("delete from " + TABLE_NAME + " where agent_id = #{agentId}")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
