package com.maple.game.osee.dao.data.mapper;

import com.maple.game.osee.dao.data.entity.StockEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 奖券商城物品库存数据操作接口
 *
 * @author Junlong
 */
@Mapper
public interface StockMapper {
    String TABLE_NAME = "tbl_osee_lottery_shop_stock";

    /**
     * 建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            "  `shop_id` bigint(20) NOT NULL COMMENT '道具库存属于的商品ID'," +
            "  `user_id` bigint(20) NULL COMMENT '兑换该卡的玩家ID'," +
            "  `number` varchar(100) NOT NULL COMMENT '卡号'," +
            "  `password` varchar(100) NOT NULL COMMENT '卡密'," +
            "  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据创建时间'," +
            "  PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 保存数据
     */
    @Insert("insert into " + TABLE_NAME +
            "(`shop_id`,`user_id`,`number`,`password`)" +
            " values " +
            "(#{entity.shopId},#{entity.userId},#{entity.number},#{entity.password})")
    void save(@Param("entity") StockEntity entity);

    /**
     * 更新数据
     */
    @Update("update " + TABLE_NAME + " set "
            + "`shop_id` = #{entity.shopId}, `user_id` = #{entity.userId},"
            + "`number` = #{entity.number}, `password` = #{entity.password}"
            + " where `id` = #{entity.id}")
    void update(@Param("entity") StockEntity stockEntity);

    /**
     * 获取数据列表
     */
    @Select("select * from " + TABLE_NAME + " ${query}")
    List<StockEntity> getList(@Param("query") String query);

    /**
     * 获取指定条件所有的数据条数
     */
    @Select("select COALESCE(count(*), 0) from " + TABLE_NAME + " ${query}")
    long getCount(@Param("query") String query);

    /**
     * 获取指定商品未使用的库存
     */
    @Select("select COALESCE(count(*), 0) from " + TABLE_NAME + " where `user_id` is null and `shop_id` = #{shopId}")
    long getUnusedCount(@Param("shopId") long shopId);

    /**
     * 获取一项未使用的库存数据
     */
    @Select("select * from " + TABLE_NAME + " where `user_id` is null and `shop_id` = #{shopId} limit 0,1")
    StockEntity getUnusedOne(@Param("shopId") long shopId);

    /**
     * 根据id查找数据
     */
    @Select("select * from " + TABLE_NAME + " where `id` = #{id}")
    StockEntity getById(@Param("id") long id);
}
