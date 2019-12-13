package com.maple.game.osee.controller.fishing;

import com.google.protobuf.Message;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.FishingGamePlayer;
import com.maple.game.osee.entity.fishing.FishingGameRoom;
import com.maple.game.osee.entity.fishing.challenge.FishingChallengeRoom;
import com.maple.game.osee.entity.fishing.game.FireStruct;
import com.maple.game.osee.entity.fishing.task.TaskType;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.manager.fishing.FishingManager;
import com.maple.game.osee.manager.fishing.FishingRobotManager;
import com.maple.game.osee.manager.fishing.FishingTaskManager;
import com.maple.game.osee.manager.fishing.TreasureManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.fishing.OseeFishingMessage;
import com.maple.game.osee.proto.fishing.OseeFishingMessage.*;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.data.BaseGameRoom;
import com.maple.network.manager.NetManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

/**
 * 捕鱼控制器
 */
@AppController
public class FishingController {

    @Autowired
    private FishingManager fishingManager;

    @Autowired
    private TreasureManager treasureManager;

    @Autowired
    private FishingRobotManager fishingRobotManager;

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
                if (!(gameRoom instanceof FishingGameRoom)) {
                    return;
                }
                FishingGamePlayer player = gameRoom.getGamePlayerById(user.getId());
                taskMethod.invoke(this, req, player, gameRoom);
            } else {
                taskMethod.invoke(this, req, user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 捕鱼加入房间任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_JOIN_ROOM_REQUEST_VALUE)
    public void doFishingJoinRoomTask(FishingJoinRoomRequest req, ServerUser user) {
        fishingManager.playerJoinRoom(user, req.getRoomIndex());
    }

    /**
     * 捕鱼玩家信息任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_PLAYER_INFO_REQUEST_VALUE, exp = 1)
    public void doFishingPlayerInfoTask(FishingPlayerInfoRequest req, FishingGamePlayer player,
                                        FishingGameRoom gameRoom) {
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_FISHING_PLAYER_INFO_RESPONSE_VALUE, FishingManager.createPlayerInfoResponse(gameRoom, player), player.getUser());
    }

    /**
     * 捕鱼玩家列表信息任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_PLAYERS_INFO_REQUEST_VALUE, exp = 1)
    public void doFishingPlayersInfoTask(FishingPlayersInfoRequest req, FishingGamePlayer player,
                                         FishingGameRoom gameRoom) {
        fishingManager.sendPlayersInfoResponse(gameRoom, player.getUser());
    }

    /**
     * 捕鱼退出房间任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_EXIT_ROOM_REQUEST_VALUE, exp = 1)
    public void doFishingExitRoomTask(FishingExitRoomRequest req, FishingGamePlayer player, FishingGameRoom gameRoom) {
        fishingManager.exitFishingRoom(gameRoom, player.getUser());
    }

    /**
     * 捕鱼获取宝藏任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_GET_TREASURE_REQUEST_VALUE, exp = -1)
    public void doFishingGetTreasureTask(FishingGetTreasureRequest req, ServerUser user) {
        treasureManager.drawTreasure(user, req.getIndex(), req.getDrawIndex());
    }

    /**
     * 捕鱼获取房间任务列表任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_ROOM_TASK_LIST_REQUEST_VALUE, exp = -1)
    public void doFishingTaskListTask(FishingRoomTaskListRequest req, ServerUser user) {
        FishingTaskManager.sendRoomTaskListResponse(user);
    }

    /**
     * 捕鱼获取任务奖励任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_GET_ROOM_TASK_REWARD_REQUEST_VALUE, exp = -1)
    public void doFishingGetTaskRewardTask(FishingGetRoomTaskRewardRequest req, ServerUser user) {
        FishingTaskManager.getTaskReward(user, TaskType.ROOM, req.getTaskId());
    }

    /**
     * 捕鱼改变炮台外观任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_CHANGE_BATTERY_VIEW_REQUEST_VALUE, exp = 1)
    public void doFishingChangeBatteryViewTask(FishingChangeBatteryViewRequest req, FishingGamePlayer player,
                                               FishingGameRoom gameRoom) {
        int viewIndex = req.getTargetViewIndex();
        if (viewIndex >= ItemId.QSZS_BATTERY_VIEW.getId() && viewIndex <= ItemId.SWHP_BATTERY_VIEW.getId()) { // 切换到自己购买的炮台外观
            if (PlayerManager.getItemNum(player.getUser(), ItemId.getItemIdById(viewIndex)) <= 0) {
                NetManager.sendHintMessageToClient("该炮台外观已到期", player.getUser());
                return;
            }
            player.setViewIndex(viewIndex);
            FishingChangeBatteryViewResponse.Builder builder = FishingChangeBatteryViewResponse.newBuilder();
            builder.setPlayerId(player.getId());
            builder.setViewIndex(player.getViewIndex());
            FishingManager.sendRoomMessage(gameRoom, OseeMsgCode.S_C_OSEE_FISHING_CHANGE_BATTERY_VIEW_RESPONSE_VALUE, builder.build());
        } else if (PlayerManager.getPlayerVipLevel(player.getUser()) >= viewIndex) {
            player.setViewIndex(viewIndex);
            FishingChangeBatteryViewResponse.Builder builder = FishingChangeBatteryViewResponse.newBuilder();
            builder.setPlayerId(player.getId());
            builder.setViewIndex(player.getViewIndex());
            int msgCode = OseeMsgCode.S_C_OSEE_FISHING_CHANGE_BATTERY_VIEW_RESPONSE_VALUE;
            FishingManager.sendRoomMessage(gameRoom, msgCode, builder.build());
        } else {
            NetManager.sendHintMessageToClient("您的vip等级不足，无法更改该炮台外观", player.getUser());
        }
    }

    /**
     * 捕鱼改变炮台等级任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_CHANGE_BATTERY_LEVEL_REQUEST_VALUE, exp = 1)
    public void doFishingChangeBatteryLevelTask(FishingChangeBatteryLevelRequest req, FishingGamePlayer player,
                                                FishingGameRoom gameRoom) {
        int[] roomBatteryLimit = FishingManager.batteryLevelLimit[gameRoom.getRoomIndex() - 1];
        if (req.getTargetLevel() < roomBatteryLimit[0] || req.getTargetLevel() > roomBatteryLimit[1] ||
                req.getTargetLevel() > PlayerManager.getPlayerBatteryLevel(player.getUser())) {
            return;
        }

        player.setBatteryLevel(req.getTargetLevel());
        FishingChangeBatteryLevelResponse.Builder builder = FishingChangeBatteryLevelResponse.newBuilder();
        builder.setPlayerId(player.getId());
        builder.setLevel(player.getBatteryLevel());

        int msgCode = OseeMsgCode.S_C_OSEE_FISHING_CHANGE_BATTERY_LEVEL_RESPONSE_VALUE;
        FishingManager.sendRoomMessage(gameRoom, msgCode, builder.build());
    }

    /**
     * 捕鱼改变炮台倍数任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_CHANGE_BATTERY_MULT_REQUEST_VALUE, exp = 1)
    public void doFishingChangeBatteryMultTask(FishingChangeBatteryMultRequest req, FishingGamePlayer player,
                                               FishingGameRoom gameRoom) {
        player.setBatteryLevel(req.getTargetMult());
        FishingChangeBatteryMultResponse.Builder builder = FishingChangeBatteryMultResponse.newBuilder();
        builder.setPlayerId(player.getId());
        builder.setMult(player.getBatteryLevel());

        int msgCode = OseeMsgCode.S_C_OSEE_FISHING_CHANGE_BATTERY_MULT_RESPONSE_VALUE;
        FishingManager.sendRoomMessage(gameRoom, msgCode, builder.build());
    }

    /**
     * 捕鱼发射子弹任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_FIRE_REQUEST_VALUE, exp = 1)
    public void doFishingFireTask(FishingFireRequest req, FishingGamePlayer player, FishingGameRoom gameRoom) {
        FireStruct fire = new FireStruct();
        fire.setId(req.getFireId() > 0 ? req.getFireId() : gameRoom.getNextId());
        fire.setFishId(req.getFishId());
        fire.setAngle(req.getAngle());
        fishingManager.playerFire(gameRoom, player, fire);
    }

    /**
     * 捕鱼击中鱼类任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_FIGHT_FISH_REQUEST_VALUE, exp = 1)
    public void doFishingFightFishTask(FishingFightFishRequest req, FishingGamePlayer player,
                                       FishingGameRoom gameRoom) {
        fishingManager.playerFightFish(gameRoom, player, req.getFireId(), req.getFishIdList(), false);
    }

    /**
     * 捕鱼同步任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_SYNCHRONISE_REQUEST_VALUE, exp = 1)
    public void doFishingSynchroniseTask(FishingSynchroniseRequest req, FishingGamePlayer player,
                                         FishingGameRoom gameRoom) {
        fishingManager.sendSynchroniseResponse(gameRoom, player.getUser());
    }

    /**
     * 捕鱼重新激活任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_REACTIVE_REQUEST_VALUE, exp = 1)
    public void doFishingReactiveTask(FishingReactiveRequest req, FishingGamePlayer player, FishingGameRoom gameRoom) {
        fishingManager.sendReactiveMessage(gameRoom, player.getUser());
    }

    /**
     * 捕鱼机器人击中鱼类任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_OSEE_FISHING_ROBOT_FIGHT_FISH_REQUEST_VALUE, exp = 1)
    public void doFishingRobotFireTask(FishingRobotFightFishRequest req, FishingGamePlayer player,
                                       FishingGameRoom gameRoom) {
        fishingRobotManager.robotFightFish(gameRoom, req.getRobotId(), req.getFireId(), req.getFishIdList(), false);
    }

    /**
     * 使用鱼雷任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_TTMY_FISHING_USE_TORPEDO_REQUEST_VALUE, exp = 1)
    public void doUseTorpedoTask(OseeFishingMessage.FishingUseTorpedoRequest request,
                                 FishingGamePlayer player, FishingGameRoom gameRoom) {
            if (request.getTorpedoId() == ItemId.DRAGON_CRYSTAL.getId())
            fishingManager.useDragonCrystal(gameRoom, player, request.getTorpedoId(), request.getTorpedoNum(), request.getAngle());
    }




    /**
     * 玩家使用技能任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_TTMY_FISHING_USE_SKILL_REQUEST_VALUE, exp = 1)
    public void doUseSkillTask(OseeFishingMessage.FishingUseSkillRequest request,
                               FishingGamePlayer player, FishingGameRoom gameRoom) {
        fishingManager.useSkill(gameRoom, player, request.getSkillId());
    }

    /**
     * 解锁炮台提示信息
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_TTMY_UNLOCK_BATTERY_LEVEL_HINT_REQUEST_VALUE, exp = -1)
    public void doUnlockBatteryLevelHintTask(OseeFishingMessage.UnlockBatteryLevelHintRequest request, ServerUser user) {
        fishingManager.unlockBatteryLevelHint(user, -1);
    }

    /**
     * 玩家解锁炮台等级任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_TTMY_UNLOCK_BATTERY_LEVEL_REQUEST_VALUE, exp = -1)
    public void doUnlockBatteryLevelTask(OseeFishingMessage.UnlockBatteryLevelRequest request, ServerUser user) {
        fishingManager.unlockBatteryLevel(user, request.getLevel());
    }

    /**
     * 捕捉特殊鱼任务
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_TTMY_CATCH_SPECIAL_FISH_REQUEST_VALUE, exp = 1)
    public void doCatchSpecialFishTask(OseeFishingMessage.CatchSpecialFishRequest request,
                                       FishingGamePlayer player, FishingGameRoom gameRoom) {
        fishingManager.catchSpecialFish(gameRoom, request.getPlayerId(), request.getFishIdsList(), request.getSpecialFishId());
    }

    /**
     * 快速开始
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_TTMY_QUICK_START_REQUEST_VALUE, exp = -1)
    public void doQuickStartTask(OseeFishingMessage.QuickStartRequest request, ServerUser user) {
        fishingManager.quickStart(user);
    }

    /**
     * 获取捕鱼场次信息
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_TTMY_FISHING_GET_FIELD_INFO_REQUEST_VALUE, exp = -1)
    public void doGetFieldInfoTask(FishingGetFieldInfoRequest request, ServerUser user) {
        fishingManager.getFieldInfo(user);
    }

    /**
     * 使用boss号角
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_TTMY_USE_BOSS_BUGLE_REQUEST_VALUE, exp = 1)
    public void doUseBossBugleTask(UseBossBugleRequest request, FishingGamePlayer player, FishingGameRoom gameRoom) {
        fishingManager.useBossBugle(gameRoom, player);
    }

    /**
     * 获取玩家是否在捕鱼房间内
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_TTMY_IS_IN_FISHING_ROOM_REQUEST_VALUE, exp = -1)
    public void doIsInFishingRoomTask(IsInFishingRoomRequest request, ServerUser user) {
        IsInFishingRoomResponse.Builder builder = IsInFishingRoomResponse.newBuilder();
        BaseGamePlayer gamePlayer = GameContainer.getPlayerById(user.getId());
        if (gamePlayer == null) {
            builder.setIn(false);
        } else {
            BaseGameRoom gameRoom = GameContainer.getGameRoomByCode(gamePlayer.getRoomCode());
            if ((gameRoom instanceof FishingGameRoom)
                    || (gameRoom instanceof FishingChallengeRoom)) {
                // 在捕鱼房间或者在龙晶战场捕鱼房间
                builder.setIn(true);
            } else {
                builder.setIn(false);
            }
        }
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_IS_IN_FISHING_ROOM_RESPONSE_VALUE, builder, user);
    }
}
