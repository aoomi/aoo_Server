package core.server;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Production policy: foreign keys stay enabled and every declared relationship is orphan-free. */
final class ForeignKeyIntegrityReadiness {
    private ForeignKeyIntegrityReadiness() {}
    private record Relation(String table,String referencedTable,List<String> columns,List<String> referencedColumns) {}
    private record Builder(String table,String referencedTable,List<String> columns,List<String> referencedColumns) {}

    static void verify(Connection connection,String catalog)throws Exception{
        try(var statement=connection.createStatement();var result=statement.executeQuery("SELECT @@SESSION.FOREIGN_KEY_CHECKS")){
            if(!result.next()||result.getInt(1)!=1)throw new IllegalStateException("FOREIGN_KEY_CHECKS must remain enabled in production");
        }
        Map<String,Builder> builders=new LinkedHashMap<>();
        String metadata="SELECT CONSTRAINT_NAME,TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_SCHEMA=? AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY CONSTRAINT_NAME,ORDINAL_POSITION";
        try(var statement=connection.prepareStatement(metadata)){statement.setString(1,catalog);try(var rows=statement.executeQuery()){while(rows.next()){
            String key=rows.getString(1);Builder builder=builders.computeIfAbsent(key,ignored->new Builder(rowsValue(rows,2),rowsValue(rows,4),new ArrayList<>(),new ArrayList<>()));builder.columns().add(rowsValue(rows,3));builder.referencedColumns().add(rowsValue(rows,5));
        }}}
        for(var entry:builders.entrySet()){
            Builder value=entry.getValue();Relation relation=new Relation(value.table(),value.referencedTable(),List.copyOf(value.columns()),List.copyOf(value.referencedColumns()));
            StringBuilder join=new StringBuilder(),present=new StringBuilder();for(int i=0;i<relation.columns().size();i++){if(i>0){join.append(" AND ");present.append(" AND ");}join.append("c.").append(id(relation.columns().get(i))).append("=p.").append(id(relation.referencedColumns().get(i)));present.append("c.").append(id(relation.columns().get(i))).append(" IS NOT NULL");}
            String sql="SELECT COUNT(*) FROM "+id(relation.table())+" c LEFT JOIN "+id(relation.referencedTable())+" p ON "+join+" WHERE "+present+" AND p."+id(relation.referencedColumns().getFirst())+" IS NULL";
            try(var statement=connection.createStatement();var rows=statement.executeQuery(sql)){if(!rows.next()||rows.getLong(1)>0)throw new IllegalStateException("foreign-key orphan rows: "+entry.getKey());}
        }
    }
    private static String rowsValue(java.sql.ResultSet rows,int column){try{return rows.getString(column);}catch(java.sql.SQLException error){throw new IllegalStateException(error);}}
    private static String id(String value){if(value==null||!value.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException("unsafe schema identifier");return "`"+value+"`";}
}
