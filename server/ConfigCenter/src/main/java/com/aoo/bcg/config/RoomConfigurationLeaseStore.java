package com.aoo.bcg.config;

import com.aoo.bcg.common.config.RoomRuleSnapshot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** Binds a room to the configuration selected at creation; active publication is never read again. */
public final class RoomConfigurationLeaseStore {
    public record Lease(long roomId, RoomRuleSnapshot snapshot, long releaseId) {
        public Lease {
            if (roomId <= 0 || snapshot == null || releaseId <= 0)
                throw new IllegalArgumentException("invalid room configuration lease");
        }
    }

    private final DataSource dataSource;
    private final ObjectMapper json;
    public RoomConfigurationLeaseStore(DataSource dataSource, ObjectMapper json) { this.dataSource=dataSource; this.json=json; }

    public Lease open(long roomId, int gameId, String regionCode, String playVersion) {
        try(Connection connection=dataSource.getConnection()) { connection.setAutoCommit(false); try {
            String select="SELECT i.release_id,i.index_generation,i.component_chain,i.rule_validator,i.bundle_hash,r.rule_hash,r.component_hash,r.card_codec_version,r.event_interpreter_version,r.protocol_version FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version AND i.index_generation=a.index_generation JOIN aoo_game_release r ON r.release_id=i.release_id WHERE a.game_id=? AND a.region_code=? AND a.play_version=? AND i.lifecycle_state='ACTIVE' FOR UPDATE";
            try(PreparedStatement statement=connection.prepareStatement(select)) { statement.setInt(1,gameId);statement.setString(2,regionCode);statement.setString(3,playVersion);try(ResultSet row=statement.executeQuery()) { if(!row.next())throw new IllegalStateException("active room configuration index not found");
                String insert="INSERT INTO aoo_room_rule_lock(room_id,game_id,region_code,play_version,index_generation,release_id,component_chain,immutable_rules,bundle_hash,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                try(PreparedStatement write=connection.prepareStatement(insert)){for(int i=1;i<=14;i++){/* assigned below */}write.setLong(1,roomId);write.setInt(2,gameId);write.setString(3,regionCode);write.setString(4,playVersion);write.setLong(5,row.getLong("index_generation"));write.setLong(6,row.getLong("release_id"));write.setString(7,row.getString("component_chain"));write.setString(8,row.getString("rule_validator"));write.setString(9,row.getString("bundle_hash"));write.setString(10,row.getString("rule_hash"));write.setString(11,row.getString("component_hash"));write.setString(12,row.getString("card_codec_version"));write.setString(13,row.getString("event_interpreter_version"));write.setString(14,row.getString("protocol_version"));write.executeUpdate();}
            }} connection.commit(); return require(roomId);
        } catch(Exception failure){connection.rollback();throw failure;} } catch(Exception failure){throw new IllegalStateException("cannot durably lock room configuration",failure);}
    }

    public Optional<Lease> find(long roomId) { try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement("SELECT game_id,play_version,release_id,component_hash,card_codec_version,event_interpreter_version,protocol_version,immutable_rules,created_at FROM aoo_room_rule_lock WHERE room_id=?")){s.setLong(1,roomId);try(ResultSet r=s.executeQuery()){if(!r.next())return Optional.empty();Map<String,Object> rules=json.readValue(r.getString("immutable_rules"),new TypeReference<>(){});RoomRuleSnapshot snapshot=new RoomRuleSnapshot(r.getLong("release_id"),r.getInt("game_id"),r.getString("play_version"),r.getString("component_hash"),r.getString("card_codec_version"),r.getString("event_interpreter_version"),"release-scoring",r.getString("protocol_version"),r.getTimestamp("created_at").toInstant(),rules);return Optional.of(new Lease(roomId,snapshot,r.getLong("release_id")));}}catch(Exception e){throw new IllegalStateException("cannot load room configuration lock",e);} }
    public Lease require(long roomId) {
        return find(roomId).orElseThrow(() -> new IllegalArgumentException("room configuration lease not found"));
    }
}
