/*
 * Decompiled with CFR 0.152.
 */
package BaseServer;

import BaseCommon.CommLog;
import BaseServer.Monitor;
import BaseThread.ThreadManager;
import ConsoleTask.ConsoleTaskDealThread;

public class BaseServerInit {
    private static boolean g_inited = false;

    public static void initBaseServer() {
        if (g_inited) {
            CommLog.error("Try to init Cos Server twice!!");
            return;
        }
        g_inited = true;
        ThreadManager.getInstance().regThread();
        ConsoleTaskDealThread cosCmdTaskThread = new ConsoleTaskDealThread();
        cosCmdTaskThread.start();
        Monitor.getInstance().start();
    }
}

