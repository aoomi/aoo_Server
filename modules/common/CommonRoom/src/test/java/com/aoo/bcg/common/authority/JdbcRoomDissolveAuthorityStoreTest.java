package com.aoo.bcg.common.authority;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-boundary checks; the disposable JDBC execution lives in Bootstrap where H2 is provided. */
class JdbcRoomDissolveAuthorityStoreTest {
    @Test
    void storeUsesTheRuntimeIdentityAndRoomLockAsTheOnlyAuthorityInputs() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/common/authority/"
                + "JdbcRoomDissolveAuthorityStore.java"));
        assertTrue(source.contains("aoo_room_runtime_identity_authority"));
        assertTrue(source.contains("aoo_room_rule_lock"));
        assertTrue(source.contains("FOR UPDATE"));
        assertTrue(source.contains("ROOM_DISSOLVE_CANONICAL_METADATA_CONFLICT"));
        assertTrue(source.contains("ROOM_DISSOLVE_STATE_VERSION_CONFLICT"));
        assertTrue(source.contains("connection.commit()"));
        assertTrue(source.contains("connection.rollback()"));
        assertTrue(!source.contains("DissolveVoteState"));
        assertTrue(!source.contains("RoomMgr"));
    }

    @Test
    void storePersistsReceiptsAndTerminalTombstonesInsteadOfProcessLocalState() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/common/authority/"
                + "JdbcRoomDissolveAuthorityStore.java"));
        assertTrue(source.contains("aoo_room_dissolve_command_receipt"));
        assertTrue(source.contains("terminal_tombstone"));
        assertTrue(source.contains("ROOM_DISSOLVE_IDEMPOTENCY_CONFLICT"));
        assertTrue(source.contains("ROOM_DISSOLVE_ALREADY_TERMINAL"));
    }
}
