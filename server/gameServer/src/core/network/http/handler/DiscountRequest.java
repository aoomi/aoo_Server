package core.network.http.handler;

import business.global.config.DiscountMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.network.http.proto.ZleData_Result;

public class DiscountRequest {

    private Object Lock = new Object();

    /**
     * 删除打折（免费）活动
     */
    @RequestMapping(uri = "/delDiscount")
    public void delDiscount(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long id = HttpUtils.getLong(dataJson, "id");
            // 检查ID
            if (id < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "id < 0 : " + id));
                return;
            }
            try {
                // 删除指定任务
                if (DiscountMgr.getInstance().delDiscount(id)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "del error or delDiscount error"));
                }
            } catch (Exception e) {
                CommLogD.error("delDiscount error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }

    /**
     * 修改打折（免费）活动
     */
    @RequestMapping(uri = "/setDiscount")
    public void setDiscount(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long id = HttpUtils.getLong(dataJson, "id");
            int crowdType = HttpUtils.getInt(dataJson, "crowdType");
            String crowdList = HttpUtils.getString(dataJson, "crowdList");
            String gameList = HttpUtils.getString(dataJson, "gameList");
            int value = HttpUtils.getInt(dataJson, "value");
            int dateType = HttpUtils.getInt(dataJson, "dateType");
            int startTime = HttpUtils.getInt(dataJson, "startTime");
            int endTime = HttpUtils.getInt(dataJson, "endTime");
            int state = HttpUtils.getInt(dataJson, "state");

            if (crowdType <= 0 || crowdType > 7) {
                response.response(
                        ZleData_Result.make(ErrorCode.InvalidParam, "(crowdType <= 0 || crowdType >4) :" + crowdType));
                return;
            }

            if (value < 0 || value > 100) {
                response.response(
                        ZleData_Result.make(ErrorCode.InvalidParam, "discountType < 0 || discountType > 100"));
                return;
            }

            if (dateType <= 0 || dateType > 3) {
                response.response(
                        ZleData_Result.make(ErrorCode.InvalidParam, "(dateType <= 0 || dateType >3) :" + dateType));
                return;
            }

            if (startTime <= 0) {
                response.response(
                        ZleData_Result.make(ErrorCode.InvalidParam, "startTime <=0 "));
                return;
            }

            if (endTime <= 0) {
                response.response(
                        ZleData_Result.make(ErrorCode.InvalidParam, "endTime <=0 "));
                return;
            }
            try {
                // 更新活动任务
                if (DiscountMgr.getInstance().setDiscount(id, crowdType, crowdList, gameList, value,
                        dateType, startTime, endTime, state)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "setDiscount error"));
                }
            } catch (Exception e) {
                CommLogD.error("setTask error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

}
