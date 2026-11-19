package business.global.mj.util;

import business.global.mj.MJCardInit;
import com.ddm.server.common.utils.CommString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 258将
 *
 * @author Administrator
 */
public class Hu258JiangKeUtil {
    public static final List<Integer> tfeType = new ArrayList<>();

    static {
        tfeType.add(2);
        tfeType.add(5);
        tfeType.add(8);
    }

    private volatile static Hu258JiangKeUtil singleton;

    private Hu258JiangKeUtil() {
    }

    public static Hu258JiangKeUtil getInstance() {
        if (singleton == null) {
            synchronized (Hu258JiangKeUtil.class) {
                if (singleton == null) {
                    singleton = new Hu258JiangKeUtil();
                }
            }
        }
        return singleton;
    }

    /**
     * 胡
     *
     * @return
     */
    public boolean checkHu(MJCardInit mCardInit) {
        List<Integer> allCards = mCardInit.getAllCardInts();
        Collections.sort(allCards, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                // 从小到大
                return o1 - o2;
            }
        });
        return check3nP2(allCards, mCardInit.sizeJin());
    }

    /**
     * 胡
     *
     * @param allCardInts
     * @param Jins
     * @param totalJin
     * @param cardType
     * @return
     */
    public boolean checkHu(List<Integer> allCards, int totalJin) {
        Collections.sort(allCards, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                // 从小到大
                return o1 - o2;
            }
        });
        return check3nP2(allCards, totalJin);
    }

    public boolean check3nP2(List<Integer> allCards, int totalJin) {

        if (allCards.size() <= 0 && totalJin > 0) {
            return true;
        }

        List<String> resultList = new ArrayList<>();
        List<Integer> typeList = new ArrayList<>(); // 计算牌的品类
        int lastType = -1;
        for (int type : allCards) {
            if (type != lastType) {
                typeList.add(type);
                lastType = type;
            }
        }
        int duiType = 0;
        // 遍历每种牌
        for (Integer type : typeList) {
            if (type >= 40) {
                continue;
            }
            duiType = type % 10;
            if (!tfeType.contains(duiType)) {
                continue;
            }
            List<Integer> dui = new ArrayList<>();
            List<Integer> leftCards = new ArrayList<>(allCards);
            leftCards.remove(type);
            dui.add(type);
            int leftJin = totalJin;
            if (leftCards.contains(type)) {
                leftCards.remove(type);
                dui.add(type);
            } else {
                if (leftJin == 0) {
                    continue;
                }
                leftJin -= 1;
                dui.add(0);
            }
            List<List<Integer>> matched = new ArrayList<>();
            matchThird(resultList, 0, dui, matched, leftJin, leftCards);
        }
        if (resultList.size() > 0) {
            return true;
        }
        // 检查是否胡
        return this.isHuResult(resultList, allCards, totalJin);
    }

    /**
     * 检查是否胡
     *
     * @param resultList 结果列表
     * @param allCards   所有牌值
     * @param totalJin   金数量
     * @return
     */
    private boolean isHuResult(List<String> resultList, List<Integer> allCards, int totalJin) {
        int leftJin = totalJin;
        if (totalJin >= 2) {
            leftJin -= 2;
        }
        List<Integer> dui = new ArrayList<>();
        dui.add(0);
        dui.add(0);
        List<Integer> leftCards = new ArrayList<>(allCards);
        List<List<Integer>> matched = new ArrayList<>();
        matchThird(resultList, 0, dui, matched, leftJin, leftCards);
        return resultList.size() > 0;
    }


    public void matchThird(List<String> resultList, int tabCnt,
                           List<Integer> dui, List<List<Integer>> matched, int leftJin,
                           List<Integer> leftCards) {
        if (leftCards.size() == 0 && leftJin % 3 == 0) {
            String end = CommString.join(",", dui);
            for (List<Integer> match : matched) {
                end += "  :  " + CommString.join(",", match);
            }
            for (int i = 1; i <= leftJin / 3; i++) {
                end += "  :  " + "0,0,0";
            }
            if (!resultList.contains(end)) {
                resultList.add(end);
            }
            return;
        }

        if (leftCards.size() == 0) {
            return;
        }
        // 取一个值
        Integer type = leftCards.get(0);
        leftCards.remove(type);

        // 尝试匹配 3type
        List<Integer> newMatched = new ArrayList<>();
        List<Integer> newLeftCard = matchKe(new ArrayList<>(leftCards), type,
                leftJin, newMatched);
        if (null != newLeftCard) {
            List<List<Integer>> newMatchList = new ArrayList<>(matched);
            newMatchList.add(newMatched);
            int newLeftJin = leftJin - calcNum(newMatched, 0);
            matchThird(resultList, tabCnt + 1, dui, newMatchList, newLeftJin,
                    newLeftCard);
        }

    }

    public List<Integer> matchKe(List<Integer> srcCards, Integer type, int jin,
                                 List<Integer> newMatched) {
        newMatched.add(type);
        if (srcCards.contains(type)) {
            srcCards.remove(type);
            newMatched.add(type);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        if (srcCards.contains(type)) {
            srcCards.remove(type);
            newMatched.add(type);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }


    public int calcNum(List<Integer> total, int match) {
        int ret = 0;

        for (int i : total) {
            if (i == match) {
                ret += 1;
            }
        }

        return ret;
    }

}

