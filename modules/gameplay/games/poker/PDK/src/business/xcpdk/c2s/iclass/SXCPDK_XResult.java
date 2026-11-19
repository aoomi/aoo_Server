package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SXCPDK_XResult extends BaseSendMsg {
    //用户信息
    private Map<Integer, SXCPDK_UserInfo> userInfo = new HashMap<>();
    //局数信息
    private List<SXCPDK_SetInfo> setInfo = new ArrayList<>();

    public List<SXCPDK_SetInfo> getSetInfo() {
        return setInfo;
    }

    public Map<Integer, SXCPDK_UserInfo> getUserInfo() {
        return userInfo;
    }

}
