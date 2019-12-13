package com.maple.game.osee.dao.data.mapper;

import com.maple.database.data.mapper.UserMapper;
import com.maple.game.osee.dao.data.entity.AgentEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 代理数据接口
 *
 * @author Junlong
 */
@Mapper
public interface AgentMapper {

    String TABLE_NAME = "tbl_ttmy_agent";

    /**
     * 建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            "  `player_id` bigint(20) NOT NULL COMMENT '玩家ID'," +
            "  `player_name` varchar(100) NOT NULL COMMENT '玩家昵称'," +
            "  `agent_level` int(11) NOT NULL COMMENT '代理等级'," +
            "  `agent_player_id` bigint(20) NULL COMMENT '上级代理玩家ID'," +
            "  `upper_player_id` bigint(20) NULL COMMENT '上级玩家ID'," +
            "  `first_commission_rate` decimal(8,4) NULL DEFAULT 0.05 COMMENT '一级代理玩家佣金比例'," +
            "  `second_commission_rate` decimal(8,4) NULL DEFAULT 0.05 COMMENT '二级代理玩家佣金比例'," +
            "  `total_commission` decimal(8,4) NULL COMMENT '赚取的总佣金'," +
            "  `total_active_money` bigint(20) NOT NULL DEFAULT 0 COMMENT '赚取的总活跃金币'," +
            "  `total_active_dragon_crystal` bigint(20) NOT NULL DEFAULT 0 COMMENT '赚取的总活跃龙晶'," +
            "  `invite_qrcode_img` text NULL COMMENT '代理邀请二维码图片'," +
            "  `invite_url` varchar(100) NULL COMMENT '代理邀请链接'," +
            "  `state` int(11) NULL COMMENT '代理身份状态'," +
            "  `bank` varchar(32) NULL COMMENT '银行'," +
            "  `real_name` varchar(32) NULL COMMENT '户名'," +
            "  `bank_num` varchar(32) NULL COMMENT '卡号'," +
            "  `open_bank` varchar(64) NULL COMMENT '开户行'," +
            "  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据创建时间'," +
            "  PRIMARY KEY (`id`)," +
            "  UNIQUE INDEX `player_id_index`(`player_id`) USING BTREE COMMENT '唯一用户id'" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 通过玩家id获取玩家代理信息
     */
    @Select("select * from " + TABLE_NAME + " where player_id = #{playerId}")
    AgentEntity getByPlayerId(@Param("playerId") Long playerId);

    /**
     * 获取代理玩家的总数量
     */
    @Select("select count(*) from " + TABLE_NAME + " ${condition}")
    int getAgentCount(@Param("condition") String condition);

    /**
     * 获取代理下级在线人数
     */
    @Select("select coalesce(count(`agent`.id), 0) from " + TABLE_NAME + " `agent`, " + UserMapper.TABLE_NAME + " `user`" +
            " where `agent`.player_id = `user`.id and `agent`.agent_player_id = #{agentPlayerId}" +
            " and `user`.online_state > 0")
    int getOnlineSize(@Param("agentPlayerId") Long agentPlayerId);

    /**
     * 获取今日新增玩家数量
     */
    @Select("select coalesce(count(*), 0) from " + TABLE_NAME +
            " where agent_player_id = #{agentPlayerId} and date(create_time) = curdate()")
    int getDailyPlayerSize(@Param("agentPlayerId") Long agentPlayerId);

    /**
     * 获取渠道商今日新增玩家数量
     */
    @Select("select coalesce(count(player.id), 0) from " + TABLE_NAME + " player, " + TABLE_NAME + " promoter " +
            "where player.agent_player_id = promoter.player_id and promoter.agent_player_id = #{channelId} and " +
            "date(player.create_time) = curdate()")
    int getDailyChannelPlayerSize(@Param("channelId") Long channelId);

    /**
     * 获取所有渠道今日新增玩家数量
     */
    @Select("select coalesce(count(player.id), 0) from " + TABLE_NAME + " player where agent_player_id > 0 and " +
            "date(player.create_time) = curdate()")
    int getDailyTotalChannelPlayerSize();

    /**
     * 获取今日推广玩家数量
     */
    @Select("select coalesce(count(*), 0) from " + TABLE_NAME +
            " where upper_player_id = #{upperPlayerId} and date(create_time) = curdate()")
    int getDailyPlayerSizeByUpper(@Param("upperPlayerId") Long upperPlayerId);

    /**
     * 获取当月新增玩家数量
     */
    @Select("select coalesce(count(*), 0) from " + TABLE_NAME +
            " where agent_player_id = #{agentPlayerId} and DATE_FORMAT(create_time, '%Y%m') = DATE_FORMAT(curdate(), '%Y%m')")
    int getMonthPlayerSize(@Param("agentPlayerId") Long agentPlayerId);

    /**
     * 根据年份获取新增玩家数量
     */
    @Select("select MONTH(create_time) month, COUNT(*) count from " + TABLE_NAME + " where agent_player_id = #{agentPlayerId}" +
            " and YEAR(create_time) = #{year} GROUP BY MONTH(create_time)")
    List<Map<Object, Object>> getPerMonthPlayerSize(@Param("agentPlayerId") Long agentPlayerId, @Param("year") Integer year);

    /**
     * 查询代理玩家列表
     */
    @Select("select * from " + TABLE_NAME + " ${condition} ORDER BY create_time desc ${page}")
    List<AgentEntity> getAgentList(@Param("condition") String condition, @Param("page") String page);

    /**
     * 查询该代理玩家下面的下级
     */
    @Select("select * from " + TABLE_NAME + " where agent_player_id = #{agentPlayerId}")
    List<AgentEntity> getByAgentPlayerId(@Param("agentPlayerId") Long agentPlayerId);

    /**
     * 根据上级玩家查询
     */
    @Select("select * from " + TABLE_NAME + " where upper_player_id = #{upperPlayerId}")
    List<AgentEntity> getByUpperPlayerId(@Param("upperPlayerId") Long upperPlayerId);

    /**
     * 查询该代理玩家下面的下级(带分页)
     *
     * @param agentPlayerId 代理玩家ID
     */
    @Select("select * from " + TABLE_NAME + " where agent_player_id = #{agentPlayerId} limit #{offset},#{size}")
    List<AgentEntity> getByAgentPlayerIdWithPage(@Param("agentPlayerId") Long agentPlayerId, @Param("offset") int offset, @Param("size") int size);

    /**
     * 获取代理直属下级代理人数
     */
    @Select("select coalesce(count(*), 0) from " + TABLE_NAME + " where agent_player_id = #{agentPlayerId}")
    int getAgentNextLevelCount(@Param("agentPlayerId") Long agentPlayerId);

    /**
     * 获取推广人数
     */
    @Select("select coalesce(count(*), 0) from " + TABLE_NAME + " where upper_player_id = #{upperPlayerId}")
    int getAgentNextUpperCount(@Param("upperPlayerId") Long upperPlayerId);

    /**
     * 获取指定渠道下的所有玩家
     */
    @Select("select * from " + TABLE_NAME + " player, " + TABLE_NAME + " promoter, " + TABLE_NAME + " channel, " +
            UserMapper.TABLE_NAME + " userInfo, " + OseePlayerMapper.TABLE_NAME + " playerInfo " +
            "where player.agent_player_id = promoter.player_id " +
            "AND promoter.agent_player_id = channel.player_id " +
            "AND player.player_id = userInfo.id " +
            "AND player.player_id = playerInfo.user_id  " +
            "AND channel.player_id = #{channelId} ${condition} order by player.id DESC ${page}")
    List<Map<String, Object>> getPlayerByChannelId(@Param("channelId") Long channelId, @Param("condition") String condition, @Param("page") String page);

    /**
     * 获取指定渠道下的玩家数量
     */
    @Select("select count(player.id) from " + TABLE_NAME + " player, " + TABLE_NAME + " promoter, " + TABLE_NAME + " channel, " +
            UserMapper.TABLE_NAME + " userInfo, " + OseePlayerMapper.TABLE_NAME + " playerInfo " +
            "where player.agent_player_id = promoter.player_id " +
            "AND promoter.agent_player_id = channel.player_id " +
            "AND player.player_id = userInfo.id " +
            "AND player.player_id = playerInfo.user_id  " +
            "AND channel.player_id = #{channelId} ${condition}")
    int getPlayerByChannelIdSize(@Param("channelId") Long channelId, @Param("condition") String condition);

    /**
     * 插入一条新数据
     */
    @Insert("insert into " + TABLE_NAME + " " +
            "(player_id, player_name, agent_level, agent_player_id, upper_player_id," +
            " first_commission_rate, second_commission_rate, total_commission, total_active_money, total_active_dragon_crystal," +
            " invite_qrcode_img, invite_url, state, bank, real_name, bank_num, open_bank)" +
            " values " +
            "(#{agent.playerId}, #{agent.playerName}, #{agent.agentLevel}, #{agent.agentPlayerId}, #{agent.upperPlayerId}," +
            " #{agent.firstCommissionRate}, #{agent.secondCommissionRate}," +
            " #{agent.totalCommission}, #{agent.totalActiveMoney}, #{agent.totalActiveDragonCrystal}," +
            " #{agent.inviteQrCodeImg}, #{agent.inviteUrl}, #{agent.state}, #{agent.bank}, #{agent.realName}, #{agent.bankNum}, #{agent.openBank})")
    @Options(useGeneratedKeys = true, keyProperty = "agent.id")
    int save(@Param("agent") AgentEntity agentEntity);

    /**
     * 更新数据
     */
    @Update("update " + TABLE_NAME + " set " +
            "player_id = #{agent.playerId}, player_name = #{agent.playerName}, " +
            "agent_level = #{agent.agentLevel}, agent_player_id = #{agent.agentPlayerId}, upper_player_id = #{agent.upperPlayerId}," +
            "first_commission_rate = #{agent.firstCommissionRate}, second_commission_rate = #{agent.secondCommissionRate}, " +
            "total_commission = #{agent.totalCommission}, total_active_money = #{agent.totalActiveMoney}, " +
            "total_active_dragon_crystal = #{agent.totalActiveDragonCrystal}, " +
            "invite_qrcode_img = #{agent.inviteQrCodeImg}, invite_url = #{agent.inviteUrl}, state = #{agent.state}, " +
            "bank = #{agent.bank}, real_name = #{agent.realName}, bank_num = #{agent.bankNum}, open_bank = #{agent.openBank} " +
            "where id = #{agent.id}")
    int update(@Param("agent") AgentEntity agentEntity);

    /**
     * 更新某代理玩家下面所有玩家的代理等级
     */
    @Update("update " + TABLE_NAME + " set " +
            "agent_level=#{level} " +
            "where agent_player_id=#{agentId}")
    void updateLevelByAgentId(@Param("agentId") Long agentId, @Param("level") Integer level);

    /**
     * 修改与指定比例相同的值为新值
     */
    @Update("update " + TABLE_NAME + " set first_commission_rate = #{newRate} where first_commission_rate = #{oldRate}")
    void updateSameFirstRate(@Param("oldRate") Double oldRate, @Param("newRate") Double newRate);

    /**
     * 修改所有值为新值
     */
    @Update("update " + TABLE_NAME + " set first_commission_rate = #{rate}")
    void updateAllFirstRate(@Param("rate") Double rate);

    /**
     * 修改与指定比例相同的值为新值
     */
    @Update("update " + TABLE_NAME + " set first_commission_rate = #{newRate} where first_commission_rate = #{oldRate} " +
            "and agent_player_id = #{channelId}")
    void updateSameSecondRate(@Param("oldRate") Double oldRate, @Param("newRate") Double newRate, @Param("channelId") Long channelId);

    /**
     * 修改所有值为新值
     */
    @Update("update " + TABLE_NAME + " set first_commission_rate = #{rate} where agent_player_id = #{channelId}")
    void updateAllSecondRate(@Param("rate") Double rate, @Param("channelId") Long channelId);

    /**
     * 解绑玩家的下级成员
     */
    @Update("update " + TABLE_NAME + " set upper_player_id = null where upper_player_id = #{upperPlayerId}")
    void removeByUpperPlayer(@Param("upperPlayerId") Long upperPlayerId);

    /**
     * 删除代理信息及绑定该代理的玩家/代理信息
     */
    @Delete("delete from " + TABLE_NAME + " where player_id = #{agentId} or agent_player_id = #{agentId}")
    void deleteByAgentId(@Param("agentId") Long agentId);

    /**
     * 删除代理记录
     */
    @Delete("delete from " + TABLE_NAME + " where id = #{agent.id}")
    void delete(@Param("agent") AgentEntity agent);
}
