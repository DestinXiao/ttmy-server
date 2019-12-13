package com.maple.game.osee.controller.fishing;

import com.google.protobuf.Message;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.FishingGamePlayer;
import com.maple.game.osee.entity.fishing.challenge.FishingChallengePlayer;
import com.maple.game.osee.entity.fishing.challenge.FishingChallengeRoom;
import com.maple.game.osee.entity.fishing.game.FireStruct;
import com.maple.game.osee.manager.fishing.FishingChallengeManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.fishing.OseeFishingMessage;
import com.maple.game.osee.proto.fishing.TtmyFishingChallengeMessage;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGameRoom;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

/**
 * 捕鱼挑战赛控制层
 */
@AppController
public class FishingChallengeController {

    @Autowired
    private FishingChallengeManager challengeManager;



    /**
     * 默认检查器
     */
    public void checker(Method taskMethod, Message req, ServerUser user, Long exp) {
        try {
            BaseGameRoom gameRoom = GameContainer.getGameRoomByPlayerId(user.getId());
            // 根据任务方法设定的exp值，判断是否需要玩家在房间中操作
            if (exp == 0) { // 不在房间内的操作，同时玩家不在任何房间内
                if (gameRoom != null) {
                    return;
                }
                taskMethod.invoke(this, req, user);
            } else if (exp == 1) { // 房间内的操作
                if (!(gameRoom instanceof FishingChallengeRoom)) {
                    return;
                }
                FishingChallengePlayer player = gameRoom.getGamePlayerById(user.getId());
                taskMethod.invoke(this, req, player, gameRoom);
            } else {
                taskMethod.invoke(this, req, user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 挑战赛房间列表
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_ROOM_LIST_REQUEST_VALUE, exp = -1)
    public void roomList(TtmyFishingChallengeMessage.FishingChallengeRoomListRequest request, ServerUser user) {
        System.out.println(request.getRoomType());
        challengeManager.roomList(user, request.getRoomType());
    }

    /**
     * 创建房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_CREATE_ROOM_REQUEST_VALUE, exp = 0)
    public void createRoom(TtmyFishingChallengeMessage.FishingChallengeCreateRoomRequest request, ServerUser user) {
        challengeManager.createRoom(user, request.getRoomPassword());
    }

    /**
     * 加入房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_JOIN_ROOM_REQUEST_VALUE, exp = 0)
    public void joinRoom(TtmyFishingChallengeMessage.FishingChallengeJoinRoomRequest request, ServerUser user) {
        challengeManager.joinRoom(user, request.getRoomCode(), request.getRoomPassword(), request.getRoomType());
    }

    /**
     * 退出房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_EXIT_ROOM_REQUEST_VALUE, exp = 1)
    public void exitRoom(TtmyFishingChallengeMessage.FishingChallengeExitRoomRequest request, FishingChallengePlayer player, FishingChallengeRoom room) {
        challengeManager.exitRoom(player, room);
    }

    /**
     * 快速自动加入房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_QUICK_JOIN_REQUEST_VALUE, exp = 0)
    public void quickJoin(TtmyFishingChallengeMessage.FishingChallengeQuickJoinRequest request, ServerUser user) {
        challengeManager.quickJoin(user);
    }

    /**
     * 捕鱼改变炮台外观任务
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_CHANGE_BATTERY_VIEW_REQUEST_VALUE, exp = 1)
    public void changeBatteryView(TtmyFishingChallengeMessage.FishingChallengeChangeBatteryViewRequest request,
                                  FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
        challengeManager.changeBatteryView(gameRoom, player, request.getTargetViewIndex());
    }

    /**
     * 捕鱼改变炮台等级任务
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_CHANGE_BATTERY_LEVEL_REQUEST_VALUE, exp = 1)
    public void changeBatteryLevel(TtmyFishingChallengeMessage.FishingChallengeChangeBatteryLevelRequest request,
                                   FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
        challengeManager.changeBatteryLevel(gameRoom, player, request.getTargetLevel());
    }

    /**
     * 玩家发射子弹
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_FIRE_REQUEST_VALUE, exp = 1)
    public void playerFire(TtmyFishingChallengeMessage.FishingChallengeFireRequest request,
                           FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
        FireStruct fire = new FireStruct();
        fire.setId(request.getFireId() > 0 ? request.getFireId() : gameRoom.getNextId());
        fire.setFishId(request.getFishId());
        fire.setAngle(request.getAngle());
        challengeManager.playerFire(gameRoom, player, fire);
    }

    /**
     * 玩家打中鱼请求
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_FIGHT_FISH_REQUEST_VALUE, exp = 1)
    public void playerFightFish(TtmyFishingChallengeMessage.FishingChallengeFightFishRequest request,
                                FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
        challengeManager.playerFightFish(gameRoom, player, request.getFireId(), request.getFishIdList(), false);
    }

    /**
     * 同步房间内的鱼
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_SYNCHRONISE_REQUEST_VALUE, exp = 1)
    public void fishSynchronise(TtmyFishingChallengeMessage.FishingChallengeSynchroniseRequest request,
                                FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
        challengeManager.sendSynchroniseResponse(gameRoom, player);
    }

    /**
     * 捕鱼重新激活
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_REACTIVE_REQUEST_VALUE, exp = 1)
    public void fishReactive(TtmyFishingChallengeMessage.FishingChallengeReactiveRequest request,
                             FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
        challengeManager.sendReactiveMessage(gameRoom, player);
    }

    /**
     * 玩家使用技能
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_USE_SKILL_REQUEST_VALUE, exp = 1)
    public void useSkill(TtmyFishingChallengeMessage.FishingChallengeUseSkillRequest request,
                         FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
        challengeManager.useSkill(gameRoom, player, request.getSkillId());
    }

    /**
     * 捕捉到特殊鱼
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_CATCH_SPECIAL_FISH_REQUEST_VALUE, exp = 1)
    public void catchSpecialFish(TtmyFishingChallengeMessage.FishingChallengeCatchSpecialFishRequest request,
                                 FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
        challengeManager.catchSpecialFish(gameRoom, request.getPlayerId(), request.getFishIdsList(), request.getSpecialFishId());
    }

    /**
     * 使用boss号角
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_USE_BOSS_BUGLE_REQUEST_VALUE, exp = 1)
    public void useBossBugle(TtmyFishingChallengeMessage.FishingChallengeUseBossBugleRequest request,
                             FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
        challengeManager.useBossBugle(gameRoom, player);
    }

    /**
     * VIP换座
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_CHANGE_SEAT_REQUEST_VALUE, exp = 1)
    public void changeSeat(TtmyFishingChallengeMessage.FishingChallengeChangeSeatRequest request,
                           FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
        challengeManager.changeSeat(gameRoom, player, request.getSeat());
    }

    /**
     * 使用鱼雷任务
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_Challenge_USE_TORPEDO_REQUEST_VALUE, exp = 1)
    public void doUseTorpedoTask(OseeFishingMessage.FishingUseTorpedoRequest request,
                                 FishingGamePlayer player, FishingChallengeRoom gameRoom) {
        if (request.getTorpedoId() == ItemId.GOLD_TORPEDO.getId())
            challengeManager.useTorpedo(gameRoom, player, request.getTorpedoId(), request.getTorpedoNum(), request.getAngle());
    }

}
