package com.aoo.bcg.common.persistence;

import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.common.error.DomainFailure;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class JdbcSupportTest {
    @Test void mapsStableCodesAndNeverExposesSqlDetails() {
        DomainFailure duplicate = JdbcSupport.failure("insert user", new SQLException("secret SQL", "23000", 1062));
        assertEquals(1009, duplicate.code());
        assertFalse(duplicate.getMessage().contains("SQL"));
        assertFalse(duplicate.publicError().message().contains("secret"));
        assertNotNull(duplicate.getCause());
        assertEquals(1010, JdbcSupport.failure("update room", new SQLException("deadlock", "40001", 1213)).code());
        assertEquals(1012, JdbcSupport.failure("lock room", new SQLException("timeout", "41000", 1205)).code());
        assertEquals(1011, JdbcSupport.failure("fk", new SQLException("fk detail", "23000", 1452)).code());
        assertEquals(1008, JdbcSupport.failure("query", new SQLException("down", "08001", 0)).code());
    }
}
