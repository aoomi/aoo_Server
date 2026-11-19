package core.network.http.handler;

import business.global.GM.MaintainGameMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.db.entity.clarkGame.MaintainGameBO;
import core.network.http.proto.ZleData_Result;

import java.util.List;

public class MaintainGameRequest {
    private Object Lock = new Object();


    /**
     * 设置游戏维护内容
     */
    @RequestMapping(uri = "/setMaintainGame")
    public void setMaintainGame(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        synchronized (Lock) {

            int startTime = HttpUtils.getInt(dataJson, "startTime");
            int endTime = HttpUtils.getInt(dataJson, "endTime");
            int status = HttpUtils.getInt(dataJson, "status");
            String title = HttpUtils.getString(dataJson, "title");
            String content = HttpUtils.getString(dataJson, "content");
            String mainTitle = HttpUtils.getString(dataJson, "mainTitle");
            int gameTypeId = HttpUtils.getInt(dataJson, "gameTypeId");

            try {
                MaintainGameBO maintainGameBO = new MaintainGameBO();
                maintainGameBO.setContent("");
                maintainGameBO.setStartTime(startTime);
                maintainGameBO.setEndTime(endTime);
                maintainGameBO.setStatus(status);
                maintainGameBO.setTitle(title);
                maintainGameBO.setContent(content);
                maintainGameBO.setMainTitle(mainTitle);
                maintainGameBO.setGameTypeId(gameTypeId);
                MaintainGameMgr.getInstance().saveMaintainGame(maintainGameBO);
                response.response(ZleData_Result.make(ErrorCode.Success, "success"));
            } catch (Exception e) {
                CommLogD.error("setMaintainGame error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }

    /**
     * 获取维护的游戏节点
     */
    @RequestMapping(uri = "/getMaintainGame")
    public void getMaintainGame(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        int gameTypeId = HttpUtils.getInt(dataJson, "gameTypeId");
        try {
            MaintainGameBO maintainGameBO = MaintainGameMgr.getInstance().getMaintainGame(gameTypeId);
            response.response(ZleData_Result.make(ErrorCode.Success, maintainGameBO));
        } catch (Exception e) {
            CommLogD.error("getMaintainGame error : {}", e.getMessage());
            response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
        }

    }

    /**
     * 获取维护的游戏列表
     */
    @RequestMapping(uri = "/listMaintainGame")
    public void listMaintainGame(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        try {
            List<MaintainGameBO> list = MaintainGameMgr.getInstance().listMaintainGameBO();
            response.response(ZleData_Result.make(ErrorCode.Success, list));
        } catch (Exception e) {
            CommLogD.error("listMaintainGame error : {}", e.getMessage());
            response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
        }

    }


}
