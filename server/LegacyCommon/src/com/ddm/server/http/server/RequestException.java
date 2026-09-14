package com.ddm.server.http.server;

public class RequestException extends Exception {
    private static final long serialVersionUID = 2420902066665001397L;

    private int code;

    public RequestException(int code, String msg, Object... params) {
        super(String.format(msg, params));
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
