package core.aop;

import com.ddm.server.annotation.OperLog;
import com.ddm.server.common.utils.GsonUtils;
import com.ddm.server.common.utils.IpUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

@Aspect
@Component
public class OperLogAspect {
    private static final Logger LOG = LoggerFactory.getLogger("operation-audit");

    @Pointcut("@annotation(com.ddm.server.annotation.OperLog)")
    public void operLogPoinCut() {}

    @Pointcut("execution(* core.network.http..*.*(..))")
    public void operExceptionLogPoinCut() {}

    @AfterReturning(value = "operLogPoinCut()", returning = "result")
    public void saveOperLog(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = currentRequest();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        OperLog annotation = method.getAnnotation(OperLog.class);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "operation");
        event.put("method", joinPoint.getTarget().getClass().getName() + "." + method.getName());
        event.put("module", annotation == null ? "" : annotation.operModul());
        event.put("operationType", annotation == null ? "" : annotation.operType());
        event.put("description", annotation == null ? "" : annotation.operDesc());
        event.put("uri", request == null ? "" : request.getRequestURI());
        event.put("ip", request == null ? "" : IpUtil.getIpAddr(request));
        event.put("parameters", request == null ? Map.of() : request.getParameterMap());
        event.put("result", result);
        LOG.info("{}", GsonUtils.toJsonString(event));
    }

    @AfterThrowing(pointcut = "operExceptionLogPoinCut()", throwing = "error")
    public void saveExceptionLog(JoinPoint joinPoint, Throwable error) {
        HttpServletRequest request = currentRequest();
        LOG.error("HTTP operation failed method={} uri={} ip={}",
                joinPoint.getSignature().toShortString(),
                request == null ? "" : request.getRequestURI(),
                request == null ? "" : IpUtil.getIpAddr(request),
                error);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes == null ? null :
                (HttpServletRequest) attributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
    }
}
