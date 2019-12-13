package com.maple.game.osee.controller.gobang;

import java.lang.reflect.Method;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.protobuf.Message;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.entity.gobang.GobangGamePlayer;
import com.maple.game.osee.entity.gobang.GobangGameRoom;
import com.maple.game.osee.manager.gobang.GobangManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangAdmitDefeatRequest;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangCreateRoomRequest;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangExitRoomRequest;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangJoinRoomRequest;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangPlacePieceRequest;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangReadyRequest;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangSetTuitionRequest;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.data.gobang.BaseGobangPlayer;

/**
 * 1688五子棋控制器
 */
@AppController
public class GobangController {

	@Autowired
	private GobangManager gobangManager;

	/**
	 * 默认检查器
	 */
	public void checker(Method taskMethod, Message req, ServerUser user, Long exp) throws Exception {
		if (user.getEntity() == null) {
			return;
		}

		GobangGameRoom gameRoom = GameContainer.getGameRoomByPlayerId(user.getId());

		if (gameRoom == null && exp != 0) {
			taskMethod.invoke(this, req, user);
		} else if (gameRoom != null && exp == 0) {
			GobangGamePlayer gamePlayer = gameRoom.getGamePlayerById(user.getId());
			taskMethod.invoke(this, req, gamePlayer, gameRoom);
		}
	}

	/**
	 * 五子棋创建房间任务
	 */
	@AppHandler(msgCode = OseeMsgCode.C_S_OSEE_GOBANG_CREATE_ROOM_REQUEST_VALUE, exp = -1)
	public void doCreateRoomTask(GobangCreateRoomRequest req, ServerUser user) {
		gobangManager.createGameRoom(user);
	}

	/**
	 * 五子棋加入房间任务
	 */
	@AppHandler(msgCode = OseeMsgCode.C_S_OSEE_GOBANG_JOIN_ROOM_REQUEST_VALUE, exp = -1)
	public void doJoinRoomTask(GobangJoinRoomRequest req, ServerUser user) {
		gobangManager.joinGameRoom(user, req.getRoomCode());
	}

	/**
	 * 五子棋退出房间任务
	 */
	@AppHandler(msgCode = OseeMsgCode.C_S_OSEE_GOBANG_EXIT_ROOM_REQUEST_VALUE)
	public void doExitRoomTask(GobangExitRoomRequest req, GobangGamePlayer gamePlayer, GobangGameRoom gameRoom) {
		gobangManager.exitGameRoom(gameRoom, gamePlayer.getUser());
	}

	/**
	 * 五子棋设置学费任务
	 */
	@AppHandler(msgCode = OseeMsgCode.C_S_OSEE_GOBANG_SET_TUITION_REQUEST_VALUE)
	public void doSetTuitionTask(GobangSetTuitionRequest req, GobangGamePlayer gamePlayer, GobangGameRoom gameRoom) {
		gobangManager.setTuition(gamePlayer, gameRoom, req.getTuition());
	}

	/**
	 * 五子棋准备任务
	 */
	@AppHandler(msgCode = OseeMsgCode.C_S_OSEE_GOBANG_READY_REQUEST_VALUE)
	public void doReadyTask(GobangReadyRequest req, GobangGamePlayer gamePlayer, GobangGameRoom gameRoom) {
		gobangManager.playerReady(gamePlayer, gameRoom);
	}

	/**
	 * 五子棋玩家落子任务
	 */
	@AppHandler(msgCode = OseeMsgCode.C_S_OSEE_GOBANG_PLACE_PIECE_REQUEST_VALUE)
	public void doPlacePieceTask(GobangPlacePieceRequest req, GobangGamePlayer gamePlayer, GobangGameRoom gameRoom) {
		gobangManager.playChess(gameRoom, gamePlayer, req.getX(), req.getY());
	}

	/**
	 * 五子棋认输任务
	 */
	@AppHandler(msgCode = OseeMsgCode.C_S_OSEE_GOBANG_ADMIT_DEFEAT_REQUEST_VALUE)
	public void doAdmitDefeatTask(GobangAdmitDefeatRequest req, GobangGamePlayer gamePlayer, GobangGameRoom gameRoom) {
		for (BaseGamePlayer player : gameRoom.getGamePlayers()) {
			if (player != null && player.getId() != gamePlayer.getId()) {
				gobangManager.playerWin(gameRoom, (BaseGobangPlayer) player);
			}
		}
	}

}
