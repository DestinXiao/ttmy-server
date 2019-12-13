package com.maple.game.osee.manager.fightten;

import com.maple.engine.container.UserContainer;
import com.maple.engine.data.ServerUser;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.dao.log.entity.OseeFighttenRecordLogEntity;
import com.maple.game.osee.dao.log.entity.TenChallengeRankingLogEntity;
import com.maple.game.osee.dao.log.mapper.TenChallengeRankingLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fightten.FightTenPlayer;
import com.maple.game.osee.entity.fightten.FightTenRoom;
import com.maple.game.osee.entity.fightten.challenge.FightTenChallengePlayer;
import com.maple.game.osee.entity.fightten.challenge.FightTenChallengeRoom;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.fightten.OseeFightTenMessage;
import com.maple.game.osee.proto.fightten.TtmyFightTenChallengeMessage;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.data.BaseGameRoom;
import com.maple.network.manager.NetManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static com.maple.game.osee.entity.fightten.FightTenRoom.MIN_PLAYER_NUM;

/**
 * 拼十挑战赛管理类
 *
 * @author Junlong
 */
@Component
public class FightTenChallengeManager extends FightTenManager {

    @Autowired
    private TenChallengeRankingLogMapper rankingLogMapper;

    /**
     * 进入房间金币限制 100万、300万、500万、1000万 后台可控
     */
    public static long[] moneyLimit = {1000000, 3000000, 5000000, 10000000};

    /**
     * 10局扣除3颗钻石
     * 20局则扣除5颗钻石
     */
    private static final int[][] roundFee = {
            {10, 3},
            {20, 5}
    };

    /**
     * 匹配队列 map里面是玩家和匹配结束时间
     */
    public static ConcurrentLinkedQueue<Map<ServerUser, LocalDateTime>> matchQueue = new ConcurrentLinkedQueue<>();

    /**
     * 创建挑战赛房间信息数据
     */
    private TtmyFightTenChallengeMessage.TenChallengeRoomInfoProto createRoomInfoProto(FightTenChallengeRoom room) {
        TtmyFightTenChallengeMessage.TenChallengeRoomInfoProto.Builder builder = TtmyFightTenChallengeMessage.TenChallengeRoomInfoProto.newBuilder();
        builder.setRoomCode(room.getCode());
        builder.setRoundNow(room.getRoundNow());
        builder.setRoundTotal(room.getRoundTotal());
        builder.setFeeType(room.getFeeType());
        builder.setMoneyLimit(room.getEnterMoneyLimit());
        builder.setOwnerId(room.getOwnerId());
        if (room.isPrivateRoom()) {
            builder.setState(0); // 房主私密房间，不可加入
        } else {
            if (room.getRoomState() <= FightTenRoom.RoomState.READY.getIndex()) {
                builder.setState(1);
            } else {
                builder.setState(2);
            }
        }
        // 房间玩家信息
        for (BaseGamePlayer baseGamePlayer : room.getGamePlayers()) {
            if (baseGamePlayer != null) {
                builder.addRoomPlayers(createPlayerInfoProto((FightTenChallengePlayer) baseGamePlayer));
            }
        }
        return builder.build();
    }

    /**
     * 发送房间信息给玩家
     */
    @Override
    protected void sendRoomInfoResponse(FightTenRoom room, FightTenPlayer player) {
        if (room != null) {
            TtmyFightTenChallengeMessage.TenChallengeRoomInfoResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeRoomInfoResponse.newBuilder();
            builder.setRoomInfo(createRoomInfoProto((FightTenChallengeRoom) room));
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_ROOM_INFO_RESPONSE_VALUE, builder, player.getUser());
        }
    }

    /**
     * 玩家加入房间内
     */
    @Override
    public void addRoomPlayer(FightTenRoom room, FightTenPlayer player) {
        synchronized (room) {
            // 更新玩家上次操作时间
            player.setLastOptionTime(System.currentTimeMillis());

            // 发送房间信息
            sendRoomInfoResponse(room, player);
            // 发送自己的信息给房间的其他玩家
            sendRoomPlayerInfoResponse(room, player);
            // 发送房间已有玩家的响应
            sendRoomPlayerInfoListResponse(room, player);

            // 改变房间状态
            if (room.getRoomState() != FightTenRoom.RoomState.READY.getIndex()) { // 进入房间不刷新倒计时
                room.setRoomState(FightTenRoom.RoomState.READY.getIndex());
            }
            // 发送房间状态改变响应
            sendChangeRoomStateResponse(room);
        }
    }

    /**
     * 发送玩家剩余的拼十挑战次数
     */
    public void sendRestChallengeTimes(ServerUser user) {
        long times = PlayerManager.getPlayerTenChallengeTimes(user);
        TtmyFightTenChallengeMessage.TenChallengeRestTimeResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeRestTimeResponse.newBuilder();
        builder.setTimes(times);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_REST_TIMES_RESPONSE_VALUE, builder, user);
    }

    // ************************ 以下是逻辑部分 ****************************

    /**
     * 钻石兑换挑战次数 钻石:次数 = 1:10
     */
    public void exchangeTimes(ServerUser user, int diamondNum) {
        if (diamondNum <= 0) {
            return;
        }
        if (!PlayerManager.checkItem(user, ItemId.DIAMOND, diamondNum)) {
            NetManager.sendErrorMessageToClient("钻石不足！", user);
            return;
        }
        // 扣除钻石
        PlayerManager.addItem(user, ItemId.DIAMOND, -diamondNum, ItemChangeReason.USE_ITEM, true);
        // 兑换的次数
        int times = diamondNum * 10;
        // 玩家挑战次数增加
        PlayerManager.addPlayerTenChallengeTimes(user, times);
        TtmyFightTenChallengeMessage.TenChallengeExchangeTimeResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeExchangeTimeResponse.newBuilder();
        builder.setTimes((int) PlayerManager.getPlayerTenChallengeTimes(user));
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_EXCHANGE_TIMES_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送房间列表
     */
    public void sendRoomList(ServerUser user, int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1) {
            return;
        }
        List<FightTenChallengeRoom> gameRooms = GameContainer.getGameRooms(FightTenChallengeRoom.class);
        // 分页获取数据
        List<FightTenChallengeRoom> roomList = gameRooms
                .stream()
                .sorted(Comparator.comparing(FightTenChallengeRoom::getRoomState))
                .skip((pageNo - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());

        TtmyFightTenChallengeMessage.TenChallengeRoomListResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeRoomListResponse.newBuilder();
        builder.setPageNo(pageNo);
        builder.setTotalCount(gameRooms.size());
        for (FightTenChallengeRoom room : roomList) {
            builder.addRoomList(createRoomInfoProto(room));
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_ROOM_LIST_RESPONSE_VALUE, builder, user);
    }

    /**
     * 创建挑战赛房间
     */
    public void createRoom(ServerUser user, int moneyLimitIndex, int feeType, int roundIndex, boolean privateRoom) {
        if (PlayerManager.getItemNum(user, ItemId.MONTH_CARD) <= 0) {
            NetManager.sendErrorMessageToClient("月卡会员才能进行拼十挑战赛", user);
            return;
        }
        if (moneyLimitIndex < 0 || moneyLimitIndex >= moneyLimit.length ||
                feeType < 0 || feeType > 1 ||
                roundIndex < 0 || roundIndex >= roundFee.length) {
            return;
        }

        if (GameContainer.getGameRoomByPlayerId(user.getId()) != null) {
            NetManager.sendErrorMessageToClient("你已在房间中！", user);
            return;
        }

        // 房主支付
        if (feeType == 0) {
            // 判断钻石是否足够
            if (!PlayerManager.checkItem(user, ItemId.DIAMOND, roundFee[roundIndex][1])) {
                NetManager.sendErrorMessageToClient("账户钻石不足，创建房间失败！", user);
                return;
            }
        } else { // 挑战模式
            // 判断挑战次数是否足够
            long challengeTimes = PlayerManager.getPlayerTenChallengeTimes(user);
            if (challengeTimes < 1) {
                NetManager.sendErrorMessageToClient("挑战次数不足，创建房间失败！", user);
                return;
            }
        }

        // 判断金币是否足够
        long limit = moneyLimit[moneyLimitIndex];
        if (!PlayerManager.checkItem(user, ItemId.MONEY, limit)) {
            NetManager.sendErrorMessageToClient("您携带的金币不足，创建房间失败！", user);
            return;
        }

        // 创建房间
        FightTenChallengeRoom gameRoom = GameContainer.createGameRoom(FightTenChallengeRoom.class, FightTenChallengeRoom.MAX_PLAYER_NUM);
        if (gameRoom == null) {
            NetManager.sendErrorMessageToClient("创建房间失败！", user);
            return;
        }
        gameRoom.setOwnerId(user.getId());
        gameRoom.setEnterMoneyLimit(limit);
        gameRoom.setFeeType(feeType);
        gameRoom.setPrivateRoom(privateRoom);
        if (feeType == 0) { // 房主承包房间才有对局局数
            gameRoom.setRoundNow(0);
            gameRoom.setRoundTotal(roundFee[roundIndex][0]);
            gameRoom.setRoundIndex(roundIndex);
        }
        // 设置房间状态
        gameRoom.setRoomState(FightTenRoom.RoomState.NONE.getIndex());
        // 房间最高下注限制
        gameRoom.setMaxBetMoney(moneyLimit[moneyLimitIndex]);

        // 创建房间游戏玩家
        FightTenChallengePlayer gamePlayer = GameContainer.createGamePlayer(gameRoom, user, FightTenChallengePlayer.class);
        // 加入并进入房间
        addRoomPlayer(gameRoom, gamePlayer);
    }

    /**
     * 申请加入房间
     */
    public void joinRoom(ServerUser user, int roomCode) {
        if (PlayerManager.getItemNum(user, ItemId.MONTH_CARD) <= 0) {
            NetManager.sendErrorMessageToClient("月卡会员才能进行拼十挑战赛", user);
            return;
        }
        if (GameContainer.getGameRoomByPlayerId(user.getId()) != null) {
            NetManager.sendErrorMessageToClient("你已在房间中！", user);
            return;
        }
        BaseGameRoom baseGameRoom = GameContainer.getGameRoomByCode(roomCode);
        if (!(baseGameRoom instanceof FightTenChallengeRoom)) {
            NetManager.sendErrorMessageToClient("房间不存在！", user);
            return;
        }
        FightTenChallengeRoom gameRoom = (FightTenChallengeRoom) baseGameRoom;
//        if (!gameRoom.isPrivateRoom()) {
//            NetManager.sendErrorMessageToClient("房间不是亲友房！", user);
//            return;
//        }
        if (gameRoom.getPlayerSize() >= gameRoom.getMaxSize()) {
            NetManager.sendErrorMessageToClient("房间人数已满！", user);
            return;
        }

        // 判断挑战次数是否足够
        long challengeTimes = PlayerManager.getPlayerTenChallengeTimes(user);
        if (gameRoom.getFeeType() == 1 && challengeTimes < 1) { // 挑战房才消耗挑战次数
            TtmyFightTenChallengeMessage.TenChallengeJoinRoomResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeJoinRoomResponse.newBuilder();
            builder.setResult(1);
            builder.setMsg("当前挑战次数不足，是否立即兑换？");
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_JOIN_ROOM_RESPONSE_VALUE, builder, user);
            return;
        }

        if (!PlayerManager.checkItem(user, ItemId.MONEY, gameRoom.getEnterMoneyLimit())) {
            TtmyFightTenChallengeMessage.TenChallengeJoinRoomResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeJoinRoomResponse.newBuilder();
            builder.setResult(2);
            builder.setMsg("您的金币不足，是否立即前往商城充值？");
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_JOIN_ROOM_RESPONSE_VALUE, builder, user);
            return;
        }

        // 创建房间游戏玩家
        FightTenChallengePlayer gamePlayer = GameContainer.createGamePlayer(gameRoom, user, FightTenChallengePlayer.class);
        // 加入并进入房间
        addRoomPlayer(gameRoom, gamePlayer);
    }

    /**
     * 发牌
     */
    @Override
    protected void dispatchCard(FightTenRoom room) {
        List<Integer> cardData = new ArrayList<>(Arrays.asList(CARD_DATA_ARRAY));
        // 打乱牌组
        Collections.shuffle(cardData);
        for (BaseGamePlayer gamePlayer : room.getGamePlayers()) {
            if (gamePlayer != null) {
                FightTenChallengePlayer player = (FightTenChallengePlayer) gamePlayer;
                for (int i = 0; i < 5; i++) {
                    player.getCards().add(cardData.remove(0));
                }
                // 给玩家发送牌信息
                // 先只给玩家发前面4张名牌
                OseeFightTenMessage.TenDispatchCardResponse.Builder builder = OseeFightTenMessage.TenDispatchCardResponse.newBuilder();
                builder.setSeat(player.getSeat());
                builder.setPlayerId(player.getId());
                builder.addAllCards(new ArrayList<>(player.getCards().subList(0, player.getCards().size() - 1)));
                NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_OSEE_TEN_DISPATCH_CARD_RESPONSE_VALUE, builder, player.getUser());
            }
        }

        // 房间对局或挑战次数扣除
        FightTenChallengeRoom challengeRoom = (FightTenChallengeRoom) room;
        challengeRoom.setPlayed(true); // 设置房间为已经对局过
        challengeRoom.setRoundNow(challengeRoom.getRoundNow() + 1); // 对局数加一
        if (challengeRoom.getFeeType() == 0) { // 房主承包扣除房主钻石
            if (challengeRoom.getRoundNow() == 1) { // 第一局才扣
                int cost = roundFee[challengeRoom.getRoundIndex()][1];
                PlayerManager.addItem(UserContainer.getUserById(challengeRoom.getOwnerId()), ItemId.DIAMOND, -cost, null, true);
            }
        } else {  // 挑战房扣除挑战次数
            for (BaseGamePlayer gamePlayer : challengeRoom.getGamePlayers()) {
                if (gamePlayer != null) {
                    PlayerManager.addPlayerTenChallengeTimes(gamePlayer.getUser(), -1);
                }
            }
        }
    }

    /**
     * 对局结束
     */
    @Override
    protected void roundOver(FightTenRoom room) {
        FightTenChallengePlayer banker = (FightTenChallengePlayer) room.getBanker();
        // 庄家牌型
        int bankerCardType = FightTenCardManager.getCardType(banker.getCards());
        banker.setCardType(bankerCardType);
        OseeFightTenMessage.TenRoundOverResponse.Builder roundOverResponse = OseeFightTenMessage.TenRoundOverResponse.newBuilder();
        // 结算时庄家的数据
        OseeFightTenMessage.TenRoundOverPlayerDataProto.Builder bankerData = OseeFightTenMessage.TenRoundOverPlayerDataProto.newBuilder();
        bankerData.setPlayerId(banker.getId());
        bankerData.setSeat(banker.getSeat());
        bankerData.addAllCards(banker.getCards());
        bankerData.setCardType(bankerCardType);
        // 庄家输赢的总金币数
        long bankerWinMoney = 0;
        for (BaseGamePlayer baseGamePlayer : room.getGamePlayers()) {
            if (baseGamePlayer == null) {
                continue;
            }
            if (baseGamePlayer.getId() == banker.getId()) { // 庄家跳过
                continue;
            }
            // 闲家
            FightTenChallengePlayer freePlayer = (FightTenChallengePlayer) baseGamePlayer;

            // 闲家牌型
            int freeCardType = FightTenCardManager.getCardType(freePlayer.getCards());
            freePlayer.setCardType(freeCardType);

            // 闲家结算时数据
            OseeFightTenMessage.TenRoundOverPlayerDataProto.Builder playerData = OseeFightTenMessage.TenRoundOverPlayerDataProto.newBuilder();
            playerData.setPlayerId(baseGamePlayer.getId());
            playerData.setSeat(baseGamePlayer.getSeat());
            playerData.addAllCards(freePlayer.getCards());
            playerData.setCardType(freeCardType);
            // 闲家是否赢了
            boolean freeWin = false;
            int compare = FightTenCardManager.compare(banker.getCards(), freePlayer.getCards());
            if (compare < 0) { // 前者小于后者，即闲家牌大
                freeWin = true;
            }
            playerData.setWin(freeWin);

            String cardType;
            long freeWinMoney;
            // 闲家下注金额
            long freeBetMoney = freePlayer.getBetMoney();
            if (freeWin) { // 如果闲家赢了
                freePlayer.setWin(1);
                // 结算 = 闲家下注金额 * 闲家牌倍数
                freeWinMoney = freeBetMoney * FightTenCardManager.getCardTypeMultiple(freeCardType);// *
                // banker.getFightMultiple()
                // 抢庄默认为4倍
                long bankerMoney = banker.getMoney();
                if (bankerMoney < freeWinMoney) { // 携带的金币不够赔付就扣除能够扣的
                    freeWinMoney = bankerMoney < 0 ? 0 : bankerMoney;
                }
                // 播报
//                if (freeWinMoney > 1000000) {
//                    cardType = check(freeCardType);
//                    String info = String.format(
//                            AutoWanderSubtitle.TEMPLATES[ThreadLocalRandom.current().nextInt(4, 6)],
//                            freePlayer.getUser().getNickname(), cardType, freeWinMoney / 10000
//                    );
//                    sendWanderSubtitle(info);
//                }
                playerData.setWinMoney(freeWinMoney);
                bankerWinMoney -= freeWinMoney;
                PlayerManager.addItem(freePlayer.getUser(), ItemId.MONEY, freeWinMoney, ItemChangeReason.FIGHT_TEN_CHALLENGE_WIN, true); // 闲家加钱
                PlayerManager.addItem(banker.getUser(), ItemId.MONEY, -freeWinMoney, ItemChangeReason.FIGHT_TEN_CHALLENGE_LOSE, true); // 庄家减钱
            } else { // 闲家输了，庄家赢了
                freePlayer.setWin(0);
                // 结算 = 闲家下注金额 * 庄家牌倍数 * 庄家抢庄倍数
                freeWinMoney = freeBetMoney * FightTenCardManager.getCardTypeMultiple(bankerCardType);// *
                // banker.getFightMultiple()
                // 抢庄默认为4倍
                long freePlayerMoney = freePlayer.getMoney();
                if (freePlayerMoney < freeWinMoney) { // 携带的金币不够赔付就扣除能够扣的
                    freeWinMoney = freePlayerMoney < 0 ? 0 : freePlayerMoney;
                }
                // 播报
//                if (freeWinMoney > 1000000) {
//                    cardType = check(bankerCardType);
//                    String info = String.format(
//                            AutoWanderSubtitle.TEMPLATES[ThreadLocalRandom.current().nextInt(4, 6)],
//                            freePlayer.getUser().getNickname(), cardType, freeWinMoney / 10000
//                    );
//                    sendWanderSubtitle(info);
//                }

                playerData.setWinMoney(-freeWinMoney);
                bankerWinMoney += freeWinMoney;
                PlayerManager.addItem(freePlayer.getUser(), ItemId.MONEY, -freeWinMoney, ItemChangeReason.FIGHT_TEN_CHALLENGE_LOSE, true); // 闲家减钱
                PlayerManager.addItem(banker.getUser(), ItemId.MONEY, freeWinMoney, ItemChangeReason.FIGHT_TEN_CHALLENGE_WIN, true); // 庄家加钱
            }
            playerData.setMyMoney(freePlayer.getMoney());
            roundOverResponse.addPlayerData(playerData);
            // 记录该闲家单局输赢的钱
            freePlayer.setWinMoney(playerData.getWinMoney());

            // 开始记录闲家玩家排行榜
            if (freePlayer.getWinMoney() > 0) {
                TenChallengeRankingLogEntity rankingLogEntity = rankingLogMapper.getByUserId(freePlayer.getId());
                if (rankingLogEntity != null) {
                    if (rankingLogEntity.getScore() < freePlayer.getWinMoney()) { // 记录的数据小于当前就更新
                        rankingLogEntity.setScore(freePlayer.getWinMoney());
                        rankingLogEntity.setUpdateTime(new Date());
                        rankingLogMapper.updateById(rankingLogEntity);
                    }
                } else { // 没有记录就新建一个
                    TenChallengeRankingLogEntity logEntity = new TenChallengeRankingLogEntity();
                    ServerUser user = freePlayer.getUser();
                    logEntity.setUserId(user.getId());
                    logEntity.setNickname(user.getNickname());
                    logEntity.setHeadIndex(user.getEntity().getHeadIndex());
                    logEntity.setHeadUrl(user.getEntity().getHeadUrl());
                    logEntity.setScore(freePlayer.getWinMoney());
                    rankingLogMapper.save(logEntity);
                }
            }

            // 记录闲家日志
            OseeFighttenRecordLogEntity log = new OseeFighttenRecordLogEntity();
            log.setPlayerId(freePlayer.getId());
            if (freeWin) { // 如果赢了
                log.setMoney(freeWinMoney);
            } else { // 输了
                log.setMoney(-freeWinMoney);
            }
            cardType = check(freeCardType);
            log.setCardType(cardType);
            log.setInput(freeBetMoney);
            log.setRate(FightTenCardManager.getCardTypeMultiple(freeCardType));// 闲家牌倍数
            log.setNickname(freePlayer.getUser().getNickname());
            log.setPlayBeforeMoney(freePlayer.getMoney() - log.getMoney());
            log.setPlayAfterMoney(freePlayer.getMoney());
            logMapper.save(log);
        }

        // 庄家日志记录
        String cardType = check(bankerCardType);
        OseeFighttenRecordLogEntity log = new OseeFighttenRecordLogEntity(); // 日志记录
        log.setPlayerId(banker.getId());
        log.setNickname(banker.getUser().getNickname());
        log.setInput(0);// 庄家下注是0
        log.setRate(FightTenCardManager.getCardTypeMultiple(bankerCardType)); // 庄家牌倍数
        log.setCardType(cardType);
        log.setMoney(bankerWinMoney);// 输赢总金币
        log.setPlayBeforeMoney(banker.getMoney() - bankerWinMoney);
        log.setPlayAfterMoney(banker.getMoney());
        logMapper.save(log);

        bankerData.setMyMoney(banker.getMoney());
        bankerData.setWinMoney(bankerWinMoney);
        roundOverResponse.addPlayerData(bankerData);
        // 记录庄家单局输赢的钱
        banker.setWinMoney(bankerWinMoney);

        // 开始记录庄家玩家排行榜
        if (banker.getWinMoney() > 0) {
            TenChallengeRankingLogEntity rankingLogEntity = rankingLogMapper.getByUserId(banker.getId());
            if (rankingLogEntity != null) {
                if (rankingLogEntity.getScore() < banker.getWinMoney()) { // 记录的数据小于当前就更新
                    rankingLogEntity.setScore(banker.getWinMoney());
                    rankingLogEntity.setUpdateTime(new Date());
                    rankingLogMapper.updateById(rankingLogEntity);
                }
            } else { // 没有记录就新建一个
                TenChallengeRankingLogEntity logEntity = new TenChallengeRankingLogEntity();
                ServerUser user = banker.getUser();
                logEntity.setUserId(user.getId());
                logEntity.setNickname(user.getNickname());
                logEntity.setHeadIndex(user.getEntity().getHeadIndex());
                logEntity.setHeadUrl(user.getEntity().getHeadUrl());
                logEntity.setScore(banker.getWinMoney());
                rankingLogMapper.save(logEntity);
            }
        }

        // 广播房间结算信息
        sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_ROUND_OVER_RESPONSE_VALUE, roundOverResponse);
    }

    /**
     * 退出房间
     *
     * @param user           玩家
     * @param checkForDelete 是否检查删除房间
     * @return 是否离开了房间
     */
    @Override
    public boolean leaveRoom(ServerUser user, boolean checkForDelete) {
        BaseGamePlayer gamePlayer = GameContainer.getPlayerById(user.getId());
        if (gamePlayer == null) {
            logger.error("玩家离开拼十挑战赛房间失败：player为空！");
            return false;
        }
        BaseGameRoom gameRoom = GameContainer.getGameRoomByCode(gamePlayer.getRoomCode());
        if (gameRoom == null) {
            logger.error("玩家离开拼十挑战赛房间失败：room为空！");
            return false;
        }
        if (gameRoom instanceof FightTenChallengeRoom && gamePlayer instanceof FightTenChallengePlayer) {
            FightTenChallengeRoom room = (FightTenChallengeRoom) gameRoom;
            FightTenChallengePlayer player = (FightTenChallengePlayer) gamePlayer;
            // 设置玩家上次操作时间
            player.setLastOptionTime(System.currentTimeMillis());
            int roomState = room.getRoomState();
            // 只有在未准备阶段或准备阶段退出房间
            if (roomState != FightTenRoom.RoomState.NONE.getIndex()
                    && roomState != FightTenRoom.RoomState.READY.getIndex()
                    && roomState != FightTenRoom.RoomState.OVER.getIndex()) {
                NetManager.sendErrorMessageToClient("对局已开始，不能离开房间！", user);
                return false;
            }
            TtmyFightTenChallengeMessage.TenChallengeLeaveRoomResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeLeaveRoomResponse.newBuilder();
            builder.setPlayerId(user.getId());
            // 广播给房间内每一位玩家
            sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_LEAVE_ROOM_RESPONSE_VALUE, builder);

            // 是房主退出房间就要删除房间
            if (room.getFeeType() == 0 &&
                    room.getOwnerId() == user.getId()
            ) {
                for (BaseGamePlayer baseGamePlayer : room.getGamePlayers()) {
                    if (baseGamePlayer == null) {
                        continue;
                    }
                    if (baseGamePlayer.getId() != room.getOwnerId()) {
                        NetManager.sendHintMessageToClient("房主离开房间，房间被解散！", baseGamePlayer.getUser());
                    }
                    // 广播给房间内每一位玩家退房信息
                    sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_LEAVE_ROOM_RESPONSE_VALUE, OseeFightTenMessage.TenLeaveRoomResponse.newBuilder().setPlayerId(baseGamePlayer.getId()));
                }
                GameContainer.removeGameRoom(room); // 删除房间
                return true;
            }
        }

        // 从房间内删除玩家
        GameContainer.removeGamePlayer(gameRoom, gamePlayer.getSeat());

        // 判断是否还有玩家，没有就要解散房间
        if (gameRoom.getPlayerSize() <= 0) {
            // 解散删除房间
            GameContainer.removeGameRoom(gameRoom);
            return true;
        }
        return true;
    }

    /**
     * 房主踢人
     */
    public void kickPlayer(ServerUser user, long playerId) {
        FightTenChallengeRoom room = GameContainer.getGameRoomByPlayerId(user.getId());
        if (room == null) {
            return;
        }
        if (playerId == user.getId()) {
            return; // 自己不能踢自己
        }
        if (room.getOwnerId() != user.getId()) {
            NetManager.sendErrorMessageToClient("房主才有踢人的权限！", user);
            return;
        }
        BaseGamePlayer kickPlayer = GameContainer.getPlayerById(playerId);
        if (kickPlayer == null || kickPlayer.getRoomCode() != room.getCode()) {
            NetManager.sendErrorMessageToClient("该玩家已不在房间内！", user);
            return;
        }
        // 移除成功与否都通知，要把玩家从界面移除
        TtmyFightTenChallengeMessage.TenChallengeLeaveRoomResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeLeaveRoomResponse.newBuilder();
        builder.setPlayerId(playerId);
        // 广播给房间内每一位玩家
        sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_LEAVE_ROOM_RESPONSE_VALUE, builder);
        NetManager.sendHintMessageToClient("你已被房主移出房间！", kickPlayer.getUser());
        GameContainer.removeGamePlayer(room, kickPlayer.getSeat());
    }

    /**
     * 检查玩家是否要被请离房间
     */
    @Override
    public void checkPlayerLeaveRoom(FightTenRoom fightTenRoom) {
        FightTenChallengeRoom challengeRoom = (FightTenChallengeRoom) fightTenRoom;
        BaseGamePlayer[] gamePlayers = challengeRoom.getGamePlayers();
        for (BaseGamePlayer baseGamePlayer : gamePlayers) {
            if (baseGamePlayer == null) {
                continue;
            }
            FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
            if (!tenPlayer.getUser().isOnline()) { // 玩家不在线也要踢出玩家
                leaveRoom(tenPlayer.getUser(), true);
                continue;
            }
            // 据上次操作时长
//            long optionTime = System.currentTimeMillis() - tenPlayer.getLastOptionTime();
//            if (optionTime / 1000 >= LEAVE_ROOM_TIME && tenPlayer.getUser().isOnline()) {
//                NetManager.sendErrorMessageToClient("您长时间未操作，已被移出房间！", tenPlayer.getUser());
//                leaveRoom(tenPlayer.getUser(), true);
//                continue;
//            }
            if (challengeRoom.getFeeType() == 1) { // 挑战房判断挑战次数是否足够
                if (PlayerManager.getPlayerTenChallengeTimes(baseGamePlayer.getUser()) < 1) { // 挑战次数不足
                    NetManager.sendErrorMessageToClient("您的挑战次数不足，已被移出房间！", tenPlayer.getUser());
                    leaveRoom(tenPlayer.getUser(), false);
                    continue;
                }
            }
            long enterMoney = challengeRoom.getEnterMoneyLimit();
            if (tenPlayer.getMoney() < enterMoney) { // 玩家金币不够入场金币也要被请离
                NetManager.sendErrorMessageToClient("您的金币数量低于房间要求，已被移出房间！", tenPlayer.getUser());
                leaveRoom(tenPlayer.getUser(), true);
            }
        }
    }

    /**
     * 处理拼十的房间状态
     */
    @Override
    public void dealRoomState(FightTenRoom room) {
        if (room == null) {
            return;
        }
        FightTenChallengeRoom challengeRoom = (FightTenChallengeRoom) room;
        if (challengeRoom.getRoomState() == FightTenRoom.RoomState.READY.getIndex()
                && !challengeRoom.isPlayed()
                && challengeRoom.getCreateTime().plusMinutes(20).isBefore(LocalDateTime.now()) // 当前时间超过了房间创建时间20分钟
        ) { // 房间20分钟未玩过就自动解散
            for (BaseGamePlayer baseGamePlayer : room.getGamePlayers()) {
                if (baseGamePlayer == null) {
                    continue;
                }
                NetManager.sendErrorMessageToClient("房间20分钟未开局，自动解散！", baseGamePlayer.getUser());
                leaveRoom(baseGamePlayer.getUser(), false);
            }
            return;
        }
        Integer roomState = room.getRoomState();
        long currentTime = System.currentTimeMillis() / 1000;
        Long enterStateTime = room.getEnterStateTime();
        if (roomState == FightTenRoom.RoomState.READY.getIndex()) { // 准备阶段
            if (currentTime >= enterStateTime + FightTenRoom.RoomState.READY.getTime()) { // 当前时间大于阶段持续时间
                checkPlayerLeaveRoom(room);
                if (room.getPlayerSize() >= FightTenRoom.MIN_PLAYER_NUM) { // 人数大于最少人数就检测是否可以开始对局
                    // 切到发牌阶段
                    room.setRoomState(FightTenRoom.RoomState.DISPATCH_CARD.getIndex());
                    // 发送房间状态改变响应
                    sendChangeRoomStateResponse(room);
                    // 对局开始
                    dispatchCard(room);
                } else {
                    // 切到无状态
                    room.setRoomState(FightTenRoom.RoomState.NONE.getIndex());
                    // 发送房间状态改变响应
                    sendChangeRoomStateResponse(room);
                    // 房间数据重置
                    room.roundReset();
                }
            }
        } else if (roomState == FightTenRoom.RoomState.DISPATCH_CARD.getIndex()) { // 发牌阶段
            if (currentTime >= enterStateTime + FightTenRoom.RoomState.DISPATCH_CARD.getTime()) {
                // 切到抢庄阶段
                room.setRoomState(FightTenRoom.RoomState.FIGHT_BANKER.getIndex());
                // 发送房间状态改变响应
                sendChangeRoomStateResponse(room);
            }
        } else if (roomState == FightTenRoom.RoomState.FIGHT_BANKER.getIndex()) { // 抢庄
            if (currentTime >= enterStateTime + FightTenRoom.RoomState.FIGHT_BANKER.getTime()) {
                // 抢庄结束选择庄家
                selectBanker(room);

                OseeFightTenMessage.TenSelectBankerResponse.Builder builder = OseeFightTenMessage.TenSelectBankerResponse.newBuilder();
                builder.setBankerId(room.getBanker().getId());
                builder.setBankerFightMultiple(room.getBanker().getFightMultiple());
                builder.addAllRandomPlayerIdList(room.getFightBankerRandomPlayerIdList());
                sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_SELECT_BANKER_RESPONSE_VALUE, builder); // 广播选庄信息

                if (room.getFightBankerRandomPlayerIdList().size() == 0) { // 不需要随机选庄
                    // 切到下注阶段
                    room.setRoomState(FightTenRoom.RoomState.BET_MONEY.getIndex());
                    // 发送房间状态改变响应
                    sendChangeRoomStateResponse(room);
                    // 计算下注金额列表
                    calcBetMoney(room);
                } else {
                    // 切换到随机选庄动画阶段
                    room.setRoomState(FightTenRoom.RoomState.FIGHT_BANKER_ANI.getIndex());
                    // 发送房间状态改变响应
                    sendChangeRoomStateResponse(room);
                }
            }
        } else if (roomState == FightTenRoom.RoomState.FIGHT_BANKER_ANI.getIndex()) { // 抢庄随机庄家的动画阶段
            if (currentTime >= enterStateTime + FightTenRoom.RoomState.FIGHT_BANKER_ANI.getTime()) {
                // 切到下注阶段
                room.setRoomState(FightTenRoom.RoomState.BET_MONEY.getIndex());
                // 发送房间状态改变响应
                sendChangeRoomStateResponse(room);
                // 计算下注金额列表
                calcBetMoney(room);
            }
        } else if (roomState == FightTenRoom.RoomState.BET_MONEY.getIndex()) { // 下注
            if (currentTime >= enterStateTime + FightTenRoom.RoomState.BET_MONEY.getTime()) {
                // 检查是否所有人都下注了
                for (BaseGamePlayer baseGamePlayer : room.getGamePlayers()) {
                    if (baseGamePlayer == null || baseGamePlayer.getId() == room.getBanker().getId()) { // 庄家不检查下注
                        continue;
                    }
                    FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
                    if (tenPlayer.getBetMoney() == null) { // 还有人未下注就默认设置为25%那档
                        tenPlayer.setBetMoney(tenPlayer.getBetMoneyList().get(0));
                        // 广播给房间内所有玩家默认下注信息
                        OseeFightTenMessage.TenBetMoneyResponse.Builder builder = OseeFightTenMessage.TenBetMoneyResponse.newBuilder();
                        builder.setPlayerId(tenPlayer.getId());
                        builder.setBetMoney(tenPlayer.getBetMoney());
                        sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_BET_MONEY_RESPONSE_VALUE, builder);
                    }
                }

                // 切到看牌阶段
                room.setRoomState(FightTenRoom.RoomState.SEE_CARD.getIndex());
                // 发送房间状态改变响应
                sendChangeRoomStateResponse(room);

                // 发送最后一张牌
                sendLastOneCard(room);
            }
        } else if (roomState == FightTenRoom.RoomState.SEE_CARD.getIndex()) { // 看牌
            if (currentTime >= enterStateTime + FightTenRoom.RoomState.SEE_CARD.getTime()) {
                // 切到对局结束阶段
                room.setRoomState(FightTenRoom.RoomState.OVER.getIndex());
                // 发送房间状态改变响应
                sendChangeRoomStateResponse(room);
                // 对局结束
                roundOver(room);
            }
        } else if (roomState == FightTenRoom.RoomState.OVER.getIndex()) { // 结算亮牌阶段
            if (currentTime >= enterStateTime + FightTenRoom.RoomState.OVER.getTime() + room.getPlayerSize()) { // 还要每个玩家加1秒，避免结算没有过完就进入下一阶段
                // 房主承包检查局数是否已完结
                if (challengeRoom.getFeeType() == 0) {
                    if (challengeRoom.getRoundNow() >= challengeRoom.getRoundTotal()) { // 所有的对局结束
                        for (BaseGamePlayer baseGamePlayer : room.getGamePlayers()) { // 所有人离开房间
                            if (baseGamePlayer == null) {
                                continue;
                            }
                            NetManager.sendHintMessageToClient("所有对局已结束，房间被解散", baseGamePlayer.getUser());
                            leaveRoom(baseGamePlayer.getUser(), false);
                        }
                        return;
                    }
                }

                // 对局结束，开始准备下一场的对局
                checkPlayerLeaveRoom(room);
                // 房间内数据重置
                room.roundReset();

                if (room.getPlayerSize() < MIN_PLAYER_NUM) {
                    room.setRoomState(FightTenRoom.RoomState.NONE.getIndex());
                } else {
                    room.setRoomState(FightTenRoom.RoomState.READY.getIndex());
                }
                // 发送房间状态改变响应
                sendChangeRoomStateResponse(room);
            }
        } else {
            room.setRoomState(roomState);
            room.setEnterStateTime(0L);
            checkPlayerLeaveRoom(room);
        }
    }

    /**
     * 拼十挑战赛房间重连
     */
    @Override
    public void reconnect(FightTenRoom room, FightTenPlayer player) {
        ThreadPoolUtils.TASK_SERVICE_POOL.execute(() -> {
            try {
                if (room == null || player == null) {
                    return;
                }

                // 设置上次操作时间
                player.setLastOptionTime(System.currentTimeMillis());

                // 发送房间及房间内玩家信息
                sendRoomInfoResponse(room, player);
                sendRoomPlayerInfoResponse(room, player);
                sendRoomPlayerInfoListResponse(room, player);

                // 发送重连消息
                TtmyFightTenChallengeMessage.TenChallengeReconnectResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeReconnectResponse.newBuilder();

                builder.setRoomCode(room.getCode());
                builder.setRoomState(room.getRoomState());
                builder.setStateRestTime((int) (FightTenRoom.RoomState.getTimeByIndex(room.getRoomState())
                        - (System.currentTimeMillis() / 1000 - room.getEnterStateTime())));

                BaseGamePlayer[] gamePlayers = room.getGamePlayers();
                for (BaseGamePlayer baseGamePlayer : gamePlayers) {
                    if (baseGamePlayer == null) {
                        continue;
                    }
                    FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
                    if (tenPlayer.getId() == player.getId()) { // 自己的座位和牌信息
                        builder.setSeat(tenPlayer.getSeat());
                        int cardSize = tenPlayer.getCards().size();
                        for (int i = 0; i < cardSize; i++) {
                            if (i < cardSize - 1) {
                                // 自己的四张手牌
                                builder.addCards(tenPlayer.getCards().get(i));
                            } else {
                                // 自己的最后一张牌
                                builder.setLastCard(tenPlayer.getCards().get(i));
                            }
                        }
                        // 自己的下注列表
                        builder.addAllBetMoneyList(tenPlayer.getBetMoneyList());
                    }
                    if (tenPlayer.getReadyType() == 0) { // 已准备玩家
                        builder.addReadyPlayers(tenPlayer.getId());
                    }

                    // 抢庄信息
                    OseeFightTenMessage.TenFightBankerProto.Builder fightBankerProto = OseeFightTenMessage.TenFightBankerProto
                            .newBuilder();
                    fightBankerProto.setPlayerId(tenPlayer.getId());
                    Integer fightMultiple = tenPlayer.getFightMultiple();
                    fightBankerProto.setFightMultiple(fightMultiple == null ? -1 : fightMultiple);
                    builder.addFightedBankerProto(fightBankerProto);
                    if (room.getBanker() != null) { // 已选择庄家
                        builder.setBankerId(room.getBanker().getId());
                        builder.addFightBankerProto(fightBankerProto);
                    }

                    if (tenPlayer.getBetMoney() != null) { // 玩家已下注
                        OseeFightTenMessage.TenBetMoneyProto.Builder betMoneyProto = OseeFightTenMessage.TenBetMoneyProto
                                .newBuilder();
                        betMoneyProto.setPlayerId(tenPlayer.getId());
                        betMoneyProto.setBetMoney(tenPlayer.getBetMoney());
                        builder.addBetMoneyProto(betMoneyProto);
                    }
                    if (tenPlayer.getSeeOrRubCard()) { // 已经看牌或者搓牌的玩家
                        builder.addSeeOrRubCardPlayer(tenPlayer.getId());
                    }

                    // 对局结束时的玩家结算信息
                    OseeFightTenMessage.TenRoundOverPlayerDataProto.Builder overPlayerDataProto = OseeFightTenMessage.TenRoundOverPlayerDataProto
                            .newBuilder();
                    overPlayerDataProto.setSeat(tenPlayer.getSeat());
                    overPlayerDataProto.setPlayerId(tenPlayer.getId());

                    overPlayerDataProto.setCardType(tenPlayer.getCardType());
                    overPlayerDataProto.setWin(tenPlayer.getWin() == 1);
                    overPlayerDataProto.setWinMoney(tenPlayer.getWinMoney());
                    overPlayerDataProto.setMyMoney(tenPlayer.getMoney());
                    overPlayerDataProto.addAllCards(tenPlayer.getCards());
                    builder.addRoundOverProto(overPlayerDataProto);
                }

                // 参与抢庄动画的玩家id列表
                if (room.getFightBankerRandomPlayerIdList().size() > 0) {
                    builder.addAllAniPlayerId(room.getFightBankerRandomPlayerIdList());
                }

                // 发送重连数据响应
                NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_RECONNECT_RESPONSE_VALUE, builder, player.getUser());

                logger.info("拼十:已经给玩家[{}]发送了拼十挑战赛房间[{}]的重连数据", player.getId(), room.getCode());
            } catch (Exception e) {
                logger.error("拼十:玩家[{}]拼十挑战赛房间重连出错", player.getId());
                e.printStackTrace();
            }
        });
    }

    /**
     * 自动匹配
     */
    public void autoMatch(ServerUser user) {
        if (PlayerManager.getItemNum(user, ItemId.MONTH_CARD) <= 0) {
            NetManager.sendErrorMessageToClient("月卡会员才能进行拼十挑战赛", user);
            return;
        }
        TtmyFightTenChallengeMessage.TenChallengeMatchResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeMatchResponse.newBuilder();
        long limit = moneyLimit[0];
        if (!PlayerManager.checkItem(user, ItemId.MONEY, limit)) {
            builder.setResult(0);
            builder.setInfo("您的账户金币低于最低" + (limit / 10000) + "万，无法开启自动匹配");
        } else if (PlayerManager.getPlayerTenChallengeTimes(user) < 1) {
            builder.setResult(1);
            builder.setInfo("您的挑战次数不足，请前往兑换");
        } else {
            builder.setResult(2);
            builder.setInfo("匹配中...");
            // 开始放入匹配队列
            Map<ServerUser, LocalDateTime> match = new ConcurrentHashMap<>();
            // 匹配结束的时间 1分钟之后
            LocalDateTime matchOverTime = LocalDateTime.now().plusMinutes(1);
            match.put(user, matchOverTime);
            // 放入匹配队列
            matchQueue.offer(match);
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_MATCH_RESPONSE_VALUE, builder, user);
    }

    /**
     * 取消匹配
     */
    public void cancelMatch(ServerUser user) {
        TtmyFightTenChallengeMessage.TenChallengeCancelMatchResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeCancelMatchResponse.newBuilder();
        boolean removeIf = matchQueue.removeIf(map -> map.keySet().contains(user));
        if (removeIf) {
            builder.setResult(0);
        } else {
            builder.setResult(1);
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_CANCEL_MATCH_RESPONSE_VALUE, builder, user);
    }

    /**
     * 获取排行榜
     */
    public void rankingList(ServerUser user) {
        List<TenChallengeRankingLogEntity> list = rankingLogMapper.getList();
        TtmyFightTenChallengeMessage.TenChallengeRankingListResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeRankingListResponse.newBuilder();
        builder.setMyRanking(-1);
        for (int i = 0; i < list.size(); i++) {
            TenChallengeRankingLogEntity entity = list.get(i);
            TtmyFightTenChallengeMessage.TenChallengeRankingProto.Builder ranking = TtmyFightTenChallengeMessage.TenChallengeRankingProto.newBuilder();
            ranking.setUserId(entity.getUserId());
            ranking.setHeadIndex(entity.getHeadIndex());
            ranking.setHeadUrl(entity.getHeadUrl());
            ranking.setNickname(entity.getNickname());
            ranking.setScore(entity.getScore());
            if (entity.getUserId() == user.getId()) { // 我的排名
                builder.setMyRanking(i + 1);
            }
            builder.addRanking(ranking);
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_RANKING_LIST_RESPONSE_VALUE, builder, user);
    }
}
