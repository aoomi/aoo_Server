package com.aoo.bcg.hall.room;

import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import com.aoo.bcg.lobby.JdbcLobbyRepository;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Hall compatibility facade. Lobby owns the repository implementation; this
 * type preserves the frozen source/binary entry point without duplicate SQL,
 * authority, recovery, or rule behavior.
 */
public class JdbcHallRepository extends JdbcLobbyRepository {
    static final String FILTER_ACTIVE_INDEX_JOIN = JdbcLobbyRepository.FILTER_ACTIVE_INDEX_JOIN;
    static final String CATALOG_ACTIVE_INDEX_JOIN = JdbcLobbyRepository.CATALOG_ACTIVE_INDEX_JOIN;

    public JdbcHallRepository(DataSource dataSource, ObjectMapper json, Clock clock,
            RoomMembershipAuthorityPort membershipAuthority) {
        super(dataSource, json, clock, membershipAuthority);
    }

    public JdbcHallRepository(DataSource dataSource, ObjectMapper json, Clock clock,
            RoomMembershipAuthorityPort membershipAuthority,
            RuntimeMetadataProjection runtimeMetadataProjection) {
        super(dataSource, json, clock, membershipAuthority, runtimeMetadataProjection);
    }

    /**
     * Binary/source compatibility forwarding methods for the frozen Hall
     * entry point.  The implementation remains exclusively in Lobby; these
     * methods intentionally contain no SQL or authority logic.
     */
    @Override
    public Map<String, Object> activeRoom(long account) {
        return super.activeRoom(account);
    }

    @Override
    public Map<String, Object> closeRoom(long room, String key, String reason, String traceId) {
        return super.closeRoom(room, key, reason, traceId);
    }

    @Override
    public Map<String, Object> leave(long account, String key, long room, String traceId,
            Map<String, Object> suppliedIdentity) {
        return super.leave(account, key, room, traceId, suppliedIdentity);
    }

    public static String rejectCode(String state, String authority, Map<String,Object> snapshot) {
        return JdbcLobbyRepository.rejectCode(state, authority, snapshot);
    }
    static boolean authorityConfirmsAlreadyLeft(RoomAuthorityRejected rejected) {
        return JdbcLobbyRepository.authorityConfirmsAlreadyLeft(rejected);
    }
    public static Map<String,Object> mergeClubTemplatePolicy(Map<String,Object> gameplay, Map<String,Object> submitted) {
        return JdbcLobbyRepository.mergeClubTemplatePolicy(gameplay, submitted);
    }
    public static String resolveActiveRegion(List<String> regions) { return JdbcLobbyRepository.resolveActiveRegion(regions); }
    public static Map<String,Object> settlementUi(String familyCode, Object raw) { return JdbcLobbyRepository.settlementUi(familyCode, raw); }
    public static int compareVersions(String left, String right) { return JdbcLobbyRepository.compareVersions(left, right); }
    public static int firstVacantSeat(Collection<Integer> occupied) { return JdbcLobbyRepository.firstVacantSeat(occupied); }
}
