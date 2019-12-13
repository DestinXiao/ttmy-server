package com.maple.game.osee.manager.lobby;

import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.entity.UserAuthenticationEntity;
import com.maple.database.data.mapper.UserAuthenticationMapper;
import com.maple.engine.data.ServerUser;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.dao.data.entity.AddressEntity;
import com.maple.game.osee.dao.data.entity.OseeLotteryShopEntity;
import com.maple.game.osee.dao.data.entity.StockEntity;
import com.maple.game.osee.dao.data.mapper.AddressMapper;
import com.maple.game.osee.dao.data.mapper.OseeLotteryShopMapper;
import com.maple.game.osee.dao.data.mapper.StockMapper;
import com.maple.game.osee.dao.log.entity.OseeRealLotteryLogEntity;
import com.maple.game.osee.dao.log.entity.OseeUnrealLotteryLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeRealLotteryLogMapper;
import com.maple.game.osee.dao.log.mapper.OseeUnrealLotteryLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.BuyShopItemResponse;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.GetLotteryShopListResponse;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.LotteryShopItemProto;
import com.maple.game.osee.util.CommonUtil;
import com.maple.network.manager.NetManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 商城管理类
 */
@Component
public class ShoppingManager {

    @Autowired
    private OseeLotteryShopMapper lotteryShopMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private UserAuthenticationMapper authenticationMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private OseeRealLotteryLogMapper realLotteryLogMapper;

    @Autowired
    private OseeUnrealLotteryLogMapper unrealLotteryLogMapper;

    /**
     * 奖券商品列表
     */
    private List<OseeLotteryShopEntity> lotteryShops = new LinkedList<>();

    /**
     * 金币价格 数量-价格
     */
    private int[][] goldPrice = {{144 * 10000, 8}, {324 * 10000, 18}, {1224 * 10000, 68}, {2128 * 10000, 128}, {4824 * 10000, 268}, {8964 * 10000, 498}};

    /**
     * 道具价格 道具数量-所需钻石数量
     * 锁定卡X100	20钻
     * 冰冻卡X100	50钻
     * 加速卡X50	50钻
     * 暴击卡X50	100钻
     * BOSS号角	    10钻
     * 分身炮X10    50钻
     */
    private final int[][] propPrice = {{100, 20}, {50, 20}, {50, 20}, {8, 20}, {1, 20}, {25, 20}};

    /**
     * VIP3-9的每日boss号角购买次数限制
     */
    private final int[] bossBugleBuyLimit = {1, 2, 4, 10, 16, 32, 64};

    /**
     * 炮台外观价格 外观体验天数-所需钻石数量
     */
    private final int[][] batteryViewPrice = {
            {3, 30}, {30, 68}, {30, 128}, {30, 518}
    };

    public ShoppingManager() {
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(this::refreshLottery, 5, TimeUnit.SECONDS);
    }

    /**
     * 刷新奖券商城数据
     */
    public void refreshLottery() {
        lotteryShops = new ArrayList<>(lotteryShopMapper.getAll());
        for (int i = 0; i < lotteryShops.size(); i++) {
            lotteryShops.get(i).getId();
            if (lotteryShops.get(i).getIndex() != i + 1) {
                lotteryShops.get(i).setIndex(i + 1);
                lotteryShopMapper.update(lotteryShops.get(i));
            }
        }
    }

    /**
     * 调换奖品位置
     */
    public boolean changeLottery(long id, int type) {
        int index = -1;
        for (int i = 0; i < lotteryShops.size(); i++) {
            if (lotteryShops.get(i).getId() == id) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return false;
        }
        if (index + type < 0 || index + type >= lotteryShops.size()) {
            return false;
        }

        lotteryShops.get(index + type).setIndex(index);
        lotteryShops.get(index).setIndex(index + type);
        lotteryShopMapper.update(lotteryShops.get(index + type));
        lotteryShopMapper.update(lotteryShops.get(index));

        refreshLottery();

        return true;
    }

    /**
     * 购买商品
     */
    public void buyShopItem(ServerUser user, long index) {
        if (index > 100000) { // 实物兑换
            for (OseeLotteryShopEntity entity : lotteryShops) {
                if (entity.getId() == index) {
                    if (!PlayerManager.checkItem(user, ItemId.LOTTERY, entity.getCost())) {
                        NetManager.sendHintMessageToClient("剩余奖券不足，无法兑换", user);
                        return;
                    }

                    long stock; // 商品库存
                    if (entity.getType() == 1 && entity.getSendType() == 3) {
                        // 获取自动发卡的实物库存内未兑换的数量
                        stock = stockMapper.getUnusedCount(entity.getId());
                    } else {
                        stock = entity.getStock();
                    }
                    if (stock <= 0) {
                        NetManager.sendHintMessageToClient("商品库存不足，无法兑换", user);
                        return;
                    }

                    List<ItemData> itemDatas = new LinkedList<>();
                    itemDatas.add(new ItemData(ItemId.LOTTERY.getId(), -entity.getCost()));
                    if (entity.getType() == 1) { // 实物
                        AddressEntity addressEntity = addressMapper.getByPlayerId(user.getId());
                        if (addressEntity == null) {
                            NetManager.sendHintMessageToClient("请设置收货地址后再兑换", user);
                            return;
                        }
                        StockEntity stockEntity = null;
                        if (entity.getSendType() == 3) { // 自动发卡就要从库存中获取
                            stockEntity = stockMapper.getUnusedOne(entity.getId());
                            if (stockEntity == null) {
                                NetManager.sendHintMessageToClient("商品库存不足，无法兑换", user);
                                return;
                            }
                            // 库存设置为有玩家兑换了
                            stockEntity.setUserId(user.getId());
                        }
                        // 保存玩家实物兑换记录
                        OseeRealLotteryLogEntity realLotteryLogEntity = new OseeRealLotteryLogEntity();
                        realLotteryLogEntity.setOrderNum("R" + System.currentTimeMillis() / 1000 + ThreadLocalRandom.current().nextInt(1000));
                        realLotteryLogEntity.setUserId(user.getId());
                        realLotteryLogEntity.setNickname(user.getNickname());
                        realLotteryLogEntity.setRewardName(entity.getName());
                        realLotteryLogEntity.setCount((int) entity.getCount());
                        realLotteryLogEntity.setCost(entity.getCost());
                        realLotteryLogEntity.setCreator(user.getNickname());
                        realLotteryLogEntity.setConsignee(addressEntity.getName()); // 收货人
                        realLotteryLogEntity.setPhoneNum(addressEntity.getPhone()); // 收货号码
                        realLotteryLogEntity.setAddress(addressEntity.getAddress()); // 收货地址
                        if (entity.getSendType() == 3) { // 自动发卡 自动置为已发货状态
                            if (stockEntity != null) {
                                realLotteryLogEntity.setOrderState(1);
                                realLotteryLogEntity.setStockId(stockEntity.getId()); // 关联的库存物品
                                stockMapper.update(stockEntity); // 更新库存物品使用
                            } else {
                                NetManager.sendHintMessageToClient("商品库存不足，无法兑换", user);
                                return;
                            }
                        }
                        realLotteryLogMapper.save(realLotteryLogEntity);
                    } else {
                        int itemId;
                        if (entity.getType() == 2) { // 钻石
                            itemId = ItemId.DIAMOND.getId();
                        } else if (entity.getType() == 3) { // 金币
                            itemId = ItemId.DIAMOND.getId();
                        } else {
                            NetManager.sendHintMessageToClient("商品类型有误", user);
                            return;
                        }
                        itemDatas.add(new ItemData(itemId, entity.getCount()));
                        // 奖券兑换虚拟物品记录
                        OseeUnrealLotteryLogEntity unrealLotteryLogEntity = new OseeUnrealLotteryLogEntity();
                        unrealLotteryLogEntity.setOrderNum("U" + System.currentTimeMillis() / 1000 + ThreadLocalRandom.current().nextInt(1000));
                        unrealLotteryLogEntity.setNickname(user.getNickname());
                        unrealLotteryLogEntity.setUserId(user.getId());
                        unrealLotteryLogEntity.setType(entity.getType());
                        unrealLotteryLogEntity.setCount((int) entity.getCount());
                        unrealLotteryLogEntity.setRewardName(entity.getName());
                        unrealLotteryLogEntity.setItemId(ItemId.LOTTERY.getId());
                        unrealLotteryLogEntity.setCost(entity.getCost());
                        unrealLotteryLogMapper.save(unrealLotteryLogEntity);
                    }
                    PlayerManager.addItems(user, itemDatas, ItemChangeReason.SHOPPING, true);

//                    entity.setUsedSize(entity.getUsedSize() + 1); // 使用数量+1
                    if (entity.getStock() > 0) {
                        entity.setStock(entity.getStock() - 1); // 库存减-1
                    }
                    lotteryShopMapper.update(entity);

                    BuyShopItemResponse.Builder builder = BuyShopItemResponse.newBuilder();
                    builder.setSuccess(true);
                    builder.setIndex(index);
                    NetManager.sendMessage(OseeMsgCode.S_C_OSEE_BUY_SHOP_ITEM_RESPONSE_VALUE, builder, user);
                    return;
                }
            }
            NetManager.sendHintMessageToClient("商品选择有误，请重新选择", user);
        } else if (index > 20000) { // 购买炮台外观 20001-骑士之誓炮台 20002-冰龙怒吼 20003-莲花童子 20004-死亡火炮
            int shopIndex = (int) (index - 20000 - 1); // 商品下标序号
            if (shopIndex >= batteryViewPrice.length) {
                NetManager.sendHintMessageToClient("商品选择有误，请重新选择", user);
                return;
            }
            int shopNum = batteryViewPrice[shopIndex][0]; // 炮台体验天数
            int price = batteryViewPrice[shopIndex][1]; // 消耗钻石数量
            if (!PlayerManager.checkItem(user, ItemId.DIAMOND, price)) {
                NetManager.sendHintMessageToClient("钻石不足，无法购买", user);
                return;
            }
            // 变动玩家物品数据
            List<ItemData> itemDatas = new LinkedList<>();
            switch (shopIndex) {
                case 0: // 购买骑士之誓
                    itemDatas.add(new ItemData(ItemId.QSZS_BATTERY_VIEW.getId(), shopNum));
                    break;
                case 1: // 购买冰龙怒吼
                    itemDatas.add(new ItemData(ItemId.BLNH_BATTERY_VIEW.getId(), shopNum));
                    break;
                case 2: // 购买莲花童子
                    itemDatas.add(new ItemData(ItemId.LHTZ_BATTERY_VIEW.getId(), shopNum));
                    break;
                case 3: // 购买死亡火炮
                    itemDatas.add(new ItemData(ItemId.SWHP_BATTERY_VIEW.getId(), shopNum));
                    break;
            }
            itemDatas.add(new ItemData(ItemId.DIAMOND.getId(), -price));
            PlayerManager.addItems(user, itemDatas, ItemChangeReason.SHOPPING, true);

            // 购买成功响应
            BuyShopItemResponse.Builder builder = BuyShopItemResponse.newBuilder();
            builder.setSuccess(true);
            builder.setIndex(index);
            NetManager.sendMessage(OseeMsgCode.S_C_OSEE_BUY_SHOP_ITEM_RESPONSE_VALUE, builder, user);
        } else if (index > 10000) { // 道具商城购买 10001-锁定,10002-冰冻,10003-急速,10004-暴击.10005-boss号角,10006-分身炮
            int shopIndex = (int) (index - 10000 - 1); // 商品下标序号
            if (shopIndex >= propPrice.length) {
                NetManager.sendHintMessageToClient("商品选择有误，请重新选择", user);
                return;
            }
            int vipLevel = PlayerManager.getPlayerVipLevel(user);
            int usedLimit = 0; // 玩家今日已使用限购次数
            if (shopIndex == 4) { // 购买的是boss号角
                if (vipLevel < 3) {
                    NetManager.sendHintMessageToClient("VIP3及以上才可以购买BOSS号角", user);
                    return;
                }
                // 当前VIP等级的每日购买数量上限
                int buyLimit = bossBugleBuyLimit[vipLevel - 3];
                String value = RedisHelper.get(String.format("Server:BossBugleBuyLimit:%d", user.getId()));
                if (!StringUtils.isEmpty(value)) {
                    String[] split = value.split(",");
                    if (split.length == 2) {
                        usedLimit = Integer.parseInt(split[0]);
                        LocalDate date = LocalDate.parse(split[1]);
                        if (!date.isEqual(LocalDate.now())) { // 非今日限制次数就要重置
                            usedLimit = 0;
                        }
                        if (usedLimit >= buyLimit) {
                            NetManager.sendHintMessageToClient("今日购买BOSS号角数量已达上限", user);
                            return;
                        }
                    }
                }
            }
            int skillNum = propPrice[shopIndex][0]; // 购买的技能数量
            int skillDiamond = propPrice[shopIndex][1]; // 需要花费的钻石数量
            if (!PlayerManager.checkItem(user, ItemId.DIAMOND, skillDiamond)) {
                NetManager.sendHintMessageToClient("钻石不足，无法购买", user);
                return;
            }

            // 变动玩家物品数据
            List<ItemData> itemDatas = new LinkedList<>();
            switch (shopIndex) {
                case 0: // 购买锁定技能
                    itemDatas.add(new ItemData(ItemId.SKILL_LOCK.getId(), skillNum));
                    break;
                case 1: // 购买冰冻技能
                    itemDatas.add(new ItemData(ItemId.SKILL_FROZEN.getId(), skillNum));
                    break;
                case 2: // 购买急速技能
                    itemDatas.add(new ItemData(ItemId.SKILL_FAST.getId(), skillNum));
                    break;
                case 3: // 购买暴击技能
                    itemDatas.add(new ItemData(ItemId.SKILL_CRIT.getId(), skillNum));
                    break;
                case 4: // 购买boss号角
                    itemDatas.add(new ItemData(ItemId.BOSS_BUGLE.getId(), skillNum));
                    break;
                case 5: // 购买分身炮
                    itemDatas.add(new ItemData(ItemId.FEN_SHEN.getId(), skillNum));
                    break;
            }
            itemDatas.add(new ItemData(ItemId.DIAMOND.getId(), -skillDiamond));
            PlayerManager.addItems(user, itemDatas, ItemChangeReason.SHOPPING, true);

            if (shopIndex == 4) { // 记录购买的boss号角限制信息
                usedLimit++; // 购买次数加一
                RedisHelper.set(String.format("Server:BossBugleBuyLimit:%d", user.getId()), usedLimit + "," + LocalDate.now().toString());
            }

            // 购买成功响应
            BuyShopItemResponse.Builder builder = BuyShopItemResponse.newBuilder();
            builder.setSuccess(true);
            builder.setIndex(index);
            NetManager.sendMessage(OseeMsgCode.S_C_OSEE_BUY_SHOP_ITEM_RESPONSE_VALUE, builder, user);
        } else { // 购买金币
            if (index < 1 || index > goldPrice.length) {
                NetManager.sendHintMessageToClient("商品选择有误，请重新选择", user);
                return;
            }

            int shopIndex = (int) (index - 1);
            if (PlayerManager.checkItem(user, ItemId.DIAMOND, goldPrice[shopIndex][1])) {
                List<ItemData> itemDatas = new LinkedList<>();
                itemDatas.add(new ItemData(ItemId.DIAMOND.getId(), -goldPrice[shopIndex][1]));
                itemDatas.add(new ItemData(ItemId.MONEY.getId(), goldPrice[shopIndex][0]));
                PlayerManager.addItems(user, itemDatas, ItemChangeReason.SHOPPING, true);

                BuyShopItemResponse.Builder builder = BuyShopItemResponse.newBuilder();
                builder.setSuccess(true);
                builder.setIndex(index);
                NetManager.sendMessage(OseeMsgCode.S_C_OSEE_BUY_SHOP_ITEM_RESPONSE_VALUE, builder, user);
            } else {
                NetManager.sendHintMessageToClient("钻石不足，无法购买", user);
            }
        }
    }

    /**
     * 发送奖券商品列表
     */
    public void sendLotteryShopListResponse(ServerUser user) {
        GetLotteryShopListResponse.Builder builder = GetLotteryShopListResponse.newBuilder();
        for (OseeLotteryShopEntity entity : lotteryShops) {
            LotteryShopItemProto.Builder itemBuilder = LotteryShopItemProto.newBuilder();
            itemBuilder.setId(entity.getId());
            itemBuilder.setImg(entity.getImg());
            itemBuilder.setLottery(entity.getCost());
            itemBuilder.setName(entity.getName());
            long stock;
            if (entity.getType() == 1 && entity.getSendType() == 3) { // 自动发卡的实物读取库存
                stock = stockMapper.getUnusedCount(entity.getId());
            } else {
                stock = entity.getStock();
            }
            itemBuilder.setRest((int) stock);
            builder.addShopItems(itemBuilder);
        }
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_GET_LOTTERY_SHOP_LIST_RESPONSE_VALUE, builder, user);
    }

    /**
     * 获取玩家今日Boss号角购买限制
     */
    public void getBossBugleBuyLimit(ServerUser user) {
        int vipLevel = PlayerManager.getPlayerVipLevel(user);
        OseeLobbyMessage.BossBugleBuyLimitResponse.Builder builder = OseeLobbyMessage.BossBugleBuyLimitResponse.newBuilder();
        if (vipLevel < 3) {
            builder.setBuyLimit(0);
            builder.setUsedLimit(0);
        } else {
            // 当前VIP等级的每日购买数量上限
            int buyLimit = bossBugleBuyLimit[vipLevel - 3];
            String value = RedisHelper.get(String.format("Server:BossBugleBuyLimit:%d", user.getId()));
            int usedLimit = 0;
            if (!StringUtils.isEmpty(value)) {
                String[] split = value.split(",");
                if (split.length == 2) {
                    usedLimit = Integer.parseInt(split[0]);
                    LocalDate date = LocalDate.parse(split[1]);
                    if (!date.isEqual(LocalDate.now())) { // 非今日限制次数就要重置
                        usedLimit = 0;
                    }
                }
            }
            builder.setBuyLimit(buyLimit);
            builder.setUsedLimit(usedLimit);
        }
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_BOSS_BUGLE_BUY_LIMIT_RESPONSE_VALUE, builder, user);
    }

    /**
     * 获取玩家的收货地址
     */
    public void getAddress(ServerUser user) {
        AddressEntity addressEntity = addressMapper.getByPlayerId(user.getId());
        OseeLobbyMessage.GetAddressResponse.Builder builder = OseeLobbyMessage.GetAddressResponse.newBuilder();
        if (addressEntity != null) {
            builder.setName(addressEntity.getName());
            builder.setPhone(addressEntity.getPhone());
            builder.setAddress(addressEntity.getAddress());
        }
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_GET_ADDRESS_RESPONSE_VALUE, builder, user);
    }

    /**
     * 设置玩家的收货地址
     */
    public void setAddress(String name, String phone, String address, ServerUser user) {
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(phone) || StringUtils.isEmpty(address)) {
            NetManager.sendErrorMessageToClient("输入请不要为空", user);
            return;
        }
        UserAuthenticationEntity authenticationEntity = authenticationMapper.getByUserId(user.getId());
        if (authenticationEntity == null) {
            NetManager.sendErrorMessageToClient("请实名认证之后再设置收货地址", user);
            return;
        }
        if (!authenticationEntity.getName().equals(name)) {
            NetManager.sendErrorMessageToClient("收货人姓名与实名认证姓名不一致", user);
            return;
        }
        String phonenum = user.getPhonenum();
        if (StringUtils.isEmpty(phonenum)) {
            NetManager.sendErrorMessageToClient("请设置账号后重试", user);
            return;
        }
        if (!phonenum.equals(phone)) {
            NetManager.sendErrorMessageToClient("收货人手机号与绑定手机号不一致", user);
            return;
        }
        boolean exist = true;
        AddressEntity addressEntity = addressMapper.getByPlayerId(user.getId());
        if (addressEntity == null) {
            exist = false;
            addressEntity = new AddressEntity();
            addressEntity.setPlayerId(user.getId());
        }
        addressEntity.setName(name);
        addressEntity.setPhone(phone);
        addressEntity.setAddress(address);
        if (exist) {
            addressMapper.update(addressEntity);
        } else {
            addressMapper.save(addressEntity);
        }
        NetManager.sendHintMessageToClient("收货地址信息设置成功", user);
    }

    /**
     * 获取玩家兑换记录
     */
    public void lotteryExchangeLog(int pageNo, int pageSize, ServerUser user) {
        OseeLobbyMessage.LotteryExchangeLogResponse.Builder builder = OseeLobbyMessage.LotteryExchangeLogResponse.newBuilder();
        StringBuilder query = new StringBuilder();
        query.append("where record.user_id = ").append(user.getId());
        int logCount = realLotteryLogMapper.getLogCount(query.toString()); // 数据总条数
        List<OseeRealLotteryLogEntity> logList = realLotteryLogMapper.getLogList(query.toString(), " limit " + (pageNo - 1) * pageSize + "," + pageSize // 数据总条数
        );
        builder.setTotalCount(logCount);
        builder.setPageNo(pageNo);
        for (OseeRealLotteryLogEntity logEntity : logList) {
            OseeLobbyMessage.LotteryExchangeLogProto.Builder proto = OseeLobbyMessage.LotteryExchangeLogProto.newBuilder();
            proto.setDate(CommonUtil.dateFormat(logEntity.getCreateTime(), "yyyy/MM/dd HH:mm:ss"));
            proto.setShopName(logEntity.getRewardName());
            proto.setState(logEntity.getOrderState());
            if (logEntity.getStockId() > 0) { // 获取商品对应的库存物品信息
                StockEntity stockEntity = stockMapper.getById(logEntity.getStockId());
                if (stockEntity != null) {
                    OseeLobbyMessage.StockInfoProto.Builder info = OseeLobbyMessage.StockInfoProto.newBuilder();
                    info.setNumber(stockEntity.getNumber());
                    info.setPassword(stockEntity.getPassword());
                    proto.setInfo(info);
                }
            }
            builder.addLog(proto);
        }
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_LOTTERY_EXCHANGE_LOG_RESPONSE_VALUE, builder, user);
    }
}
