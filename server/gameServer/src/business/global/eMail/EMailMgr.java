package business.global.eMail;

import BaseTask.AsynTask.AsyncCallBackTaskBase;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerCurrency;
import cenum.ItemFlow;
import cenum.PrizeType;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import core.db.entity.clarkGame.EMailBO;
import core.db.other.AsyncInfo;
import core.db.service.clarkGame.EMailBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.RewardInfo;
import jsproto.c2s.cclass.email.EMail_Info;
import jsproto.c2s.cclass.email.EMail_define.EMail_Attachment_Status;
import jsproto.c2s.cclass.email.EMail_define.EMail_Status;
import jsproto.c2s.iclass.email.SEMail_NewMail;

import java.util.ArrayList;
import java.util.List;

/**
 * 邮件管理数据类
 *
 * @author zaf
 * @date 2018年2月22日
 */
public class EMailMgr {

    private static EMailMgr instance = new EMailMgr();
    protected EMailConfigMgr m_ConfigMgr;
    private EMailBOService eMailBOService;

    public EMailMgr() {
        m_ConfigMgr = new EMailConfigMgr();
        eMailBOService = ContainerMgr.get().getComponent(EMailBOService.class);
    }

    public static EMailMgr getInstance() {
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        eMailBOService.findAll(null, null, new AsyncInfo(new AsyncCallBackTaskBase() {
            @Override
            public void runSuc(Object o) {
                List<EMailBO> arg0 = (List<EMailBO>) o;
                CommLogD.error("邮件读取成功！ size = " + arg0.size());
                for (EMailBO eMailBO : arg0) {
                    //超过七天没有读取的邮件自动读取并删除(有附件时自动领取)
                    if ((CommTime.nowSecond() - eMailBO.getCreateTime() > CommTime.WeekSec) &&
                            ((eMailBO.getStatus() & EMail_Status.EMAIL_STATUS_DELETE.value()) <= 0)) {

                        if (((eMailBO.getStatus() & EMail_Status.EMAIL_STATUS_READ.value()) <= 0)) {
                            eMailBO.setStatus(eMailBO.getStatus() | EMail_Status.EMAIL_STATUS_READ.value());
                        }
                        if (eMailBO.getIsHaveAnyAttachment() == EMail_Attachment_Status.EMAIL_ATTACHMENT_STATUS_HAVE.value() &&
                                ((eMailBO.getStatus() & EMail_Status.EMAIL_STATUS_GETREWARD.value()) <= 0)) {
                            getReward(eMailBO.getRewardString(), eMailBO.getPlayerID());
                            eMailBO.setStatus(eMailBO.getStatus() | EMail_Status.EMAIL_STATUS_GETREWARD.value());
                        }
                        eMailBO.setStatus(eMailBO.getStatus() | EMail_Status.EMAIL_STATUS_DELETE.value());
                        eMailBO.setDeleteTime(CommTime.nowSecond());
                        eMailBO.getBaseService().saveOrUpDate(eMailBO, new AsyncInfo(eMailBO.getId()));
                    }
                    //超过一个月删除邮件
                    else if (((eMailBO.getStatus() & EMail_Status.EMAIL_STATUS_DELETE.value()) > 0) &&
                            (CommTime.nowSecond() - eMailBO.getDeleteTime() - CommTime.MonthSec > 0)) {
                        eMailBO.getBaseService().delete(eMailBO.getId(), new AsyncInfo(eMailBO.getId()));
                    }
                }
            }

            @Override
            public void runError() {
                CommLogD.error("邮件读取失败！ ");
            }
        }, 0, true));
    }

    /**
     * 获取奖励
     */
    public void getReward(String rewardString, long playerID) {
        Player player = PlayerMgr.getInstance().getPlayer(playerID);
        if (null == player) {
            return;
        }
        ArrayList<RewardInfo> rewards = new Gson().fromJson(rewardString, new TypeToken<ArrayList<RewardInfo>>() {
        }.getType());
        for (RewardInfo eMail_Reward : rewards) {
            player.getFeature(PlayerCurrency.class).gainItemFlow(PrizeType.valueOf(eMail_Reward.prizeType), eMail_Reward.count, ItemFlow.Mail);
        }
    }


    /**
     * 邮件奖励，插入数据库记录，并推送给该玩家通知
     *
     * @param playerID            欲推送玩家
     * @param title               标题
     * @param msgInfo             消息体
     * @param sender              邮件发送者
     * @param status              邮件状态
     * @param isHaveAnyAttachment 是否有附件
     * @param rewardString        奖励信息
     * @return
     */
    public boolean onInsertEMailAndPushToPlayer(long playerID, String title, String msgInfo, String sender, int status, int isHaveAnyAttachment, String rewardString) {
        Player player = PlayerMgr.getInstance().getPlayer(playerID);
        if (null == player) {
            return false;
        }
        EMailBO eMailBO = new EMailBO();
        eMailBO.setCreateTime(CommTime.nowSecond());
        eMailBO.setTitle(title);
        eMailBO.setIsHaveAnyAttachment(isHaveAnyAttachment);
        eMailBO.setRewardString(rewardString);
        eMailBO.setMsgInfo(msgInfo);
        eMailBO.setSender(sender);
        eMailBO.setStatus(status);
        eMailBO.setPlayerID(playerID);
        eMailBO.getBaseService().saveOrUpDate(eMailBO);

        EMail_Info info = new EMail_Info();
        info.emailID = eMailBO.getId();
        info.createTime = eMailBO.getCreateTime();
        info.title = title;
        info.msgInfo = msgInfo;
        info.sender = sender;
        info.status = status;
        info.isHaveAnyAttachment = isHaveAnyAttachment;
        info.rewardString = rewardString;

        player.pushProtoMq(SEMail_NewMail.make(info));
        return true;
    }


    /**
     * 获取邮箱配置文件信息
     *
     * @return
     */
    public EMailConfigMgr getConfigMgr() {
        return m_ConfigMgr;
    }


    /**
     * 插入邮件
     * 所有玩家都接受
     */
    public boolean onInsertEMail(boolean isOnlinePlayer, String title, String msgInfo, String sender, int status, int isHaveAnyAttachment, String rewardString) {
        if (isOnlinePlayer) {
            for (Player player : PlayerMgr.getInstance().getOnlinePlayers()) {
                this.onInsertEMailAndPushToPlayer(player.getPid(), title, msgInfo, sender, status, isHaveAnyAttachment, rewardString);
            }
        } else {
            for (Player player : PlayerMgr.getInstance().getAllPlayers()) {
                this.onInsertEMailAndPushToPlayer(player.getPid(), title, msgInfo, sender, status, isHaveAnyAttachment, rewardString);
            }
        }
        return true;
    }


}
