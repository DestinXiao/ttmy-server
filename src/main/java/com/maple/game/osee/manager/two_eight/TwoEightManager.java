package com.maple.game.osee.manager.two_eight;

import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.dao.log.entity.OseeCutMoneyLogEntity;
import com.maple.game.osee.dao.log.entity.OseePlayerTenureLogEntity;
import com.maple.game.osee.dao.log.entity.TwoEightRecordLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeCutMoneyLogMapper;
import com.maple.game.osee.dao.log.mapper.OseePlayerTenureLogMapper;
import com.maple.game.osee.dao.log.mapper.TwoEightRecordMapper;
import com.maple.game.osee.entity.GameEnum;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.two_eight.TwoEightConfig;
import com.maple.game.osee.entity.two_eight.TwoEightPlayer;
import com.maple.game.osee.entity.two_eight.TwoEightRoom;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeTwoEightMessage;
import com.maple.game.osee.proto.OseeTwoEightPublicData;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.manager.BaseRoomManager;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


/**
 * 二八杠房间
 */
@Component
public class TwoEightManager extends BaseRoomManager {

    private static Logger logger = LoggerFactory.getLogger(TwoEightManager.class);

    @Autowired
    private OseePlayerMapper playerMapper;
    //二八杠房间列表
    public static final List<TwoEightRoom> twoEightRoomList = new ArrayList<>();

    @Autowired
    private TEMessageService teMessageService;

    @Autowired
    private TwoEightRecordMapper twoEightRecordMapper;

    @Autowired
    private OseePlayerTenureLogMapper tenureLogMapper;


    @Autowired
    private OseeCutMoneyLogMapper cutMoneyLogMapper;

    public static long getTERobotMoney(String key) {
        String value = RedisHelper.get(key);
        if (StringUtils.isEmpty(value)) {
            return 0;
        }
        return Long.parseLong(value);
    }


    public static int getplayerSize() {
        int peopleNum = 0;
        for (TwoEightRoom room : twoEightRoomList) {
            for (BaseGamePlayer player : room.getGamePlayers()) {
                if (player != null)
                    peopleNum += 1;
            }
        }
        return peopleNum;
    }

    public void joinRoom(ServerUser user) {

//        if (PlayerManager.getPlayerLevel(user) < 25) {
//            NetManager.sendErrorMessageToClient("等级不足，进入失败！", user);
//            return;
//        }

        TwoEightPlayer gamePlayer = null;
        TwoEightRoom twoEightRoom = null;

        OseePlayerEntity playerEntity = PlayerManager.getPlayerEntity(user);
        if (playerEntity.getMoney() < 500000) {
            NetManager.sendErrorMessageToClient("金币不够500000", user);
            return;
        }


        synchronized (twoEightRoomList) {
            for (TwoEightRoom room : twoEightRoomList) {
                if (room.getPlayerSize() < room.getMaxSize()) {
                    // 为房间创建一名游戏玩家
                    gamePlayer = GameContainer.createGamePlayer(room, user, TwoEightPlayer.class);
                    gamePlayer.setEntity(playerEntity);
                    twoEightRoom = room;
                    teMessageService.addPlayerToRoom(gamePlayer, twoEightRoom);
                    break;
                }
            }
            if (twoEightRoom == null) {
                //创建新的房间
                twoEightRoom = GameContainer.createGameRoom(TwoEightRoom.class, 100);
                twoEightRoom.roundSet();
                twoEightRoomList.add(twoEightRoom);
                gamePlayer = GameContainer.createGamePlayer(twoEightRoom, user, TwoEightPlayer.class);
                gamePlayer.setEntity(playerEntity);
                teMessageService.addPlayerToRoom(gamePlayer, twoEightRoom);

            }

            gamePlayer.roundSet();
            //第一次加入房间要设置角色
            gamePlayer.setRole(0);
//            gamePlayer.getUser().getEntity().setOnlineState(4);//更新状态
            logger.info("玩家[{}]加入房间[{}]", gamePlayer.getId(), twoEightRoom.getCode());


            // 发送玩家基本信息（座位号，金币等）发送给玩家
            teMessageService.sendPlayerInfoResponse(twoEightRoom, gamePlayer);
            //发送房间信息给玩家
            teMessageService.sendRoomInfoResponse(twoEightRoom, gamePlayer);
            // 将房间内所有玩家信息发给所有玩家
            teMessageService.sendRoomPlayerInfoListResponse(twoEightRoom);
            //发送庄家列表
            teMessageService.sendBankersInfoResponse(twoEightRoom, user);
            //发送房间信息详情
            teMessageService.sendReconnectInfoResponse(twoEightRoom, gamePlayer);

            teMessageService.sendAllRoundWinInfo(twoEightRoom);


        }
    }


    public void quitRoom(ServerUser user) {
        TwoEightPlayer player = GameContainer.getPlayerById(user.getId());
        if (player == null) {
            return;
        }
        TwoEightRoom gameRoom = GameContainer.getGameRoomByCode(player.getRoomCode());
        OseeTwoEightMessage.TERoomQuitResponse.Builder builder =
                OseeTwoEightMessage.TERoomQuitResponse.newBuilder();
        if (gameRoom.getRoomStatus() == TwoEightRoomState.NOTBEGIN || gameRoom.getRoomStatus() == TwoEightRoomState.NEXTROUND) {
            if (!gameRoom.getBanker().isRobot() && player.getId() == gameRoom.getBanker().getId())
                gameRoom.setBanker(null);
            if (player.getNotBetNum() >= 3)
                builder.setIsSuccess(3);
            else
                builder.setIsSuccess(0);

        } else if (gameRoom.getBetPlayers().contains(player) && gameRoom.getRoomStatus() != TwoEightRoomState.NEXTROUND) {
            builder.setIsSuccess(1);
            NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_QUIT_RESPONSE_VALUE
                    , builder.build(), user);
            return;
        } else if (gameRoom.getBanker() != null && !gameRoom.getBanker().isRobot() && user.getId() == gameRoom.getBanker().getId()) {
            builder.setIsSuccess(2);
            NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_QUIT_RESPONSE_VALUE
                    , builder.build(), user);
            return;
        } else {
            builder.setIsSuccess(0);
        }

        teMessageService.deletePayerFromRoom(gameRoom, player);
        GameContainer.removeGamePlayer(gameRoom, player.getSeat());

        logger.info("玩家[{}]退出房间[{}]", player.getId(), gameRoom.getCode());
//        player.getUser().getEntity().setOnlineState(1);//返回大厅 设置状态
        NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_QUIT_RESPONSE_VALUE
                , builder.build(), user);
        if (gameRoom.getAllPlayers().size() <= 1 && gameRoom.getBanker().isRobot()) {
            GameContainer.removeGameRoom(gameRoom);
            twoEightRoomList.remove(gameRoom);

//            gameRoom.roundSet();
//            gameRoom.setRoomStatus(TwoEightRoomState.NOTBEGIN);

        } else {

            teMessageService.sendBankersInfoResponse(gameRoom);
            teMessageService.sendRoomPlayerInfoListResponse(gameRoom);
        }


//        teMessageService.deletePayerFromRoom(gameRoom,player);

    }

    //申请上庄
    public void applyForBanker(int roomCode, ServerUser serverUser) {
        TwoEightPlayer twoEightPlayer = GameContainer.getPlayerById(serverUser.getId());
        TwoEightRoom gameRoom = GameContainer.getGameRoomByCode(roomCode);
        //申请当庄响应
        OseeTwoEightMessage.RoomBankerResponse.Builder bankResponse =
                OseeTwoEightMessage.RoomBankerResponse.newBuilder();
        if (twoEightPlayer == null) {
            return;
        }
        if (twoEightPlayer.getEntity().getMoney() < TwoEightRoom.toBeBankerMinMoney) {
//            NetManager.sendHintMessageToClient("金币不够1000万",serverUser);
            bankResponse.setIsSuccess(false);
            logger.info("玩家[{}]在房间[{}]申请上庄失败", twoEightPlayer.getId(), gameRoom.getCode());

            NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_BANKER_RESPONSE_VALUE,
                    bankResponse.build(), serverUser);
            return;
        } else {
            //申请成功，返回响应信息以及申请玩家列表更新；
            if (!gameRoom.getApplyForBanker().contains(twoEightPlayer)) {
                gameRoom.getApplyForBanker().add(twoEightPlayer);
            }
            bankResponse.setIsSuccess(true);
            logger.info("玩家[{}]在房间[{}]申请上庄成功", twoEightPlayer.getId(), gameRoom.getCode());

            NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_BANKER_RESPONSE_VALUE,
                    bankResponse.build(), serverUser);

            teMessageService.sendBankersInfoResponse(gameRoom);
            twoEightPlayer.setRole(2);
            if (gameRoom.getBanker() == null) {
                twoEightPlayer.setRole(1);
                gameRoom.setBanker(twoEightPlayer);
            }


        }
    }

    //摇骰子
    public void shakeDice(int roomCode) {
        TwoEightRoom twoEightRoom = GameContainer.getGameRoomByCode(roomCode);
        for (int i = 0; i < 2; i++) {
            twoEightRoom.getBankerDice().add(ThreadLocalRandom.current().nextInt(1, 7));
        }
        OseeTwoEightMessage.ShakeDiceResponse.Builder builder =
                OseeTwoEightMessage.ShakeDiceResponse.newBuilder();
        builder.addAllDice(twoEightRoom.getBankerDice());
        //发送骰子信息
        sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_SHAKE_DICE_REPONSE_VALUE,
                builder.build(), twoEightRoom);

        OseeTwoEightMessage.TERoomStateResponse.Builder stateResponse =
                OseeTwoEightMessage.TERoomStateResponse.newBuilder();
        //进入发牌阶段
        stateResponse.setRoomState(TwoEightRoomState.GETCARDS);
        stateResponse.setRemainTime(3);
        sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_STATE_RESPONSE_VALUE,
                stateResponse.build(), twoEightRoom);
        twoEightRoom.setRoomStatus(TwoEightRoomState.GETCARDS);
        twoEightRoom.setEnterStateTime(System.currentTimeMillis());

    }


    //发牌响应
    public void sendCardResponce(int roomCode) {
        logger.info("进入发牌响应");
        TwoEightRoom twoEightRoom = GameContainer.getGameRoomByCode(roomCode);
        ArrayList<Integer> cardList = new ArrayList<>();
        for (int card : twoEightRoom.TWOEIGHT_CARDS_DATA_ARRAY) {
            cardList.add(card);
        }
        //洗牌
        Collections.shuffle(cardList);
        OseeTwoEightMessage.CardInfoResponse.Builder builder =
                OseeTwoEightMessage.CardInfoResponse.newBuilder();

        List<int[]> allCards = new ArrayList<>();
        //发牌
        for (int i = 0; i < 4; i++) {
            int carda = cardList.get(i * 2);
            int cardb = cardList.get(i * 2 + 1);
            twoEightRoom.getCards()[i][0] = carda;
            twoEightRoom.getCards()[i][1] = cardb;
            allCards.add(twoEightRoom.getCards()[i]);


//            builder.addCards(teMessageService.createCardInfo(twoEightRoom,i));
        }
        //所有牌排序
        allCards.sort((o1, o2) -> {
            return teMessageService.getCardScore(o2) - teMessageService.getCardScore(o1);
        });


        //机器人坐庄判断金币是否满足盈利需求,如果为满足则大概率发大牌//用1000个随机数保证随机均匀
        if (getTERobotMoney(TwoEightConfig.RedisTwoEightRobotWinKey) <= TwoEightConfig.robotMinMoney && twoEightRoom.getBanker().isRobot()) {
            int rand = new Random().nextInt(1000);
            logger.info("赢钱随机数：" + rand);
            //处于发最大牌的概率
            if (rand <= TwoEightConfig.toWinFirstCardProbably * 10) {
                //与牌型最大的门交换牌
                swap(twoEightRoom.getCards()[3], allCards.get(0));

            } else if (rand <= TwoEightConfig.toWinSecondCardProbably * 10 + TwoEightConfig.toWinFirstCardProbably * 10) {
                //与牌型第二大的门交换牌
                swap(twoEightRoom.getCards()[3], allCards.get(1));

            } else if (rand <= 1000 - TwoEightConfig.toWinLastCardProbably * 10) {
                //与牌型第三大的门交换牌
                swap(twoEightRoom.getCards()[3], allCards.get(2));

            } else {
                //与牌型最小的牌交换牌
                swap(twoEightRoom.getCards()[3], allCards.get(3));

            }


        }//机器人坐庄判断金币是否满足盈利需求,如果满足则大概率发小牌
        else if (getTERobotMoney(TwoEightConfig.RedisTwoEightRobotWinKey) > TwoEightConfig.robotMinMoney && twoEightRoom.getBanker().isRobot()) {
            int rand = new Random().nextInt(1000);
            logger.info("输钱随机数：" + rand);
            //处于发最大牌的概率
            if (rand <= TwoEightConfig.toLoseFirstCardProbably * 10) {
                //与牌型最大的门交换牌
                swap(twoEightRoom.getCards()[3], allCards.get(0));

            } else if (rand <= TwoEightConfig.toLoseFirstCardProbably * 10 + TwoEightConfig.toLoseSecondCardProbably * 10) {
                //与牌型第二大的门交换牌
                swap(twoEightRoom.getCards()[3], allCards.get(1));

            } else if (rand <= 1000 - TwoEightConfig.toLoseLastCardProbably * 10) {
                //与牌型第三大的门交换牌
                swap(twoEightRoom.getCards()[3], allCards.get(2));

            } else {
                //与牌型最小的牌交换牌
                swap(twoEightRoom.getCards()[3], allCards.get(3));

            }
        }

        for (int i = 0; i < 4; i++) {
            builder.addCards(teMessageService.createCardInfo(twoEightRoom, i));
        }


        //发送发牌响应
        sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_CSRD_RESPONSE_VALUE,
                builder.build(), twoEightRoom);

    }

    private void swap(int[] carda, int[] cardb) {
        int[] s = cardb.clone();
        cardb[0] = carda[0];
        cardb[1] = carda[1];
        carda[0] = s[0];
        carda[1] = s[1];
    }


    /**
     * 玩家下注请求
     */
    public void betMoney(List<OseeTwoEightPublicData.BetInfo> betInfoList, ServerUser user) {
        TwoEightPlayer player = GameContainer.getPlayerById(user.getId());
        TwoEightRoom room = GameContainer.getGameRoomByCode(player.getRoomCode());
        if (room.getRoomStatus() != TwoEightRoomState.DOBET) {
            return;
        }
        synchronized (room) {

            Map<Integer, Long> playerBet = new HashMap<>();
            long betNum = 0l;
            for (int i = 0; i < 3; i++) {
                playerBet.put(i, 0l);
            }
            for (OseeTwoEightPublicData.BetInfo betInfo : betInfoList) {
                int type = betInfo.getBetType();
                playerBet.put(type, playerBet.get(type) + betInfo.getBetNum());
                betNum += betInfo.getBetNum();
            }


            OseeTwoEightMessage.BetInfoResponse.Builder betInfo =
                    OseeTwoEightMessage.BetInfoResponse.newBuilder();
            if (player.getEntity().getMoney() < betNum) {
                //下注金额不够
                betInfo.setRes(1);
            } else if (room.getTotalMoney() + betNum > (room.getBanker().getEntity().getMoney())) {
                //注已下满
                betInfo.setRes(2);
            } else {
                betInfo.setRes(0);
            }
            //发送下注响应,失败时单独给一个人发
            if (betInfo.getRes() != 0) {
                NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROMM_BET_RESPONSE_VALUE,
                        betInfo.build(), user);
                return;
            }

            player.getEntity().setMoney(player.getEntity().getMoney() - betNum);
            for (Map.Entry<Integer, Long> entry : playerBet.entrySet()) {
                if (entry.getValue() != 0l) {
                    player.addBetMoney(entry.getValue(), entry.getKey());
                    room.addBetMoney(entry.getValue(), entry.getKey());
                    room.addBetInfo(entry.getValue(), entry.getKey());

                }


            }

            if (!room.getBetPlayers().contains(player)) {
                room.getBetPlayers().add(player);
            }

            //下注后设置未下注局数为0，连续三局未下注踢出
            player.setNotBetNum(0);

            //成功时给所有人发
            OseeTwoEightMessage.PlayerBetInfoResponse.Builder plyerBetInfo =
                    OseeTwoEightMessage.PlayerBetInfoResponse.newBuilder();
            plyerBetInfo.addAllBetInfo(betInfoList);
            plyerBetInfo.setRemainMoney(player.getEntity().getMoney());
            plyerBetInfo.setPlayerId(player.getId());
            for (int i = 0; i < 3; i++) {
                plyerBetInfo.addAllMoney(room.getBetMoney(i));
            }

            sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_PLAYER_BET_INFO_RESPONSE_VALUE,
                    plyerBetInfo.build(), room);
            teMessageService.sendPlayerAllTypeBetResponse(player);//发送玩家 各个门的下注情况
            teMessageService.sendPlayerMoneyResponse(player);//发送玩家目前金币状况
            teMessageService.sendRoomPlayerInfoListResponse(room);

        }

    }


    public void roundOver(TwoEightRoom room) {


        int[][] cards = room.getCards();
        int[] scores = new int[4];
        //获取顺门牌大小映射值
        scores[0] = teMessageService.getCardScore(cards[0]);
        //获取天门牌大小映射值
        scores[1] = teMessageService.getCardScore(cards[1]);
        //获取地门牌大小映射值
        scores[2] = teMessageService.getCardScore(cards[2]);
        //获取庄牌大小映射值
        scores[3] = teMessageService.getCardScore(cards[3]);

        //发送各个门当前输赢情况
        teMessageService.sendCurrentRoundWinInfo(scores, room);
        //发送各个门历史输赢情况
        teMessageService.sendAllRoundWinInfo(room);


        TwoEightPlayer bankerPlayer = room.getBanker();
        for (TwoEightPlayer player : room.getBetPlayers()) {
            long betAllMoney = player.getBetMoney(0) +
                    player.getBetMoney(1) + player.getBetMoney(2);
            TwoEightRecordLogEntity twoEightRecord = new TwoEightRecordLogEntity();
            twoEightRecord.setPlayerId(player.getId());
            twoEightRecord.setNickname(player.getUser().getNickname());
            twoEightRecord.setInput(betAllMoney);
            twoEightRecord.setPlayBeforeMoney(
                    player.getEntity().getMoney() + betAllMoney);
            twoEightRecord.setCardType("");

            //更新各个门情况
            for (int i = 0; i < 3; i++) {
                //下注金额
                long betMoney = player.getBetMoney(i);
                if (betMoney != 0) {
                    int carda = cards[i][0];
                    int cardb = cards[i][1];
                    String doorType = "";
                    switch (i) {
                        case 0:
                            doorType = "顺门";
                            break;
                        case 1:
                            doorType = "天门";
                            break;
                        case 2:
                            doorType = "地门";
                    }
                    int type = teMessageService.getCardType(carda, cardb);
                    String scoreType = teMessageService.getScoreType(type);
                    twoEightRecord.setCardType(twoEightRecord.getCardType() + doorType + scoreType + "下注：" + betMoney + ";");

                }
                if (scores[i] <= scores[3]) {
                    //玩家情况，下注时减过了 所以entity不用减
                    player.setCurrentScore(player.getCurrentScore() - betMoney);

                    //庄家情况
                    bankerPlayer.setCurrentScore(bankerPlayer.getCurrentScore() + betMoney);
                } else {
                    player.setCurrentScore(player.getCurrentScore() + betMoney);
                    //下注减去的钱要加回来
                    player.getEntity().setMoney(player.getEntity().getMoney() + 2 * betMoney);


                    bankerPlayer.setCurrentScore(bankerPlayer.getCurrentScore() - betMoney);

                }
            }
            if (player.getCurrentScore() > 0) {
                //抽水金额
                long cutMoney = (long) (player.getCurrentScore() * TwoEightConfig.cutMoneyPre);
                //玩家赢钱抽水
                player.setReturnScore(player.getCurrentScore() - cutMoney);
                player.getEntity().setMoney(player.getEntity().getMoney() - cutMoney);
                //保存抽水金额
                OseeCutMoneyLogEntity entity = new OseeCutMoneyLogEntity();
                entity.setCutMoney(cutMoney);
                entity.setGame(GameEnum.ERBA_GAME.getId());
                entity.setUserId(player.getId());
//                entity.setNickname(player.getUser().getNickname());
//                entity.setBeforeMoney((int) player.getCurrentScore());
                cutMoneyLogMapper.save(entity);
            }


            // 账户明细保存
            OseePlayerTenureLogEntity logEntity = new OseePlayerTenureLogEntity();
            logEntity.setUserId(player.getId());
            logEntity.setNickname(player.getUser().getNickname());
            logEntity.setPreMoney(twoEightRecord.getPlayBeforeMoney());
            logEntity.setChangeMoney(player.getReturnScore());
            if (player.getCurrentScore() < 0)
                logEntity.setReason(ItemChangeReason.ERBA_GANG_LOSE.getId());
            else
                logEntity.setReason(ItemChangeReason.ERBA_GANG_WIN.getId());
            tenureLogMapper.save(logEntity);

            //战绩保存
            twoEightRecord.setMoney(player.getReturnScore());
            twoEightRecord.setPlayAfterMoney(player.getEntity().getMoney());
            //加上庄家点数
            int type = teMessageService.getCardType(cards[3][0], cards[3][1]);
            String scoreType = teMessageService.getScoreType(type);
            twoEightRecord.setCardType(twoEightRecord.getCardType() + "庄家" + scoreType);
            twoEightRecordMapper.save(twoEightRecord);

            //金币保存
            playerMapper.update(player.getEntity());
        }

        long bankReturnScore = 0L;
        long bankScore = bankerPlayer.getCurrentScore();
        if (bankerPlayer.getCurrentScore() > 0) {
            bankReturnScore = (long) (bankerPlayer.getCurrentScore() * (1 - TwoEightConfig.cutMoneyPre));
            //庄家信息保存
            if (!bankerPlayer.isRobot()) {
                bankerPlayer.getEntity().setMoney(bankerPlayer.getEntity().getMoney() + bankReturnScore);
                long SystemGetMoney = bankScore - bankReturnScore;
                //保存抽水金额
                OseeCutMoneyLogEntity entity = new OseeCutMoneyLogEntity();
                entity.setCutMoney(SystemGetMoney);
                entity.setGame(GameEnum.ERBA_GAME.getId());
                entity.setUserId(bankerPlayer.getId());
//                entity.setNickname(bankerPlayer.getUser().getNickname());
//                entity.setBeforeMoney((int) bankScore);
                cutMoneyLogMapper.save(entity);

            }
        } else {
            if (!bankerPlayer.isRobot())
                bankerPlayer.getEntity().setMoney(bankerPlayer.getEntity().getMoney() + bankScore);
        }

        if (!bankerPlayer.isRobot()) {
            // 账户明细保存
            OseePlayerTenureLogEntity logEntity = new OseePlayerTenureLogEntity();
            logEntity.setUserId(bankerPlayer.getId());
            logEntity.setNickname(bankerPlayer.getUser().getNickname());


            //战绩记录保存
            TwoEightRecordLogEntity bankerRecord = new TwoEightRecordLogEntity();
            bankerRecord.setPlayerId(bankerPlayer.getId());
            bankerRecord.setNickname(bankerPlayer.getUser().getNickname());
            bankerRecord.setPlayAfterMoney(bankerPlayer.getEntity().getMoney());
            bankerRecord.setInput(0);
            if (bankScore > 0) {
                bankerRecord.setMoney(bankReturnScore);
                bankerRecord.setPlayBeforeMoney(bankerPlayer.getEntity().getMoney() - bankReturnScore);

                logEntity.setChangeMoney(bankReturnScore);
                logEntity.setReason(ItemChangeReason.ERBA_GANG_WIN.getId());
                logEntity.setPreMoney(bankerPlayer.getEntity().getMoney() - bankReturnScore);
            } else {
                bankerRecord.setPlayBeforeMoney(bankerPlayer.getEntity().getMoney() - bankScore);
                bankerRecord.setMoney(bankScore);

                logEntity.setChangeMoney(bankScore);
                logEntity.setReason(ItemChangeReason.ERBA_GANG_LOSE.getId());
                logEntity.setPreMoney(bankerPlayer.getEntity().getMoney() - bankScore);
            }
            String res = "";
            for (int k = 0; k < 4; k++) {
                int carda = cards[k][0];
                int cardb = cards[k][1];
                String doorType = "";
                switch (k) {
                    case 0:
                        doorType = "顺门";
                        break;
                    case 1:
                        doorType = "天门";
                        break;
                    case 2:
                        doorType = "地门";
                        break;
                    case 3:
                        doorType = "庄家";
                        break;
                }
                int type = teMessageService.getCardType(carda, cardb);
                String scoreType = teMessageService.getScoreType(type);
                res += doorType + scoreType + ";";
            }
            bankerRecord.setCardType(res);
            twoEightRecordMapper.save(bankerRecord);
            playerMapper.update(bankerPlayer.getEntity());
            tenureLogMapper.save(logEntity);


            room.getBetPlayers().add(bankerPlayer);
        } else {
            long resCur = bankScore + getTERobotMoney(TwoEightConfig.RedisTwoEightRobotWinKey);
            RedisHelper.set(TwoEightConfig.RedisTwoEightRobotWinKey, resCur + "");

            long resAll = bankScore + getTERobotMoney(TwoEightConfig.RedisTwoEightRobotHistoryWinKey);
            RedisHelper.set(TwoEightConfig.RedisTwoEightRobotHistoryWinKey, resAll + "");

            long resDaily = bankScore + getTERobotMoney(TwoEightConfig.RedisTwoEightDailyMoney);
            RedisHelper.set(TwoEightConfig.RedisTwoEightDailyMoney, resDaily + "");


        }

        for (TwoEightPlayer player : room.getBetPlayers()) {
            //发送结算响应
            OseeTwoEightMessage.RoundOverResponse.Builder builder = OseeTwoEightMessage.RoundOverResponse.newBuilder();
            builder.setMyScore(player.getCurrentScore());
            long returnScore = 0L;
            if (player.getCurrentScore() > 0) {
                returnScore = player.getReturnScore();
            }
            builder.setMyReturnScore(returnScore);
            builder.setBankerScore(bankScore);
            builder.setBankerReturnScore(bankReturnScore);
            NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_ROUND_OVER_RESPONSE_VALUE,
                    builder.build(), player.getUser());
//            player.roundSet();
            //发送金币更新响应
            teMessageService.sendPlayerMoneyResponse(player);

        }

        //清除上庄列表中不够格的人
        List<TwoEightPlayer> removePlayers = new ArrayList<>();
        for (TwoEightPlayer player : room.getApplyForBanker()) {
            if (player.getEntity().getMoney() < TwoEightRoom.toBeBankerMinMoney)
                removePlayers.add(player);
        }
        for (TwoEightPlayer player : removePlayers) {
            room.getApplyForBanker().remove(player);
            if (room.getBanker().getId() == player.getId()) {
                room.setBanker(null);
            }
        }

        //庄家金币不够 由下面一个人顶上
        if (room.getBanker() == null && room.getApplyForBanker().size() >= 1) {
            room.setBanker(room.getApplyForBanker().get(0));
        }

        //有人申请上庄，机器人下庄
        if (bankerPlayer.isRobot() && room.getApplyForBanker().size() > 1) {
            room.getApplyForBanker().remove(bankerPlayer);
            room.getAllPlayers().remove(bankerPlayer);

            room.setBanker(room.getApplyForBanker().get(0));
        }

        teMessageService.sendRoomPlayerInfoListResponse(room);
        teMessageService.sendBankersInfoResponse(room);

    }


    public void roomClock(TwoEightRoom room) {
        long now = System.currentTimeMillis();
        OseeTwoEightMessage.TERoomStateResponse.Builder stateResponse =
                OseeTwoEightMessage.TERoomStateResponse.newBuilder();
        switch (room.getRoomStatus()) {
            case TwoEightRoomState.NOTBEGIN: {
                //无庄时加入系统庄
                if (room.getBanker() == null && room.getAllPlayers().size() > 0) {

                    //增加系统上庄
                    TwoEightPlayer robot = new TwoEightPlayer();
                    robot.setEntity(new OseePlayerEntity());
                    robot.setRole(1);
                    robot.getEntity().setMoney(30000000);
                    robot.setRobot(true);
                    room.getAllPlayers().add(robot);
                    room.getApplyForBanker().add(robot);
                    room.setBanker(robot);
                    teMessageService.sendRoomPlayerInfoListResponse(room);
                    teMessageService.sendBankersInfoResponse(room);
                }
                if (room.getAllPlayers().size() >= TwoEightRoom.begainMinPlayers && room.getBanker() != null) {
                    //TODO 发送阶段响应
                    logger.info("房间[{}]进入下注阶段", room.getCode());
                    stateResponse.setRoomState(TwoEightRoomState.DOBET);
                    stateResponse.setRemainTime(30);
                    sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_STATE_RESPONSE_VALUE,
                            stateResponse.build(), room);
                    room.setRoomStatus(TwoEightRoomState.DOBET);
                    room.setEnterStateTime(now);

                }
                return;
            }
            case TwoEightRoomState.DOBET: {
                if ((now - room.getEnterStateTime()) / 1000 > 29) { // 下注阶段结束
                    //TODO 发送阶段响应
                    stateResponse.setRoomState(TwoEightRoomState.SHAKEDICE);
                    stateResponse.setRemainTime(5);
                    sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_STATE_RESPONSE_VALUE,
                            stateResponse.build(), room);
                    room.setRoomStatus(TwoEightRoomState.SHAKEDICE);
                    room.setEnterStateTime(now);
                    logger.info("下注阶段结束");
                }
                return;
            }
            case TwoEightRoomState.SHAKEDICE: {
                if (room.getBanker().isRobot()) {
                    if ((now - room.getEnterStateTime()) / 1000 > 2)
                        shakeDice(room.getCode());

                } else if ((now - room.getEnterStateTime()) / 1000 > 4) { // 摇骰子阶段，这个时间是留个玩家点骰子的时间
                    //TODO 发送阶段响应
                    shakeDice(room.getCode());

                }
                return;
            }
            case TwoEightRoomState.GETCARDS: {
                if ((now - room.getEnterStateTime()) / 1000 > 3) { //这个时间是留给骰子显示页面的
                    sendCardResponce(room.getCode());

                    room.setEnterStateTime(System.currentTimeMillis());
                    room.setRoomStatus(TwoEightRoomState.ROUNDOVER);
                    logger.info("进入发牌阶段");
                }

            }
            case TwoEightRoomState.ROUNDOVER: {
                if ((now - room.getEnterStateTime()) / 1000 > 6) { // 结算阶段：这个时间是留给比牌页面的
                    //发送进入结算阶段
                    stateResponse.setRoomState(TwoEightRoomState.ROUNDOVER);
                    stateResponse.setRemainTime(3);
                    sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_STATE_RESPONSE_VALUE,
                            stateResponse.build(), room);
                    roundOver(room);
                    room.setRoomStatus(TwoEightRoomState.NEXTROUND);
                    room.setEnterStateTime(System.currentTimeMillis());
                    logger.info("进入结算阶段");

                }
                return;
            }
            case TwoEightRoomState.NEXTROUND: {
                if ((now - room.getEnterStateTime()) / 1000 > 2) {  //这个时间是留个结算页面 的
                    beginNextRound(room, stateResponse);
                    logger.info("开始下一局");

                }
                return;
            }


        }
    }

    private void beginNextRound(TwoEightRoom room, OseeTwoEightMessage.TERoomStateResponse.Builder stateResponse) {
        //庄家更新
        TwoEightPlayer banker = room.getBanker();
        List<TwoEightPlayer> applyForBanker = room.getApplyForBanker();
        //清理房间玩家及信息(把强制退出玩家移除房间)
        for (BaseGamePlayer player : room.getAllPlayers()) {
            TwoEightPlayer twoEightPlayer = (TwoEightPlayer) player;
            if (!twoEightPlayer.isInRoom()) {
                quitRoom(twoEightPlayer.getUser());
            } else {
                twoEightPlayer.roundSet();
            }

        }

        //两次未下注的玩家自动移除房间
        for (BaseGamePlayer player : room.getGamePlayers()) {
            if (player != null) {
                TwoEightPlayer twoEightPlayer = (TwoEightPlayer) player;
                if (!room.getBetPlayers().contains(twoEightPlayer)) {
                    twoEightPlayer.setNotBetNum(twoEightPlayer.getNotBetNum() + 1);
                }
                if (twoEightPlayer.getNotBetNum() >= 3) {
                    if (room.getBanker().isRobot() || twoEightPlayer.getId() != room.getBanker().getId())
                        quitRoom(twoEightPlayer.getUser());
                }
            }

        }
        if (banker != null) {
            //庄家是否申请了下庄
            if (banker.getRole() == 0) {
                applyForBanker.remove(banker);
                if (applyForBanker.size() > 0) {
                    room.setBanker(applyForBanker.get(0));
                } else {
                    //没有庄了进入等待阶段
                    room.setBanker(null);
                }
            }
        } else {
            if (applyForBanker.size() > 0) {
                room.setBanker(applyForBanker.get(0));
            }
        }
        teMessageService.sendBankersInfoResponse(room);
        teMessageService.sendRoomPlayerInfoListResponse(room);


        room.roundSet();

//        teMessageService.sendRoomPlayerInfoListResponse(room);
//        teMessageService.sendBankersInfoResponse(room);
        stateResponse.setRoomState(TwoEightRoomState.NOTBEGIN);
        sendRoomMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_STATE_RESPONSE_VALUE,
                stateResponse.build(), room);

        room.setRoomStatus(TwoEightRoomState.NOTBEGIN);
        room.setEnterStateTime(System.currentTimeMillis());
    }

    public void cancelBanker(ServerUser user) {
        TwoEightPlayer player = GameContainer.getPlayerById(user.getId());
        TwoEightRoom room = GameContainer.getGameRoomByCode(player.getRoomCode());
        OseeTwoEightMessage.TERoomCancelBankerResponse.Builder builder =
                OseeTwoEightMessage.TERoomCancelBankerResponse.newBuilder();
        if (room.getRoomStatus() == TwoEightRoomState.NOTBEGIN || room.getBanker().getId() != player.getId()) {
            room.getApplyForBanker().remove(player);
            if (room.getRoomStatus() == TwoEightRoomState.NOTBEGIN)
                room.setBanker(null);
            teMessageService.sendBankersInfoResponse(room, player.getUser());
            builder.setIsSuccess(0);
        } else {
            player.setRole(0);//如果是庄家 下局生效，对局结束后生效
            builder.setIsSuccess(1);
        }


        NetManager.sendMessage(OseeTwoEightMessage.TwoEightMessageCode.S_C_TEROOM_CANCEL_BANKER_RESPONSE_VALUE,
                builder.build(), user);

    }

    public void reconnect(TwoEightRoom gameRoom, TwoEightPlayer gamePlayer) {
        gamePlayer.setInRoom(true);

        teMessageService.sendRoomInfoResponse(gameRoom, gamePlayer);
        teMessageService.sendRoomPlayerInfoListResponse(gameRoom, gamePlayer);
//        teMessageService.sendRoomBetInfoResponse(gameRoom,gamePlayer);
        teMessageService.sendAllRoundWinInfo(gameRoom);
        teMessageService.sendBankersInfoResponse(gameRoom, gamePlayer.getUser());

        teMessageService.sendReconnectInfoResponse(gameRoom, gamePlayer);//发送重连相关数据


    }


}
