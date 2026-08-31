package core.server;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/** The sole production schema migration entry point. */
public final class FlywaySchemaMigrator {
    private FlywaySchemaMigrator() {}

    public static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .table("flyway_schema_history")
                .encoding("UTF-8")
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .baselineOnMigrate(false)
                .initSql("SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci; "
                        + "SET SESSION time_zone = '+00:00'; "
                        + "SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,"
                        + "ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'")
                .load()
                .migrate();
    }
}
