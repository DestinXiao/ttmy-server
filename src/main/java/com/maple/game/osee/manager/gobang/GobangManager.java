package com.maple.game.osee.manager.gobang;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.log.entity.OseeGobangRecordLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeGobangRecordLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.gobang.GobangGamePlayer;
import com.maple.game.osee.entity.gobang.GobangGameRoom;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangExitRoomResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangGameOverResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangGameStartResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangJoinRoomResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangOrderPlayerResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangPlacePiece;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangPlacePieceResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangPlayerInfoListResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangPlayerInfoProto;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangPlayerInfoResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangPlayerStateChangeResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangReadyResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangReconnectResponse;
import com.maple.game.osee.proto.gobang.OseeGobangMessage.GobangSetTuitionResponse;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.data.gobang.BaseGobangPlayer;
import com.maple.gamebase.data.gobang.BaseGobangRoom;
import com.maple.gamebase.data.gobang.GoBangRoomState;
import com.maple.gamebase.manager.gobang.BaseGobangManager;
import com.maple.network.manager.NetManager;

/**
 * 1688五子棋管理类
 */
@Component
public class GobangManager extends BaseGobangManager {

    @Autowired
    private OseeGobangRecordLogMapper gobangRecordLogMapper;

    /**
     * 创建房间
     */
    public void createGameRoom(ServerUser user) {
        if (PlayerManager.getPlayerVipLevel(user) < 2) {
            NetManager.sendErrorMessageToClient("需要VIP2及以上才能创建五子棋房间！", user);
            return;
        }
        GobangGameRoom gameRoom = GameContainer.createGameRoom(GobangGameRoom.class, 2);
        joinGameRoom(user, gameRoom.getCode());
    }

    /**
     * 加入房间
     */
    public void joinGameRoom(ServerUser user, int roomCode) {
        if (PlayerManager.getPlayerVipLevel(user) < 2) {
            NetManager.sendErrorMessageToClient("需要VIP2及以上才能加入五子棋房间！", user);
            return;
        }
        GobangGameRoom gameRoom = GameContainer.getGameRoomByCode(roomCode);
        if (gameRoom == null) {
            NetManager.sendHintMessageToClient("房间不存在", user);
            return;
        }
        synchronized (gameRoom) {
            if (gameRoom.getPlayerSize() >= 2) {
                NetManager.sendHintMessageToClient("房间人数已满", user);
                return;
            }
            GobangGamePlayer gamePlayer = GameContainer.createGamePlayer(gameRoom, user, GobangGamePlayer.class);
            sendJoinRoomResponse(gameRoom, gamePlayer);
            sendPlayerInfoResponse(gameRoom, gamePlayer);
            sendPlayerInfoListResponse(gameRoom, gamePlayer);
        }
    }

    /**
     * 退出房间
     */
    public void exitGameRoom(GobangGameRoom gameRoom, ServerUser user) {
        GobangGamePlayer player = gameRoom.getGamePlayerById(user.getId());

        if (player.getSeat() == 0) {
            GobangGamePlayer otherPlayer = gameRoom.getGamePlayerBySeat(1);
            if (otherPlayer != null && otherPlayer.getUser().isOnline()) {
                NetManager.sendHintMessageToClient("房主已离开房间，房间已解散", otherPlayer.getUser());
            }

            for (BaseGamePlayer roomPlayer : gameRoom.getGamePlayers()) {
                if (roomPlayer != null && roomPlayer.getUser().isOnline()) {
                    GobangExitRoomResponse.Builder builder = GobangExitRoomResponse.newBuilder();
                    builder.setPlayerId(roomPlayer.getId());
                    sendRoomMessage(OseeMsgCode.S_C_OSEE_GOBANG_EXIT_ROOM_RESPONSE_VALUE, builder, gameRoom);
                }
            }

            GameContainer.removeGameRoom(gameRoom);
        } else {
            GobangExitRoomResponse.Builder builder = GobangExitRoomResponse.newBuilder();
            builder.setPlayerId(player.getId());
            sendRoomMessage(OseeMsgCode.S_C_OSEE_GOBANG_EXIT_ROOM_RESPONSE_VALUE, builder, gameRoom);

            GameContainer.removeGamePlayer(gameRoom, player.getSeat());
            gameRoom.reset();
        }
    }

    /**
     * 玩家重连
     */
    public void reconnect(GobangGameRoom gameRoom, ServerUser user) {
        sendPlayerStateChangeResponse(gameRoom, user);
        sendReconnectResponse(gameRoom, user);
    }

    /**
     * 玩家断线
     */
    public void disconnect(GobangGameRoom gameRoom, ServerUser user) {
        sendPlayerStateChangeResponse(gameRoom, user);
    }

    /**
     * 设置学费
     */
    public void setTuition(GobangGamePlayer player, GobangGameRoom gameRoom, long tuition) {
        if (gameRoom.isStart()) {
            NetManager.sendHintMessageToClient("游戏已开始，无法更改学费", player.getUser());
            return;
        }

        if (gameRoom.getGamePlayerBySeat(1) != player) {
            return;
        }

        gameRoom.setTuition(tuition);
        GobangSetTuitionResponse.Builder builder = GobangSetTuitionResponse.newBuilder();
        builder.setTuition(tuition);
        sendRoomMessage(OseeMsgCode.S_C_OSEE_GOBANG_SET_TUITION_RESPONSE_VALUE, builder, gameRoom);
    }

    /**
     * 玩家准备
     */
    public void playerReady(GobangGamePlayer player, GobangGameRoom gameRoom) {
        if (gameRoom.getTuition() <= 0) {
            NetManager.sendHintMessageToClient("学费未设置，无法准备", player.getUser());
            return;
        }

        if (player.getSeat() != 0 && !PlayerManager.checkItem(player.getUser(), ItemId.MONEY, gameRoom.getTuition())) {
            NetManager.sendHintMessageToClient("学费不足，无法准备", player.getUser());
            return;
        }

        player.setReady(true);
        GobangReadyResponse.Builder builder = GobangReadyResponse.newBuilder();
        builder.setPlayerId(player.getId());
        sendRoomMessage(OseeMsgCode.S_C_OSEE_GOBANG_READY_RESPONSE_VALUE, builder, gameRoom);

        for (BaseGamePlayer basePlayer : gameRoom.getGamePlayers()) {
            if (!((GobangGamePlayer) basePlayer).isReady()) {
                return;
            }
        }

        gameStart(gameRoom);
    }

    /**
     * 创建玩家数据
     */
    public GobangPlayerInfoProto createPlayerInfoProto(GobangGamePlayer gamePlayer) {
        GobangPlayerInfoProto.Builder builder = GobangPlayerInfoProto.newBuilder();
        builder.setPlayerId(gamePlayer.getId());
        builder.setName(gamePlayer.getUser().getNickname());
        builder.setHeadIndex(gamePlayer.getUser().getEntity().getHeadIndex());
        builder.setHeadUrl(gamePlayer.getUser().getEntity().getHeadUrl());
        builder.setSex(gamePlayer.getUser().getEntity().getSex());
        builder.setMoney(PlayerManager.getPlayerMoney(gamePlayer.getUser()));
        builder.setSeat(gamePlayer.getSeat());
        builder.setOnline(gamePlayer.getUser().isOnline());
        builder.setVipLevel(PlayerManager.getPlayerVipLevel(gamePlayer.getUser()));
        builder.setReady(gamePlayer.isReady());
        return builder.build();
    }

    /**
     * 发送加入房间消息
     */
    public void sendJoinRoomResponse(GobangGameRoom gameRoom, GobangGamePlayer gamePlayer) {
        GobangJoinRoomResponse.Builder builder = GobangJoinRoomResponse.newBuilder();
        builder.setRoomCode(gameRoom.getCode());
        builder.setTuition(gameRoom.getTuition());
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_GOBANG_JOIN_ROOM_RESPONSE_VALUE, builder, gamePlayer.getUser());
    }

    /**
     * 发送玩家数据给房间玩家
     */
    public void sendPlayerInfoResponse(GobangGameRoom gameRoom, GobangGamePlayer gamePlayer) {
        GobangPlayerInfoResponse.Builder builder = GobangPlayerInfoResponse.newBuilder();
        builder.setPlayerInfo(createPlayerInfoProto(gamePlayer));
        sendRoomMessage(OseeMsgCode.S_C_OSEE_GOBANG_PLAYER_INFO_RESPONSE_VALUE, builder, gameRoom);
    }

    /**
     * 发送玩家列表给玩家
     */
    public void sendPlayerInfoListResponse(GobangGameRoom gameRoom, GobangGamePlayer gamePlayer) {
        GobangPlayerInfoListResponse.Builder builder = GobangPlayerInfoListResponse.newBuilder();
        for (BaseGamePlayer basePlayer : gameRoom.getGamePlayers()) {
            if (basePlayer != null) {
                builder.addPlayerInfos(createPlayerInfoProto((GobangGamePlayer) basePlayer));
            }
        }
        int msgCode = OseeMsgCode.S_C_OSEE_GOBANG_PLAYER_INFO_LIST_RESPONSE_VALUE;
        NetManager.sendMessage(msgCode, builder, gamePlayer.getUser());
    }

    /**
     * 发送五子棋玩家状态变化消息
     */
    public void sendPlayerStateChangeResponse(GobangGameRoom gameRoom, ServerUser user) {
        GobangPlayerStateChangeResponse.Builder builder = GobangPlayerStateChangeResponse.newBuilder();
        builder.setPlayerId(user.getId());
        builder.setOnline(user.isOnline());
        sendRoomMessage(OseeMsgCode.S_C_OSEE_GOBANG_PLAYER_STATE_CHANGE_RESPONSE_VALUE, builder, gameRoom);
    }

    /**
     * 发送重连消息
     */
    public void sendReconnectResponse(GobangGameRoom gameRoom, ServerUser user) {
        GobangReconnectResponse.Builder builder = GobangReconnectResponse.newBuilder();
        builder.setRoomCode(gameRoom.getCode());
        builder.setTuition(gameRoom.getTuition());
        for (BaseGamePlayer basePlayer : gameRoom.getGamePlayers()) {
            if (basePlayer != null) {
                builder.addPlayerInfos(createPlayerInfoProto((GobangGamePlayer) basePlayer));
            }
        }

        builder.setGameState(gameRoom.getGameState());
        if (gameRoom.getGameState() == GoBangRoomState.GOBANG_PLAY) {
            builder.setOrderPlayerId(gameRoom.getNowPlayerId());
            for (int x = 0; x < BaseGobangRoom.CHESSBOARD_WIDTH; x++) {
                for (int y = 0; y < BaseGobangRoom.CHESSBOARD_WIDTH; y++) {
                    if (gameRoom.getChessboard()[x][y] > 0) {
                        GobangPlacePiece.Builder placePiece = GobangPlacePiece.newBuilder();
                        placePiece.setX(x);
                        placePiece.setY(y);
                        long playerId = gameRoom.getGamePlayerBySeat(gameRoom.getChessboard()[x][y] - 1).getId();
                        placePiece.setPlayerId(playerId);
                        builder.addPlacePieces(placePiece);
                    }
                }
            }
        }
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_GOBANG_RECONNECT_RESPONSE_VALUE, builder, user);
    }

    /**
     * 五子棋
     */
    @Override
    public void playChess0(BaseGobangRoom gameRoom, BaseGobangPlayer nowPlayer, int x, int y) {
        GobangPlacePieceResponse.Builder builder = GobangPlacePieceResponse.newBuilder();
        builder.setPlayerId(nowPlayer.getId());
        builder.setX(x);
        builder.setY(y);
        sendRoomMessage(OseeMsgCode.S_C_OSEE_GOBANG_PLACE_PIECE_RESPONSE_VALUE, builder, gameRoom);
    }

    @Override
    public void playerWin0(BaseGobangRoom gameRoom, BaseGobangPlayer nowPlayer) {
        synchronized (gameRoom) {
            GobangGameRoom room = (GobangGameRoom) gameRoom;
            long money = nowPlayer.getSeat() == 0 ? room.getTuition() : 0;

            OseeGobangRecordLogEntity log = new OseeGobangRecordLogEntity();
            log.setMoney(room.getTuition());
            for (BaseGamePlayer basePlayer : gameRoom.getGamePlayers()) {
                boolean win = basePlayer.getSeat() == nowPlayer.getSeat();
                long changeMoney = win ? money : -money;
                if (win) {
                    log.setWinnerId(basePlayer.getId());
                    log.setWinnerNickname(basePlayer.getUser().getNickname());
                    log.setWinnerBeforeMoney(PlayerManager.getPlayerEntity(basePlayer.getUser()).getMoney());
                } else {
                    log.setLoserId(basePlayer.getId());
                    log.setLoserNickname(basePlayer.getUser().getNickname());
                    log.setLoserBeforeMoney(PlayerManager.getPlayerEntity(basePlayer.getUser()).getMoney());
                }

                ItemChangeReason reason = win ? ItemChangeReason.GOBANG_RESULT : ItemChangeReason.GOBANG_RESULT;
                PlayerManager.addItem(basePlayer.getUser(), ItemId.MONEY, changeMoney, reason, true);

                if (win) {
                    log.setWinnerAfterMoney(PlayerManager.getPlayerEntity(basePlayer.getUser()).getMoney());
                } else {
                    log.setLoserAfterMoney(PlayerManager.getPlayerEntity(basePlayer.getUser()).getMoney());
                }

                GobangGameOverResponse.Builder builder = GobangGameOverResponse.newBuilder();
                builder.setResult(win);
                if (win) {
                    builder.setWinnerChange(changeMoney);
                    builder.setLoserChange(-changeMoney);
                } else {
                    builder.setWinnerChange(-changeMoney);
                    builder.setLoserChange(changeMoney);
                }
                NetManager.sendMessage(OseeMsgCode.S_C_OSEE_GOBANG_GAME_OVER_RESPONSE_VALUE, builder, basePlayer.getUser());
            }
            gobangRecordLogMapper.save(log);

            room.reset();
        }
    }

    @Override
    public void changeNowPlayer0(BaseGobangRoom gameRoom, BaseGobangPlayer nowPlayer) {
        GobangOrderPlayerResponse.Builder builder = GobangOrderPlayerResponse.newBuilder();
        builder.setPlayerId(nowPlayer.getId());
        sendRoomMessage(OseeMsgCode.S_C_OSEE_GOBANG_ORDER_PLAYER_RESPONSE_VALUE, builder, gameRoom);
    }

    @Override
    public void gameStart0(BaseGobangRoom gameRoom) {
        GobangGameStartResponse.Builder builder = GobangGameStartResponse.newBuilder();
        sendRoomMessage(OseeMsgCode.S_C_OSEE_GOBANG_GAME_START_RESPONSE_VALUE, builder, gameRoom);
    }

}
