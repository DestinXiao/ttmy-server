package com.maple.game.osee.dao.data.mapper;

import com.maple.database.data.mapper.UserMapper;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 玩家实体数据交互接口
 */
@Mapper
public interface OseePlayerMapper {

    String TABLE_NAME = "tbl_osee_player";

    /**
     * 新增数据
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`user_id`, `money`, `bank_money`, `bank_password`, `lottery`, "
            + "`diamond`, `vip_level`, `level`, `experience`, `recharge_money`, `lose_control`, `player_type`, "
            + "`bronze_torpedo`, `silver_torpedo`, `gold_torpedo`, "
            + "`skill_lock`, `skill_frozen`, `skill_fast`, `skill_crit`, "
            + "`battery_level`, `monthcard_expire_date`, `ten_challenge_times`, `boss_bugle`, "
            + "`qszs_battery_expire_date`, `blnh_battery_expire_date`, `lhtz_battery_expire_date`, `swhp_battery_expire_date`, "
            + "`dragon_crystal`, `fen_shen`"
            + ") VALUES ("
            + "#{entity.userId}, #{entity.money}, #{entity.bankMoney}, #{entity.bankPassword}, "
            + "#{entity.lottery}, #{entity.diamond}, #{entity.vipLevel}, #{entity.level}, "
            + "#{entity.experience}, #{entity.rechargeMoney}, #{entity.loseControl}, #{entity.playerType}, "
            + "#{entity.bronzeTorpedo}, #{entity.silverTorpedo}, #{entity.goldTorpedo}, "
            + "#{entity.skillLock}, #{entity.skillFrozen}, #{entity.skillFast}, #{entity.skillCrit},"
            + "#{entity.batteryLevel}, #{entity.monthCardExpireDate}, #{entity.tenChallengeTimes}, #{entity.bossBugle},"
            + "#{entity.qszsBatteryExpireDate}, #{entity.blnhBatteryExpireDate}, #{entity.lhtzBatteryExpireDate}, #{entity.swhpBatteryExpireDate}, "
            + "#{entity.dragonCrystal}, #{entity.fenShen}"
            + ")")
    @Options(useGeneratedKeys = true, keyProperty = "entity.id")
    void save(@Param("entity") OseePlayerEntity playerEntity);

    /**
     * 更新数据
     */
    @Update("UPDATE " + TABLE_NAME + " SET `money`=#{entity.money}, `bank_money`=#{entity.bankMoney}, "
            + "`bank_password`=#{entity.bankPassword}, `lottery`=#{entity.lottery}, `diamond`=#{entity.diamond}, "
            + "`vip_level`=#{entity.vipLevel}, `level`=#{entity.level}, `experience`=#{entity.experience}, "
            + "`recharge_money`=#{entity.rechargeMoney}, `lose_control`=#{entity.loseControl}, "
            + "`player_type`=#{entity.playerType}, "
            + "`bronze_torpedo`=#{entity.bronzeTorpedo}, `silver_torpedo`=#{entity.silverTorpedo}, `gold_torpedo`=#{entity.goldTorpedo},"
            + "`skill_lock`=#{entity.skillLock}, `skill_frozen`=#{entity.skillFrozen}, "
            + "`skill_fast`=#{entity.skillFast}, `skill_crit`=#{entity.skillCrit}, "
            + "`battery_level`=#{entity.batteryLevel}, "
            + "`monthcard_expire_date`=#{entity.monthCardExpireDate}, `ten_challenge_times`=#{entity.tenChallengeTimes}, "
            + "`boss_bugle` = #{entity.bossBugle},"
            + "`qszs_battery_expire_date` = #{entity.qszsBatteryExpireDate}, `blnh_battery_expire_date` = #{entity.blnhBatteryExpireDate}, "
            + "`lhtz_battery_expire_date` = #{entity.lhtzBatteryExpireDate}, `swhp_battery_expire_date` = #{entity.swhpBatteryExpireDate}, "
            + "`dragon_crystal` = #{entity.dragonCrystal}, `fen_shen` = #{entity.fenShen}"
            + " WHERE id=#{entity.id}")
    void update(@Param("entity") OseePlayerEntity playerEntity);

    /**
     * 根据用户id查找玩家数据
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE user_id=#{userId}")
    OseePlayerEntity findByUserId(@Param("userId") long useId);

    /**
     * 查询金币排行榜
     */
    @Select("SELECT * FROM " + TABLE_NAME + " ORDER BY (money + bank_money) DESC, vip_level DESC LIMIT 0, ${size}")
    List<OseePlayerEntity> selectMoneyRanking(@Param("size") int size);

    /**
     * 查询vip排行榜
     */
    @Select("SELECT * FROM " + TABLE_NAME + " WHERE vip_level > 0 ORDER BY vip_level DESC, (money + bank_money) DESC "
            + "LIMIT 0, ${size}")
    List<OseePlayerEntity> selectVipRanking(@Param("size") int size);

    /**
     * 查询黄金鱼雷排行榜
     */
    @Select("SELECT * FROM " + TABLE_NAME + " ORDER BY gold_torpedo DESC, vip_level DESC LIMIT 0, ${size}")
    List<OseePlayerEntity> selectGoldTorpedoRanking(@Param("size") int size);

    /**
     * 查询用户id列表
     */
    @Select("select user.id from (" + UserMapper.TABLE_NAME + " user," + TABLE_NAME + " player) " +
            "LEFT JOIN tbl_ttmy_agent agent ON `user`.id = agent.player_id " +
            "where user.id = player.user_id ${condition} ORDER BY user.id DESC ${page}")
    List<Long> getGmPlayerIdList(@Param("condition") String condition, @Param("page") String page);

    /**
     * 查询用户数量
     */
    @Select("select count(user.id) from (" + UserMapper.TABLE_NAME + " user," + TABLE_NAME + " player) " +
            "LEFT JOIN tbl_ttmy_agent agent ON `user`.id = agent.player_id " +
            "where user.id = player.user_id ${condition}")
    int getGmPlayerCount(@Param("condition") String condition);

    /**
     * 获取后台统计记录
     */
    @Select("SELECT COALESCE(SUM(money), 0) money, COALESCE(SUM(bank_money), 0) bankMoney FROM " + TABLE_NAME)
    Map<String, Object> getGmStatistics();

    /**
     * 创建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + "id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '玩家id', "
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', "
            + "`user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户id', "
            + "`money` bigint(20) NULL DEFAULT NULL COMMENT '玩家金币', "
            + "`bank_money` bigint(20) NULL DEFAULT NULL COMMENT '保险箱金币', "
            + "`bank_password` varchar(32) NULL DEFAULT NULL COMMENT '保险箱密码', "
            + "`lottery` bigint(20) NULL DEFAULT NULL COMMENT '奖券', "
            + "`diamond` bigint(20) NULL DEFAULT NULL COMMENT '钻石', "
            + "`vip_level` int(11) NULL DEFAULT NULL COMMENT 'vip等级', "
            + "`level` int(11) NULL DEFAULT NULL COMMENT '玩家等级', "
            + "`experience` bigint(20) NULL DEFAULT NULL COMMENT '玩家经验', "
            + "`recharge_money` bigint(20) NULL DEFAULT NULL COMMENT '充值金额', "
            + "`lose_control` int(11) NULL DEFAULT NULL COMMENT '必输控制', "
            + "`player_type` int(11) NULL DEFAULT NULL COMMENT '玩家类型', "
            + "`bronze_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '玩家青铜鱼雷数量', "
            + "`silver_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '玩家白银鱼雷数量', "
            + "`gold_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '玩家黄金鱼雷数量', "
            + "`skill_lock` bigint(20) NULL DEFAULT NULL COMMENT '玩家锁定技能数量', "
            + "`skill_frozen` bigint(20) NULL DEFAULT NULL COMMENT '玩家冰冻技能数量', "
            + "`skill_fast` bigint(20) NULL DEFAULT NULL COMMENT '玩家急速技能数量', "
            + "`skill_crit` bigint(20) NULL DEFAULT NULL COMMENT '玩家暴击技能数量', "
            + "`boss_bugle` bigint(20) NULL DEFAULT NULL COMMENT '玩家BOSS号角数量', "
            + "`battery_level` int(11) NULL DEFAULT NULL COMMENT '玩家目前拥有的最高炮台等级', "
            + "`monthcard_expire_date` date NULL DEFAULT NULL COMMENT '玩家月卡到期时间', "
            + "`ten_challenge_times` bigint(20) NULL DEFAULT NULL COMMENT '玩家拼十剩余挑战次数', "
            + "`qszs_battery_expire_date` date NULL DEFAULT NULL COMMENT '骑士之誓炮台外观到期时间', "
            + "`blnh_battery_expire_date` date NULL DEFAULT NULL COMMENT '冰龙怒吼炮台外观到期时间', "
            + "`lhtz_battery_expire_date` date NULL DEFAULT NULL COMMENT '莲花童子炮台外观到期时间', "
            + "`swhp_battery_expire_date` date NULL DEFAULT NULL COMMENT '死亡火炮炮台外观到期时间', "
            + "`dragon_crystal` bigint(20) NULL DEFAULT NULL COMMENT '玩家拥有的龙晶数量', "
            + "`fen_shen` bigint(20) NULL DEFAULT NULL COMMENT '玩家拥有的分身炮道具数量', "
            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

}
