package jsproto.c2s.cclass.email;

/**
 * 邮件具体信息
 *
 * @author zaf
 */
public class EMail_Info {
    public long emailID;            //邮件ID
    public long createTime;            //创建时间
    public String title;            //标题
    public String msgInfo;            //邮件信息
    public String sender;            //发送者
    public int status;                //状态
    public int isHaveAnyAttachment;    //是否有附件
    public String rewardString;        //奖励
}
