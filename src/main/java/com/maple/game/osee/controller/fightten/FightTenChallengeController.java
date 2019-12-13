package com.maple.game.osee.controller.fightten;

import com.google.protobuf.Message;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.manager.fightten.FightTenChallengeManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.fightten.TtmyFightTenChallengeMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

/**
 * 拼十挑战赛控制器
 *
 * @author Junlong
 */
@AppController
public class FightTenChallengeController {

    @Autowired
    private FightTenChallengeManager challengeManager;

    /**
     * 默认检查器
     */
    public void checker(Method taskMethod, Message req, ServerUser user, Long exp) throws Exception {
        taskMethod.invoke(this, req, user);
    }

    /**
     * 钻石兑换挑战次数
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_TEN_CHALLENGE_EXCHANGE_TIMES_REQUEST_VALUE)
    public void exchangeTime(TtmyFightTenChallengeMessage.TenChallengeExchangeTimeRequest request, ServerUser user) {
        challengeManager.exchangeTimes(user, request.getDiamondNum());
    }

    /**
     * 获取房间列表
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_TEN_CHALLENGE_ROOM_LIST_REQUEST_VALUE)
    public void roomList(TtmyFightTenChallengeMessage.TenChallengeRoomListRequest request, ServerUser user) {
        challengeManager.sendRoomList(user, request.getPageNo(), request.getPageSize());
    }

    /**
     * 创建挑战赛房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_TEN_CHALLENGE_CREATE_ROOM_REQUEST_VALUE)
    public void createRoom(TtmyFightTenChallengeMessage.TenChallengeCreateRoomRequest request, ServerUser user) {
        challengeManager.createRoom(user, request.getMoneyLimitIndex(), request.getFeeType(), request.getRoundIndex(), request.getPrivateRoom());
    }

    /**
     * 加入房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_TEN_CHALLENGE_JOIN_ROOM_REQUEST_VALUE)
    public void joinRoom(TtmyFightTenChallengeMessage.TenChallengeJoinRoomRequest request, ServerUser user) {
        challengeManager.joinRoom(user, request.getRoomCode());
    }

    /**
     * 房主踢人
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_TEN_CHALLENGE_KICK_PLAYER_REQUEST_VALUE)
    public void kickPlayer(TtmyFightTenChallengeMessage.TenChallengeKickPlayerRequest request, ServerUser user) {
        challengeManager.kickPlayer(user, request.getPlayerId());
    }

    /**
     * 自动匹配
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_TEN_CHALLENGE_MATCH_REQUEST_VALUE)
    public void autoMatch(TtmyFightTenChallengeMessage.TenChallengeMatchRequest request, ServerUser user) {
        challengeManager.autoMatch(user);
    }

    /**
     * 取消匹配请求
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_TEN_CHALLENGE_CANCEL_MATCH_REQUEST_VALUE)
    public void cancelMatch(TtmyFightTenChallengeMessage.TenChallengeCancelMatchRequest request, ServerUser user) {
        challengeManager.cancelMatch(user);
    }

    /**
     * 玩家获取剩余的拼十挑战次数
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_TEN_CHALLENGE_REST_TIMES_REQUEST_VALUE)
    public void restChallengeTimes(TtmyFightTenChallengeMessage.TenChallengeRestTimesRequest request, ServerUser user) {
        challengeManager.sendRestChallengeTimes(user);
    }

    /**
     * 获取挑战赛排行榜
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_TEN_CHALLENGE_RANKING_LIST_REQUEST_VALUE)
    public void rankingList(TtmyFightTenChallengeMessage.TenChallengeRankingListRequest request, ServerUser user) {
        challengeManager.rankingList(user);
    }

    /**
     * 请求离开房间
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_TEN_CHALLENGE_LEAVE_ROOM_REQUEST_VALUE)
    public void leaveRoom(TtmyFightTenChallengeMessage.TenChallengeLeaveRoomRequest request, ServerUser user) {
        challengeManager.leaveRoom(user, true);
    }
}
