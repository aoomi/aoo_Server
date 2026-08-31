package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SPlayerHeartBeat extends BaseSendMsg {

    public String strBeatMessage;    //心跳信息
    public long lBeatTime;        //心跳时间
    public boolean bBeatFlag; //心跳标志

    public static SPlayerHeartBeat make(String StrBeatMessage, Long playerID, int time, boolean beatFlag) {
        SPlayerHeartBeat ret = new SPlayerHeartBeat();
        ret.strBeatMessage = StrBeatMessage;
        ret.lBeatTime = time;
        ret.bBeatFlag = beatFlag;
        return ret;
    }
}
