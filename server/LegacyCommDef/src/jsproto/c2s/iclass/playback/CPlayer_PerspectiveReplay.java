package jsproto.c2s.iclass.playback;

import jsproto.c2s.cclass.BaseSendMsg;

public class CPlayer_PerspectiveReplay extends BaseSendMsg {
    public long roomId;
    public int setId;
    public long afterSequence;
    public int limit = 200;
}
