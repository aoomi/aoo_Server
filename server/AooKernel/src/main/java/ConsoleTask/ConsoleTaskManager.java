/*
 * Decompiled with CFR 0.152.
 */
package ConsoleTask;

import BaseCommon.CommLog;
import ConsoleTask._AConsoleTaskRunner;

public class ConsoleTaskManager {
    private static ConsoleTaskManager instance = new ConsoleTaskManager();
    private _AConsoleTaskRunner runner = null;

    public static ConsoleTaskManager GetInstance() {
        return instance;
    }

    public void setRunner(_AConsoleTaskRunner _runner) {
        this.runner = _runner;
    }

    public void run(String cmd) {
        if (this.runner == null) {
            CommLog.info(String.format("Class<_ACosCmdRunner> doesn't reg to CosCmdManager, reg it to deal the cmd:%s", cmd));
            return;
        }
        this.runner.run(cmd);
    }
}

