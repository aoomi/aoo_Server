package jsproto.c2s.cclass.email;

/**
 * 邮件
 *
 * @author zaf
 **/
public class EMail_define {

    public static enum EMail_Status {
        EMAIL_STATUS_NOMARL(0x00), //默认状态
        EMAIL_STATUS_READ(0x01), //已读
        EMAIL_STATUS_GETREWARD(0x02), //领取奖励
        EMAIL_STATUS_DELETE(0x04), //删除
        ;

        private int value;

        private EMail_Status(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static EMail_Status getStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (EMail_Status flow : EMail_Status.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return EMail_Status.EMAIL_STATUS_NOMARL;
        }

        public static EMail_Status valueOf(int value) {
            for (EMail_Status flow : EMail_Status.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return EMail_Status.EMAIL_STATUS_NOMARL;
        }
    }

    ;


    public static enum EMail_Attachment_Status {
        EMAIL_ATTACHMENT_STATUS_NOTANY(0), //没有附件
        EMAIL_ATTACHMENT_STATUS_HAVE(1), //有附件
        ;

        private int value;

        private EMail_Attachment_Status(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static EMail_Attachment_Status getStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (EMail_Attachment_Status flow : EMail_Attachment_Status.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return EMail_Attachment_Status.EMAIL_ATTACHMENT_STATUS_NOTANY;
        }

        public static EMail_Attachment_Status valueOf(int value) {
            for (EMail_Attachment_Status flow : EMail_Attachment_Status.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return EMail_Attachment_Status.EMAIL_ATTACHMENT_STATUS_NOTANY;
        }
    }

    ;
}
