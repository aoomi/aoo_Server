package jsproto.c2s.iclass.email;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.email.EMail_Info;

import java.util.ArrayList;

/**
 * 邮件列表
 *
 * @author zaf
 */
public class SEMail_GetList extends BaseSendMsg {

    public ArrayList<EMail_Info> eMailList;

    public static SEMail_GetList make(ArrayList<EMail_Info> eMailList) {
        SEMail_GetList ret = new SEMail_GetList();
        ret.eMailList = eMailList;
        return ret;
    }
}