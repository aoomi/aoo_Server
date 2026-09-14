package com.ddm.server.http.server;

import BaseCommon.CommClass;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.IpUtils;
import com.ddm.server.http.annotation.RequestMapping;
import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.lang.reflect.Method;
import java.util.*;

public class HttpDispather {

    private Map<HttpMethod, MethodAdapater> methodAdapaters = new HashMap<>();

    private Set<String> allowRequestedIP = new HashSet<>();

    public void init(String pack) throws Exception {
        this.allowRequestedIP = new Gson().fromJson(Config.getAllowRequestedIP(), Set.class);
        Set<Class<?>> dealers = CommClass.getClasses(pack);
        for (Class<?> cs : dealers) {
            Object instance = null;
            for (Method method : cs.getMethods()) {
                RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 2) {
                    throw new IllegalArgumentException("[" + method.getName() + "]不是固定2个参数");
                }
                if (params[0] != HttpRequest.class) {
                    throw new IllegalArgumentException("[" + method.getName() + "]第一个参数不是HttpRequest");
                }
                if (params[1] != HttpResponse.class) {
                    throw new IllegalArgumentException("[" + method.getName() + "]第二个参数不是HttpResponse");
                }
                if (instance == null) {
                    instance = CommClass.forName(cs.getName()).newInstance();
                }
                for (HttpMethod httpMethod : mapping.method()) {
                    if (methodAdapaters.get(httpMethod) == null) {
                        methodAdapaters.put(httpMethod, new MethodAdapater());
                    }
                    methodAdapaters.get(httpMethod).addAdapter(mapping.uri(), new HttpAdaperter(instance, method));
                }
            }
        }
    }


    public void handle(ChannelHandlerContext context, FullHttpRequest request) {
        String requestUri = new QueryStringDecoder(request.uri()).path();
        HttpResponse response = new HttpResponse(context, requestUri);
        try {
            // 请求类型
            MethodAdapater methodAdapater = methodAdapaters.get(HttpMethod.nameOf(request.method().name()));
            if (this.checkAllowRequestedIP(context, request)) {
                if (Objects.nonNull(methodAdapater) && methodAdapater.containsKey(requestUri)) {
                    methodAdapater.getAdaperter(requestUri).invoke(new HttpRequest(request), response);
                } else {
                    response.response(404, "File Not Found");
                }
            } else {
                response.response(405, "File Not Found!");
            }
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof RequestException) {
                RequestException re = (RequestException) cause;
                response.error(re.getCode(), re.getMessage());
            } else {
                response.error(300001, "服务器处理请求失败");
            }
            CommLogD.error("Http 服务器处理请求失败", e);
        }
    }

    public boolean checkAllowRequestedIP(ChannelHandlerContext context, FullHttpRequest request) {
        InetSocketAddress remote = (InetSocketAddress) context.channel().remoteAddress();
        String remoteAddress = remote.getAddress().getHostAddress();
        String ipStr = IpUtils.getIpAddress(remoteAddress, request.headers()::get);
        if (StringUtils.isEmpty(ipStr)) {
            return false;
        }
        return this.allowRequestedIP.contains(ipStr);
    }
}
