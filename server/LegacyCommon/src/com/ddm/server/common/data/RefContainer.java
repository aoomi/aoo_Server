package com.ddm.server.common.data;

import com.ddm.server.common.data.ref.RefBase;

import java.util.Random;
import java.util.TreeMap;

/**
 * 配表管理器
 *
 * @param <T>
 */
public class RefContainer<T extends RefBase> extends TreeMap<Object, T> {
    private static final long serialVersionUID = 5170618524149079288L;

    @SuppressWarnings("unchecked")
    public T random() {
        if (this.size() <= 0) {
            return null;
        }
        int rand = 0;
        if (this.size() > 1) {
            rand = java.util.concurrent.ThreadLocalRandom.current().nextInt(this.size());
        }
        return (T) this.values().toArray()[rand];
    }

    public T last() {
        if (this.size() <= 0) {
            return null;
        }
        Object k = this.keySet().toArray()[this.size() - 1];
        return this.get(k);
    }

    public T first() {
        if (this.size() <= 0) {
            return null;
        }
        Object k = this.keySet().toArray()[0];
        return this.get(k);
    }
}
