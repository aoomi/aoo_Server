package jsproto.c2s.iclass.email;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.email.EMail_Info;

/**
 * 新邮件
 *
 * @author zaf
 */
public class SEMail_NewMail extends BaseSendMsg {

    public EMail_Info newEMail;

    public static SEMail_NewMail make(EMail_Info newEMail) {
        SEMail_NewMail ret = new SEMail_NewMail();
        ret.newEMail = newEMail;
        return ret;
    }
}