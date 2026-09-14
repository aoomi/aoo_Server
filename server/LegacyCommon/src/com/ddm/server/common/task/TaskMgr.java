package com.ddm.server.common.task;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

public class TaskMgr {

    private SchedulerFactory schedulerFactory;
    private Scheduler scheduler;

    private static class SingleCase {
        public static final TaskMgr INSTANCE = new TaskMgr();
    }

    private TaskMgr() {

    }

    public static TaskMgr get() {
        return SingleCase.INSTANCE;
    }

    public void init(String fileName) throws SchedulerException {
        schedulerFactory = new StdSchedulerFactory(fileName);
        scheduler = schedulerFactory.getScheduler();
    }

    public void startAll() throws SchedulerException {
        scheduler.start();
    }


    public void shutdownAll() throws SchedulerException {
        scheduler.shutdown();
    }

    public void start(TaskTrigger taskTrigger) throws SchedulerException {
        scheduler.scheduleJob(taskTrigger.getJob(), taskTrigger.getTrigger());
    }

    public void stop(TaskTrigger taskTrigger) throws SchedulerException {
        scheduler.unscheduleJob(taskTrigger.getTrigger().getKey());
    }

}
