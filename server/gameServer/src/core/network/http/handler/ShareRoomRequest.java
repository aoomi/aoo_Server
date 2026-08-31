package core.network.http.handler;

import business.global.shareroom.ShareRoomMgr;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.network.http.proto.ZleData_Result;
import org.apache.commons.lang3.StringUtils;

public class ShareRoomRequest {

    @RequestMapping(uri = "/shareRoom/remove")
    public void shareRoomRemove(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        //取得参数，参数如果恶意，抛出异常
        String roomKey = HttpUtils.getString(dataJson, "roomKey");
        if (StringUtils.isEmpty(roomKey)) {
            response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "roomKey null"));
            return;
        }
        //删除缓存亲友圈房间
        ShareRoomMgr.getInstance().removeShareRoom(roomKey);
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));
    }

}
