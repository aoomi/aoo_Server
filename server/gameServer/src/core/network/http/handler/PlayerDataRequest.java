package core.network.http.handler;

import BaseCommon.CommLog;
import business.global.club.ClubMgr;
import business.global.shareroom.ShareRoomMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerWallet;
import business.shareplayer.ShareNodePlayerSize;
import business.shareplayer.SharePlayerMgr;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.db.persistence.BaseDao;
import core.network.http.proto.SData_Result;
import core.network.http.proto.ZleData_Result;
import jsproto.c2s.iclass.union.CUnion_Change;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 后台获取玩家数据
 *
 * @author Administrator
 */
public class PlayerDataRequest {

    /**
     * 获取当前在线人数
     */
    @RequestMapping(uri = "/onlinePlayerSize")
    public void onlinePlayerSize(HttpRequest request, HttpResponse response) throws Exception {
        int onlinePlayerSizeInt = PlayerMgr.getInstance().getOnlinePlayerSize();
        response.response(ZleData_Result.make(ErrorCode.Success, String.valueOf(onlinePlayerSizeInt)));
    }

    /**
     * 获取线程信息
     */
    @RequestMapping(uri = "/onlineStack")
    public void onlineStack(HttpRequest request, HttpResponse response) throws Exception {
        try {
            BaseDao.systemOutAllThreadInfo();
            response.response(ZleData_Result.make(ErrorCode.Success, ""));
        } catch (Exception e) {
            CommLog.error(e.getMessage());
        }
    }

    /**
     * 获取当前真正的在线人数
     */
    @RequestMapping(uri = "/onlinePlayerSizeReal")
    public void onlinePlayerSizeReal(HttpRequest request, HttpResponse response) throws Exception {
        int onlinePlayerSizeInt = PlayerMgr.getInstance().getOnlinePlayerSizeReal();
        response.response(ZleData_Result.make(ErrorCode.Success, String.valueOf(onlinePlayerSizeInt)));
    }

    /**
     * 获取当前在线人数
     */
    @RequestMapping(uri = "/allNodeOnlinePlayerSize")
    public void allNodeOnlinePlayerSize(HttpRequest request, HttpResponse response) throws Exception {
        List<ShareNodePlayerSize> list = SharePlayerMgr.getInstance().onlinePlayerSizeGroup();
        response.response(ZleData_Result.make(ErrorCode.Success, list));
    }

    /**
     * 校验在线人数
     */
    @RequestMapping(uri = "/checkOnlinePlayerSize")
    public void checkOnlinePlayerSize(HttpRequest request, HttpResponse response) throws Exception {
        PlayerMgr.getInstance().checkOnlinePlayerSize();
        response.response(ZleData_Result.make(ErrorCode.Success, String.valueOf(0)));
    }

    /**
     * 设置手机号
     */
    @RequestMapping(uri = "/setPhone")
    public void setPhone(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        String unionid = HttpUtils.getString(dataJson, "unionid");
        long phone = HttpUtils.getLong(dataJson, "phone");
        long oldPhone = HttpUtils.getLong(dataJson, "oldPhone");
        // 检查unionid是否存在
        if (StringUtils.isEmpty(unionid)) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "unionid error"));
            return;
        }
        // 是否手机号
        if (!StringUtil.isPhone(String.valueOf(phone))) {
            response.response(ZleData_Result.make(ErrorCode.Error_Phone, "phone error :" + phone));
            return;
        }
        // 是否手机号
        if (!StringUtil.isPhone(String.valueOf(phone))) {
            response.response(ZleData_Result.make(ErrorCode.Error_Phone, "phone error :" + phone));
            return;
        }


        if (PlayerMgr.getInstance().checkExistPhone(phone)) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "手机号存在:" + phone));
            return;
        }
        response.response(PlayerMgr.getInstance().setPlayerPhone(unionid, phone, oldPhone));
    }

    /**
     * 设置手机号
     */
    @RequestMapping(uri = "/setPhone1")
    public void setPhone1(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        long accountId = HttpUtils.getLong(dataJson, "accountId");
        long phone = HttpUtils.getLong(dataJson, "phone");
        // 检查unionid是否存在
        if (accountId == 0) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "accountId error"));
            return;
        }
        // 是否手机号
        if (!StringUtil.isPhone(String.valueOf(phone))) {
            response.response(ZleData_Result.make(ErrorCode.Error_Phone, "phone error :" + phone));
            return;
        }

        if (PlayerMgr.getInstance().checkExistPhone(phone)) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "手机号存在:" + phone));
            return;
        }
        response.response(PlayerMgr.getInstance().setPlayerPhone1(accountId, phone));
    }

    /**
     * 检查手机号
     */
    @RequestMapping(uri = "/checkPlayerPhone")
    public void checkPlayerPhone(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        String unionid = HttpUtils.getString(dataJson, "unionid");
        // 检查unionid是否存在
        if (StringUtils.isEmpty(unionid)) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "unionid error"));
            return;
        }
        response.response(PlayerMgr.getInstance().checkPlayerPhone(unionid));
    }


    /**
     * 切换玩家
     */
    @RequestMapping(uri = "/changeCreate")
    public void changeCreate(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        int clubId = HttpUtils.getInt(dataJson, "clubId");
        long oldPid = HttpUtils.getLong(dataJson, "oldPid");
        long newPid = HttpUtils.getLong(dataJson, "newPid");
        if (clubId <= 0) {
            // 亲友圈key
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "clubId error"));
            return;
        }
        Player oldPlayer = PlayerMgr.getInstance().getPlayer(oldPid);
        if (Objects.isNull(oldPlayer)) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("oldPlayer error oldPid:{%d}", oldPid)));
            return;
        }
        Player newPlayer = PlayerMgr.getInstance().getPlayer(newPid);
        if (Objects.isNull(newPlayer)) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("newPlayer error newPid:{%d}", newPid)));
            return;
        }
        SData_Result result = ClubMgr.getInstance().changeCreate(clubId, oldPlayer, newPlayer, true);
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 亲友圈操作失败
            response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
            return;
        }
        CUnion_Change change = (CUnion_Change) result.getData();
        if (change.getUnionId() <= 0L) {
            // 没有赛事
            response.response(ZleData_Result.make(ErrorCode.Success, "success"));
        } else {
            // 赛事操作
            result = UnionMgr.getInstance().changeCreate(change, newPlayer);
            if (!ErrorCode.Success.equals(result.getCode())) {
                response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                return;
            }
            response.response(ZleData_Result.make(ErrorCode.Success, "success"));
        }
    }

    /**
     * 切换正常亲友圈圈主玩家
     */
    @RequestMapping(uri = "/changeNormalCreate")
    public void changeNormalCreate(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        int clubId = HttpUtils.getInt(dataJson, "clubId");
        long oldPid = HttpUtils.getLong(dataJson, "oldPid");
        long newPid = HttpUtils.getLong(dataJson, "newPid");
        if (clubId <= 0) {
            // 亲友圈key
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "clubId error"));
            return;
        }
        Player oldPlayer = PlayerMgr.getInstance().getPlayer(oldPid);
        if (Objects.isNull(oldPlayer)) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("oldPlayer error oldPid:{%d}", oldPid)));
            return;
        }
        Player newPlayer = PlayerMgr.getInstance().getPlayer(newPid);
        if (Objects.isNull(newPlayer)) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("newPlayer error newPid:{%d}", newPid)));
            return;
        }
        SData_Result result = ClubMgr.getInstance().changeCreate(clubId, oldPlayer, newPlayer, false);
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 亲友圈操作失败
            response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
            return;
        }
        // 没有赛事
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));

    }

    /**
     * 获取当前联盟在线房间数
     */
    @RequestMapping(uri = "/unionOnlineRoomSize")
    public void unionOnlineRoomSize(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        long unionId = HttpUtils.getLong(dataJson, "unionId");
        long count = ShareRoomMgr.getInstance().playingRoomByUnionId(unionId);
        response.response(ZleData_Result.make(ErrorCode.Success, count));
    }

}
