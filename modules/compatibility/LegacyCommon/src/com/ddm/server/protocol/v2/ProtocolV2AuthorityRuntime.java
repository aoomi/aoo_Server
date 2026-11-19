package com.ddm.server.protocol.v2;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Transport-neutral production hook for the authoritative game runtime. */
public final class ProtocolV2AuthorityRuntime {
    public interface Dispatcher {
        Object dispatch(Command command);
    }

    public record Command(long accountId, String msgId, String requestId, long sequence,
                          String roomId, int roundNo, String playVersion, long timestamp,
                          Map<String, Object> body) {
        public Command {
            body = Map.copyOf(body == null ? Map.of() : body);
        }
    }

    private static volatile Dispatcher dispatcher;

    private ProtocolV2AuthorityRuntime() { }

    public static void install(Dispatcher value) {
        dispatcher = Objects.requireNonNull(value, "dispatcher");
    }

    public static Optional<Object> dispatch(Command command) {
        Dispatcher current = dispatcher;
        return current == null ? Optional.empty() : Optional.ofNullable(current.dispatch(command));
    }
}
