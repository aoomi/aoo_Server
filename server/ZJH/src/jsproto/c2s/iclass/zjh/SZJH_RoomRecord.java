package jsproto.c2s.iclass.zjh;
import java.util.List;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.zjh.ZJHRoom_SetEnd;


public class SZJH_RoomRecord extends BaseSendMsg {
    
    public List<ZJHRoom_SetEnd> records;


    public static SZJH_RoomRecord make(List<ZJHRoom_SetEnd> records) {
        SZJH_RoomRecord ret = new SZJH_RoomRecord();
        ret.records = records;

        return ret;
    

    }
}