package com.maple.game.osee.dao.data.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.maple.game.osee.dao.data.entity.OseeNoticeEntity;

/**
 * 1688 公告接口
 */
@Mapper
public interface OseeNoticeMapper {

    String TABLE_NAME = "tbl_osee_notice";

    /**
     * 新增数据
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`index`, `title`, `content`, `start_time`, `end_time`) "
            + "VALUES (#{entity.index}, #{entity.title}, #{entity.content}, #{entity.startTime}, #{entity.endTime})")
    @Options(useGeneratedKeys = true, keyProperty = "entity.id")
    void save(@Param("entity") OseeNoticeEntity entity);

    /**
     * 获取所有数据
     */
    @Select("SELECT * FROM " + TABLE_NAME + " ORDER BY `index`")
    List<OseeNoticeEntity> getAll();

    /**
     * 根据ID查找公告
     */
    @Select("select * from " + TABLE_NAME + " where id = #{id}")
    OseeNoticeEntity getById(@Param("id") long id);

    /**
     * 更新数据
     */
    @Update("UPDATE " + TABLE_NAME + " SET `index`=#{entity.index}, `title`=#{entity.title}, `content`=#{entity.content}, "
            + "`start_time`=#{entity.startTime}, `end_time`=#{entity.endTime} WHERE `id`=#{entity.id}")
    void update(@Param("entity") OseeNoticeEntity entity);

    /**
     * 删除数据
     */
    @Delete("DELETE FROM " + TABLE_NAME + " WHERE `id`=#{id}")
    void deleteById(@Param("id") long id);

    /**
     * 删除数据
     */
    @Delete("DELETE FROM " + TABLE_NAME + " WHERE `id`=#{entity.id}")
    void delete(@Param("entity") OseeNoticeEntity entity);

    /**
     * 创建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类型id', "
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', "
            + "`index` int(11) NULL DEFAULT NULL COMMENT '序号', "
            + "`title` varchar(32) NULL DEFAULT NULL COMMENT '标题', "
            + "`content` varchar(512) NULL DEFAULT NULL COMMENT '内容', "
            + "`start_time` timestamp NULL COMMENT '生效时间', "
            + "`end_time` timestamp NULL COMMENT '失效时间', "
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

}
