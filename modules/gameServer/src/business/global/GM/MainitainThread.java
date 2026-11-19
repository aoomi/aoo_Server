package business.global.GM;

import BaseCommon.CommLog;
import BaseThread.ThreadManager;

public class MainitainThread extends Thread {
    public MainitainThread() {
        CommLog.info("MainitainThread init");
        ThreadManager.getInstance().regThread(this.threadId());
    }

    @Override
    public void run() {
        CommLog.info("Start Runtime ShutdownHook。。。");
        MaintainServerMgr.getInstance().shutdownHook();
        CommLog.info("End Runtime ShutdownHook。。。");
    }

}
