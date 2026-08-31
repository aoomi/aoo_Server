package com.ddm.server.common.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Maps {

    /**
     * List2Map
     *
     * @param keyMapper
     * @param list
     * @return
     */
    public static <K, V> Map<K, V> list2Map(Function<? super V, ? extends K> keyMapper, List<V> list) {
        return list.stream().collect(Collectors.toMap(keyMapper, (p) -> p));
    }

    /**
     * HashMap
     *
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, V> newMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Thread-safe view for which all reads are mutex-free and map updates (e.g.putAll) are atomic.
     *
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, V> newConcurrentMap() {
        ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();

        return map;
    }

    /**
     * allowing concurrent modifications.
     *
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, V> newConcurrentHashMap() {
        ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();

        return map;
    }
}
