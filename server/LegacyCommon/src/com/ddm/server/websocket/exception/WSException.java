package com.ddm.server.websocket.exception;

import com.ddm.server.websocket.def.ErrorCode;

public class WSException extends Exception {

    private static final long serialVersionUID = 7496451863918773713L;

    private IExceptionCallback _callback = null;
    ErrorCode errorCode = null;

    public WSException(ErrorCode code, String msg) {
        super(msg);
        this.errorCode = code;
        _callback = null;
    }

    public WSException(ErrorCode code, String msg, Object... args) {
        super(String.format(msg, args));
        this.errorCode = code;
        _callback = null;
    }

    public WSException(ErrorCode code, String msg, IExceptionCallback callback) {
        super(msg);
        this.errorCode = code;
        _callback = callback;
    }

    public WSException(ErrorCode code, String msg, IExceptionCallback callback, Throwable cause) {
        super(msg, cause);
        _callback = callback;
    }

    public void callback() {
        if (_callback != null) {
            _callback.callback();
        }
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
