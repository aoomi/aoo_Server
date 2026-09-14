package com.ddm.server.common.utils;

import com.ddm.server.websocket.def.ErrorCode;
import lombok.Data;

/**
 * 自定义的错误
 */
@Data
public class ErrorCodeException extends RuntimeException {
    private ErrorCode errorCode;

    public ErrorCodeException(ErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public ErrorCodeException(String s, ErrorCode errorCode) {
        super(s);
        this.errorCode = errorCode;
    }

    public ErrorCodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ErrorCodeException(Throwable cause) {
        super(cause);
    }

    static final long serialVersionUID = -1848914673093119416L;
}
