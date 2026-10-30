package com.aoo.bcg.bootstrap;
import com.aoo.bcg.lobby.LobbyRoomCreateSaga;
import javax.sql.DataSource;
import java.time.Clock;
import java.util.function.LongSupplier;
/** Compatibility name only: the sole create billing lifecycle owns persistence and charges. */
final class JdbcRoomSagaBillingPort implements LobbyRoomCreateSaga.BillingPort {
 private final JdbcRoomBillingLifecyclePort lifecycle;
 JdbcRoomSagaBillingPort(DataSource source,LongSupplier ids,Clock clock){lifecycle=new JdbcRoomBillingLifecyclePort(source,ids,clock);}
 public void reserve(LobbyRoomCreateSaga.Command c){lifecycle.reserve(c);}
 public void confirm(LobbyRoomCreateSaga.Command c){lifecycle.confirm(c);}
 public void release(LobbyRoomCreateSaga.Command c){lifecycle.release(c);}
}
