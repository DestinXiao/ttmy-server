package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.AppRewardRankEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AppRewardRankMapper {

    String TABLE_NAME = "tbl_app_rank_reward_log";

    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n" +
            "  `id` int(0) NOT NULL AUTO_INCREMENT,\n" +
            "  `name` varchar(255) NULL COMMENT '名称',\n" +
            "  `rank` int(255) NOT NULL COMMENT '小于等于该排名',\n" +
            "  `type` int(255) NOT NULL COMMENT '类型，日排名1 周排名2',\n" +
            "  `status` int(255) NOT NULL COMMENT '状态',\n" +
            "  `reward_id` int(0) NOT NULL COMMENT '奖励表id',\n" +
            "  `update_time` datetime(0) NOT NULL COMMENT '最后一次更新时间',\n" +
            "  PRIMARY KEY (`id`)\n" +
            ");" )
    void create();

    @Select("SELECT\n" +
            "tbl1.id,\n" +
            "tbl1.`name`,\n" +
            "tbl1.rank,\n" +
            "tbl1.type,\n" +
            "tbl1.`status`,\n" +
            "tbl1.update_time,\n" +
            "tbl2.id AS `reward.id`,\n" +
            "tbl2.gold AS `reward.gold`,\n" +
            "tbl2.diamond AS `reward.diamond`,\n" +
            "tbl2.lower_ball AS `reward.lower_ball`,\n" +
            "tbl2.middle_ball AS `reward.middle_ball`,\n" +
            "tbl2.high_ball AS `reward.high_ball`,\n" +
            "tbl2.skill_lock AS `reward.skill_lock`,\n" +
            "tbl2.skill_fast AS `reward.skill_fast`,\n" +
            "tbl2.skill_crit AS `reward.skill_crit`,\n" +
            "tbl2.skill_frozen AS `reward.skill_frozen`,\n" +
            "tbl2.boss_bugle AS `reward.boss_bugle`" +
            "FROM\n" +
            TABLE_NAME + " AS tbl1 LEFT JOIN\n" +
            "tbl_app_reward_log AS tbl2\n" +
            "ON tbl1.reward_id = tbl2.id")
    List<AppRewardRankEntity> find();

    @Select("SELECT\n" +
            "tbl1.id,\n" +
            "tbl1.`name`,\n" +
            "tbl1.rank,\n" +
            "tbl1.type,\n" +
            "tbl1.`status`,\n" +
            "tbl1.update_time,\n" +
            "tbl2.id AS `reward.id`,\n" +
            "tbl2.gold AS `reward.gold`,\n" +
            "tbl2.diamond AS `reward.diamond`,\n" +
            "tbl2.lower_ball AS `reward.lower_ball`,\n" +
            "tbl2.middle_ball AS `reward.middle_ball`,\n" +
            "tbl2.high_ball AS `reward.high_ball`,\n" +
            "tbl2.skill_lock AS `reward.skill_lock`,\n" +
            "tbl2.skill_fast AS `reward.skill_fast`,\n" +
            "tbl2.skill_crit AS `reward.skill_crit`,\n" +
            "tbl2.skill_frozen AS `reward.skill_frozen`,\n" +
            "tbl2.boss_bugle AS `reward.boss_bugle`" +
            "FROM\n" +
            TABLE_NAME + " AS tbl1 LEFT JOIN\n" +
            "tbl_app_reward_log AS tbl2\n" +
            "ON tbl1.reward_id = tbl2.id WHERE tbl1.id = #{id}")
    AppRewardRankEntity findById(@Param("id") int id);

    @Insert("INSERT INTO " +
            "`app_log`.`tbl_app_rank_reward_log`(  `rank`, `type`, `status`, `reward_id`, `update_time`" +
            ") VALUES (" +
            "  #{entity.rank}, #{entity.type}, #{entity.status}, #{entity.reward.id}, #{entity.updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "entity.id")
    int save(@Param("entity")AppRewardRankEntity entity);

    @Delete("DELETE FROM " + TABLE_NAME + " WHERE `id` = #{id}")
    void delete(@Param("id") int id);

    @Select("SELECT\n" +
            "tbl1.id,\n" +
            "tbl1.`name`,\n" +
            "tbl1.rank,\n" +
            "tbl1.type,\n" +
            "tbl1.`status`,\n" +
            "tbl1.update_time,\n" +
            "tbl2.id AS `reward.id`,\n" +
            "tbl2.gold AS `reward.gold`,\n" +
            "tbl2.diamond AS `reward.diamond`,\n" +
            "tbl2.lower_ball AS `reward.lower_ball`,\n" +
            "tbl2.middle_ball AS `reward.middle_ball`,\n" +
            "tbl2.high_ball AS `reward.high_ball`,\n" +
            "tbl2.skill_lock AS `reward.skill_lock`,\n" +
            "tbl2.skill_fast AS `reward.skill_fast`,\n" +
            "tbl2.skill_crit AS `reward.skill_crit`,\n" +
            "tbl2.skill_frozen AS `reward.skill_frozen`,\n" +
            "tbl2.boss_bugle AS `reward.boss_bugle`" +
            "FROM\n" +
            TABLE_NAME + " AS tbl1 LEFT JOIN\n" +
            "tbl_app_reward_log AS tbl2\n" +
            "ON tbl1.reward_id = tbl2.id WHERE tbl1.type = #{type}")
    List<AppRewardRankEntity> findByType(@Param("type") int type);

    @Update("UPDATE " + TABLE_NAME + " SET `rank`=#{entity.rank} WHERE `id`=#{entity.id}")
    void update(@Param("entity") AppRewardRankEntity entity);

    @Select("SELECT\n" +
            "tbl1.id,\n" +
            "tbl1.`name`,\n" +
            "tbl1.rank,\n" +
            "tbl1.type,\n" +
            "tbl1.`status`,\n" +
            "tbl1.update_time,\n" +
            "tbl2.id AS `reward.id`,\n" +
            "tbl2.gold AS `reward.gold`,\n" +
            "tbl2.diamond AS `reward.diamond`,\n" +
            "tbl2.lower_ball AS `reward.lower_ball`,\n" +
            "tbl2.middle_ball AS `reward.middle_ball`,\n" +
            "tbl2.high_ball AS `reward.high_ball`,\n" +
            "tbl2.skill_lock AS `reward.skill_lock`,\n" +
            "tbl2.skill_fast AS `reward.skill_fast`,\n" +
            "tbl2.skill_crit AS `reward.skill_crit`,\n" +
            "tbl2.skill_frozen AS `reward.skill_frozen`,\n" +
            "tbl2.boss_bugle AS `reward.boss_bugle`" +
            "FROM\n" +
            TABLE_NAME + " AS tbl1 LEFT JOIN\n" +
            "tbl_app_reward_log AS tbl2\n" +
            "ON tbl1.reward_id = tbl2.id WHERE tbl1.type = #{type} AND tbl1.rank >= #{rank} ORDER BY tbl1.rank LIMIT 1")
    AppRewardRankEntity findReward(int type, int rank);
}
