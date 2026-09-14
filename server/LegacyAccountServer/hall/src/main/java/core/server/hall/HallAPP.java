package core.server.hall;

import business.account.AccountManager;
import business.sdk.WeChatManager;
import business.secret.SecretManager;
import cenum.ServerSceneEnum;
import core.dispatch.DispatcherComponent;
import core.server.App;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 单游戏启动项
 * @author Administrator
 *
 */
public class HallAPP extends App {
    @Autowired
    private SecretManager secretManager;
    @Autowired
    private AccountManager accountManager;
    @Autowired
    private WeChatManager weChatManager;

    /**
     * 基础组建 - 除了基础模块外最先启动的扩展模块
     */
    @Override
    public void initBaseExtend() {
        // 初始队列线程
        DispatcherComponent.getInstance().init(ServerSceneEnum.HALL);
    }


    /**
     * 大厅服初始化
     * @return
     */
    @Override
    public boolean initHallLogic() {
        // 初始RSA密钥对
        this.secretManager.init();
        // 初始账号管理
        this.accountManager.init();
        // 初始微信
        this.weChatManager.onServerInitOK();



        return true;
    }

    public static void main(String[] args) {
        SpringApplication.run(HallAPP.class, args);
    }

}
