package core.dispatcher;

public class RegMaooandler {
    /**
     * 注册中心通知下线
     */
    public static final String CRegistry2ServerOffline = "registry.CRegistry2ServerOffline".toLowerCase();
    /**
     * 注册中心通知上线
     */
    public static final String CRegistry2ServerOnline = "registry.CRegistry2ServerOnline".toLowerCase();
    /**
     * 服务间通知
     */
    public static final String CServer2Server = "server.CServer2Server".toLowerCase();
    /**
     * 通知服务玩家离开
     */
    public static final String CServer2PlayerKickOut = "server.CServer2PlayerKickOut".toLowerCase();
}
