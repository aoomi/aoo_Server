package com.aoo.bcg.hall;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class HallSessionCoordinatorTest {
 @Test void lateLoginIsDiscardedAndStateIsAccountScoped(){var c=new HallSessionCoordinator();var old=c.beginLogin(1);var current=c.beginLogin(2);assertFalse(c.accept(old));assertTrue(c.accept(current));c.put("room","two");var back=c.beginLogin(1);assertTrue(c.accept(back));assertTrue(c.get("room").isEmpty());var again=c.beginLogin(2);assertTrue(c.accept(again));assertEquals("two",c.get("room").orElseThrow());c.logout();assertTrue(c.get("room").isEmpty());assertTrue(c.accept(c.beginLogin(2)));assertTrue(c.get("room").isEmpty());}
 @Test void startupOrderIsExplicit(){var c=new HallSessionCoordinator();var p=HallSessionCoordinator.StartupPhase.CONFIG;p=c.next(p);assertEquals(HallSessionCoordinator.StartupPhase.REFRESH_TOKEN,p);p=c.next(p);assertEquals(HallSessionCoordinator.StartupPhase.RESTORE_ROOM,p);p=c.next(p);assertEquals(HallSessionCoordinator.StartupPhase.LOAD_HALL,p);assertEquals(HallSessionCoordinator.StartupPhase.READY,c.next(p));}
}
