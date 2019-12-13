package com.maple.game.osee.dao.data.mapper;

import com.maple.game.osee.dao.data.entity.AddressEntity;
import org.apache.ibatis.annotations.*;

/**
 * 玩家收货地址数据库操作接口
 *
 * @author Junlong
 */
@Mapper
public interface AddressMapper {

    String TABLE_NAME = "tbl_ttmy_address";

    /**
     * 建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID'," +
            "  `player_id` bigint(20) NOT NULL COMMENT '玩家ID'," +
            "  `name` varchar(50) NOT NULL COMMENT '玩家昵称'," +
            "  `phone` varchar(11) NOT NULL COMMENT '玩家手机号码'," +
            "  `address` varchar(500) NOT NULL COMMENT '玩家收货地址'," +
            "  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据创建时间'," +
            "  PRIMARY KEY (`id`) USING BTREE" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 100001;")
    void createTable();

    /**
     * 保存数据
     */
    @Insert("insert into " + TABLE_NAME +
            " (player_id, name, phone, address)" +
            " values " +
            " (#{entity.playerId}, #{entity.name}, #{entity.phone}, #{entity.address})")
    @Options(useGeneratedKeys = true, keyProperty = "entity.id")
    void save(@Param("entity") AddressEntity entity);

    /**
     * 更新数据
     */
    @Update("update " + TABLE_NAME + " set " +
            "player_id = #{entity.playerId}, name = #{entity.name}, " +
            "phone = #{entity.phone}, address = #{entity.address} " +
            "where id = #{entity.id}")
    void update(@Param("entity") AddressEntity entity);

    /**
     * 获取对应玩家ID的收货地址信息
     */
    @Select("select * from " + TABLE_NAME + " where player_id = #{playerId}")
    AddressEntity getByPlayerId(@Param("playerId") Long playerId);
}
