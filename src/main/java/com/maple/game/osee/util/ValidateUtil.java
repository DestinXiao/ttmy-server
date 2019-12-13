package com.maple.game.osee.util;

import java.util.regex.Pattern;

/**
 * 一些日常格式验证的工具类
 *
 * @author Junlong
 */
public class ValidateUtil {

    /**
     * 正则表达式：验证手机号
     */
    private static final String REGEX_MOBILE = "^((13|14|15|16|17|18|19)[0-9]{1}\\d{8})$";

    /**
     * 正则表达式：验证邮箱
     */
    private static final String REGEX_EMAIL = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";

    /**
     * 正则表达式：验证汉字
     */
    private static final String REGEX_CHINESE = "^[\u4e00-\u9fa5],*$";

    /**
     * 正则表达式：验证身份证
     */
    private static final String REGEX_ID_CARD = "(^\\d{18}$)|(^\\d{15}$)";

    /**
     * 正则表达式：验证URL
     */
    private static final String REGEX_URL = "http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?";

    /**
     * 正则表达式：验证IP地址
     */
    private static final String REGEX_IP_ADDR = "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)";

    /**
     * 检查手机号码是否合格 11位
     */
    public static boolean isPhoneNumber(String phoneNumber) {
        return Pattern.matches(REGEX_MOBILE, phoneNumber);
    }

    /**
     * 校验邮箱
     */
    public static boolean isEmail(String email) {
        return Pattern.matches(REGEX_EMAIL, email);
    }

    /**
     * 校验汉字
     */
    public static boolean isChinese(String chinese) {
        return Pattern.matches(REGEX_CHINESE, chinese);
    }

    /**
     * 校验身份证
     */
    public static boolean isIDCard(String idCard) {
        return Pattern.matches(REGEX_ID_CARD, idCard);
    }

    /**
     * 校验URL
     */
    public static boolean isUrl(String url) {
        return Pattern.matches(REGEX_URL, url);
    }

    /**
     * 校验IP地址
     */
    public static boolean isIPAddr(String ipAddr) {
        return Pattern.matches(REGEX_IP_ADDR, ipAddr);
    }

    /**
     * 是否为空
     */
    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

}
