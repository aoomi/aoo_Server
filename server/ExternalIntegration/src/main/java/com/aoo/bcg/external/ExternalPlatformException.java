package com.aoo.bcg.external;

public sealed class ExternalPlatformException extends RuntimeException
        permits ExternalPlatformException.Configuration, ExternalPlatformException.CircuitOpen,
        ExternalPlatformException.Transport, ExternalPlatformException.Protocol {
    ExternalPlatformException(String message) { super(message); }
    ExternalPlatformException(String message, Throwable cause) { super(message, cause); }

    public static final class Configuration extends ExternalPlatformException { public Configuration(String message) { super(message); } }
    public static final class CircuitOpen extends ExternalPlatformException { public CircuitOpen() { super("external platform circuit is open"); } }
    public static final class Transport extends ExternalPlatformException { public Transport(String message, Throwable cause) { super(message, cause); } }
    public static final class Protocol extends ExternalPlatformException {
        private final int statusCode;
        public Protocol(int statusCode) { super("external platform returned HTTP " + statusCode); this.statusCode = statusCode; }
        public int statusCode() { return statusCode; }
    }
}
