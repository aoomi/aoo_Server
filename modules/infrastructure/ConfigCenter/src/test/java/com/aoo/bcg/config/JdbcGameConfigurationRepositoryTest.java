package com.aoo.bcg.config;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class JdbcGameConfigurationRepositoryTest {
    private static final String SOURCE_HASH = "a".repeat(64);
    private static final String OTHER_SOURCE_HASH = "b".repeat(64);

    @Test void publishesAndReadsOnlyCanonicalConfigurationTable() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:config;MODE=MySQL;DB_CLOSE_DELAY=-1");
        try(var c=source.getConnection();var s=c.createStatement()){s.execute("CREATE TABLE aoo_game_release(release_id BIGINT PRIMARY KEY,game_id BIGINT NOT NULL,play_version VARCHAR(64) NOT NULL,snapshot_schema_version VARCHAR(64),card_codec_version VARCHAR(64),event_interpreter_version VARCHAR(64),protocol_version VARCHAR(64),source_hash CHAR(64),release_version BIGINT NOT NULL,status VARCHAR(16) NOT NULL)");s.execute("CREATE TABLE aoo_compiled_room_create_index(game_id BIGINT NOT NULL,play_version VARCHAR(64) NOT NULL,release_id BIGINT NOT NULL,index_generation BIGINT NOT NULL)");s.execute("CREATE TABLE aoo_published_game_configuration(game_id BIGINT NOT NULL,play_version VARCHAR(64) NOT NULL,release_id BIGINT NOT NULL,configuration_payload CLOB NOT NULL,created_by BIGINT NOT NULL,reason VARCHAR(500) NOT NULL,PRIMARY KEY(game_id,play_version))");s.execute("INSERT INTO aoo_game_release(release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,protocol_version,source_hash,release_version,status) VALUES(100,62,'v2','snapshot-v2','codec-v2','events-v2','protocol-v2','"+SOURCE_HASH+"',1,'VALIDATED')");s.execute("INSERT INTO aoo_compiled_room_create_index VALUES(62,'v2',100,1)");}
        GameConfigurationCodec<Object> codec=new GameConfigurationCodec<>(){
            public String encode(PublishedGameConfiguration<Object> value){return value.manifest().playVersion();}
            public PublishedGameConfiguration<Object> decode(String payload){return new PublishedGameConfiguration<>(null,new ReleaseManifest(62,payload,"snapshot-v2","codec-v2","events-v2","protocol-v2",SOURCE_HASH,100,1,"c","b",Instant.EPOCH,null),null);}
        };
        var repository=new JdbcGameConfigurationRepository(source,id->id==62?codec:null);
        var value=new PublishedGameConfiguration<Object>(null,new ReleaseManifest(62,"v2","snapshot-v2","codec-v2","events-v2","protocol-v2",SOURCE_HASH,100,1,"c","b",Instant.EPOCH,null),null);
        repository.publish(value,9,"approved");
        assertEquals("v2",repository.find(62,"v2").orElseThrow().manifest().playVersion());
        assertThrows(IllegalStateException.class,()->repository.publish(value,9,"duplicate"));
        try (var c = source.getConnection(); var s = c.createStatement()) {
            s.execute("INSERT INTO aoo_game_release(release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,protocol_version,source_hash,release_version,status) VALUES(103,62,'v2',NULL,NULL,NULL,NULL,NULL,2,'ACTIVE')");
        }
        assertThrows(IllegalStateException.class, () -> repository.publish(value, 9, "latest release incomplete"));
        try (var c = source.getConnection(); var s = c.createStatement()) {
            s.execute("INSERT INTO aoo_game_release(release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,protocol_version,source_hash,release_version,status) VALUES(101,62,'legacy',NULL,NULL,NULL,NULL,NULL,2,'ACTIVE')");
        }
        var legacy = new PublishedGameConfiguration<Object>(null,
                new ReleaseManifest(62,"legacy","snapshot-v2","codec-v2","events-v2","protocol-v2",SOURCE_HASH,101,2,"c","b",Instant.EPOCH,null),null);
        assertThrows(IllegalStateException.class, () -> repository.publish(legacy, 9, "legacy release"));
        try (var c = source.getConnection(); var s = c.createStatement()) {
            s.execute("INSERT INTO aoo_game_release(release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,protocol_version,source_hash,release_version,status) VALUES(102,62,'mismatch','snapshot-db','codec-db','events-db','protocol-db','"+OTHER_SOURCE_HASH+"',3,'ACTIVE')");
        }
        var mismatched = new PublishedGameConfiguration<Object>(null,
                new ReleaseManifest(62,"mismatch","snapshot-client","codec-client","events-client","protocol-client",SOURCE_HASH,102,3,"c","b",Instant.EPOCH,null),null);
        assertThrows(IllegalStateException.class, () -> repository.publish(mismatched, 9, "schema mismatch"));
        try (var c = source.getConnection(); var s = c.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM aoo_published_game_configuration WHERE game_id=62 AND play_version='mismatch'")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
        assertThrows(IllegalStateException.class,()->repository.publish(new PublishedGameConfiguration<>(null,new ReleaseManifest(62,"v3","snapshot-v2","codec-v2","events-v2","protocol-v2",SOURCE_HASH,300,3,"c","b",Instant.EPOCH,null),null),9,"not released"));
        assertThrows(IllegalArgumentException.class,()->repository.publish(value,0,"approved"));
        try(var c=source.getConnection();var s=c.createStatement();var rs=s.executeQuery("SELECT release_id FROM aoo_published_game_configuration WHERE game_id=62 AND play_version='v2'")){assertTrue(rs.next());assertEquals(100,rs.getLong(1));}
    }

    @Test void concurrentPublishersConvergeOnOneImmutableRow() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:configConcurrent;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        try(var c=source.getConnection();var s=c.createStatement()){s.execute("CREATE TABLE aoo_game_release(release_id BIGINT PRIMARY KEY,game_id BIGINT NOT NULL,play_version VARCHAR(64) NOT NULL,snapshot_schema_version VARCHAR(64),card_codec_version VARCHAR(64),event_interpreter_version VARCHAR(64),protocol_version VARCHAR(64),source_hash CHAR(64),release_version BIGINT NOT NULL,status VARCHAR(16) NOT NULL)");s.execute("CREATE TABLE aoo_compiled_room_create_index(game_id BIGINT NOT NULL,play_version VARCHAR(64) NOT NULL,release_id BIGINT NOT NULL,index_generation BIGINT NOT NULL)");s.execute("CREATE TABLE aoo_published_game_configuration(game_id BIGINT NOT NULL,play_version VARCHAR(64) NOT NULL,release_id BIGINT NOT NULL,configuration_payload CLOB NOT NULL,created_by BIGINT NOT NULL,reason VARCHAR(500) NOT NULL,PRIMARY KEY(game_id,play_version))");s.execute("INSERT INTO aoo_game_release(release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,protocol_version,source_hash,release_version,status) VALUES(200,62,'v4','snapshot-v4','codec-v4','events-v4','protocol-v4','"+SOURCE_HASH+"',1,'ACTIVE')");s.execute("INSERT INTO aoo_compiled_room_create_index VALUES(62,'v4',200,4)");}
        GameConfigurationCodec<Object> codec=new GameConfigurationCodec<>(){
            public String encode(PublishedGameConfiguration<Object> value){return value.manifest().playVersion();}
            public PublishedGameConfiguration<Object> decode(String payload){return new PublishedGameConfiguration<>(null,new ReleaseManifest(62,payload,"snapshot-v4","codec-v4","events-v4","protocol-v4",SOURCE_HASH,200,4,"c","b",Instant.EPOCH,null),null);}
        };
        var repository=new JdbcGameConfigurationRepository(source,id->codec);
        var value=new PublishedGameConfiguration<Object>(null,new ReleaseManifest(62,"v4","snapshot-v4","codec-v4","events-v4","protocol-v4",SOURCE_HASH,200,4,"c","b",Instant.EPOCH,null),null);
        AtomicInteger successes=new AtomicInteger();
        try(var workers=Executors.newFixedThreadPool(8)){
            for(int i=0;i<8;i++) workers.submit(()->{try{repository.publish(value,9,"concurrent approval");successes.incrementAndGet();}catch(IllegalStateException expected){}});
            workers.shutdown();assertTrue(workers.awaitTermination(10,TimeUnit.SECONDS));
        }
        assertEquals(1,successes.get());
        try(var c=source.getConnection();var s=c.createStatement();var rs=s.executeQuery("SELECT COUNT(*),MIN(release_id) FROM aoo_published_game_configuration")){assertTrue(rs.next());assertEquals(1,rs.getInt(1));assertEquals(200,rs.getLong(2));}
    }
}
