package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.LocationInfo;

import java.util.List;

public class SGet_LocationEx extends BaseSendMsg {
    public List<LocationInfo> locationInfos;

    public static SGet_LocationEx make(List<LocationInfo> locationInfos) {
        SGet_LocationEx ret = new SGet_LocationEx();
        ret.locationInfos = locationInfos;
        return ret;
    }
}
