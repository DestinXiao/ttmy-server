package com.maple.game.osee.timer.fightten;

import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.entity.fightten.FightTenRoom;
import com.maple.game.osee.manager.fightten.FightTenManager;
import com.maple.gamebase.container.GameContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 拼十房间定时检测任务
 */
@Component
public class RoomCheckTask {

    @Autowired
    private FightTenManager fightTenManager;

    /**
     * 房间检测，状态控制
     * <p>
     * 每300ms运行一次
     */
    @Scheduled(fixedRate = 300)
    public void stateCheck() {
        List<FightTenRoom> fightTenRooms = GameContainer.getGameRooms(FightTenRoom.class);
        for (FightTenRoom fightTenRoom : fightTenRooms) {
            if (fightTenRoom == null) {
                continue;
            }
            ThreadPoolUtils.TASK_SERVICE_POOL.execute(() -> fightTenManager.dealRoomState(fightTenRoom));
        }
    }
}
