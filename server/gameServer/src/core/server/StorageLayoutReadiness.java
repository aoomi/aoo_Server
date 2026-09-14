package core.server;

import java.sql.Connection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Verifies physical MySQL storage limits that are not represented by JDBC column types. */
final class StorageLayoutReadiness {
    private StorageLayoutReadiness() {}

    static void verify(Connection connection, String catalog) throws Exception {
        Set<String> defects = new LinkedHashSet<>();
        String tables = "SELECT TABLE_NAME,ENGINE,TABLE_COLLATION,ROW_FORMAT FROM information_schema.TABLES "
            + "WHERE TABLE_SCHEMA=? AND TABLE_TYPE='BASE TABLE' AND TABLE_NAME LIKE 'aoo\\_%' ESCAPE '\\\\'";
        try (var statement = connection.prepareStatement(tables)) {
            statement.setString(1, catalog);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    String table = rows.getString("TABLE_NAME");
                    if (!"InnoDB".equalsIgnoreCase(rows.getString("ENGINE"))) defects.add(table + " (engine)");
                    String collation = rows.getString("TABLE_COLLATION");
                    if (collation == null || !collation.toLowerCase().startsWith("utf8mb4_")) defects.add(table + " (charset)");
                    String rowFormat = rows.getString("ROW_FORMAT");
                    if (!"Dynamic".equalsIgnoreCase(rowFormat) && !"Compressed".equalsIgnoreCase(rowFormat)) defects.add(table + " (row format)");
                }
            }
        }
        String oversized = "SELECT s.TABLE_NAME,s.INDEX_NAME,SUM(CASE WHEN c.CHARACTER_SET_NAME='utf8mb4' "
            + "THEN COALESCE(s.SUB_PART,c.CHARACTER_MAXIMUM_LENGTH,0)*4 WHEN c.CHARACTER_SET_NAME IS NOT NULL "
            + "THEN COALESCE(s.SUB_PART,c.CHARACTER_MAXIMUM_LENGTH,0) ELSE COALESCE(c.NUMERIC_PRECISION,8) END) bytes "
            + "FROM information_schema.STATISTICS s JOIN information_schema.COLUMNS c ON c.TABLE_SCHEMA=s.TABLE_SCHEMA "
            + "AND c.TABLE_NAME=s.TABLE_NAME AND c.COLUMN_NAME=s.COLUMN_NAME WHERE s.TABLE_SCHEMA=? "
            + "GROUP BY s.TABLE_NAME,s.INDEX_NAME HAVING bytes>3072";
        try (var statement = connection.prepareStatement(oversized)) {
            statement.setString(1, catalog);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) defects.add(rows.getString(1) + "." + rows.getString(2) + " (index bytes " + rows.getLong(3) + ")");
            }
        }
        if (!defects.isEmpty()) throw new IllegalStateException("storage layout validation failed: " + String.join(", ", defects));
    }
}
