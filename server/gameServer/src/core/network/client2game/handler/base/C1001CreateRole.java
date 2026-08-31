package core.network.client2game.handler.base;

import BaseCommon.CommLog;
import business.global.GM.MaintainServerMgr;
import business.global.family.Family;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerBase;
import business.player.feature.PlayerFamily;
import business.rocketmq.bo.MqPLayerCreateNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.db.entity.clarkGame.RefererReceiveListBO;
import core.db.entity.dbZle.RecommendBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.RefererReceiveListBOService;
import core.db.service.dbZle.RecommendBOService;
import core.ioc.ContainerMgr;
import core.network.client2game.ClientSession;
import core.network.client2game.handler.BaseHandler;
import jsproto.c2s.iclass.C1001_CreateRole;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * 角色登录
 */
public class C1001CreateRole extends BaseHandler {
    @Override
    public void handle(WebSocketRequest request, String message) throws IOException {

        final C1001_CreateRole req = new Gson().fromJson(message, C1001_CreateRole.class);


        String nickName = req.nickName;
        long accountID = req.accountID;
        String headImageUrl = req.headImageUrl;
        int sex = req.sex;
        // 检查是否处于维护中
        if (!MaintainServerMgr.getInstance().checkUnderMaintenance(request, null)) {
            // 维护中
            return;
        }
        //客户端连接 session
        ClientSession session = (ClientSession) request.getSession();
        if (0 != req.Phone) {
            session.setPhone(req.Phone);
        }
        // session 账户ID
        long sessionAccountID = session.getAccountID();
        if (sessionAccountID != accountID) {
            request.error(ErrorCode.ErrorSysMsg, "创建角色失败,绑定的accountID不同");
            return;
        }
        // 服务端ID
        int serverID = session.getPlayerSid();
        // 玩家管理
        PlayerMgr playerMgr = PlayerMgr.getInstance();
        // 客户端连接,检查角色是否创建失败
        ClientSession waitSession = playerMgr.getWaitCreateSessionByAccountID(accountID);

        if (waitSession != session) {
            CommLog.error("waitSession:" + waitSession.toString() + "   session:" + session.toString());
            request.error(ErrorCode.ErrorSysMsg, "创建角色失败,缓存的session错误");
            return;
        }
        playerMgr.deleteWaitCreateAccountID(accountID);

        // 是否存在
        if (playerMgr.havePlayerByAccountID(accountID)) {
            request.error(ErrorCode.ErrorSysMsg, "账号已经创建过角色了,不能重复创建角色");
            return;
        }

        // 如果新注册用户有名称，则玩家0,否则游客1;
        int tourist = !StringUtils.isEmpty(nickName) ? 0 : 1;

        //如果传空,默认取名字
        if (nickName.length() == 0) {
            nickName = "游客_" + accountID;
        }
        nickName = StringUtil.regexMobile(nickName);
        //如果名字被使用了添加账号ID后缀
        if (playerMgr.havePlayerByName(nickName)) {
            nickName = nickName + "_" + accountID;
            //如果依然被使用,则返回提示
            if (playerMgr.havePlayerByName(nickName)) {
                request.error(ErrorCode.ErrorSysMsg, "CreatRole_FindName");
                return;
            }
        }
        long familyID = Family.DefaultFamilyID;
        int real_referer = 0;
        // 检查玩家推广表，是否有该用户的 accountID。
        RecommendBO rBo = ContainerMgr.get().getComponent(RecommendBOService.class).findOne(Restrictions.eq("accountid", accountID), null);
        if (rBo != null) {
            familyID = rBo.getFamilyID();
            real_referer = rBo.getReal_referer();
        }
        // 检查 familyID 是否为0，如果为0，使用默认工会。
        familyID = familyID == 0L ? Family.DefaultFamilyID : familyID;
        // 创建玩家角色。
        ErrorCode rslt = playerMgr.createPlayer(session, accountID, serverID, nickName, headImageUrl, sex, familyID, real_referer, tourist);
        if (rslt != ErrorCode.Success) {
            request.error(rslt, "创建角色失败, 详细错误信息看服务端控制台or游戏服日志");
            return;
        }
        // 检查 工会和直接推广人是否存在
        checkFamilyAndRealReferer(familyID, session, real_referer);

        session.getPlayer().setIsMobile(req.isMobile);
        // 发送前台
        request.response(session.getPlayer().getFeature(PlayerBase.class).fullInfo(false));
        //共享情况
        if (Config.isShare()) {
            //通知其他节点创建玩家
            MqPLayerCreateNotifyBo mqPLayerCreateNotifyBo = new MqPLayerCreateNotifyBo();
            Player player = playerMgr.getPlayerByAccountID(accountID);
            mqPLayerCreateNotifyBo.setPid(player.getPid());
            MqProducerMgr.get().send(MqTopic.PLAYER_CREATE, mqPLayerCreateNotifyBo);
        }
        return;
    }

    /**
     * 检查 工会和直接推广人是否存在
     *
     * @param familyID     工会
     * @param session      连接
     * @param real_referer 直接推广人
     */
    private void checkFamilyAndRealReferer(long familyID, ClientSession session, int real_referer) {
        if (familyID != Family.DefaultFamilyID) {
            session.getPlayer().getFeature(PlayerFamily.class).roomCardReward();
        }
        if (real_referer > 0) {
            receiveList(session.getPlayer().getPid(), real_referer);
        }

    }


    /**
     * 推广列表
     *
     * @param pid       玩家列表
     * @param refererId 推广ID
     */
    private void receiveList(long pid, int refererId) {
        RefererReceiveListBO receiveList = ContainerMgr.get().getComponent(RefererReceiveListBOService.class).findOne(Restrictions.eq("pid", pid), null);
        if (null == receiveList) {
            RefererReceiveListBO receiveListBO = new RefererReceiveListBO();
            receiveListBO.setPid(pid);
            receiveListBO.setRefererId(refererId);
            receiveListBO.setCreateTime(CommTime.nowSecond());
            receiveListBO.getBaseService().save(receiveListBO);
        }
    }
}
