package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pk.PKRoom_Record;


public class SNJPDK_RoomEnd extends BaseSendMsg {

    public PKRoom_Record record;
    //public List<NNRoom_SetEnd> records;

    public static SNJPDK_RoomEnd make(PKRoom_Record record/*, List<NNRoom_SetEnd> records*/) {
        SNJPDK_RoomEnd ret = new SNJPDK_RoomEnd();
        ret.record = record;
        //ret.records = records;
        return ret;


    }
}
