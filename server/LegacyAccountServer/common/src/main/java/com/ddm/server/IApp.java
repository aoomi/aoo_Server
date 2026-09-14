package com.ddm.server;

import com.ddm.server.common.utils.CommLogD;
import org.springframework.boot.CommandLineRunner;
public abstract class IApp implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        if (!initBase()) {
            CommLogD.error("初始化基础服务失败, 退出服务器启动");
            System.exit(-1);
        }
        if (!initLogic()) {
            CommLogD.error("初始化逻辑组建失败, 退出服务器启动");
            System.exit(-1);
        }
        if (!initNetwork()) {
            CommLogD.error("初始化网络组建失败, 退出服务器启动");
            System.exit(-1);
        }
        afterInit();
        this.successServer();
    }

    /**
     * 启动底层模块
     *
     * @param configdir
     */
    protected abstract void beforeInit(String configdir);

    /**
     * 游戏服初始
     * @return
     */
    public boolean initGameLogic() {
        return true;
    }

    /**
     * 大厅服初始化
     * @return
     */
    public boolean initHallLogic() {
        return true;
    }


    /**
     * 基础组建 - 除了基础模块外最先启动的扩展模块
     */
    public void initBaseExtend() {

    }

    /**
     * 基础组建 - 除了基础模块外最先启动的模块
     */
    protected abstract boolean initBase();

    /**
     * 网络组建 - 基础管理器初始化后马上进行初始化
     */
    protected abstract boolean initNetwork();

    /**
     * 逻辑组建 - 在网络组建初始化后初始化
     */
    protected abstract boolean initLogic();

    /**
     * 初始化后的事件
     */
    protected abstract void afterInit();

    /**
     * 成功启动
     */
    protected abstract void successServer();
}
