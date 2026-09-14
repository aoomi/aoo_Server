package jsproto.c2s.iclass.popup;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

/**
 * 弹窗推送
 */
public class SPopUp_List<T> extends BaseSendMsg {
    public List<T> popList;

    public static <T> SPopUp_List make(List<T> popList) {
        SPopUp_List ret = new SPopUp_List();
        ret.popList = popList;
        return ret;
    }
}
