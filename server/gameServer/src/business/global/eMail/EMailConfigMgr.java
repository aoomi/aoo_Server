package business.global.eMail;

import com.ddm.server.common.utils.Txt2Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 邮件 配置文件
 *
 * @author zaf
 */
public class EMailConfigMgr {
    public static final String fileName = "EMailConfig.txt";
    public static final String filePath = "conf/";
    private Map<String, String> configMap = new HashMap<String, String>();


    public String clubRewardTitle; //俱乐部邮件标题
    public String clubJoinSucessTitle; //俱乐部邮件标题
    public String clubJoinLoseTitle; //俱乐部邮件标题
    public String clubKickOutTitle; //俱乐部邮件标题
    public String clubSender; //俱乐部邮件发送者
    public String clubRewardMsgInfo; //俱乐部邮件内容
    public String clubJoinSucessMsgInfo; //俱乐部邮件内容
    public String clubJoinLoseMsgInfo; //俱乐部邮件内容
    public String clubKickOutMsgInfo; //俱乐部邮件内容
    public String arenaTitle;//比赛场邮件标题
    public String arenaSender;//比赛场邮件发送者
    public String exclusiveTitle;//比赛场专属场邮件标题
    public String exclusiveMsgInfo;//比赛场专属邮件内容
    public String arenaGainTitle;//比赛场退还标题
    public String arenaGainMsgInfo;//比赛场退还内容


    public String arenaMsgInfo;//比赛场邮件内容
    public String arenaWishing;//比赛场红包祝福语
    public String arenaSendName;//比赛场红包发送商户名称

    public String activityTitle;// 活动邮件标题
    public String activitySender;//活动邮件发送者
    public String activityMsgInfo;// 活动邮件内容

    public EMailConfigMgr() {
        this.configMap = Txt2Utils.txt2Map(filePath, fileName, "UTF-8");
        this.clubRewardTitle = this.configMap.get("clubRewardTitle");
        this.clubJoinSucessTitle = this.configMap.get("clubJoinSucessTitle");
        this.clubJoinLoseTitle = this.configMap.get("clubJoinLoseTitle");
        this.clubKickOutTitle = this.configMap.get("clubKickOutTitle");
        this.clubSender = this.configMap.get("clubSender");
        this.clubRewardMsgInfo = this.configMap.get("clubRewardMsgInfo");
        this.clubJoinSucessMsgInfo = this.configMap.get("clubJoinSucessMsgInfo");
        this.clubJoinLoseMsgInfo = this.configMap.get("clubJoinLoseMsgInfo");
        this.clubKickOutMsgInfo = this.configMap.get("clubKickOutMsgInfo");
        this.arenaTitle = this.configMap.get("arenaTitle");
        this.arenaSender = this.configMap.get("arenaSender");
        this.arenaMsgInfo = this.configMap.get("arenaMsgInfo");
        this.exclusiveTitle = this.configMap.get("exclusiveTitle");
        this.exclusiveMsgInfo = this.configMap.get("exclusiveMsgInfo");

        this.arenaGainTitle = this.configMap.get("arenaGainTitle");
        this.arenaGainMsgInfo = this.configMap.get("arenaGainMsgInfo");

        this.arenaWishing = this.configMap.get("arenaWishing");
        this.arenaSendName = this.configMap.get("arenaSendName");

        this.activityTitle = this.configMap.get("activityTitle");
        this.activitySender = this.configMap.get("activitySender");
        this.activityMsgInfo = this.configMap.get("activityMsgInfo");

    }

    /**
     * @return clubSender
     */
    public String getClubSender() {
        return clubSender;
    }

    /**
     * @return clubRewardTitle
     */
    public String getClubRewardTitle() {
        return clubRewardTitle;
    }

    /**
     * @return clubJoinSucessTitle
     */
    public String getClubJoinSucessTitle() {
        return clubJoinSucessTitle;
    }

    /**
     * @return clubJoinLoseTitle
     */
    public String getClubJoinLoseTitle() {
        return clubJoinLoseTitle;
    }

    /**
     * @return clubRewardMsgInfo
     */
    public String getClubRewardMsgInfo() {
        return clubRewardMsgInfo;
    }

    /**
     * @return clubJoinSucessMsgInfo
     */
    public String getClubJoinSucessMsgInfo() {
        return clubJoinSucessMsgInfo;
    }

    /**
     * @return clubJoinLoseMsgInfo
     */
    public String getClubJoinLoseMsgInfo() {
        return clubJoinLoseMsgInfo;
    }

    /**
     * @return clubKickOutTitle
     */
    public String getClubKickOutTitle() {
        return clubKickOutTitle;
    }

    /**
     * @return clubKickOutMsgInfo
     */
    public String getClubKickOutMsgInfo() {
        return clubKickOutMsgInfo;
    }

    /**
     * @return arenaTitle
     */
    public String getArenaTitle() {
        return arenaTitle;
    }

    /**
     * @returnarenaSender
     */
    public String getArenaSender() {
        return arenaSender;
    }

    /**
     * @return arenaMsgInfo
     */
    public String getArenaMsgInfo() {
        return arenaMsgInfo;
    }

    /**
     * @return arenaWishing
     */
    public String getArenaWishing() {
        return arenaWishing;
    }

    /**
     * @return arenaSendName
     */
    public String getArenaSendName() {
        return arenaSendName;
    }

    /**
     * @return activityTitle
     */
    public String getActivityTitle() {
        return activityTitle;
    }

    /**
     * @return activitySender
     */
    public String getActivitySender() {
        return activitySender;
    }


    /**
     * @return activityMsgInfo
     */
    public String getActivityMsgInfo() {
        return activityMsgInfo;
    }

    public String getExclusiveTitle() {
        return exclusiveTitle;
    }

    public String getExclusiveMsgInfo() {
        return exclusiveMsgInfo;
    }

    public String getArenaGainTitle() {
        return arenaGainTitle;
    }

    public String getArenaGainMsgInfo() {
        return arenaGainMsgInfo;
    }


}
