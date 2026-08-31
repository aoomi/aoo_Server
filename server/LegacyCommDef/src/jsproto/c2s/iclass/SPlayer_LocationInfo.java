package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.LocationInfo;

public class SPlayer_LocationInfo extends BaseSendMsg {
    public LocationInfo locationInfo;

    public static SPlayer_LocationInfo make(LocationInfo locationInfo) {
        SPlayer_LocationInfo ret = new SPlayer_LocationInfo();
        ret.locationInfo = locationInfo;
        return ret;
    }
}
