package com.aoo.bcg.account.wechat;

/** Stable, non-secret-bearing failure returned by the WeChat protocol adapter. */
public final class WeChatProtocolException extends SecurityException {
    private final Code code;

    public WeChatProtocolException(Code code, String message) {
        super(message);
        this.code = java.util.Objects.requireNonNull(code, "code");
    }

    public Code code() { return code; }

    public enum Code {
        INVALID_CODE,
        PROVIDER_REJECTED,
        PROVIDER_UNAVAILABLE,
        INVALID_PROVIDER_RESPONSE
    }
}
