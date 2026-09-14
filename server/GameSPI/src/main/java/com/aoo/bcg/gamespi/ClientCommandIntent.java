package com.aoo.bcg.gamespi;

import java.util.Map;

/** The complete set of values accepted from an untrusted game client. */
public record ClientCommandIntent(String msgId, String requestId, long sequence, CommandPayload body) {
    public ClientCommandIntent(String msgId, String requestId, long sequence, Map<String, Object> body) {
        this(msgId, requestId, sequence, CommandPayload.copyOf(body));
    }

    public ClientCommandIntent {
        if (msgId == null || msgId.isBlank() || requestId == null || requestId.isBlank()
                || requestId.length() > 128 || sequence <= 0)
            throw new IllegalArgumentException("invalid client command intent");
        body = body == null ? CommandPayload.empty() : body;
    }
}
