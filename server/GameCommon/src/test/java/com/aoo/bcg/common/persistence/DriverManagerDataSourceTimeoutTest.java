package com.aoo.bcg.common.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
