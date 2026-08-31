package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.NoticeInfo;

import java.util.List;


public class S2226_InitNotice extends BaseSendMsg {

    public List<NoticeInfo> noticeInfoList;


    public static S2226_InitNotice make(List<NoticeInfo> noticeInfoList) {
        S2226_InitNotice ret = new S2226_InitNotice();
        ret.noticeInfoList = noticeInfoList;

        return ret;


    }
}