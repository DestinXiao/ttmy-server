package com.maple.game.osee.timer;

import com.maple.common.lobby.proto.LobbyMessage;
import com.maple.engine.container.DataContainer;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.csv.file.FishConfig;
import com.maple.game.osee.manager.BaseRobotManager;
import com.maple.game.osee.manager.PlayerManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 系统主动推送游走字幕
 *
 * @author Junlong
 */
@Component
public class AutoWanderSubtitle {

    /**
     * 游走字幕模板
     */
    public static final String[] TEMPLATES = new String[]{
            // 捕鱼模板
            "玩家<color=#66FF66>%s</color>可真是吉星高照，只听<color=yellow>捕鱼场</color>内一声巨响，" +
                    "竟捕获了<color=red>%d倍</color>的<color=red>%s</color>，" +
                    "最终获得了<color=red>%d万</color>金币",
            "玩家<color=#66FF66>%s</color>欧气爆棚，竟在<color=yellow>捕鱼场</color>中" +
                    "捕获了<color=red>%d倍</color>的<color=red>%s</color>，" +
                    "最终获得了<color=red>%d万</color>金币，周围玩家纷纷表示想吸取欧气",
            "玩家<color=#66FF66>%s</color>可真是吉星高照，只听<color=yellow>捕鱼场</color>内一声巨响，" +
                    "竟捕获了<color=red>%d倍</color>的<color=red>%s</color>，" +
                    "最终获得了<color=red>%s</color>",
            "玩家<color=#66FF66>%s</color>欧气爆棚，竟在<color=yellow>捕鱼场</color>中" +
                    "捕获了<color=red>%d倍</color>的<color=red>%s</color>，" +
                    "最终获得了<color=red>%s</color>，周围玩家纷纷表示想吸取欧气",
            // 拼十模板
            "玩家<color=#66FF66>%s</color>今天是踩了狗屎嘛，在<color=yellow>拼十场</color>中竟" +
                    "使用牌型<color=red>%s</color>取得了胜利，" +
                    "最终获得了<color=red>%d万</color>金币，真是羡煞旁人啊",
            "玩家<color=#66FF66>%s</color>时来运转，在<color=yellow>拼十场</color>中" +
                    "使用牌型<color=red>%s</color>取得了胜利，" +
                    "最终获得了<color=red>%d万</color>金币，这下可真是发大财啦",
            // 拉霸模板
//            "玩家<color=#66FF66>%s</color>人品大爆发竟在<color=yellow>经典拉霸</color>上" +
//                    "抽中了<color=red>%d</color>倍大奖，" +
//                    "获得了<color=red>%d万</color>金币，真是羡煞旁人啊",
//            "玩家<color=#66FF66>%s</color>在<color=yellow>经典拉霸</color>游戏中天降横财，" +
//                    "本次命中<color=red>%d</color>倍大奖，" +
//                    "最终获得了<color=red>%d万</color>金币"
            "玩家<color=#66FF66>%s</color>人品大爆发竟在<color=yellow>经典拉霸</color>上" +
                    "抽中了<color=red>%d</color>倍大奖，" +
                    "获得了<color=red>%d万</color>龙晶，真是羡煞旁人啊",
            "玩家<color=#66FF66>%s</color>在<color=yellow>经典拉霸</color>游戏中天降横财，" +
                    "本次命中<color=red>%d</color>倍大奖，" +
                    "最终获得了<color=red>%d万</color>龙晶",
            // 龙晶战场
            "玩家<color=#66FF66>%s</color>可真是吉星高照，只听<color=yellow>龙晶战场</color>内一声巨响，" +
                    "竟捕获了<color=red>%d倍</color>的<color=red>%s</color>，" +
                    "最终获得了<color=red>%d万</color>龙晶",
            "玩家<color=#66FF66>%s</color>欧气爆棚，竟在<color=yellow>龙晶战场</color>中" +
                    "捕获了<color=red>%d倍</color>的<color=red>%s</color>，" +
                    "最终获得了<color=red>%d万</color>龙晶，周围玩家纷纷表示想吸取欧气",
            "玩家<color=#66FF66>%s</color>欧气爆棚，竟在<color=yellow>龙晶战场</color>中" +
                    "捕获了<color=red>%d倍</color>的<color=red>%s</color>，" +
                    "最终获得了<color=red>%d</color>龙珠，周围玩家纷纷表示想吸取欧气"
    };

    /**
     * 拉霸所有大于等于70的倍数
     */
    private static final Integer[] LABA_MULTIPLE_OVER_70 = new Integer[]{70, 75, 80, 85, 100, 175, 200, 250, 400, 550, 650, 800, 1250, 1750};

    /**
     * 拉霸所有单线底注
     */
    private static final Long[] LABA_SINGLE_BASE_BET = new Long[]{500L, 2500L, 5000L, 25000L, 50000L, 250000L, 500000L};

    private long lastSendTime = 0;

    /**
     * 最佳发送时间 10-25s之间
     */
    @Scheduled(fixedRate = 1000)
    public void task() {
        long now = System.currentTimeMillis();
        int spaceTime = ThreadLocalRandom.current().nextInt(15, 25 + 1);
        int diffTime = (int) ((now - lastSendTime) / 1000);
        // 随机游走字幕发送间隔时间
        if (diffTime < spaceTime) {
            return;
        }
        lastSendTime = now;
        String robotName = BaseRobotManager.getRobotName();
        // 随机一个游走字幕模板
        int index = ThreadLocalRandom.current().nextInt(TEMPLATES.length);
        String template = TEMPLATES[index];
        String text = "";
        // 随机的赢取的金币
        long winMoney;
        if (index <= 3) { // 捕鱼模板
            List<FishConfig> fishConfigs = DataContainer.getDatas(FishConfig.class);
            Collections.shuffle(fishConfigs); // 打乱数据

            int bossRandom = ThreadLocalRandom.current().nextInt(0, 100);
            for (FishConfig fishConfig : fishConfigs) {
                if (bossRandom < 30) { // 刷boss
                    if (fishConfig.getFishType() != 10) {
                        continue;
                    }
                } else { // 不刷boss
                    if (fishConfig.getFishType() == 10) {
                        continue;
                    }
                }

                long randomMoney = fishConfig.getMaxMoney() > fishConfig.getMoney()
                        ? ThreadLocalRandom.current().nextLong(fishConfig.getMoney(), fishConfig.getMaxMoney() + 1)
                        : fishConfig.getMoney();
                if ((fishConfig.getFishType() == 100 || fishConfig.getFishType() == 10) && index >= 2) { // 可以掉落鱼雷的鱼
                    // 掉落的鱼雷名称信息
                    StringBuilder torpedoInfo = new StringBuilder();
                    // 随机掉落的鱼雷个数
                    int tNum = ThreadLocalRandom.current().nextInt(1, 3);
                    for (int i = 0; i < tNum; i++) {
                        ItemId item = ItemId.getItemIdById(ThreadLocalRandom.current().nextInt(ItemId.BRONZE_TORPEDO.getId(), ItemId.GOLD_TORPEDO.getId() + 1));
                        if (item == null) {
                            continue;
                        }
                        torpedoInfo.append(item.getInfo()).append(",");
                    }
                    text = String.format(template,
                            robotName,
                            randomMoney, fishConfig.getName(),
                            torpedoInfo.toString().substring(0, torpedoInfo.length() - 1)
                    );
                    break;
                }
                if (randomMoney >= 60 && randomMoney <= 80) { // 播报字幕条件
                    template = TEMPLATES[index >= 2 ? index - 2 : index];
                    winMoney = ThreadLocalRandom.current().nextLong(1000000, 4000000 + 1);
                    text = String.format(template,
                            robotName,
                            randomMoney, fishConfig.getName(),
                            winMoney / 10000
                    );
                    break;
                }
            }
        } else if (index <= 5) { // 拼十模板
//            winMoney = ThreadLocalRandom.current().nextLong(1000000, 5000000 + 1);
//            text = String.format(template,
//                    robotName,
//                    FightTenManager.check(ThreadLocalRandom.current().nextInt(5, 16)),
//                    winMoney / 10000
//            );
            return;
        } else if (index <= 7) { // 拉霸模板
            // 拉霸赢取的钱是 单线底注*线的条数(1-9)*倍数 ThreadLocalRandom.current().nextInt(1, 4) *
            Integer multiple = LABA_MULTIPLE_OVER_70[ThreadLocalRandom.current().nextInt(LABA_MULTIPLE_OVER_70.length - 3)];
            // 虚拟的赢钱数
            winMoney = LABA_SINGLE_BASE_BET[ThreadLocalRandom.current().nextInt(LABA_SINGLE_BASE_BET.length - 2)] * multiple;
            if (winMoney > 1000000) {
                text = String.format(template,
                        robotName,
                        multiple,
                        winMoney / 10000
                );
            }
        } else { // 龙晶战场不需要自动播报
            return;
        }
        if (!StringUtils.isEmpty(text)) {
            // 发送给所有的在线玩家
            PlayerManager.sendMessageToOnline(
                    LobbyMessage.LobbyMsgCode.S_C_WANDER_SUBTITLE_RESPONSE_VALUE,
                    LobbyMessage.WanderSubtitleResponse.newBuilder().setLevel(1).setContent(text).build()
            );
        }
    }
}
