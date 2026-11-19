package jsproto.c2s.cclass.mj.template;

import jsproto.c2s.cclass.pos.PlayerPosInfo;

public class MJTemplatePlayerPosInfo extends PlayerPosInfo {
    private int actualTimePoint;//实时扣分动画值

    public MJTemplatePlayerPosInfo(PlayerPosInfo playerPosInfo, int actualTimePoint) {
        this.pid = playerPosInfo.pid;
        this.point = playerPosInfo.point;
        this.posID = playerPosInfo.posID;
        this.sportsPoint = playerPosInfo.sportsPoint;
        this.actualTimePoint = actualTimePoint;
    }
}
