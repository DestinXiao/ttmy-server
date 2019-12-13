package com.maple.game.osee.timer.fightten;

import com.maple.engine.data.ServerUser;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.entity.fightten.FightTenRoom;
import com.maple.game.osee.entity.fightten.challenge.FightTenChallengePlayer;
import com.maple.game.osee.entity.fightten.challenge.FightTenChallengeRoom;
import com.maple.game.osee.manager.fightten.FightTenChallengeManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.fightten.TtmyFightTenChallengeMessage;
import com.maple.gamebase.container.GameContainer;
import com.maple.network.manager.NetManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 拼十挑战赛房间定时检测任务
 */
@Component
public class ChallengeRoomCheckTask {

    private final FightTenChallengeManager challengeManager;

    public ChallengeRoomCheckTask(FightTenChallengeManager challengeManager) {
        this.challengeManager = challengeManager;
    }

    /**
     * 房间检测，状态控制
     * <p>
     * 每300ms运行一次
     */
    @Scheduled(fixedRate = 300)
    public void stateCheck() {
        List<FightTenChallengeRoom> rooms = GameContainer.getGameRooms(FightTenChallengeRoom.class)
                .stream()
                .sorted(Comparator.comparing(FightTenChallengeRoom::getRoomState)) // 从NONE、READY状态往后排序
                .collect(Collectors.toList());
        for (FightTenChallengeRoom room : rooms) {
            if (room == null) {
                continue;
            }
            ConcurrentLinkedQueue<Map<ServerUser, LocalDateTime>> matchQueue = FightTenChallengeManager.matchQueue;
            if (matchQueue.size() > 0) {
                if (room.getFeeType() == 1 && // 挑战房
                        !room.isPrivateRoom() && // 非亲友房
                        room.getPlayerSize() < FightTenRoom.MAX_PLAYER_NUM &&
                        room.getRoomState() <= FightTenRoom.RoomState.READY.getIndex()) {
                    Map<ServerUser, LocalDateTime> map = matchQueue.poll(); // 返回第一个元素并删除
                    if (map != null) {
                        ServerUser user = map.keySet().iterator().next();
                        if (user != null &&
                                user.isOnline() &&
                                user.getEntity().getOnlineState() == 1) { // 未在其他房间中
                            TtmyFightTenChallengeMessage.TenChallengeMatchResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeMatchResponse.newBuilder();
                            builder.setResult(4);
                            builder.setInfo("匹配成功");
                            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_MATCH_RESPONSE_VALUE, builder, user);
                            // 玩家匹配进房间
                            FightTenChallengePlayer gamePlayer = GameContainer.createGamePlayer(room, user, FightTenChallengePlayer.class);
                            challengeManager.addRoomPlayer(room, gamePlayer);
                        }
                    }
                }
            }
            ThreadPoolUtils.TASK_SERVICE_POOL.execute(() -> challengeManager.dealRoomState(room));
        }
    }

    /**
     * 匹配定时检测
     */
    @Scheduled(fixedRate = 300)
    public void matchCheck() {
        Iterator<Map<ServerUser, LocalDateTime>> iterator = FightTenChallengeManager.matchQueue.iterator();
        while (iterator.hasNext()) {
            Map<ServerUser, LocalDateTime> map = iterator.next();
            for (Map.Entry entry : map.entrySet()) {
                LocalDateTime overTime = (LocalDateTime) entry.getValue();
                if (LocalDateTime.now().isAfter(overTime)) { // 匹配时间到
                    iterator.remove(); // 删除此匹配玩家
                    ServerUser user = (ServerUser) entry.getKey();
                    TtmyFightTenChallengeMessage.TenChallengeMatchResponse.Builder builder = TtmyFightTenChallengeMessage.TenChallengeMatchResponse.newBuilder();
                    builder.setResult(3);
                    builder.setInfo("匹配时间到");
                    NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_TEN_CHALLENGE_MATCH_RESPONSE_VALUE, builder, user);
                }
            }
        }
    }
}
