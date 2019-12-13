package com.maple.game.osee.listener;

import com.maple.engine.container.DataContainer;
import com.maple.game.osee.entity.fishing.csv.file.BatteryLevelConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.maple.engine.data.ServerUser;
import com.maple.engine.event.userinit.IUserInitEventListener;
import com.maple.engine.event.userinit.UserInitEvent;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.manager.PlayerManager;

/**
 * 用户初始化监听器
 */
@Component
public class OseeUserInitListener implements IUserInitEventListener {

    @Autowired
    private OseePlayerMapper playerMapper;

    @Override
    public void handleUserInitEvent(UserInitEvent event) {
        ServerUser user = event.getUser();

        if (PlayerManager.getPlayerEntity(user, false) == null) {
            OseePlayerEntity entity = playerMapper.findByUserId(user.getId());
            if (entity == null) {
                entity = new OseePlayerEntity();
                entity.setUserId(user.getId());
                entity.setMoney(1000);
                // 初始炮台等级为最低等级
                entity.setBatteryLevel(DataContainer.getData(1, BatteryLevelConfig.class).getBatteryLevel());
                playerMapper.save(entity);
            }

            user.putExpertData(OseePlayerEntity.EntityId, entity);
            PlayerManager.updateEntities.add(entity);
        }
    }

}
