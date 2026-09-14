package jsproto.c2s.cclass.pk.pocker256;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author zhujianming
 * @date 2020-09-03 16:27
 */
public class Pocker256 {

    //扑克牌(一副) a 最小
    public static Integer[] PockerList_AFirst = {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, //方块A~K
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, //梅花A~K
            0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, //红桃A~K
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, //黑桃A~K
    };

    //扑克牌(一副) a 最大
    public static Integer[] PockerList_AEnd = {
            0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,//方块2~A
            0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E,//梅花2~A
            0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E,//红桃2~A
            0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E,//黑桃2~A
    };

    //扑克牌(一副) 2 最大
    public static Integer[] PockerList_TWOEnd = {
            0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, //方块3~2
            0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, //梅花3~2
            0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, //红桃3~2
            0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, //黑桃3~2
    };

    //扑克牌(一副)
    public static Integer[] Trump_PockerList = {
            0x41, 0x42 //大小王
    };

    //牌值掩码
    public static final int LOGIC_MASK_VALUE = 0x0F;

    //花色掩码
    public static final int LOGIC_MASK_COLOR = 0xF0;

    //牌数
    public static final int MAX_NORMAL_POCKER = 52;

    //单色牌数
    public static final int ONE_COLOR_POCKER_COUNT = 13;

    //王牌数
    public static final int MAX_TRUMP_POCKER = 2;

    //顺子最少牌数
    public static final int MIN_FLUSH_COUNT = 5;

    // 花色掩码 % 80
    public static final int LOGIC_MASK_COLOR_MOD = 256;

    //每种牌有几张，例一幅牌A有4张
    public static final int EVERY_NUM = 4;

    //获取牌值
    public static int getCardValue(int card)
    {
        int value = card & LOGIC_MASK_VALUE;
        return value;
    }

    public static int getCardValueEx(int card)
    {
        int value = card & LOGIC_MASK_VALUE;
        if (Pocker256ColorType.POCKER_COLOR_TYPE_TRUMP.equals(Pocker256ColorType.valueOf(getCardColor(card)))) {
            value += 0x10;
        }
        return value;
    }

    /**
     * 大小王为1组牌17
     * @param card
     * @return
     */
    public static int getCardValueExx(int card)
    {
        int cardValueEx = getCardValueEx(card);
        return cardValueEx == 18 || cardValueEx==17 ? 18:cardValueEx;
    }


    /**
     * 癞子19，大小王18，17
     *
     * @param card 卡
     * @return int
     */
    public static int getCardValueExxx(int card)
    {
        int value = card & LOGIC_MASK_VALUE;
        if (Pocker256ColorType.POCKER_COLOR_TYPE_TRUMP.equals(Pocker256ColorType.valueOf(getCardColor(card)))) {
            value += 0x10;
        }
        if (Pocker256ColorType.POCKER_COLOR_TYPE_LaiZi.equals(Pocker256ColorType.valueOf(getCardColor(card)))) {
            return 19;
        }
        return value;
    }

    //获取掩码
    public static int getCardColor(int card)
    {
        int color = card & LOGIC_MASK_COLOR ;
        while (color >= LOGIC_MASK_COLOR_MOD) {
            color -= LOGIC_MASK_COLOR_MOD;
        }
        return color / 0x10 ;
    }

    /**
     * 唯一 获取乱牌 normalcount 加几幅普通牌 trumpCount 带几对大小王
     */
    public static ArrayList<Integer> getOnlyRandomPockerList(int normalCount  , int trumpCount , Pocker256ListType pocketListType) {
        int total = MAX_NORMAL_POCKER * normalCount + trumpCount * MAX_TRUMP_POCKER;

        ArrayList<Integer> pocketList = new ArrayList<Integer>(MAX_NORMAL_POCKER);
        if(Pocker256ListType.POCKERLISTTYPE_AFIRST == pocketListType) {
            pocketList.addAll(Arrays.asList(PockerList_AFirst));
        }else	if(Pocker256ListType.POCKERLISTTYPE_AEND == pocketListType) {
            pocketList.addAll(Arrays.asList(PockerList_AEnd));
        } else {
            pocketList.addAll(Arrays.asList(PockerList_TWOEnd));
        }

        ArrayList<Integer> list = new ArrayList<Integer>(total);

        for (int i = 0; i < normalCount; i++) {
            for (Integer cardValue : pocketList) {
                list.add(cardValue + (Pocker256.LOGIC_MASK_COLOR_MOD)*i);
            }
        }

        for (int i = 0; i < trumpCount; i++) {
            for (Integer cardValue : Trump_PockerList) {
                list.add(cardValue + (Pocker256.LOGIC_MASK_COLOR_MOD)*i);
            }
        }

        Collections.shuffle(list);
        return list;
    }

    /**
     * 唯一 获取乱牌 normalcount 加几幅普通牌 trumpCount 带几对大小王
     */
    public static ArrayList<Integer> getOnlyRandomPockerList(int normalCount  , int trumpCount , Pocker256ListType pocketListType, List<Integer> cardValueList) {
        //
        int total = cardValueList.size()*normalCount*EVERY_NUM + trumpCount * MAX_TRUMP_POCKER;

        ArrayList<Integer> pocketList = new ArrayList<Integer>(MAX_NORMAL_POCKER);
        if(Pocker256ListType.POCKERLISTTYPE_AFIRST == pocketListType) {
            pocketList.addAll(Arrays.asList(PockerList_AFirst));
        }else	if(Pocker256ListType.POCKERLISTTYPE_AEND == pocketListType) {
            pocketList.addAll(Arrays.asList(PockerList_AEnd));
        } else {
            pocketList.addAll(Arrays.asList(PockerList_TWOEnd));
        }

        ArrayList<Integer> list = new ArrayList<Integer>(total);

        for (int i = 0; i < normalCount; i++) {
            for (Integer cardValue : pocketList) {
                if(cardValueList.contains(getCardValueEx(cardValue))){
                    list.add(cardValue + (Pocker256.LOGIC_MASK_COLOR_MOD)*i);
                }
            }
        }

        for (int i = 0; i < trumpCount; i++) {
            for (Integer cardValue : Trump_PockerList) {
                list.add(cardValue + (Pocker256.LOGIC_MASK_COLOR_MOD)*i);
            }
        }

        Collections.shuffle(list);
        return list;
    }

    /**
     * 唯一 获取乱牌 normalcount 加几幅普通牌 trumpCount 带几对大小王
     */
    public static ArrayList<Integer> getOnlyRandomPockerListByFilterList(int normalCount  , int trumpCount , Pocker256ListType pocketListType, List<Integer> filterCardList) {
        //
        int total = MAX_NORMAL_POCKER*normalCount*EVERY_NUM + trumpCount * MAX_TRUMP_POCKER - filterCardList.size();

        ArrayList<Integer> pocketList = new ArrayList<Integer>(MAX_NORMAL_POCKER);
        if(Pocker256ListType.POCKERLISTTYPE_AFIRST == pocketListType) {
            pocketList.addAll(Arrays.asList(PockerList_AFirst));
        }else	if(Pocker256ListType.POCKERLISTTYPE_AEND == pocketListType) {
            pocketList.addAll(Arrays.asList(PockerList_AEnd));
        } else {
            pocketList.addAll(Arrays.asList(PockerList_TWOEnd));
        }

        ArrayList<Integer> list = new ArrayList<Integer>(total);

        for (int i = 0; i < normalCount; i++) {
            for (Integer cardValue : pocketList) {
                int card = cardValue + (Pocker256.LOGIC_MASK_COLOR_MOD) * i;
                if(!filterCardList.contains(card)){
                    list.add(card);
                }
            }
        }

        for (int i = 0; i < trumpCount; i++) {
            for (Integer cardValue : Trump_PockerList) {
                list.add(cardValue + (Pocker256.LOGIC_MASK_COLOR_MOD)*i);
            }
        }

        Collections.shuffle(list);
        return list;
    }

//    public static void main(String args[]){
//        System.out.println(getCardSystem(575));
//    }

    public static String getCardSystem(int card){
        int rank = getCardValueEx(card);
        int color = getCardColor(card);
        StringBuilder stringBuilder = new StringBuilder();
        switch (color){
            case 0:
                stringBuilder.append("♦");
                break;
            case 1:
                stringBuilder.append("♣");
                break;
            case 2:
                stringBuilder.append("♥");
                break;
            case 3:
                stringBuilder.append("♠");
                break;
            case 4:
                stringBuilder.append("");
                break;
            default:
                stringBuilder.append("♦");
                break;
        }

        if(rank<11){
            stringBuilder.append(rank);
        }else if(rank<=15){
            switch (rank){
                case 11:
                    stringBuilder.append("J");
                    break;
                case 12:
                    stringBuilder.append("Q");
                    break;
                case 13:
                    stringBuilder.append("K");
                    break;
                case 14:
                    stringBuilder.append("A");
                    break;
                case 15:
                    stringBuilder.append("2");
                    break;
                default:
                    stringBuilder.append("");
                    break;
            }
        }else{
            switch (rank){
                case 17:
                    stringBuilder.append("小鬼");
                    break;
                case 18:
                    stringBuilder.append("大鬼");
                    break;
                case 19:
                    stringBuilder.append("精牌");
                    break;
                default:
                    stringBuilder.append("");
                    break;
            }
        }
        return stringBuilder.toString();
    }
}
