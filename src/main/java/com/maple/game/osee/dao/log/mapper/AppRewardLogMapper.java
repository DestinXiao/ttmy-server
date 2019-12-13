package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.AppRewardLogEntity;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AppRewardLogMapper {

    String TABLE_NAME = "tbl_app_reward_log";

    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "  (\n" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '玩家奖励表',\n" +
            "  `player_id` bigint(20) NOT NULL COMMENT '玩家id',\n" +
            "  `gold` int(11) NOT NULL COMMENT '金币',\n" +
            "  `diamond` int(11) NOT NULL COMMENT '钻石',\n" +
            "  `lower_ball` int(11) NOT NULL COMMENT '低阶龙珠',\n" +
            "  `middle_ball` int(11) NOT NULL COMMENT '中阶龙珠',\n" +
            "  `high_ball` int(11) NOT NULL COMMENT '高阶龙珠',\n" +
            "  `skill_lock` int(11) NOT NULL COMMENT '锁定技能',\n" +
            "  `skill_fast` int(255) NOT NULL COMMENT '极速技能',\n" +
            "  `skill_crit` int(255) NOT NULL COMMENT '暴击技能',\n" +
            "  `skill_frozen` int(255) NOT NULL COMMENT '冰冻技能',\n" +
            "  `boss_bugle` int(255) NOT NULL COMMENT 'BOSS号角',\n" +
            "  PRIMARY KEY (`id`) USING BTREE\n" +
            ");")
    void createTable();

    @Insert("INSERT INTO " + TABLE_NAME + "(" +
            "`player_id`, `gold`, `diamond`, `lower_ball`, `middle_ball`, `high_ball`" +
            ", `skill_lock`, `skill_fast`, `skill_crit`, `skill_frozen`, `boss_bugle`" +
            ") VALUES (" +
            "#{entity.playerId}, #{entity.gold}, #{entity.diamond}, #{entity.lowerBall}, #{entity.middleBall}, #{entity.highBall}" +
            ", #{entity.skillLock}, #{entity.skillFast}, #{entity.skillCrit}, #{entity.skillFrozen}, #{entity.bossBugle})")
    @Options(useGeneratedKeys = true, keyProperty = "entity.id")
    void save(@Param("entity") AppRewardLogEntity entity);

    @Update("UPDATE "+ TABLE_NAME +" \n" +
            "SET gold = #{entity.gold},\n" +
            "player_id = #{entity.playerId},\n" +
            "diamond = #{entity.diamond},\n" +
            "lower_ball = #{entity.lowerBall},\n" +
            "middle_ball = #{entity.middleBall},\n" +
            "high_ball = #{entity.highBall},\n" +
            "skill_crit = #{entity.skillCrit},\n" +
            "skill_fast = #{entity.skillFast},\n" +
            "skill_lock = #{entity.skillLock},\n" +
            "skill_frozen = #{entity.skillFrozen},\n" +
            "boss_bugle = #{entity.bossBugle}\n" +
            "WHERE\n" +
            "id = #{entity.id}")
    int update(@Param("entity") AppRewardLogEntity entity);

    @Delete("DELETE FROM " + TABLE_NAME + " WHERE `id` = #{id}")
    void delete(@Param("id") int id);
}
