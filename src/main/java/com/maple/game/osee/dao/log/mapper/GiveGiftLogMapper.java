package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.GiveGiftLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 赠送礼物记录数据操作接口
 *
 * @author Junlong
 */
@Mapper
public interface GiveGiftLogMapper {
    String TABLE_NAME = "tbl_ttmy_give_gift_log";

    /**
     * 建表
     */
    @Update("create table if not exists " + TABLE_NAME + " (" +
            " `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            " `from_id` bigint(20) null comment '赠送人ID'," +
            " `from_name` varchar(100) null comment '赠送人昵称'," +
            " `to_id` bigint(20) null comment '被赠送人ID'," +
            " `to_name` varchar(100) null comment '被赠送人昵称'," +
            " `gift_name` varchar(100) null comment '赠送礼物名称'," +
            " `gift_num` bigint(20) null comment '赠送礼物数量'," +
            " `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间'," +
            " PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 保存数据
     */
    @Insert("insert into " + TABLE_NAME + " (" +
            "from_id, from_name, to_id, to_name, gift_name, gift_num" +
            ") values (" +
            "#{entity.fromId}, #{entity.fromName}, #{entity.toId}, #{entity.toName}, #{entity.giftName}, #{entity.giftNum}" +
            ")")
    void save(@Param("entity") GiveGiftLogEntity entity);

    /**
     * 根据条件获取礼物赠送数据
     */
    @Select("select * from " + TABLE_NAME + " ${condition}")
    List<GiveGiftLogEntity> getList(@Param("condition") String condition);

    /**
     * 获取指定条件所有的数据条数
     */
    @Select("select COALESCE(count(*), 0) from " + TABLE_NAME + " ${condition}")
    long getCount(@Param("condition") String condition);

    /**
     * 获取总的礼物赠送数量
     */
    @Select("select COALESCE(sum(gift_num), 0) from " + TABLE_NAME + " ${condition}")
    long getGiftTotalNum(@Param("condition") String condition);
}
