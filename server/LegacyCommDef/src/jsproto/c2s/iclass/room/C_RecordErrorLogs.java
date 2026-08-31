package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 莆田麻将 接收客户端数据 创建房间
 *
 * @author Huaxing
 */
@SuppressWarnings("serial")
public class C_RecordErrorLogs extends BaseSendMsg {

    public long roomID;//房间ID
    public String context;


    public static C_RecordErrorLogs make(long roomID, String context) {
        C_RecordErrorLogs ret = new C_RecordErrorLogs();
        ret.roomID = roomID;
        ret.context = context;
        return ret;
    }
}