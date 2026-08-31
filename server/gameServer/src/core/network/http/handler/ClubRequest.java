package core.network.http.handler;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.promotion.PromotionLevelChangeEvent;
import core.network.http.proto.SData_Result;
import core.network.http.proto.ZleData_Result;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.club.Club_define.Club_MINISTER;
import jsproto.c2s.iclass.club.CClub_Create;

import java.util.List;
import java.util.Objects;


public class ClubRequest {

    private Object Lock = new Object();

    /**
     * 掌乐后台更新 俱乐部玩家信息，接口废弃
     */
//    @RequestMapping(uri = "/updateStatus")
//    public void updateStatus(HttpRequest request, HttpResponse response) throws Exception {
//    	String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
//    	JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
//
//    	this.onUpdateStatus(response, dataJson);
//    }
//	/**
//	 * 更新邀请值
//	 */
//	@RequestMapping(uri = "/changeInviteValue")
//	public void changeInviteValue(HttpRequest request, HttpResponse response) throws Exception {
//		DispatcherComponent.getInstance().publish( new ClubChangeInviteEvent());
//		response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
//	}

    /**
     * //	 * 更新邀请值
     * //
     */
    @RequestMapping(uri = "/changeClubMember")
    public void changeClubMember(HttpRequest request, HttpResponse response) throws Exception {
        ClubMember promotionMember;
        if (Config.isShare()) {
            promotionMember = ShareClubMemberMgr.getInstance().getClubMember(298113L);
        } else {
            promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(298113L);
        }
        promotionMember.getClubMemberBO().savePromotionManage(0);
        response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
    }

    /**
     * 修改职务
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/updateIsMinister")
    public void updateIsMinister(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        this.onUpdateIsMinister(response, dataJson);
    }

    /**
     * 掌乐后台更新 俱乐部信息
     */
    @RequestMapping(uri = "/updateClub")
    public void updateClub(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        this.onUpdateClub(response, dataJson);
    }

    /**
     * 关闭游戏房间
     */
    @RequestMapping(uri = "/closeClub")
    public void closeClub(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        this.onCloseClub(response, dataJson);
    }


    /**
     * 关闭游戏房间
     */
    @RequestMapping(uri = "/createClub")
    public void createClub(HttpRequest request, HttpResponse response) throws Exception {
        try {
            String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
            JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

            this.onCreateClub(response, dataJson);
        } catch (Exception e) {
            CommLogD.error("createClub msg = " + e.toString());
        }
    }


    /**
     * 掌乐后台更新 插入俱乐部成员信息
     */
    @RequestMapping(uri = "/insertClubMember")
    public void insertClubMember(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        this.onInsertClubMember(response, dataJson);
    }

    /**
     * 掌乐后台更新 踢出俱乐部成员信息
     */
    @RequestMapping(uri = "/kickClubMember")
    public void kickClubMember(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        this.kickClubMember(response, dataJson);
    }

    //修改职务
    public void onUpdateIsMinister(HttpResponse response, JsonObject dataJson) throws Exception {
        CommLogD.info("=====ClubRequest onUpdateStatus()收到回调===：{}", dataJson);
        synchronized (Lock) {
            long id = HttpUtils.getLong(dataJson, "id");
            int isminister = HttpUtils.getInt(dataJson, "isminister");
            if (ClubMgr.getInstance().getClubMemberMgr().onUpdateIsMinister(id, isminister)) {
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } else {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "onUpdateStatus update error"));
            }
        }
    }
//    //掌乐后台更新 俱乐部玩家信息
//    public void onUpdateStatus(HttpResponse response, JsonObject dataJson) throws Exception{
//    	CommLogD.info("=====ClubRequest onUpdateStatus()收到回调===：{}", dataJson);
//    	synchronized (Lock) {
//    		long id = HttpUtils.getLong(dataJson, "id");
//    		if(ClubMgr.getInstance().getClubMemberMgr().onUpdateStatus(id)){
//    			response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
//    		}else{
//    			response.response(ZleData_Result.make(ErrorCode.NotAllow, "onUpdateStatus update error"));
//    		}
//		}
//    }

    //掌乐后台更新 俱乐部信息
    public void onInsertClubMember(HttpResponse response, JsonObject dataJson) throws Exception {
        CommLogD.info("=====ClubRequest onInsertClubMember()收到回调===：{}", dataJson);
        synchronized (Lock) {
            long playerID = HttpUtils.getLong(dataJson, "playerID");
            long clubID = HttpUtils.getLong(dataJson, "clubID");
            int status = HttpUtils.getInt(dataJson, "status");
            int minister = Club_MINISTER.Club_MINISTER_GENERAL.value();
            if (dataJson.has("minister")) {
                minister = HttpUtils.getInt(dataJson, "minister");
            }
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
            if (null == club) {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "onInsertClubMember null == club"));
                return;
            }
            if (ClubMgr.getInstance().getClubMemberMgr().onInsertClubMember(playerID, clubID, minister, status, club.getClubListBO().getOwnerID())) {
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } else {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "onInsertClubMember update error"));
            }
        }
    }

    //掌乐后台更新 踢出成员
    public void kickClubMember(HttpResponse response, JsonObject dataJson) throws Exception {
        CommLogD.info("=====ClubRequest kickClubMember()收到回调===：{}", dataJson);
        synchronized (Lock) {
            long exePid = HttpUtils.getLong(dataJson, "exePid");//执行操作的pid
            long playerID = HttpUtils.getLong(dataJson, "playerID");//被操作人员id
            long clubID = HttpUtils.getLong(dataJson, "clubID");//亲友圈成员id
            int status = HttpUtils.getInt(dataJson, "status");//状态 踢出 8
            Player player = PlayerMgr.getInstance().getPlayer(playerID);
            if (null == player) {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "kcikClubMember null == player"));
                return;
            }
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
            if (null == club) {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "kcikClubMember null == club"));
                return;
            }
            ClubMember exeMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, exePid);
            if (!exeMember.isMinister()) {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "exeMember not Minister "));
                return;
            }

            ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, playerID);
            if (null == clubMember) {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "kcikClubMember null == clubMember"));
                return;
            }
            // 存在竞技点不为0
            if (checkExistSportsPointNotEqualZero(response, club, status, clubMember)) {
                return;
            }
            //存在保险箱不为0
            if (checkExistCaseSportsPointNotEqualZero(response, club, clubMember, player)) {
                return;
            }
            clubMember.setStatus(player, club, status, exePid, false);
            response.response(ZleData_Result.make(ErrorCode.Success, "OK"));

        }
    }

    /**
     * 存在保险箱不为0
     *
     * @param response
     * @param club
     * @return
     */
    private boolean checkExistCaseSportsPointNotEqualZero(HttpResponse response, Club club, ClubMember tempClubMember, Player player) {
        if (tempClubMember.getClubMemberBO().getCaseSportsPoint() != 0D) {
            response.response(ZleData_Result.make(ErrorCode.NotAllow, "checkExistCaseSportsPointNotEqualZero  not  0D "));
            return true;
        }
        return false;

    }

    /**
     * 存在竞技点不为0
     *
     * @param response
     * @param club
     * @param status
     * @param ClubMember
     * @return
     */
    private boolean checkExistSportsPointNotEqualZero(HttpResponse response, Club club, int status, ClubMember ClubMember) {
        if (status == Club_define.Club_Player_Status.PLAYER_TICHU.value()) {
            if (ClubMember.isPromotion()) {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "kcikClubMember null == clubMember"));
                return true;
            }
        }
        if (club.getClubListBO().getUnionId() > 0L) {
            if (status == Club_define.Club_Player_Status.PLAYER_TUICHU.value() || status == Club_define.Club_Player_Status.PLAYER_TICHU.value()) {
                if (ClubMember.getClubMemberBO().getSportsPoint() != 0D) {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO"));
                    return true;
                }
            }
        }
        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(ClubMember.getClubMemberBO().getPlayerID());
        if (Objects.isNull(sharePlayer)) {
            response.response(ZleData_Result.make(ErrorCode.NotAllow, "Player_PidError"));
            return true;
        }
        if (sharePlayer.getRoomInfo().getRoomId() > 0L && sharePlayer.getRoomInfo().getClubId() == club.getClubListBO().getId()) {
            response.response(ZleData_Result.make(ErrorCode.NotAllow, "CLUB_MEMBER_ROOM_ERROR"));
            return true;
        }
        return false;
    }

    //掌乐后台更新 俱乐部信息
    public void onUpdateClub(HttpResponse response, JsonObject dataJson) throws Exception {
        CommLogD.info("=====ClubRequest onUpdateClub()收到回调===：{}", dataJson);
        synchronized (Lock) {
            long clubID = HttpUtils.getLong(dataJson, "clubID");
            if (ClubMgr.getInstance().getClubListMgr().onUpdateClub(clubID)) {
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } else {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "onUpdateClub update error"));
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
    public void onCloseClub(HttpResponse response, JsonObject dataJson) throws Exception {
        CommLogD.info("=====ClubRequest onCloseClub()收到回调===：{}", dataJson);
        synchronized (Lock) {
            long clubID = HttpUtils.getLong(dataJson, "clubID");
            SData_Result result = ClubMgr.getInstance().getClubListMgr().onCloseClub(clubID, 123L);
            if (ErrorCode.Success.equals(result.getCode())) {
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } else {
                response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
            }
        }
    }

    /**
     * 掌乐后台创建俱乐部信息
     *
     * @param response
     * @param dataJson
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public void onCreateClub(HttpResponse response, JsonObject dataJson) throws Exception {
        try {
            CommLogD.info("=====ClubRequest onCreateClub()收到回调===：{}", dataJson);
            synchronized (Lock) {
                String clubName = HttpUtils.getString(dataJson, "clubName");
                int playerID = HttpUtils.getInt(dataJson, "playerID");
                Player player = PlayerMgr.getInstance().getPlayer(playerID);
                if (null == player) {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "onCreateClub null == player"));
                    return;
                }
                SData_Result result = ClubMgr.getInstance().getClubListMgr().onClubCreate(CClub_Create.make(clubName), player);
                if (ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                } else {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            CommLogD.error("onCreateClub Exception e.msg" + e.toString());
        }

    }


    /**
     * 清空玩家合伙人绑定
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/clearPartnerPid")
    public void clearPartnerPid(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int clubSign = HttpUtils.getInt(dataJson, "clubId");
            int pid = HttpUtils.getInt(dataJson, "pid");
            try {
                SData_Result result = ClubMgr.getInstance().clearPartnerPid(clubSign, pid);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execPostTypeUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 清空合伙人
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/clearPartner")
    public void clearPartner(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int clubSign = HttpUtils.getInt(dataJson, "clubId");
            int pid = HttpUtils.getInt(dataJson, "pid");
            try {
                SData_Result result = ClubMgr.getInstance().clearPartner(clubSign, pid);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execPostTypeUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 掌乐后台更新 插入俱乐部成员信息
     */
    @RequestMapping(uri = "/changeClubMemberPromotionBelong")
    public void changeClubMemberPromotionBelong(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        long playerID = HttpUtils.getLong(dataJson, "playerID");
        long upPlayerID = HttpUtils.getLong(dataJson, "upPlayerID");
        long clubID = HttpUtils.getLong(dataJson, "clubID");
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubID);
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(playerID, clubID);
        if (Objects.isNull(clubMember)) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "changeClubMemberPromotionBelong null == clubMember"));
            return;
        }
        if (clubMember.isLevelPromotion()) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "changeClubMemberPromotionBelong clubMember is CLUB_EXIST_PROMOTION"));
            return;
        }
        ClubMember upClubMember = ClubMgr.getInstance().getClubMemberMgr().find(upPlayerID, clubID);
        if (Objects.isNull(upClubMember)) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "changeClubMemberPromotionBelong null == upClubMember"));
            return;
        }
        if (upClubMember.isNotLevelPromotion()) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "changeClubMemberPromotionBelong upClubMember is CLUB_NOT_PROMOTION"));
            return;
        }

        List<Long> uidList = Lists.newArrayList();
        uidList.add(clubMember.getId());
        DispatcherComponent.getInstance().publish(new PromotionLevelChangeEvent(clubID, playerID, upPlayerID, CommTime.getNowTimeStringYMD(), uidList, playerID, club.getClubListBO().getUnionId()));
    }

    /**
     * 掌乐后台更新 修改成员为推广员管理
     */
    @RequestMapping(uri = "/changeClubMemberPromotionManager")
    public void changeClubMemberPromotionManager(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        long playerID = HttpUtils.getLong(dataJson, "playerID");
        long upPlayerID = HttpUtils.getLong(dataJson, "upPlayerID");
        long clubID = HttpUtils.getLong(dataJson, "clubID");

        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(playerID, clubID);
        if (Objects.isNull(clubMember)) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "changeClubMemberPromotionBelong null == clubMember"));
            return;
        }
        if (clubMember.isLevelPromotion()) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_EXIST_PROMOTION, "changeClubMemberPromotionBelong clubMember is CLUB_EXIST_PROMOTION"));
            return;
        }
        ClubMember upClubMember = ClubMgr.getInstance().getClubMemberMgr().find(upPlayerID, clubID);
        if (Objects.isNull(upClubMember)) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "changeClubMemberPromotionBelong null == upClubMember"));
            return;
        }
        if (upClubMember.isNotLevelPromotion()) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "changeClubMemberPromotionBelong upClubMember is CLUB_NOT_PROMOTION"));
            return;
        }
        //判断是不是直属的下线
        if (upClubMember.getId() != clubMember.getClubMemberBO().getUpLevelId()) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_SUBORDINATE, "CClubSetMinister CLUB_NOT_SUBORDINATE "));
            return;
        }
        Player toPlayer = PlayerMgr.getInstance().getPlayer(upPlayerID);
        if (null == toPlayer) {
            response.response(ZleData_Result.make(ErrorCode.Player_PidError, "null == toPlayer:{%d}"));
            return;
        }// 检查是否管理员上限
        if (ClubMgr.getInstance().getClubMemberMgr().checkPromotionMinisterUpperLimit(clubID, upClubMember)) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_SETMINISTERMAXTATNTWO, "CClubSetMinister  minister count max than two "));
            return;
        }
        clubMember.getClubMemberBO().savePromotionManage(1);

    }


    /**
     * 设置显示断开连接
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/clubShowLostConnect")
    public void clubShowLostConnect(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long clubSign = HttpUtils.getInt(dataJson, "clubId");
            int type = HttpUtils.getInt(dataJson, "getType");
            try {
                SData_Result result = ClubMgr.getInstance().showLostConnect(clubSign, type);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("unionShowLostConnect error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 掌乐后台更新 修改成员推广权限
     */
    @RequestMapping(uri = "/changeClubMemberModifyValue")
    public void changeClubMemberModifyValue(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        long playerID = HttpUtils.getLong(dataJson, "playerID");
        long clubID = HttpUtils.getLong(dataJson, "clubID");
        int modifyValue = HttpUtils.getInt(dataJson, "modifyValue");
        int invite = HttpUtils.getInt(dataJson, "invite");
        int kicking = HttpUtils.getInt(dataJson, "kicking");
        int showShare = HttpUtils.getInt(dataJson, "showShare");
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, playerID);
        if (Objects.isNull(clubMember)) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "changeClubMemberPromotionBelong null == clubMember"));
            return;
        }
        if (clubMember.isNotLevelPromotion()) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "changeClubMemberPromotionBelong clubMember is CLUB_EXIST_PROMOTION"));
            return;
        }
        clubMember.getClubMemberBO().savePromotionLevelPowerOp(kicking, modifyValue, showShare, invite);
        response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
    }

    /**
     * 修改上级归属
     */
    @RequestMapping(uri = "/changeClubMemberUpLevelId")
    public void changeClubMemberUpLevelId(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        long playerID = HttpUtils.getLong(dataJson, "playerID");
        long clubID = HttpUtils.getLong(dataJson, "clubID");
        long upLevelId = HttpUtils.getInt(dataJson, "upLevelId");
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubID, playerID);
        if (Objects.isNull(clubMember)) {
            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "changeClubMemberPromotionBelong null == clubMember"));
            return;
        }
        //普通成员也可以修改
//        if (clubMember.isNotLevelPromotion()) {
//            response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_PROMOTION, "changeClubMemberPromotionBelong clubMember is CLUB_EXIST_PROMOTION"));
//            return;
//        }
        long oldUpLevelId = clubMember.getClubMemberBO().getRealUpLevelId();
        clubMember.getClubMemberBO().saveUpLevelId(upLevelId);
        CommLogD.error("changeClubMemberUpLevelId playerID: " + playerID + "   clubID: " + clubID + "  upLevelId: " + upLevelId + "  oldUpLevelId: " + oldUpLevelId);
        response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
    }
}
