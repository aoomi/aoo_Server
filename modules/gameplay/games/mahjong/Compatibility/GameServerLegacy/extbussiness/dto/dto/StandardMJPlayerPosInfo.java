package business.global.mj.extbussiness.dto;

import jsproto.c2s.cclass.pos.PlayerPosInfo;

public class StandardMJPlayerPosInfo extends PlayerPosInfo {
    public int actualTimePoint;

    public StandardMJPlayerPosInfo(PlayerPosInfo playerPosInfo, int gangPoint) {
        this.pid = playerPosInfo.pid;
        this.point = playerPosInfo.point;
        this.posID = playerPosInfo.posID;
        this.sportsPoint = playerPosInfo.sportsPoint;
        this.actualTimePoint = gangPoint;
    }

}
