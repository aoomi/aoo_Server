package jsproto.c2s.iclass.email;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.email.EMail_ChangeStatus;

import java.util.ArrayList;

/**
 * 改变邮件状态
 *
 * @author zaf
 */
public class CEMail_ChangeStatus extends BaseSendMsg {

    public ArrayList<EMail_ChangeStatus> changelist;

    public static CEMail_ChangeStatus make(ArrayList<EMail_ChangeStatus> changelist) {
        CEMail_ChangeStatus ret = new CEMail_ChangeStatus();
        ret.changelist = changelist;
        return ret;
    }
}