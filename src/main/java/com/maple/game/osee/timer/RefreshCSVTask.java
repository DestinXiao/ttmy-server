package com.maple.game.osee.timer;

import com.maple.engine.container.DataContainer;
import com.maple.game.osee.entity.fishing.csv.file.*;
import com.maple.game.osee.entity.lobby.csv.RobotNameConfig;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 自动定时刷新配置文件
 *
 * @author Junlong
 */
@Component
public class RefreshCSVTask {

    /**
     * 定时自动重新加载配置文件
     * <p>
     * 首次延迟10秒后每5秒执行一次
     */
    @Scheduled(initialDelay = 10000, fixedRate = 5000)
    public void refreshTimer() {
        // 捕鱼配置
        DataContainer.dataInit(FishConfig.class);
        DataContainer.dataInit(FishGroupConfig.class);
        DataContainer.dataInit(FishRefreshRule.class);
        DataContainer.dataInit(FishRouteConfig.class);

        // 机器人姓名配置
        DataContainer.dataInit(RobotNameConfig.class);
    }
}
