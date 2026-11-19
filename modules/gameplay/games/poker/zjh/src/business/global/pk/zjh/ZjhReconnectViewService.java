package business.global.pk.zjh;

import java.util.Map;

public final class ZjhReconnectViewService {
    public Map<String, Object> build(ZjhTable table, long viewerPlayerId) {
        if (viewerPlayerId <= 0) throw new IllegalArgumentException("viewerPlayerId must be positive");
        return table.viewFor(viewerPlayerId);
    }
}
