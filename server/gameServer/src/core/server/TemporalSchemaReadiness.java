package core.server;

import java.sql.Connection;
import java.util.Map;
import java.util.Set;

/** Enforces UTC, millisecond precision, and explicit database/application timestamp ownership. */
final class TemporalSchemaReadiness {
    private TemporalSchemaReadiness() {}
    private static final Set<String> DATABASE_OWNED=Set.of("aoo_connection_generation.updated_at","aoo_currency_balance.updated_at","perspective_replay_event.created_at");
    private static final Set<String> APPLICATION_OWNED=Set.of("aoo_room_snapshot.captured_at","aoo_room_lease.expires_at","aoo_business_idempotency.created_at","aoo_business_idempotency.expires_at","aoo_ledger.created_at","aoo_settlement.created_at","aoo_outbox.created_at");
    static void verify(Connection connection,String catalog)throws Exception{
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT @@SESSION.time_zone")){if(!rows.next()||!Set.of("+00:00","UTC").contains(rows.getString(1)))throw new IllegalStateException("database session time zone must be UTC");}
        String sql="SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,DATETIME_PRECISION,COLUMN_DEFAULT,EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND DATA_TYPE IN ('datetime','timestamp') AND (TABLE_NAME LIKE 'aoo\\_%' ESCAPE '\\\\' OR TABLE_NAME LIKE 'admin\\_%' ESCAPE '\\\\' OR TABLE_NAME LIKE 'game\\_profile\\_%' ESCAPE '\\\\' OR TABLE_NAME LIKE 'perspective\\_%' ESCAPE '\\\\')";
        java.util.Set<String>seen=new java.util.HashSet<>();try(var statement=connection.prepareStatement(sql)){statement.setString(1,catalog);try(var rows=statement.executeQuery()){while(rows.next()){
            String key=rows.getString(1)+"."+rows.getString(2),type=rows.getString(3),defaultValue=rows.getString(5),extra=rows.getString(6);seen.add(key);
            if(!"datetime".equalsIgnoreCase(type)||rows.getInt(4)!=3)throw new IllegalStateException("temporal column must be DATETIME(3): "+key);
            boolean current=defaultValue!=null&&defaultValue.toLowerCase(java.util.Locale.ROOT).contains("current_timestamp");
            if(DATABASE_OWNED.contains(key)&&!current)throw new IllegalStateException("database-owned timestamp lacks CURRENT_TIMESTAMP(3): "+key);
            if(APPLICATION_OWNED.contains(key)&&(current||(extra!=null&&extra.toLowerCase(java.util.Locale.ROOT).contains("on update"))))throw new IllegalStateException("application-owned timestamp has database mutation: "+key);
        }}}
        if(!seen.containsAll(DATABASE_OWNED)||!seen.containsAll(APPLICATION_OWNED))throw new IllegalStateException("temporal ownership columns are missing");
    }
}
