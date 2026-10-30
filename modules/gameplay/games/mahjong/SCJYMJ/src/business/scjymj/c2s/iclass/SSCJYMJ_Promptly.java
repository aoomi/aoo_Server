package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pos.PlayerPosInfo;

import java.util.ArrayList;
import java.util.List;

public class SSCJYMJ_Promptly extends BaseSendMsg {
    public long roomID;
    public List<PlayerPosInfo> playerPosInfoList = new ArrayList<>();

    public static SSCJYMJ_Promptly make(long roomID, List<PlayerPosInfo> playerPosInfoList) {
        SSCJYMJ_Promptly ret = new SSCJYMJ_Promptly();
        ret.roomID = roomID;
        ret.playerPosInfoList = playerPosInfoList;
        return ret;

    }
}
