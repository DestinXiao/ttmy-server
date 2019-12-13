package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.AppGameLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AppGameLogMapper {

    String TABLE_NAME = "tbl_app_game_log";

    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '捕鱼竞技模式游戏记录id',\n" +
            "  `number` int(11) NOT NULL COMMENT '参赛人数',\n" +
            "  `income` int(255) NOT NULL COMMENT '报名收入',\n" +
            "  `reward_id` int(255) NOT NULL COMMENT '奖励支出 id',\n" +
            "  `stock` bigint(20) NOT NULL COMMENT '库存',\n" +
            "  `mode` int(255) NOT NULL COMMENT '游戏模式',\n" +
            "  `type` int(255) NOT NULL COMMENT '游戏类型',\n" +
            "`create_time` datetime(0) NOT NULL COMMENT '创建时间',\n" +
            "  PRIMARY KEY (`id`) USING BTREE\n" +
            ") ;")
    void createTable();

    @Insert("INSERT INTO " + TABLE_NAME + "(" +
            "`number`, `income`, `reward_id`, `stock`, `mode`, `create_time`, `type`" +
            ") VALUES (" +
            "#{entity.number}, #{entity.income}, #{entity.reward.id}, #{entity.stock}, #{entity.mode}, #{entity.createTime}, #{entity.type})")
    void save(@Param("entity") AppGameLogEntity entity);

    @Select("<script>" +
            "SELECT\n" +
            "tbl1.id,\n" +
            "tbl1.number,\n" +
            "tbl1.income,\n" +
            "tbl1.stock,\n" +
            "tbl1.`mode`,\n" +
            "tbl1.`type`,\n" +
            "tbl1.`create_time`,\n" +
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
            "tbl2.boss_bugle AS `reward.boss_bugle`\n" +
            "FROM\n" +
            "tbl_app_game_log AS tbl1 ,\n" +
            "tbl_app_reward_log AS tbl2\n" +
            "<where>" +
                "tbl1.reward_id = tbl2.id " +
                "<if test='startDate != null'>" +
                    "and Date(tbl1.`create_time`) BETWEEN #{startDate} AND #{endDate}" +
                "</if>" +
                "<if test= 'mode != null'>" +
                    "and #{mode} = tbl1.`mode`" +
                "</if>" +
                "<if test= 'type != null'>" +
                    "and #{type} = tbl1.`type`" +
                "</if>" +
            "</where>" +
            "LIMIT #{start}, #{end}" +
            "</script>")
    List<AppGameLogEntity> find(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("mode") Integer mode, @Param("start") int start,@Param("end") int end, @Param("type") Integer type);

    @Select("<script>" +
            "SELECT\n" +
            "count(tbl1.id) \n" +
            "FROM\n" +
            "tbl_app_game_log AS tbl1 ,\n" +
            "tbl_app_reward_log AS tbl2\n" +
            "<where>" +
            "tbl1.reward_id = tbl2.id " +
            "<if test='startDate != null'>" +
                "and Date(tbl1.`create_time`) BETWEEN #{startDate} AND #{endDate}" +
            "</if>" +
            "<if test= 'mode != null'>" +
                "and #{mode} = tbl1.`mode`" +
            "</if>" +
            "<if test= 'type != null'>" +
                "and #{type} = tbl1.`type`" +
            "</if>" +
            "</where>" +
            "</script>")
    int count(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("mode") Integer mode, @Param("type") Integer type);
}
