package com.aoo.bcg.hall.http;

import com.aoo.bcg.hall.room.JdbcHallRepository;
import com.aoo.bcg.hall.room.RoomCreateSaga;
import com.aoo.bcg.hall.room.RoomMembershipAuthorityPort;
import com.aoo.bcg.lobby.LobbyRoomCreateSaga;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import core.replay.RecordReplayQueryService;
import core.replay.ReplayCodeRepository;
import core.replay.ReplayCodeService;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Route boundary: leave remains the canonical membership route. */
final class HallLeaveRouteContractTest {
    @Test
    void leaveRemainsAnExistingRouteAndDoesNotAddReconnectOrHandoffHttpEndpoints() {
        CapturingServer server = new CapturingServer();
        RoomCreateSaga saga = new RoomCreateSaga(
                fake(LobbyRoomCreateSaga.Store.class, (proxy, method, args) -> null),
                fake(LobbyRoomCreateSaga.BillingPort.class, (proxy, method, args) -> null),
                fake(LobbyRoomCreateSaga.RoomPort.class, (proxy, method, args) -> null),
                command -> command,
                fake(LobbyRoomCreateSaga.AuthorityPort.class, (proxy, method, args) -> null));
        RecordReplayQueryService records = new RecordReplayQueryService(
                fake(DataSource.class, (proxy, method, args) -> null), new ObjectMapper());
        ReplayCodeRepository replayRepository = new ReplayCodeRepository() {
            @Override public Optional<Mapping> findShortCode(String code) { return Optional.empty(); }
            @Override public Optional<Mapping> findByTarget(long roomId, int setId) { return Optional.empty(); }
            @Override public Optional<LegacyTarget> findLegacyCode(String code) { return Optional.empty(); }
            @Override public boolean replayReady(long roomId, int setId) { return false; }
            @Override public boolean mayView(long playerId, long roomId, int setId) { return false; }
            @Override public void grantCodeAccess(String code, long playerId) {}
            @Override public long allocatedCount(int length) { return 0; }
            @Override public InsertResult insert(String code, long roomId, int setId) { return InsertResult.CODE_COLLISION; }
        };

        HallHttpRoutes.mount(server, null, null, new ObjectMapper(), saga, records,
                new ReplayCodeService(replayRepository), "i".repeat(32));

        assertEquals(List.of("/health/hall", "/api/v2/hall/replay-codes", "/api/v2/hall",
                "/internal/v1/hall/rooms"), server.paths);
        assertFalse(server.paths.stream().anyMatch(path -> path.contains("reconnect") || path.contains("handoff")));
    }

    @Test
    void missingLeaveIdentityIsNotReplacedFromRoomOrAccount() {
        JdbcHallRepository repository = new JdbcHallRepository(
                fake(DataSource.class, (proxy, method, args) -> null), new ObjectMapper(),
                Clock.systemUTC(), fake(RoomMembershipAuthorityPort.class, (proxy, method, args) -> null));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> repository.leave(7001L, "leave-request-901001", 901001L, "trace", Map.of()));
        assertEquals("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_REQUIRED:roomInstanceId", failure.getMessage());
    }

    private static final class CapturingServer extends HttpServer {
        final List<String> paths = new ArrayList<>();
        @Override public void bind(InetSocketAddress address, int backlog) {}
        @Override public void start() {}
        @Override public void setExecutor(java.util.concurrent.Executor executor) {}
        @Override public java.util.concurrent.Executor getExecutor() { return null; }
        @Override public void stop(int delay) {}
        @Override public HttpContext createContext(String path, HttpHandler handler) {
            paths.add(path);
            return null;
        }
        @Override public HttpContext createContext(String path) { paths.add(path); return null; }
        @Override public void removeContext(String path) {}
        @Override public void removeContext(HttpContext context) {}
        @Override public InetSocketAddress getAddress() { return new InetSocketAddress(0); }
    }

    @SuppressWarnings("unchecked")
    private static <T> T fake(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
