package com.aoo.bcg.media;

import com.sun.net.httpserver.HttpExchange;
import java.util.Objects;

/** Production bearer-session authentication. No client-side signing secret exists. */
public final class MediaAuthenticator {
    @FunctionalInterface public interface SessionAuthorizer { long authorize(String bearer,String deviceId,String channel,String clientVersion,String ip); }
    private final SessionAuthorizer sessions;
    public MediaAuthenticator(SessionAuthorizer sessions){this.sessions=Objects.requireNonNull(sessions);}
    public long authenticate(HttpExchange ex,byte[] ignoredBody){String authorization=header(ex,"Authorization");if(!authorization.startsWith("Bearer ")||authorization.length()<=7)throw new SecurityException("bearer session required");return sessions.authorize(authorization.substring(7),header(ex,"X-Device-Id"),header(ex,"X-Client-Channel"),header(ex,"X-Client-Version"),ex.getRemoteAddress().getAddress().getHostAddress());}
    private static String header(HttpExchange ex,String name){String v=ex.getRequestHeaders().getFirst(name);if(v==null||v.isBlank())throw new SecurityException(name+" required");return v;}
}
