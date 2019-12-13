package com.maple.game.osee.manager.two_eight;

import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.entity.two_eight.TwoEightPlayer;
import com.maple.game.osee.entity.two_eight.TwoEightRoom;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.OseeTwoEightMessage;
import com.maple.game.osee.proto.OseeTwoEightPublicData;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage;
import com.maple.network.manager.NetManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maple.gamebase.manager.BaseRoomManager.sendRoomMessage;

@Component
public class TEMessageService  {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    public void test(){
        int []carda={2,9};
        int []cardb={0,1};
        System.out.println(getCardScore(carda));
        System.out.println(getCardScore(cardb));
    }

    //创建玩家消息
    public OseeTwoEightPublicData.RoomPlayerInfoProto createRoomPlayerInfoResponse(TwoEightPlayer player) {
        OseeTwoEightPublicData.RoomPlayerInfoProto.Builder builder = OseeTwoEightPublicData.RoomPlayerInfoProto.newBuilder();
        if (player.isRobot()){
            builder.setPlayerId(-1l);
            builder.setHeadUrl("1");
            builder.setNickname("系统坐庄");
            builder.setRole(1);
            builder.setMoney(30000000);
            return builder.build();
        }
        ServerUser user = player.getUser();

        OseePlayerEntity playerEntity = PlayerManager.getPlayerEntity(user);


        builder.setPlayerId(user.getId());
        builder.setHeadUrl(user.getEntity().getHeadIndex() == 0 ? user.getEntity().getHeadUrl()
                : Integer.toString(user.getEntity().getHeadIndex()));
        builder.setNickname(user.getEntity().getNickname());
        builder.setRole(player.getRole());
        builder.setMoney(player.getEntity().getMoney());

        return builder.build();
    }

    //更新房间内桌上无座玩家列表
    public void addPlayerToRoom(TwoEightPlayer gamePlayer, TwoEightRoom room) {
        List<TwoEightPlayer> players = room.getAllPlayers();
        players.add(gamePlayer);

    }


    //更新玩家列表
    public void deletePayerFromRoom(TwoEightRoom room, TwoEightPlayer player) {
        List<TwoEightPlayer> players = room.getAllPlayers();
        if (room.getApplyForBanker().contains(player)){
            room.getApplyForBanker().remove(player);
        }
        players.remove(player);

    }

    /**
     * 发送给玩家信息（金币、昵称、座位号）
     * @param twoEightRoom
     * @param gamePlayer
     */
    public void sendPlayerInfoResponse(TwoEightRoom twoEightRoom, TwoEightPlayer gamePlayer) {
        OseeTwoEightMessage.TERoomPlayerResponse.Builder playerInfo=
                OseeTwoEightMessage.TERoomPlayerResponse.newBuilder();
        playerInfo.setInfo(createRoomPlayerInfoResponse(gamePlayer));


        //发送给该玩家房间信息
        NetManager.sendMessage(
                OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_PLAYER_INFO_RESPONSE_VALUE,
                playerInfo.build(),gamePlayer.getUser());
    }

    /**
     * 发送玩家物品数据
     */
    public  void sendPlayerMoneyResponse(TwoEightPlayer player) {
        OseeLobbyMessage.PlayerMoneyResponse.Builder builder = OseeLobbyMessage.PlayerMoneyResponse.newBuilder();
        builder.setMoney(player.getEntity().getMoney());
        builder.setLottery(player.getEntity().getLottery());
        builder.setDiamond(player.getEntity().getDiamond());
        builder.setBankMoney(player.getEntity().getBankMoney());
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_OSEE_PLAYER_MONEY_RESPONSE_VALUE, builder.build(), player.getUser());
    }





    /**
     * 将房间信息发送给所有玩家
     */
    public void sendRoomPlayerInfoListResponse(TwoEightRoom twoEightRoom) {



        sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_PLAYER_INFO_LIST_RESPONSE_VALUE,
                createRoomPlayerListInfoResponse(twoEightRoom),twoEightRoom);
    }

    private OseeTwoEightMessage.TEPlayerListResponse createRoomPlayerListInfoResponse(TwoEightRoom twoEightRoom) {
        OseeTwoEightMessage.TEPlayerListResponse.Builder infoListResponse =
                OseeTwoEightMessage.TEPlayerListResponse.newBuilder();

        for (int i = 0; i < twoEightRoom.getGamePlayers().length; i++) {
            if (twoEightRoom.getGamePlayerBySeat(i) != null) {
                infoListResponse.addPlayerList(createRoomPlayerInfoResponse(twoEightRoom.getGamePlayerBySeat(i)));
            }
        }
        return infoListResponse.build();

    }

    /**
     * 将房间信息发送给某个玩家
     */
    public void sendRoomPlayerInfoListResponse(TwoEightRoom twoEightRoom,TwoEightPlayer player) {

        NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_PLAYER_INFO_LIST_RESPONSE_VALUE,
                createRoomPlayerListInfoResponse(twoEightRoom),player.getUser());
    }

    /**
     * 发送上庄列表给所有玩家
     */

    public void sendBankersInfoResponse(TwoEightRoom gameRoom){
        OseeTwoEightMessage.WaitToBeBankerInfoResponse.Builder bankerListInfo =
                OseeTwoEightMessage.WaitToBeBankerInfoResponse.newBuilder();
        for (TwoEightPlayer player:gameRoom.getApplyForBanker()){
            bankerListInfo.addBankerList(createRoomPlayerInfoResponse(player));
        }
        sendRoomMessage(
                OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_TO_BANKER_RESPONSE_VALUE,
                bankerListInfo.build(),gameRoom);
    }

    /**
     * 发送上庄列表给某个玩家
     */

    public void sendBankersInfoResponse(TwoEightRoom gameRoom,ServerUser user){
        OseeTwoEightMessage.WaitToBeBankerInfoResponse.Builder bankerListInfo =
                OseeTwoEightMessage.WaitToBeBankerInfoResponse.newBuilder();
        for (TwoEightPlayer player:gameRoom.getApplyForBanker()){
            bankerListInfo.addBankerList(createRoomPlayerInfoResponse(player));
        }
        NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_TO_BANKER_RESPONSE_VALUE,
                bankerListInfo.build(),user);
    }

    /**
     * 发送房间信息给加入房间的玩家
     * @param twoEightRoom
     * @param gamePlayer
     */
    public void sendRoomInfoResponse(TwoEightRoom twoEightRoom, TwoEightPlayer gamePlayer) {
        OseeTwoEightMessage.TwoEightRoomInfoResponse.Builder builder =
                OseeTwoEightMessage.TwoEightRoomInfoResponse.newBuilder();
        builder.setRoomState(twoEightRoom.getRoomStatus());
        builder.setRoomCode(twoEightRoom.getCode());
        builder.setPeopleNum(twoEightRoom.getPlayerSize());
        if (twoEightRoom.getBanker()!=null&&!twoEightRoom.getBanker().isRobot()){
            builder.setBankerId(twoEightRoom.getBanker().getId());
        }
        builder.setShunMoney(twoEightRoom.getBetMoney(0));
        builder.setTianMoney(twoEightRoom.getBetMoney(1));
        builder.setDiMoney(twoEightRoom.getBetMoney(2));
        NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TWOEIGHT_ROOMINFO_RESPONSE_VALUE,
                builder.build(),
                gamePlayer.getUser());
    }

    //    //获取牌的映射值
//    public void test(){
//        int[] carda={2,3};
//        int[] cardb={8,4};
//        System.out.println(getCardScore(carda));
//        System.out.println(getCardScore(cardb));
//    }
    public int getCardScore(int []cards){
        int carda = cards[0];
        int cardb = cards[1];
        //carda cardb 按顺序排列
        int scores=0;
        if (carda>cardb){
            int i=carda;
            carda =cardb;
            cardb = i;
        }
        //豹子情况
        if (carda==cardb){
            //对幺鸡情况
            if (carda==0){
                scores=400*10;
            }else {
                scores=400*carda;
            }
            return scores;
        }
        //28筒情况
        if (carda==2&&cardb==8){
            scores=300;
            return scores;
        }

        //散牌情况 最大得分为9.5*20+9=199
        float pointScore;
        if (carda==0){
            pointScore=(0.5f+cardb);
        }else {
            pointScore=(carda+cardb)%10;
        }
        return (int)(pointScore*20+cardb);
    }

    //发送房间内所有下注详情
    public void sendRoomBetInfoResponse(TwoEightRoom room,TwoEightPlayer player) {
        OseeTwoEightMessage.RoomBetInfoResponse.Builder builder =
                OseeTwoEightMessage.RoomBetInfoResponse.newBuilder();
        builder.addAllTypeShun(room.getBetInfo()[0]);
        builder.addAllTypeTian(room.getBetInfo()[1]);
        builder.addAllTypeDi(room.getBetInfo()[2]);
        NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_ROOM_BET_INFO_RESPONSE_VALUE,
                builder.build(),player.getUser());
    }

    /**
     * 发送当前局的输赢状况
     * @param scores
     * @param room
     */
    public void sendCurrentRoundWinInfo(int[] scores,TwoEightRoom room) {
        OseeTwoEightMessage.WinLoseInfoResponse.Builder winBuild = OseeTwoEightMessage.WinLoseInfoResponse.newBuilder();
        for (int i=0;i<3;i++){
            OseeTwoEightPublicData.WinInfoProto.Builder build = OseeTwoEightPublicData.WinInfoProto.newBuilder();
            build.setType(i);
            if (scores[i]>scores[3]){
                //输赢信息保存
                room.addWinInfo(i,0);
                build.setWinLose(0);
            }else {
//                输赢信息保存
                room.addWinInfo(i,1);
                build.setWinLose(1);
            }
            winBuild.addWinInfo(build.build());
        }
        sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_WIN_LOSE_INFO_RESPONSE_VALUE,
                winBuild.build(),room);
    }

    public void sendAllRoundWinInfo(TwoEightRoom room) {
        OseeTwoEightMessage.WinLoseHistoryInfoResponse.Builder listBuilder =
                OseeTwoEightMessage.WinLoseHistoryInfoResponse.newBuilder();
        for (int i = 0; i < 3; i++) {
            OseeTwoEightPublicData.WinHistoryInfoProto.Builder builder =
                    OseeTwoEightPublicData.WinHistoryInfoProto.newBuilder();
            builder.setType(i);
            builder.addAllWinLoseHistory(room.getWinInfoList(i));
            builder.build();
            listBuilder.addWinHistoryInfo(builder);
        }

        sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_WIN_LOSE_HISTORY_INFO_RESPONSE_VALUE,
                listBuilder.build(),room);
    }

    //发送玩家在各个门的下注情况
    public void sendPlayerAllTypeBetResponse(TwoEightPlayer player) {
        OseeTwoEightMessage.PlayerAllTypeBetMoneyResponse.Builder response =
                OseeTwoEightMessage.PlayerAllTypeBetMoneyResponse.newBuilder();
        response.setShunMoney(player.getBetMoney(0));
        response.setTianMoney(player.getBetMoney(1));
        response.setDiMoney(player.getBetMoney(2));

        NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_PLAYER_ALL_TYPE_BET_MONEY_RESPONSE_VALUE,
                response.build(),player.getUser());
    }

    public void sendReconnectInfoResponse(TwoEightRoom gameRoom, TwoEightPlayer gamePlayer) {
        OseeTwoEightMessage.ReconnectInfoResponse.Builder response =
                OseeTwoEightMessage.ReconnectInfoResponse.newBuilder();
        int roomState = gameRoom.getRoomStatus();
        response.setRoomState(roomState);
        if (gameRoom.getRoomStatus()!=TwoEightRoomState.NOTBEGIN){
            response.addAllTypeShun(gameRoom.getBetInfo()[0]);
            response.addAllTypeTian(gameRoom.getBetInfo()[1]);
            response.addAllTypeDi(gameRoom.getBetInfo()[2]);
            int remainTime = 4;
            if (roomState==TwoEightRoomState.DOBET){
                remainTime = (int) (30-(System.currentTimeMillis()-gameRoom.getEnterStateTime())/1000);
            }else if (roomState==TwoEightRoomState.ROUNDOVER){
                for (int i=0;i<4;i++){
                    response.addCards(createCardInfo(gameRoom,i));
                }
            }
            response.setRemainTime(remainTime);
        }


        NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_PLAYER_RECONNECT_INFO_RESPONSE_VALUE,
                response.build(),gamePlayer.getUser());


    }

    /**
     * 牌消息的构建
     * @param twoEightRoom
     * @param i 0-顺，1-天，2-地，3-庄
     * @return
     */
    public OseeTwoEightPublicData.CardInfoProto createCardInfo(TwoEightRoom twoEightRoom,int i) {
        OseeTwoEightPublicData.CardInfoProto.Builder cardInfo =
                OseeTwoEightPublicData.CardInfoProto.newBuilder();
        cardInfo.setBetType(i);
        int cards[] = twoEightRoom.getCards()[i];
        int carda = cards[0];
        int cardb = cards[1];
        cardInfo.setCarda(carda);
        cardInfo.setCardb(cardb);


        cardInfo.setScoreType(getCardType(carda,cardb));
        return cardInfo.build();

    }

    public int getCardType(int carda,int cardb){
        int type =0;
        if (carda==cardb){
            type=11;
        }else if ((carda==2&&cardb==8)||(carda==8&&cardb==2)){
            type=10;
        }else if (carda==0||cardb==0){
            if (carda<cardb)
                type=cardb;
            else type=carda;
            type+=11;
        }
        else {
            type=(carda+cardb)%10;
        }
        return type;
    }

    public  String getScoreType(int type){
        //1-9 分别表示点数 10 表示二八 11表示豹子 12-一点半 13-二点半 14-三点半。。。20-九点半
        switch (type){
            case 0:
                return "零点";
            case 1:
                return "一点";
            case 2:
                return "二点";
            case 3:
                return "三点";
            case 4:
                return "四点";
            case 5:
                return "五点";
            case 6:
                return "六点";
            case 7:
                return "七点";
            case 8:
                return "八点";
            case 9:
                return "九点";
            case 10:
                return "二八杠";
            case 11:
                return "豹子";
            case 12:
                return "一点半";
            case 13:
                return "二点半";
            case 14:
                return "三点半";
            case 15:
                return "四点半";
            case 16:
                return "五点半";
            case 17:
                return "六点半";
            case 18:
                return "七点半";
            case 19:
                return "八点半";
            case 20:
                return "九点半";

        }
        return "";
    }
}
