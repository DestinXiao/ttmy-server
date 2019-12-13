package com.maple.game.osee.manager.lobby;

import com.maple.engine.container.UserContainer;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.GetRankingListResponse;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.RankingDataProto;
import com.maple.network.manager.NetManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * 排行榜管理类
 */
@Component
public class RankingManager {

    @Autowired
    private OseePlayerMapper playerMapper;

    /**
     * 金币排行榜
     */
    private List<RankingData> moneyRanking = new LinkedList<>();

    /**
     * VIP排行榜
     */
    private List<RankingData> vipRanking = new LinkedList<>();

    /**
     * 黄金鱼雷排行榜
     */
    private List<RankingData> torpedoRanking = new LinkedList<>();

    /**
     * 排名显示数量
     */
    private static final int RANKING_COUNT = 30;

//    private static final String REDIS_KEY = "Ttmy:Sys:RankingData";

    /**
     * 每5分钟执行一次刷新排行榜数据
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void update() {
        List<OseePlayerEntity> moneyEntities = playerMapper.selectMoneyRanking(RANKING_COUNT);
        List<OseePlayerEntity> vipEntities = playerMapper.selectVipRanking(RANKING_COUNT);
        List<OseePlayerEntity> torpedoEntities = playerMapper.selectGoldTorpedoRanking(RANKING_COUNT);

        // 金币排行榜
        moneyRanking.clear();
        for (OseePlayerEntity entity : moneyEntities) {
            moneyRanking.add(RankingData.createData(entity));
        }

        // VIP等级排行榜
        vipRanking.clear();
        for (OseePlayerEntity entity : vipEntities) {
            vipRanking.add(RankingData.createData(entity));
        }

        // 鱼雷排行榜
        torpedoRanking.clear();
        for (OseePlayerEntity entity : torpedoEntities) {
            torpedoRanking.add(RankingData.createData(entity));
        }
    }

    /**
     * 发送排行榜数据
     */
    public void sendRankingList(ServerUser user, int rankingType) {
        GetRankingListResponse.Builder builder = GetRankingListResponse.newBuilder();
        builder.setRankingType(rankingType);

        List<RankingData> dataList;
        if (rankingType == 0) {
            dataList = moneyRanking;
        } else if (rankingType == 1) {
            dataList = vipRanking;
        } else {
            dataList = torpedoRanking;
        }

        int myRanking = 0, index = 0;
        for (RankingData data : dataList) {
            index++;
            if (data.id == user.getId()) { // 如果是自己的ID就说明当前排名是自己
                myRanking = index;
            }
            RankingDataProto.Builder proto = RankingDataProto.newBuilder();
            proto.setId(data.id);
            proto.setLevel(data.level);
            proto.setMoney(data.money);
            proto.setNickname(data.nickname);
            proto.setVipLevel(data.vipLevel);
            proto.setGoldTorpedo(data.goldTorpedo);
            proto.setHead(data.getHead());
            builder.addRankingList(proto);
        }
        builder.setMyRanking(myRanking);
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_GET_RANKING_LIST_RESPONSE_VALUE, builder, user);
    }

    /**
     * 排行数据
     */
    public static class RankingData {

        /**
         * 玩家id
         */
        private long id;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 等级
         */
        private int level;

        /**
         * vip等级
         */
        private int vipLevel;

        /**
         * 金币
         */
        private long money;

        /**
         * 黄金鱼雷
         */
        private long goldTorpedo;

        /**
         * 头像
         */
        private String head;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int getVipLevel() {
            return vipLevel;
        }

        public void setVipLevel(int vipLevel) {
            this.vipLevel = vipLevel;
        }

        public long getMoney() {
            return money;
        }

        public void setMoney(long money) {
            this.money = money;
        }

        public String getHead() {
            return head;
        }

        public void setHead(String head) {
            this.head = head;
        }

        public long getGoldTorpedo() {
            return goldTorpedo;
        }

        public void setGoldTorpedo(long goldTorpedo) {
            this.goldTorpedo = goldTorpedo;
        }

        public static RankingData createData(OseePlayerEntity entity) {
            ServerUser user = UserContainer.getUserById(entity.getUserId());
            RankingData data = new RankingData();
            data.setId(entity.getId());
            data.setNickname(user.getNickname());
            data.setMoney(entity.getMoney());
            data.setLevel(entity.getLevel());
            data.setVipLevel(entity.getVipLevel());
            data.setGoldTorpedo(entity.getGoldTorpedo());
            data.setHead(user.getEntity().getHeadIndex() == 0 ? user.getEntity().getHeadUrl() :
                    Integer.toString(user.getEntity().getHeadIndex()));
            return data;
        }
    }

}
