package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;


public class SNJPDK_RoomRecord<T> extends BaseSendMsg {

    public List<T> records;

    public static <T> SNJPDK_RoomRecord make(List<T> records) {
        SNJPDK_RoomRecord ret = new SNJPDK_RoomRecord();
        ret.records = records;

        return ret;


    }
}
