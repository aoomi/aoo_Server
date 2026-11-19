package com.ddm.server.websocket.exception;

import lombok.Data;


@Data
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    protected int errorCode;
    /**
     * 错误信息
     */
    protected String errorMsg;

    public BizException() {
        super();
    }


    public BizException(String errorMsg) {
        super(errorMsg);
        this.errorMsg = errorMsg;
    }

    public BizException(int errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public BizException(int errorCode, String errorMsg, Throwable cause) {
        super(errorMsg, cause);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public final static BizException Of(int errorCode, String format, Object... params) {
        return new BizException(errorCode, String.format(format, params));
    }

    public final static BizException Of(Throwable cause, int errorCode, String format, Object... params) {
        return new BizException(errorCode, String.format(format, params), cause);
    }

    @Override
    public String getMessage() {
        return errorMsg;
    }

    /**
     *
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}