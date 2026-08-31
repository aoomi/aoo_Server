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
    private static final class NeverConnect implements DataSource{
        public Connection getConnection()throws SQLException{throw new AssertionError("database must not be reached");}public Connection getConnection(String u,String p)throws SQLException{return getConnection();}
        public PrintWriter getLogWriter(){return null;}public void setLogWriter(PrintWriter out){}public void setLoginTimeout(int seconds){}public int getLoginTimeout(){return 0;}public Logger getParentLogger(){return Logger.getGlobal();}public <T>T unwrap(Class<T> iface)throws SQLException{throw new SQLException();}public boolean isWrapperFor(Class<?> iface){return false;}
    }
}
