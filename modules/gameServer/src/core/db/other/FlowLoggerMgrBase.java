package core.db.other;

import BaseCommon.CommClass;
import com.ddm.server.common.CommLogD;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FlowLoggerMgrBase<Logger extends FlowLoggerBase> {

    private List<Logger> loggers = new ArrayList<>();
    private Map<String, Method> methods = new HashMap<String, Method>();

    @SuppressWarnings("unchecked")
    public boolean init(Class<Logger> baselogger, String loggerPath) {
        List<Class<?>> loggersClass = CommClass.getAllClassByInterface(baselogger, loggerPath);

        Method[] baseMethod = Object.class.getDeclaredMethods();

        for (Method method : baselogger.getMethods()) {
            if (isInSuperType(method, baseMethod)) {
                continue;
            }
            if (methods.containsKey(method.getName())) {
                CommLogD.error("FlowLogger 重复定义了log 方法[{}]", method);
                System.exit(-1);
            }
            methods.put(method.getName(), method);
        }

        for (Class<?> cs : loggersClass) {
            Logger logger = null;
            try {
                logger = (Logger) CommClass.forName(cs.getName()).newInstance();
            } catch (Exception e) {
                CommLogD.error("初始化SDK日志失败，原因{}", e.getMessage(), e);
            }
            if (null == logger || !logger.isOpen()) {
                continue;
            }
            loggers.add(logger);
            CommLogD.info("注册SDK日志 {} ", logger.getClass().getSimpleName());
        }
        return true;
    }

    private boolean isInSuperType(Method method, Method[] baseMethod) {
        for (Method m : baseMethod) {
            if (method.getName().equals(m.getName())) {
                return true;
            }
        }
        return false;
    }

    protected void log(String method, Object... params) {
        for (Logger logger : loggers) {
            if (!logger.isOpen()) {
                continue;
            }
            try {
                methods.get(method).invoke(logger, params);
            } catch (Exception e) {
                CommLogD.error("{} 记录 {} 时发生异常,信息:{}", logger.getClass().getSimpleName(), method, e.getMessage(), e);
            }
        }
    }
}
