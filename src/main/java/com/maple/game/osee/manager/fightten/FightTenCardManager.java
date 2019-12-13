package com.maple.game.osee.manager.fightten;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼十牌型管理类
 */
public class FightTenCardManager {

    /**
     * 同花顺：5张数值相连且花色相同的牌，牌型倍数10
     **/
    public static final int CARD_TYPE_THS = 15;

    /**
     * 炸弹：4张相同点数的牌和1张牌，牌型倍数9
     **/
    public static final int CARD_TYPE_ZD = 14;

    /**
     * 葫芦：3张相同牌和2张相同的牌，牌型倍数8
     **/
    public static final int CARD_TYPE_HL = 13;

    /**
     * 同花：5张相同花色的牌组成，牌型倍数7
     **/
    public static final int CARD_TYPE_TH = 12;

    /**
     * 顺子：由5张数字连续的牌组成，牌型倍数6
     **/
    public static final int CARD_TYPE_SZ = 11;

    /**
     * 双十：有十的情况下，剩余两张牌相加之和为10的倍数。牌型倍数5
     **/
    public static final int CARD_TYPE_SS = 10;

    /**
     * 十带九：有十的情况下，剩余两张牌相加之和的个位数为9。牌型倍数4。
     **/
    public static final int CARD_TYPE_ST9 = 9;

    /**
     * 十带八：有十的情况下，剩余两张牌相加之和的个位数为8。牌型倍数3
     **/
    public static final int CARD_TYPE_ST8 = 8;

    /**
     * 十带七：有十的情况下，剩余两张牌相加之和的个位数为7。牌型倍数2
     **/
    public static final int CARD_TYPE_ST7 = 7;

    /**
     * 十带一到十带六：有十的情况下，剩余两张牌相加之和的个位数为1-6。牌型倍数1
     **/
    public static final int CARD_TYPE_ST6 = 6;
    public static final int CARD_TYPE_ST5 = 5;
    public static final int CARD_TYPE_ST4 = 4;
    public static final int CARD_TYPE_ST3 = 3;
    public static final int CARD_TYPE_ST2 = 2;
    public static final int CARD_TYPE_ST1 = 1;

    /**
     * 散牌：任意三张牌相加都不是十的倍数，称为散牌，牌型倍数为0
     **/
    public static final int CARD_TYPE_NONE = 0;

    /**
     * 花色掩码
     **/
    private static final int MASK_COLOR = 0xF0; // 花色掩码

    /**
     * 数值掩码
     **/
    private static final int MASK_VALUE = 0x0F; // 数值掩码

    private static int tCardPoint = 0;

    /**
     * 两副牌的牌型比较
     *
     * @return 1-前者大于后者 0-相等 -1-前者小于后者
     */
    public static int compare(List<Integer> cards1, List<Integer> cards2) {
        List<Integer> c1 = new ArrayList<>(cards1);
        List<Integer> c2 = new ArrayList<>(cards2);
        int c1CardType = getCardType(c1);
        int c2CardType = getCardType(c2);
        if (c1CardType > c2CardType) {
            return 1;
        } else if (c1CardType < c2CardType) {
            return -1;
        } else { // 牌型相等就比较最大牌
            int c1MaxCard = 0, c1MaxCardColor = -1;
            for (Integer card : c1) {
                int cardColor = getCardColor(card);
                int cardValue = getCardValue(card);
                if (c1MaxCard < cardValue) {
                    c1MaxCard = cardValue;
                    c1MaxCardColor = cardColor;
                } else if (c1MaxCard == cardValue) {
                    // 相等就要花色大的那张
                    if (cardColor < c1MaxCardColor) {
                        c1MaxCardColor = cardColor;
                    }
                }
            }
            int c2MaxCard = 0, c2MaxCardColor = -1;
            for (Integer card : c2) {
                int cardValue = getCardValue(card);
                int cardColor = getCardColor(card);
                if (c2MaxCard < cardValue) {
                    c2MaxCard = cardValue;
                    c2MaxCardColor = cardColor;
                } else if (c2MaxCard == cardValue) {
                    // 相等就要花色大的那张
                    if (cardColor < c2MaxCardColor) {
                        c2MaxCardColor = cardColor;
                    }
                }
            }
            if (c1MaxCard > c2MaxCard) { // 最大牌值比较
                return 1;
            } else if (c1MaxCard == c2MaxCard) { // 最大牌的牌值一样就比较花色大小：黑>红>梅>方，但在程序中定义的值是从小到大
                if (c1MaxCardColor < c2MaxCardColor) {
                    return 1;
                } else if (c1MaxCardColor > c2MaxCardColor) {
                    return -1;
                }
                return 0;
            } else {
                return -1;
            }
        }
    }

    /**
     * 获取牌组的类型
     *
     * @param cards 牌组数据
     * @return 牌组类型
     */
    public static int getCardType(List<Integer> cards) {
        if (cards == null || cards.size() != 5) {
            return -1;
        }
        List<Integer> tempList = new ArrayList<>(cards);
        // 排序
        tempList.sort((card1, card2) -> {
            if ((card1 & 0x0F) < (card2 & 0x0F)) {
                return -1;
            } else if ((card1 & 0x0F) > (card2 & 0x0F)) {
                return 1;
            } else if ((card1 & 0xF0) < (card2 & 0xF0)) {
                return -1;
            }
            return 1;
        });
        int cardType = 0;
        if (isThs(tempList)) {
            // 1) 同花顺：5张数值相连且花色相同的牌，牌型倍数10。
            cardType = CARD_TYPE_THS;
        } else if (isBomb(tempList)) {
            // 2) 炸弹：4张相同点数的牌和1张牌，牌型倍数9。
            cardType = CARD_TYPE_ZD;
        } else if (isCucurbit(tempList)) {
            // 3) 葫芦：3张相同牌和2张相同的牌，牌型倍数8。
            cardType = CARD_TYPE_HL;
        } else if (isTh(tempList)) {
            // 4) 同花：5张相同花色的牌组成，牌型倍数7。
            cardType = CARD_TYPE_TH;
        } else if (isSz(tempList)) {
            // 5) 顺子：由5张数字连续的牌组成，牌型倍数6
            cardType = CARD_TYPE_SZ;
        } else if (isSs(tempList)) {
            // 6) 双十：有十的情况下，剩余两张牌相加之和为10的倍数。牌型倍数5。
            // 7) 十带九：有十的情况下，剩余两张牌相加之和的个位数为9。牌型倍数4。
            // 8) 十带八：有十的情况下，剩余两张牌相加之和的个位数为8。牌型倍数3。
            // 9) 十带七：有十的情况下，剩余两张牌相加之和的个位数为7。牌型倍数2。
            // 10) 十带一到十带六：有十的情况下，剩余两张牌相加之和的个位数为1-6。牌型倍数1。
            // 11) 散牌：任意三张牌相加都不是十的倍数，称为散牌，牌型倍数为0。
            if (tCardPoint % 10 == 0) {
                cardType = CARD_TYPE_SS;
            } else if (tCardPoint % 10 == 9) {
                cardType = CARD_TYPE_ST9;
            } else if (tCardPoint % 10 == 8) {
                cardType = CARD_TYPE_ST8;
            } else if (tCardPoint % 10 == 7) {
                cardType = CARD_TYPE_ST7;
            } else if (tCardPoint % 10 == 6) {
                cardType = CARD_TYPE_ST6;
            } else if (tCardPoint % 10 == 5) {
                cardType = CARD_TYPE_ST5;
            } else if (tCardPoint % 10 == 4) {
                cardType = CARD_TYPE_ST4;
            } else if (tCardPoint % 10 == 3) {
                cardType = CARD_TYPE_ST3;
            } else if (tCardPoint % 10 == 2) {
                cardType = CARD_TYPE_ST2;
            } else if (tCardPoint % 10 == 1) {
                cardType = CARD_TYPE_ST1;
            } else {
                cardType = CARD_TYPE_NONE;
            }
        }
        return cardType;
    }

    /**
     * 是否是同花顺 同花顺：5张数值相连且花色相同的牌，牌型倍数10
     */
    private static boolean isThs(List<Integer> tempCard) {
        int lastVal = 0;
        boolean ths = true;
        tCardPoint = 0;
        for (Integer card : tempCard) {
            if (lastVal == 0) {
                lastVal = card;
            } else if (lastVal + 1 == card) {
                lastVal = card;
                tCardPoint = card;
            } else {
                ths = false;
                break;
            }
        }
        return ths;
    }

    /**
     * 炸弹：4张相同点数的牌和1张牌，牌型倍数9。
     */
    private static boolean isBomb(List<Integer> tempCard) {
        int lastVal = 0;
        int count = 0;
        tCardPoint = 0;
        for (Integer card : tempCard) {
            if (lastVal == 0) {
                lastVal = card;
                count = 1;
            } else if (getCardValue(lastVal) == getCardValue(card)) {
                lastVal = card;
                tCardPoint = card;
                count++;
                if(count==4)
                    return true;
            } else {
                lastVal = card;
                count=1;
            }
        }
        return false;
    }

    /**
     * 葫芦：3张相同牌和2张相同的牌，牌型倍数8。
     */
    private static boolean isCucurbit(List<Integer> tempCard) {
        int lastVal = 0;
        int count = 0;
        tCardPoint = 0;

        List<Integer> list = new ArrayList<>(tempCard);

        for (Integer card : list) {
            if (lastVal == 0) {
                lastVal = card;
                count = 1;
            } else if (getCardValue(lastVal) == getCardValue(card)) { // 三个的
                lastVal = card;
                if (++count == 3) {
                    break;
                }
            } else {
                lastVal = card;
                count = 1;
            }
        }

        if (count == 3) {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (getCardValue(lastVal) == getCardValue(list.get(i))) {// 三个的
                    tCardPoint = list.get(i);
                    list.remove(i);
                }
            }

            if (list.size() == 2 && getCardValue(list.get(0)) == getCardValue(list.get(1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 同花：5张相同花色的牌组成，牌型倍数7。
     */
    private static boolean isTh(List<Integer> tempCard) {
        int lastVal = 0;
        boolean ths = true;
        tCardPoint = 0;
        for (Integer card : tempCard) {
            if (lastVal == 0) {
                lastVal = card;
            } else if (getCardColor(lastVal) == getCardColor(card)) {
                lastVal = card;
                tCardPoint = card;
            } else {
                ths = false;
                break;
            }
        }
        return ths;
    }

    /**
     * 顺子：由5张数字连续的牌组成，牌型倍数6
     */
    private static boolean isSz(List<Integer> tempCard) {
        int lastVal = 0;
        boolean ths = true;
        tCardPoint = 0;
        for (Integer card : tempCard) {
            if (lastVal == 0) {
                lastVal = card;
            } else if (getCardValue(lastVal) + 1 == getCardValue(card)) {
                lastVal = card;
                tCardPoint = card;
            } else {
                ths = false;
                break;
            }
        }
        return ths;
    }

    /**
     * 双十：有十的情况下，剩余两张牌相加之和为10的倍数。牌型倍数5。
     */
    private static boolean isSs(List<Integer> tempCard) {
        boolean ths = false;
        tCardPoint = 0;
        List<Integer> list = new ArrayList<>();
        List<Integer> tTempList = new ArrayList<>(tempCard);
        List<int[]> tList = new ArrayList<>();

        for (Integer tTard : tTempList) {// 第一个值
            list.clear();
            list.addAll(tTempList);
            list.remove(tTard);
            int fv = getCardValue(tTard);
            if (fv == 11 || fv == 12 || fv == 13) {
                fv = 10; // 点数10点
            }

            for (Integer card : list) {// 第二个值
                int v = getCardValue(card);
                if (v == 11 || v == 12 || v == 13) {
                    v = 10;// 点数10点
                }
                List<Integer> slist = new ArrayList<>(list);
                slist.remove(card);
                for (Integer dcard : slist) {// 第三个值
                    int dv = getCardValue(dcard);
                    if (dv == 11 || dv == 12 || dv == 13) {
                        dv = 10;// 点数10点
                    }
                    // 10的倍数
                    if ((fv + v + dv) % 10 == 0) {
                        tList.add(new int[]{tTard, card, dcard});
                    }
                }
            }
        }

        if (tList.size() > 0) {// 有十
            ths = true;
            // 判断哪一个值最大
            int maxVal = 0;
            for (int i = 0; i < tList.size(); i++) {
                list = new ArrayList<>(tempCard);
                int[] tm = tList.get(i);
                for (Integer wj : tm) {
                    list.remove(wj);
                }
                int val = 0;
                for (Integer wj : list) {
                    int v = getCardValue(wj);
                    if (v == 11 || v == 12 || v == 13) {
                        v = 10;// 点数10点
                    }
                    val += v;
                }
                if (maxVal < val) {
                    maxVal = val;
                }
            }

            tCardPoint = maxVal;
        } else {// 散牌
            tCardPoint = tempCard.get(tempCard.size() - 1);
        }

        return ths;
    }

    /**
     * 获取牌值
     */
    public static int getCardValue(int cardData) {
        return cardData & MASK_VALUE;
    }

    /**
     * 获取牌花色
     */
    public static int getCardColor(int cardData) {
        return (cardData & MASK_COLOR) >> 4;
    }

    /**
     * 获取对应牌型的倍数
     *
     * @param cardType 牌型
     * @return 牌型对应的倍数
     */
    public static int getCardTypeMultiple(int cardType) {
        if (cardType > CARD_TYPE_SS) { // 双十以后的特殊牌型倍数最高设定为5倍
            return 5;
        }
        int multiple = cardType - 5;
        return multiple <= 0 ? 1 : multiple;
    }
}
