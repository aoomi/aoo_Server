package core.network.http.handler;

import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerRecharge;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.network.http.proto.ZleData_Result;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class ServerRequest {

    private Object Lock = new Object();

    /**
     * PHP后台充值
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/rechargePHP")
    public void rechargePHP(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long pid = HttpUtils.getLong(dataJson, "pid");
            int appPrice = HttpUtils.getInt(dataJson, "appPrice");
            int rechargeNum = HttpUtils.getInt(dataJson, "rechargeNum");
            String orderID = HttpUtils.getString(dataJson, "orderID");
            long orderTime = HttpUtils.getLong(dataJson, "orderTime");
            int sourceType = HttpUtils.getInt(dataJson, "sourceType");
            String platformType = HttpUtils.getString(dataJson, "platformType");
            int rechargeType = HttpUtils.isHas(dataJson, "rechargeType") ? HttpUtils.getInt(dataJson, "rechargeType") : 0;
            long clubID = HttpUtils.isHas(dataJson, "clubID") ? HttpUtils.getLong(dataJson, "clubID") : 0;
            if (pid <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "pid <=0"));
                return;
            }
            Player player = PlayerMgr.getInstance().getPlayer(pid);
            if (player == null) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "player == null"));
                return;
            }
            if (appPrice <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "appPrice <=0"));
                return;
            }
            if (rechargeNum <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "rechargeNum <= 0"));
                return;
            }
            if (StringUtils.isEmpty(orderID)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "null == orderID"));
                return;
            }
            if (orderTime <= 0L) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "orderTime <= 0"));
                return;
            }
            if (sourceType < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "sourceType <= 0"));
                return;
            }
            if (StringUtils.isEmpty(platformType)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "null == platformType"));
                return;
            }

            try {
                response.response(player.getFeature(PlayerRecharge.class).recharge(appPrice, rechargeNum, orderID, orderTime, sourceType, platformType, rechargeType, clubID));
            } catch (Exception e) {
                CommLogD.error("rechargePHP error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }


    /**
     * PHP后台充值
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/rechargePHPToPlayer")
    public void rechargePHPToPlayer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long pid = HttpUtils.getLong(dataJson, "pid");
            int value = HttpUtils.getInt(dataJson, "value");
            String beizhu = HttpUtils.getString(dataJson, "beizhu");

            if (pid <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "pid <=0"));
                return;
            }
            Player player = PlayerMgr.getInstance().getPlayer(pid);
            if (Objects.isNull(player)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "player == null"));
                return;
            }
//            if (value <= 0) {
//                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "value <= 0"));
//                return;
//            }
            try {
                response.response(player.getFeature(PlayerRecharge.class).rechargePHPToPlayer(value, beizhu));
            } catch (Exception e) {
                CommLogD.error("rechargePHP error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }

    /**
     * PHP后台撤回
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/deductPHPToPlayer")
    public void deductPHPToPlayer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long pid = HttpUtils.getLong(dataJson, "pid");
            int value = HttpUtils.getInt(dataJson, "value");
            String beizhu = HttpUtils.getString(dataJson, "beizhu");

            if (pid <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "pid <=0"));
                return;
            }
            Player player = PlayerMgr.getInstance().getPlayer(pid);
            if (Objects.isNull(player)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "player == null"));
                return;
            }
            if (value <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "value <= 0"));
                return;
            }
            try {
                response.response(player.getFeature(PlayerRecharge.class).deductPHPToPlayer(value, beizhu));
            } catch (Exception e) {
                CommLogD.error("rechargePHP error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }
}
