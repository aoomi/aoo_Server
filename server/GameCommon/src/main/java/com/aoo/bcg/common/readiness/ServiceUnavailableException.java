package com.aoo.bcg.common.readiness;

/** Stable boundary error used by transports to return HTTP 503 or reject a socket handshake. */
public final class ServiceUnavailableException extends IllegalStateException {
    public ServiceUnavailableException(String message) { super(message); }
}
