package business.global.sharegm;

import com.ddm.server.common.CommLogD;
import core.ioc.ContainerMgr;

/**
 * @author xsj
 * @date 2020/10/30 16:15
 * @description 初始化共享key
 */
public class ShareInitMgr {
    private static ShareInitMgr instance = new ShareInitMgr();
    //初始化共享key
    private final String SHARE_DATA_INIT_KEY = "shareDataInitKey";

    public static ShareInitMgr getInstance() {
        return instance;
    }

    //是否需要初始化缓存数据
    private Boolean shareDataInit;


    /**
     * 初始化节点
     */
    public void init() {
        this.setShareDataInit(!ContainerMgr.get().getRedis().exists(SHARE_DATA_INIT_KEY));
        if (this.getShareDataInit()) {
            CommLogD.info("初始化redis数据");
        } else {
            CommLogD.info("不用初始化redis数据");
        }
        ContainerMgr.get().getRedis().put(SHARE_DATA_INIT_KEY, "init");

    }

    public Boolean getShareDataInit() {
        return shareDataInit;
    }

    public void setShareDataInit(Boolean shareDataInit) {
        this.shareDataInit = shareDataInit;
    }
}
