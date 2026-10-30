package com.aoo.bcg.common.persistence;

import org.junit.jupiter.api.Test;
import com.alibaba.druid.pool.DruidDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverManagerDataSourceTimeoutTest {
    @Test void suppliesBoundedMysqlNetworkWaits() {
        assertEquals("jdbc:mysql://db/aoo?connectTimeout=5000&socketTimeout=15000",
                DriverManagerDataSource.boundedUrl("jdbc:mysql://db/aoo"));
    }

    @Test void preservesExplicitMysqlTimeouts() {
        assertEquals("jdbc:mysql://db/aoo?socketTimeout=9000&connectTimeout=7000",
                DriverManagerDataSource.boundedUrl(
                        "jdbc:mysql://db/aoo?socketTimeout=9000&connectTimeout=7000"));
    }

    @Test void doesNotRewriteOtherJdbcDrivers() {
        assertEquals("jdbc:h2:mem:test", DriverManagerDataSource.boundedUrl("jdbc:h2:mem:test"));
    }

    @Test void usesABoundedReusableConnectionPool() throws Exception {
        try (var source = new DriverManagerDataSource("jdbc:h2:mem:pool-contract", "sa", "")) {
            assertTrue(source.isWrapperFor(DruidDataSource.class));
            DruidDataSource pool = source.unwrap(DruidDataSource.class);
            assertEquals(16, pool.getMaxActive());
            assertEquals(15_000, pool.getMaxWait());
            assertEquals(java.util.List.of("SET time_zone='+00:00'"), pool.getConnectionInitSqls());
        }
    }
}
