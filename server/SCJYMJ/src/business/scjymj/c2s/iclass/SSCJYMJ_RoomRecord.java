package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;


public class SSCJYMJ_RoomRecord<T> extends BaseSendMsg {

    public List<T> records;

    public static <T> SSCJYMJ_RoomRecord make(List<T> records) {
        SSCJYMJ_RoomRecord ret = new SSCJYMJ_RoomRecord();
        ret.records = records;

        return ret;


    }
}
