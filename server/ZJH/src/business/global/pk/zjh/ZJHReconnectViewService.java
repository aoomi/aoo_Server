package business.global.pk.zjh;

import java.util.Map;

public final class ZJHReconnectViewService {
    public Map<String, Object> build(ZJHTable table, long viewerPlayerId) {
        if (viewerPlayerId <= 0) throw new IllegalArgumentException("viewerPlayerId must be positive");
        return table.viewFor(viewerPlayerId);
    }
}
