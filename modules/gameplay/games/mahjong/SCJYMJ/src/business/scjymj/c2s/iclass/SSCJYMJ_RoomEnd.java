package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.RoomEndResult;


public class SSCJYMJ_RoomEnd<T> extends BaseSendMsg {

    public T record;
    public RoomEndResult sRoomEndResult;

    public static <T> SSCJYMJ_RoomEnd make(T record, RoomEndResult sRoomEndResult) {
        SSCJYMJ_RoomEnd ret = new SSCJYMJ_RoomEnd();
        ret.record = record;
        ret.sRoomEndResult = sRoomEndResult;
        return ret;


    }
}
