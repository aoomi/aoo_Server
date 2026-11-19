package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;


public class S1010_SystemMessage extends BaseSendMsg {

    public String key;
    public List<String> p;


    public static S1010_SystemMessage make(String key, List<String> p) {
        S1010_SystemMessage ret = new S1010_SystemMessage();
        ret.key = key;
        ret.p = p;

        return ret;


    }
}