package com.maple.game.osee.listener;

import com.maple.engine.container.DataContainer;
import com.maple.game.osee.entity.fightten.FightTenPlayer;
import com.maple.game.osee.entity.fightten.FightTenRoom;
import com.maple.game.osee.entity.fightten.challenge.FightTenChallengePlayer;
import com.maple.game.osee.entity.fightten.challenge.FightTenChallengeRoom;
import com.maple.game.osee.entity.fishing.FishingGamePlayer;
import com.maple.game.osee.entity.fishing.FishingGameRoom;
import com.maple.game.osee.entity.fishing.challenge.FishingChallengePlayer;
import com.maple.game.osee.entity.fishing.challenge.FishingChallengeRoom;
import com.maple.game.osee.entity.fishing.csv.file.BatteryLevelConfig;
import com.maple.game.osee.entity.gobang.GobangGameRoom;
import com.maple.game.osee.entity.two_eight.TwoEightPlayer;
import com.maple.game.osee.entity.two_eight.TwoEightRoom;
import com.maple.game.osee.manager.fightten.FightTenChallengeManager;
import com.maple.game.osee.manager.fightten.FightTenManager;
import com.maple.game.osee.manager.fishing.FishingChallengeManager;
import com.maple.game.osee.manager.fishing.FishingManager;
import com.maple.game.osee.manager.gobang.GobangManager;
import com.maple.game.osee.manager.lobby.CommonLobbyManager;
import com.maple.game.osee.manager.two_eight.TwoEightManager;
import com.maple.gamebase.container.GameContainer;
import com.maple.gamebase.data.BaseGamePlayer;
import com.maple.gamebase.data.BaseGameRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.maple.common.login.event.login.ILoginEventListener;
import com.maple.common.login.event.login.LoginEvent;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.manager.PlayerManager;

/**
 * 1688玩家登录监听器
 */
@Component
public class OseeLoginListener implements ILoginEventListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OseePlayerMapper playerMapper;

    @Autowired
    private FightTenManager fightTenManager;

    @Autowired
    private FightTenChallengeManager tenChallengeManager;

    @Autowired
    private GobangManager gobangManager;

    @Autowired
    private TwoEightManager twoEightManager;

    @Autowired
    private CommonLobbyManager commonLobbyManager;

    @Autowired
    private FishingManager fishingManager;

    @Autowired
    private FishingChallengeManager fishingChallengeManager;

    @Override
    public void handleLoginEvent(LoginEvent event) {
        ServerUser user = event.getUser();

        logger.info("天天摸鱼:玩家[{}:{}]登录", user.getId(), user.getNickname());

        if (PlayerManager.getPlayerEntity(user, false) == null) {
            OseePlayerEntity entity = playerMapper.findByUserId(user.getId());
            if (entity == null) {
                entity = new OseePlayerEntity();
                entity.setUserId(user.getId());
                entity.setMoney(1000);
                // 初始炮台等级为最低等级
                entity.setBatteryLevel(DataContainer.getData(1, BatteryLevelConfig.class).getBatteryLevel());
                playerMapper.save(entity);
            }

            user.putExpertData(OseePlayerEntity.EntityId, entity);
            PlayerManager.updateEntities.add(entity);
        }

        PlayerManager.sendPlayerLevelResponse(user);
        PlayerManager.sendPlayerMoneyResponse(user);
        PlayerManager.sendVipLevelResponse(user);
        PlayerManager.sendPlayerBatteryLevelResponse(user);

        // 发送月卡每日奖励
        commonLobbyManager.sendDailyMonthCardRewards(user);
        // 发送VIP每日奖励
        commonLobbyManager.sendDailyVipRewards(user);
        // 检查vip的金币补足情况
        commonLobbyManager.checkVipMoneyEnough(user);
        // 发送后台系统邮件给玩家
        commonLobbyManager.sendSystemMail(user);

        // 检查房间是否需要重连
        BaseGamePlayer gamePlayer = GameContainer.getPlayerById(user.getId());
        if (gamePlayer != null) {
            BaseGameRoom gameRoom = GameContainer.getGameRoomByCode(gamePlayer.getRoomCode());
            if (gameRoom != null) {
                if (gameRoom instanceof FightTenChallengeRoom) { // 房间是拼十挑战赛房间
                    tenChallengeManager.reconnect((FightTenChallengeRoom) gameRoom, (FightTenChallengePlayer) gamePlayer);
                } else if (gameRoom instanceof FightTenRoom) { // 房间是拼十房间就要发送重连信息
                    fightTenManager.reconnect((FightTenRoom) gameRoom, (FightTenPlayer) gamePlayer);
                } else if (gameRoom instanceof GobangGameRoom) { // 五子棋房间重连
                    gobangManager.reconnect((GobangGameRoom) gameRoom, user);
                } else if (gameRoom instanceof TwoEightRoom) { // 房间是二八杠房间就要发送重连
                    twoEightManager.reconnect((TwoEightRoom) gameRoom, (TwoEightPlayer) gamePlayer);
                } else if (gameRoom instanceof FishingChallengeRoom) { // 捕鱼挑战赛
                    fishingChallengeManager.reconnect((FishingChallengeRoom) gameRoom, (FishingChallengePlayer) gamePlayer);
                } else if (gameRoom instanceof FishingGameRoom) { // 普通捕鱼
                    fishingManager.reconnect((FishingGameRoom) gameRoom, (FishingGamePlayer) gamePlayer);
                }
            }
        }
    }

}
