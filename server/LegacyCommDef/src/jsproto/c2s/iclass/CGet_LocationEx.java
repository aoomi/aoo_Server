package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class CGet_LocationEx extends BaseSendMsg {
    public double Latitude; //纬度
    public double Longitude; //精度
    public boolean isGetError;//获取是否失败
    public String Address = "";//地址

    public static CGet_LocationEx make(double Latitude, double Longitude, boolean isGetError, String Address) {
        CGet_LocationEx ret = new CGet_LocationEx();
        ret.Latitude = Latitude;
        ret.Longitude = Longitude;
        ret.isGetError = isGetError;
        ret.Address = Address;
        return ret;
    }
}
