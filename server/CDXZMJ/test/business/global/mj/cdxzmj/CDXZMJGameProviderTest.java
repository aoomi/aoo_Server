package business.global.mj.cdxzmj;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.RoomCreationContext;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CDXZMJGameProviderTest {
    @Test
    void exposesMahjongDescriptorAndRoomFactory() {
        CDXZMJGameProvider provider = new CDXZMJGameProvider();

        assertEquals(GameCategory.MAHJONG, provider.descriptor().category());
        assertEquals("cdxzmj", provider.descriptor().code());
        assertNotNull(provider.roomFactory());
        assertEquals("mahjong-xue-zhan", provider.mahjongFamily().familyCode());
        provider.validateMahjongCategory();
        var authority=provider.createAuthoritativeSession(new RoomCreationContext(1,2,Map.of())).orElseThrow();
        assertEquals(provider.descriptor().version(),authority.authoritativeState().get("playVersion"));
        assertEquals(108,authority.authoritativeState().get("tileSetSize"));
    }
}
