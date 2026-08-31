package com.aoo.bcg.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.IntFunction;

public final class JdbcGameConfigurationRepository implements GameConfigurationRepository {
    private final DataSource dataSource;
    private final IntFunction<GameConfigurationCodec<?>> codecs;
    public JdbcGameConfigurationRepository(DataSource dataSource, IntFunction<GameConfigurationCodec<?>> codecs) { this.dataSource=dataSource; this.codecs=codecs; }

    public <C> void publish(PublishedGameConfiguration<C> configuration, long operatorId, String reason) {
        if (configuration == null || configuration.manifest() == null) throw new IllegalArgumentException("configuration manifest is required");
        if (operatorId <= 0) throw new IllegalArgumentException("operatorId must be positive");
        if (reason == null || reason.isBlank() || reason.length() > 500) throw new IllegalArgumentException("publication reason is required and must not exceed 500 characters");
        int gameId=configuration.manifest().gameId(); String version=configuration.manifest().playVersion();
        @SuppressWarnings("unchecked") GameConfigurationCodec<C> codec=(GameConfigurationCodec<C>) requireCodec(gameId);
        String payload=codec.encode(configuration);
        if (payload == null || payload.isBlank()) throw new IllegalArgumentException("encoded configuration payload is required");
        String sql="INSERT INTO aoo_published_game_configuration(game_id,play_version,release_id,configuration_payload,created_by,reason) VALUES(?,?,?,?,?,?)";
        try(var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long releaseId=requirePublishableRelease(connection,gameId,version);
                try(var statement=connection.prepareStatement(sql)) {
                    statement.setInt(1,gameId); statement.setString(2,version); statement.setLong(3,releaseId);
                    statement.setString(4,payload); statement.setLong(5,operatorId); statement.setString(6,reason);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch(SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch(SQLException rollback) { exception.addSuppressed(rollback); }
                throw exception;
            }
        } catch(SQLException exception) { throw new IllegalStateException("cannot publish immutable game configuration: "+gameId+"/"+version,exception); }
    }

    @Override public <C> Optional<PublishedGameConfiguration<C>> find(int gameId, String playVersion) {
        @SuppressWarnings("unchecked") GameConfigurationCodec<C> codec=(GameConfigurationCodec<C>) requireCodec(gameId);
        try(var connection=dataSource.getConnection(); var statement=connection.prepareStatement("SELECT configuration_payload FROM aoo_published_game_configuration WHERE game_id=? AND play_version=?")) { statement.setInt(1,gameId); statement.setString(2,playVersion); try(var result=statement.executeQuery()) { if(!result.next()) return Optional.empty(); PublishedGameConfiguration<C> decoded=codec.decode(result.getString(1)); if(decoded==null || decoded.manifest()==null || decoded.manifest().gameId()!=gameId || !playVersion.equals(decoded.manifest().playVersion())) throw new IllegalStateException("configuration payload identity mismatch: "+gameId+"/"+playVersion); return Optional.of(decoded); } }
        catch(java.sql.SQLException exception) { throw new IllegalStateException("cannot load game configuration",exception); }
    }

    private long requirePublishableRelease(Connection connection,int gameId,String playVersion) throws SQLException {
        String sql="SELECT release_id FROM aoo_game_release WHERE game_id=? AND play_version=? AND status IN ('VALIDATED','ACTIVE') ORDER BY release_version DESC LIMIT 1 FOR UPDATE";
        try(var statement=connection.prepareStatement(sql)) { statement.setInt(1,gameId); statement.setString(2,playVersion); try(var result=statement.executeQuery()) { if(!result.next()) throw new IllegalStateException("no validated immutable release for "+gameId+"/"+playVersion); return result.getLong(1); } }
    }

    private GameConfigurationCodec<?> requireCodec(int gameId) { GameConfigurationCodec<?> codec=codecs.apply(gameId); if(codec==null) throw new IllegalArgumentException("missing configuration codec for gameId="+gameId); return codec; }
}
