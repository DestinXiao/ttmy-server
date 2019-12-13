package com.maple.game.osee.dao.data.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.maple.game.osee.dao.data.entity.FruitLaBaRewardInfo;

@Mapper
public interface FruitLaBaRewardInfoMapper {
	
	String TABLE_NAME = "fruitLaBaRewardInfo";

	/**
	 * 新增数据
	 */
	@Insert("INSERT INTO " + TABLE_NAME + " (`user_id`, `achieve_num`, `reward_gold`, `reward_lottery`, `weather_receive`)"
			+ " VALUES (#{entity.userId}, #{entity.achieveNum}, #{entity.rewardGold}, #{entity.rewardLottery}, "
			+ "#{entity.weatherReceive})")
	@Options(useGeneratedKeys = true, keyProperty = "entity.id")
	void save(@Param("entity") FruitLaBaRewardInfo rewardInfo);
	
	/**
	 * 查询所有奖励数据
	 */
	@Select("SELECT * FROM " + TABLE_NAME + " ORDER BY `id` ASC")
	List<FruitLaBaRewardInfo> findAllReward();
	
	/**
	 * 根据用户id查找所有奖励数据
	 */
	@Select("SELECT * FROM " + TABLE_NAME + " WHERE user_id=#{userId} ORDER BY `id` ASC")
	List<FruitLaBaRewardInfo> findByUserId(@Param("userId") long userId);
	
	/**
	 * 根据奖励id删除数据
	 */
	@Delete("DELETE FROM " + TABLE_NAME + " WHERE id=#{id}")
	void delete(@Param("id") long id);
	
	@Select("SELECT * FROM " + TABLE_NAME + " WHERE id=#{id} ")
	FruitLaBaRewardInfo findById(@Param("id") long id);
	
	/**
	 * 创建表
	 */
	@Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '奖励id', "
			+ "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', "
			+ "`user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户id', "
			+ "`achieve_num` int(11) NULL DEFAULT NULL COMMENT '旋转次数', "
			+ "`reward_gold` int(11) NULL DEFAULT NULL COMMENT '奖励金币', "
			+ "`reward_lottery` int(11) NULL DEFAULT NULL COMMENT '奖励点券', "
			+ "`weather_receive` tinyint(1) NULL DEFAULT NULL COMMENT '是否领取', "
			+ "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
	void createTable();
	
}
