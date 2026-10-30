package com.aoo.bcg.gamespi;

public final class AuthoritativeSessionCommandHandler implements GameCommandHandler {
    private final SeatAdmissionCoordinator admissions;

    public AuthoritativeSessionCommandHandler() { this(new SeatAdmissionCoordinator()); }
    public AuthoritativeSessionCommandHandler(SeatAdmissionCoordinator admissions) {
        this.admissions = java.util.Objects.requireNonNull(admissions);
    }

    @Override public GameCommandResult handle(GameRoomHandle room, GameCommandRequest request) {
        if (!"join".equalsIgnoreCase(operation(request.msgId()))) {
            return executeChecked(room.requireAuthoritativeSession(),request);
        }
        admissions.prepare(room, request);
        try {
            GameCommandResult result = executeChecked(room.requireAuthoritativeSession(),request);
            admissions.admitted(room, request);
            return result;
        } catch (RuntimeException | Error failure) {
            admissions.rejected(room, request, failure);
            throw failure;
        }
    }

    private static GameCommandResult executeChecked(AuthoritativeGameSession session,GameCommandRequest request){requireInvariants(session,"before");GameCommandResult result=session.execute(request);requireInvariants(session,"after");return result;}
    private static void requireInvariants(AuthoritativeGameSession session,String phase){var violations=session.invariantViolations();if(!violations.isEmpty())throw new IllegalStateException("authority invariant violation "+phase+": "+String.join(",",violations));}

    private static String operation(String msgId) {
        int separator = msgId.lastIndexOf('.');
        String value = separator < 0 ? msgId : msgId.substring(separator + 1);
        if (value.endsWith("_req")) value = value.substring(0, value.length() - 4);
        return value;
    }
}
