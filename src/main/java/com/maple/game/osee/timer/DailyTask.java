package com.maple.game.osee.timer;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.entity.UserEntity;
import com.maple.database.data.mapper.UserMapper;
import com.maple.game.osee.common.RedisUtil;
import com.maple.game.osee.dao.data.entity.MessageEntity;
import com.maple.game.osee.dao.data.mapper.MessageMapper;
import com.maple.game.osee.dao.log.entity.AppGameLogEntity;
import com.maple.game.osee.dao.log.entity.AppRankLogEntity;
import com.maple.game.osee.dao.log.entity.AppRewardLogEntity;
import com.maple.game.osee.dao.log.entity.AppRewardRankEntity;
import com.maple.game.osee.dao.log.mapper.*;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.fightten.task.FightTenTask;
import com.maple.game.osee.entity.two_eight.TwoEightConfig;
import com.maple.game.osee.manager.MessageManager;
import com.maple.game.osee.manager.fightten.FightTenTaskManager;
import com.maple.game.osee.manager.fishing.FishingGrandPrixManager;
import com.maple.game.osee.manager.fishing.FishingManager;
import com.maple.game.osee.manager.fruitlaba.FruitLaBaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 每日定时任务
 *
 * @author Junlong
 */
@Component
public class DailyTask {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private FruitLaBaManager fruitLaBaManager;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private TenChallengeRankingLogMapper tenChallengeRankingLogMapper;

    @Autowired
    UserMapper userMapper;

    @Autowired
    AppRankLogMapper rankLogMapper;

    @Autowired
    AppRewardLogMapper rewardLogMapper;

    @Autowired
    AppGameLogMapper gameLogMapper;

    @Autowired
    MessageManager messageManager;

    @Autowired
    AppRewardRankMapper rewardRankMapper;
    /**
     * 每日0点执行的定时任务
     */
    @Scheduled(cron = "0 0 0 * * ? ")
    public void task() {
        // 拼十任务
        String patternKey = FightTenTaskManager.TASK_REDIS_KEY_PREFIX + "*";
        Map<String, String> patternMap = RedisHelper.getPatternMap(patternKey);
        Gson gson = new Gson();
        for (Map.Entry<?, ?> entry : patternMap.entrySet()) {
            FightTenTask tenTask = gson.fromJson((String) entry.getValue(), FightTenTask.class);
            List<FightTenTask.TaskInfo> taskInfos = tenTask.getTaskInfos();
            for (FightTenTask.TaskInfo taskInfo : taskInfos) {
                // 重置任务数据
                taskInfo.reset();
            }
            // 设置任务更新时间
            tenTask.setUpdateTime(LocalDate.now().toString());
            // 更改后的数据更新到redis
            RedisHelper.set((String) entry.getKey(), gson.toJson(tenTask));
        }
        logger.info("每日零点任务：重置了玩家的拼十任务");

        int delete = messageMapper.deleteOut7Day();
        logger.info("每日零点任务：总共删除了[{}]条玩家的过期消息/邮件", delete);

        // 拉霸重置旋转数据
        fruitLaBaManager.cleanUserRotateTask();

        // 二八杠机器人盈利金币重置
        RedisHelper.set(TwoEightConfig.RedisTwoEightDailyMoney, "0");
        logger.info("每日零点任务：重置了二八杠机器人盈利金币");

        // 捕鱼任务
        FishingManager.TORPEDO_RECORD.put("bronzeDropNumToday", 0L);
        FishingManager.TORPEDO_RECORD.put("silverDropNumToday", 0L);
        FishingManager.TORPEDO_RECORD.put("goldDropNumToday", 0L);
        FishingManager.TORPEDO_RECORD.put("bronzeUseNumToday", 0L);
        FishingManager.TORPEDO_RECORD.put("silverUseNumToday", 0L);
        FishingManager.TORPEDO_RECORD.put("goldUseNumToday", 0L);
        RedisHelper.set("Fishing:TorpedoDropNum", JSON.toJSONString(FishingManager.TORPEDO_RECORD)); // 更新redis数据
        logger.info("每日零点任务：重置了每日鱼雷掉落/使用数量");

        // Notice:需要每日0点的所有任务在这个方面里面添加就行，如果比较耗时就用新线程运行

        // 重开一个线程，清除用户每天累计中奖的金币
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> RedisHelper.removePattern(FruitLaBaManager.REDIS_PLAYER_TODAY_PRIZE_KEY + "*"));
    }


    /**
     * 每周一的0点重置玩家排行榜得分数据
     */
    @Scheduled(cron = "0 0 0 ? * MON")
    public void tenChallengeTask() {
        int count = tenChallengeRankingLogMapper.updateScoreToZero();
        logger.info("每周一零点任务：重置了{}名玩家的拼十挑战赛得分排行", count);
    }

    @Scheduled(cron = "0 30 23 ? * SUN")
    public void grandPrixWeekHandler() {
        saveWeek();

        // 每周日23：30开始清除大奖赛每周积分、排名
        RedisHelper.removePattern(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_POINT_WEEK_KEY + "*");
        RedisHelper.removePattern(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_RANK_WEEK_KEY + "*");
        logger.info("每周日23：30任务：清除大奖赛每周积分、排名");
    }

    @Scheduled(cron = "0 30 23 * * ? ")
    public void grandPrixDayHandler() {
        saveDay();

        // 每日零点开始清除大奖赛每日积分、子弹剩余数量、游戏局数
        RedisHelper.removePattern(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_POINT_DAY_KEY + "*");
        RedisHelper.removePattern(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_BULLET_KEY + "*");
        RedisHelper.removePattern(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_GAMES_KEY + "*");
        RedisHelper.removePattern(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_RANK_DAY_KEY + "*");
        logger.info("每日23：30任务：清除大奖赛每日积分、子弹剩余数量、游戏局数、每日游戏排名");
    }

    private void saveDay() {
        // 存储竞技模式游戏记录到大奖赛
        Set<String> dayRanks = RedisUtil.values(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_RANK_DAY_KEY, 0, -1);
        Iterator<String> iterator = dayRanks.iterator();
        int index = 0;

        AppGameLogEntity gameLogEntity = new AppGameLogEntity();
        AppRewardLogEntity rewardLogEntity = new AppRewardLogEntity();

        AppRewardLogEntity emptyReward = new AppRewardLogEntity();
        emptyReward.setId(0);

        while (iterator.hasNext()) {
            String next = iterator.next();
            long playerId = Long.parseLong(next);
            AppRankLogEntity entity = new AppRankLogEntity();
            entity.setPlayerId(playerId);
            UserEntity userEntity = userMapper.findById(playerId);
            entity.setNickname(userEntity.getNickname());
            entity.setScore(RedisUtil.zScore(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_RANK_DAY_KEY, next).intValue());
            entity.setRank(++index);

            AppRewardLogEntity reward = sendReward(playerId, index, 1, entity);
            if (reward == null) {
                reward = emptyReward;
            } else {
                rewardLogMapper.save(reward);
            }

            entity.setReward(reward);
            entity.setMode(1);
            entity.setType(1);

            int games = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_GAMES_KEY + playerId, 0);
            entity.setGames(games + 1);
            entity.setCost(games * 20);
            gameLogEntity.setIncome(gameLogEntity.getIncome() + games * 20);

            entity.setChange(RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_POINT_DAY_TOTAL_KEY + playerId, 0));
            entity.setReceiveTime(null);
            entity.setCreateTime(new Date());

            rankLogMapper.save(entity);

            rewardLogEntity.add(reward.getGold(),
                    reward.getDiamond(),
                    reward.getLowerBall(),
                    reward.getMiddleBall(),
                    reward.getHighBall(),
                    reward.getSkillLock(),
                    reward.getSkillFast(),
                    reward.getSkillCrit(),
                    reward.getSkillFrozen(),
                    reward.getBossBugle());

        }
        rewardLogMapper.save(rewardLogEntity);
        gameLogEntity.setReward(rewardLogEntity);   // 奖励
        gameLogEntity.setNumber(index);     // 参赛人数
        gameLogEntity.setMode(1);   // 大奖赛
        gameLogEntity.setType(1);   // 比赛类型 (日赛1，周赛2)
        gameLogEntity.setStock(RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, 0L));
        gameLogEntity.setCreateTime(new Date());
        gameLogMapper.save(gameLogEntity);
    }

    private void saveWeek() {
        // 存储竞技模式游戏记录到大奖赛
        Set<String> weekRanks = RedisUtil.values(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_RANK_WEEK_KEY, 0, -1);
        Iterator<String> iterator = weekRanks.iterator();
        int index = 0;

        AppGameLogEntity gameLogEntity = new AppGameLogEntity();
        AppRewardLogEntity rewardLogEntity = new AppRewardLogEntity();
        AppRewardLogEntity emptyReward = new AppRewardLogEntity();
        emptyReward.setId(0);
        while (iterator.hasNext()) {

            String next = iterator.next();
            long playerId = Long.parseLong(next);
            AppRankLogEntity entity = new AppRankLogEntity();
            entity.setPlayerId(playerId);
            UserEntity userEntity = userMapper.findById(playerId);
            entity.setNickname(userEntity.getNickname());
            entity.setScore(RedisUtil.zScore(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_RANK_WEEK_KEY, next).intValue());
            entity.setRank(++index);

            AppRewardLogEntity reward = sendReward(playerId, index, 2, entity);
            if (reward == null) {
                reward = emptyReward;
            } else {
                rewardLogMapper.save(reward);
            }
            entity.setReward(reward);
            entity.setMode(1);
            entity.setType(2);

            int games = RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_GAMES_KEY + playerId, 0);
            entity.setGames(games + 1);
            entity.setCost(games * 20);
            gameLogEntity.setIncome(gameLogEntity.getIncome() + games * 20);

            entity.setChange(RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_POINT_WEEK_TOTAL_KEY + playerId, 0));
            entity.setReceiveTime(null);
            entity.setCreateTime(new Date());

            rankLogMapper.save(entity);

            rewardLogEntity.add(reward.getGold(),
                    reward.getDiamond(),
                    reward.getLowerBall(),
                    reward.getMiddleBall(),
                    reward.getHighBall(),
                    reward.getSkillLock(),
                    reward.getSkillFast(),
                    reward.getSkillCrit(),
                    reward.getSkillFrozen(),
                    reward.getBossBugle());

        }
        rewardLogMapper.save(rewardLogEntity);
        gameLogEntity.setReward(rewardLogEntity);   // 奖励
        gameLogEntity.setNumber(index);     // 参赛人数
        gameLogEntity.setMode(1);   // 大奖赛
        gameLogEntity.setType(2);   // 比赛类型 (日赛1，周赛2)
        gameLogEntity.setStock(RedisUtil.val(FishingGrandPrixManager.PLAYER_GRANDPRIX_CONFIG_STOCK_KEY, 0L));
        gameLogEntity.setCreateTime(new Date());
        gameLogMapper.save(gameLogEntity);
    }

    private AppRewardLogEntity sendReward(long playerId, int rank, int type, AppRankLogEntity entity) {


        SimpleDateFormat dft = new SimpleDateFormat("YYYY/MM/dd");

        AppRewardRankEntity rewardRank = rewardRankMapper.findReward(type, rank);
        if (rewardRank == null) return null;
        AppRewardLogEntity reward = rewardRank.getReward();
        System.out.println(reward);

        MessageEntity messageEntity = new MessageEntity();
        // 系统邮件发送ID为-1
        messageEntity.setFromId(-1L);
        messageEntity.setToId(playerId);
        if(type == 1) {
            messageEntity.setTitle("《大奖赛》日排名奖励");
            messageEntity.setContent("恭喜您在" + dft.format(new Date()) + "大奖赛中夺得日排名第" + rank + "的成绩，以下是您的排名奖励");
        } else {
            Calendar beforeCalender = Calendar.getInstance();
            beforeCalender.add(Calendar.DAY_OF_MONTH, -6);
            messageEntity.setTitle("《大奖赛》周排名奖励");
            messageEntity.setContent("恭喜您在" + dft.format(beforeCalender.getTime()) + " - " + dft.format(new Date()) + "大奖赛中夺得周排名第" + rank + "的成绩，以下是您的排名奖励");
        }


        List<ItemData> itemData = new ArrayList<>();
        if (reward.getGold() != 0) itemData.add(new ItemData(1, reward.getGold()));
        if (reward.getDiamond() != 0) itemData.add(new ItemData(4, reward.getDiamond()));
        if (reward.getLowerBall() != 0) itemData.add(new ItemData(5, reward.getLowerBall()));
        if (reward.getMiddleBall() != 0) itemData.add(new ItemData(6, reward.getMiddleBall()));
        if (reward.getHighBall() != 0) itemData.add(new ItemData(7, reward.getHighBall()));
        if (reward.getSkillLock() != 0) itemData.add(new ItemData(8, reward.getSkillLock()));
        if (reward.getSkillFrozen() != 0) itemData.add(new ItemData(9, reward.getSkillFrozen()));
        if (reward.getSkillFast() != 0) itemData.add(new ItemData(10, reward.getGold()));
        if (reward.getSkillCrit() != 0) itemData.add(new ItemData(11, reward.getSkillCrit()));
        if (reward.getBossBugle() != 0) itemData.add(new ItemData(13, reward.getBossBugle()));

        messageEntity.setItems(itemData.toArray(new ItemData[0]));

        long emailId = messageManager.sendMessage(messageEntity);// 发送邮件
        entity.setEmailId(emailId);
        return reward;
    }
}
