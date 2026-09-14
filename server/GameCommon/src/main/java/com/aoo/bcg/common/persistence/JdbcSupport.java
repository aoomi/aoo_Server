package com.aoo.bcg.common.persistence;

import com.aoo.bcg.common.error.DomainFailure;
import java.sql.SQLException;
import java.util.UUID;

public final class JdbcSupport {
    private JdbcSupport() {}

    public static DomainFailure failure(String operation, Throwable cause) {
        SQLException sql = findSql(cause);
        int code = 1008;
        String message = "数据库服务暂时不可用";
        if (sql != null) {
            String state = sql.getSQLState() == null ? "" : sql.getSQLState();
            int vendor = sql.getErrorCode();
            if (vendor == 1062 || (state.startsWith("23") && vendor == 23505)) {
                code = 1009;
                message = "数据已存在，请勿重复提交";
            } else if (vendor == 1213 || "40001".equals(state)) {
                code = 1010;
                message = "请求发生并发冲突，请重试";
            } else if (state.startsWith("23")) {
                code = 1011;
                message = "数据不符合业务约束";
            } else if (vendor == 1205 || "41000".equals(state)) {
                code = 1012;
                message = "请求处理超时，请重试";
            }
        }
        return new DomainFailure(code, message, UUID.randomUUID().toString(),
            new DatabaseOperationException(operation, cause));
    }

    public static DomainFailure conflict(String operation, Throwable cause) {
        return new DomainFailure(1009, "数据已存在，请勿重复提交", UUID.randomUUID().toString(),
            new DatabaseOperationException(operation, cause));
    }

    private static SQLException findSql(Throwable cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof SQLException sql) return sql;
        }
        return null;
    }

    private static final class DatabaseOperationException extends RuntimeException {
        private DatabaseOperationException(String operation, Throwable cause) {
            super("Database operation failed: " + operation, cause);
        }
    }
}
