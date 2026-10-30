package com.aoo.bcg.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aoo.bcg.hall.http.HallError;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

final class JdbcLobbyRepositoryRuntimeMetadataProjectionTest {
    @Test
    void compatibilityConstructorFailsClosedForCatalogAndConfiguration() {
        JdbcLobbyRepository repository = new JdbcLobbyRepository(
                inertDataSource(), new ObjectMapper(), Clock.systemUTC(), inertMembershipAuthority());

        HallError catalog = assertThrows(HallError.class,
                () -> repository.catalog("ALL", "3.8.8"));
        assertEquals("HALL_RUNTIME_METADATA_PROJECTION_REQUIRED", catalog.code());

        HallError configuration = assertThrows(HallError.class,
                () -> repository.configuration(1L, "play-v1", "3.8.8"));
        assertEquals("HALL_RUNTIME_METADATA_PROJECTION_REQUIRED", configuration.code());
    }

    private static DataSource inertDataSource() {
        return (DataSource) Proxy.newProxyInstance(
                JdbcLobbyRepositoryRuntimeMetadataProjectionTest.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, arguments) -> {
                    throw new UnsupportedOperationException("database must not be reached");
                });
    }

    private static LobbyMembershipAuthorityPort inertMembershipAuthority() {
        return new LobbyMembershipAuthorityPort() {
            @Override
            public Map<String, Object> join(long roomId, long accountId, int seatNo,
                                             String playVersion, String requestId,
                                             String traceId, Map<String, Object> admission) {
                throw new UnsupportedOperationException("authority must not be reached");
            }

            @Override
            public Map<String, Object> leave(long roomId, long accountId, String playVersion,
                                              String requestId, String traceId,
                                              Map<String, Object> completeIdentity) {
                throw new UnsupportedOperationException("authority must not be reached");
            }
        };
    }
}
