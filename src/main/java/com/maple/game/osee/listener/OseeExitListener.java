package com.maple.game.osee.listener;

import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.entity.fishing.FishingGameRoom;
import com.maple.game.osee.entity.gobang.GobangGameRoom;
import com.maple.game.osee.entity.two_eight.TwoEightPlayer;
import com.maple.game.osee.entity.two_eight.TwoEightRoom;
import com.maple.game.osee.manager.fruitlaba.FruitLaBaManager;
import com.maple.game.osee.manager.gobang.GobangManager;
import com.maple.game.osee.manager.two_eight.TwoEightManager;
import com.maple.game.osee.manager.two_eight.TwoEightRoomState;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGameRoom;
import com.maple.network.event.exit.ExitEvent;
import com.maple.network.event.exit.IExitEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 1688玩家退出监听器
 */
@Component
public class OseeExitListener implements IExitEventListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private GobangManager gobangManager;
    @Autowired
    private FruitLaBaManager fruitLaBaManager;

//    @Autowired
//    private FishingManager fishingManager;

    @Autowired
    private TwoEightManager twoEightManager;

    @Override
    public void handleExitEvent(ExitEvent event) {
        ServerUser user = event.getUser();

        logger.info("天天摸鱼:玩家[{}:{}]退出", user.getId(), user.getNickname());

        BaseGameRoom gameRoom = GameContainer.getGameRoomByPlayerId(user.getId());
        if (gameRoom instanceof FishingGameRoom) {
//            fishingManager.exitFishingRoom((FishingGameRoom) gameRoom, user);
        } else if (gameRoom instanceof GobangGameRoom) {
            gobangManager.exitGameRoom((GobangGameRoom) gameRoom, user);
        } else if (gameRoom instanceof TwoEightRoom) {
            TwoEightPlayer twoEightPlayer = GameContainer.getPlayerById(user.getId());
            TwoEightRoom room = (TwoEightRoom) gameRoom;
            //游戏未开始时可以离开
            if (room.getRoomStatus() == TwoEightRoomState.NOTBEGIN) {
                twoEightManager.quitRoom(user);
            }
            //玩家不是庄家且玩家没有下注时可以退出房间
            if (!room.getApplyForBanker().contains(twoEightPlayer) && !room.getBetPlayers().contains(twoEightPlayer)) {
                twoEightManager.quitRoom(user);
            } else {
                logger.info("玩家[{}]强制退出", twoEightPlayer.getId());
                twoEightPlayer.setInRoom(false);
            }
        } else if(gameRoom == null){
                fruitLaBaManager.playerLeaveRoom(user);
                RedisHelper.set(FruitLaBaManager.FruitDrawSign + user.getId(), "0");
            }
        }
    }

