package com.maple.game.osee.manager.fishing.util;

import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.data.mapper.AgentMapper;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.dao.log.entity.OseeCutMoneyLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeCutMoneyLogMapper;
import com.maple.game.osee.entity.fishing.FishingGamePlayer;
import com.maple.game.osee.manager.PlayerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class FishingUtil {

    @Autowired
    private OseeCutMoneyLogMapper cutMoneyLogMapper;

    @Autowired
    private OseePlayerMapper playerMapper;

    @Autowired
    private AgentMapper fishingRecordLogMapper;

    public static Long[] q0 = {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};

    public static Double[] ap = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

    public static Long[] apt = {100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L, 100L};
    /**
     * 保存抽水记录
     * @param player 玩家
     */
    public void saveCutProb(FishingGamePlayer player, int gameId) {
        ServerUser user = player.getUser();
        if (player.getCutMoney() != 0) {
            OseeCutMoneyLogEntity cutLog = new OseeCutMoneyLogEntity();
            cutLog.setUserId(user.getId());
            cutLog.setGame(gameId);
            cutLog.setCutMoney(player.getCutMoney());
            cutMoneyLogMapper.save(cutLog);
        }

        playerMapper.update(PlayerManager.getPlayerEntity(user));
    }


    public static double getQ0(int index) {
        return q0[index];
    }

    public static void setQ0(int index, long val) {
        q0[index] = val;
    }

    public static long getApt(int index) {
        return apt[index];
    }

    public static void setApt(int index, long val) {
        apt[index] = val;
    }

    public static double getAp(int index) {
        return ap[index];
    }

    public static void setAP(int index, double val) {
        ap[index] = val;
    }



}
