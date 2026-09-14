package core.network.http.handler;

import business.global.redBagActivity.ActivityManager;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.network.http.proto.ZleData_Result;


public class RedActivityRequest {

    private Object Lock = new Object();

    /**
     * 掌乐后台更新 俱乐部玩家信息
     */
    @RequestMapping(uri = "/updateRedActivity")
    public void updateRedActivity(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        this.onUpdateRedActivity(response, dataJson);
    }

    /**
     * 掌乐后台更新 俱乐部玩家信息
     */
    @RequestMapping(uri = "/instertRedActivity")
    public void instertRedActivity(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        this.onInstertRedActivity(response, dataJson);
    }

    /**
     * 关闭游戏房间
     */
    @RequestMapping(uri = "/closeAllActity")
    public void closeAllActity(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        this.onCloseAllActity(response, dataJson);
    }

    /**
     * 关闭游戏房间
     */
    @RequestMapping(uri = "/closeActity")
    public void closeActity(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        this.onCloseActity(response, dataJson);
    }

    public void onUpdateRedActivity(HttpResponse response, JsonObject dataJson) throws Exception {
        CommLogD.info("=====RedActivityRequest onUpdateRedActivity()收到回调===：{}", dataJson);
        synchronized (Lock) {
            if (!HttpUtils.isHas(dataJson, "activityID")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "activityID is not"));
                return;
            }
            long activityID = HttpUtils.getLong(dataJson, "activityID");

            if (!HttpUtils.isHas(dataJson, "crowd")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "crowd is not"));
                return;
            }
            int crowd = HttpUtils.getInt(dataJson, "crowd");

            if (!HttpUtils.isHas(dataJson, "crowd_daili")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "crowd_daili is not"));
                return;
            }
            String crowd_daili = HttpUtils.getString(dataJson, "crowd_daili");

            if (!HttpUtils.isHas(dataJson, "game_type")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "game_type is not"));
                return;
            }
            String game_type = HttpUtils.getString(dataJson, "game_type");

            if (!HttpUtils.isHas(dataJson, "begin_time")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "begin_time is not"));
                return;
            }
            int begin_time = HttpUtils.getInt(dataJson, "begin_time");

            if (!HttpUtils.isHas(dataJson, "end_time")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "end_time is not"));
                return;
            }
            int end_time = HttpUtils.getInt(dataJson, "end_time");

            if (begin_time >= end_time) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "begin_time >= end_time"));
                return;
            }

            if (!HttpUtils.isHas(dataJson, "max_money")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "max_money is not"));
                return;
            }
            int max_money = HttpUtils.getInt(dataJson, "max_money");

            if (!HttpUtils.isHas(dataJson, "every_money")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "every_money is not"));
                return;
            }
            int every_money = HttpUtils.getInt(dataJson, "every_money");

            if (every_money > 100) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "every_money > 100"));
                return;
            }

            if (ActivityManager.getInstance().onUpdateActivity(activityID, crowd, crowd_daili, game_type, begin_time, end_time, max_money, every_money)) {
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } else {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "onUpdateRedActivity update error"));
            }
        }
    }

    public void onInstertRedActivity(HttpResponse response, JsonObject dataJson) throws Exception {
        CommLogD.info("=====RedActivityRequest onInstertRedActivity()收到回调===：{}", dataJson);
        synchronized (Lock) {
//    		if (HttpUtils.isHas(dataJson, "activityID")) {
//    			response.response(ZleData_Result.make(ErrorCode.InvalidParam, "activityID is not"));
//				return;
//			}
//    		long activityID = HttpUtils.getLong(dataJson, "activityID");

            if (!HttpUtils.isHas(dataJson, "crowd")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "crowd is not"));
                return;
            }
            int crowd = HttpUtils.getInt(dataJson, "crowd");

            if (!HttpUtils.isHas(dataJson, "crowd_daili")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "crowd_daili is not"));
                return;
            }
            String crowd_daili = HttpUtils.getString(dataJson, "crowd_daili");

            if (!HttpUtils.isHas(dataJson, "game_type")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "game_type is not"));
                return;
            }
            String game_type = HttpUtils.getString(dataJson, "game_type");

            if (!HttpUtils.isHas(dataJson, "begin_time")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "begin_time is not"));
                return;
            }
            int begin_time = HttpUtils.getInt(dataJson, "begin_time");

            if (!HttpUtils.isHas(dataJson, "end_time")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "end_time is not"));
                return;
            }
            int end_time = HttpUtils.getInt(dataJson, "end_time");

            if (begin_time >= end_time) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "begin_time >= end_time"));
                return;
            }

            if (!HttpUtils.isHas(dataJson, "max_money")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "max_money is not"));
                return;
            }
            int max_money = HttpUtils.getInt(dataJson, "max_money");

            if (!HttpUtils.isHas(dataJson, "every_money")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "every_money is not"));
                return;
            }
            int every_money = HttpUtils.getInt(dataJson, "every_money");

            if (every_money > 100) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "every_money > 100"));
                return;
            }

            if (ActivityManager.getInstance().onInstertActivity(crowd, crowd_daili, game_type, begin_time, end_time, max_money, every_money)) {
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } else {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "onInstertRedActivity update error"));
            }
        }
    }


    /**
     * 掌乐后台关闭俱乐部信息
     *
     * @param response
     * @param dataJson
     * @throws Exception
     */
    public void onCloseAllActity(HttpResponse response, JsonObject dataJson) throws Exception {
        CommLogD.info("=====RedActivityRequest onCloseAllActity()收到回调===：{}", dataJson);
        synchronized (Lock) {
            if (ActivityManager.getInstance().closeAllActivity()) {
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } else {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "onCloseAllActity error"));
            }
        }
    }

    /**
     * 掌乐后台关闭俱乐部信息
     *
     * @param response
     * @param dataJson
     * @throws Exception
     */
    public void onCloseActity(HttpResponse response, JsonObject dataJson) throws Exception {
        CommLogD.info("=====RedActivityRequest onCloseAllActity()收到回调===：{}", dataJson);
        synchronized (Lock) {
            if (!HttpUtils.isHas(dataJson, "activityID")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "activityID is not"));
                return;
            }
            long activityID = HttpUtils.getLong(dataJson, "activityID");

            if (!HttpUtils.isHas(dataJson, "end_time")) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "end_time is not"));
                return;
            }
            int end_time = HttpUtils.getInt(dataJson, "end_time");
            if (ActivityManager.getInstance().closeActivity(activityID, end_time)) {
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } else {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "onCloseAllActity error"));
            }
        }
    }
}
