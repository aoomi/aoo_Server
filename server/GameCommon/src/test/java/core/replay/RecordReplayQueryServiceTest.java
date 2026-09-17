package core.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecordReplayQueryServiceTest {
    private final RecordReplayQueryService service=new RecordReplayQueryService(new NeverConnect(),new ObjectMapper());
    @Test void rejectsUnauthenticatedAndUnboundedQueriesBeforeDatabaseAccess(){
        assertThrows(SecurityException.class,()->service.history(0,0,20));
        assertThrows(IllegalArgumentException.class,()->service.history(1,0,101));
        assertThrows(IllegalArgumentException.class,()->service.replay(1,2,0,0,501));
        assertThrows(IllegalArgumentException.class,()->service.replay(1,2,0,-1,1));
    }
    @Test void historyOnlyIncludesRoomsWithAFormalSettlement(){
        assertTrue(RecordReplayQueryService.HISTORY_SQL.contains("JOIN aoo_settlement"));
        assertFalse(RecordReplayQueryService.HISTORY_SQL.contains("LEFT JOIN aoo_settlement"));
        assertTrue(RecordReplayQueryService.HISTORY_COUNT_SQL.contains("EXISTS (SELECT 1 FROM aoo_settlement"));
        assertTrue(RecordReplayQueryService.HISTORY_SQL.contains("JOIN aoo_hall_room h"));
        assertTrue(RecordReplayQueryService.HISTORY_SQL.contains("h.club_id=?"));
        assertTrue(RecordReplayQueryService.HISTORY_COUNT_SQL.contains("COALESCE(h.club_id,0)=0"));
        assertTrue(RecordReplayQueryService.BIG_WINNER_SQL.contains("JOIN aoo_settlement"));
        assertTrue(RecordReplayQueryService.BIG_WINNER_SQL.contains("h.club_id=?"));
        assertArrayEquals(new String[]{"items","nextBeforeRoomId","hasMore","totalCount","bigWinnerCount"},
                java.util.Arrays.stream(RecordReplayQueryService.HistoryPage.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName).toArray(String[]::new));
    }
    @Test void rejectsInvalidClubScopeBeforeDatabaseAccess(){
        assertThrows(IllegalArgumentException.class,()->service.history(1,0,20,0,0,-1));
    }
    @Test void historyDetailCarriesCatalogIdentityForGameplaySettlementResolution(){
        var fields=RecordReplayQueryService.HistoryDetail.class.getRecordComponents();
        assertArrayEquals(new String[]{"roomId","gameCode","playFamily","smallSettleTemplate",
                "rounds","ruleSnapshot","ruleFields"},java.util.Arrays.stream(fields).map(java.lang.reflect.RecordComponent::getName).toArray(String[]::new));
    }
    private static final class NeverConnect implements DataSource{
        public Connection getConnection()throws SQLException{throw new AssertionError("database must not be reached");}public Connection getConnection(String u,String p)throws SQLException{return getConnection();}
        public PrintWriter getLogWriter(){return null;}public void setLogWriter(PrintWriter out){}public void setLoginTimeout(int seconds){}public int getLoginTimeout(){return 0;}public Logger getParentLogger(){return Logger.getGlobal();}public <T>T unwrap(Class<T> iface)throws SQLException{throw new SQLException();}public boolean isWrapperFor(Class<?> iface){return false;}
    }
}
