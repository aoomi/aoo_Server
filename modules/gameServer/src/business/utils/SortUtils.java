package business.utils;

import business.global.mj.MJCard;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数组和列表的排序通用类
 */
public class SortUtils {

    /**
     * 数组升序
     *
     * @param sortArray
     */
    public static void ascArray(Integer[] sortArray) {
        if (sortArray != null) {
            Arrays.sort(sortArray);//升序
        }
    }

    /**
     * 反转排序数组
     *
     * @param sortArray
     */
    public static Integer[] reverseArray(Integer[] sortArray) {
        if (sortArray != null) {
            Arrays.sort(sortArray, Comparator.reverseOrder());//降序
        }
        return null;
    }

    /**
     * 升序Set
     *
     * @param sortSet
     */
    public static List<Integer> ascSet(Set<Integer> sortSet) {
        if (sortSet != null) {
            return sortSet.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 降序Set
     *
     * @param sortSet
     */
    public static List<Integer> descSet(Set<Integer> sortSet) {
        if (sortSet != null) {
            return sortSet.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 升序列表
     *
     * @param cardList
     */
    public static void ascList(ArrayList<Integer> cardList) {
        if (CollectionUtils.isNotEmpty(cardList)) {
            Collections.sort(cardList);
        }
    }

    /**
     * 升序列表
     *
     * @param cardList
     */
    public static void descList(ArrayList<Integer> cardList) {
        if (CollectionUtils.isNotEmpty(cardList)) {
            Collections.sort(cardList, Comparator.reverseOrder());
        }
    }

    /**
     * 麻将牌按类型降序
     *
     * @param card
     * @param <T>
     */
    public <T extends MJCard> void descMjCard(List<T> card) {
        Collections.sort(card, Comparator.comparing(T::getType).reversed());
    }

}
