package jsproto.c2s.iclass.zjh;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.zjh.ZJHRoom_Record;


public class SZJH_RoomEnd extends BaseSendMsg {
    
    public ZJHRoom_Record record;
    //public List<NNRoom_SetEnd> records;

    public static SZJH_RoomEnd make(ZJHRoom_Record record/*, List<NNRoom_SetEnd> records*/) {
        SZJH_RoomEnd ret = new SZJH_RoomEnd();
        ret.record = record;
        //ret.records = records;
        return ret;
    

    }
}