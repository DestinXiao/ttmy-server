package com.maple.game.osee.controller.fishing;

import com.google.protobuf.Message;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.entity.fishing.game.FireStruct;
import com.maple.game.osee.entity.fishing.grandprix.FishingGrandPrixPlayer;
import com.maple.game.osee.entity.fishing.grandprix.FishingGrandPrixRoom;
import com.maple.game.osee.manager.fishing.FishingGrandPrixManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.TtmyFishingGrandPrixMessage;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGameRoom;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;


@AppController
public class FishingGrandPrixController {

    @Autowired
    FishingGrandPrixManager fishingGrandPrixManager;

    /**
     * 默认检查器
     */
    public void checker(Method taskMethod, Message req, ServerUser user, Long exp) {
        BaseGameRoom gameRoom = GameContainer.getGameRoomByPlayerId(user.getId());
        try {
            if (exp == 0) { // 根据任务方法设定的exp值，判断是否需要玩家在房间中操作
                if (gameRoom != null) {
                    return;
                }
                taskMethod.invoke(this, req, user);
            } else if (exp == 1) {
                if (!(gameRoom instanceof FishingGrandPrixRoom)) {
                    return;
                }
                FishingGrandPrixPlayer player = gameRoom.getGamePlayerById(user.getId());
                taskMethod.invoke(this, req, player, gameRoom);
            } else {
                taskMethod.invoke(this, req, user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查大奖赛是否开始
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_GRAND_PRIX_START_REQUEST_VALUE, exp = -1)
    public void start(TtmyFishingGrandPrixMessage.FishingGrandPrixStartRequest request, ServerUser serverUser) {
        fishingGrandPrixManager.start(request.getPlayerId(), serverUser);
    }

    /**
     * 获取玩家（日 或 周）排行榜
     * @param request
     * @param serverUser
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FINSHING_GRAND_PRIX_RANK_REQUEST_VALUE, exp = 0)
    public void rank(TtmyFishingGrandPrixMessage.FishingGrandPrixRankRequest request, ServerUser serverUser) {
        fishingGrandPrixManager.rank(request.getRankType(), request.getPageCurrent(), request.getPageSize(), request.getTotal(),  serverUser);
    }

    /**
     * 获取玩家大奖赛信息
     * @param request
     * @param serverUser
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FINSHING_GRAND_PRIX_PLAYER_INFO_REQUEST_VALUE, exp = 0)
    public void getPlayerInfo(TtmyFishingGrandPrixMessage.FishingGrandPrixPlayerInfoRequest request, ServerUser serverUser) {
        fishingGrandPrixManager.getPlayerInfo(request.getPlayerId(), serverUser);
    }


    /**
     * 玩家加入捕鱼大奖赛
     * @param request
     * @param user
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_GRAND_PRIX_JOIN_ROOM_REQUEST_VALUE, exp = 0)
    public void joinRoom(TtmyFishingGrandPrixMessage.FishingGrandPrixJoinRoomRequest request, ServerUser user) {
        fishingGrandPrixManager.joinRoom(user);
    }

    /**
     * 退出房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FINSHING_GRAND_PRIX_QUIT_REQUEST_VALUE, exp = 1)
    public void exitRoom(TtmyFishingGrandPrixMessage.FishingGrandPrixQuitRequest request, FishingGrandPrixPlayer player, FishingGrandPrixRoom room) {
        fishingGrandPrixManager.exitRoom(player, room);
    }

    /**
     * 玩家发射子弹
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_GRAND_PRIX_FIRE_REQUEST_VALUE, exp = 1)
    public void playerFire(TtmyFishingGrandPrixMessage.FishingGrandPrixFireRequest request,
                           FishingGrandPrixPlayer player, FishingGrandPrixRoom gameRoom) {
        FireStruct fire = new FireStruct();
        fire.setId(request.getFireId() > 0 ? request.getFireId() : gameRoom.getNextId());
        fire.setFishId(request.getFishId());
        fire.setAngle(request.getAngle());
        fishingGrandPrixManager.playerFire(gameRoom, player, fire);
    }

    /**
     * 玩家打中鱼请求
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_GRAND_PRIX_FIGHT_FISH_REQUEST_VALUE, exp = 1)
    public void playerFightFish(TtmyFishingGrandPrixMessage.FishingGrandPrixFightFishRequest request,
                                FishingGrandPrixPlayer player, FishingGrandPrixRoom gameRoom) {
        fishingGrandPrixManager.playerFightFish(gameRoom, player, request.getFireId(), request.getFishIdList(), false);
    }


    /**
     * 同步房间内的鱼
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_GRAND_PRIX_SYNCHRONISE_REQUEST_VALUE, exp = 1)
    public void fishSynchronise(TtmyFishingGrandPrixMessage.FishingGrandPrixSynchroniseRequest request,
                                FishingGrandPrixPlayer player, FishingGrandPrixRoom gameRoom) {
        fishingGrandPrixManager.sendSynchroniseResponse(gameRoom, player);
    }

    /**
     * 捕鱼重新激活
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_GRAND_PRIX_REACTIVE_REQUEST_VALUE, exp = 1)
    public void fishReactive(TtmyFishingGrandPrixMessage.FishingGrandPrixReactiveRequest request,
                             FishingGrandPrixPlayer player, FishingGrandPrixRoom gameRoom) {
        fishingGrandPrixManager.sendReactiveMessage(gameRoom, player);
    }

    /**
     * 玩家使用技能
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_GRAND_PRIX_USE_SKILL_REQUEST_VALUE, exp = 1)
    public void useSkill(TtmyFishingGrandPrixMessage.FishingGrandPrixUseSkillRequest request,
                         FishingGrandPrixPlayer player, FishingGrandPrixRoom gameRoom) {
        fishingGrandPrixManager.useSkill(gameRoom, player, request.getSkillId());
    }
//
//    /**
//     * 捕捉到特殊鱼
//     */
//    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_CHALLENGE_CATCH_SPECIAL_FISH_REQUEST_VALUE, exp = 1)
//    public void catchSpecialFish(TtmyFishingChallengeMessage.FishingChallengeCatchSpecialFishRequest request,
//                                 FishingChallengePlayer player, FishingChallengeRoom gameRoom) {
//        challengeManager.catchSpecialFish(gameRoom, request.getPlayerId(), request.getFishIdsList(), request.getSpecialFishId());
//    }

    /**
     * 使用boss号角
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_GRAND_PRIX_USE_BOSS_BUGLE_REQUEST_VALUE, exp = 1)
    public void useBossBugle(TtmyFishingGrandPrixMessage.FishingGrandPrixUseBossBugleRequest request,
                             FishingGrandPrixPlayer player, FishingGrandPrixRoom gameRoom) {
        fishingGrandPrixManager.useBossBugle(gameRoom, player);
    }

    /**
     * 捕鱼改变炮台外观任务
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_GRAND_PRIX_CHANGE_BATTERY_VIEW_REQUEST_VALUE, exp = 1)
    public void changeBatteryView(TtmyFishingGrandPrixMessage.FishingGrandPrixChangeBatteryViewRequest request,
                                  FishingGrandPrixPlayer player, FishingGrandPrixRoom gameRoom) {
        fishingGrandPrixManager.changeBatteryView(gameRoom, player, request.getTargetViewIndex());
    }

    /**
     * 捕鱼改变炮台等级任务
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_FISHING_GRAND_PRIX_CHANGE_BATTERY_LEVEL_REQUEST_VALUE, exp = 1)
    public void changeBatteryLevel(TtmyFishingGrandPrixMessage.FishingGrandPrixChangeBatteryLevelRequest request,
                                   FishingGrandPrixPlayer player, FishingGrandPrixRoom gameRoom) {
        fishingGrandPrixManager.changeBatteryLevel(gameRoom, player, request.getTargetLevel());
    }
}
