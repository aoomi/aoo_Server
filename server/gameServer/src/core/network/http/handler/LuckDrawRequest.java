package core.network.http.handler;

import business.global.config.LuckDrawConfigMgr;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.network.http.proto.ZleData_Result;

public class LuckDrawRequest {

    /**
     * 更新指定配置表
     */
    @RequestMapping(uri = "/updateLuckDrawConfig")
    public void updateLuckDrawConfig(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        long id = HttpUtils.getLong(dataJson, "id");
        if (LuckDrawConfigMgr.getInstance().updateConfig(id)) {
            response.response(ZleData_Result.make(ErrorCode.Success, "success"));
        } else {
            response.response(ZleData_Result.make(ErrorCode.NotAllow, "update error"));
        }
    }
}
