package com.aoo.bcg.lobby;
import com.fasterxml.jackson.core.type.TypeReference;import com.fasterxml.jackson.databind.ObjectMapper;import javax.sql.DataSource;import java.sql.*;import java.time.Duration;import java.util.*;
public class LobbyRoomCreateSagaStore implements LobbyRoomCreateSaga.Store{
 private final DataSource ds;private final ObjectMapper json;public LobbyRoomCreateSagaStore(DataSource ds,ObjectMapper json){this.ds=Objects.requireNonNull(ds);this.json=Objects.requireNonNull(json);}
 public LobbyRoomCreateSaga.State begin(LobbyRoomCreateSaga.Command c){try(Connection x=ds.getConnection();PreparedStatement s=x.prepareStatement("INSERT INTO aoo_room_create_saga(request_id,request_hash,request_json,account_id,room_id,game_id,play_version,state_version,step,trace_id,created_at,updated_at) VALUES(?,?,?,?,?,?,?,1,'STARTED',?,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE request_id=request_id")){s.setString(1,c.requestId());s.setString(2,c.requestHash());s.setString(3,json.writeValueAsString(requestWithReceiptProvenance(c)));s.setLong(4,c.accountId());s.setLong(5,c.roomId());s.setInt(6,c.gameId());s.setString(7,c.playVersion());s.setString(8,c.traceId());s.executeUpdate();return get(x,c.requestId());}catch(Exception e){throw new IllegalStateException("room saga persistence failure",e);}}
 public LobbyRoomCreateSaga.State find(String id){try(Connection x=ds.getConnection()){return get(x,id);}catch(Exception e){throw new IllegalStateException("room saga lookup failure",e);}}
 public LobbyRoomCreateSaga.State advance(String id,long version,LobbyRoomCreateSaga.Step step,Map<String,Object>response){if(step==LobbyRoomCreateSaga.Step.CONFIRMED)return confirmOnce(id,version,response);update(id,version,step,response,null);return find(id);}
 public void fail(String id,long version,LobbyRoomCreateSaga.Step step,LobbyRoomCreateSaga.Step origin,String reason){try(Connection x=ds.getConnection();PreparedStatement s=x.prepareStatement("UPDATE aoo_room_create_saga SET step=?,compensation_origin_step=?,state_version=state_version+1,failure_code=?,attempt_count=attempt_count+1,next_retry_at=CASE WHEN ?='COMPENSATION_PENDING' AND attempt_count<9 THEN DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL LEAST(60,POW(2,attempt_count)) SECOND) ELSE NULL END,updated_at=CURRENT_TIMESTAMP(3) WHERE request_id=? AND state_version=?")){s.setString(1,step.name());s.setString(2,origin.name());s.setString(3,reason);s.setString(4,step.name());s.setString(5,id);s.setLong(6,version);if(s.executeUpdate()!=1)throw new IllegalStateException("stale room saga stateVersion");}catch(Exception e){throw e instanceof RuntimeException r?r:new IllegalStateException(e);}}
 public boolean acquire(String id,String token,Duration lease){try(Connection x=ds.getConnection();PreparedStatement s=x.prepareStatement("UPDATE aoo_room_create_saga SET processing_token=?,processing_until=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL ? SECOND) WHERE request_id=? AND (processing_until IS NULL OR processing_until<CURRENT_TIMESTAMP(3) OR processing_token=?)")){s.setString(1,token);s.setLong(2,Math.max(5,Math.min(120,lease.toSeconds())));s.setString(3,id);s.setString(4,token);return s.executeUpdate()==1;}catch(Exception e){throw new IllegalStateException("room saga lock failure",e);}}
 public void release(String id,String token){try(Connection x=ds.getConnection();PreparedStatement s=x.prepareStatement("UPDATE aoo_room_create_saga SET processing_token=NULL,processing_until=NULL WHERE request_id=? AND processing_token=?")){s.setString(1,id);s.setString(2,token);s.executeUpdate();}catch(Exception e){throw new IllegalStateException("room saga unlock failure",e);}}
 public List<LobbyRoomCreateSaga.Pending>pending(int limit){String sql="SELECT account_id,request_id,request_json FROM aoo_room_create_saga WHERE request_json IS NOT NULL AND step NOT IN ('CONFIRMED','COMPENSATED') AND (processing_until IS NULL OR processing_until<CURRENT_TIMESTAMP(3)) AND ((next_retry_at IS NOT NULL AND next_retry_at<=CURRENT_TIMESTAMP(3)) OR updated_at<DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 15 SECOND)) ORDER BY COALESCE(next_retry_at,updated_at),request_id LIMIT ?";try(Connection x=ds.getConnection();PreparedStatement s=x.prepareStatement(sql)){s.setInt(1,limit);List<LobbyRoomCreateSaga.Pending>out=new ArrayList<>();try(ResultSet r=s.executeQuery()){while(r.next())out.add(new LobbyRoomCreateSaga.Pending(r.getLong(1),r.getString(2),publicPendingRequest(r.getString(3))));}return List.copyOf(out);}catch(Exception e){throw new IllegalStateException("room saga recovery scan failure",e);}}
 private void update(String id,long version,LobbyRoomCreateSaga.Step step,Map<String,Object>response,String failure){try(Connection x=ds.getConnection();PreparedStatement s=x.prepareStatement("UPDATE aoo_room_create_saga SET step=?,compensation_origin_step=NULL,state_version=state_version+1,response_json=COALESCE(?,response_json),failure_code=?,next_retry_at=NULL,updated_at=CURRENT_TIMESTAMP(3) WHERE request_id=? AND state_version=?")){s.setString(1,step.name());s.setString(2,response==null?null:json.writeValueAsString(response));s.setString(3,failure);s.setString(4,id);s.setLong(5,version);if(s.executeUpdate()!=1)throw new IllegalStateException("stale room saga stateVersion");}catch(Exception e){throw e instanceof RuntimeException r?r:new IllegalStateException(e);}}
 private static final String RECEIPT_PROVENANCE="__aooRoomCreateReceipt";
 private Map<String,Object> receiptProvenance(String requestId,String requestHash){return Map.of("schemaVersion",1,"producer","LobbyRoomCreateSagaStore","eventId",eventId(requestId),"requestHash",requestHash);}
 private Map<String,Object> requestWithReceiptProvenance(LobbyRoomCreateSaga.Command command){Map<String,Object> out=new LinkedHashMap<>(command.requestBody());out.put(RECEIPT_PROVENANCE,receiptProvenance(command.requestId(),command.requestHash()));return out;}
 private Map<String,Object> publicPendingRequest(String text)throws Exception{Map<String,Object> out=json.readValue(text,new TypeReference<>(){});out.remove(RECEIPT_PROVENANCE);return out;}
 private boolean hasValidReceiptProvenance(Map<String,Object> document,String requestId,String requestHash){
  if(!document.containsKey(RECEIPT_PROVENANCE))return false;
  Object raw=document.get(RECEIPT_PROVENANCE);
  if(!(raw instanceof Map<?,?> marker)||!marker.keySet().equals(Set.of("schemaVersion","producer","eventId","requestHash"))||!Objects.equals(marker.get("schemaVersion"),1)||!Objects.equals(marker.get("producer"),"LobbyRoomCreateSagaStore")||!Objects.equals(marker.get("eventId"),eventId(requestId))||!Objects.equals(marker.get("requestHash"),requestHash))throw new IllegalStateException("room creation receipt provenance is unknown or damaged");
  return true;
 }
 private LobbyRoomCreateSaga.State get(Connection x,String id)throws Exception{
  try(PreparedStatement s=x.prepareStatement("SELECT request_hash,step,state_version,response_json,compensation_origin_step,request_json,request_id FROM aoo_room_create_saga WHERE request_id=?")){
   s.setString(1,id);try(ResultSet r=s.executeQuery()){
    if(!r.next())throw new IllegalStateException("room saga missing");String persistedId=r.getString(7),requestHash=r.getString(1);LobbyRoomCreateSaga.Step step=LobbyRoomCreateSaga.Step.valueOf(r.getString(2));
    Map<String,Object> out=r.getString(4)==null?new LinkedHashMap<>():json.readValue(r.getString(4),new TypeReference<>(){});
    Map<String,Object> request=r.getString(6)==null?Map.of():json.readValue(r.getString(6),new TypeReference<>(){});
    boolean requestRequiresReceipt=hasValidReceiptProvenance(request,persistedId,requestHash),responseRequiresReceipt=hasValidReceiptProvenance(out,persistedId,requestHash);
    if(step==LobbyRoomCreateSaga.Step.CONFIRMED){
     if(requestRequiresReceipt&&!responseRequiresReceipt)throw new IllegalStateException("new final room confirmation lost its receipt provenance");
     if(responseRequiresReceipt)requireReceipt(x,persistedId);
    }else if(responseRequiresReceipt)throw new IllegalStateException("receipt provenance exists without final room confirmation");
    out.remove(RECEIPT_PROVENANCE);String origin=r.getString(5);return new LobbyRoomCreateSaga.State(persistedId,requestHash,step,r.getLong(3),out,origin==null?null:LobbyRoomCreateSaga.Step.valueOf(origin));
   }
  }
 }
 /** Only a validated persisted command may be upgraded from the legacy pending format. */
 private Map<String,Object> trustedFinalRequest(Connection x,LobbyRoomCreateSaga.State state)throws Exception{
  try(PreparedStatement q=x.prepareStatement("SELECT account_id,room_id,game_id,play_version,request_hash,request_json FROM aoo_room_create_saga WHERE request_id=?")){
   q.setString(1,state.requestId());try(ResultSet r=q.executeQuery()){
    if(!r.next()||r.getString(6)==null)throw new IllegalStateException("final room request provenance is unavailable");
    Map<String,Object> body=json.readValue(r.getString(6),new TypeReference<>(){});hasValidReceiptProvenance(body,state.requestId(),state.requestHash());body.remove(RECEIPT_PROVENANCE);
    LobbyRoomCreateSaga.Command parsed=LobbyRoomCreateSaga.Command.parse(r.getLong(1),state.requestId(),body,LobbyRoomCreateSaga.CreationOrigin.persisted(body.get("_creationOrigin")));
    if(!(body.get("admission") instanceof Map<?,?> rawAdmission))throw new IllegalStateException("final room request admission provenance is unavailable");
    Map<String,Object> admission=new LinkedHashMap<>();for(var entry:rawAdmission.entrySet()){if(!(entry.getKey() instanceof String key))throw new IllegalStateException("invalid stored admission key");admission.put(key,entry.getValue());}
    LobbyRoomCreateSaga.Command trusted=new LobbyRoomCreateSaga.Command(parsed.accountId(),parsed.requestId(),parsed.roomId(),parsed.gameId(),parsed.playVersion(),parsed.ruleRevision(),parsed.clientVersion(),parsed.stateVersion(),parsed.rules(),parsed.scope(),Map.copyOf(admission),parsed.traceId(),parsed.creationOrigin(),parsed.requestHash()).withValidatedRules(parsed.rules());
    Map<String,Object> knownFormat=json.readValue(json.writeValueAsString(trusted.requestBody()),new TypeReference<>(){});
    if(!Objects.equals(body,knownFormat)||trusted.roomId()!=r.getLong(2)||trusted.gameId()!=r.getInt(3)||!trusted.playVersion().equals(r.getString(4))||!trusted.requestHash().equals(r.getString(5))||!trusted.requestHash().equals(state.requestHash()))throw new IllegalStateException("final room persisted request identity or hash is damaged");
    body.put(RECEIPT_PROVENANCE,receiptProvenance(state.requestId(),state.requestHash()));return body;
   }
  }
 }
 /** Final state and its immutable occurrence are one local transaction, after both external confirmations. */
 private LobbyRoomCreateSaga.State confirmOnce(String id,long version,Map<String,Object>response){
  try(Connection x=ds.getConnection()){
   x.setAutoCommit(false);
   try{
    try(PreparedStatement lock=x.prepareStatement("SELECT request_id FROM aoo_room_create_saga WHERE request_id=? FOR UPDATE")){lock.setString(1,id);try(ResultSet r=lock.executeQuery()){if(!r.next())throw new IllegalStateException("room saga missing");}}
    LobbyRoomCreateSaga.State state=get(x,id);
    if(state.step()==LobbyRoomCreateSaga.Step.CONFIRMED){requireReceipt(x,id);x.commit();return state;}
    if(state.step()!=LobbyRoomCreateSaga.Step.AUTHORITY_CREATED||state.version()!=version)throw new IllegalStateException("stale final room saga stateVersion");
    Map<String,Object> trustedRequest=trustedFinalRequest(x,state);
    try(PreparedStatement update=x.prepareStatement("UPDATE aoo_room_create_saga SET step='CONFIRMED',compensation_origin_step=NULL,state_version=state_version+1,request_json=?,response_json=?,failure_code=NULL,next_retry_at=NULL,updated_at=CURRENT_TIMESTAMP(3) WHERE request_id=? AND state_version=? AND step='AUTHORITY_CREATED'")){
     Map<String,Object> storedResponse=new LinkedHashMap<>(Objects.requireNonNull(response));storedResponse.put(RECEIPT_PROVENANCE,receiptProvenance(state.requestId(),state.requestHash()));update.setString(1,json.writeValueAsString(trustedRequest));update.setString(2,json.writeValueAsString(storedResponse));update.setString(3,id);update.setLong(4,version);if(update.executeUpdate()!=1)throw new IllegalStateException("stale final room saga stateVersion");
    }
    try(PreparedStatement row=x.prepareStatement("SELECT request_hash,account_id,room_id,game_id,play_version,UTC_TIMESTAMP(3) FROM aoo_room_create_saga WHERE request_id=?")){
     row.setString(1,id);try(ResultSet r=row.executeQuery()){
      if(!r.next())throw new IllegalStateException("room saga missing");
      try(PreparedStatement insert=x.prepareStatement("INSERT INTO aoo_room_create_confirmed_receipt(event_id,source_request_id,request_hash,creator_id,room_id,game_id,play_version,occurred_at,schema_version) VALUES(?,?,?,?,?,?,?,?,1)")){
       insert.setString(1,eventId(state.requestId()));insert.setString(2,state.requestId());insert.setString(3,r.getString(1));insert.setLong(4,r.getLong(2));insert.setLong(5,r.getLong(3));insert.setLong(6,r.getLong(4));insert.setString(7,r.getString(5));insert.setTimestamp(8,r.getTimestamp(6,utcCalendar()),utcCalendar());insert.executeUpdate();
      }
     }
    }
    requireReceipt(x,id);LobbyRoomCreateSaga.State confirmed=get(x,id);x.commit();return confirmed;
   }catch(Exception failure){try{x.rollback();}catch(SQLException rollbackFailure){failure.addSuppressed(rollbackFailure);}throw failure;}
  }catch(Exception failure){
   // A thrown commit/close does not establish rollback. Reconcile both durable facts on a fresh connection.
   try(Connection check=ds.getConnection()){
    LobbyRoomCreateSaga.State state=get(check,id);
    if(state.step()==LobbyRoomCreateSaga.Step.CONFIRMED){requireReceipt(check,id);return state;}
    try(PreparedStatement receipt=check.prepareStatement("SELECT event_id FROM aoo_room_create_confirmed_receipt WHERE source_request_id=?")){receipt.setString(1,id);try(ResultSet r=receipt.executeQuery()){if(r.next())throw new IllegalStateException("room final receipt exists without final state");}}
   }catch(Exception unknown){failure.addSuppressed(unknown);}
   throw new LobbyRoomCreateSaga.FinalConfirmationRecoveryException("final room confirmation persistence requires same-request recovery",failure);
  }
 }
 private static Calendar utcCalendar(){return Calendar.getInstance(TimeZone.getTimeZone("UTC"));}
 private static String eventId(String id){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(("ROOM_CREATED_CONFIRMED:v1:"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private void requireReceipt(Connection x,String id)throws SQLException{
  String sql="SELECT s.request_hash,s.account_id,s.room_id,s.game_id,s.play_version,r.event_id,r.request_hash,r.creator_id,r.room_id,r.game_id,r.play_version,r.occurred_at,r.schema_version,s.request_id FROM aoo_room_create_saga s JOIN aoo_room_create_confirmed_receipt r ON r.source_request_id=s.request_id WHERE s.request_id=? AND s.step='CONFIRMED'";
  try(PreparedStatement q=x.prepareStatement(sql)){q.setString(1,id);try(ResultSet r=q.executeQuery()){
   if(!r.next()||!Objects.equals(r.getString(1),r.getString(7))||r.getLong(2)!=r.getLong(8)||r.getLong(3)!=r.getLong(9)||r.getLong(4)!=r.getLong(10)||!Objects.equals(r.getString(5),r.getString(11))||!eventId(r.getString(14)).equals(r.getString(6))||r.getTimestamp(12,utcCalendar())==null||r.getInt(13)!=1)throw new IllegalStateException("room final receipt identity mismatch");
  }}
 }

}
