package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.AppRankLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

/**
 * 捕鱼大奖赛 游戏记录
 */
@Mapper
public interface AppRankLogMapper {

    String TABLE_NAME = "tbl_app_rank_log";


    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME +  "(\n" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '竞技模式玩家比赛排名表',\n" +
            "  `player_id` bigint(20) NOT NULL COMMENT '玩家id',\n" +
            "  `nickname` varchar(255)  NOT NULL COMMENT '玩家昵称',\n" +
            "  `score` int(255) NOT NULL COMMENT '玩家积分',\n" +
            "  `rank` int(255) NOT NULL COMMENT '玩家排名',\n" +
            "  `mode` int(255) NOT NULL COMMENT '竞技模式类型（1：大奖赛、2：全民赛、3：满人赛、4：道具赛）',\n" +
            "  `cost` int(10) NOT NULL COMMENT '报名消耗钻石数',\n" +
            "  `games` int(255) NOT NULL COMMENT '游戏参与局数',\n" +
            "  `change` int(255) NOT NULL COMMENT '玩家金币变化',\n" +
            "  `reward_id` int(255) NOT NULL COMMENT '奖励表id',\n" +
            "  `receive` int(255) NOT NULL COMMENT '玩家奖励是否领取',\n" +
            "  `receive_time` datetime(0) NULL DEFAULT NULL COMMENT '领取时间',\n" +
            "  `create_time` datetime(0) NOT NULL COMMENT '创建时间',\n" +
            "  `email_id` bigint(20) NOT NULL COMMENT '邮件id'\n" +
            "  `type` int(255) NOT NULL COMMENT '游戏类型'\n" +
            "  PRIMARY KEY (`id`) USING BTREE\n" +
            ") ;")
    void createTable();

    @Select("SELECT\n" +
                "tb1.id,\n" +
                "tb1.player_id,\n" +
                "tb1.nickname,\n" +
                "tb1.score,\n" +
                "tb1.rank,\n" +
                "tb1.`mode`,\n" +
                "tb1.cost,\n" +
                "tb1.games,\n" +
                "tb1.`change`,\n" +
                "tb1.receive,\n" +
                "tb1.receive_time,\n" +
                "tb1.create_time,\n" +
                "tb1.email_id,\n" +
                "tb1.type,\n" +
                "tb2.id AS `reward.id`,\n" +
                "tb2.gold AS `reward.gold`,\n" +
                "tb2.diamond AS `reward.diamond`,\n" +
                "tb2.lower_ball AS `reward.lower_ball`,\n" +
                "tb2.middle_ball AS `reward.middle_ball`,\n" +
                "tb2.high_ball AS `reward.high_ball`,\n" +
                "tb2.skill_lock AS `reward.skill_lock`,\n" +
                "tb2.skill_fast AS `reward.skill_fast`,\n" +
                "tb2.skill_crit AS `reward.skill_crit`,\n" +
                "tb2.skill_frozen AS `reward.skill_frozen`,\n" +
                "tb2.boss_bugle AS `reward.boss_bugle` \n" +
            "FROM\n" +
                "tbl_app_rank_log AS tb1\n" +
                "LEFT JOIN tbl_app_reward_log AS tb2 ON tb1.reward_id = tb2.id\n" +
            "WHERE Date(tb1.create_time) = #{date}\n" +
            "AND tb1.`type` = #{type}\n" +
            "LIMIT #{start}, #{end}")
    List<AppRankLogEntity> find(@Param("start") int start,@Param("end") int end, @Param("date") String date, @Param("type") int type);

    @Insert({"INSERT INTO " + TABLE_NAME + "(" +
            "`player_id`, `nickname`, `score`, `rank`, `mode`, `cost`, `games`, `change`, `reward_id`, `receive`, `receive_time`, `create_time`, `email_id`, `type`" +
            ") VALUES (" +
            "#{entity.playerId}, #{entity.nickname}, #{entity.score}, #{entity.rank}, #{entity.mode}," +
            " #{entity.cost}, #{entity.games}, #{entity.change}, #{entity.reward.id}, #{entity.receive}," +
            "#{entity.receiveTime}, #{entity.createTime}, #{entity.emailId}, #{entity.type})"})
    void save(@Param("entity") AppRankLogEntity entity);

    @Select("SELECT\n" +
            "count(tb1.id)" +
            "FROM\n" +
            "tbl_app_rank_log AS tb1\n" +
            "LEFT JOIN tbl_app_reward_log AS tb2 ON tb1.reward_id = tb2.id\n" +
            "WHERE Date(tb1.create_time) = #{date}\n" +
            "AND tb1.`type` = #{type}")
    int count(@Param("date") String date, @Param("type") int type);

    @Update("UPDATE `app_log`.`tbl_app_rank_log` SET `receive_time` = #{date}, `receive` = 1 WHERE `email_id` = #{emailId}")
    void updateByEamilId(@Param("emailId") long emailId, @Param("date") Date date);
}
