package com.maple.game.osee.controller.fightten;

import com.google.protobuf.Message;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.manager.fightten.FightTenTaskManager;
import com.maple.game.osee.manager.fightten.FightTenManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.fightten.OseeFightTenMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

/**
 * 拼十控制层
 */
@AppController
public class FightTenController {

    @Autowired
    private FightTenManager fightTenManager;

    @Autowired
    private FightTenTaskManager taskManager;

    /**
     * 默认检查器
     */
    public void checker(Method taskMethod, Message req, ServerUser user, Long exp) throws Exception {
        taskMethod.invoke(this, req, user);
    }

    /**
     * 获取拼十的所有场次信息
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_GET_FIELD_LIST_REQUEST_VALUE)
    public void fieldList(OseeFightTenMessage.TenGetFieldListRequest request, ServerUser user) {
        fightTenManager.sendFieldList(user);
    }

    /**
     * 加入拼十房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_JOIN_ROOM_REQUEST_VALUE)
    public void joinRoom(OseeFightTenMessage.TenJoinRoomRequest request, ServerUser user) {
        fightTenManager.joinRoom(request.getFieldType(), user, 0);
    }

    /**
     * 请求准备
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_READY_ROOM_REQUEST_VALUE)
    public void readyRoom(OseeFightTenMessage.TenReadyRoomRequest request, ServerUser user) {
        fightTenManager.readyRoom(request.getReadyType(), user);
    }

    /**
     * 抢庄
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_FIGHT_BANKER_REQUEST_VALUE)
    public void fightBanker(OseeFightTenMessage.TenFightBankerRequest request, ServerUser user) {
        fightTenManager.fightBanker(request.getFightMultiple(), user);
    }

    /**
     * 下注
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_BET_MONEY_REQUEST_VALUE)
    public void betMoney(OseeFightTenMessage.TenBetMoneyRequest request, ServerUser user) {
        fightTenManager.betMoney(request.getBetMoneyIndex(), user);
    }

    /**
     * 看牌
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_SEE_CARD_REQUEST_VALUE)
    public void seeCard(OseeFightTenMessage.TenSeeCardRequest request, ServerUser user) {
        fightTenManager.seeOrRubCard(0, user);
    }

    /**
     * 搓牌
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_RUB_CARD_REQUEST_VALUE)
    public void rubCard(OseeFightTenMessage.TenRubCardRequest request, ServerUser user) {
        fightTenManager.seeOrRubCard(1, user);
    }

    /**
     * 离开房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_LEAVE_ROOM_REQUEST_VALUE)
    public void leaveRoom(OseeFightTenMessage.TenLeaveRoomRequest request, ServerUser user) {
        fightTenManager.leaveRoom(user, true);
    }

    /**
     * 更换房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_CHANGE_ROOM_REQUEST_VALUE)
    public void changeRoom(OseeFightTenMessage.TenChangeRoomRequest request, ServerUser user) {
        fightTenManager.changeRoom(user);
    }

    /**
     * 赠送礼物
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_GIVE_GIFT_REQUEST_VALUE)
    public void giveGift(OseeFightTenMessage.TenGiveGiftRequest request, ServerUser user) {
        fightTenManager.giveGift(user.getId(), request.getToPlayerId(), request.getGiftType());
    }

    // **************************************** 玩家任务相关 ************************************************

    /**
     * 获取玩家任务列表
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_TASK_LIST_REQUEST_VALUE)
    public void getTaskList(OseeFightTenMessage.TenTaskListRequest request, ServerUser user) {
        taskManager.getTaskList(user);
    }

    /**
     * 获取任务奖励
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_OSEE_TEN_GET_TASK_REWARD_REQUEST_VALUE)
    public void getTaskReward(OseeFightTenMessage.TenGetTaskRewardRequest request, ServerUser user) {
        taskManager.getTaskReward(request.getTaskId(), user);
    }
}
