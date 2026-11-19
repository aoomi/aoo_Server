package com.aoo.bcg.gamespi;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommonRoomPublicViewTest {
    @Test void acceptsCompleteSeatedAndSpectatorViews() {
        var seats=List.of(new CommonRoomPublicSeat(0,true,11L,"A",null,8L),
                new CommonRoomPublicSeat(1,false,null,null,null,null));
        assertEquals(-1,new CommonRoomPublicView(2,-1,seats).localSeatId());
        assertEquals(2,new CommonRoomPublicView(2,0,seats).asMap().get("seatCount"));
    }

    @Test void vacantSeatCannotLeakIdentityOrScore() {
        assertThrows(IllegalArgumentException.class,()->new CommonRoomPublicSeat(0,false,11L,null,null,null));
        assertThrows(IllegalArgumentException.class,()->new CommonRoomPublicSeat(0,false,null,null,null,0L));
    }

    @Test void rejectsInvalidCountsObserverAndIncompleteOrDuplicateSeats() {
        var vacant=new CommonRoomPublicSeat(0,false,null,null,null,null);
        assertThrows(IllegalArgumentException.class,()->new CommonRoomPublicView(1,-1,List.of(vacant)));
        assertThrows(IllegalArgumentException.class,()->new CommonRoomPublicView(2,-2,List.of(vacant,vacant)));
        assertThrows(IllegalArgumentException.class,()->new CommonRoomPublicView(2,-1,List.of(vacant)));
        assertThrows(IllegalArgumentException.class,()->new CommonRoomPublicView(2,-1,List.of(vacant,vacant)));
    }
}
