package com.ddm.server.http.configuration;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OKHttpConfigTest {
    @Test
    void appliesDocumentedTimeoutAndKeepAliveUnits() {
        OkHttpProperties properties = new OkHttpProperties();
        properties.setConnectTimeoutMs(125L);
        properties.setReadTimeoutMs(250L);
        properties.setWriteTimeoutMs(375L);
        properties.setMaxIdle(7);
        properties.setKeepAliveDurationSec(45L);

        OKHttpConfig config = new OKHttpConfig();
        config.setOkHttpProperties(properties);
        OkHttpClient client = config.okHttpClient();

        assertEquals(125, client.connectTimeoutMillis());
        assertEquals(250, client.readTimeoutMillis());
        assertEquals(375, client.writeTimeoutMillis());
    }
}
