package core.server;

import ConsoleTask._AConsoleTaskRunner;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public class BshRunner extends _AConsoleTaskRunner {


    public BshRunner() {
    }

    @Override
    public void run(String statements) {
        eval(statements);
    }

    public Object eval(String statements) {
        return null;
    }
}
