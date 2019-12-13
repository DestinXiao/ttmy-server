package com.maple.game.osee.manager.pig;

import com.maple.database.config.redis.RedisHelper;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.OseePublicData;
import com.maple.game.osee.proto.goldenpig.TtmyGoldenPig;
import com.maple.network.manager.NetManager;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 砸金猪管理类
 */
@Component
public class GoldenPigManager {

    private static final String REDIS_KEY_FREE_TIMES = "GoldenPig:FreeTimes:%d";

    private static final String REDIS_KEY_HIT_LIMIT = "GoldenPig:DailyHitLimit:%d";

    /**
     * 每日使用鱼雷砸的次数限制 vip3-9
     */
    private static final Integer[] VIP_HIT_LIMIT = {1, 2, 4, 8, 16, 32, 64};

    /**
     * 获取玩家今日砸金猪的免费次数
     */
    private int getFreeTimes(ServerUser user) {
        int times = 1;
        String value = RedisHelper.get(String.format(REDIS_KEY_FREE_TIMES, user.getId()));
        if (!StringUtils.isEmpty(value)) {
            if (LocalDate.parse(value).isEqual(LocalDate.now())) { // 如果是当天说明次数已用完
                times = 0;
            }
        }
        return times;
    }

    /**
     * 获取玩家今日砸金猪剩余限制次数
     *
     * @return [剩余，总共]
     */
    private int[] getHitLimit(ServerUser user) {
        int vipLevel = PlayerManager.getPlayerVipLevel(user);
        int[] limit = {0, 0};
        String key = String.format(REDIS_KEY_HIT_LIMIT, user.getId());
        String value = RedisHelper.get(key);
        if (!StringUtils.isEmpty(value) && vipLevel > 3) {
            String[] strings = value.split(",");
            if (strings.length == 3) {
                Integer vipLimit = VIP_HIT_LIMIT[vipLevel - 3];
                int restLimit = Integer.parseInt(strings[0]);
                int totalLimit = Integer.parseInt(strings[1]);
                LocalDate date = LocalDate.parse(strings[2]);
                if (!date.isEqual(LocalDate.now())) { // 不是当天就重置次数
                    restLimit = vipLimit;
                    totalLimit = vipLimit;
                    RedisHelper.set(key, String.format("%d,%d,%s", restLimit, totalLimit, LocalDate.now().toString()));
                }
                if (totalLimit != vipLimit) { // 限制次数变了说明VIP等级也变了
                    totalLimit = vipLimit;
                    if (restLimit > totalLimit) {
                        restLimit = totalLimit;
                    }
                    RedisHelper.set(key, String.format("%d,%d,%s", restLimit, totalLimit, LocalDate.now().toString()));
                }
                limit[0] = restLimit;
                limit[1] = totalLimit;
            }
        } else {
            if (vipLevel < 3) {
                return limit;
            }
            Integer vipLimit = VIP_HIT_LIMIT[vipLevel - 3];
            int restLimit = vipLimit;
            int totalLimit = vipLimit;
            limit[0] = restLimit;
            limit[1] = totalLimit;
            RedisHelper.set(key, String.format("%d,%d,%s", restLimit, totalLimit, LocalDate.now().toString()));
        }
        return limit;
    }

    // **********************************************

    /**
     * 砸金猪
     */
    public void pigBreak(ServerUser user, int index) {
        int[] hitLimit = null;
        if (index > 0) { // 检查可砸次数
            hitLimit = getHitLimit(user);
            if (hitLimit[0] <= 0) {
                NetManager.sendErrorMessageToClient("今日可砸次数已用完", user);
                return;
            }
        }
        List<ItemData> addItems = new LinkedList<>();
        // 掉落出来的物品
        List<OseePublicData.ItemDataProto> breakItems = new LinkedList<>();
        switch (index) {
            case 0: { // 免费 仅能获得技能卡道具2种，单种技能数量1-2随机。
                if (getFreeTimes(user) <= 0) {
                    NetManager.sendErrorMessageToClient("今日免费次数已用完", user);
                    return;
                }
                // 标识今日免费锤子已使用
                RedisHelper.set(String.format(REDIS_KEY_FREE_TIMES, user.getId()), LocalDate.now().toString());
                Set<Integer> skillItems = new HashSet<>();
                // 随机2种道具
                while (skillItems.size() != 2) {
                    skillItems.add(ThreadLocalRandom.current().nextInt(ItemId.SKILL_LOCK.getId(), ItemId.SKILL_CRIT.getId() + 1));
                }
                // 随机掉落技能个数
                skillItems.forEach(skillId -> addItems.add(new ItemData(skillId, ThreadLocalRandom.current().nextLong(1, 2 + 1))));
                addItems.forEach(itemData ->
                        breakItems.add(OseePublicData.ItemDataProto.newBuilder().
                                setItemId(itemData.getItemId())
                                .setItemNum(itemData.getCount())
                                .build()));
                break;
            }
            case 1: { // 木棰 必定掉落：10张奖券 技能卡随机掉落1-2种，单种技能数量1-3随机。
                if (!PlayerManager.checkItem(user, ItemId.BRONZE_TORPEDO, 1)) {
                    NetManager.sendErrorMessageToClient("青铜鱼雷不足", user);
                    return;
                }
                // 1000张奖券
                addItems.add(new ItemData(ItemId.LOTTERY.getId(), 1000));
                int skillNum = ThreadLocalRandom.current().nextInt(1, 2 + 1);
                Set<Integer> skillItems = new HashSet<>();
                // 随机2种道具
                while (skillItems.size() != skillNum) {
                    skillItems.add(ThreadLocalRandom.current().nextInt(ItemId.SKILL_LOCK.getId(), ItemId.SKILL_CRIT.getId() + 1));
                }
                // 随机掉落技能个数
                skillItems.forEach(skillId -> addItems.add(new ItemData(skillId, ThreadLocalRandom.current().nextLong(1, 3 + 1))));
                addItems.forEach(itemData ->
                        breakItems.add(OseePublicData.ItemDataProto.newBuilder().
                                setItemId(itemData.getItemId())
                                .setItemNum(itemData.getCount())
                                .build()));
                addItems.add(new ItemData(ItemId.BRONZE_TORPEDO.getId(), -1));
                break;
            }
            case 2: { // 铁锤 必定掉落：50张奖券 技能卡随机掉落1-3种，单种技能数量1-3随机。
                if (!PlayerManager.checkItem(user, ItemId.SILVER_TORPEDO, 1)) {
                    NetManager.sendErrorMessageToClient("白银鱼雷不足", user);
                    return;
                }
                // 5000张奖券
                addItems.add(new ItemData(ItemId.LOTTERY.getId(), 5000));
                int skillNum = ThreadLocalRandom.current().nextInt(1, 3 + 1);
                Set<Integer> skillItems = new HashSet<>();
                // 随机2种道具
                while (skillItems.size() != skillNum) {
                    skillItems.add(ThreadLocalRandom.current().nextInt(ItemId.SKILL_LOCK.getId(), ItemId.SKILL_CRIT.getId() + 1));
                }
                // 随机掉落技能个数
                skillItems.forEach(skillId -> addItems.add(new ItemData(skillId, ThreadLocalRandom.current().nextLong(1, 4 + 1))));
                addItems.forEach(itemData ->
                        breakItems.add(OseePublicData.ItemDataProto.newBuilder().
                                setItemId(itemData.getItemId())
                                .setItemNum(itemData.getCount())
                                .build()));
                addItems.add(new ItemData(ItemId.SILVER_TORPEDO.getId(), -1));
                break;
            }
            case 3: { // 金锤 必定掉落：100张奖券 技能卡随机掉落1-4种，单种技能数量1-5随机。
                if (!PlayerManager.checkItem(user, ItemId.GOLD_TORPEDO, 1)) {
                    NetManager.sendErrorMessageToClient("黄金鱼雷不足", user);
                    return;
                }
                // 10000张奖券
                addItems.add(new ItemData(ItemId.LOTTERY.getId(), 10000));
                int skillNum = ThreadLocalRandom.current().nextInt(1, 4 + 1);
                Set<Integer> skillItems = new HashSet<>();
                // 随机2种道具
                while (skillItems.size() != skillNum) {
                    skillItems.add(ThreadLocalRandom.current().nextInt(ItemId.SKILL_LOCK.getId(), ItemId.SKILL_CRIT.getId() + 1));
                }
                // 随机掉落技能个数
                skillItems.forEach(skillId -> addItems.add(new ItemData(skillId, ThreadLocalRandom.current().nextLong(1, 5 + 1))));
                addItems.forEach(itemData ->
                        breakItems.add(OseePublicData.ItemDataProto.newBuilder().
                                setItemId(itemData.getItemId())
                                .setItemNum(itemData.getCount())
                                .build()));
                addItems.add(new ItemData(ItemId.GOLD_TORPEDO.getId(), -1));
                break;
            }
            default: {
                NetManager.sendErrorMessageToClient("锤子序号选择错误", user);
                return;
            }
        }
        if (hitLimit != null) {
            // 今日可砸减一
            RedisHelper.set(String.format(REDIS_KEY_HIT_LIMIT, user.getId()),
                    String.format("%d,%d,%s", hitLimit[0] - 1, hitLimit[1], LocalDate.now().toString()));
        }
        // 玩家物品变更
        PlayerManager.addItems(user, addItems, null, true);
        breakItems.sort(Comparator.comparingInt(OseePublicData.ItemDataProto::getItemId));
        TtmyGoldenPig.GoldenPigBreakResponse.Builder builder = TtmyGoldenPig.GoldenPigBreakResponse.newBuilder();
        builder.addAllItems(breakItems);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_GOLDEN_PIG_BREAK_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送玩家今日砸金猪的免费次数
     */
    public void sendFreeTimesResponse(ServerUser user) {
        TtmyGoldenPig.GoldenPigFreeTimesResponse.Builder builder = TtmyGoldenPig.GoldenPigFreeTimesResponse.newBuilder();
        builder.setTimes(getFreeTimes(user));
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_GOLDEN_PIG_FREE_TIMES_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送玩家今日砸金猪剩余限制次数
     */
    public void sendHitLimitResponse(ServerUser user) {
        int[] hitLimit = getHitLimit(user);
        TtmyGoldenPig.GoldenPigHitLimitResponse.Builder builder = TtmyGoldenPig.GoldenPigHitLimitResponse.newBuilder();
        builder.setRestLimit(hitLimit[0]);
        builder.setTotalLimit(hitLimit[1]);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_GOLDEN_PIG_HIT_LIMIT_RESPONSE_VALUE, builder, user);
    }
}
