package business.global.mj.util;

import business.global.mj.MJCardInit;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 对对胡（八对）
 *
 * @author Huaxing
 */
public class HuBDuiUtil {

    private volatile static HuBDuiUtil singleton;

    private HuBDuiUtil() {
    }

    public static HuBDuiUtil getInstance() {
        if (singleton == null) {
            synchronized (HuBDuiUtil.class) {
                if (singleton == null) {
                    singleton = new HuBDuiUtil();
                }
            }
        }
        return singleton;
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
     * 检查对胡
     *
     * @param allCardInts
     * @param Jins
     * @param totalJin
     * @param cardType
     * @return
     */
    public boolean checkDuiHu(List<Integer> allCards, int totalJin) {
        return check3nP2(allCards, totalJin);
    }

    public boolean check3nP2(List<Integer> allCards, int totalJin) {

        Map<Integer, Long> groupingByMap = allCards.stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == groupingByMap || groupingByMap.size() <= 0) {
            return false;
        }
        // 三张一样的数量
        int third = 0;
        for (Entry<Integer, Long> map : groupingByMap.entrySet()) {
            if (map.getValue() < 2) {
                map.setValue(map.getValue() + 1);
                totalJin--;
            } else if (map.getValue() == 3) {
                third++;
            }
        }
        if (totalJin < 0) {
            return false;
        }


        if (third > 0) {
            // 检查三张一样的数量
            third -= 1;
        } else if (totalJin > 0) {
            totalJin -= 1;
        } else {
            return false;
        }

        if (third == totalJin || totalJin >= 2) {
            return true;
        }

        return false;

    }
}
