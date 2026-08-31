package business.xcpdk.c2s.iclass;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pk.PKRoom_Record;


@SuppressWarnings("serial")
public class SXCPDK_RoomEnd extends BaseSendMsg {

    public PKRoom_Record record;

    public static SXCPDK_RoomEnd make(PKRoom_Record record) {
        SXCPDK_RoomEnd ret = new SXCPDK_RoomEnd();
        ret.record = record;
        //ret.records = records;
        return ret;


    }
}
