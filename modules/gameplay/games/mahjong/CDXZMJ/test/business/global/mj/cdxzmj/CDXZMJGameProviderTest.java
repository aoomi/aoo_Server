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

        assertEquals(1001, provider.descriptor().gameId());
        assertEquals(GameCategory.MAHJONG, provider.descriptor().category());
        assertEquals("chengdu1001", provider.descriptor().code());
        assertEquals("chengdu1001-v1.0.0", provider.descriptor().version());
        assertNotNull(provider.roomFactory());
        assertEquals("mahjong:xue-zhan", provider.descriptor().family());
        assertEquals("mahjong-xue-zhan", provider.mahjongFamily().familyCode());
        provider.validateMahjongCategory();
        var authority=provider.createAuthoritativeSession(new RoomCreationContext(1,2,Map.of())).orElseThrow();
        assertEquals(1001,authority.authoritativeState().get("gameId"));
    }
}
