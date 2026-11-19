package com.aoo.bcg.common.idempotency;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.gamespi.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.time.*;
class GameCommandResultBindingCodecTest {
    final ObjectMapper json=new ObjectMapper(); final IdempotencyKey key=new IdempotencyKey("user","move",7,0,"req");
    GameCommandResult value(){return new GameCommandResult("move","req",Map.of("roomInstanceId","spoof")).withCommittedBinding(new CommittedCommandBinding(2,"user","move",7,0,"req","A",1,2,3));}
    String encoded() throws Exception{return GameCommandResultJsonCodec.encode(json,value());}
    @Test void v2RoundTrip() throws Exception {assertEquals(value(),GameCommandResultJsonCodec.decode(json,encoded(),key,2));}
    @Test void v1IgnoresTypedLookingMetadata() throws Exception {assertNull(GameCommandResultJsonCodec.decode(json,encoded(),key,1).committedBinding());}
    @Test void rejectsWrongScope() {assertThrows(IllegalArgumentException.class,()->GameCommandResultJsonCodec.decode(json,encoded(),new IdempotencyKey("other","move",7,0,"req"),2));}
    @Test void rejectsMissingAndUnknownSchema(){assertThrows(IllegalArgumentException.class,()->GameCommandResultJsonCodec.decode(json,GameCommandResultJsonCodec.encode(json,new GameCommandResult("move","req",Map.of())),key,2));assertThrows(IllegalArgumentException.class,()->GameCommandResultJsonCodec.decode(json,encoded(),key,3));}
    @Test void rejectsFractionOverflowAndString() throws Exception {for(String replacement:new String[]{"0.5","18446744073709551616","\"3\""}) {String text=encoded().replace("\"stateVersion\":3","\"stateVersion\":"+replacement);assertThrows(IllegalArgumentException.class,()->GameCommandResultJsonCodec.decode(json,text,key,2));}}
    @Test void narrowUsesSameValidator() throws Exception {assertEquals(value().committedBinding(),GameCommandResultJsonCodec.decodeCommittedBinding(json,json.readTree(encoded()).get("committedBinding"),key));}
    @Test void saveRejectsEnvelopeBeforeConnection() {var store=new JdbcIdempotencyStore<GameCommandResult>(null,json,GameCommandResult.class,Clock.systemUTC());assertThrows(IllegalArgumentException.class,()->store.saveResult(key,new IdempotencyResult<>(0,"v1",1,value(),Instant.now().plusSeconds(60))));assertThrows(IllegalArgumentException.class,()->store.saveResult(key,new IdempotencyResult<>(1,"v2",2,value(),Instant.now().plusSeconds(60))));}
}
