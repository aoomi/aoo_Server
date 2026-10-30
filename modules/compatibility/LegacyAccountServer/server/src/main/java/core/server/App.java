package core.server;

import business.account.AccountManager;
import business.rocketmq.common.RocketmqConstant;
import business.secret.SecretManager;
import com.ddm.server.IApp;
import com.ddm.server.common.utils.CommLog;
import com.ddm.server.common.utils.CommLogD;
import com.ddm.server.common.utils.SpringBeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@ComponentScan(basePackages = {"server.aoo","core","business","com.ddm.server"})
@EnableTransactionManagement
public class App extends IApp {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private SecretManager secretManager;
    @Autowired
    private AccountManager accountManager;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    protected void beforeInit(String configdir) {

    }

    @Override
    protected boolean initBase() {
        //日志配置初始化
        CommLogD.initLog();
        // 自动更新
        SpringBeanUtils.setApplicationContext(this.applicationContext);
        // 初始消费日志线程
        // 初始消费测试
        RocketmqConstant.init();
        // 基础组建 - 除了基础模块外最先启动的扩展模块
        this.initBaseExtend();
        return true;
    }

    @Override
    protected boolean initLogic() {
        this.secretManager.init();
        this.accountManager.init();
        // 大厅服初始化逻辑
        this.initHallLogic();
        // 游戏服初始化逻辑
        this.initGameLogic();
        return true;
    }

    @Override
    protected void afterInit() {

    }

    @Override
    protected void successServer() {
        CommLog.info("[服务器启动完毕]!");
    }

    @Override
    protected boolean initNetwork() {
        return true;
    }
}
