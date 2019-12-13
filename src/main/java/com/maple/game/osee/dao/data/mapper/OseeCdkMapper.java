package com.maple.game.osee.dao.data.mapper;

import java.util.List;

import org.apache.ibatis.annotations.*;

import com.maple.game.osee.dao.data.entity.OseeCdkEntity;

/**
 * CDK数据接口
 */
@Mapper
public interface OseeCdkMapper {

    String TABLE_NAME = "tbl_osee_cdk";

    /**
     * 新增数据
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`cdkey`, `type_id`, `rewards`, `user_id`, `nickname`) "
            + "VALUES (#{entity.cdk}, #{entity.typeId}, #{entity.rewards}, #{entity.userId}, #{entity.nickname})")
    void save(@Param("entity") OseeCdkEntity entity);

    /**
     * 根据cdk类型删除cdk
     */
    @Delete("delete from " + TABLE_NAME + " where `type_id` = #{typeId}")
    void deleteByTypeId(@Param("typeId") long typeId);

    /**
     * 获取所有数据
     */
    @Select("SELECT * FROM " + TABLE_NAME)
    List<OseeCdkEntity> getAll();

    /**
     * 查询单条数据
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE `id`=#{id}")
    OseeCdkEntity getById(@Param("id") long id);

    /**
     * 查询单条数据
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE `cdkey`=#{cdk}")
    OseeCdkEntity getByCdk(@Param("cdk") String cdk);

    /**
     * 查询玩家已使用的cdk类型
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE `user_id` = #{playerId} AND `type_id` = #{typeId}")
    OseeCdkEntity getByUsed(@Param("typeId") Long typeId, @Param("playerId") Long playerId);

    /**
     * 更新数据
     */
    @Update("UPDATE " + TABLE_NAME + " SET `user_id`=#{entity.userId}, `nickname`=#{entity.nickname} "
            + "WHERE `id`=#{entity.id}")
    void update(@Param("entity") OseeCdkEntity entity);

    /**
     * 创建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'cdk id', "
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', "
            + "`cdkey` varchar(32) NULL DEFAULT NULL COMMENT 'cdkey内容', "
            + "`type_id` bigint(20) NOT NULL COMMENT '类型id', "
            + "`rewards` varchar(512) NULL DEFAULT NULL COMMENT '奖励', `user_id` bigint(20) NULL COMMENT '兑换人id', "
            + "`nickname` varchar(512) NULL DEFAULT NULL COMMENT '兑换人昵称', "
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

}
