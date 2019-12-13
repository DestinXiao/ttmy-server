package com.maple.game.osee.entity.two_eight;

public class TwoEightConfig {
    /**
     * 给机器人发大牌的概率
     */
    public static double getLargeCardProbably =70;

    /**
     * 未达成盈利目标机器庄家得到最大，第二大，第三大，第四大牌的概率
     */
    public static double toWinFirstCardProbably=30;
    public static double toWinSecondCardProbably=40;
    public static double toWinThirdCardProbably=20;
    public static double toWinLastCardProbably=10;

    /**
     * 已达成盈利目标机器庄家得到最大，第二大，第三大，第四大牌的概率
     */
    public static double toLoseFirstCardProbably=10;
    public static double toLoseSecondCardProbably=10;
    public static double toLoseThirdCardProbably=40;
    public static double toLoseLastCardProbably=40;

    /**
     * 机器人需要盈利金额
     */
    public static long robotMinMoney = 0;

    /**
     * 庄家抽水比例
     */
    public static double cutMoneyPre =0.05;

    public static final String RedisTwoEightRobotWinKey="Osee:TwoEight:Robot:WinMoney";
    public static final String RedisTwoEightRobotHistoryWinKey ="Osee:TwoEight:Robot:AllMoney";


    //二八杠机器人今日盈利金额
    public static final String RedisTwoEightDailyMoney = "Osee:TwoEight:Daily:Money";


}
