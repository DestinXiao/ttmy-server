package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.TenChallengeRankingLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 拼十挑战赛玩家对局获胜金币排行榜数据操作接口
 *
 * @author Junlong
 */
@Mapper
public interface TenChallengeRankingLogMapper {

    String TABLE_NAME = "tbl_ttmy_ten_challenge_ranking_log";

    /**
     * 建表
     */
    @Update("create table if not exists " + TABLE_NAME + " (" +
            "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            "`user_id` bigint(20) NOT NULL COMMENT '用户id'," +
            "`nickname` varchar(64) NULL DEFAULT NULL COMMENT '昵称'," +
            "`head_index` int(11) NULL DEFAULT NULL COMMENT '头像序号'," +
            "`head_url` varchar(512) NULL DEFAULT NULL COMMENT '头像地址'," +
            "`score` bigint(20) NOT NULL COMMENT '得分'," +
            "`update_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据更新时间'," +
            "`create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间'," +
            " PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    @Insert("insert into " + TABLE_NAME + " (" +
            "user_id, nickname, head_index, head_url, score" +
            ") values (" +
            "#{entity.userId}, #{entity.nickname}, #{entity.headIndex}, #{entity.headUrl}, #{entity.score}" +
            ")")
    void save(@Param("entity") TenChallengeRankingLogEntity entity);

    @Update("update " + TABLE_NAME + " set " +
            "user_id = #{entity.userId}, nickname = #{entity.nickname}, head_index =  #{entity.headIndex}, head_url = #{entity.headUrl}, " +
            "score = #{entity.score}, update_time = #{entity.updateTime} " +
            "where id = #{entity.id}")
    void updateById(@Param("entity") TenChallengeRankingLogEntity entity);

    /**
     * 重置所有排行榜分数数据为0
     */
    @Update("update " + TABLE_NAME + " set score = 0, update_time = now()")
    int updateScoreToZero();

    @Select("select * from " + TABLE_NAME + " where user_id = #{userId}")
    TenChallengeRankingLogEntity getByUserId(@Param("userId") long userId);

    /**
     * 获取排行榜列表
     */
    @Select("select * from " + TABLE_NAME + " order by score desc, update_time desc limit 0,100")
    List<TenChallengeRankingLogEntity> getList();
}
