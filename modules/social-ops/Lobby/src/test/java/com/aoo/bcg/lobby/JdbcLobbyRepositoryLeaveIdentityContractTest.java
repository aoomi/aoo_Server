package com.aoo.bcg.lobby;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class JdbcLobbyRepositoryLeaveIdentityContractTest {
    static Map<String,Object> publicIdentity() {
        Map<String,Object> value=identity(); value.remove("sessionId");value.remove("permission");
        value.put("roomId","901001");value.put("playVersion","v1");return value;
    }
    @Test void publicIdentityIsNonAuthorizingAndLosslessOnly() {
        assertDoesNotThrow(()->JdbcLobbyRepository.requirePublicLeaveIdentity(publicIdentity()));
        for(String field:new String[]{"actorType","actorSessionId","accountSessionId","accountId","sessionId","permission","trustedActor","_actor","extra"}) {
            var value=publicIdentity();value.put(field,"forged");
            assertThrows(RuntimeException.class,()->JdbcLobbyRepository.requirePublicLeaveIdentity(value));
        }
        var numeric=publicIdentity();numeric.put("lifecycleEpoch",9007199254740993L);
        assertThrows(RuntimeException.class,()->JdbcLobbyRepository.requirePublicLeaveIdentity(numeric));
    }
    @Test void httpActorIsSeparateFromWssSessionAndSeatedPermission() {
        var value=publicIdentity();value.put("actorType","ACCOUNT_HTTP");value.put("accountSessionId","actual-account-session");
        value.put("accountId",42L);value.put("permission","AUTHENTICATED_ROOM_MEMBER");
        assertDoesNotThrow(()->JdbcLobbyRepository.requireLeaveIdentity(value));
        value.put("sessionId","connection-session");
        assertThrows(SecurityException.class,()->JdbcLobbyRepository.requireLeaveIdentity(value));
        value.remove("sessionId");value.put("permission","SEATED_MEMBER");
        assertThrows(SecurityException.class,()->JdbcLobbyRepository.requireLeaveIdentity(value));
    }
    @Test
    void acceptsLosslessCanonicalDecimalStringsWithoutRewritingIdentity() {
        Map<String, Object> identity = identity();

        assertDoesNotThrow(() -> JdbcLobbyRepository.requireLeaveIdentity(identity));
        // The caller-owned values remain strings so the authority boundary can serialize them losslessly.
        assertEquals("9007199254740993", identity.get("lifecycleEpoch"));
        assertEquals("1107349", identity.get("releaseId"));
    }

    @Test
    void rejectsNonCanonicalOrOutOfRangeDecimalStringsFailClosed() {
        Map<String, Object> leadingZero = identity();
        leadingZero.put("stateVersion", "01");
        assertThrows(IllegalArgumentException.class,
                () -> JdbcLobbyRepository.requireLeaveIdentity(leadingZero));

        Map<String, Object> tooLarge = identity();
        tooLarge.put("lifecycleEpoch", "9223372036854775808");
        assertThrows(IllegalArgumentException.class,
                () -> JdbcLobbyRepository.requireLeaveIdentity(tooLarge));
    }

    @Test
    void retainsExistingNumberCompatibility() {
        Map<String, Object> numeric = identity();
        numeric.put("lifecycleEpoch", 7L);
        numeric.put("stateVersion", 0L);

        assertDoesNotThrow(() -> JdbcLobbyRepository.requireLeaveIdentity(numeric));
    }

    private static Map<String, Object> identity() {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("roomInstanceId", "room-instance-901001");
        identity.put("sessionId", "session-901001");
        identity.put("lifecycleEpoch", "9007199254740993");
        identity.put("fencingToken", "1");
        identity.put("stateVersion", "0");
        identity.put("releaseId", "1107349");
        identity.put("indexGeneration", "80417");
        identity.put("regionCode", "CN-51-01");
        identity.put("sourceHash", "a".repeat(64));
        identity.put("ruleContentHash", "b".repeat(64));
        identity.put("componentContentHash", "c".repeat(64));
        identity.put("cardCodecVersion", "card-v3");
        identity.put("eventInterpreterVersion", "events-v4");
        identity.put("protocolVersion", "protocol-v2");
        identity.put("snapshotSchemaVersion", "room-snapshot-v1");
        identity.put("gameId", "2001");
        identity.put("gameCode", "chengdu2001");
        identity.put("permission", "SEATED_MEMBER");
        return identity;
    }

}
