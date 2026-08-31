package core.network.http.handler;

import business.global.eMail.EMailMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.network.http.proto.ZleData_Result;


public class EMailRequest {

    private Object Lock = new Object();

    /**
     * 掌乐后台发送邮件 通知所有人
     */
    @RequestMapping(uri = "/sendAllEMail")
    public void sendAllEMail(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());

        CommLogD.info("sendAllEMail requestBytes:{}", jsonPost == null ? 0 : jsonPost.length());

        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        this.onSendAllEMail(response, dataJson);
    }


    /**
     * 掌乐后台发送邮件 针对摸一个玩家
     */
    @RequestMapping(uri = "/sendEMailToPlayer")
    public void sendEMailToPlayer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        CommLogD.info("sendEMailToPlayer requestBytes:{}", jsonPost == null ? 0 : jsonPost.length());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        this.onSendEMailToPlayer(response, dataJson);
    }


    /**
     * 掌乐后台发送邮件 通知所有人
     */
    public void onSendAllEMail(HttpResponse response, JsonObject dataJson) throws Exception {

        CommLogD.info("=====EMailRequest sendAllEMail()收到回调===：{}", dataJson);

        synchronized (Lock) {

            boolean isOnlinePlayer = HttpUtils.getInt(dataJson, "isOnlinePlayer") != 0;
            String title = HttpUtils.getString(dataJson, "title");
            String msgInfo = HttpUtils.getString(dataJson, "msgInfo");

            String sender = HttpUtils.getString(dataJson, "sender");
            int status = HttpUtils.getInt(dataJson, "status");
            int isHaveAnyAttachment = HttpUtils.getInt(dataJson, "isHaveAnyAttachment");
            String rewardString = HttpUtils.getString(dataJson, "rewardString");

            if (EMailMgr.getInstance().onInsertEMail(isOnlinePlayer, title, msgInfo, sender, status, isHaveAnyAttachment, rewardString)) {
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } else {
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "sendAllEMail update error"));
            }
        }
    }


    /**
     * 掌乐后台发送邮件 针对摸一个玩家
     */
    public void onSendEMailToPlayer(HttpResponse response, JsonObject dataJson) throws Exception {

        CommLogD.info("=====EMailRequest sendEMailToPlayer()收到回调===：{}", dataJson);

        synchronized (Lock) {

            int playerID = HttpUtils.getInt(dataJson, "playerID");
            String title = HttpUtils.getString(dataJson, "title");
            String msgInfo = HttpUtils.getString(dataJson, "msgInfo");
            String sender = HttpUtils.getString(dataJson, "sender");
            int status = HttpUtils.getInt(dataJson, "status");
            int isHaveAnyAttachment = HttpUtils.getInt(dataJson, "isHaveAnyAttachment");
            String rewardString = HttpUtils.getString(dataJson, "rewardString");

            if (EMailMgr.getInstance().onInsertEMailAndPushToPlayer(playerID, title, msgInfo, sender, status, isHaveAnyAttachment, rewardString)) {
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } else {
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "sendEMailToPlayer update error"));
            }
        }
    }
}
