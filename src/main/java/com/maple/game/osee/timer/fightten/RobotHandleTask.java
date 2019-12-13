package com.maple.game.osee.timer.fightten;

import com.maple.database.data.entity.UserEntity;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.entity.fightten.FightTenRobotPlayer;
import com.maple.game.osee.entity.fightten.FightTenRoom;
import com.maple.game.osee.entity.fightten.FightTenRoom.RoomState;
import com.maple.game.osee.entity.fightten.config.FieldConfig;
import com.maple.game.osee.entity.fightten.config.RobotConfig;
import com.maple.game.osee.manager.BaseRobotManager;
import com.maple.game.osee.manager.fightten.FightTenManager;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 拼十机器人操作处理定时任务
 */
@Component
public class RobotHandleTask extends BaseRobotManager {

    @Autowired
    private FightTenManager fightTenManager;

    /**
     * 房间机器人操作
     * <p>
     * corn:每1s运行一次
     */
    @Scheduled(fixedRate = 1000)
    public void handle() {
        try {
            List<FightTenRoom> fightTenRooms = GameContainer.getGameRooms(FightTenRoom.class);
            long nowTimeMillis = System.currentTimeMillis();
            for (FightTenRoom fightTenRoom : fightTenRooms) {
                if (fightTenRoom == null) {
                    continue;
                }

                int playerCount = 0; // 真实玩家数量
                int robotCount = 0; // 机器人数量
                for (BaseGamePlayer baseGamePlayer : fightTenRoom.getGamePlayers()) {
                    if (baseGamePlayer != null) {
                        if (baseGamePlayer instanceof FightTenRobotPlayer) {
                            robotCount++;
                        } else {
                            playerCount++;
                        }
                    }
                }

                // 房间状态
                int roomState = fightTenRoom.getRoomState();
                if (roomState == RoomState.NONE.getIndex() || roomState == RoomState.READY.getIndex()) {
                    // 获取机器人配置
                    RobotConfig robotConfig = FightTenManager.getRobotConfig();
                    // 开启了机器人，房间玩家数量少于最大玩家数，机器人数量少于后台设置的
                    if (robotConfig.getUseRobot() == 1 &&
                            fightTenRoom.getPlayerSize() < fightTenRoom.getMaxSize() &&
                            robotCount < robotConfig.getRobotNum()) {
                        // 随机间隔时间创建机器人
                        if ((nowTimeMillis - fightTenRoom.getLastCreateRobotTime()) / 1000 >=
                                ThreadLocalRandom.current().nextInt(robotConfig.getRefreshTimeRangeBegin(), robotConfig.getRefreshTimeRangeEnd())) {
                            // 创建拼十机器人
                            createTenRobot(fightTenRoom);
                        }
                    } else if (playerCount == 0 && robotCount > 0) { // 玩家走完了，还有机器人
                        for (BaseGamePlayer player : fightTenRoom.getGamePlayers()) {
                            if (player != null) {
                                fightTenManager.leaveRoom(player.getUser(), true);
                            }
                        }
                    }
                    for (BaseGamePlayer player : fightTenRoom.getGamePlayers()) {
                        if (player == null) {
                            continue;
                        }
                        if (!(player instanceof FightTenRobotPlayer)) {
                            continue;
                        }
                        FightTenRobotPlayer robotPlayer = (FightTenRobotPlayer) player;
                        if (robotPlayer.getReadyType() == 1) { // 机器人未准备
                            if (robotPlayer.getLeaveTime() < nowTimeMillis) { // 到了机器人消失的时间就离开房间
                                fightTenManager.leaveRoom(player.getUser(), true);
                                continue;
                            }

//                        int randTime = ThreadLocalRandom.current().nextInt(1, 5);
//                        if ((nowTimeMillis / 1000 - fightTenRoom.getEnterStateTime()) >= randTime) {
                            // 机器人准备
                            fightTenManager.readyRoom(0, player.getUser());
//                        }
                        }
                    }
                } else if (roomState == RoomState.FIGHT_BANKER.getIndex()) {
                    for (BaseGamePlayer player : fightTenRoom.getGamePlayers()) {
                        if (player == null) {
                            continue;
                        }
                        if (!(player instanceof FightTenRobotPlayer)) {
                            continue;
                        }
                        FightTenRobotPlayer robotPlayer = (FightTenRobotPlayer) player;
                        if (robotPlayer.getFightMultiple() == null) { // 机器人未抢庄
//                        int randTime = ThreadLocalRandom.current().nextInt(1, 4);
//                        if ((nowTimeMillis / 1000 - fightTenRoom.getEnterStateTime()) >= RoomState.FIGHT_BANKER.getTime() - randTime) {
                            // 机器人抢庄
//                            int randFightMultiple = ThreadLocalRandom.current().nextInt(1, 5);
                            int randFightMultiple = ThreadLocalRandom.current().nextInt(0, 2);
                            fightTenManager.fightBanker(randFightMultiple == 0 ? -1 : 4, player.getUser());
//                        }
                        }
                    }
                } else if (roomState == RoomState.BET_MONEY.getIndex()) {
                    for (BaseGamePlayer player : fightTenRoom.getGamePlayers()) {
                        if (player == null) {
                            continue;
                        }
                        if (!(player instanceof FightTenRobotPlayer)) {
                            continue;
                        }
                        FightTenRobotPlayer robotPlayer = (FightTenRobotPlayer) player;
                        if (robotPlayer.getBetMoney() == null && robotPlayer.getBetMoneyList().size() > 0) { // 机器人未下注
//                        int randTime = ThreadLocalRandom.current().nextInt(1, 4);
//                        if ((nowTimeMillis / 1000 - fightTenRoom.getEnterStateTime()) >= RoomState.BET_MONEY.getTime() - randTime) {
                            // 机器人下注
                            int randBetMoneyIndex = ThreadLocalRandom.current().nextInt(0, robotPlayer.getBetMoneyList().size());
                            fightTenManager.betMoney(randBetMoneyIndex, player.getUser());
//                        }
                        }
                    }
                } else if (roomState == RoomState.SEE_CARD.getIndex()) {
                    for (BaseGamePlayer player : fightTenRoom.getGamePlayers()) {
                        if (player == null) {
                            continue;
                        }
                        if (!(player instanceof FightTenRobotPlayer)) {
                            continue;
                        }
                        FightTenRobotPlayer robotPlayer = (FightTenRobotPlayer) player;
                        if (!robotPlayer.getSeeOrRubCard()) { // 机器人还未看牌/搓牌
//                        int randTime = ThreadLocalRandom.current().nextInt(1, 4);
//                        if ((nowTimeMillis / 1000 - fightTenRoom.getEnterStateTime()) >= RoomState.SEE_CARD.getTime() - randTime) {
                            // 机器人看牌
                            fightTenManager.seeOrRubCard(0, player.getUser());
//                        }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建一个拼十机器人
     *
     * @param fightTenRoom 拼十房间
     */
    public void createTenRobot(FightTenRoom fightTenRoom) {
        if (fightTenRoom.getPlayerSize() == fightTenRoom.getMaxSize()) {
            return;
        }
        // 新建机器人服务器用户实体
        ServerUser robotServerUser = new ServerUser();
        robotServerUser.setEntity(new UserEntity());
        robotServerUser.setOnline(true);

        // 新建机器人数据库实体
        // 创建机器人id
        robotServerUser.getEntity().setId(getNewRobotId());
        robotServerUser.getEntity().setNickname(getRobotName());
        // 性别：0-男，1-女
        robotServerUser.getEntity().setSex(ThreadLocalRandom.current().nextInt(0, 2));
        String headUrl = getRandomHeadUrl();
        if (StringUtils.isEmpty(headUrl)) {
            // 随机一个系统头像下标
            robotServerUser.getEntity().setHeadIndex(ThreadLocalRandom.current().nextInt(1, 21));
        } else {
            robotServerUser.getEntity().setHeadIndex(0);
            robotServerUser.getEntity().setHeadUrl(headUrl);
        }

        // 设置房间上次创建机器人时间
        fightTenRoom.setLastCreateRobotTime(System.currentTimeMillis());

        // 获取场次配置
        FieldConfig.Config config = FightTenManager.getFieldConfigList().get(fightTenRoom.getFieldType());
        // 为房间创建机器人玩家
        FightTenRobotPlayer robotPlayer = GameContainer.createGamePlayer(fightTenRoom, robotServerUser, FightTenRobotPlayer.class, true);
        // 随机生成机器人玩家的金币数量
        long start, end;
        switch (config.getType()) {
            case 0: // 初级场
                start = 50000;
                end = 5000000;
                break;
            case 1: // 中级场
                start = 100000;
                end = 20000000;
                break;
            case 2: // 高级场
                start = 5000000;
                end = 80000000;
                break;
            default:
                start = config.getEnterMoney();
                end = config.getEnterMoney();
                break;
        }
        long money = ThreadLocalRandom.current().nextLong(start, end);
        // 设置机器人的金币
        robotPlayer.setMoney(money);
        // 机器人加入房间
        fightTenManager.addRoomPlayer(fightTenRoom, robotPlayer);
    }
}
