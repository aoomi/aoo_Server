package core.dispatcher;

public interface IServerMessageDispatcher {
    /**
     * 注册中心消息处理
     *
     * @param message
     * @return
     */
    public abstract void registryHandleMessage(final String event, final String message);
}
