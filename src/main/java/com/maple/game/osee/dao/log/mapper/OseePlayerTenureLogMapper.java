package com.maple.game.osee.dao.log.mapper;

import com.maple.database.data.mapper.UserMapper;
import com.maple.game.osee.dao.log.entity.OseePlayerTenureLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户账户变动日志接口
 */
@Mapper
public interface OseePlayerTenureLogMapper {

    String TABLE_NAME = "tbl_osee_player_tenure_log";

    /**
     * 保存数据
     */
    @Insert("INSERT INTO " + TABLE_NAME + " (`user_id`, `nickname`, `reason`, "
            + "`pre_diamond`, `change_diamond`, `pre_money`, `change_money`, "
            + "`pre_lottery`, `change_lottery`, `pre_bank_money`, `change_bank_money`,"
            + "`pre_bronze_torpedo`, `change_bronze_torpedo`, `pre_silver_torpedo`, `change_silver_torpedo`, `pre_gold_torpedo`, `change_gold_torpedo`,"
            + "`pre_skill_lock`, `change_skill_lock`, `pre_skill_frozen`, `change_skill_frozen`, `pre_skill_fast`, `change_skill_fast`, `pre_skill_crit`, `change_skill_crit`,"
            + "`pre_boss_bugle`, `change_boss_bugle`"
            + ") VALUES ("
            + "#{entity.userId}, #{entity.nickname}, #{entity.reason}, "
            + "#{entity.preDiamond}, #{entity.changeDiamond}, #{entity.preMoney}, #{entity.changeMoney}, "
            + "#{entity.preLottery}, #{entity.changeLottery}, #{entity.preBankMoney}, #{entity.changeBankMoney},"
            + "#{entity.preBronzeTorpedo}, #{entity.changeBronzeTorpedo}, #{entity.preSilverTorpedo}, #{entity.changeSilverTorpedo}, #{entity.preGoldTorpedo}, #{entity.changeGoldTorpedo},"
            + "#{entity.preSkillLock}, #{entity.changeSkillLock}, #{entity.preSkillFrozen}, #{entity.changeSkillFrozen}, #{entity.preSkillFast}, #{entity.changeSkillFast}, #{entity.preSkillCrit}, #{entity.changeSkillCrit},"
            + "#{entity.preBossBugle}, #{entity.changeBossBugle}"
            + ")")
    void save(@Param("entity") OseePlayerTenureLogEntity entity);

    /**
     * 根据条件查询记录
     */
    @Select("SELECT * FROM " + TABLE_NAME + " log LEFT JOIN app_data." + UserMapper.TABLE_NAME
            + " user ON user.id = log.user_id ${where} ORDER BY log.id DESC ${page}")
    List<OseePlayerTenureLogEntity> getLogList(@Param("where") String where, @Param("page") String page);

    /**
     * 根据条件查询记录数量
     */
    @Select("SELECT COUNT(*) totalNum FROM " + TABLE_NAME + " log LEFT JOIN app_data." + UserMapper.TABLE_NAME
            + " `user` ON `user`.id = `log`.user_id ${where}")
    int getLogCount(@Param("where") String where);

    /**
     * 创建表
     */
    @Update("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',"
            + "`create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
            + "`user_id` bigint(20) NULL COMMENT '玩家id',`nickname` varchar(32) NULL DEFAULT NULL COMMENT '昵称',"
            + "`reason` int(11) NOT NULL COMMENT '变动来源',"
            + "`pre_diamond` bigint(20) NULL COMMENT '变动前钻石',`change_diamond` bigint(20) NULL COMMENT '变动钻石',"
            + "`pre_money` bigint(20) NULL COMMENT '变动前金币',`change_money` bigint(20) NULL COMMENT '变动金币',"
            + "`pre_lottery` bigint(20) NULL COMMENT '变动前奖券',`change_lottery` bigint(20) NULL COMMENT '变动奖券',"
            + "`pre_bank_money` bigint(20) NULL COMMENT '变动前保险箱金币',`change_bank_money` bigint(20) NULL COMMENT '变动保险箱金币',"

            + "`pre_bronze_torpedo` bigint(20) NULL COMMENT '变动前青铜鱼雷',`change_bronze_torpedo` bigint(20) NULL COMMENT '变动的青铜鱼雷数量',"
            + "`pre_silver_torpedo` bigint(20) NULL COMMENT '变动前白银鱼雷',`change_silver_torpedo` bigint(20) NULL COMMENT '变动的白银鱼雷数量',"
            + "`pre_gold_torpedo` bigint(20) NULL COMMENT '变动前黄金鱼雷',`change_gold_torpedo` bigint(20) NULL COMMENT '变动的黄金鱼雷数量',"

            + "`pre_skill_lock` bigint(20) NULL COMMENT '变动前锁定技能数量',`change_skill_lock` bigint(20) NULL COMMENT '变动的锁定技能数量',"
            + "`pre_skill_frozen` bigint(20) NULL COMMENT '变动前冰冻技能数量',`change_skill_frozen` bigint(20) NULL COMMENT '变动的冰冻技能数量',"
            + "`pre_skill_fast` bigint(20) NULL COMMENT '变动前急速技能数量',`change_skill_fast` bigint(20) NULL COMMENT '变动的急速技能数量',"
            + "`pre_skill_crit` bigint(20) NULL COMMENT '变动前暴击技能数量',`change_skill_crit` bigint(20) NULL COMMENT '变动的暴击技能数量',"
            + "`pre_boss_bugle` bigint(20) NULL COMMENT '变动前boss号角数量',`change_boss_bugle` bigint(20) NULL COMMENT '变动的boss号角数量',"

            + "PRIMARY KEY (`id`) USING BTREE) ENGINE = InnoDB AUTO_INCREMENT = 100001")
    void createTable();

}
