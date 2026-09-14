package core.network.client2game.handler.base;

import BaseCommon.CommLog;
import business.global.GM.MaintainServerMgr;
import business.global.secret.SecretManager;
import business.player.Player;
import business.player.PlayerMgr;
import cenum.DefaultEnum;
import cenum.VisitSignEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.RSACoder;
import com.ddm.server.http.client.HttpAsyncClient;
import com.ddm.server.http.client.IResponseHandler;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.db.entity.dbZle.RecommendBO;
import core.db.other.Restrictions;
import core.db.service.dbZle.RecommendBOService;
import core.ioc.ContainerMgr;
import core.network.client2game.ClientSession;
import core.network.client2game.handler.BaseHandler;
import core.server.OpenSeverTime;
import jsproto.c2s.iclass.C1004_Login;
import jsproto.c2s.iclass.S1004_Login;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * 登录
 */
public class C1004Login extends BaseHandler {

    @Override
    public void handle(WebSocketRequest request, String data) throws IOException {
        final C1004_Login req = new Gson().fromJson(data, C1004_Login.class);
        long accountID = req.accountID;
        String token = req.token;
        String version = req.version;
        // 检查版本号是否正确。
        if (!"1.0.1".equalsIgnoreCase(version)) {
            request.error(ErrorCode.KickOut_ClientVersion, String.format("版本号错误(%s)!=(1.0.1)", version));
            return;
        }
        // 人数限制
        int onlineCount = PlayerMgr.getInstance().getOnlinePlayers().size();
        if (onlineCount >= 20000) {
            request.error(ErrorCode.ErrorSysMsg, "World_MaxOnLine");
            return;
        }
        // 服务器判断
        int serverID = req.serverID;
        String decryptKey = "";
        // 这版网页子游戏的 C1004Login 不携带 clientToken；仅在确实携带时启用 RSA 会话加密。
        if (Objects.nonNull(request.getSession().getChannel()) && StringUtils.isNotEmpty(req.clientToken)) {
            // 用私钥解RSA公钥加密
            // 获取客户端aes秘钥
            try {
                decryptKey = RSACoder.decryptByPrivateKey(req.clientToken, Config.getPrivateKey());
            } catch (Exception e) {
                CommLog.error("C1004Login error:{}", e.getMessage());
                request.error(ErrorCode.KickOut_ClientVersion, String.format("empty aesSecret"));
                return;
            }
            if (StringUtils.isEmpty(decryptKey)) {
                request.error(ErrorCode.KickOut_ClientVersion, String.format("empty aesSecret"));
                return;
            }
        }


        // 请求账号服地址
        String url = System.getProperty("AccounterServerUrl");
        url = url + "?Sign=DDCat&ServerName=JavaServer";
        String requestBody = null;
        try {
            JsonObject json = new JsonObject();
            json.addProperty("_Head", "java.0x000D.account");
            json.addProperty("AccountID", accountID);
            json.addProperty("Token", token);
            json.addProperty("ServerID", serverID);
            requestBody = json.toString();
        } catch (Exception e) {
            CommLogD.error("请求[{}]封装post参数失败:{}", url, e.toString());
        }
        if (requestBody == null) {
            request.error(ErrorCode.KickOut_AccountAuthorizationFail, "");
            return;
        }
        // 通知系统
        String finalDecryptKey = decryptKey;
        HttpAsyncClient.startHttpPost(url, requestBody, new IResponseHandler() {

            @Override
            public void compeleted(String response) {
                login(request, response, req, finalDecryptKey);

            }

            @Override
            public void failed(Exception exception) {
                String url = System.getProperty("AccounterServerUrl");
                CommLogD.error("url:{} 通知回包失败：{}", url, exception.toString());
                login(request, "{\"Code\":4}", req, finalDecryptKey);
            }
        });

    }

    /**
     * @param request
     * @param resultString
     * @param req
     */
    private void login(WebSocketRequest request, String resultString, C1004_Login req, String decryptKey) {
        String url = System.getProperty("AccounterServerUrl");

        try {
            JsonObject resJson = new JsonParser().parse(resultString).getAsJsonObject();

            int code = resJson.get("Code").getAsInt();
            if (code != 0) {
                request.error(ErrorCode.KickOut_AccountAuthorizationFail, "");
                return;
            }

            long phoneNum = 0L;
            String name = "";
            if (com.alibaba.druid.util.StringUtils.isNumber(resJson.has("PhoneNum") ? resJson.get("PhoneNum").getAsString() : "")) {
                phoneNum = resJson.has("PhoneNum") ? resJson.get("PhoneNum").getAsLong() : 0L;
            } else {
                name = resJson.has("PhoneNum") ? resJson.get("PhoneNum").getAsString() : "";
            }

            long accountID = resJson.get("AccountID").getAsLong();
            int serverID = resJson.get("ServerID").getAsInt();
            ClientSession session = (ClientSession) request.getSession();
            session.setValid(true);
            session.setAccountID(accountID);
            session.setPlayerSid(serverID);
            session.setWxUnionid(req.unionid);
            session.setPhone(phoneNum);
            PlayerMgr playerMgr = PlayerMgr.getInstance();

            byte isNeedCreateRole = 0;
            // 如果已经存在player实例
            if (playerMgr.havePlayerByAccountID(accountID)) {
                Player player = playerMgr.getPlayerByAccountID(accountID);
                // 检查是否处于维护中
                if (!MaintainServerMgr.getInstance().checkUnderMaintenance(request, player)) {
                    // 维护中
                    return;
                }
                if (player.isBannedLogin(request)) {
                    return;
                }
                if (phoneNum > 0L && player.getPlayerBO().getPhone() <= 0L) {
                    player.getPlayerBO().savePhone(phoneNum);
                }
                if (StringUtils.isNotEmpty(name)) {
                    player.getPlayerBO().saveName(name);
                }
                // 连接账号玩家
                playerMgr.connectPlayer(session, player);
                isNeedCreateRole = 0;
                // 检查玩家Wx_unionid是否存
                if (StringUtils.isEmpty(player.getPlayerBO().getWx_unionid()) || player.getPlayerBO().getWx_unionid().length() <= 10) {
                    if (StringUtils.isEmpty(session.getWxUnionid())) {
                        CommLog.info("Login empty unionId Pid:{},accountID:{}", player.getPid(), accountID);
                        RecommendBO rBo = ContainerMgr.get().getComponent(RecommendBOService.class).findOne(Restrictions.eq("accountid", accountID), null);
                        if (Objects.nonNull(rBo) && StringUtils.isNotEmpty(rBo.getWx_unionid())) {
                            // 检查推荐表是否有Wx_unionid
                            player.getPlayerBO().saveWx_unionid(rBo.getWx_unionid());
                        }
                    } else {
                        player.getPlayerBO().saveWx_unionid(session.getWxUnionid());
                    }
                }
                player.setUUID(req.token);
                player.setIsMobile(req.isMobile);
                if (req.isMobile != 1) {
                    player.updateHeadImageUrl(req.headImageUrl, req.nickName);
                }
                //重新登陆设置访问页
                player.setSignEnum(VisitSignEnum.NONE);
            }
            // 是新建账号登录
            else {
                // 检查是否处于维护中
                if (!MaintainServerMgr.getInstance().checkUnderMaintenance(request, null)) {
                    // 维护中
                    return;
                }
                isNeedCreateRole = 1;
                session.setToken(req.token);
                playerMgr.addWaitCreateAccountID(accountID, session);
            }

            getZleDB(session.getWxUnionid(), accountID);

            S1004_Login resultInfo = new S1004_Login();
            resultInfo.time = CommTime.nowMS();
            resultInfo.timeZone = CommTime.timezone().getRawOffset();
            resultInfo.startServerTime = OpenSeverTime.getInstance().getStartServerTime();
            resultInfo.defaultFamilyID = DefaultEnum.FAMILY_ID.value();
            resultInfo.isNeedCreateRole = isNeedCreateRole;
            resultInfo.isMobile = req.isMobile;
            if (StringUtils.isNotEmpty(decryptKey)) {
                String serverToken = UUID.randomUUID().toString().replaceAll("-", "");
                SecretManager.saveC1004Login(accountID, decryptKey, serverToken);
                resultInfo.serverToken = serverToken;
            }

            // 发送前台
            request.response(resultInfo);

        } catch (Exception e) {
            CommLogD.error("账号服务器({})返回({}),验证解析失败", url, resultString, e);
            request.error(ErrorCode.KickOut_AccountAuthorizationFail, "");
        }
    }

    /**
     * 获取掌乐后台数据
     *
     * @param wx_unionid
     * @param accountID
     */
    public void getZleDB(String wx_unionid, long accountID) {
        RecommendBO rBo = ContainerMgr.get().getComponent(RecommendBOService.class).findOne(Restrictions.eq("wx_unionid", wx_unionid), null);
        if (rBo != null) {
            rBo.saveAccountid(accountID);
        }
    }
}
