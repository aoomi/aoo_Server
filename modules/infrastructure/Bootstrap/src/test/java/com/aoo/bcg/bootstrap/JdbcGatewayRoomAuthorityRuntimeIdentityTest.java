package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the authority's same-generation state-version transition without
 * weakening the registry's generation, rollback, or same-version fences.
 */
final class JdbcGatewayRoomAuthorityRuntimeIdentityTest {
    private static final Method COPY_WITH_IDENTITY = copyWithIdentityMethod();

    @Test
    void sameGenerationStateVersionAdvanceKeepsTheGeneration() {
        RoomRuntimeIdentity initial = identity(4);
        RoomRuntimeIdentity advanced = identity(5);
        GameRoomHandle current = room(initial);

        GameRoomHandle replacement = copy(current, advanced);

        assertEquals(advanced, replacement.runtimeIdentity());
        assertEquals(initial.roomInstanceId(), replacement.runtimeIdentity().roomInstanceId());
        assertEquals(initial.lifecycleEpoch(), replacement.runtimeIdentity().lifecycleEpoch());
        assertEquals(initial.fencingToken(), replacement.runtimeIdentity().fencingToken());
    }

    @Test
    void differentGenerationRemainsAConflict() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copy(room(identity(4)), new RoomRuntimeIdentity("other", 9, 12, 5)));

        assertEquals("ROOM_RUNTIME_IDENTITY_CONFLICT", failure.getMessage());
    }

    @Test
    void stateVersionRollbackRemainsStale() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copy(room(identity(5)), identity(4)));

        assertEquals("ROOM_RUNTIME_STATE_VERSION_STALE", failure.getMessage());
    }

    @Test
    void registryStillRejectsAReplacementAtTheSameStateVersion() {
        RuntimeGameRoomRegistry rooms = new RuntimeGameRoomRegistry();
        RoomRuntimeIdentity currentIdentity = identity(4);
        GameRoomHandle current = room(currentIdentity);
        rooms.bindRuntime(current);
        GameRoomHandle sameVersion = room(currentIdentity);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> rooms.updateRuntime(sameVersion));

        assertEquals("ROOM_RUNTIME_STATE_VERSION_CONFLICT", failure.getMessage());
        assertSame(current, rooms.requireRuntime(current.roomId()));
    }

    private static GameRoomHandle room(RoomRuntimeIdentity identity) {
        return new GameRoomHandle(1001, 1001, "chengdu1001-v1.0.0", new Object(), identity);
    }

    private static RoomRuntimeIdentity identity(long stateVersion) {
        return new RoomRuntimeIdentity("room-instance-1001", 7, 11, stateVersion);
    }

    private static GameRoomHandle copy(GameRoomHandle room, RoomRuntimeIdentity identity) {
        try {
            return (GameRoomHandle) COPY_WITH_IDENTITY.invoke(null, room, identity);
        } catch (IllegalAccessException impossible) {
            throw new AssertionError(impossible);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new AssertionError(cause);
        }
    }

    private static Method copyWithIdentityMethod() {
        try {
            Method method = JdbcGatewayRoomAuthority.class.getDeclaredMethod(
                    "copyWithIdentity", GameRoomHandle.class, RoomRuntimeIdentity.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
