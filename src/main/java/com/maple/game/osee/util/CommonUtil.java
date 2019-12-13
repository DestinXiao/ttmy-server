package com.maple.game.osee.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 一些通用的工具方法封装
 *
 * @author Junlong
 */
public class CommonUtil {

    /**
     * 随机轮盘算法
     *
     * @param probs 轮盘各部分的概率大小数组，概率总和必须为100
     * @return 随机的轮盘数组选中的对应序号
     */
    public static int getWheelRandom(int[] probs) {
        if (probs == null || probs.length < 1) {
            return 0;
        }
        int sum = 0;
        for (int prob : probs) {
            sum += prob;
        }
        int random = ThreadLocalRandom.current().nextInt(sum);
        sum = 0;
        for (int i = 0; i < probs.length; i++) {
            sum += probs[i];
            if (random < sum) {
                return i;
            }
        }
        return ThreadLocalRandom.current().nextInt(probs.length);
    }

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat();

    /**
     * 格式化日期
     */
    public static String dateFormat(Date date, String pattern) {
        simpleDateFormat.applyPattern(pattern);
        return simpleDateFormat.format(date);
    }
}
