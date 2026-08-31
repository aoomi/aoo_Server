package core.network.http.handler;

import business.rocketmq.constant.MqTopic;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.data.AbstractRefDataMgr;
import com.ddm.server.common.rocketmq.MqAbsBo;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.network.http.proto.ZleData_Result;

/**
 * 配置表更新
 */
public class ConfigRequest {
    /**
     * 更新指定配置表
     */
    @RequestMapping(uri = "/updateReloadConfig")
    public void updateReloadConfig(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        String reloadName = HttpUtils.getString(dataJson, "name");
        if (AbstractRefDataMgr.getInstance().updateReloadName(reloadName)) {
            response.response(ZleData_Result.make(ErrorCode.Success, "success"));
        } else {
            response.response(ZleData_Result.make(ErrorCode.NotAllow, "update error"));
        }
    }


    /**
     * 重新加载配置表
     */
    @RequestMapping(uri = "/reloadConfig")
    public void reloadConfig(HttpRequest request, HttpResponse response) throws Exception {
        try {
            MqProducerMgr.get().send(MqTopic.HTTP_RELOAD_CONFIG, new MqAbsBo());
            response.response(ZleData_Result.make(ErrorCode.Success, "success"));
        } catch (Exception e) {
            CommLogD.error("reloadConfig error : {}", e.getMessage());
            response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
        }

    }
}
