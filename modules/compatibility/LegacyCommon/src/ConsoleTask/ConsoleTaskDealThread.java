package ConsoleTask;

import BaseCommon.CommLog;
import BaseTask.SyncTask.SyncTaskManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Java 26 compatible console reader that terminates cleanly when stdin is closed.
 */
public class ConsoleTaskDealThread extends Thread {
    private volatile boolean threadExit;

    public ConsoleTaskDealThread() {
        setName("ConsoleTaskDealThread");
        setDaemon(true);
    }

    public void ExitThread() {
        threadExit = true;
        interrupt();
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (!threadExit) {
                String line = reader.readLine();
                if (line == null) {
                    return;
                }
                String command = line.trim();
                if (!command.isEmpty()) {
                    SyncTaskManager.task(() -> ConsoleTaskManager.GetInstance().run(command));
                }
            }
        } catch (IOException | RuntimeException error) {
            if (!threadExit) {
                CommLog.error("ConsoleTaskDealThread.run", error);
            }
        }
    }
}
