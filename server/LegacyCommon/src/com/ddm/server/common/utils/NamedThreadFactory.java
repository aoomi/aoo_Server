package com.ddm.server.common.utils;

import BaseThread.ThreadManager;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可命名线程工厂
 *
 * @author kingston
 */
public class NamedThreadFactory implements ThreadFactory {

    private final ThreadGroup threadGroup;

    private final String groupName;

    private final boolean daemo;

    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public NamedThreadFactory(String group) {
        this(group, false);
    }

    public NamedThreadFactory(String group, boolean daemo) {
        this.groupName = group;
        this.daemo = daemo;
        this.threadGroup = Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable r) {
        String name = getNextThreadName();
        Thread ret = new Thread(threadGroup, r, name, 0);
        ret.setDaemon(daemo);
        ThreadManager.getInstance().regThread(ret.threadId());
        return ret;
    }

    private String getNextThreadName() {
        return this.groupName + "-thread-" + this.idGenerator.getAndIncrement();
    }

}
