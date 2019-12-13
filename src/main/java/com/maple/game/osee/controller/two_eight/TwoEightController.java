package com.maple.game.osee.controller.two_eight;


import com.google.protobuf.Message;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.entity.two_eight.TwoEightPlayer;
import com.maple.game.osee.entity.two_eight.TwoEightRoom;
import com.maple.game.osee.manager.two_eight.TwoEightManager;
import com.maple.game.osee.proto.OseeTwoEightMessage;
import com.maple.gamebase.container.GameContainer;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

/**
 * 二八杠控制类
 */
@AppController
public class TwoEightController {

    @Autowired
    private TwoEightManager twoEightManager;

    /**
     * 默认检查方法
     */
    public void checker(Method taskMethod, Message msg, ServerUser user, Long exp) throws Exception {
        if (user.getEntity() == null) {
            return;
        }
        taskMethod.invoke(this, msg, user);
    }

    /**
     * 加入二八杠房间
     */
    @AppHandler(msgCode = OseeTwoEightMessage.TwoEightMessageCode.C_S_TWOEIGHT_JOIN_ROOM_REQUEST_VALUE)
    public void doJoinTwoEightRoom(OseeTwoEightMessage.TwoEightJoinRoomRequest request, ServerUser user){
        twoEightManager.joinRoom(user);
    }

    /**
     * 退出房间
     */
    @AppHandler(msgCode = OseeTwoEightMessage.TwoEightMessageCode.C_S_TEROOM_QUIT_REQUEST_VALUE)
    public void doQuitTwoEightRoom(OseeTwoEightMessage.TERoomQuitRequest request, ServerUser user){
        twoEightManager.quitRoom(user);
    }

    /**
     * 申请上庄
     */
    @AppHandler(msgCode = OseeTwoEightMessage.TwoEightMessageCode.C_S_TEROOM_BANKER_REQUEST_VALUE)
    public void toBeBanker(OseeTwoEightMessage.RoomBankerRequest request,ServerUser user){
        twoEightManager.applyForBanker(request.getRoomCode(),user);
    }

    /**
     * 申请摇骰子
     */
    @AppHandler(msgCode=OseeTwoEightMessage.TwoEightMessageCode.C_S_SHAKE_DICE_REQUEST_VALUE)
    public void doShakeDice(OseeTwoEightMessage.ShakeDiceRequest request,ServerUser user){
        twoEightManager.shakeDice(request.getRoomCode());
    }

    /**
     * 申请下注
     */
    @AppHandler(msgCode = OseeTwoEightMessage.TwoEightMessageCode.C_S_TEROMM_BET_REQUEST_VALUE)
    public void doBet(OseeTwoEightMessage.BetInFoRequest request,ServerUser user){
        twoEightManager.betMoney(request.getBetInfoList(),user);
    }

    /**
     *申请下庄
     */
    @AppHandler(msgCode = OseeTwoEightMessage.TwoEightMessageCode.C_S_TEROOM_CANCEL_BANKER_REQUEST_VALUE)
    public void cancelBanker(OseeTwoEightMessage.TERoomCancelBankerRequest request,ServerUser user){
        twoEightManager.cancelBanker(user);
    }


    /**
     * 玩家 重连
     */
    @AppHandler(msgCode = OseeTwoEightMessage.TwoEightMessageCode.C_S_TEROOM_PLAYER_RECONNECT_INFO_REQUEST_VALUE)
    public void cancelBanker(OseeTwoEightMessage.TERoomReconnectRequest request,ServerUser user){
        TwoEightPlayer player = GameContainer.getPlayerById(user.getId());
        TwoEightRoom room = GameContainer.getGameRoomByCode(player.getRoomCode());
        twoEightManager.reconnect(room,player);
    }


}
