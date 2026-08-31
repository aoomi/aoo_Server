package core.network.http.handler;

import business.global.family.Family;
import business.global.family.FamilyManager;
import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.common.utils.TypeUtils;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.network.http.proto.SData_Result;
import core.network.http.proto.ZleData_Result;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class FamilyRequest {
    private Object Lock = new Object();

    /**
     * 2017/10/12 新增不要进行改动 掌乐后台更新工会信息
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/updateFamily")
    public void updateFamily(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        // 用户Id
        String familyID = HttpUtils.getString(dataJson, "familyID");

        boolean result = FamilyManager.getInstance().updateFamily(TypeUtils.StringTypeLong(familyID));
        if (!result) {
            response.response(ZleData_Result.make(ErrorCode.NotAllow, "该工会信息错误:" + familyID));
            return;
        }
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));
    }

    /**
     * 设置公会
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/setFamilyRoomcardNum")
    public void setFamilyRoomCard(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long familyID = HttpUtils.getLong(dataJson, "familyID");
            int roomcardNum = HttpUtils.getInt(dataJson, "roomcardNum");

            if (familyID <= 0L) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "familyID < 0"));
                return;
            }
            try {
                if (FamilyManager.getInstance().setFamilyRoomCard(familyID, roomcardNum)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "error"));
                }
            } catch (Exception e) {
                CommLogD.error("setFamilyRoomcardNum error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }


    /**
     * 设置公会
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/setFamily")
    public void setFamily(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long familyID = HttpUtils.getLong(dataJson, "familyID");
            String name = HttpUtils.getString(dataJson, "name");
            long ownerID = HttpUtils.getLong(dataJson, "ownerID");
            int fencheng = HttpUtils.getInt(dataJson, "fencheng");
            long recommend = HttpUtils.getLong(dataJson, "recommend");
            int minTixian = HttpUtils.getInt(dataJson, "minTixian");
            int roomcardNum = HttpUtils.getInt(dataJson, "roomcardNum");
            int higherLevel = HttpUtils.getInt(dataJson, "higherLevel");
            int lowerLevel = HttpUtils.getInt(dataJson, "lowerLevel");
            int clubLevel = HttpUtils.isHas(dataJson, "clubLevel") ? HttpUtils.getInt(dataJson, "clubLevel") : 0;
            String beizhu = HttpUtils.isHas(dataJson, "beizhu") ? HttpUtils.getString(dataJson, "beizhu") : "";
            int vip = HttpUtils.getInt(dataJson, "vip");
            int power = HttpUtils.getInt(dataJson, "power");
            if (familyID < 0L) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "familyID < 0"));
                return;
            }

            if (StringUtils.isEmpty(name)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "name == null || name.length() <= 0"));
                return;
            }

            if (ownerID <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "ownerID <= 0"));
                return;
            }

            if (fencheng < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "fencheng < 0"));
                return;
            }

            if (recommend < 0L) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "recommend < 0"));
                return;
            }

            if (minTixian < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "minTixian < 0"));
                return;
            }

            if (roomcardNum < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "roomcardNum < 0"));
                return;
            }

            if (higherLevel < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "higherLevel < 0"));
                return;
            }

            if (lowerLevel < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "lowerLevel < 0"));
                return;
            }

            if (clubLevel < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "clubLevel < 0"));
                return;
            }

            if (vip < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "vip < 0"));
                return;
            }

            if (power < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "power < 0"));
                return;
            }

            try {
                FamilyManager.getInstance().setFamily(familyID, name, ownerID, fencheng, recommend, minTixian,
                        roomcardNum, higherLevel, lowerLevel, clubLevel, beizhu, vip, power);
                response.response(ZleData_Result.make(ErrorCode.Success, "success"));
            } catch (Exception e) {
                CommLogD.error("setFamily error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }

    /**
     * 删除公会
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/deleteFamily")
    public void deleteFamily(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long familyID = HttpUtils.getLong(dataJson, "familyID");
            try {
                FamilyManager.getInstance().deleteFamily(familyID);
                response.response(ZleData_Result.make(ErrorCode.Success, "success"));
            } catch (Exception e) {
                CommLogD.error("deleteFamily error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 给代理充值圈卡
     */
    @RequestMapping(uri = "/onFamilyClubCard")
    public void onFamilyClubCard(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long familyID = HttpUtils.getLong(dataJson, "familyID");
            int clubCard = HttpUtils.getInt(dataJson, "clubCard");
            int type = HttpUtils.getInt(dataJson, "type");
            if (familyID <= 0L) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "familyID < 0L"));
                return;
            }
            Family family = FamilyManager.getInstance().getFamily(familyID);
            if (null == family) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "null == family familyID :" + familyID));
                return;
            }
            if (clubCard <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "clubCard <= 0"));
                return;
            }
            if (type <= 0 || type >= 3) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "type <= 0 || type >=3"));
                return;
            }
            try {
                if (family.onClubCard(clubCard, type)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "error"));
                }
            } catch (Exception e) {
                CommLogD.error("onFamilyClubCard error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 代理对玩家操作圈卡
     */
    @RequestMapping(uri = "/onFamilyClubCardToPlayer")
    public void onFamilyClubCardToPlayer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long pid = HttpUtils.getLong(dataJson, "pid");
            long familyID = HttpUtils.getLong(dataJson, "familyID");
            int clubCard = HttpUtils.getInt(dataJson, "clubCard");
            int type = HttpUtils.getInt(dataJson, "type");
            if (pid <= 0L) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "pid <= 0L"));
                return;
            }
            Player player = PlayerMgr.getInstance().getPlayer(pid);
            if (null == player) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "null == player pid :" + pid));
                return;
            }
            if (familyID <= 0L) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "familyID < 0L"));
                return;
            }
            Family family = FamilyManager.getInstance().getFamily(familyID);
            if (null == family) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "null == family familyID :" + familyID));
                return;
            }
            if (clubCard <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "clubCard <= 0"));
                return;
            }
            if (type <= 0 || type >= 3) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "type <= 0 || type >=3"));
                return;
            }
            try {
                // 代理对玩家操作圈卡
                if (family.onFamilyClubCardToPlayer(player, clubCard, type)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "error"));
                }
            } catch (Exception e) {
                CommLogD.error("onFamilyClubCardToPlayer error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 会长对玩家操作钻石
     */
    @RequestMapping(uri = "/onFamilyCardToPlayer")
    public void onFamilyCardToPlayer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long pid = HttpUtils.getLong(dataJson, "pid");
            long familyID = HttpUtils.getLong(dataJson, "familyID");
            int value = HttpUtils.getInt(dataJson, "value");
            String beizhu = HttpUtils.getString(dataJson, "beizhu");
            if (pid <= 0L) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "pid <= 0L"));
                return;
            }
            Player player = PlayerMgr.getInstance().getPlayer(pid);
            if (Objects.isNull(player)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "null == player pid :" + pid));
                return;
            }
            if (familyID <= 0L) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "familyID < 0L"));
                return;
            }
            Family family = FamilyManager.getInstance().getFamily(familyID);
            if (Objects.isNull(family)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "null == family familyID :" + familyID));
                return;
            }
            if (value <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "value <= 0"));
                return;
            }
            try {
                // 会长转玩家钻石
                SData_Result result = family.onFamilyCardToPlayer(player, value, beizhu);
                if (ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                }
            } catch (Exception e) {
                CommLogD.error("onFamilyCardToPlayer error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }


}
