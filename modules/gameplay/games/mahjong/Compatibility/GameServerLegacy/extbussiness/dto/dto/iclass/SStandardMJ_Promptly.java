package business.global.mj.extbussiness.dto.iclass;

import business.global.mj.extbussiness.dto.StandardMJPlayerPosInfo;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pos.PlayerPosInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("serial")
public class SStandardMJ_Promptly extends BaseSendMsg {
    public long roomID;
    public List<? extends StandardMJPlayerPosInfo> playerPosInfoList = new ArrayList<>();

    public static SStandardMJ_Promptly make(long roomID, List<? extends StandardMJPlayerPosInfo> playerPosInfoList, String gameNameStr) {
        SStandardMJ_Promptly ret = new SStandardMJ_Promptly();
        ret.roomID = roomID;
        ret.playerPosInfoList = playerPosInfoList;
        ret.setGameNameStr(gameNameStr);
        return ret;
    }

    public String getOpName() {
        if (Objects.isNull(getGameNameStr())) {
            return this.getClass().getSimpleName();
        } else {
            return String.format("S%s%s", getGameNameStr(), this.getClass().getSimpleName().replace( "SStandardMJ",""));
        }
    }
}		
