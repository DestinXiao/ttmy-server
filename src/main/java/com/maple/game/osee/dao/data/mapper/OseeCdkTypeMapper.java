package com.maple.game.osee.dao.data.mapper;

import java.util.List;

import org.apache.ibatis.annotations.*;

import com.maple.game.osee.dao.data.entity.OseeCdkTypeEntity;

/**
 * 1688 cdk类型接口
 */
@Mapper
public interface OseeCdkTypeMapper {

    String TABLE_NAME = "tbl_osee_cdk_type";

    /**
     * 新增数据
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`name`, `start_with`) VALUES (#{entity.name}, #{entity.startWith})")
    @Options(useGeneratedKeys = true, keyProperty = "entity.id")
    void save(@Param("entity") OseeCdkTypeEntity playerEntity);

    /**
     * 删除cdk类型
     */
    @Delete("delete from " + TABLE_NAME + " where `id` = #{id}")
    void delete(@Param("id") long id);

    /**
     * 获取所有cdk类型
     */
    @Select("SELECT * FROM " + TABLE_NAME)
    List<OseeCdkTypeEntity> getAllCdkType();

    /**
     * 根据id查找cdk类型
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE `id` = #{id}")
    OseeCdkTypeEntity getById(@Param("id") long id);

    /**
     * 根据开头字符查找cdk类型
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE `start_with` = #{start}")
    OseeCdkTypeEntity getByStart(@Param("start") String startWith);

    /**
     * 创建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类型id', "
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', "
            + "`name` varchar(32) NULL DEFAULT NULL COMMENT '类型名', "
            + "`start_with` varchar(32) NULL DEFAULT NULL COMMENT '开头字符', "
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

}
