package business.global.mj.scjymj;

import business.scjymj.c2s.iclass.CSCJYMJ_CreateRoom;
import cenum.PrizeType;
import com.aoo.bcg.gamespi.RoomCreationContext;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SCJYMJGameProviderTest {
    private final SCJYMJGameProvider provider = new SCJYMJGameProvider();

    @Test void bindsExecutableStandardFamily() {
        assertEquals("mahjong-standard", provider.mahjongFamily().familyCode());
        provider.validateMahjongCategory();
        var authority=provider.createAuthoritativeSession(new RoomCreationContext(1,2,Map.of())).orElseThrow();
        assertEquals(provider.descriptor().version(),authority.authoritativeState().get("playVersion"));
        assertEquals(136,authority.authoritativeState().get("tileSetSize"));
    }

    @Test void rejectsMissingConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> provider.roomFactory().create(new RoomCreationContext(1, 2, Map.of())));
    }

    @Test void rejectsConfigurationForAnotherGame() {
        BaseRoomConfigure<CSCJYMJ_CreateRoom> configuration = new BaseRoomConfigure<>(
                PrizeType.None, new GameType(999, "wrong", 1), new CSCJYMJ_CreateRoom());
        assertThrows(IllegalArgumentException.class, () -> provider.roomFactory().create(
                new RoomCreationContext(1, 2, Map.of("baseRoomConfigure", configuration))));
    }
}
