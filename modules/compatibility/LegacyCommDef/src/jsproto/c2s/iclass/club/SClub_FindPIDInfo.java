package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;

public class SClub_FindPIDInfo {
    private ShortPlayer player;
    private int state;

    public static SClub_FindPIDInfo make(ShortPlayer player, int state) {
        SClub_FindPIDInfo ret = new SClub_FindPIDInfo();
        ret.player = player;
        ret.state = state;
        return ret;
    }

}
