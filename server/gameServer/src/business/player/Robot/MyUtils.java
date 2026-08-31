package business.player.Robot;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 通用类。
 */
public class MyUtils {
    /**
     * 返回指定集合中指定个数的元素组合（ArrayList）组成的流。
     *
     * @param coll 指定集合
     * @param size 组合中的元素个数
     * @return 组合List的流
     */
    public static <E> Stream<List<E>> combListStream(Collection<E> coll, int size) {
        return combStream(coll, size, ArrayList<E>::new, null, null);
    }

    /**
     * 返回指定集合中指定个数的元素组合组成的流。
     *
     * @param coll                指定集合
     * @param size                组合中的元素个数。仅当greedy为false时有意义。
     * @param combCollFactory     新建集合对象的函数，用于新建元素组合使用的集合，参数为元素集合
     * @param elementFilter       组合中所有元素需要符合的条件，null表示不设此条件 TODO 删除此参数
     * @param elementInCombFilter 一个组合中的元素需要相互符合的条件，null表示不设此条件
     * @return 组合的流
     */
    public static <E, C extends Collection<E>> Stream<C> combStream(Collection<E> coll, int size,
                                                                    Function<Collection<E>, C> combCollFactory, Predicate<E> elementFilter,
                                                                    BiPredicate<E, E> elementInCombFilter) {
        if (size == 0) {
            return Stream.of(combCollFactory.apply(Collections.emptyList()));
        }
        if (coll.isEmpty() || size > coll.size() || size < 0) {
            return Stream.empty();
        }

        if (size == 1) {
            if (elementFilter == null) {
                return coll.stream().map(e -> combCollFactory.apply(Arrays.asList(e)));
            } else {
                return coll.stream().filter(elementFilter).map(e -> combCollFactory.apply(Arrays.asList(e)));
            }
        }

        List<E> list;
        if (elementFilter == null) {
            if (elementInCombFilter == null && size == coll.size()) {
                return Stream.of(combCollFactory.apply(coll));
            }
            list = (coll instanceof List) ? (List<E>) coll : new ArrayList<>(coll);
        } else {
            list = coll.stream().filter(elementFilter).collect(Collectors.toList());
            if (list.isEmpty() || size > list.size()) {
                return Stream.empty();
            }
            if (elementInCombFilter == null && size == list.size()) {
                return Stream.of(combCollFactory.apply(list));
            }
        }

        return IntStream.rangeClosed(0, list.size() - size).boxed().flatMap(index -> {
            E first = list.get(index);
            List<E> others = list.subList(index + 1, list.size());
            Predicate<E> othersElementFilter = elementFilter;
            if (elementInCombFilter != null) {
                Predicate<E> othersWithFirstFilter = e -> elementInCombFilter.test(first, e);
                othersElementFilter = othersElementFilter == null ? othersWithFirstFilter
                        : othersElementFilter.and(othersWithFirstFilter);
            }
            return combStream(others, size - 1, combCollFactory, othersElementFilter, elementInCombFilter)
                    .peek(comb -> comb.add(first));
        });
    }
}
