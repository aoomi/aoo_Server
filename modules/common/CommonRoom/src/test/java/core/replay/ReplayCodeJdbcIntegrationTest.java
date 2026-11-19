package core.replay;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.aoo.bcg.common.persistence.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named="AOO_DB_IT_URL",matches=".+")
class ReplayCodeJdbcIntegrationTest {
    @Test void mysqlUniqueIndexesMakeConcurrentAllocationIdempotent()throws Exception{
        var source=new DriverManagerDataSource(System.getenv("AOO_DB_IT_URL"),System.getenv("AOO_DB_IT_USER"),System.getenv("AOO_DB_IT_PASSWORD"));
        long base=System.currentTimeMillis();
        try{
            try(var c=source.getConnection();var q=c.prepareStatement("INSERT INTO replay_set_manifest(room_id,set_id,play_version,schema_version,event_count,content_hash,closed_at,archive_after,delete_after,legal_hold) VALUES(?,1,'it',1,1,REPEAT('a',64),CURRENT_TIMESTAMP(3),DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 180 DAY),DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 730 DAY),FALSE)")){for(int i=0;i<24;i++){q.setLong(1,base+i);q.addBatch();}q.executeBatch();}
            var service=new ReplayCodeService(new JdbcReplayCodeRepository(source));
            long seatZero=base+101,seatOne=base+102,viewer=base+1000;
            var participants=new JdbcReplayParticipantRegistry(source);
            participants.grant(base,1,seatZero,0);participants.grant(base,1,seatOne,1);
            var events=new JdbcPerspectiveReplayRepository(source);
            events.append(new PerspectiveReplayEvent(base,1,1,ReplayEventVisibility.PUBLIC,0,"public",new byte[]{1},1,"it"));
            events.append(new PerspectiveReplayEvent(base,1,2,ReplayEventVisibility.PLAYER_PRIVATE,seatZero,"seat-zero",new byte[]{2},1,"it"));
            events.append(new PerspectiveReplayEvent(base,1,2,ReplayEventVisibility.PLAYER_PRIVATE,seatOne,"seat-one",new byte[]{3},1,"it"));
            ExecutorService pool=Executors.newFixedThreadPool(12);
            try{
                List<Future<ReplayCodeRepository.Mapping>> jobs=new ArrayList<>();
                for(int i=0;i<24;i++){long room=base+i;jobs.add(pool.submit(()->service.allocate(room,1)));}
                Set<String> codes=new HashSet<>();for(var job:jobs)assertTrue(codes.add(job.get().code()));
                List<Future<String>> same=new ArrayList<>();for(int i=0;i<12;i++)same.add(pool.submit(()->service.allocate(base,1).code()));
                String stable=same.getFirst().get();for(var job:same)assertEquals(stable,job.get());
                var resolved=service.resolve(viewer,ReplayCodeService.LookupType.REPLAY_CODE,stable);
                assertEquals(base,resolved.roomId());assertTrue(new JdbcReplayCodeRepository(source).mayView(viewer,base,1));
                var replay=new RecordReplayQueryService(source,new com.fasterxml.jackson.databind.ObjectMapper()).replay(viewer,base,1,0,100);
                assertEquals(List.of("public","seat-zero"),replay.events().stream().map(RecordReplayQueryService.ReplayEvent::messageId).toList());
            }finally{pool.shutdownNow();}
        }finally{
            try(var c=source.getConnection()){for(String table:List.of("perspective_replay_event","perspective_replay_event_archive","replay_participant","replay_participant_archive")){try(var q=c.prepareStatement("DELETE FROM "+table+" WHERE room_id>=? AND room_id<?")){q.setLong(1,base);q.setLong(2,base+24);q.executeUpdate();}}try(var q=c.prepareStatement("DELETE a FROM replay_short_code_access a JOIN replay_short_code c ON c.short_code=a.short_code WHERE c.room_id>=? AND c.room_id<?")){q.setLong(1,base);q.setLong(2,base+24);q.executeUpdate();}try(var q=c.prepareStatement("DELETE FROM replay_short_code WHERE room_id>=? AND room_id<?")){q.setLong(1,base);q.setLong(2,base+24);q.executeUpdate();}try(var q=c.prepareStatement("DELETE FROM replay_set_manifest WHERE room_id>=? AND room_id<?")){q.setLong(1,base);q.setLong(2,base+24);q.executeUpdate();}}
        }
    }
}
