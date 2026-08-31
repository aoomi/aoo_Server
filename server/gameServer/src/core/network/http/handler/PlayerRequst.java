package core.network.http.handler;

import business.global.family.Family;
import business.global.family.FamilyManager;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerClub;
import business.player.feature.PlayerTask;
import business.rocketmq.bo.MqOnGMExitRoomMsg;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.ConstEnum.RechargeType;
import cenum.ItemFlow;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.GameConfig;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.phone.PhoneEvent;
import core.network.http.proto.ZleData_Result;
import jsproto.c2s.cclass.task.TaskConfigEnum.TaskTargetType;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;


public class PlayerRequst {
    private Object Lock = new Object();

    /**
     * 玩家停机维护的测试权限
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/gmLevelPlayer")
    public void gmLevelPlayer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        // 用户Id
        long pid = HttpUtils.getLong(dataJson, "pid");
        int type = HttpUtils.getInt(dataJson, "type");//改为传值
        // 用户信息
        Player player = PlayerMgr.getInstance().getPlayer(pid);
        if (player == null) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "找不到该玩家:" + pid));
            return;
        }
        if (type >= 0) {
            player.getPlayerBO().saveGmLevel(type);
        } else {
            response.response(ZleData_Result.make(ErrorCode.NotAllow, "玩家权限失败:" + pid));
            return;
        }
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));
    }

    /**
     * 玩家踢出并解散玩家房间
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/kickPlayer")
    public void kickPlayer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        // 用户Id
        long pid = HttpUtils.getLong(dataJson, "pid");
        // 用户信息
        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(pid);
        if (sharePlayer == null) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "找不到该玩家:" + pid));
            return;
        }
        if (sharePlayer.getRoomInfo().getRoomId() > 0) {
            ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByRoomId(sharePlayer.getRoomInfo().getRoomId());
            if (shareRoom != null) {
                MqOnGMExitRoomMsg bo = new MqOnGMExitRoomMsg();
                bo.setPid(pid);
                bo.setShareNode(shareRoom.getCurShareNode());
                MqProducerMgr.get().send(MqTopic.ON_GM_EXIT_ROOM, bo);
            }
        }
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));
    }


    /**
     * 玩家禁止登陆
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/bannedLogin")
    public void BannedLogin(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        // 用户Id
        long pid = HttpUtils.getLong(dataJson, "pid");

        int isBanned = HttpUtils.getInt(dataJson, "isBanned");
        Player player = PlayerMgr.getInstance().getPlayer(pid);
        if (null == player) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "找不到该玩家:" + pid));
            return;
        }
        // 用户信息
        if (player.setBannedLogin(isBanned)) {
            response.response(ZleData_Result.make(ErrorCode.Success, "success"));
        } else {
            response.response(ZleData_Result.make(ErrorCode.NotAllow, "BannedLogin error:" + pid));
        }
    }


    /**
     * 玩家改变公会
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/playerChangeFamily")
    public void playerChangeFamily(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        // 用户Id
        long pid = HttpUtils.getLong(dataJson, "pid");
        long familyID = HttpUtils.getLong(dataJson, "familyID");
        Player player = PlayerMgr.getInstance().getPlayer(pid);
        if (null == player) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "找不到该玩家:" + pid));
            return;
        }
        Family family = FamilyManager.getInstance().getFamily(familyID);
        if (null == family) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "找不到该公会:" + familyID));
            return;
        }
        if (Family.DefaultFamilyID == player.getFamiliID() && familyID > Family.DefaultFamilyID) {
            player.getPlayerBO().saveFamilyID_sync(familyID);
            player.getFeature(PlayerTask.class).exeTask(TaskTargetType.ReferralCode.ordinal());
        } else {
            player.getPlayerBO().saveFamilyID_sync(familyID);
        }
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));

    }


    /**
     * 更新玩家的数据
     * 从数据库重新获取玩家数据
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/updatePlayerMgr")
    public void updatePlayerMgr(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        // 用户Id
        long pid = HttpUtils.getLong(dataJson, "updatePid");

        // 用户信息
        if (PlayerMgr.getInstance().updatePlayerMgr(pid)) {
            response.response(ZleData_Result.make(ErrorCode.Success, "success"));
        } else {
            response.response(ZleData_Result.make(ErrorCode.NotAllow, "更新玩家失败:" + pid));
        }
    }

    /**
     * 更新玩家绑定的手机
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/updatePlayerPhone")
    public void updatePlayerPhone(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long playerID = HttpUtils.getLong(dataJson, "playerID");
            long phone = HttpUtils.getLong(dataJson, "phone");
            Player player = PlayerMgr.getInstance().getPlayer(playerID);
            if (null == player) {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "updatePlayerPhone null == player"));
                return;
            }
            //检测手机号是否被用过了
            Player wxPhone = PlayerMgr.getInstance().getPlayerPhone(phone);
            if (Objects.nonNull(wxPhone)) {
                if (StringUtils.isNotEmpty(player.getPlayerBO().getWx_unionid()) && player.getPlayerBO().getWx_unionid().length() >= 20) {
                    if (StringUtils.isNotEmpty(wxPhone.getPlayerBO().getWx_unionid()) && wxPhone.getPlayerBO().getWx_unionid().length() >= 20) {
                        // 手机和微信uid 一样不能进行绑定
                        response.response(ZleData_Result.make(ErrorCode.Exist_Phone, "Exist_Phone"));
                        return;
                    } else {
                        wxPhone.getPlayerBO().savePhone(0L);
                    }
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Exist_Phone, "Exist_Phone"));
                    return;
                }

            }
            //记录绑定手机信息
            if (player.getPlayerBO().getPhone() <= 0L) {
                player.getFeature(PlayerWallet.class).gainItemFlow(GameConfig.Phone(), ItemFlow.Phone);
            }
            player.getPlayerBO().savePhone(phone);
            if (player.getPlayerBO().getPhone() > 0L) {
                DispatcherComponent.getInstance().publish(new PhoneEvent(player));
            }
            response.response(ZleData_Result.make(ErrorCode.Success, "OK"));

        }
    }

    /**
     * 2017/9/11 新增不要进行改动
     * 掌乐后台更新用户信息
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/updatePlayer")
    public void updatePlayer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        // 用户Id
        long pid = HttpUtils.getLong(dataJson, "updatePid");

        // 用户信息
        Player player = PlayerMgr.getInstance().getPlayer(pid);
        if (player == null) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, "找不到该玩家:" + pid));
            return;
        }
        if (PlayerMgr.getInstance().updatePlayer(pid)) {
            // 返回PHP
            response.response(ZleData_Result.make(ErrorCode.Success, "success"));
        } else {
            response.response(ZleData_Result.make(ErrorCode.NotAllow, "更新玩家失败:" + pid));
            return;
        }
    }


    /**
     * 管理员给玩家圈卡操作
     */
    @RequestMapping(uri = "/onPlayerClubCard")
    public void onFamilyClubCard(HttpRequest request, HttpResponse response) throws Exception {
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

            if (familyID < 0L) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "familyID < 0L"));
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
                if (familyID > 0L) {
                    Family family = FamilyManager.getInstance().getFamily(familyID);
                    if (null == family) {
                        response.response(ZleData_Result.make(ErrorCode.InvalidParam, "null == family familyID :" + familyID));
                        return;
                    }
                }

                if (player.getFeature(PlayerClub.class).onAdminClubCard(clubCard, familyID, type, RechargeType.Platform, ItemFlow.FamilyClubCard)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "error"));
                }
            } catch (Exception e) {
                CommLogD.error("onPlayerClubCard error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


}
