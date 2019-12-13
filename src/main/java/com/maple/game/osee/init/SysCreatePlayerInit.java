package com.maple.game.osee.init;

import com.maple.database.data.entity.UserEntity;
import com.maple.database.data.mapper.UserMapper;
import com.maple.engine.anotation.AppInit;
import com.maple.engine.data.ServerUser;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.manager.BaseRobotManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 系统自动创建玩家初始化类
 *
 * @author Junlong
 */
@AppInit
public class SysCreatePlayerInit {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserMapper userMapper;

    private final OseePlayerMapper playerMapper;

    /**
     * 需要创建的玩家数量，需要新添多少个就直接加在后面，看着清晰一些
     */
    private static final int MAX_CREATE_PLAYER_NUM = 5000 + 2200 + 3000 + 2360 + 1200;

//    private static final String REDIS_KEY = "Ttmy:Sys:CreatePlayerIDs";

    @Autowired
    public SysCreatePlayerInit(UserMapper userMapper, OseePlayerMapper playerMapper) {
        this.userMapper = userMapper;
        this.playerMapper = playerMapper;
    }

    public void init() {
        // 延迟执行，避免数据加载还未完成
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(this::createPlayers, 5, TimeUnit.SECONDS);
    }

    private void createPlayers() {
        long userCount = userMapper.getUserCount();
        if (userCount < MAX_CREATE_PLAYER_NUM) { // 玩家数量少于这个数量就要系统自动创建一些用户
            logger.info("天天摸鱼系统开始自动创建新用户！");
            long createNum = MAX_CREATE_PLAYER_NUM - userCount;
            for (int i = 0; i < createNum; i++) {
                ServerUser user = new ServerUser();
                user.setEntity(new UserEntity());
                // 设置玩家信息
                String robotName = BaseRobotManager.getRobotName();
                user.getEntity().setUsername(robotName + "_" + System.currentTimeMillis() % 10000000);
                user.getEntity().setNickname(robotName);
                user.getEntity().setSex(ThreadLocalRandom.current().nextInt(0, 2));
                user.getEntity().setHeadIndex(ThreadLocalRandom.current().nextInt(1, 22));
                userMapper.save(user.getEntity());
                // 设置玩家游戏数据
                OseePlayerEntity entity = new OseePlayerEntity();
                entity.setUserId(user.getId());
                entity.setMoney(1000);
                playerMapper.save(entity);
//                user.putExpertData(OseePlayerEntity.EntityId, entity);
            }
            logger.info("天天摸鱼系统本次成功自动创建了[{}]个用户！", createNum);
        }
    }
}
