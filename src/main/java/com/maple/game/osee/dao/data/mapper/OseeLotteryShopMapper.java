package com.maple.game.osee.dao.data.mapper;

import com.maple.game.osee.dao.data.entity.OseeLotteryShopEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 1688 奖券商品接口
 */
@Mapper
public interface OseeLotteryShopMapper {

    String TABLE_NAME = "tbl_osee_lottery_shop";

    /**
     * 根据id查找记录
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE `id` = #{id}")
    OseeLotteryShopEntity getById(@Param("id") long id);

    /**
     * 获取所有数据
     */
    @Select("SELECT * FROM " + TABLE_NAME + " ORDER BY `index`")
    List<OseeLotteryShopEntity> getAll();

    /**
     * 获取所有实物数据
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE `type` = 1 ORDER BY `index`")
    List<OseeLotteryShopEntity> getAllEntity();

    /**
     * 根据条件查询数据
     */
    @Select("SELECT * FROM " + TABLE_NAME + " ${where} ORDER BY `index` ${page}")
    List<OseeLotteryShopEntity> selectList(@Param("where") String where, @Param("page") String page);

    /**
     * 根据条件查询数据数量
     */
    @Select("SELECT COUNT(*) FROM " + TABLE_NAME + " ${where}")
    int selectCount(@Param("where") String where);

    /**
     * 新增数据
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`index`, `type`, `count`, `name`, `cost`, `img`, `size`, `used_size`, "
            + "`refresh_type`, `send_type`, `stock`) VALUES (#{entity.index}, #{entity.type}, #{entity.count}, #{entity.name}, #{entity.cost}, "
            + "#{entity.img}, #{entity.size}, #{entity.usedSize}, #{entity.refreshType}, "
            + "#{entity.sendType}, #{entity.stock})")
    void save(@Param("entity") OseeLotteryShopEntity entity);

    /**
     * 更新数据
     */
    @Update("UPDATE " + TABLE_NAME + " SET `index`=#{entity.index}, `type`=#{entity.type}, `count`=#{entity.count}, "
            + "`name`=#{entity.name}, `cost`=#{entity.cost}, `img`=#{entity.img}, `size`=#{entity.size}, "
            + "`used_size`=#{entity.usedSize}, `refresh_type`=#{entity.refreshType}, "
            + "`send_type`=#{entity.sendType}, `stock`=#{entity.stock}"
            + " WHERE `id`=#{entity.id}")
    void update(@Param("entity") OseeLotteryShopEntity entity);

    /**
     * 删除数据
     */
    @Delete("DELETE FROM " + TABLE_NAME + " WHERE `id`=#{entity.id}")
    void delete(@Param("entity") OseeLotteryShopEntity entity);

    /**
     * 删除数据
     */
    @Delete("DELETE FROM " + TABLE_NAME + " WHERE `id`=#{id}")
    void deleteById(@Param("id") long id);

    /**
     * 创建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类型id', "
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', "
            + "`index` int(11) NULL DEFAULT NULL COMMENT '序号', `type` int(11) NULL DEFAULT NULL COMMENT '类型', "
            + "`count` bigint(20) NOT NULL COMMENT '物品数量', `name` varchar(32) NULL DEFAULT NULL COMMENT '名称', "
            + "`cost` bigint(20) NOT NULL COMMENT '消耗奖券数量', `img` varchar(512) NULL DEFAULT NULL COMMENT '图片', "
            + "`size` int(11) NULL DEFAULT NULL COMMENT '总数量', `used_size` int(11) NULL DEFAULT NULL COMMENT '已兑换数量', "
            + "`send_type` int(11) NULL DEFAULT NULL COMMENT '发货类型', `stock` int(11) NULL DEFAULT NULL COMMENT '库存', "
            + "`refresh_type` int(11) NULL DEFAULT NULL COMMENT '刷新类型', "
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

}
