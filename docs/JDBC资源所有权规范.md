# JDBC 资源所有权规范

现代模块获取 Connection、Statement、ResultSet、游标、Stream 和批处理资源时必须使用 try-with-resources。公共 API 禁止返回 JDBC 资源或惰性数据库 Stream。

仅连接工厂可通过 `JDBC_OWNERSHIP_TRANSFER` 将关闭责任移交给明确的仓储/服务边界。`JdbcResourceSafetyCheck` 在 Maven validate 扫描现代模块，违反即构建失败。
