package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.AgentCutReceiveLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 领取全民推广奖励记录接口
 */
@Mapper
public interface AgentCutReceiveLogMapper {

    String TABLE_NAME = "tbl_agent_cut_receive_log";

    /**
     * 建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            "  `agent_id` bigint(20) NOT NULL COMMENT '代理玩家ID'," +
            "  `agent_name` varchar(100) NOT NULL COMMENT '代理玩家昵称'," +
            "  `money` bigint(20) NULL COMMENT '金币数量'," +
            "  `dragon_crystal` bigint(20) NULL COMMENT '花费的佣金'," +
            "  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据创建时间'," +
            "  PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 保存数据
     */
    @Insert("insert into " + TABLE_NAME + " (agent_id, agent_name, money, dragon_crystal) values " +
            "(#{log.agentId}, #{log.agentName}, #{log.money}, #{log.dragonCrystal})")
    void save(@Param("log") AgentCutReceiveLogEntity log);

    /**
     * 根据条件查找数据
     */
    @Select("select * from " + TABLE_NAME + " ${condition} order by id desc ${page}")
    List<AgentCutReceiveLogEntity> getList(@Param("condition") String condition, @Param("page") String page);

    /**
     * 根据条件获取数据总条数
     */
    @Select("select count(*) from " + TABLE_NAME + " ${condition}")
    int getCount(@Param("condition") String condition);

    /**
     * 删除指定代理玩家所有的佣金信息
     */
    @Delete("delete from " + TABLE_NAME + " where agent_id = #{agentId}")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
