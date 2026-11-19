package core.dispatch.event.phone;

import BaseCommon.CommLog;
import business.player.Player;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.ddm.server.http.client.HttpAsyncClient;
import com.ddm.server.http.client.IResponseHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 登录
 *
 * @author Administrator
 */
@Data
public class PhoneEvent implements BaseExecutor {
    /**
     * 玩家信息
     */
    private Player player;


    public PhoneEvent(Player player) {
        this.setPlayer(player);
    }

    @Override
    public void invoke() {
        if (Objects.isNull(getPlayer())) {
            CommLog.error("[PhoneEvent]([PhoneEvent]) null == PhoneEvent || null == PhoneEvent.getPlayer() ");
            return;
        }
        if (StringUtils.isEmpty(getPlayer().getPlayerBO().getWx_unionid()) || getPlayer().getPlayerBO().getWx_unionid().length() < 20) {
            return;
        }

        // 请求账号服地址
        String url = System.getProperty("AccounterServerUrl");
        url = url + "?Sign=DDCat&ServerName=JavaServer";
        String requestBody = null;
        try {
            JsonObject json = new JsonObject();
            json.addProperty("Head", "java.0x000F.account");
            json.addProperty("AccountID", getPlayer().getPlayerBO().getAccountID());
            json.addProperty("Phone", getPlayer().getPlayerBO().getPhone());
            json.addProperty("ServerID", 10001);
            requestBody = json.toString();
        } catch (Exception e) {
            CommLogD.error("请求[{}]封装post参数失败:{}", url, e.toString());
        }
        if (requestBody == null) {
            return;
        }
        // 通知系统
        HttpAsyncClient.startHttpPost(url, requestBody, new IResponseHandler() {

            @Override
            public void compeleted(String response) {
                httpPhoneEvent(response);
            }

            @Override
            public void failed(Exception exception) {
                String url = System.getProperty("AccounterServerUrl");
                CommLogD.error("PhoneEvent url:{} 通知回包失败：{}", url, exception.toString());
            }
        });
    }

    private void httpPhoneEvent(String resultString) {
        JsonObject resJson = new JsonParser().parse(resultString).getAsJsonObject();
        int code = resJson.get("Code").getAsInt();
        if (code == 0) {
            // 标记已经绑定手机登录
            this.getPlayer().getPlayerBO().saveFastCard(1);
            CommLogD.info("httpPhoneEvent Pid:{},AccountId:{},Phone:{}", getPlayer().getPid(), getPlayer().getAccountID(), getPlayer().getPlayerBO().getPhone());
        } else {
            CommLogD.error("httpPhoneEvent resultString:{}", resultString);
        }
    }


    @Override
    public int threadId() {
        return DispatcherComponentEnum.PHONE.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.PHONE.bufferSize();
    }
}
