package com.ddm.server.http.configuration;

import com.ddm.server.http.IOkHttpClient;
import com.ddm.server.http.OKHttpUtil;
import com.ddm.server.http.impl.OkHttpClientImpl;
import lombok.Data;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * okhttp 配置
 */
@Configuration
@Data
@EnableConfigurationProperties(OkHttpProperties.class)
public class OKHttpConfig {

    @Autowired
    private OkHttpProperties okHttpProperties;

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient().newBuilder().retryOnConnectionFailure(false).connectionPool(pool())
                .connectTimeout(this.okHttpProperties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(this.okHttpProperties.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(this.okHttpProperties.getWriteTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();

    }

    @Bean
    public ConnectionPool pool() {
        return new ConnectionPool(this.okHttpProperties.getMaxIdle(), this.okHttpProperties.getKeepAliveDurationSec(), TimeUnit.SECONDS);
    }

    @Bean
    public IOkHttpClient iOkHttpClient(OkHttpClient okHttpClient) {
        IOkHttpClient iOkHttpClient = new OkHttpClientImpl();
        ((OkHttpClientImpl) iOkHttpClient).setOkHttpClient(okHttpClient);
        OKHttpUtil.setiOkHttpClient(iOkHttpClient);
        return iOkHttpClient;
    }

}
