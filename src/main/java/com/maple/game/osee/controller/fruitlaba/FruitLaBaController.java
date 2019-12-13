package com.maple.game.osee.controller.fruitlaba;

import com.google.protobuf.Message;
import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.log.entity.CrystalExchangeLogEntity;
import com.maple.game.osee.dao.log.mapper.CrystalExchangeLogMapper;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.manager.fruitlaba.FruitLaBaManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.OseePublicData;
import com.maple.game.osee.proto.fruit.OseeFruitMessage;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.FruitLaBaLeaveRoomRequest;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.FruitLaBaStartRunRequest;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.FruitLabaReceiveTaskRequest;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.PlayerEnterFruitLaBaRoomRequest;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 水果拉霸控制器
 *
 * @author lzr
 * <p>
 * 2018年12月27日
 */
@AppController
public class FruitLaBaController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private FruitLaBaManager fruitLaBaManager;

    @Autowired
    private CrystalExchangeLogMapper crystalExchangeLogMapper;

    /**
     * 默认检查器
     */
    public void checker(Method taskMethod, Message req, ServerUser user, Long exp) throws Exception {
        taskMethod.invoke(this, req, user);
    }

    /**
     * 处理玩家进入水果拉霸
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_PLAYER_ENTER_FRUITLABA_ROOM_REQUEST_VALUE)
    public void doPlayerEnterRoom(PlayerEnterFruitLaBaRoomRequest req, ServerUser user) {
        logger.info("玩家:[{}],开始进入水果拉霸房间", user.getNickname());
        logger.info("玩家:[{}],开始进入水果拉霸[{}号]房间", user.getUsername(), req.getRoomType());
        fruitLaBaManager.playerEnterFruitLaBaRoom(user, req.getRoomType());
    }

    /**
     * 玩家领取任务奖励
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_FRUITLABA_RECEIVE_TASK_REQUEST_VALUE)
    public void receiveTaskReward(FruitLabaReceiveTaskRequest req, ServerUser user) {
        logger.info("玩家[{}]领取水果拉霸任务奖励", user.getUsername());
        fruitLaBaManager.receiveTaskReward(req.getRewardId(), user);
    }

    /**
     * 处理玩家旋转转盘
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_FRUITLABA_START_RUN_REQUEST_VALUE)
    public void doPlayerStartRotate(FruitLaBaStartRunRequest req, ServerUser user) {
        logger.info("玩家[{}]开始水果拉霸旋转抽奖", user.getUsername());
        // 下注的线
        List<Integer> lines = new ArrayList<>(req.getLinesList());
        // 单笔下注金额
        int singleGold = req.getSingleGold();
        // 处理数据
        fruitLaBaManager.startFruitLaBaRotateDraw(lines, singleGold, user);
    }

    /**
     * 处理玩家离开水果拉霸房间
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_FRUITLABA_LEAVE_ROOM_REQUEST_VALUE)
    public void doPlayerLeaveFruitLaBaRoom(FruitLaBaLeaveRoomRequest req, ServerUser user) {
        logger.info("玩家:[{}],离开水果拉霸房间", user.getNickname());
        RedisHelper.set(FruitLaBaManager.FruitDrawSign + user.getId(), "0");
        fruitLaBaManager.playerLeaveRoom(user);
    }

    /**
     * 玩家鱼雷和龙晶的兑换
     */
    @AppHandler(msgCode = OseeMsgCode.C_S_EXCHANGE_DRAGON_CRYSTAL_REQUEST_VALUE)
    public void doExchangeDragonCrystalTask(OseeFruitMessage.ExchangeDragonCrystalRequest request, ServerUser user) {
        // 兑换类型 0-鱼雷兑换龙晶 1-龙晶兑换鱼雷
        int exchangeType = request.getExchangeType();
        List<OseePublicData.ItemDataProto> itemsList = request.getItemsList();
        if (itemsList.isEmpty()) {
            NetManager.sendErrorMessageToClient("请选择兑换鱼雷", user);
            return;
        }

        CrystalExchangeLogEntity exchangeLogEntity = new CrystalExchangeLogEntity();
        exchangeLogEntity.setPlayerId(user.getId());
        exchangeLogEntity.setExchangeType(exchangeType);

        exchangeLogEntity.setDragonCrystalBefore(PlayerManager.getItemNum(user, ItemId.DRAGON_CRYSTAL));
        exchangeLogEntity.setBronzeTorpedoBefore(PlayerManager.getItemNum(user, ItemId.BRONZE_TORPEDO));
        exchangeLogEntity.setSilverTorpedoBefore(PlayerManager.getItemNum(user, ItemId.SILVER_TORPEDO));
        exchangeLogEntity.setGoldTorpedoBefore(PlayerManager.getItemNum(user, ItemId.GOLD_TORPEDO));

        // 龙晶和鱼雷的兑换比例  青铜，白银，黄金
        long[] CRYSTAL_TORPEDO_VALUE = {50000, 250000, 500000};
        // 兑换或消耗的龙晶数量
        long dragonCrystal = 0L;
        List<ItemData> itemDataList = new LinkedList<>();
        for (OseePublicData.ItemDataProto item : itemsList) {
            int itemId = item.getItemId();
            long itemNum = item.getItemNum();
            if (itemNum > 0) {
                if (exchangeType == 0) { // 鱼雷兑换龙晶
                    if (!PlayerManager.checkItem(user, itemId, itemNum)) {
                        NetManager.sendErrorMessageToClient("兑换失败：鱼雷不足", user);
                        return;
                    }
                    itemDataList.add(new ItemData(itemId, -itemNum));
                } else if (exchangeType == 1) { // 龙晶兑换鱼雷
                    itemDataList.add(new ItemData(itemId, itemNum));
                }
                // 鱼雷的龙晶价值
                long value = CRYSTAL_TORPEDO_VALUE[itemId - ItemId.BRONZE_TORPEDO.getId()] * itemNum;
                dragonCrystal += value;
            }
        }
        if (exchangeType == 0) { // 鱼雷兑换龙晶
            itemDataList.add(new ItemData(ItemId.DRAGON_CRYSTAL.getId(), dragonCrystal));
        } else if (exchangeType == 1) { // 龙晶兑换鱼雷
            if (!PlayerManager.checkItem(user, ItemId.DRAGON_CRYSTAL, dragonCrystal)) {
                NetManager.sendErrorMessageToClient("兑换失败：龙晶不足", user);
                return;
            }
            itemDataList.add(new ItemData(ItemId.DRAGON_CRYSTAL.getId(), -dragonCrystal));
        }
        // 记录各物品的变动数据
        for (ItemData itemData : itemDataList) {
            int itemId = itemData.getItemId();
            long count = itemData.getCount();
            if (itemId == ItemId.DRAGON_CRYSTAL.getId()) {
                exchangeLogEntity.setDragonCrystalChange(count);
            } else if (itemId == ItemId.BRONZE_TORPEDO.getId()) {
                exchangeLogEntity.setBronzeTorpedoChange(count);
            } else if (itemId == ItemId.SILVER_TORPEDO.getId()) {
                exchangeLogEntity.setSilverTorpedoChange(count);
            } else if (itemId == ItemId.GOLD_TORPEDO.getId()) {
                exchangeLogEntity.setGoldTorpedoChange(count);
            }
        }
        // 增加玩家物品
        PlayerManager.addItems(user, itemDataList, null, true);
        // 保存兑换记录
        crystalExchangeLogMapper.save(exchangeLogEntity);
        OseeFruitMessage.ExchangeDragonCrystalResponse.Builder builder = OseeFruitMessage.ExchangeDragonCrystalResponse.newBuilder();
        builder.addAllItems(itemsList);
        builder.setDragonCrystalCount(dragonCrystal);
        NetManager.sendMessage(OseeMsgCode.S_C_EXCHANGE_DRAGON_CRYSTAL_RESPONSE_VALUE, builder, user);
    }

}
