package com.maple.game.osee.timer.two_eight;

import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.entity.two_eight.TwoEightRoom;
import com.maple.game.osee.manager.two_eight.TwoEightManager;
import com.maple.gamebase.container.GameContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 检查房间玩家在线状态的定时任务
 */
@Component
public class TERoomCheckTask {

    /**
     * 线程池
     */
    private static final ScheduledExecutorService EXECUTOR = ThreadPoolUtils.createThreadPool(5);

    @Autowired
    private TwoEightManager twoEightManager;

    /**
     * 定时检查房间状态
     *
     */
    @Scheduled(fixedRate = 500)
    public void check() {
        List<TwoEightRoom> rooms = GameContainer.getGameRooms(TwoEightRoom.class);
        // 遍历所有房间
        for (TwoEightRoom room : rooms) {
            if (room == null) {
                continue;
            }

            EXECUTOR.execute(() -> twoEightManager.roomClock(room));
        }
    }
}
