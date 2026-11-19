package jsproto.c2s.iclass.email;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.email.EMail_ChangeStatus;

import java.util.ArrayList;

/**
 * 改变邮件状态 回复
 *
 * @author zaf
 */
public class SEMail_ChangeStatus extends BaseSendMsg {

    public ArrayList<EMail_ChangeStatus> changelist;

    public static SEMail_ChangeStatus make(ArrayList<EMail_ChangeStatus> changelist) {
        SEMail_ChangeStatus ret = new SEMail_ChangeStatus();
        ret.changelist = changelist;
        return ret;
    }
}