package com.aoo.bcg.lobby;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LobbyRoomCoreCanonicalizationTest {
    @Test
    void lobbyOwnsRoomCoreImplementationsAndHallFilesAreAdapters() throws Exception {
        String lobbyRepository = read("src/main/java/com/aoo/bcg/lobby/JdbcLobbyRepository.java");
        String hallRepository = read("src/main/java/com/aoo/bcg/hall/room/JdbcHallRepository.java");
        String lobbySaga = read("src/main/java/com/aoo/bcg/lobby/LobbyRoomCreateSaga.java");
        String hallSaga = read("src/main/java/com/aoo/bcg/hall/room/RoomCreateSaga.java");
        String lobbyAuthority = read("src/main/java/com/aoo/bcg/lobby/LobbyHttpRoomAuthorityPort.java");
        String hallAuthority = read("src/main/java/com/aoo/bcg/hall/room/HttpRoomAuthorityPort.java");

        assertTrue(lobbyRepository.contains("package com.aoo.bcg.lobby;"));
        assertTrue(lobbySaga.contains("public class LobbyRoomCreateSaga"));
        assertTrue(lobbyAuthority.contains("public class LobbyHttpRoomAuthorityPort"));
        assertFalse(hallRepository.contains("SELECT "));
        assertFalse(hallSaga.contains("class RoomCreateSaga {"));
        assertFalse(hallAuthority.contains("HttpClient"));
        assertTrue(hallRepository.contains("extends JdbcLobbyRepository"));
        assertTrue(hallSaga.contains("extends LobbyRoomCreateSaga"));
        assertTrue(hallAuthority.contains("extends LobbyHttpRoomAuthorityPort"));
    }

    @Test
    void canonicalRecoveryAndRulePortsPreserveAcceptedBehavior() {
        assertTrue(LobbyActiveRoomRecoveryPolicy.isRecoverable(
                "OPEN", "ACTIVE", Map.of("phase", "WAITING")));
        assertFalse(LobbyActiveRoomRecoveryPolicy.isRecoverable(
                "PLAYING", "ACTIVE", Map.of("finalSettlement", Map.of("rounds", 8))));
        assertTrue(LobbyRoomRuleSchemaValidator.validate(
                Map.of("fields", java.util.List.of(
                        Map.of("key", "playerCount", "defaultValue", 4))),
                Map.of()).containsKey("playerCount"));
    }

    @Test
    void canonicalRouteAndConsumersUseLobbyRoomTypes() throws Exception {
        String routes = read("src/main/java/com/aoo/bcg/lobby/LobbyHttpRoutes.java");
        String bootstrap = read("../Bootstrap/src/main/java/com/aoo/bcg/bootstrap/BootstrapAPP.java");
        assertTrue(routes.contains("JdbcLobbyRepository"));
        assertTrue(routes.contains("LobbyRoomCreateSaga"));
        assertTrue(bootstrap.contains("com.aoo.bcg.lobby.LobbyRoomCreateSaga"));
        assertTrue(bootstrap.contains("com.aoo.bcg.lobby.LobbyRoomCreateSagaStore"));
        assertFalse(bootstrap.contains("com.aoo.bcg.hall.room.RoomCreateSaga"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
