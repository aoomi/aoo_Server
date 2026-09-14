package core.server;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Fail-fast schema gate for every production capability enabled by the unified runtime. */
final class ProductionSchemaReadiness {
    private static final Map<String,Map<String,ColumnRule>> REQUIRED = requiredSchema();
    private record ColumnRule(boolean nonNull, boolean forbidPlaceholderDefault) {}

    private ProductionSchemaReadiness() {}

    static void verify(DataSource dataSource) {
        Set<String> missing = new LinkedHashSet<>();
        try (var connection=dataSource.getConnection()) {
            DatabaseMetaData metadata=connection.getMetaData();
            requireEnforcedCheckEngine(metadata);
            String catalog=connection.getCatalog();
            for (var requirement:REQUIRED.entrySet()) {
                Map<String,ColumnMetadata> columns=new LinkedHashMap<>();
                try(var result=metadata.getColumns(catalog,null,requirement.getKey(),null)) {
                    while(result.next()) columns.put(result.getString("COLUMN_NAME").toLowerCase(Locale.ROOT),
                            new ColumnMetadata(result.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                                    result.getString("COLUMN_DEF")));
                }
                if(columns.isEmpty()) missing.add(requirement.getKey()+" (table)");
                else for(var column:requirement.getValue().entrySet()) {
                    ColumnMetadata actual=columns.get(column.getKey());
                    if(actual==null) missing.add(requirement.getKey()+"."+column.getKey());
                    else {
                        if(column.getValue().nonNull()&&actual.nullable()) missing.add(requirement.getKey()+"."+column.getKey()+" (nullable)");
                        if(column.getValue().forbidPlaceholderDefault()&&placeholder(actual.defaultValue())) missing.add(requirement.getKey()+"."+column.getKey()+" (placeholder default)");
                    }
                }
            }
            Set<String> checks=new LinkedHashSet<>();
            try(var statement=connection.prepareStatement("SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=? AND CONSTRAINT_TYPE='CHECK'")) {
                statement.setString(1,catalog);try(var result=statement.executeQuery()){while(result.next())checks.add(result.getString(1).toLowerCase(Locale.ROOT));}
            }
            for(String required:REQUIRED_CHECKS)if(!checks.contains(required))missing.add("CHECK "+required);
            ForeignKeyIntegrityReadiness.verify(connection,catalog);
            QueryPlanReadiness.verify(connection);
            SchemaTypeReadiness.verify(connection,catalog);
            TemporalSchemaReadiness.verify(connection,catalog);
            IdentitySchemaReadiness.verify(connection,catalog);
            PostImportIntegrityReadiness.verify(connection,catalog);
            StorageLayoutReadiness.verify(connection,catalog);
        } catch(Exception exception) {
            String detail = exception.getMessage();
            throw new IllegalStateException("cannot inspect production database schema"
                    + (detail == null || detail.isBlank() ? "" : ": " + detail), exception);
        }
        if(!missing.isEmpty()) throw new IllegalStateException(
                "production database migrations are incomplete: "+String.join(", ",missing));
    }

    private record ColumnMetadata(boolean nullable,String defaultValue) {}
    private static final Set<String> REQUIRED_CHECKS=Set.of("chk_idempotency_status","chk_room_event_identity","chk_room_event_visibility","chk_snapshot_identity","chk_lease_identity","chk_settlement_identity","chk_ledger_identity","chk_club_member_identity","chk_club_member_status","chk_play_variant","chk_room_template");
    private static void requireEnforcedCheckEngine(DatabaseMetaData metadata)throws java.sql.SQLException{
        String product=metadata.getDatabaseProductName();int major=metadata.getDatabaseMajorVersion(),minor=metadata.getDatabaseMinorVersion();
        if(!"MySQL".equalsIgnoreCase(product)||major<8||(major==8&&minor<0))throw new IllegalStateException("MySQL 8.0.16+ with enforced CHECK constraints is required");
        var connection=metadata.getConnection();try(var statement=connection.createStatement();var result=statement.executeQuery("SELECT VERSION()")){if(!result.next())throw new IllegalStateException("database version unavailable");String version=result.getString(1);String[]parts=version.split("[.-]");int patch=parts.length>2?Integer.parseInt(parts[2]):0;if(major==8&&minor==0&&patch<16)throw new IllegalStateException("MySQL 8.0.16+ with enforced CHECK constraints is required");}
    }
    private static boolean placeholder(String value){return value!=null&&(value.isBlank()||"0".equals(value)||"''".equals(value));}
    private static ColumnRule identity(){return new ColumnRule(true,true);}
    private static ColumnRule required(){return new ColumnRule(true,false);}
    private static Map<String,Map<String,ColumnRule>> requiredSchema() {
        Map<String,Map<String,ColumnRule>> values=new LinkedHashMap<>();
        values.put("aoo_business_idempotency",Map.of("request_id",identity(),"status",identity(),"expires_at",required()));
        values.put("aoo_room_event",Map.of("room_id",identity(),"event_sequence",required(),"visibility",identity(),"owner_player_id",required()));
        values.put("aoo_room_snapshot",Map.of("room_id",identity(),"fencing_token",required(),"last_event_sequence",required(),"state_payload",required()));
        values.put("aoo_room_lease",Map.of("room_id",identity(),"owner_node",identity(),"fencing_token",required(),"expires_at",required()));
        values.put("aoo_settlement",Map.of("business_id",identity(),"room_id",identity(),"round_no",required(),"result_payload",required()));
        values.put("aoo_ledger",Map.of("business_id",identity(),"player_id",identity(),"balance_after",required()));
        values.put("aoo_currency_balance",Map.of("player_id",identity(),"currency",identity(),"balance",required()));
        values.put("aoo_club_member",Map.of("club_id",identity(),"player_id",identity(),"member_status",identity(),"online",required()));
        return Map.copyOf(values);
    }
}
