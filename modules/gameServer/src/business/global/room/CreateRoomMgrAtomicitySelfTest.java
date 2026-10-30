package business.global.room;

import java.util.ArrayList;
import java.util.List;

/** Executable contract tests for CreateRoomMgr's prepare-bind-publish boundary. */
public final class CreateRoomMgrAtomicitySelfTest {
    public static void main(String[] args) throws Exception {
        bindFailureRollsBackAndReturnsNoRoom();
        admissionRejectionIsNotSwallowed();
        publishFailureRollsBackAndReturnsNoRoom();
        successfulBindPublishesExactlyOnce();
        System.out.println("CreateRoomMgrAtomicitySelfTest: PASS");
    }

    private static void bindFailureRollsBackAndReturnsNoRoom() {
        List<String> events = new ArrayList<>();
        expectFailure(() -> CreateRoomMgr.bindThenPublish("room",
                ignored -> {
                    events.add("bind");
                    throw new IllegalStateException("authority unavailable");
                },
                ignored -> events.add("publish"),
                ignored -> events.add("rollback")));
        check(events.equals(List.of("bind", "rollback")), "bind failure must rollback before returning");
    }

    private static void publishFailureRollsBackAndReturnsNoRoom() {
        List<String> events = new ArrayList<>();
        expectFailure(() -> CreateRoomMgr.bindThenPublish("room",
                ignored -> events.add("bind"),
                ignored -> {
                    events.add("publish");
                    throw new IllegalStateException("publish unavailable");
                },
                ignored -> events.add("rollback")));
        check(events.equals(List.of("bind", "publish", "rollback")), "publish failure must rollback");
    }

    private static void admissionRejectionIsNotSwallowed() {
        List<String> events = new ArrayList<>();
        IllegalStateException rejection = expectFailureReturning(() -> CreateRoomMgr.bindThenPublish("room",
                ignored -> {
                    events.add("bind");
                    throw new IllegalStateException("ROOM_ADMISSION_REJECTED|reason=COMPATIBILITY_ORIGIN_REQUIRED"
                            + "|roomId=7|gameId=0|gameCode=unknown|playVersion=missing|source=missing");
                },
                ignored -> events.add("publish"),
                ignored -> events.add("rollback")));
        check(rejection.getMessage().startsWith("ROOM_ADMISSION_REJECTED|"),
                "compatibility rejection root must be preserved");
        check(events.equals(List.of("bind", "rollback")), "admission rejection must rollback before propagation");
    }

    private static void successfulBindPublishesExactlyOnce() throws Exception {
        List<String> events = new ArrayList<>();
        String room = CreateRoomMgr.bindThenPublish("room", ignored -> events.add("bind"),
                ignored -> events.add("publish"), ignored -> events.add("rollback"));
        check("room".equals(room), "successful creation must return the published room");
        check(events.equals(List.of("bind", "publish")), "successful creation must not rollback");
    }

    private static void expectFailure(ThrowingAction action) {
        expectFailureReturning(action);
    }

    private static IllegalStateException expectFailureReturning(ThrowingAction action) {
        try {
            action.run();
            throw new AssertionError("expected creation failure");
        } catch (IllegalStateException expected) {
            return expected;
        } catch (Exception unexpected) {
            throw new AssertionError("unexpected failure type", unexpected);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
