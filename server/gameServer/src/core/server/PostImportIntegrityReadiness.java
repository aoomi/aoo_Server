package core.server;

import java.sql.Connection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Revalidates constraints and indexes after any historical-data import before traffic is admitted. */
final class PostImportIntegrityReadiness {
    private PostImportIntegrityReadiness() {}

    static void verify(Connection connection, String catalog) throws Exception {
        Set<String> defects = new LinkedHashSet<>();
        String constraints = "SELECT CONSTRAINT_NAME,TABLE_NAME,CONSTRAINT_TYPE,ENFORCED "
            + "FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=?";
        try (var statement = connection.prepareStatement(constraints)) {
            statement.setString(1, catalog);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    String type = rows.getString("CONSTRAINT_TYPE");
                    String enforced = rows.getString("ENFORCED");
                    if (("CHECK".equals(type) || "FOREIGN KEY".equals(type)) && !"YES".equalsIgnoreCase(enforced))
                        defects.add(rows.getString("TABLE_NAME") + "." + rows.getString("CONSTRAINT_NAME") + " (not enforced)");
                }
            }
        }
        String indexes = "SELECT TABLE_NAME,INDEX_NAME,IS_VISIBLE FROM information_schema.STATISTICS "
            + "WHERE TABLE_SCHEMA=? GROUP BY TABLE_NAME,INDEX_NAME,IS_VISIBLE";
        Set<String> primaryTables = new LinkedHashSet<>();
        try (var statement = connection.prepareStatement(indexes)) {
            statement.setString(1, catalog);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    String table = rows.getString("TABLE_NAME");
                    String index = rows.getString("INDEX_NAME");
                    if ("PRIMARY".equals(index)) primaryTables.add(table);
                    if (!"YES".equalsIgnoreCase(rows.getString("IS_VISIBLE"))) defects.add(table + "." + index + " (invisible)");
                }
            }
        }
        for (String table : REQUIRED_PRIMARY_KEYS) if (!primaryTables.contains(table)) defects.add(table + " (primary key missing)");
        if (!defects.isEmpty()) throw new IllegalStateException("post-import integrity validation failed: " + String.join(", ", defects));
    }

    private static final Set<String> REQUIRED_PRIMARY_KEYS = Set.of(
        "aoo_business_idempotency", "aoo_room_event", "aoo_room_snapshot", "aoo_room_lease",
        "aoo_settlement", "aoo_ledger", "aoo_currency_balance", "aoo_club_member");
}
