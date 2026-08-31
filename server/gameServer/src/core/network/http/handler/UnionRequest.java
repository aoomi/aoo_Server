package core.network.http.handler;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.union.Union;
import business.global.union.UnionMember;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerFamily;
import business.rocketmq.bo.MqUnionDissolveInitRoomBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import core.network.http.proto.SData_Result;
import core.network.http.proto.ZleData_Result;
import jsproto.c2s.cclass.FamilyItem;
import jsproto.c2s.cclass.union.CUnionScorePercentItem;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.union.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;


/**
 *
 */
public class UnionRequest {

    private Object Lock = new Object();


    /**
     * 赛事管理员获取亲友圈成员列表
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execUnionClubMemberList")
    public void execUnionClubMemberList(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long opClubId = HttpUtils.getLong(dataJson, "opClubId");
            String query = HttpUtils.getString(dataJson, "query");
            int pageNum = HttpUtils.getInt(dataJson, "pageNum");

            CUnion_ClubMemberList req = new CUnion_ClubMemberList();
            req.setUnionId(unionId);
            req.setOpClubId(opClubId);
            req.setQuery(query);
            req.setPageNum(pageNum);
            try {
                SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getUnionMemberManageList(req.getOpClubId(), req.getUnionId(), req.getPageNum(), req.getQuery(), 0, 0);
                if (ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(ErrorCode.Success, result.getData()));
                } else {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));

                }
            } catch (Exception e) {
                CommLogD.error("execUnionRemoveMember error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 获取联盟成员批量配置列表
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execScorePercentList")
    public void execScorePercentList(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            long opClubId = HttpUtils.getLong(dataJson, "opClubId");
            long opPid = HttpUtils.getLong(dataJson, "opPid");
            int pageNum = HttpUtils.getInt(dataJson, "pageNum");
            long exePid = HttpUtils.getLong(dataJson, "exePid");

            CUnion_ScorePercentList req = new CUnion_ScorePercentList();
            req.setUnionId(unionId);
            req.setClubId(clubId);
            req.setOpClubId(opClubId);
            req.setOpPid(opPid);
            req.setPageNum(pageNum);
            try {
                SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execScorePercentList(req, exePid);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, result.getData()));
                }
            } catch (Exception e) {
                CommLogD.error("execUnionRemoveMember error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 批量修改联盟成员批量值
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execUpdateUnionRoomConfigScorePercent")
    public void execUpdateUnionRoomConfigScorePercent(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            long opClubId = HttpUtils.getLong(dataJson, "opClubId");
            long opPid = HttpUtils.getLong(dataJson, "opPid");
            long exePid = HttpUtils.getLong(dataJson, "exePid");
            long type = HttpUtils.getLong(dataJson, "exePid");
            String unionScorePercentItemList = HttpUtils.getString(dataJson, "unionScorePercentItemList");

            if (StringUtils.isEmpty(unionScorePercentItemList)) {
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "error unionScorePercentItemList"));
                return;
            }
            CUnion_ScorePercentBatchUpdate req = new CUnion_ScorePercentBatchUpdate();
            req.setUnionId(unionId);
            req.setClubId(clubId);
            req.setOpClubId(opClubId);
            req.setOpPid(opPid);
            req.setUnionScorePercentItemList(new Gson().fromJson(unionScorePercentItemList, new TypeToken<List<CUnionScorePercentItem>>() {
            }.getType()));
            try {
                SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execScorePercentBatchUpdate(req, exePid);
                if (ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                    // 设置积分比例修改
                    ((UnionMember) result.getData()).execUpdateUnionRoomConfigScorePercent(req, exePid);
                } else {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                }
            } catch (Exception e) {
                CommLogD.error("execUnionRemoveMember error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 禁止游戏
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execUnionBanGame")
    public void execUnionBanGame(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            long opClubId = HttpUtils.getLong(dataJson, "opClubId");
            long opPid = HttpUtils.getLong(dataJson, "opPid");
            int value = HttpUtils.getInt(dataJson, "value");
            long exePid = HttpUtils.getLong(dataJson, "exePid");
            CUnion_BanGameClubMember req = new CUnion_BanGameClubMember();
            req.setUnionId(unionId);
            req.setClubId(clubId);
            req.setOpClubId(opClubId);
            req.setOpPid(opPid);
            req.setValue(value);
            try {
                SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execUnionBanGame(req, exePid);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execUnionRemoveMember error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 邀请添加赛事成员
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execInsertUnionMember")
    public void execInsertUnionMember(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            int clubSign = HttpUtils.getInt(dataJson, "clubSign");
            long exePid = HttpUtils.getLong(dataJson, "exePid");

            if (UnionMgr.getInstance().getUnionMemberMgr().isNotManage(exePid, clubId, unionId)) {
                response.response(ZleData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE"));
                return;
            }
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubSign);
            if (null == club) {
                response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST"));
                return;
            }
            ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().findCreate(club.getClubListBO().getId());
            if (null == clubMember) {
                response.response(ZleData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER"));
                return;
            }
            try {
                if (UnionMgr.getInstance().getUnionMemberMgr().onInsertUnionMember(club.getOwnerPlayer(), unionId, club.getClubListBO().getId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value(), exePid, clubMember.getId())) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "NotAllowss"));
                }
            } catch (Exception e) {
                CommLogD.error("execInsertUnionMember error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 赛事移除成员信息
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execUnionRemoveMember")
    public void execUnionRemoveMember(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            long opClubId = HttpUtils.getLong(dataJson, "opClubId");
            long opPid = HttpUtils.getLong(dataJson, "opPid");
            long exePid = HttpUtils.getLong(dataJson, "exePid");
            CUnion_RemoveMember req = new CUnion_RemoveMember();
            req.setUnionId(unionId);
            req.setClubId(clubId);
            req.setOpClubId(opClubId);
            req.setOpPid(opPid);
            try {
                SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execUnionRemoveMember(req, exePid);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execUnionRemoveMember error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 执行收益更新
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execScorePercentUpdate")
    public void execScorePercentUpdate(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            long opClubId = HttpUtils.getLong(dataJson, "opClubId");
            long opPid = HttpUtils.getLong(dataJson, "opPid");
            double value = HttpUtils.getDouble(dataJson, "value");
            long exePid = HttpUtils.getLong(dataJson, "exePid");
            CUnion_ScorePercentUpdate req = new CUnion_ScorePercentUpdate();
            req.setUnionId(unionId);
            req.setClubId(clubId);
            req.setOpClubId(opClubId);
            req.setOpPid(opPid);
            req.setValue(value);
            try {
                SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execScorePercentUpdate(req, exePid);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execScorePercentUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 更新职务
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execPostTypeUpdate")
    public void execPostTypeUpdate(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            long opClubId = HttpUtils.getLong(dataJson, "opClubId");
            long opPid = HttpUtils.getLong(dataJson, "opPid");
            int value = HttpUtils.getInt(dataJson, "value");
            long exePid = HttpUtils.getLong(dataJson, "exePid");
            CUnion_PostTypeUpdate req = new CUnion_PostTypeUpdate();
            req.setUnionId(unionId);
            req.setClubId(clubId);
            req.setOpClubId(opClubId);
            req.setOpPid(opPid);
            req.setValue(value);
            try {
                SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execPostTypeUpdate(req, exePid);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execPostTypeUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


//    /**
//     * 执行竞技点修改
//     *
//     * @param request
//     * @param response
//     * @throws Exception
//     */
//    @RequestMapping(uri = "/execSportsPointUpdate")
//    public void execSportsPointUpdate(HttpRequest request, HttpResponse response) throws Exception {
//        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
//        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
//        synchronized (Lock) {
//            long unionId = HttpUtils.getLong(dataJson, "unionId");
//            long clubId = HttpUtils.getLong(dataJson, "clubId");
//            long opClubId = HttpUtils.getLong(dataJson, "opClubId");
//            long opPid = HttpUtils.getLong(dataJson, "opPid");
//            double value = HttpUtils.getDouble(dataJson, "value");
//            int type = HttpUtils.getInt(dataJson, "type");
//            long exePid = HttpUtils.getLong(dataJson, "exePid");
//            CUnion_SportsPointUpdate req = new CUnion_SportsPointUpdate();
//            req.setUnionId(unionId);
//            req.setClubId(clubId);
//            req.setOpClubId(opClubId);
//            req.setOpPid(opPid);
//            req.setValue(value);
//            req.setType(type);
//                try {
//                    SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execSportsPointUpdate(req, exePid);
//                    if (!ErrorCode.Success.equals(result.getCode())) {
//                        response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
//                        return;
//                    } else {
//                        response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
//                    }
//                } catch (Exception e) {
//                    CommLogD.error("execPostTypeUpdate error : {}", e.getMessage());
//                    response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
//                }
//        }
//    }


    /**
     * 后台更新指定赛事竞技点
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execBackstageSportsPointUpdate")
    public void execBackstageSportsPointUpdate(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            double value = HttpUtils.getDouble(dataJson, "value");
            int type = HttpUtils.getInt(dataJson, "type");
            try {
                SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execBackstageSportsPointUpdate(unionId, clubId, value, type);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execBackstageSportsPointUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 后台更新指定赛事竞技点
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execSportsPointUpdate")
    public void execSportsPointUpdate(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            double value = HttpUtils.getDouble(dataJson, "value");
            int type = HttpUtils.getInt(dataJson, "type");
            long opPid = HttpUtils.getLong(dataJson, "opPid");
            long exePid = HttpUtils.getLong(dataJson, "exePid");
            long opClubId = HttpUtils.getLong(dataJson, "opClubId");

            try {
                SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execBackstageSportsPointUpdateClub(unionId, clubId, value, type, opPid, exePid, opClubId);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execBackstageSportsPointUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 关闭游戏房间
     */
    @RequestMapping(uri = "/onCreateUnion")
    public void onCreateUnion(HttpRequest request, HttpResponse response) throws Exception {
        try {
            String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
            JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
            this.onCreateUnion(response, dataJson);
        } catch (Exception e) {
            CommLogD.error("createClub msg = " + e.toString());
        }
    }

    /**
     * 掌乐后台创建赛事信息
     */
    @SuppressWarnings("rawtypes")
    public void onCreateUnion(HttpResponse response, JsonObject dataJson) throws Exception {
        try {
            CommLogD.info("=====ClubRequest onCreateUnion()收到回调===：{}", dataJson);
            synchronized (Lock) {
                // 亲友圈Id
                long clubId = HttpUtils.getLong(dataJson, "clubId");
                // 赛事名称
                String unionName = HttpUtils.getString(dataJson, "unionName");
                // 亲友圈创建者pid
                long pid = HttpUtils.getLong(dataJson, "pid");
                // 加入申请(0需要审核、1不需要审核)
                int join = HttpUtils.getInt(dataJson, "join");
                // 退出申请(0需要审核、1不需要审核)
                int quit = HttpUtils.getInt(dataJson, "quit");
                // 裁判力度
                double initSports = HttpUtils.getDouble(dataJson, "initSports");
                // 比赛频率（30天，7天，每天）
                int matchRate = HttpUtils.getInt(dataJson, "matchRate");
                // 赛事淘汰
                double outSports = HttpUtils.getDouble(dataJson, "outSports");
                // 消耗类型(1-金币,2-房卡)
                int prizeType = HttpUtils.getInt(dataJson, "prizeType");
                // 排名前50名
                int ranking = HttpUtils.getInt(dataJson, "ranking");
                // 数量
                int value = HttpUtils.getInt(dataJson, "value");

                Player player = PlayerMgr.getInstance().getPlayer(pid);
                if (null == player) {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "pid error " + pid));
                    return;
                }

                // 检查是否公会代理
                SData_Result result = player.getFeature(PlayerFamily.class).checkFamilyOwner();
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                }
                Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
                if (clubId <= 0L || null == club) {
                    response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CUnionCreate CLUB_NOT_EXIST"));
                    return;
                }
                // 不是亲友圈创建者
                if (club.getClubListBO().getOwnerID() != player.getPid()) {
                    response.response(ZleData_Result.make(ErrorCode.CLUB_NOT_CREATE, "CUnionCreate CLUB_NOT_CREATE :" + club.getClubListBO().getOwnerID()));
                    return;
                }

                FamilyItem item = (FamilyItem) result.getData();
                if (item.getPower() != 1) {
                    response.response(ZleData_Result.make(ErrorCode.FAMILY_POWER_ERROR, "FAMILY_POWER_ERROR"));
                    return;
                }


                CUnion_Create create = new CUnion_Create();
                create.setClubId(clubId);
                create.setUnionName(unionName);
                create.setJoin(join);
                create.setQuit(quit);
                create.setInitSports(initSports);
                create.setMatchRate(matchRate);
                create.setOutSports(outSports);
                create.setPrizeType(prizeType);
                create.setRanking(ranking);
                create.setValue(value);
                result = UnionMgr.getInstance().getUnionListMgr().onUnionCreateTest(create, club, player);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            CommLogD.error("onCreateUnion Exception e.msg" + e.toString());
        }

    }


    /**
     * 邀请添加赛事成员
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execJoinClubSameUnion")
    public void execJoinClubSameUnion(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            long pid = HttpUtils.getLong(dataJson, "pid");
            int type = HttpUtils.getInt(dataJson, "type");
//            if (UnionMgr.getInstance().getUnionMemberMgr().isNotManage(pid, clubId, unionId)) {
//                response.response(ZleData_Result.make(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE"));
//                return;
//            }
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(unionId);
            if (Objects.isNull(union)) {
                response.response(ZleData_Result.make(ErrorCode.UNION_NOT_EXIST, "pid error " + pid));
                return;
            }
            try {
                union.getUnionBO().saveJoinClubSameUnion(pid, type);
                response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
            } catch (Exception e) {
                CommLogD.error("execInsertUnionMember error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 操作解散赛事
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execUnionDissolve")
    public void execUnionDissolve(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            long exePid = HttpUtils.getLong(dataJson, "pid");
            Player player = PlayerMgr.getInstance().getPlayer(exePid);
            if (Objects.isNull(player)) {
                response.response(ZleData_Result.make(ErrorCode.NotAllow, "pid error " + exePid));
                return;
            }
            CUnion_Dissolve req = new CUnion_Dissolve();
            req.setUnionId(unionId);
            req.setClubId(clubId);
            try {
                SData_Result result = UnionMgr.getInstance().execUnionDissolve(req, player);
                if (ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                }
            } catch (Exception e) {
                CommLogD.error("execUnionDissolve error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 操作退出赛事
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execUnionQuit")
    public void execUnionQuit(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            long clubId = HttpUtils.getLong(dataJson, "clubId");
            long exePid = HttpUtils.getLong(dataJson, "pid");
            CUnion_Dissolve req = new CUnion_Dissolve();
            req.setUnionId(unionId);
            req.setClubId(clubId);
            try {
                SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execUnionQuit(req, exePid);
                if (ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                }
            } catch (Exception e) {
                CommLogD.error("execUnionQuit error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 设置显示断开连接
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/unionShowLostConnect")
    public void unionShowLostConnect(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionSign = HttpUtils.getLong(dataJson, "unionId");
            int type = HttpUtils.getInt(dataJson, "type");
            try {
                SData_Result result = UnionMgr.getInstance().showLostConnect(unionSign, type);
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
     * 后台更改保险箱状态
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execUnionCaseStatus")
    public void execUnionCaseStatus(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            int exePid = HttpUtils.getInt(dataJson, "exePid");
            int type = HttpUtils.getInt(dataJson, "type");
            try {
                SData_Result result = UnionMgr.getInstance().getUnionListMgr().changeCaseStatus(unionId, exePid, type);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execBackstageSportsPointUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 后台更改联赛状态
     * 联赛类型 0 正常 1中至
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execUnionType")
    public void execUnionType(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
//            int exePid = HttpUtils.getInt(dataJson, "exePid");
            int type = HttpUtils.getInt(dataJson, "type");
            try {
                SData_Result result = UnionMgr.getInstance().getUnionListMgr().changeUnionType(unionId, type);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execBackstageSportsPointUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 后台更改变换盟主状态
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/changeAllyLeader")
    public void changeAllyLeader(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            int type = HttpUtils.getInt(dataJson, "type");
            try {
                SData_Result result = UnionMgr.getInstance().getUnionListMgr().changeAllyLeader(unionId, type);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execBackstageSportsPointUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 后台更改审核功能状态
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/execUnionExamineStatus")
    public void execUnionExamineStatus(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            int exePid = HttpUtils.getInt(dataJson, "exePid");
            int type = HttpUtils.getInt(dataJson, "type");
            try {
                SData_Result result = UnionMgr.getInstance().getUnionListMgr().changeExamineStatus(unionId, exePid, type);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execBackstageSportsPointUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

    /**
     * 解散赛事房间
     */
    @RequestMapping(uri = "/dissolveUnionRoom")
    public void dissolveUnionRoom(HttpRequest request, HttpResponse response) throws Exception {
        try {
            String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
            JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            MqProducerMgr.get().send(MqTopic.UNION_DISSOLVE_INIT_ROOM, new MqUnionDissolveInitRoomBo(0, unionId));
            response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
        } catch (Exception e) {
            CommLogD.error("dissolveUnionRoom msg = " + e.toString());
        }
    }

    /**
     * 隐藏功能(0 关闭 1开启)
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/changeHideStatus")
    public void changeHideStatus(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long unionId = HttpUtils.getLong(dataJson, "unionId");
            int type = HttpUtils.getInt(dataJson, "type");
            try {
                SData_Result result = UnionMgr.getInstance().getUnionListMgr().changeHideStatus(unionId, type);
                if (!ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                    return;
                } else {
                    response.response(ZleData_Result.make(ErrorCode.Success, "OK"));
                }
            } catch (Exception e) {
                CommLogD.error("execBackstageSportsPointUpdate error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }
}
