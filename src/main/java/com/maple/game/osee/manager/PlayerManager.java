package com.maple.game.osee.manager;

import com.google.protobuf.Message;
import com.maple.engine.container.DataContainer;
import com.maple.engine.container.UserContainer;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.data.entity.OseePlayerEntity;
import com.maple.game.osee.dao.data.mapper.OseePlayerMapper;
import com.maple.game.osee.dao.log.entity.OseePlayerTenureLogEntity;
import com.maple.game.osee.dao.log.mapper.OseePlayerTenureLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.csv.file.BatteryLevelConfig;
import com.maple.game.osee.entity.fishing.csv.file.PlayerLevelConfig;
import com.maple.game.osee.manager.lobby.CommonLobbyManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.PlayerMoneyResponse;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.VipLevelResponse;
import com.maple.network.manager.NetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 1688玩家管理类
 */
@Component
public class PlayerManager implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(PlayerManager.class);

    /**
     * 最大金币数量
     */
    public static final long MAX_MONEY = 10000000000L;

    /**
     * 最大道具数量
     */
    public static final long MAX_PROP = 99999L;

    /**
     * vip等级充值阈值
     */
    public static final long[] VIP_MONEY = {10, 100, 600, 2000, 6000, 18000, 60000, 180000, 600000};

    /**
     * 实体更新器
     */
    public static final List<IEntityUpdater> entityUpdaters = new LinkedList<>();

    /**
     * 需要更新的实体
     */
    public static Set<OseePlayerEntity> updateEntities = new HashSet<>();

    private static OseePlayerMapper playerMapper;

    private static OseePlayerTenureLogMapper tenureLogMapper;

    @Autowired
    public PlayerManager(OseePlayerMapper playerMapper, OseePlayerTenureLogMapper tenureLogMapper) {
        PlayerManager.playerMapper = playerMapper;
        PlayerManager.tenureLogMapper = tenureLogMapper;
    }

    /**
     * 添加多个物品并指定变动原因
     */
    public static void addItems(ServerUser user, List<ItemData> itemDatas, ItemChangeReason reason, boolean save) {
        OseePlayerEntity playerEntity = getPlayerEntity(user);

        OseePlayerTenureLogEntity log = new OseePlayerTenureLogEntity();
        log.setPreDiamond(playerEntity.getDiamond());
        log.setPreMoney(playerEntity.getMoney());
        log.setPreLottery(playerEntity.getLottery());
        log.setPreBankMoney(playerEntity.getBankMoney());
        log.setPreBronzeTorpedo(playerEntity.getBronzeTorpedo());
        log.setPreSilverTorpedo(playerEntity.getSilverTorpedo());
        log.setPreGoldTorpedo(playerEntity.getGoldTorpedo());
        log.setPreSkillLock(playerEntity.getSkillLock());
        log.setPreSkillFast(playerEntity.getSkillFast());
        log.setPreSkillFrozen(playerEntity.getSkillFrozen());
        log.setPreSkillCrit(playerEntity.getSkillCrit());
        log.setPreBossBugle(playerEntity.getBossBugle());

        for (ItemData itemData : itemDatas) {
            synchronized (playerEntity) {
                switch (ItemId.getItemIdById(itemData.getItemId())) {
                    case MONEY:
                        playerEntity.setMoney(getMoneyResult(playerEntity.getMoney(), itemData.getCount()));
                        log.setChangeMoney(log.getChangeMoney() + itemData.getCount());
                        break;
                    case BANK_MONEY:
                        playerEntity.setBankMoney(getMoneyResult(playerEntity.getBankMoney(), itemData.getCount()));
                        log.setChangeBankMoney(log.getChangeBankMoney() + itemData.getCount());
                        break;
                    case LOTTERY:
                        playerEntity.setLottery(getMoneyResult(playerEntity.getLottery(), itemData.getCount()));
                        log.setChangeLottery(log.getChangeLottery() + itemData.getCount());
                        break;
                    case DIAMOND:
                        playerEntity.setDiamond(getMoneyResult(playerEntity.getDiamond(), itemData.getCount()));
                        log.setChangeDiamond(log.getChangeDiamond() + itemData.getCount());
                        break;
                    // 鱼雷道具
                    case BRONZE_TORPEDO:
                        playerEntity.setBronzeTorpedo(getPropResult(playerEntity.getBronzeTorpedo(), itemData.getCount()));
                        log.setChangeBronzeTorpedo(log.getChangeBronzeTorpedo() + itemData.getCount());
                        break;
                    case SILVER_TORPEDO:
                        playerEntity.setSilverTorpedo(getPropResult(playerEntity.getSilverTorpedo(), itemData.getCount()));
                        log.setChangeSilverTorpedo(log.getChangeSilverTorpedo() + itemData.getCount());
                        break;
                    case GOLD_TORPEDO:
                        playerEntity.setGoldTorpedo(getPropResult(playerEntity.getGoldTorpedo(), itemData.getCount()));
                        log.setChangeGoldTorpedo(log.getChangeGoldTorpedo() + itemData.getCount());
                        break;
                    // 技能
                    case SKILL_LOCK:
                        playerEntity.setSkillLock(getPropResult(playerEntity.getSkillLock(), itemData.getCount()));
                        log.setChangeSkillLock(log.getChangeSkillLock() + itemData.getCount());
                        break;
                    case SKILL_FROZEN:
                        playerEntity.setSkillFrozen(getPropResult(playerEntity.getSkillFrozen(), itemData.getCount()));
                        log.setChangeSkillFrozen(log.getChangeSkillFrozen() + itemData.getCount());
                        break;
                    case SKILL_FAST:
                        playerEntity.setSkillFast(getPropResult(playerEntity.getSkillFast(), itemData.getCount()));
                        log.setChangeSkillFast(log.getChangeSkillFast() + itemData.getCount());
                        break;
                    case SKILL_CRIT:
                        playerEntity.setSkillCrit(getPropResult(playerEntity.getSkillCrit(), itemData.getCount()));
                        log.setChangeSkillCrit(log.getChangeSkillCrit() + itemData.getCount());
                        break;
                    // 月卡
                    case MONTH_CARD:
                        playerEntity.setMonthCardExpireDate(getDateResult(playerEntity.getMonthCardExpireDate(), itemData.getCount()));
                        break;
                    // BOSS号角
                    case BOSS_BUGLE:
                        playerEntity.setBossBugle(getPropResult(playerEntity.getBossBugle(), itemData.getCount()));
                        log.setChangeBossBugle(log.getChangeBossBugle() + itemData.getCount());
                        break;
                    case QSZS_BATTERY_VIEW: // 骑士之誓炮台外观
                        // 增加天数
                        playerEntity.setQszsBatteryExpireDate(getDateResult(playerEntity.getQszsBatteryExpireDate(), itemData.getCount()));
                        break;
                    case BLNH_BATTERY_VIEW: // 冰龙怒吼炮台外观
                        // 增加天数
                        playerEntity.setBlnhBatteryExpireDate(getDateResult(playerEntity.getBlnhBatteryExpireDate(), itemData.getCount()));
                        break;
                    case LHTZ_BATTERY_VIEW: // 莲花童子炮台外观
                        // 增加天数
                        playerEntity.setLhtzBatteryExpireDate(getDateResult(playerEntity.getLhtzBatteryExpireDate(), itemData.getCount()));
                        break;
                    case SWHP_BATTERY_VIEW: // 死亡火炮炮台外观
                        // 增加天数
                        playerEntity.setSwhpBatteryExpireDate(getDateResult(playerEntity.getSwhpBatteryExpireDate(), itemData.getCount()));
                        break;
                    case DRAGON_CRYSTAL: // 龙晶
                        playerEntity.setDragonCrystal(getMoneyResult(playerEntity.getDragonCrystal(), itemData.getCount()));
                        break;
                    case FEN_SHEN: // 分身炮道具
                        playerEntity.setFenShen(getPropResult(playerEntity.getFenShen(), itemData.getCount()));
                        break;
                    default:
                        return;
                }
            }
        }

        if (save) {
            if (reason != null) {
                log.setUserId(user.getId());
                log.setNickname(user.getNickname());
                log.setReason(reason.getId());
                tenureLogMapper.save(log);
            }
            playerMapper.update(playerEntity);
        } else {
            updateEntities.add(playerEntity);
        }
        sendPlayerMoneyResponse(user);
        sendPlayerPropResponse(user);
    }

    /**
     * 添加物品并指定变动原因
     */
    public static void addItem(ServerUser user, int itemId, long count, ItemChangeReason reason, boolean save) {
        addItem(user, ItemId.getItemIdById(itemId), count, reason, save);
    }

    /**
     * 添加物品并指定变动原因
     */
    public static void addItem(ServerUser user, ItemId itemId, long count, ItemChangeReason reason, boolean save) {
        OseePlayerEntity playerEntity = getPlayerEntity(user);

        OseePlayerTenureLogEntity log = new OseePlayerTenureLogEntity();
        log.setPreDiamond(playerEntity.getDiamond());
        log.setPreMoney(playerEntity.getMoney());
        log.setPreLottery(playerEntity.getLottery());
        log.setPreBankMoney(playerEntity.getBankMoney());
        log.setPreBronzeTorpedo(playerEntity.getBronzeTorpedo());
        log.setPreSilverTorpedo(playerEntity.getSilverTorpedo());
        log.setPreGoldTorpedo(playerEntity.getGoldTorpedo());
        log.setPreSkillLock(playerEntity.getSkillLock());
        log.setPreSkillFast(playerEntity.getSkillFast());
        log.setPreSkillFrozen(playerEntity.getSkillFrozen());
        log.setPreSkillCrit(playerEntity.getSkillCrit());
        log.setPreBossBugle(playerEntity.getBossBugle());

        synchronized (playerEntity) {
            switch (itemId) {
                case MONEY:
                    playerEntity.setMoney(getMoneyResult(playerEntity.getMoney(), count));
                    log.setChangeMoney(count);
                    break;
                case BANK_MONEY:
                    playerEntity.setBankMoney(getMoneyResult(playerEntity.getBankMoney(), count));
                    log.setChangeBankMoney(count);
                    break;
                case LOTTERY:
                    playerEntity.setLottery(getMoneyResult(playerEntity.getLottery(), count));
                    log.setChangeLottery(count);
                    break;
                case DIAMOND:
                    playerEntity.setDiamond(getMoneyResult(playerEntity.getDiamond(), count));
                    log.setChangeDiamond(count);
                    break;
                // 鱼雷道具
                case BRONZE_TORPEDO:
                    playerEntity.setBronzeTorpedo(getPropResult(playerEntity.getBronzeTorpedo(), count));
                    log.setChangeBronzeTorpedo(count);
                    break;
                case SILVER_TORPEDO:
                    playerEntity.setSilverTorpedo(getPropResult(playerEntity.getSilverTorpedo(), count));
                    log.setChangeSilverTorpedo(count);
                    break;
                case GOLD_TORPEDO:
                    playerEntity.setGoldTorpedo(getPropResult(playerEntity.getGoldTorpedo(), count));
                    log.setChangeGoldTorpedo(count);
                    break;
                // 技能
                case SKILL_LOCK:
                    playerEntity.setSkillLock(getPropResult(playerEntity.getSkillLock(), count));
                    log.setChangeSkillLock(count);
                    break;
                case SKILL_FROZEN:
                    playerEntity.setSkillFrozen(getPropResult(playerEntity.getSkillFrozen(), count));
                    log.setChangeSkillFrozen(count);
                    break;
                case SKILL_FAST:
                    playerEntity.setSkillFast(getPropResult(playerEntity.getSkillFast(), count));
                    log.setChangeSkillFast(count);
                    break;
                case SKILL_CRIT:
                    playerEntity.setSkillCrit(getPropResult(playerEntity.getSkillCrit(), count));
                    log.setChangeSkillCrit(count);
                    break;
                // 月卡
                case MONTH_CARD:
                    playerEntity.setMonthCardExpireDate(getDateResult(playerEntity.getMonthCardExpireDate(), count));
                    break;
                // BOSS号角
                case BOSS_BUGLE:
                    playerEntity.setBossBugle(getPropResult(playerEntity.getBossBugle(), count));
                    log.setChangeBossBugle(count);
                    break;
                case QSZS_BATTERY_VIEW: // 骑士之誓炮台外观
                    // 增加天数
                    playerEntity.setQszsBatteryExpireDate(getDateResult(playerEntity.getQszsBatteryExpireDate(), count));
                    break;
                case BLNH_BATTERY_VIEW: // 冰龙怒吼炮台外观
                    // 增加天数
                    playerEntity.setBlnhBatteryExpireDate(getDateResult(playerEntity.getBlnhBatteryExpireDate(), count));
                    break;
                case LHTZ_BATTERY_VIEW: // 莲花童子炮台外观
                    // 增加天数
                    playerEntity.setLhtzBatteryExpireDate(getDateResult(playerEntity.getLhtzBatteryExpireDate(), count));
                    break;
                case SWHP_BATTERY_VIEW: // 死亡火炮炮台外观
                    // 增加天数
                    playerEntity.setSwhpBatteryExpireDate(getDateResult(playerEntity.getSwhpBatteryExpireDate(), count));
                    break;
                case DRAGON_CRYSTAL: // 龙晶
                    playerEntity.setDragonCrystal(getMoneyResult(playerEntity.getDragonCrystal(), count));
                    break;
                case FEN_SHEN: // 分身炮道具
                    playerEntity.setFenShen(getPropResult(playerEntity.getFenShen(), count));
                    break;
                default:
                    return;
            }
        }

        if (save) {
            if (reason != null) {
                log.setUserId(user.getId());
                log.setNickname(user.getNickname());
                log.setReason(reason.getId());
                tenureLogMapper.save(log);
            }
            playerMapper.update(playerEntity);
        } else {
            updateEntities.add(playerEntity);
        }
        sendPlayerMoneyResponse(user);
        sendPlayerPropResponse(user);
    }

    /**
     * 检查物品数量
     */
    public static boolean checkItem(ServerUser user, int itemId, long count) {
        return checkItem(user, ItemId.getItemIdById(itemId), count);
    }

    /**
     * 检查物品数量
     */
    public static boolean checkItem(ServerUser user, ItemId itemId, long count) {
        OseePlayerEntity playerEntity = getPlayerEntity(user);
        if (playerEntity == null) {
            return false;
        }
        synchronized (playerEntity) {
            switch (itemId) {
                case MONEY:
                    return playerEntity.getMoney() >= count;
                case BANK_MONEY:
                    return playerEntity.getBankMoney() >= count;
                case LOTTERY:
                    return playerEntity.getLottery() >= count;
                case DIAMOND:
                    return playerEntity.getDiamond() >= count;
                // 鱼雷道具
                case BRONZE_TORPEDO:
                    return playerEntity.getBronzeTorpedo() >= count;
                case SILVER_TORPEDO:
                    return playerEntity.getSilverTorpedo() >= count;
                case GOLD_TORPEDO:
                    return playerEntity.getGoldTorpedo() >= count;
                // 技能
                case SKILL_LOCK:
                    return playerEntity.getSkillLock() >= count;
                case SKILL_FROZEN:
                    return playerEntity.getSkillFrozen() >= count;
                case SKILL_FAST:
                    return playerEntity.getSkillFast() >= count;
                case SKILL_CRIT:
                    return playerEntity.getSkillCrit() >= count;
                case MONTH_CARD: // 月卡
                    long days = playerEntity.getMonthCardExpireDate().toEpochDay() - LocalDate.now().toEpochDay();
                    if(count < 0)
                        return true;
                    else
                        return days >= count;
                // BOSS号角
                case BOSS_BUGLE:
                    return playerEntity.getBossBugle() >= count;
                case DRAGON_CRYSTAL: // 龙晶
                    return playerEntity.getDragonCrystal() >= count;
                case FEN_SHEN: // 分身炮道具
                    return playerEntity.getFenShen() >= count;
                default:
                    return false;
            }
        }
    }

    /**
     * 获取物品数量
     */
    public static long getItemNum(ServerUser user, ItemId itemId) {
        OseePlayerEntity playerEntity = getPlayerEntity(user);
        if (playerEntity == null) {
            return 0;
        }
        synchronized (playerEntity) {
            switch (itemId) {
                case MONEY:
                    return playerEntity.getMoney();
                case BANK_MONEY:
                    return playerEntity.getBankMoney();
                case LOTTERY:
                    return playerEntity.getLottery();
                case DIAMOND:
                    return playerEntity.getDiamond();
                // 鱼雷道具
                case BRONZE_TORPEDO:
                    return playerEntity.getBronzeTorpedo();
                case SILVER_TORPEDO:
                    return playerEntity.getSilverTorpedo();
                case GOLD_TORPEDO:
                    return playerEntity.getGoldTorpedo();
                // 技能
                case SKILL_LOCK:
                    return playerEntity.getSkillLock();
                case SKILL_FROZEN:
                    return playerEntity.getSkillFrozen();
                case SKILL_FAST:
                    return playerEntity.getSkillFast();
                case SKILL_CRIT:
                    return playerEntity.getSkillCrit();
                // 月卡
                case MONTH_CARD: {
                    long days = playerEntity.getMonthCardExpireDate().toEpochDay() - LocalDate.now().toEpochDay();
                    return days < 0 ? 0 : days;
                }
                // BOSS号角
                case BOSS_BUGLE:
                    return playerEntity.getBossBugle();
                case QSZS_BATTERY_VIEW: { // 骑士之誓炮台外观
                    long days = playerEntity.getQszsBatteryExpireDate().toEpochDay() - LocalDate.now().toEpochDay();
                    return days < 0 ? 0 : days;
                }
                case BLNH_BATTERY_VIEW: { // 冰龙怒吼炮台外观
                    long days = playerEntity.getBlnhBatteryExpireDate().toEpochDay() - LocalDate.now().toEpochDay();
                    return days < 0 ? 0 : days;
                }
                case LHTZ_BATTERY_VIEW: { // 莲花童子炮台外观
                    long days = playerEntity.getLhtzBatteryExpireDate().toEpochDay() - LocalDate.now().toEpochDay();
                    return days < 0 ? 0 : days;
                }
                case SWHP_BATTERY_VIEW: { // 死亡火炮炮台外观
                    long days = playerEntity.getSwhpBatteryExpireDate().toEpochDay() - LocalDate.now().toEpochDay();
                    return days < 0 ? 0 : days;
                }
                case DRAGON_CRYSTAL: // 龙晶
                    return playerEntity.getDragonCrystal();
                case FEN_SHEN: // 分身炮道具
                    return playerEntity.getFenShen();
                default:
                    return 0;
            }
        }
    }

    /**
     * 获取货币计算结果
     */
    public static long getMoneyResult(long money, long addMoney) {
        long result = money + addMoney;
        return result > 0 ? result < MAX_MONEY ? result : MAX_MONEY - 1 : 0;
    }

    /**
     * 获取道具计算结果
     */
    public static long getPropResult(long propNum, long addProp) {
        long result = propNum + addProp;
        return result > 0 ? (result <= MAX_PROP ? result : MAX_PROP) : 0;
    }

    /**
     * 获取日期计算结果
     */
    public static LocalDate getDateResult(LocalDate date, long days) {
        LocalDate now = LocalDate.now();
        if (date == null || date.isBefore(now)) { // 如果已经过期了就从今天开始计算
            return now.plusDays(days);
        }
        // 未过期就继续叠加时间
        return date.plusDays(days);
    }

    /**
     * 获取玩家数据
     */
    public static OseePlayerEntity getPlayerEntity(ServerUser user) {
        return getPlayerEntity(user, true);
    }

    /**
     * 获取玩家数据
     */
    public static OseePlayerEntity getPlayerEntity(ServerUser user, Boolean init) {
        OseePlayerEntity playerEntity = user.getExpertData(OseePlayerEntity.EntityId);
        if (playerEntity == null && init) { // 如果玩家信息为空就立即初始化一下玩家游戏信息
            if (user.getEntity() != null) {
                playerEntity = playerMapper.findByUserId(user.getId());
                if (playerEntity == null) {
                    playerEntity = new OseePlayerEntity();
                    playerEntity.setUserId(user.getId());
                    playerEntity.setMoney(1000);
                    // 初始炮台等级为最低等级
                    playerEntity.setBatteryLevel(DataContainer.getData(1, BatteryLevelConfig.class).getBatteryLevel());
                    playerMapper.save(playerEntity);
                }

                user.putExpertData(OseePlayerEntity.EntityId, playerEntity);
            }
        }
        return user.getExpertData(OseePlayerEntity.EntityId);
    }

    /**
     * 获取玩家金币数量
     */
    public static long getPlayerMoney(ServerUser user) {
        return getPlayerEntity(user).getMoney();
    }

    /**
     * 获取玩家真实金币数量
     */
    public static long getRealPlayerMoney(ServerUser user) {
        return getItemNum(user, ItemId.MONEY) + getItemNum(user, ItemId.BANK_MONEY);
    }

    /**
     * 获取玩家vip等级
     */
    public static int getPlayerVipLevel(ServerUser user) {
        return getPlayerVipLevel(getPlayerEntity(user));
    }

    /**
     * 获取玩家vip等级
     */
    public static int getPlayerVipLevel(OseePlayerEntity playerEntity) {
        for (int i = 0; i < VIP_MONEY.length; i++) {
            if (playerEntity.getRechargeMoney() < VIP_MONEY[i]) {
                return i;
            }
        }
        return VIP_MONEY.length;
    }

    /**
     * 获取玩家当前等级
     */
    public static int getPlayerLevel(ServerUser user) {
        return getPlayerEntity(user).getLevel();
    }

    /**
     * 获取玩家炮台等级
     */
    public static int getPlayerBatteryLevel(ServerUser user) {
        return getPlayerEntity(user).getBatteryLevel();
    }

    /**
     * 发送玩家物品数据
     */
    public static void sendPlayerMoneyResponse(ServerUser user) {
        CommonLobbyManager.checkReliefMoney(user);
        PlayerMoneyResponse.Builder builder = PlayerMoneyResponse.newBuilder();
        OseePlayerEntity entity = getPlayerEntity(user);
        builder.setMoney(entity.getMoney());
        builder.setLottery(entity.getLottery());
        builder.setDiamond(entity.getDiamond());
        builder.setBankMoney(entity.getBankMoney());
        builder.setDragonCrystal(entity.getDragonCrystal());
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_PLAYER_MONEY_RESPONSE_VALUE, builder.build(), user);
    }

    /**
     * 发送玩家道具信息响应
     */
    public static void sendPlayerPropResponse(ServerUser user) {
        OseeLobbyMessage.PlayerPropResponse.Builder builder = OseeLobbyMessage.PlayerPropResponse.newBuilder();
        OseePlayerEntity playerEntity = getPlayerEntity(user);
        builder.setBronzeTorpedo(playerEntity.getBronzeTorpedo());
        builder.setSilverTorpedo(playerEntity.getSilverTorpedo());
        builder.setGoldTorpedo(playerEntity.getGoldTorpedo());
        builder.setSkillLock(playerEntity.getSkillLock());
        builder.setSkillFrozen(playerEntity.getSkillFrozen());
        builder.setSkillFast(playerEntity.getSkillFast());
        builder.setSkillCrit(playerEntity.getSkillCrit());
        // 月卡结束日期的时间戳
        long epochMilli = playerEntity.getMonthCardExpireDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        builder.setMonthCardOverDate(epochMilli);
        // BOSS号角
        builder.setBossBugle(playerEntity.getBossBugle());
        builder.setQszs(getItemNum(user, ItemId.QSZS_BATTERY_VIEW));
        builder.setBlnh(getItemNum(user, ItemId.BLNH_BATTERY_VIEW));
        builder.setLhtz(getItemNum(user, ItemId.LHTZ_BATTERY_VIEW));
        builder.setSwhp(getItemNum(user, ItemId.SWHP_BATTERY_VIEW));
        // 分身炮道具
        builder.setFenShen(getItemNum(user, ItemId.FEN_SHEN));
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_PLAYER_PROP_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送玩家vip等级数据
     */
    public static void sendVipLevelResponse(ServerUser user) {
        VipLevelResponse.Builder builder = VipLevelResponse.newBuilder();
        builder.setVipLevel(getPlayerVipLevel(user));
        OseePlayerEntity entity = getPlayerEntity(user);
        builder.setTotalMoney(entity.getRechargeMoney());
        if (builder.getVipLevel() < VIP_MONEY.length) {
            builder.setNextLevel(VIP_MONEY[builder.getVipLevel()] - entity.getRechargeMoney());
        }
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_VIP_LEVEL_RESPONSE_VALUE, builder.build(), user);
    }

    /**
     * 发送玩家当前拥有的最高炮台等级响应
     */
    public static void sendPlayerBatteryLevelResponse(ServerUser user) {
        OseeLobbyMessage.PlayerBatteryLevelResponse.Builder builder = OseeLobbyMessage.PlayerBatteryLevelResponse.newBuilder();
        builder.setLevel(getPlayerBatteryLevel(user));
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_PLAYER_BATTERY_LEVEL_RESPONSE_VALUE, builder, user);
    }

    /**
     * 向所有已登录在线客户端发送消息
     */
    public static void sendMessageToOnline(int msgCode, Message msg) {
        List<ServerUser> serverUsers = UserContainer.getActiveServerUsers();
        for (ServerUser user : serverUsers) {
            if (user.isOnline() && user.getId() > 0) {
                NetManager.sendMessage(msgCode, msg, user);
            }
        }
    }

    /**
     * 发送玩家等级响应
     */
    public static void sendPlayerLevelResponse(ServerUser user) {
        OseePlayerEntity entity = PlayerManager.getPlayerEntity(user);
        OseeLobbyMessage.PlayerLevelResponse.Builder builder = OseeLobbyMessage.PlayerLevelResponse.newBuilder();
        builder.setLevel(entity.getLevel());
        PlayerLevelConfig levelConfig = DataContainer.getData(entity.getLevel(), PlayerLevelConfig.class);
        if (levelConfig != null) {
            builder.setNextExperience(levelConfig.getExp());
            builder.setNowExperience(entity.getExperience());
        }
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_PLAYER_LEVEL_RESPONSE_VALUE, builder, user);
    }

    /**
     * 更新服务器所有玩家信息到数据库
     */
    @Scheduled(fixedRate = 300000L)
    private void updatePlayers() {
        Set<OseePlayerEntity> updateEntities = PlayerManager.updateEntities;
        PlayerManager.updateEntities = new HashSet<>();

        for (OseePlayerEntity entity : updateEntities) {
            try {
                playerMapper.update(entity);
                for (IEntityUpdater updater : entityUpdaters) {
                    updater.entityUpdate(entity);
                }
            } catch (Exception e) {
                logger.error("更新玩家[{}]数据出错[{}]", entity.getId(), e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 重写类销毁时调用的方法
     */
    @Override
    public void destroy() throws Exception {
        logger.info("服务器关闭，保存用户数据");
        updatePlayers();
    }

    /**
     * 获取玩家拼十挑战剩余次数
     */
    public static long getPlayerTenChallengeTimes(ServerUser user) {
        OseePlayerEntity playerEntity = getPlayerEntity(user);
        return playerEntity.getTenChallengeTimes();
    }

    /**
     * 增加玩家拼十挑战次数
     */
    public static boolean addPlayerTenChallengeTimes(ServerUser user, long times) {
        OseePlayerEntity playerEntity = getPlayerEntity(user);
        synchronized (playerEntity) {
            long tenChallengeTimes = playerEntity.getTenChallengeTimes();
            tenChallengeTimes += times;
            if (tenChallengeTimes < 0) {
                return false;
            }
            // 更新挑战次数
            playerEntity.setTenChallengeTimes(tenChallengeTimes);
            playerMapper.update(playerEntity);
            return true;
        }
    }
}
