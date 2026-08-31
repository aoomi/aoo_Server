package com.aoo.bcg.common.club;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/** Cursor-only member directory designed for million-member clubs. */
public final class JdbcClubMemberIndex implements ClubMemberIndex<ClubMemberRecord> {
    private static final TypeReference<Map<String, Object>> PROFILE_TYPE = new TypeReference<>() {};
    private final DataSource dataSource;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final ClubAggregateTransaction transactions;
    private final com.aoo.bcg.common.cache.CacheShardRouter cacheShards=new com.aoo.bcg.common.cache.CacheShardRouter(64);
    private final com.aoo.bcg.common.cache.ProtectedLookupCache<String,ClubMemberRecord> cache=
            new com.aoo.bcg.common.cache.ProtectedLookupCache<>(com.aoo.bcg.gamespi.time.AuthoritativeTimeSource.systemUtc(),java.time.Duration.ofSeconds(30),java.time.Duration.ofSeconds(3),100_000);

    public JdbcClubMemberIndex(DataSource dataSource, ObjectMapper mapper, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.transactions = new ClubAggregateTransaction(dataSource);
    }

    @Override public Optional<ClubMemberRecord> find(long clubId, long playerId) {
        validateIdentity(clubId, playerId);
        return cache.get(cacheKey(clubId,playerId),ignored->findOrigin(clubId,playerId));
    }
    private Optional<ClubMemberRecord> findOrigin(long clubId,long playerId) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "SELECT member_status,member_role,online,profile_payload,joined_at,updated_at,row_version FROM aoo_club_member WHERE club_id=? AND player_id=?")) {
            statement.setLong(1, clubId); statement.setLong(2, playerId);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(read(rows, clubId, playerId)) : Optional.empty();
            }
        } catch (Exception error) { throw new IllegalStateException("cannot find club member", error); }
    }

    @Override public CursorPage<ClubMemberRecord> page(long clubId, String cursor, int limit) {
        if (clubId <= 0 || limit < 1 || limit > 200) throw new IllegalArgumentException("invalid member page");
        long after = cursor(cursor);
        String sql = "SELECT player_id,member_status,member_role,online,profile_payload,joined_at,updated_at,row_version "
                + "FROM aoo_club_member WHERE club_id=? AND member_status='ACTIVE' AND player_id>? ORDER BY player_id LIMIT ?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clubId); statement.setLong(2, after); statement.setInt(3, limit + 1);
            List<ClubMemberRecord> members = new ArrayList<>();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) members.add(read(rows, clubId, rows.getLong(1), 2));
            }
            boolean hasMore = members.size() > limit;
            if (hasMore) members.remove(members.size() - 1);
            String next = hasMore ? Long.toUnsignedString(members.get(members.size() - 1).playerId()) : null;
            return new CursorPage<>(members, next, hasMore);
        } catch (Exception error) { throw new IllegalStateException("cannot page club members", error); }
    }

    @Override public List<Long> onlinePlayerIds(long clubId, MemberAudience audience, int batchSize, String cursor) {
        if (clubId <= 0 || audience == null || batchSize < 1 || batchSize > 1000)
            throw new IllegalArgumentException("invalid online member page");
        long after = cursor(cursor);
        String role = audience == MemberAudience.MANAGERS ? " AND member_role IN ('MANAGER','OWNER')" : "";
        String sql = "SELECT player_id FROM aoo_club_member WHERE club_id=? AND member_status='ACTIVE' "
                + "AND online=1 AND player_id>?" + role + " ORDER BY player_id LIMIT ?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clubId); statement.setLong(2, after); statement.setInt(3, batchSize);
            List<Long> players = new ArrayList<>();
            try (var rows = statement.executeQuery()) { while (rows.next()) players.add(rows.getLong(1)); }
            return List.copyOf(players);
        } catch (Exception error) { throw new IllegalStateException("cannot page online club members", error); }
    }

    @Override public void upsert(long clubId, long playerId, ClubMemberRecord member) {
        validateIdentity(clubId, playerId);
        Objects.requireNonNull(member, "member");
        if (member.clubId() != clubId || member.playerId() != playerId)
            throw new IllegalArgumentException("club member identity mismatch");
        transactions.execute(clubId,connection->{String insert="INSERT IGNORE INTO aoo_club_member(club_id,player_id,member_status,member_role,online,profile_payload,joined_at,updated_at,row_version) VALUES(?,?,?,?,?,?,?,?,1)";try(var statement=connection.prepareStatement(insert)){bind(statement,clubId,playerId,member);if(statement.executeUpdate()==1)return null;}if(member.revision()<=0)throw new IllegalStateException("existing member revision is required");String update="UPDATE aoo_club_member SET member_status=?,member_role=?,online=?,profile_payload=?,updated_at=?,row_version=row_version+1 WHERE club_id=? AND player_id=? AND row_version=?";try(var statement=connection.prepareStatement(update)){statement.setString(1,member.status().name());statement.setString(2,member.role().name());statement.setBoolean(3,member.online());statement.setString(4,mapper.writeValueAsString(member.profile()));statement.setTimestamp(5,Timestamp.from(clock.instant()));statement.setLong(6,clubId);statement.setLong(7,playerId);statement.setLong(8,member.revision());if(statement.executeUpdate()!=1)throw new IllegalStateException("club member concurrent modification");}return null;});cache.invalidate(cacheKey(clubId,playerId));
    }

    @Override public void remove(long clubId, long playerId) {
        validateIdentity(clubId, playerId);
        ClubMemberRecord member=find(clubId,playerId).orElseThrow(()->new IllegalStateException("club member missing"));remove(clubId,playerId,member.revision());
    }
    public void remove(long clubId,long playerId,long expectedRevision){validateIdentity(clubId,playerId);if(expectedRevision<=0)throw new IllegalArgumentException("expected revision required");transactions.execute(clubId,connection->{try(var statement=connection.prepareStatement("UPDATE aoo_club_member SET member_status='LEFT',online=0,updated_at=CURRENT_TIMESTAMP(3),row_version=row_version+1 WHERE club_id=? AND player_id=? AND row_version=?")){statement.setLong(1,clubId);statement.setLong(2,playerId);statement.setLong(3,expectedRevision);if(statement.executeUpdate()!=1)throw new IllegalStateException("club member concurrent modification");}return null;});cache.invalidate(cacheKey(clubId,playerId));}
    private void bind(java.sql.PreparedStatement statement,long clubId,long playerId,ClubMemberRecord member)throws Exception{statement.setLong(1,clubId);statement.setLong(2,playerId);statement.setString(3,member.status().name());statement.setString(4,member.role().name());statement.setBoolean(5,member.online());statement.setString(6,mapper.writeValueAsString(member.profile()));statement.setTimestamp(7,Timestamp.from(member.joinedAt()));statement.setTimestamp(8,Timestamp.from(clock.instant()));}

    private ClubMemberRecord read(java.sql.ResultSet rows, long clubId, long playerId) throws Exception {
        return read(rows, clubId, playerId, 1);
    }
    private ClubMemberRecord read(java.sql.ResultSet rows, long clubId, long playerId, int offset) throws Exception {
        return new ClubMemberRecord(clubId, playerId,
                com.aoo.bcg.gamespi.StrictEnumDecoder.byName(ClubMemberRecord.Status.class, rows.getString(offset)),
                com.aoo.bcg.gamespi.StrictEnumDecoder.byName(ClubMemberRecord.Role.class, rows.getString(offset + 1)), rows.getBoolean(offset + 2),
                mapper.readValue(rows.getString(offset + 3), PROFILE_TYPE), rows.getTimestamp(offset + 4).toInstant(),
                rows.getTimestamp(offset + 5).toInstant(), rows.getLong(offset + 6));
    }
    private static long cursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0;
        try { long value = Long.parseUnsignedLong(cursor); if (value < 0) throw new NumberFormatException(); return value; }
        catch (NumberFormatException error) { throw new IllegalArgumentException("invalid member cursor", error); }
    }
    private static void validateIdentity(long clubId, long playerId) {
        if (clubId <= 0 || playerId <= 0) throw new IllegalArgumentException("invalid club member identity");
    }
    private String cacheKey(long clubId,long playerId){return cacheShards.key(new com.aoo.bcg.common.cache.CacheKey("prod","club","member",clubId+"-"+playerId,1));}
}
