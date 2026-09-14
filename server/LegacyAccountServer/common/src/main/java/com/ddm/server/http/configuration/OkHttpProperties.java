package com.ddm.server.http.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "okhttp")
public class OkHttpProperties {
    @Value("${okhttp.connect-timeout-ms:50}")
    private Long connectTimeoutMs;
    @Value("${okhttp.read-timeout-ms:50}")
    private Long readTimeoutMs;
    @Value("${okhttp.write-timeout-ms:50}")
    private Long writeTimeoutMs;
    @Value("${okhttp.max-idle:50}")
    private Integer maxIdle;
    @Value("${okhttp.keep-alive-duration-sec:50}")
    private Long keepAliveDurationSec;
}
