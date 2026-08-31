package core.server;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Rejects schema drift that would force implicit casts or collation conversion on indexed paths. */
final class SchemaTypeReadiness {
    private SchemaTypeReadiness() {}
    private record Expected(String dataType,boolean unsigned,boolean utf8) {}
    private static final Map<String,Map<String,Expected>> EXPECTED=expected();
    static void verify(Connection connection,String catalog)throws Exception{
        String sql="SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,COLUMN_TYPE,CHARACTER_SET_NAME,COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=?";
        Map<String,String> failures=new LinkedHashMap<>();
        try(var statement=connection.prepareStatement(sql)){statement.setString(1,catalog);try(var rows=statement.executeQuery()){while(rows.next()){
            Map<String,Expected> table=EXPECTED.get(rows.getString(1));if(table==null)continue;Expected expected=table.get(rows.getString(2));if(expected==null)continue;
            String dataType=rows.getString(3),columnType=rows.getString(4),charset=rows.getString(5),collation=rows.getString(6);
            if(!expected.dataType().equalsIgnoreCase(dataType))failures.put(rows.getString(1)+"."+rows.getString(2),"type="+columnType);
            else if(expected.unsigned()&&!columnType.toLowerCase(java.util.Locale.ROOT).contains("unsigned"))failures.put(rows.getString(1)+"."+rows.getString(2),"signed identity");
            else if(expected.utf8()&&(!"utf8mb4".equalsIgnoreCase(charset)||!"utf8mb4_0900_ai_ci".equalsIgnoreCase(collation)))failures.put(rows.getString(1)+"."+rows.getString(2),"charset/collation drift");
        }}}
        for(var table:EXPECTED.entrySet())for(String column:table.getValue().keySet())if(!failures.containsKey(table.getKey()+"."+column)){
            try(var statement=connection.prepareStatement("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND COLUMN_NAME=?")){statement.setString(1,catalog);statement.setString(2,table.getKey());statement.setString(3,column);try(var rows=statement.executeQuery()){if(!rows.next()||rows.getInt(1)!=1)failures.put(table.getKey()+"."+column,"missing");}}
        }
        if(!failures.isEmpty())throw new IllegalStateException("canonical database type mismatch: "+failures);
    }
    private static Expected id(){return new Expected("bigint",true,false);}private static Expected text(){return new Expected("varchar",false,true);}
    private static Map<String,Map<String,Expected>> expected(){Map<String,Map<String,Expected>> values=new LinkedHashMap<>();values.put("aoo_room_snapshot",Map.of("room_id",id(),"play_version",text()));values.put("aoo_room_lease",Map.of("room_id",id(),"owner_node",text()));values.put("aoo_room_event",Map.of("room_id",id(),"event_type",text()));values.put("aoo_settlement",Map.of("room_id",id(),"business_id",text()));values.put("aoo_ledger",Map.of("player_id",id(),"business_id",text()));values.put("aoo_currency_balance",Map.of("player_id",id(),"currency",text()));values.put("aoo_club_member",Map.of("club_id",id(),"player_id",id(),"member_status",text()));values.put("aoo_connection_generation",Map.of("room_id",id(),"user_id",text()));return Map.copyOf(values);}
}
