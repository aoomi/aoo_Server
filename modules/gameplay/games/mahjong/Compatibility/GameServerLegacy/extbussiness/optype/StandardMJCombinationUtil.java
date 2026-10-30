package business.global.mj.extbussiness.optype;

import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 自贡斗十四里面偷和巴有用到
 * 模板麻将排列组合
 * @author zhujianming
 * @date 2020-12-30 13:43
 */
public class StandardMJCombinationUtil {
    public static <T> List<List<T>> combinations(List<T> list, int k) {
        if (k == 0 || list.isEmpty()) {//去除K大于list.size的情况。即取出长度不足K时清除此list
            return Collections.emptyList();
        }
        if (k == 1) {//递归调用最后分成的都是1个1个的，从这里面取出元素
            return list.stream().map(e -> Stream.of(e).collect(Collectors.toList())).collect(Collectors.toList());
        }
        Map<Boolean, List<T>> headAndTail = split(list, 1);
        List<T> head = headAndTail.get(true);
        List<T> tail = headAndTail.get(false);
        List<List<T>> c1 = combinations(tail, (k - 1)).stream().map(e -> {
            List<T> l = new ArrayList<>();
            l.addAll(head);
            l.addAll(e);
            return l;
        }).collect(Collectors.toList());
        List<List<T>> c2 = combinations(tail, k);
        c1.addAll(c2);
        return c1;
    }

    /**
     * 根据n将集合分成两组
     **/
    public static <T> Map<Boolean, List<T>> split(List<T> list, int n) {
        return IntStream
                .range(0, list.size())
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(i, list.get(i)))
                .collect(Collectors.partitioningBy(entry -> entry.getKey() < n, Collectors.mapping(AbstractMap.SimpleEntry::getValue, Collectors.toList())));
    }

    public static void main(String args[]) throws Exception {
        List<Integer> input = Stream.of(1101, 1102, 1103).collect(Collectors.toList());
        List<List<Integer>> combinations = combinations(input, 2);
        System.out.println("2-->" + combinations.toString() + "/" + combinations.size());
        ArrayList<List<Integer>> collect1 = combinations.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(
                        Comparator.comparing(p -> replaceLaiZi(p, Arrays.asList(1))))), ArrayList::new));
        System.out.println("3-->" + collect1.toString() + "/" + collect1.size());
    }

    public static String replaceLaiZi(List<Integer> cardList, List<Integer> laiZiList) {
        cardList.sort(Comparator.naturalOrder());
        String content = "," + cardList.toString().replace("[", "").replace("]", "") + ",";
        if (CollectionUtils.isNotEmpty(laiZiList)) {
            for (Integer item : laiZiList) {
                //x只是作为占位符
                content = content.replace("," + item + ",", ",x,");
            }
        }
        return content;
    }
}
