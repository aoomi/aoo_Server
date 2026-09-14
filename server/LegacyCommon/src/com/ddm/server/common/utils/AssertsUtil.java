package com.ddm.server.common.utils;

import com.ddm.server.websocket.def.ErrorCode;
import lombok.NoArgsConstructor;

/**
 * 自定义的Asset断言
 */
@NoArgsConstructor
public class AssertsUtil {

    public static void check(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    public static void check(boolean expression, String message, ErrorCode errorCode) {
        if (!expression) {
            throw new ErrorCodeException(message, errorCode);
        }
    }

    public static void check(boolean expression, String message, ErrorCode errorCode, Object... args) {
        if (!expression) {
            throw new ErrorCodeException(String.format(message, args), errorCode);
        }
    }

    public static void check(boolean expression, String message, ErrorCode errorCode, Object arg) {
        if (!expression) {
            throw new ErrorCodeException(String.format(message, arg), errorCode);
        }
    }

    public static void notNull(Object object, ErrorCode errorCode, String errorMsg) {
        if (object == null) {
            throw new ErrorCodeException(errorMsg, errorCode);
        }
    }

    public static void notEmpty(CharSequence s, ErrorCode errorCode, String errorMsg) {
        if (s == null || s.length() == 0) {
            throw new ErrorCodeException(errorMsg, errorCode);
        }
    }

    public static void notBlank(CharSequence s, ErrorCode errorCode, String errorMsg) {
        if (s == null || s.toString().trim().isEmpty()) {
            throw new ErrorCodeException(errorMsg, errorCode);
        }
    }
}
