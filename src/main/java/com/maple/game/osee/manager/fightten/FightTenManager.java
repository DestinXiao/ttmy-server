package com.maple.game.osee.manager.fightten;

import com.google.gson.Gson;
import com.google.protobuf.GeneratedMessage;
import com.maple.common.lobby.proto.LobbyMessage;
import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.entity.UserEntity;
import com.maple.engine.data.ServerUser;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.dao.log.entity.OseeFighttenRecordLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeFighttenRecordLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fightten.FightTenPlayer;
import com.maple.game.osee.entity.fightten.FightTenRobotPlayer;
import com.maple.game.osee.entity.fightten.FightTenRoom;
import com.maple.game.osee.entity.fightten.config.FieldConfig;
import com.maple.game.osee.entity.fightten.config.RobotConfig;
import com.maple.game.osee.entity.gm.CommonResponse;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.fightten.OseeFightTenMessage;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.data.BaseGameRoom;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.maple.game.osee.entity.fightten.FightTenRoom.MIN_PLAYER_NUM;
import static com.maple.game.osee.entity.fightten.FightTenRoom.RoomState;

/**
 * 拼十的管理类
 */
@Component
public class FightTenManager {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected OseeFighttenRecordLogMapper logMapper;

    /**
     * 棋牌数组 52张扑克，其中不含大小王。A至K各四张（黑红梅方） 花色大小：黑>红>梅>方
     **/
    public static final Integer[] CARD_DATA_ARRAY = {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, // 黑桃 A-K
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, // 红桃
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, // 梅花
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, // 方块
    };

    /**
     * 场次数量：初、中、高
     */
    public static final int FIELD_NUM = 3;

    /**
     * 拼十场次配置保存在redis中的key名称
     */
    public static final String REDIS_FIELD_CONFIG_KEYNAME = "Osee:FightTen:Config:Field";

    /**
     * 拼十房间机器人配置在redis中保存的key名称
     */
    public static final String REDIS_ROBOT_CONFIG_KEYNAME = "Osee:FightTen:Config:Robot";

    /**
     * 最大抢庄倍数
     */
    public static final int MAX_FIGHT_MULTIPLE = 4;

    /**
     * 强制未操作请离房间时长：秒
     */
    public static final int LEAVE_ROOM_TIME = 2 * 60;

    /**
     * 机器人赢取的金币总额
     */
    public static final String REDIS_ROBOT_WIN_MONEY_KEY = "Osee:FightTen:Robot:WinMoney";

    /**
     * 机器人输掉的金币总额
     */
    public static final String REDIS_ROBOT_LOSE_MONEY_KEY = "Osee:FightTen:Robot:LoseMoney";

    /**
     * 拼十任务管理类
     */
    @Autowired
    private FightTenTaskManager tenTaskManager;

    /**
     * 从Redis中获取拼十场次配置信息
     */
    public static List<FieldConfig.Config> getFieldConfigList() {
        Gson gson = new Gson();
        // 直接中数据库取出拼十场次配置信息
        String fieldConfigJson = RedisHelper.get(REDIS_FIELD_CONFIG_KEYNAME);
        FieldConfig fieldConfig = gson.fromJson(fieldConfigJson, FieldConfig.class);
        if (fieldConfig == null) {
            fieldConfig = new FieldConfig();
            List<FieldConfig.Config> configs = new ArrayList<>();
            // 初级场
            FieldConfig.Config configChu = new FieldConfig.Config();
            configChu.setType(0);
            configChu.setEnterMoney(10000L);
            configChu.setMaxBetMoney(50000L);
            configs.add(configChu);
            // 中级场
            FieldConfig.Config configZhong = new FieldConfig.Config();
            configZhong.setType(1);
            configZhong.setEnterMoney(100000L);
            configZhong.setMaxBetMoney(100000L);
            configs.add(configZhong);
            // 高级场
            FieldConfig.Config configGao = new FieldConfig.Config();
            configGao.setType(2);
            configGao.setEnterMoney(1000000L);
            configGao.setMaxBetMoney(300000L);
            configs.add(configGao);

            fieldConfig.setConfigs(configs);
            // 保存到redis
            RedisHelper.set(REDIS_FIELD_CONFIG_KEYNAME, gson.toJson(fieldConfig));
        }
        return new ArrayList<>(fieldConfig.getConfigs());
    }

    /**
     * 获取拼十机器人配置信息
     */
    public static RobotConfig getRobotConfig() {
        Gson gson = new Gson();
        // 从redis中获取配置信息
        String robotConfigJson = RedisHelper.get(REDIS_ROBOT_CONFIG_KEYNAME);
        RobotConfig robotConfig = gson.fromJson(robotConfigJson, RobotConfig.class);
        if (robotConfig == null) { // redis为空就设置默认的配置
            robotConfig = new RobotConfig();
            robotConfig.setUseRobot(0);
            robotConfig.setRobotNum(3);
            robotConfig.setRefreshTimeRangeBegin(0);
            robotConfig.setRefreshTimeRangeEnd(5);
            robotConfig.setWinPercent(60); // 初始默认赢取金币占比为60%
            // 保存到redis
            RedisHelper.set(REDIS_ROBOT_CONFIG_KEYNAME, gson.toJson(robotConfig));
        }
        return robotConfig;
    }

    /**
     * 保存机器人输赢的金币
     */
    public static void saveRobotMoney(long money) {
        if (money < 0) { // 输掉
            RedisHelper.set(REDIS_ROBOT_LOSE_MONEY_KEY, String.valueOf(getRobotLoseMoney() + money));
        } else { // 赢的
            RedisHelper.set(REDIS_ROBOT_WIN_MONEY_KEY, String.valueOf(getRobotWinMoney() + money));
        }
    }

    /**
     * 获取机器人累计赢取的金币
     */
    public static long getRobotWinMoney() {
        String value = RedisHelper.get(REDIS_ROBOT_WIN_MONEY_KEY);
        if (StringUtils.isEmpty(value)) {
            return 0;
        }
        return Long.parseLong(value);
    }

    /**
     * 获取机器人累计输掉的金币
     */
    public static long getRobotLoseMoney() {
        String value = RedisHelper.get(REDIS_ROBOT_LOSE_MONEY_KEY);
        if (StringUtils.isEmpty(value)) {
            return 0;
        }
        return Long.parseLong(value);
    }

    // ******************************* 游戏逻辑相关 *********************************

    /**
     * 获取拼十场次信息列表
     *
     * @param user 玩家
     */
    public void sendFieldList(ServerUser user) {
        if (GameContainer.getGameRoomByPlayerId(user.getId()) != null) {
            NetManager.sendErrorMessageToClient("你已在房间中！", user);
            return;
        }
        List<FieldConfig.Config> fieldConfigList = getFieldConfigList();
        OseeFightTenMessage.TenGetFieldListResponse.Builder builder = OseeFightTenMessage.TenGetFieldListResponse
                .newBuilder();
        for (FieldConfig.Config config : fieldConfigList) {
            OseeFightTenMessage.TenFieldInfoProto.Builder fieldInfo = OseeFightTenMessage.TenFieldInfoProto
                    .newBuilder();
            fieldInfo.setType(config.getType());
            fieldInfo.setEnterMoney(config.getEnterMoney());
            builder.addFieldInfos(fieldInfo);
        }
        // 发送给玩家
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_OSEE_TEN_GET_FIELD_LIST_RESPONSE_VALUE, builder, user);
    }

    /**
     * 加入拼十房间
     *
     * @param fieldType 场次类型
     * @param user      玩家
     */
    public void joinRoom(int fieldType, ServerUser user, Integer lastRoom) {
//        if (PlayerManager.getPlayerLevel(user) < 15) {
//            NetManager.sendErrorMessageToClient("等级不足，进入失败！", user);
//            return;
//        }

        if (GameContainer.getGameRoomByPlayerId(user.getId()) != null) {
            NetManager.sendErrorMessageToClient("你已在房间中！", user);
            return;
        }
        FieldConfig.Config config = getFieldConfigList().get(fieldType);
        if (config == null) {
            NetManager.sendErrorMessageToClient("加入房间失败！", user);
            return;
        }

        Long enterMoney = config.getEnterMoney();
        long playerMoney = PlayerManager.getPlayerMoney(user);
        if (playerMoney < enterMoney) {
            NetManager.sendErrorMessageToClient("您身上的金币不足！", user);
            return;
        }

        // 拼十匹配
        // 指定场次的所有房间列表
        List<FightTenRoom> fieldRooms = new ArrayList<>();
        for (FightTenRoom fightTenRoom : GameContainer.getGameRooms(FightTenRoom.class)) {
            // 获取指定场次内的所有房间
            if (fightTenRoom != null && fightTenRoom.getFieldType() == fieldType) {
                fieldRooms.add(fightTenRoom);
            }
        }
        // 按照房间状态排序，倒序排列：因为要先加入准备状态的房间，再是无状态的房间
        fieldRooms.sort((o1, o2) -> {
            if (o1.getRoomState() < o2.getRoomState()) {
                return 1;
            } else if (o1.getRoomState() > o2.getRoomState()) {
                return -1;
            }
            return 0;
        });
        // 是否加入了房间
        boolean join = false;
        // 判断房间的状态，先加入准备状态的房间，再加入只有一个人的房间
        for (FightTenRoom fightTenRoom : fieldRooms) {
            if (fightTenRoom == null || fightTenRoom.getCode() == lastRoom) { // 房间为空，或者为换房间之前的房间号，不能进入
                continue;
            }
            if (fightTenRoom.getPlayerSize() >= fightTenRoom.getMaxSize()) { // 房间人员已满，不能加入
                continue;
            }
            if (fightTenRoom.getRoomState() == RoomState.READY.getIndex() // 处于准备阶段的房间
                    || fightTenRoom.getRoomState() == RoomState.NONE.getIndex()) { // 未处于任何状态的单人房间
                FightTenPlayer fightTenPlayer = GameContainer.createGamePlayer(fightTenRoom, user,
                        FightTenPlayer.class);
                if (fightTenPlayer == null) {
                    NetManager.sendErrorMessageToClient("加入房间失败！", user);
                    return;
                }
                addRoomPlayer(fightTenRoom, fightTenPlayer);
                join = true;
                break;
            }
        }
        if (!join) { // 还没有加入房间就要新建一个房间
            FightTenRoom fightTenRoom = GameContainer.createGameRoom(FightTenRoom.class, FightTenRoom.MAX_PLAYER_NUM);
            if (fightTenRoom == null) {
                NetManager.sendErrorMessageToClient("加入房间失败！", user);
                return;
            }
            // 设置房间状态
            fightTenRoom.setRoomState(RoomState.NONE.getIndex());
            // 设置房间所在场次
            fightTenRoom.setFieldType(fieldType);
            // 设置房间最高下注上限
            fightTenRoom.setMaxBetMoney(config.getMaxBetMoney());

            // 加入房间
            FightTenPlayer fightTenPlayer = GameContainer.createGamePlayer(fightTenRoom, user, FightTenPlayer.class);
            // 更新玩家上次操作时间
            fightTenPlayer.setLastOptionTime(System.currentTimeMillis());
            // 发送房间信息
            sendRoomInfoResponse(fightTenRoom, fightTenPlayer);
            // 发送自己的信息给房间的其他玩家
            sendRoomPlayerInfoResponse(fightTenRoom, fightTenPlayer);
            // 发送房间已有玩家的响应
            sendRoomPlayerInfoListResponse(fightTenRoom, fightTenPlayer);
        }
    }

    /**
     * 将玩家加入到房间
     */
    public void addRoomPlayer(FightTenRoom fightTenRoom, FightTenPlayer fightTenPlayer) {
        synchronized (fightTenRoom) {
            // 更新玩家上次操作时间
            fightTenPlayer.setLastOptionTime(System.currentTimeMillis());

            // 发送房间信息
            sendRoomInfoResponse(fightTenRoom, fightTenPlayer);
            // 发送自己的信息给房间的其他玩家
            sendRoomPlayerInfoResponse(fightTenRoom, fightTenPlayer);
            // 发送房间已有玩家的响应
            sendRoomPlayerInfoListResponse(fightTenRoom, fightTenPlayer);

            // 改变房间状态
            if (fightTenRoom.getRoomState() != RoomState.READY.getIndex()) { // 进入房间不刷新倒计时
                fightTenRoom.setRoomState(RoomState.READY.getIndex());
            }
            // 发送房间状态改变响应
            sendChangeRoomStateResponse(fightTenRoom);
        }
    }

    /**
     * 请求准备游戏
     *
     * @param readyType 准备类型：0-准备，1-取消准备
     * @param user      玩家
     */
    public void readyRoom(int readyType, ServerUser user) {
        FightTenPlayer gamePlayer = GameContainer.getPlayerById(user.getId());
        if (gamePlayer == null) {
            return;
        }
        FightTenRoom gameRoom = GameContainer.getGameRoomByCode(gamePlayer.getRoomCode());
        if (gameRoom == null) {
            return;
        }
        // 非准备阶段
        if (gameRoom.getRoomState() != RoomState.READY.getIndex()) {
            return;
        }
        // 更新玩家上次操作时间
        gamePlayer.setLastOptionTime(System.currentTimeMillis());
        // 设置玩家准备状态
        gamePlayer.setReadyType(readyType);

        OseeFightTenMessage.TenReadyRoomResponse.Builder builder = OseeFightTenMessage.TenReadyRoomResponse
                .newBuilder();
        builder.setPlayerId(user.getId());
        builder.setReadyType(readyType);
        // 广播玩家的准备状态
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_READY_ROOM_RESPONSE_VALUE, builder);

        // 检测每个玩家的准备状态
        for (BaseGamePlayer baseGamePlayer : gameRoom.getGamePlayers()) {
            if (baseGamePlayer == null) {
                continue;
            }
            FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
            if (tenPlayer.getReadyType() == 1) { // 还有玩家未准备
                return;
            }
        }
        // 所有玩家准备完成，提前开始游戏
        // 设置当前阶段进入时间，让定时器自动切换状态
        gameRoom.setEnterStateTime(System.currentTimeMillis() / 1000 - RoomState.READY.getTime());
    }

    /**
     * 发牌
     */
    protected void dispatchCard(FightTenRoom fightTenRoom) {
        List<Integer> cardData = new ArrayList<>(Arrays.asList(CARD_DATA_ARRAY));
        // 打乱牌组
        Collections.shuffle(cardData);

        // 所有玩家的手牌数据
        List<List<Integer>> allCards = new ArrayList<>();
        // 是否生成散牌
        boolean sanPai = ThreadLocalRandom.current().nextBoolean();
        for (int i = 0; i < fightTenRoom.getMaxSize(); i++) {
            List<Integer> cards = new ArrayList<>();
            for (int c = 0; c < 5; c++) { // 每位玩家发5张牌
                cards.add(cardData.remove(0));
            }
            if (!sanPai) {
                int cardType = FightTenCardManager.getCardType(cards);
                if (cardType == FightTenCardManager.CARD_TYPE_NONE) { // 散牌重新生成
                    // 把牌放回去
                    cardData.addAll(cards);
                    // 打乱牌组
                    Collections.shuffle(cardData);
                    i--;
                    continue;
                }
            }
            allCards.add(cards);
        }

        // 发牌完毕后，判断牌型好坏，控制好牌：机器人好牌、必输玩家
        // 按照牌型大小从小到大（即从坏到好）排序
        allCards.sort(FightTenCardManager::compare);

        // 是否控制机器人发好牌
        boolean robotCtrl = false;
        long robotWinMoney = getRobotWinMoney();
        long robotLoseMoney = -getRobotLoseMoney();
        long totalMoney = robotWinMoney + robotLoseMoney;
        int percent = 0;
        int winPercent = getRobotConfig().getWinPercent();
        if (totalMoney > 0) {
            percent = (int) ((float) robotWinMoney / totalMoney * 100);
        }
        if (percent < winPercent) {
            robotCtrl = true;
        }

        // 将真实玩家放到最前面
        List<BaseGamePlayer> players = new LinkedList<>(Arrays.asList(fightTenRoom.getGamePlayers()));
        Collections.shuffle(players);
        List<FightTenPlayer> tenPlayers = new LinkedList<>();
        for (BaseGamePlayer player : players) {
            if (player == null) {
                continue;
            }
            FightTenPlayer tenPlayer = (FightTenPlayer) player;
            if (!(tenPlayer instanceof FightTenRobotPlayer)) { // 把玩家放到最前面
                tenPlayers.add(0, tenPlayer);
            } else {
                tenPlayers.add(tenPlayer);
            }
        }
        // 控制更改玩家手牌
        for (FightTenPlayer tenPlayer : tenPlayers) {
            if (tenPlayer == null) {
                continue;
            }
            tenPlayer.getCards().clear();
            if (!(tenPlayer instanceof FightTenRobotPlayer)) {
                // 输赢控制：1-必输 2-必赢
                int loseControl = PlayerManager.getPlayerEntity(tenPlayer.getUser()).getLoseControl();
                if (loseControl == 2) { // 玩家必赢
                    // 从牌堆尾拿大牌给玩家
                    tenPlayer.getCards().addAll(allCards.remove(allCards.size() - 1));
                    logger.info("拼十玩家[{}]的输赢被控制了 玩家为必赢", tenPlayer.getId());
                } else if (loseControl == 1 || robotCtrl) { // 玩家必输或机器人必赢
                    // 玩家就从牌堆首拿小牌给玩家
                    tenPlayer.getCards().addAll(allCards.remove(0));
                    logger.info("拼十机器人赢钱百分比:赢-{},输-{},百分比-{}%({}%)", robotWinMoney, robotLoseMoney, percent, winPercent);
                    logger.info("拼十玩家[{}]的输赢被控制了 玩家必输:[{}] 机器人必赢:[{}]", tenPlayer.getId(), loseControl == 1, robotCtrl);
                } else { // 正常随机发牌
                    tenPlayer.getCards().addAll(allCards.remove(ThreadLocalRandom.current().nextInt(allCards.size())));
                }
            } else {
                tenPlayer.getCards().addAll(allCards.remove(ThreadLocalRandom.current().nextInt(allCards.size())));
            }
        }

        // 给玩家发送牌信息
        for (BaseGamePlayer baseGamePlayer : fightTenRoom.getGamePlayers()) {
            if (baseGamePlayer != null) {
                FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
                // 先只给玩家发前面4张名牌
                OseeFightTenMessage.TenDispatchCardResponse.Builder builder = OseeFightTenMessage.TenDispatchCardResponse.newBuilder();
                builder.setPlayerId(tenPlayer.getId());
                builder.setSeat(tenPlayer.getSeat());
                builder.addAllCards(new ArrayList<>(tenPlayer.getCards().subList(0, tenPlayer.getCards().size() - 1)));
                NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_OSEE_TEN_DISPATCH_CARD_RESPONSE_VALUE, builder,
                        tenPlayer.getUser());
            }
        }
    }

    /**
     * 抢庄
     *
     * @param fightMultiple 倍数
     * @param user          玩家
     */
    public void fightBanker(int fightMultiple, ServerUser user) {
        FightTenPlayer gamePlayer = GameContainer.getPlayerById(user.getId());
        if (gamePlayer == null) {
            return;
        }
        FightTenRoom gameRoom = GameContainer.getGameRoomByCode(gamePlayer.getRoomCode());
        if (gameRoom == null) {
            return;
        }
        // 非抢庄阶段
        if (gameRoom.getRoomState() != RoomState.FIGHT_BANKER.getIndex()) {
            return;
        }
        // 设置上次操作时间
        gamePlayer.setLastOptionTime(System.currentTimeMillis());
        // 1-4倍，-1=不抢
        if (fightMultiple >= -1 && fightMultiple <= MAX_FIGHT_MULTIPLE) {
            // 设置玩家抢庄倍数
            gamePlayer.setFightMultiple(fightMultiple);
            if (gameRoom.getMaxFightMultiple() < fightMultiple) {
                // 房间最高倍数小于当前的就置为当前倍数
                gameRoom.setMaxFightMultiple(fightMultiple);
            }
            // 广播玩家抢庄信息
            OseeFightTenMessage.TenFightBankerResponse.Builder builder = OseeFightTenMessage.TenFightBankerResponse
                    .newBuilder();
            builder.setPlayerId(user.getId());
            builder.setFightMultiple(fightMultiple);
            sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_FIGHT_BANKER_RESPONSE_VALUE, builder);
        }

        // 判断是否所有人都选择了抢或不抢
        for (BaseGamePlayer baseGamePlayer : gameRoom.getGamePlayers()) {
            if (baseGamePlayer == null) {
                continue;
            }
            FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
            if (tenPlayer.getFightMultiple() == null) {
                // 还有人还没有做出选择
                return;
            }
        }

        // 如果所有玩家都 抢/不抢庄 了就提前结束改状态进行庄家确定
        gameRoom.setEnterStateTime(System.currentTimeMillis() / 1000 - RoomState.FIGHT_BANKER.getTime());
    }

    /**
     * 选定拼十房间的庄家
     *
     * @param fightTenRoom 拼十房间
     */
    protected void selectBanker(FightTenRoom fightTenRoom) {
        // 抢庄同倍数的玩家id列表
        List<Long> fightSameMultipleBankerList = new ArrayList<>();
        // 庄家id
        Long bankerId;
        fightTenRoom.getFightBankerRandomPlayerIdList().clear();
        for (BaseGamePlayer baseGamePlayer : fightTenRoom.getGamePlayers()) {
            if (baseGamePlayer == null) {
                continue;
            }
            FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
            // 如果有人还没有选择倍数就设置为默认的不抢(-1倍)
            if (tenPlayer.getFightMultiple() == null) {
                tenPlayer.setFightMultiple(-1);
            }
            // 按照最高倍数来算
            if (tenPlayer.getFightMultiple().equals(fightTenRoom.getMaxFightMultiple())) {
                fightSameMultipleBankerList.add(tenPlayer.getId());
            }
        }
        if (fightSameMultipleBankerList.size() > 1) { // 超过一个人要进行随机
            fightTenRoom.getFightBankerRandomPlayerIdList().addAll(fightSameMultipleBankerList);
            // 随机选择的庄家
            int randomIndex = ThreadLocalRandom.current().nextInt(0, fightSameMultipleBankerList.size());
            bankerId = fightSameMultipleBankerList.get(randomIndex);
        } else if (fightSameMultipleBankerList.size() == 1) { // 只有一个人，选定庄家
            bankerId = fightSameMultipleBankerList.get(0);
        } else { // 所有人都选了"不抢"
            fightSameMultipleBankerList.clear();
            // 设置默认最高抢庄倍数为4
            fightTenRoom.setMaxFightMultiple(4);
            for (BaseGamePlayer baseGamePlayer : fightTenRoom.getGamePlayers()) {
                if (baseGamePlayer == null) {
                    continue;
                }
                // 就从所有玩家里面随机选择庄家
                fightSameMultipleBankerList.add(baseGamePlayer.getId());
            }
            fightTenRoom.getFightBankerRandomPlayerIdList().addAll(fightSameMultipleBankerList);
            // 随机选择的庄家
            int randomIndex = ThreadLocalRandom.current().nextInt(0, fightSameMultipleBankerList.size());
            bankerId = fightSameMultipleBankerList.get(randomIndex);
        }
        FightTenPlayer banker = GameContainer.getPlayerById(bankerId);
        if (banker != null) {
            // 设置房间庄家
            fightTenRoom.setBanker(banker);
            banker.setFightMultiple(fightTenRoom.getMaxFightMultiple());
        }
    }

    /**
     * 计算拼十下注金额列表
     *
     * @param fightTenRoom 拼十房间
     */
    protected void calcBetMoney(FightTenRoom fightTenRoom) {
        // 庄家金币数量
        long bankerMoney = fightTenRoom.getBanker().getMoney();
        // 闲家玩家数
        int freePlayerNum = fightTenRoom.getPlayerSize() - 1;
        // 抢庄最高倍数
        Integer maxFightMultiple = fightTenRoom.getMaxFightMultiple();
        // 最大下注数
        Long maxBetMoney = fightTenRoom.getMaxBetMoney();

        long calcMoney = bankerMoney / freePlayerNum / 5; // 5为牌型最高倍率 计算庄家的所有钱用来赔付 5倍率下 每个人能分到的钱的五分之一 超过这个值 庄家的钱将不够赔付所有人
        // 已删除 除以抢庄倍率 的设定 源代码需要再加一条 /maxFightMultiple
        // 计算出来的金额和最高下注限制取小的那个去进行计算
        long BankerbetBaseMoney = Math.min(calcMoney, maxBetMoney); // 100%
        // System.out.println("庄家金币"+bankerMoney+"庄家金币限制："+calcMoney+"
        // 房间金币限制："+maxBetMoney+" 计算所得金币："+BankerbetBaseMoney);
        for (BaseGamePlayer baseGamePlayer : fightTenRoom.getGamePlayers()) {
            if (baseGamePlayer == null || baseGamePlayer.getId() == fightTenRoom.getBanker().getId()) { // 庄家不参与下注
                continue;
            }
            FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;

            // 根据玩家携带的金币计算下注
            long idelMoney = tenPlayer.getMoney() / 5;
            long betBaseMoney = Math.min(BankerbetBaseMoney, idelMoney);
            // System.out.println("闲家金币："+tenPlayer.getMoney()+"
            // 闲家金币限制："+idelMoney+"之前限制："+demo+" 计算所得金币："+betBaseMoney);
            long bet25 = betBaseMoney * 25 / 100; // 25%
            long bet50 = betBaseMoney * 50 / 100; // 50%
            long bet75 = betBaseMoney * 75 / 100; // 75%
            tenPlayer.getBetMoneyList().clear();
            tenPlayer.getBetMoneyList().add(bet25); // 要求发送的金额列表 与赔付的一致 所以 * 4 因为抢庄不抢庄倍率都被控制为 4倍 （by客户要求
            tenPlayer.getBetMoneyList().add(bet50);
            tenPlayer.getBetMoneyList().add(bet75);
            tenPlayer.getBetMoneyList().add(betBaseMoney);

            OseeFightTenMessage.TenBetMoneyListResponse.Builder builder = OseeFightTenMessage.TenBetMoneyListResponse
                    .newBuilder();
            builder.addAllBetMoneyList(tenPlayer.getBetMoneyList());
            // 下注金额列表发给玩家
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_OSEE_TEN_BET_MONEY_LIST_RESPONSE_VALUE, builder,
                    baseGamePlayer.getUser());
        }
    }

    /**
     * 玩家下注
     *
     * @param betMoneyIndex 下注金额列表序号
     * @param user          玩家
     */
    public void betMoney(int betMoneyIndex, ServerUser user) {
        FightTenPlayer gamePlayer = GameContainer.getPlayerById(user.getId());
        if (gamePlayer == null) {
            return;
        }
        FightTenRoom gameRoom = GameContainer.getGameRoomByCode(gamePlayer.getRoomCode());
        if (gameRoom == null) {
            return;
        }
        // 非下注阶段
        if (gameRoom.getRoomState() != RoomState.BET_MONEY.getIndex()) {
            return;
        }
        // 设置上次操作时间
        gamePlayer.setLastOptionTime(System.currentTimeMillis());
        if (betMoneyIndex < 0 || betMoneyIndex >= 4) {
            NetManager.sendErrorMessageToClient("下注金额不对！", user);
            return;
        }
        // 庄家不能下注
        if (user.getId() == gameRoom.getBanker().getId()) {
            return;
        }
        long betMoney = gamePlayer.getBetMoneyList().get(betMoneyIndex);
        // 设置下注金额
        gamePlayer.setBetMoney(betMoney);
        OseeFightTenMessage.TenBetMoneyResponse.Builder builder = OseeFightTenMessage.TenBetMoneyResponse.newBuilder();
        builder.setPlayerId(user.getId());
        builder.setBetMoney(betMoney);
        // 广播给房间内所有玩家
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_BET_MONEY_RESPONSE_VALUE, builder);

        // 检查是否所有人都下注了
        for (BaseGamePlayer baseGamePlayer : gameRoom.getGamePlayers()) {
            if (baseGamePlayer == null || baseGamePlayer.getId() == gameRoom.getBanker().getId()) { // 庄家不下注
                continue;
            }
            FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
            if (tenPlayer.getBetMoney() == null) { // 还有人未下注
                return;
            }
        }

        // 所有玩家下注完毕就提前结束下注阶段进入下一阶段
        gameRoom.setEnterStateTime(System.currentTimeMillis() / 1000 - RoomState.BET_MONEY.getTime());
    }

    /**
     * 看牌或搓牌
     *
     * @param seeOrRub 0-看牌，1-搓牌
     * @param user     玩家
     */
    public void seeOrRubCard(int seeOrRub, ServerUser user) {
        FightTenPlayer gamePlayer = GameContainer.getPlayerById(user.getId());
        if (gamePlayer == null) {
            return;
        }
        FightTenRoom gameRoom = GameContainer.getGameRoomByCode(gamePlayer.getRoomCode());
        if (gameRoom == null) {
            return;
        }
        // 非看牌阶段
        if (gameRoom.getRoomState() != RoomState.SEE_CARD.getIndex()) {
            return;
        }
        // 设置玩家上次操作时间
        gamePlayer.setLastOptionTime(System.currentTimeMillis());
        gamePlayer.setSeeOrRubCard(true);

        if (seeOrRub == 0) { // 看牌
            OseeFightTenMessage.TenSeeCardResponse.Builder builder = OseeFightTenMessage.TenSeeCardResponse
                    .newBuilder();
            builder.setPlayerId(user.getId());
            // 广播给房间内每一位玩家
            sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_SEE_CARD_RESPONSE_VALUE, builder);
        } else if (seeOrRub == 1) { // 搓牌
            OseeFightTenMessage.TenRubCardResponse.Builder builder = OseeFightTenMessage.TenRubCardResponse
                    .newBuilder();
            builder.setPlayerId(user.getId());
            // 广播到房间每一位玩家
            sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_RUB_CARD_RESPONSE_VALUE, builder);
        }

        // 检测是否所有人都已经看牌或搓牌
        for (BaseGamePlayer baseGamePlayer : gameRoom.getGamePlayers()) {
            if (baseGamePlayer == null) {
                continue;
            }
            FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
            if (!tenPlayer.getSeeOrRubCard()) { // 还有人没有看牌或搓牌
                return;
            }
        }

        // 所有玩家看牌/搓牌完毕就提前结束该阶段进入下一阶段
        gameRoom.setEnterStateTime(System.currentTimeMillis() / 1000 - RoomState.SEE_CARD.getTime());
    }

    /**
     * 离开房间
     *
     * @param user           玩家
     * @param checkForDelete 是否检查删除房间
     * @return 是否离开了房间
     */
    public boolean leaveRoom(ServerUser user, boolean checkForDelete) {
        BaseGamePlayer gamePlayer = GameContainer.getPlayerById(user.getId());
        if (gamePlayer == null) {
            logger.info("尝试离开（踢出房间时），传入user为空！");
            return false;
        }
        BaseGameRoom gameRoom = GameContainer.getGameRoomByCode(gamePlayer.getRoomCode());
        if (gameRoom == null) {
            logger.info("尝试离开（踢出房间时），gameRoom为空！");
            return false;
        }
        if (gameRoom instanceof FightTenRoom && gamePlayer instanceof FightTenPlayer) {
            FightTenRoom room = (FightTenRoom) gameRoom;
            FightTenPlayer player = (FightTenPlayer) gamePlayer;
            // 设置玩家上次操作时间
            player.setLastOptionTime(System.currentTimeMillis());
            int roomState = room.getRoomState();
            // 只有在未准备阶段或准备阶段退出房间
            if (roomState != RoomState.NONE.getIndex()
                    && roomState != RoomState.READY.getIndex()
                    && roomState != RoomState.OVER.getIndex()) {
                NetManager.sendErrorMessageToClient("对局已开始，不能离开房间！", user);
                return false;
            }
            OseeFightTenMessage.TenLeaveRoomResponse.Builder builder = OseeFightTenMessage.TenLeaveRoomResponse.newBuilder();
            builder.setPlayerId(user.getId());
            // 广播给房间内每一位玩家
            sendRoomMessage(room, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_LEAVE_ROOM_RESPONSE_VALUE, builder);
        }

        // 从房间内删除玩家
        GameContainer.removeGamePlayer(gameRoom, gamePlayer.getSeat());

        // 判断是否还有玩家，没有就要解散房间
        if (gameRoom.getPlayerSize() <= 0) {
            // 解散删除房间
            GameContainer.removeGameRoom(gameRoom);
            return true;
        }

        if (checkForDelete) {
            // 判断房间是否可以删除
            boolean hasTruePlayer = false; // 是否还有真实玩家
            for (BaseGamePlayer baseGamePlayer : gameRoom.getGamePlayers()) {
                if (baseGamePlayer == null) {
                    continue;
                }
                // 判断是否是机器人
                if (!(baseGamePlayer instanceof FightTenRobotPlayer)) {
                    hasTruePlayer = true;
                    break;
                }
            }
            if (!hasTruePlayer) { // 如果没有真实玩家了就让全部机器人退出房间，解散房间
                List<BaseGamePlayer> players = new ArrayList<>(Arrays.asList(gameRoom.getGamePlayers()));
                for (BaseGamePlayer baseGamePlayer : players) {
                    if (baseGamePlayer != null) {
                        leaveRoom(baseGamePlayer.getUser(), false);
                    }
                }
            }
        }
        return true;
    }

    /**
     * 更换房间
     */
    public void changeRoom(ServerUser user) {
        FightTenPlayer gamePlayer = GameContainer.getPlayerById(user.getId());
        if (gamePlayer == null) {
            return;
        }
        if (System.currentTimeMillis() - gamePlayer.enterTime < 5000) {
            NetManager.sendHintMessageToClient("进入房间5秒内无法更换房间", user);
            return;
        }

        FightTenRoom gameRoom = GameContainer.getGameRoomByCode(gamePlayer.getRoomCode());
        RoomState state = RoomState.getEnumByIndex(gameRoom.getRoomState());
        if (state != RoomState.READY && state != RoomState.NONE) {
            NetManager.sendHintMessageToClient("游戏已开始，无法更换房间", user);
            return;
        }
        Integer fieldType = gameRoom.getFieldType();
        // 先离开房间
        if (leaveRoom(user, true)) {
            // 重新加入新房间
            joinRoom(fieldType, user, gameRoom.getCode());
        }
    }

    /**
     * 礼物赠送
     *
     * @param fromPlayerId 赠送礼物的玩家id
     * @param toPlayerId   被赠送礼物的玩家id
     * @param giftType     礼物类型
     */
    public void giveGift(long fromPlayerId, long toPlayerId, int giftType) {
        FightTenPlayer gamePlayer = GameContainer.getPlayerById(fromPlayerId);
        if (gamePlayer == null) {
            return;
        }
        FightTenRoom gameRoom = GameContainer.getGameRoomByCode(gamePlayer.getRoomCode());
        if (gameRoom == null) {
            return;
        }
        // 设置玩家上次操作时间
        gamePlayer.setLastOptionTime(System.currentTimeMillis());

        // 广播礼物赠送消息
        OseeFightTenMessage.TenGiveGiftResponse.Builder builder = OseeFightTenMessage.TenGiveGiftResponse.newBuilder();
        builder.setToPlayerId(toPlayerId);
        builder.setFromPlayerId(fromPlayerId);
        builder.setGiftType(giftType);
        sendRoomMessage(gameRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_GIVE_GIFT_RESPONSE_VALUE, builder);
    }

    /**
     * 拼十房间对局结束
     *
     * @param fightTenRoom 拼十房间
     */
    protected void roundOver(FightTenRoom fightTenRoom) {
        FightTenPlayer banker = fightTenRoom.getBanker();
        // 庄家牌型
        int bankerCardType = FightTenCardManager.getCardType(banker.getCards());
        banker.setCardType(bankerCardType);
        // 完成庄家任务
        tenTaskManager.doTask(bankerCardType, banker);
        OseeFightTenMessage.TenRoundOverResponse.Builder roundOverResponse = OseeFightTenMessage.TenRoundOverResponse.newBuilder();
        // 庄家结算时数据
        OseeFightTenMessage.TenRoundOverPlayerDataProto.Builder bankerData = OseeFightTenMessage.TenRoundOverPlayerDataProto.newBuilder();
        bankerData.setPlayerId(banker.getId());
        bankerData.setSeat(banker.getSeat());
        bankerData.addAllCards(banker.getCards());
        bankerData.setCardType(bankerCardType);
        // 庄家输赢的总金币数
        long bankerWinMoney = 0;
        for (BaseGamePlayer baseGamePlayer : fightTenRoom.getGamePlayers()) {
            if (baseGamePlayer == null) {
                continue;
            }
            if (baseGamePlayer.getId() == banker.getId()) { // 庄家跳过
                continue;
            }

            // 闲家
            FightTenPlayer freePlayer = (FightTenPlayer) baseGamePlayer;

            OseeFightTenMessage.TenRoundOverPlayerDataProto.Builder playerData = OseeFightTenMessage.TenRoundOverPlayerDataProto.newBuilder();
            playerData.setPlayerId(baseGamePlayer.getId());
            playerData.setSeat(baseGamePlayer.getSeat());
            playerData.addAllCards(freePlayer.getCards());

            // 闲家牌型
            int freeCardType = FightTenCardManager.getCardType(freePlayer.getCards());
            freePlayer.setCardType(freeCardType);
            // 完成闲家任务
            tenTaskManager.doTask(freeCardType, freePlayer);

            playerData.setCardType(freeCardType);

            // 闲家是否赢了
            boolean freeWin = false;
            int compare = FightTenCardManager.compare(banker.getCards(), freePlayer.getCards());
            if (compare < 0) { // 前者小于后者，即闲家牌大
                freeWin = true;
            }

            playerData.setWin(freeWin);
            String cardType;
            long money;
            // 闲家下注金额
            long freeBetMoney = freePlayer.getBetMoney();
            if (freeWin) { // 如果闲家赢了
                freePlayer.setWin(1);
                // 结算 = 闲家下注金额 * 闲家牌倍数
                money = freeBetMoney * FightTenCardManager.getCardTypeMultiple(freeCardType);// *
                // banker.getFightMultiple()
                // 抢庄默认为4倍
                long bankerMoney = banker.getMoney();
                if (bankerMoney < money) { // 携带的金币不够赔付就扣除能够扣的
                    money = bankerMoney < 0 ? 0 : bankerMoney;
                }
                // 播报
//                if (money > 1000000) {
//                    cardType = check(freeCardType);
//                    String info = String.format(
//                            AutoWanderSubtitle.TEMPLATES[ThreadLocalRandom.current().nextInt(4, 6)],
//                            freePlayer.getUser().getNickname(), cardType, money / 10000
//                    );
//                    sendWanderSubtitle(info);
//                }
                playerData.setWinMoney(money);
                bankerWinMoney -= money;
                if (!(freePlayer instanceof FightTenRobotPlayer)) { // 闲家不是机器人
                    PlayerManager.addItem(freePlayer.getUser(), ItemId.MONEY, money, ItemChangeReason.FIGHT_TEN_WIN, true); // 闲家加钱
                } else {
                    ((FightTenRobotPlayer) freePlayer).setMoney(freePlayer.getMoney() + money);
                    if (!(banker instanceof FightTenRobotPlayer)) { // 庄家不是机器人才记录输赢数据 否则只记录机器人输赢金币
                        saveRobotMoney(money);
                    }
                }
                if (!(banker instanceof FightTenRobotPlayer)) { // 庄家不是机器人
                    PlayerManager.addItem(banker.getUser(), ItemId.MONEY, -money, ItemChangeReason.FIGHT_TEN_LOSE, true); // 庄家减钱
                } else {
                    ((FightTenRobotPlayer) banker).setMoney(banker.getMoney() - money);
                    if (!(freePlayer instanceof FightTenRobotPlayer)) { // 闲家不是机器人才记录输赢数据
                        saveRobotMoney(-money);
                    }
                }
            } else { // 闲家输了，庄家赢了
                freePlayer.setWin(0);
                // 结算 = 闲家下注金额 * 庄家牌倍数 * 庄家抢庄倍数
                money = freeBetMoney * FightTenCardManager.getCardTypeMultiple(bankerCardType);// *
                // banker.getFightMultiple()
                // 抢庄默认为4倍
                long freePlayerMoney = freePlayer.getMoney();
                if (freePlayerMoney < money) { // 携带的金币不够赔付就扣除能够扣的
                    money = freePlayerMoney < 0 ? 0 : freePlayerMoney;
                }
                // 播报
//                if (money > 1000000) {
//                    cardType = check(bankerCardType);
//                    String info = String.format(
//                            AutoWanderSubtitle.TEMPLATES[ThreadLocalRandom.current().nextInt(4, 6)],
//                            freePlayer.getUser().getNickname(), cardType, money / 10000
//                    );
//                    sendWanderSubtitle(info);
//                }

                playerData.setWinMoney(-money);
                bankerWinMoney += money;
                if (!(freePlayer instanceof FightTenRobotPlayer)) { // 闲家不是机器人
                    PlayerManager.addItem(freePlayer.getUser(), ItemId.MONEY, -money, ItemChangeReason.FIGHT_TEN_LOSE, true); // 闲家减钱
                } else {
                    ((FightTenRobotPlayer) freePlayer).setMoney(freePlayer.getMoney() - money);
                    if (!(banker instanceof FightTenRobotPlayer)) { // 庄家不是机器人才记录输赢数据
                        saveRobotMoney(-money);
                    }
                }
                if (!(banker instanceof FightTenRobotPlayer)) { // 庄家不是机器人
                    PlayerManager.addItem(banker.getUser(), ItemId.MONEY, money, ItemChangeReason.FIGHT_TEN_WIN, true); // 庄家加钱
                } else {
                    ((FightTenRobotPlayer) banker).setMoney(banker.getMoney() + money);
                    if (!(freePlayer instanceof FightTenRobotPlayer)) { // 闲家不是机器人才记录输赢数据
                        saveRobotMoney(money);
                    }
                }
            }

            playerData.setMyMoney(freePlayer.getMoney());
            roundOverResponse.addPlayerData(playerData);

            // 记录该闲家单局输赢的钱
            freePlayer.setWinMoney(playerData.getWinMoney());

            // log日志记录闲家 如果闲家不是机器人
            if (!(freePlayer instanceof FightTenRobotPlayer)) {
                OseeFighttenRecordLogEntity log = new OseeFighttenRecordLogEntity();// 日志记录
                log.setPlayerId(freePlayer.getId());
                if (freeWin) {// 如果赢了
                    log.setMoney(money);
                } else {// 输了
                    log.setMoney(-money);
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
        }

        // 如果庄家不是机器人
        if (!(banker instanceof FightTenRobotPlayer)) {
            // 庄家日志记录
            String cardType = check(bankerCardType);
            OseeFighttenRecordLogEntity log = new OseeFighttenRecordLogEntity();// 日志记录
            log.setPlayerId(banker.getId());
            log.setNickname(banker.getUser().getNickname());
            log.setInput(0);// 庄家下注是0
            log.setRate(FightTenCardManager.getCardTypeMultiple(bankerCardType));// 庄家牌倍数
            log.setCardType(cardType);
            log.setMoney(bankerWinMoney);// 输赢总金币
            log.setPlayBeforeMoney(banker.getMoney() - bankerWinMoney);
            log.setPlayAfterMoney(banker.getMoney());
            logMapper.save(log);
        }

        bankerData.setMyMoney(banker.getMoney());
        bankerData.setWinMoney(bankerWinMoney);
        roundOverResponse.addPlayerData(bankerData);
        // 记录庄家单局输赢的钱
        banker.setWinMoney(bankerWinMoney);

        // 广播房间结算信息
        sendRoomMessage(fightTenRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_ROUND_OVER_RESPONSE_VALUE,
                roundOverResponse);
    }

    /**
     * 给房间每位玩家发送自己的最后一张手牌
     *
     * @param fightTenRoom 拼十房间
     */
    public void sendLastOneCard(FightTenRoom fightTenRoom) {
        for (BaseGamePlayer baseGamePlayer : fightTenRoom.getGamePlayers()) {
            if (baseGamePlayer == null) {
                continue;
            }
            FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
            List<Integer> cards = tenPlayer.getCards();
            OseeFightTenMessage.TenSendLastCardResponse.Builder builder = OseeFightTenMessage.TenSendLastCardResponse
                    .newBuilder();
            builder.setLastCard(cards.get(cards.size() - 1));
            // 发送最后一张牌响应给玩家
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_OSEE_TEN_SEND_LAST_CARD_RESPONSE_VALUE, builder,
                    baseGamePlayer.getUser());
        }
    }

    /**
     * 检查玩家是否要被请离房间
     *
     * @param fightTenRoom 拼十房间
     */
    public void checkPlayerLeaveRoom(FightTenRoom fightTenRoom) {
        BaseGamePlayer[] gamePlayers = fightTenRoom.getGamePlayers();
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
            long optionTime = System.currentTimeMillis() - tenPlayer.getLastOptionTime();
            if (optionTime / 1000 >= LEAVE_ROOM_TIME && tenPlayer.getUser().isOnline()) {
                NetManager.sendErrorMessageToClient("您长时间未操作，已被移出房间！", tenPlayer.getUser());
                leaveRoom(tenPlayer.getUser(), true);
                continue;
            }
            FieldConfig.Config config = getFieldConfigList().get(fightTenRoom.getFieldType());
            Long enterMoney = config.getEnterMoney();
            if (tenPlayer.getMoney() < enterMoney) { // 玩家金币不够入场金币也要被请离
                NetManager.sendErrorMessageToClient("您的金币数量低于场次要求，已被移出房间！", tenPlayer.getUser());
                leaveRoom(tenPlayer.getUser(), true);
            }
        }
    }

    /**
     * 处理拼十的房间状态
     *
     * @param fightTenRoom 拼十房间
     */
    public void dealRoomState(FightTenRoom fightTenRoom) {
        if (fightTenRoom == null) {
            return;
        }
        Integer roomState = fightTenRoom.getRoomState();
        long currentTime = System.currentTimeMillis() / 1000;
        Long enterStateTime = fightTenRoom.getEnterStateTime();
        if (roomState == RoomState.READY.getIndex()) { // 准备阶段
            if (currentTime >= enterStateTime + RoomState.READY.getTime()) { // 当前时间大于阶段持续时间
                checkPlayerLeaveRoom(fightTenRoom);
                if (fightTenRoom.getPlayerSize() >= FightTenRoom.MIN_PLAYER_NUM) { // 人数大于最少人数
                    // 切到发牌阶段
                    fightTenRoom.setRoomState(RoomState.DISPATCH_CARD.getIndex());
                    // 发送房间状态改变响应
                    sendChangeRoomStateResponse(fightTenRoom);
                    // 对局开始
                    dispatchCard(fightTenRoom);
                } else {
                    // 切到无状态
                    fightTenRoom.setRoomState(RoomState.NONE.getIndex());
                    // 发送房间状态改变响应
                    sendChangeRoomStateResponse(fightTenRoom);
                    // 房间数据重置
                    fightTenRoom.roundReset();
                }
            }
        } else if (roomState == RoomState.DISPATCH_CARD.getIndex()) { // 发牌阶段
            if (currentTime >= enterStateTime + RoomState.DISPATCH_CARD.getTime()) {
                // 切到抢庄阶段
                fightTenRoom.setRoomState(RoomState.FIGHT_BANKER.getIndex());
                // 发送房间状态改变响应
                sendChangeRoomStateResponse(fightTenRoom);
            }
        } else if (roomState == RoomState.FIGHT_BANKER.getIndex()) { // 抢庄
            if (currentTime >= enterStateTime + RoomState.FIGHT_BANKER.getTime()) {
                // 抢庄结束选择庄家
                selectBanker(fightTenRoom);

                OseeFightTenMessage.TenSelectBankerResponse.Builder builder = OseeFightTenMessage.TenSelectBankerResponse
                        .newBuilder();
                builder.setBankerId(fightTenRoom.getBanker().getId());
                builder.setBankerFightMultiple(fightTenRoom.getBanker().getFightMultiple());
                builder.addAllRandomPlayerIdList(fightTenRoom.getFightBankerRandomPlayerIdList());
                // 广播选庄信息
                sendRoomMessage(fightTenRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_SELECT_BANKER_RESPONSE_VALUE,
                        builder);

                if (fightTenRoom.getFightBankerRandomPlayerIdList().size() == 0) { // 不需要随机选庄
                    // 切到下注阶段
                    fightTenRoom.setRoomState(RoomState.BET_MONEY.getIndex());
                    // 发送房间状态改变响应
                    sendChangeRoomStateResponse(fightTenRoom);
                    // 计算下注金额列表
                    calcBetMoney(fightTenRoom);
                } else {
                    // 切换到随机选庄动画阶段
                    fightTenRoom.setRoomState(RoomState.FIGHT_BANKER_ANI.getIndex());
                    // 发送房间状态改变响应
                    sendChangeRoomStateResponse(fightTenRoom);
                }
            }
        } else if (roomState == RoomState.FIGHT_BANKER_ANI.getIndex()) { // 抢庄随机庄家的动画阶段
            if (currentTime >= enterStateTime + RoomState.FIGHT_BANKER_ANI.getTime()) {
                // 切到下注阶段
                fightTenRoom.setRoomState(RoomState.BET_MONEY.getIndex());
                // 发送房间状态改变响应
                sendChangeRoomStateResponse(fightTenRoom);
                // 计算下注金额列表
                calcBetMoney(fightTenRoom);
            }
        } else if (roomState == RoomState.BET_MONEY.getIndex()) { // 下注
            if (currentTime >= enterStateTime + RoomState.BET_MONEY.getTime()) {
                // 检查是否所有人都下注了
                for (BaseGamePlayer baseGamePlayer : fightTenRoom.getGamePlayers()) {
                    if (baseGamePlayer == null || baseGamePlayer.getId() == fightTenRoom.getBanker().getId()) { // 庄家不检查下注
                        continue;
                    }
                    FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
                    if (tenPlayer.getBetMoney() == null) { // 还有人未下注就默认设置为25%那档
                        tenPlayer.setBetMoney(tenPlayer.getBetMoneyList().get(0));
                        // 广播给房间内所有玩家默认下注信息
                        OseeFightTenMessage.TenBetMoneyResponse.Builder builder = OseeFightTenMessage.TenBetMoneyResponse
                                .newBuilder();
                        builder.setPlayerId(tenPlayer.getId());
                        builder.setBetMoney(tenPlayer.getBetMoney());
                        sendRoomMessage(fightTenRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_BET_MONEY_RESPONSE_VALUE,
                                builder);
                    }
                }

                // 切到看牌阶段
                fightTenRoom.setRoomState(RoomState.SEE_CARD.getIndex());
                // 发送房间状态改变响应
                sendChangeRoomStateResponse(fightTenRoom);

                // 发送最后一张牌
                sendLastOneCard(fightTenRoom);
            }
        } else if (roomState == RoomState.SEE_CARD.getIndex()) { // 看牌
            if (currentTime >= enterStateTime + RoomState.SEE_CARD.getTime()) {
                // 切到对局结束阶段
                fightTenRoom.setRoomState(RoomState.OVER.getIndex());
                // 发送房间状态改变响应
                sendChangeRoomStateResponse(fightTenRoom);
                // 对局结束
                roundOver(fightTenRoom);
            }
        } else if (roomState == RoomState.OVER.getIndex()) { // 结算亮牌阶段
            if (currentTime >= enterStateTime + RoomState.OVER.getTime() + fightTenRoom.getPlayerSize()) { // 还要每个玩家加1秒，避免结算没有过完就进入下一阶段
                // 对局结束，开始准备下一场的对局
                checkPlayerLeaveRoom(fightTenRoom);
                // 房间内数据重置
                fightTenRoom.roundReset();

                if (fightTenRoom.getPlayerSize() < MIN_PLAYER_NUM) {
                    fightTenRoom.setRoomState(RoomState.NONE.getIndex());
                } else {
                    fightTenRoom.setRoomState(RoomState.READY.getIndex());
                }
                // 发送房间状态改变响应
                sendChangeRoomStateResponse(fightTenRoom);
            }
        } else {
            fightTenRoom.setRoomState(roomState);
            fightTenRoom.setEnterStateTime(0L);
            checkPlayerLeaveRoom(fightTenRoom);
        }
    }

    /**
     * 拼十重连
     *
     * @param room   拼十房间
     * @param player 拼十玩家
     */
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
                OseeFightTenMessage.TenReconnectResponse.Builder reconnect = OseeFightTenMessage.TenReconnectResponse
                        .newBuilder();

                reconnect.setRoomCode(room.getCode());
                reconnect.setRoomState(room.getRoomState());
                reconnect.setStateRestTime((int) (RoomState.getTimeByIndex(room.getRoomState())
                        - (System.currentTimeMillis() / 1000 - room.getEnterStateTime())));

                BaseGamePlayer[] gamePlayers = room.getGamePlayers();
                for (BaseGamePlayer baseGamePlayer : gamePlayers) {
                    if (baseGamePlayer == null) {
                        continue;
                    }
                    FightTenPlayer tenPlayer = (FightTenPlayer) baseGamePlayer;
                    if (tenPlayer.getId() == player.getId()) { // 自己的座位和牌信息
                        reconnect.setSeat(tenPlayer.getSeat());
                        int cardSize = tenPlayer.getCards().size();
                        for (int i = 0; i < cardSize; i++) {
                            if (i < cardSize - 1) {
                                // 自己的四张手牌
                                reconnect.addCards(tenPlayer.getCards().get(i));
                            } else {
                                // 自己的最后一张牌
                                reconnect.setLastCard(tenPlayer.getCards().get(i));
                            }
                        }
                        // 自己的下注列表
                        reconnect.addAllBetMoneyList(tenPlayer.getBetMoneyList());
                    }
                    if (tenPlayer.getReadyType() == 0) { // 已准备玩家
                        reconnect.addReadyPlayers(tenPlayer.getId());
                    }

                    // 抢庄信息
                    OseeFightTenMessage.TenFightBankerProto.Builder fightBankerProto = OseeFightTenMessage.TenFightBankerProto
                            .newBuilder();
                    fightBankerProto.setPlayerId(tenPlayer.getId());
                    Integer fightMultiple = tenPlayer.getFightMultiple();
                    fightBankerProto.setFightMultiple(fightMultiple == null ? -1 : fightMultiple);
                    reconnect.addFightedBankerProto(fightBankerProto);
                    if (room.getBanker() != null) { // 已选择庄家
                        reconnect.setBankerId(room.getBanker().getId());
                        reconnect.addFightBankerProto(fightBankerProto);
                    }

                    if (tenPlayer.getBetMoney() != null) { // 玩家已下注
                        OseeFightTenMessage.TenBetMoneyProto.Builder betMoneyProto = OseeFightTenMessage.TenBetMoneyProto
                                .newBuilder();
                        betMoneyProto.setPlayerId(tenPlayer.getId());
                        betMoneyProto.setBetMoney(tenPlayer.getBetMoney());
                        reconnect.addBetMoneyProto(betMoneyProto);
                    }
                    if (tenPlayer.getSeeOrRubCard()) { // 已经看牌或者搓牌的玩家
                        reconnect.addSeeOrRubCardPlayer(tenPlayer.getId());
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
                    reconnect.addRoundOverProto(overPlayerDataProto);
                }

                // 参与抢庄动画的玩家id列表
                if (room.getFightBankerRandomPlayerIdList().size() > 0) {
                    reconnect.addAllAniPlayerId(room.getFightBankerRandomPlayerIdList());
                }

                // 发送重连数据响应
                NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_OSEE_TEN_RECONNECT_RESPONSE_VALUE, reconnect,
                        player.getUser());

                logger.info("拼十:已经给玩家[{}]发送了房间[{}]的重连数据", player.getId(), room.getCode());
            } catch (Exception e) {
                logger.error("拼十:玩家[{}]重连出错", player.getId());
                e.printStackTrace();
            }
        });
    }

    // ******************************** 消息相关 ************************************

    /**
     * 创建玩家信息消息体
     *
     * @param fightTenPlayer 拼十玩家
     */
    protected OseeFightTenMessage.TenRoomPlayerInfoProto.Builder createPlayerInfoProto(FightTenPlayer fightTenPlayer) {
        if (fightTenPlayer == null) {
            return null;
        }

        OseeFightTenMessage.TenRoomPlayerInfoProto.Builder builder = OseeFightTenMessage.TenRoomPlayerInfoProto
                .newBuilder();

        UserEntity userEntity = fightTenPlayer.getUser().getEntity();
        builder.setPlayerId(userEntity.getId());
        builder.setSex(userEntity.getSex());
        builder.setHeadIndex(userEntity.getHeadIndex());
        builder.setHeadUrl(userEntity.getHeadUrl());
        builder.setName(userEntity.getNickname());

        builder.setSeat(fightTenPlayer.getSeat());
        builder.setMoney(fightTenPlayer.getMoney());
        builder.setReadyType(fightTenPlayer.getReadyType());
        return builder;
    }

    /**
     * 将房间内指定玩家的信息发给其他玩家
     *
     * @param fightTenRoom   拼十房间
     * @param fightTenPlayer 玩家
     */
    protected void sendRoomPlayerInfoResponse(FightTenRoom fightTenRoom, FightTenPlayer fightTenPlayer) {
        OseeFightTenMessage.TenRoomPlayerInfoResponse.Builder builder = OseeFightTenMessage.TenRoomPlayerInfoResponse
                .newBuilder();
        builder.setPlayerInfo(createPlayerInfoProto(fightTenPlayer));
        sendRoomMessage(fightTenRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_ROOM_PLAYER_INFO_RESPONSE_VALUE, builder);
    }

    /**
     * 将房间内所有玩家信息发送给指定玩家
     *
     * @param fightTenRoom   拼十房间
     * @param fightTenPlayer 玩家
     */
    protected void sendRoomPlayerInfoListResponse(FightTenRoom fightTenRoom, FightTenPlayer fightTenPlayer) {
        OseeFightTenMessage.TenRoomPlayerInfoListResponse.Builder builder = OseeFightTenMessage.TenRoomPlayerInfoListResponse
                .newBuilder();
        for (BaseGamePlayer gamePlayer : fightTenRoom.getGamePlayers()) {
            if (gamePlayer != null) {
                builder.addPlayerInfos(createPlayerInfoProto((FightTenPlayer) gamePlayer));
            }
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_OSEE_TEN_ROOM_PLAYER_INFO_LIST_RESPONSE_VALUE, builder,
                fightTenPlayer.getUser());
    }

    /**
     * 发送给玩家拼十房间信息
     *
     * @param fightTenRoom   拼十房间
     * @param fightTenPlayer 玩家
     */
    protected void sendRoomInfoResponse(FightTenRoom fightTenRoom, FightTenPlayer fightTenPlayer) {
        if (fightTenRoom != null) {
            OseeFightTenMessage.TenRoomInfoResponse.Builder builder = OseeFightTenMessage.TenRoomInfoResponse
                    .newBuilder();
            builder.setRoomCode(fightTenRoom.getCode());
            builder.setFieldType(fightTenRoom.getFieldType());
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_OSEE_TEN_ROOM_INFO_RESPONSE_VALUE, builder,
                    fightTenPlayer.getUser());
        }
    }

    /**
     * 广播消息给房间内所有玩家
     *
     * @param fightTenRoom 拼十房间
     */
    protected void sendRoomMessage(FightTenRoom fightTenRoom, int msgCode, GeneratedMessage.Builder<?> message) {
        if (fightTenRoom != null) {
            for (BaseGamePlayer gamePlayer : fightTenRoom.getGamePlayers()) {
                if (gamePlayer != null) {
                    NetManager.sendMessage(msgCode, message, gamePlayer.getUser());
                }
            }
        }
    }

    /**
     * 广播房间状态改变响应
     *
     * @param fightTenRoom 拼十房间
     */
    protected void sendChangeRoomStateResponse(FightTenRoom fightTenRoom) {
        OseeFightTenMessage.TenChangeRoomStateResponse.Builder builder = OseeFightTenMessage.TenChangeRoomStateResponse.newBuilder();
        builder.setRoomState(fightTenRoom.getRoomState());
        builder.setStateRestTime((int) (RoomState.getTimeByIndex(fightTenRoom.getRoomState())
                - (System.currentTimeMillis() / 1000 - fightTenRoom.getEnterStateTime())));
        sendRoomMessage(fightTenRoom, OseeMessage.OseeMsgCode.S_C_OSEE_TEN_CHANGE_ROOM_STATE_RESPONSE_VALUE, builder);
    }

    // ********************************** GM相关 ***********************************

    /**
     * 后台设置拼十场次属性信息 并放入Redis中
     *
     * @param param 参数
     */
    public CommonResponse gmSetFieldConfig(Map<String, Object> param) {
        if (param == null || param.size() <= 0) {
            return new CommonResponse("PARAM_ERROR", "传入参数错误！");
        }
        Gson gson = new Gson();
        FieldConfig fieldConfig = gson.fromJson(gson.toJson(param), FieldConfig.class);
        if (fieldConfig == null) {
            return new CommonResponse("PARAM_FORMAT_ERROR", "传入参数格式错误！");
        }
        for (FieldConfig.Config config : fieldConfig.getConfigs()) {
            // 设置默认值
            switch (config.getType()) {
                case 0: // 初
                    if (config.getEnterMoney() == null) {
                        config.setEnterMoney(10000L);
                    }
                    if (config.getMaxBetMoney() == null) {
                        config.setMaxBetMoney(50000L);
                    }
                    break;
                case 1: // 中
                    if (config.getEnterMoney() == null) {
                        config.setEnterMoney(100000L);
                    }
                    if (config.getMaxBetMoney() == null) {
                        config.setMaxBetMoney(100000L);
                    }
                    break;
                case 2: // 高
                    if (config.getEnterMoney() == null) {
                        config.setEnterMoney(1000000L);
                    }
                    if (config.getMaxBetMoney() == null) {
                        config.setMaxBetMoney(300000L);
                    }
                    break;
            }
        }
        // 按照初中高的顺序进行排序
        fieldConfig.getConfigs().sort((o1, o2) -> {
            if (o1.getType() < o2.getType()) {
                return -1;
            } else if (o1.getType() > o2.getType()) {
                return 1;
            }
            return 0;
        });
        RedisHelper.set(REDIS_FIELD_CONFIG_KEYNAME, gson.toJson(fieldConfig));
        return new CommonResponse(true);
    }

    /**
     * 后台设置拼十机器人配置
     *
     * @param param 后台传的参数
     */
    public CommonResponse gmSetRobotConfig(Map<String, Object> param) {
        if (param == null || param.size() <= 0) {
            return new CommonResponse("PARAM_ERROR", "传入参数错误！");
        }

        // 获取设置的机器人赢取的金币
        long tenRobotTotalWinMoney = (long) (double) param.get("tenRobotTotalWinMoney");
        // 更改数据库保存的数据
        RedisHelper.set(REDIS_ROBOT_WIN_MONEY_KEY, String.valueOf(tenRobotTotalWinMoney));

        // 获取拼十机器人设置
        Gson gson = new Gson();
        RobotConfig robotConfig = gson.fromJson(gson.toJson(param.get("fighttenRobot")), RobotConfig.class);
        if (robotConfig == null) {
            return new CommonResponse("PARAM_FORMAT_ERROR", "传入参数格式错误！");
        }
        if (robotConfig.getRefreshTimeRangeBegin() >= robotConfig.getRefreshTimeRangeEnd()) {
            return new CommonResponse("PARAM_FORMAT_ERROR", "刷新开始时间应该小于结束时间！");
        }
        String configJsonStr = gson.toJson(robotConfig);
        RedisHelper.set(REDIS_ROBOT_CONFIG_KEYNAME, configJsonStr);
        return new CommonResponse(true);
    }

    /**
     * 机器人金币数量清零
     */
    public CommonResponse gmResetRobotMoney() {
        RedisHelper.set(REDIS_ROBOT_LOSE_MONEY_KEY, "0");
        RedisHelper.set(REDIS_ROBOT_WIN_MONEY_KEY, "0");
        return new CommonResponse(true);
    }

    public static String check(int num) {
        String info = "";
        switch (num) {
            case 0:
                info = "散牌";
                break;
            case 1:
                info = "十带一";
                break;
            case 2:
                info = "十带二";
                break;
            case 3:
                info = "十带三";
                break;
            case 4:
                info = "十带四";
                break;
            case 5:
                info = "十带五";
                break;
            case 6:
                info = "十带六";
                break;
            case 7:
                info = "十带七";
                break;
            case 8:
                info = "十带八";
                break;
            case 9:
                info = "十带九";
                break;
            case 10:
                info = "双十";
                break;
            case 11:
                info = "顺子";
                break;
            case 12:
                info = "同花";
                break;
            case 13:
                info = "葫芦";
                break;
            case 14:
                info = "炸弹";
                break;
            case 15:
                info = "同花顺";
                break;
        }
        return info;
    }

    /**
     * 发送游走字幕
     */
    protected void sendWanderSubtitle(String info) {
        LobbyMessage.WanderSubtitleResponse.Builder builder = LobbyMessage.WanderSubtitleResponse.newBuilder();
        builder.setLevel(1);
        builder.setContent(info);
        PlayerManager.sendMessageToOnline(LobbyMessage.LobbyMsgCode.S_C_WANDER_SUBTITLE_RESPONSE_VALUE, builder.build());
    }
}
