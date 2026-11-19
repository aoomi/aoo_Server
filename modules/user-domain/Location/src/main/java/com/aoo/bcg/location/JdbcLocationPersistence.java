package com.aoo.bcg.location;

import static com.aoo.bcg.location.LocationModels.*;
import static com.aoo.bcg.location.LocationPersistence.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;

/** MySQL adapter for V20260824_32 risk observations and callback receipts. */
public final class JdbcLocationPersistence implements AuthorizationRepository, RiskSignalRepository, AssessmentRepository, IdempotencyRepository {
    private final DataSource source;
    private final ObjectMapper json;
    public JdbcLocationPersistence(DataSource source){this(source,new ObjectMapper().findAndRegisterModules());}
    public JdbcLocationPersistence(DataSource source,ObjectMapper json){this.source=Objects.requireNonNull(source);this.json=Objects.requireNonNull(json);}

    @Override public void append(String tenant,AuthorizationReport r,String actor){
        insertObservation(id(tenant,"auth",r.eventId()),r.playerId(),null,"LOCATION_AUTHORIZATION",r.status().name(),null,null,null,null,null,r.occurredAt());
    }
    @Override public void append(String tenant,NetworkRiskReport r,String actor){
        int index=0;
        for(RiskSignal s:r.ipSignals()) insertObservation(id(tenant,r.eventId(),"ip"+(index++)),r.playerId(),null,"IP",null,null,null,null,parseIp(s.valueHash()),null,r.observedAt());
        for(RiskSignal s:r.deviceSignals()) insertObservation(id(tenant,r.eventId(),"device"+(index++)),r.playerId(),null,"DEVICE",null,null,null,null,null,requireSha256(s.valueHash()),r.observedAt());
    }
    @Override public void save(String tenant,TableRiskAssessment a,String actor){
        // The observation table cannot represent pair scores. Reuse the shared durable business-result envelope for the JSON result.
        String sql="INSERT INTO aoo_business_idempotency(request_id,request_hash,user_id,operation,room_id,round_no,client_request_id,status,response_code,response_version,response_schema_version,response_payload,created_at,expires_at) VALUES(?,?,?,?,0,0,?,'COMPLETED',0,'v1',1,?,CURRENT_TIMESTAMP(3),DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 30 DAY))";
        try(Connection c=source.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,a.assessmentId());p.setString(2,id(a.toString()));p.setString(3,tenant);p.setString(4,"location.table.assessment");p.setString(5,a.assessmentId());p.setString(6,json.writeValueAsString(a));p.executeUpdate();}catch(Exception e){if(e instanceof SQLException sqlException)throw persistence(sqlException);throw new LocationServiceException(LocationErrorCode.PERSISTENCE_FAILURE,"cannot serialize assessment");}
    }
    @Override public Optional<TableRiskAssessment> findById(String tenant,String id){try(Connection c=source.getConnection();PreparedStatement p=c.prepareStatement("SELECT response_payload FROM aoo_business_idempotency WHERE request_id=? AND user_id=? AND operation='location.table.assessment' AND status='COMPLETED'")){p.setString(1,id);p.setString(2,tenant);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(json.readValue(r.getString(1),TableRiskAssessment.class)):Optional.empty();}}catch(Exception e){if(e instanceof SQLException sqlException)throw persistence(sqlException);throw new LocationServiceException(LocationErrorCode.PERSISTENCE_FAILURE,"cannot deserialize assessment");}}

    private void insertObservation(String id,long player,Long room,String type,String auth,Double lat,Double lon,Double accuracy,byte[] ip,String device,Instant at){
        String sql="INSERT INTO aoo_player_risk_observation(observation_id,player_id,room_id,signal_type,authorization_state,latitude,longitude,accuracy_meters,ip_address,device_hash,observed_at,schema_version) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try(Connection c=source.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,id);p.setLong(2,player);if(room==null)p.setNull(3,Types.BIGINT);else p.setLong(3,room);p.setString(4,type);p.setString(5,auth);setDouble(p,6,lat);setDouble(p,7,lon);setDouble(p,8,accuracy);p.setBytes(9,ip);p.setString(10,device);p.setTimestamp(11,Timestamp.from(at));p.setString(12,"location/v1");p.executeUpdate();}catch(SQLException e){throw persistence(e);}
    }
    @Override public Claim claim(String tenant,String operation,String key,String fingerprint,Instant expiresAt){
        String event=id(tenant,operation,key);String provider=provider(operation);
        try(Connection c=source.getConnection()){
            try(PreparedStatement p=c.prepareStatement("INSERT INTO aoo_external_callback_receipt(provider,event_id,payload_sha256,schema_version) VALUES(?,?,?,?)")){p.setString(1,provider);p.setString(2,event);p.setString(3,fingerprint);p.setString(4,"location/v1");p.executeUpdate();return new Claim(ClaimStatus.CLAIMED,Optional.empty());}
            catch(SQLIntegrityConstraintViolationException duplicate){try(PreparedStatement p=c.prepareStatement("SELECT payload_sha256,processed_at,result_code FROM aoo_external_callback_receipt WHERE provider=? AND event_id=?")){p.setString(1,provider);p.setString(2,event);try(ResultSet rs=p.executeQuery()){if(!rs.next())return new Claim(ClaimStatus.IN_PROGRESS,Optional.empty());if(!fingerprint.equals(rs.getString(1)))return new Claim(ClaimStatus.CONFLICT,Optional.empty());return rs.getTimestamp(2)==null?new Claim(ClaimStatus.IN_PROGRESS,Optional.empty()):new Claim(ClaimStatus.COMPLETED,Optional.ofNullable(rs.getString(3)));}}}
        }catch(SQLException e){throw persistence(e);}
    }
    @Override public void complete(String tenant,String operation,String key,String reference){update("UPDATE aoo_external_callback_receipt SET processed_at=CURRENT_TIMESTAMP(3),result_code=? WHERE provider=? AND event_id=?",reference,provider(operation),id(tenant,operation,key));}
    @Override public void abandon(String tenant,String operation,String key){update("DELETE FROM aoo_external_callback_receipt WHERE result_code IS NULL AND provider=? AND event_id=?",provider(operation),id(tenant,operation,key),null);}
    private void update(String sql,String a,String b,String c){try(Connection x=source.getConnection();PreparedStatement p=x.prepareStatement(sql)){p.setString(1,a);p.setString(2,b);if(c!=null)p.setString(3,c);p.executeUpdate();}catch(SQLException e){throw persistence(e);}}
    private static byte[] parseIp(String value){try{return InetAddress.getByName(value).getAddress();}catch(UnknownHostException e){throw new LocationServiceException(LocationErrorCode.INVALID_ARGUMENT,"invalid IP address");}}
    private static String requireSha256(String value){if(value==null||!value.matches("[0-9a-f]{64}"))throw new LocationServiceException(LocationErrorCode.INVALID_ARGUMENT,"device valueHash must be lowercase SHA-256");return value;}
    private static void setDouble(PreparedStatement p,int i,Double v)throws SQLException{if(v==null)p.setNull(i,Types.DECIMAL);else p.setDouble(i,v);}
    private static String provider(String operation){return switch(operation){case "authorization"->"location-auth";case "network-risk"->"location-network";case "table-risk"->"location-table";default->throw new IllegalArgumentException("unknown operation");};}
    private static String id(String... values){try{var d=MessageDigest.getInstance("SHA-256");d.update(String.join("\u001f",values).getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(d.digest());}catch(Exception e){throw new IllegalStateException(e);}}
    private static LocationServiceException persistence(SQLException e){return new LocationServiceException(LocationErrorCode.PERSISTENCE_FAILURE,"location persistence failed: "+e.getSQLState());}
}
