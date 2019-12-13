package com.maple.game.osee.dao.log.mapper;

import com.maple.game.osee.dao.log.entity.OseeFishingRecordLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OseeFishingRecordLogMapper {

    String TABLE_NAME = "tbl_osee_fishing_record_log";

    /**
     * 保存数据
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`player_id`,`room_index`, `spend_money`, `win_money`, " +
            "`drop_bronze_torpedo_num`, `drop_silver_torpedo_num`, `drop_gold_torpedo_num`) "
            + "VALUES (#{entity.playerId}, #{entity.roomIndex}, #{entity.spendMoney}, #{entity.winMoney}, "
            + "#{entity.dropBronzeTorpedoNum}, #{entity.dropSilverTorpedoNum}, #{entity.dropGoldTorpedoNum})")
    void save(@Param("entity") OseeFishingRecordLogEntity entity);

    /**
     * 创建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',"
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
            + "`player_id` bigint(20) NULL DEFAULT NULL COMMENT '玩家id',"
            + "`room_index` int(11) NULL COMMENT '场次序号',"
            + "`spend_money` bigint(20) NULL COMMENT '花费金币',"
            + "`win_money` bigint(20) NULL COMMENT '赢取金币',"
            + "`drop_bronze_torpedo_num` bigint(20) NULL COMMENT '掉落的青铜鱼雷数量',"
            + "`drop_silver_torpedo_num` bigint(20) NULL COMMENT '掉落的白银鱼雷数量',"
            + "`drop_gold_torpedo_num` bigint(20) NULL COMMENT '掉落的黄金鱼雷数量',"
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

    /**
     * 根据条件查询记录
     */
    @Select("SELECT * FROM " + TABLE_NAME + " log ${where} ORDER BY log.id DESC ${page}")
    List<OseeFishingRecordLogEntity> getLogList(@Param("where") String where, @Param("page") String page);

    /**
     * 根据条件查询记录数量
     */
    @Select("SELECT COUNT(*) totalNum FROM " + TABLE_NAME + " log ${where}")
    int getLogCount(@Param("where") String where);

    /**
     * 获取总数
     */
    @Select("select COALESCE(sum(${sum}), 0) from " + TABLE_NAME + " log ${where}")
    long getSum(@Param("sum") String sum, @Param("where") String where);
}
