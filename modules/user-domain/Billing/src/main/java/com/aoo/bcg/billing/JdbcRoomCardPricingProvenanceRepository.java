package com.aoo.bcg.billing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import com.aoo.bcg.billing.RoomCardPricingImportValidator.SourceBinding;
import com.aoo.bcg.billing.RoomCardPricingImportValidator.SourceClass;

/** Persists immutable provenance within the caller's publication transaction. */
public final class JdbcRoomCardPricingProvenanceRepository{
    private static final String COLUMNS="configuration_version,workbook_manifest_hash,authority_hash,source_class,release_id,index_generation,source_hash,free_policy_ref,free_valid_from,free_valid_until,always_free";
    public void save(Connection connection,RoomCardPricingProvenance value)throws SQLException{
        Objects.requireNonNull(connection,"connection");Objects.requireNonNull(value,"value");
        if(connection.getAutoCommit())throw new IllegalArgumentException("provenance save requires caller transaction");
        var existing=find(connection,value.publicationId(),value.policyKey(),true);
        if(existing!=null){requireSame(existing,value,null);return;}
        String sql="INSERT INTO aoo_room_card_policy_provenance(publication_id,policy_key,"+COLUMNS+") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try(var q=connection.prepareStatement(sql)){
            int i=1;q.setLong(i++,value.publicationId());q.setString(i++,value.policyKey());
            q.setString(i++,value.configurationVersion());q.setString(i++,value.workbookManifestHash());q.setString(i++,value.authorityHash());
            q.setString(i++,value.sourceBinding().sourceClass().name());q.setLong(i++,value.sourceBinding().releaseId());
            q.setLong(i++,value.sourceBinding().indexGeneration());q.setString(i++,value.sourceBinding().sourceHash());q.setString(i++,value.freeRef());
            timestamp(q,i++,value.freeValidFrom());timestamp(q,i++,value.freeValidUntil());q.setBoolean(i,value.alwaysFree());q.executeUpdate();
        }catch(SQLException failure){
            if(failure.getSQLState()==null||!failure.getSQLState().startsWith("23"))throw failure;
            existing=find(connection,value.publicationId(),value.policyKey(),true);
            if(existing==null)throw failure;
            requireSame(existing,value,failure);
        }
    }
    public RoomCardPricingProvenance load(Connection connection,long publicationId,String policyKey)throws SQLException{
        if(publicationId<=0||policyKey==null||!policyKey.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("invalid provenance key");
        var result=find(Objects.requireNonNull(connection,"connection"),publicationId,policyKey,false);
        if(result==null)throw new IllegalArgumentException("missing pricing provenance");return result;
    }
    private RoomCardPricingProvenance find(Connection connection,long publicationId,String key,boolean lock)throws SQLException{
        String sql="SELECT "+COLUMNS+" FROM aoo_room_card_policy_provenance WHERE publication_id=? AND policy_key=?"+(lock?" FOR UPDATE":"");
        try(var q=connection.prepareStatement(sql)){
            q.setLong(1,publicationId);q.setString(2,key);
            try(var row=q.executeQuery()){
                if(!row.next())return null;
                return new RoomCardPricingProvenance(publicationId,key,row.getString(1),row.getString(2),row.getString(3),
                        new SourceBinding(SourceClass.valueOf(row.getString(4)),row.getLong(5),row.getLong(6),row.getString(7)),
                        row.getString(8),instant(row.getTimestamp(9)),instant(row.getTimestamp(10)),row.getBoolean(11));
            }
        }
    }
    private static void requireSame(RoomCardPricingProvenance existing,RoomCardPricingProvenance expected,SQLException cause){
        if(!existing.equals(expected))throw new IllegalArgumentException("pricing provenance cannot be rewritten",cause);
    }
    private static Instant instant(Timestamp value){return value==null?null:value.toInstant();}
    private static void timestamp(PreparedStatement q,int index,Instant value)throws SQLException{
        if(value==null)q.setNull(index,java.sql.Types.TIMESTAMP);else q.setTimestamp(index,Timestamp.from(value));
    }
}
