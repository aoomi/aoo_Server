package jsproto.c2s.iclass.server;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CServer_PlayerKickOut extends BaseSendMsg {
    /**
     * 玩家pid
     */
    private long pid;

    public static CServer_PlayerKickOut make(long pid) {
        CServer_PlayerKickOut ret = new CServer_PlayerKickOut();
        ret.pid = pid;
        return ret;
    }
}
