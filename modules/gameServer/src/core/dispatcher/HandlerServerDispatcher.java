package core.dispatcher;

import BaseCommon.CommClass;
import BaseCommon.CommLog;
import com.ddm.server.common.CommLogD;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HandlerServerDispatcher implements IServerMessageDispatcher {
    public static final Map<String, IServerBaseHandler> registryHandlers = new HashMap();

    private static class SingletonHolder {
        static final HandlerServerDispatcher instance = new HandlerServerDispatcher();

        private SingletonHolder() {
        }
    }

    private HandlerServerDispatcher() {
    }

    public static HandlerServerDispatcher getInstance() {
        return HandlerServerDispatcher.SingletonHolder.instance;
    }

    public void init() {
        this.registerRequestHandlers(IServerBaseHandler.class);
    }


    public void registerRequestHandlers(Class<? extends IServerBaseHandler> clazz) {
        List<Class<?>> dealers = CommClass.getAllClassByInterface(clazz, clazz.getPackage().getName());
        int regCnt = 0;
        for (Class<?> cs : dealers) {
            IServerBaseHandler dealer = null;
            try {
                dealer = (IServerBaseHandler) CommClass.forName(cs.getName()).newInstance();
            } catch (Exception e) {
                CommLogD.error("HandlerServerDispatcher register handler occured error:{}", e.getMessage(), e);
            }
            if (null == dealer) {
                continue;
            }
            registryHandlers.put(dealer.getEvent(), dealer);
            regCnt += 1;
        }
        CommLogD.info("HandlerServerDispatcher registerHandler count:{}", regCnt);
    }


    @Override
    public void registryHandleMessage(String event, String message) {
        try {
            IServerBaseHandler iRegistryBaseHandler = registryHandlers.get(event);
            if (Objects.isNull(iRegistryBaseHandler)) {
                CommLog.error("registryHandleMessage Handle event:{},message", event, message);
            } else {
                iRegistryBaseHandler.handleMessage(message);
            }
        } catch (Exception e) {
            CommLog.error("handle [0x{}] request failed, reason: {}", event, e.getMessage(), e);
        }
    }


}
