package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.ItemFlow;
import cenum.RoomTypeEnum;
import cenum.VisitSignEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.db.entity.clarkGame.UnionDynamicBO;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.club.CClub_PlayerStatusChange;

import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

/**
 * 改变俱乐部玩家状态
 *
 * @author zaf
 */
public class CClubChangePlayerStatus extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_PlayerStatusChange req = new Gson().fromJson(message, CClub_PlayerStatusChange.class);

        ClubMember myClubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.clubId);
        if (null == myClubMember) {
            request.error(ErrorCode.NotAllow, "CClubChangePlayerStatus not find myClubMember");
            return;
        }
        //检查时间  凌晨0点到2点不允许修改从属
        if (Club_Player_Status.PLAYER_TICHU.value() == req.status) {
            Calendar ca = Calendar.getInstance();
            if (ca.get(Calendar.HOUR_OF_DAY) < 2) {
                request.error(ErrorCode.CLUB_MEMBER_TIME_ERROR, "CLUB_MEMBER_TIME_ERROR = " + ca.get(Calendar.HOUR));
                return;
            }
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        if (null == club) {
            request.error(ErrorCode.NotAllow, "CClubChangePlayerStatus not find club  clubId = " + req.clubId);
            return;
        }
        if (club.getClubListBO().getOwnerID() == req.pid) {
            request.error(ErrorCode.NotAllow, "CClubChangePlayerStatus club.getClubListBO().getOwnerID() == req.pid" + req.clubId);
            return;
        }

        if (player.getPid() == req.pid) {
            if (Club_Player_Status.PLAYER_JUJIEYAOQING.value() == req.status || Club_Player_Status.PLAYER_JIARU.value() == req.status
                    || /*Club_Player_Status.PLAYER_TICHU.value() == req.status ||*/ Club_Player_Status.PLAYER_TUICHU.value() == req.status) {
                request.response();

                // 存在竞技点不为0
                if (checkExistSportsPointNotEqualZero(request, club, req.status, myClubMember)) {
                    return;
                }
                // 在游戏房间中
                if (checkExistRoom(request, club, req.status, myClubMember)) {
                    return;
                }
                if (Club_Player_Status.PLAYER_JIARU.value() == req.status) {
                    myClubMember.setStatus(player, club, req.status, myClubMember.getClubMemberBO().getInvitationPid(), req.audit);
                } else {
                    myClubMember.setStatus(player, club, req.status, req.pid, req.audit);
                }
            } else {
                request.error(ErrorCode.NotAllow, "CClubChangePlayerStatus you have not  permission");
                return;
            }
        } else {

            Player tempplayer = PlayerMgr.getInstance().getPlayer(req.pid);
            if (null == tempplayer) {
                request.error(ErrorCode.NotAllow, "CClubChangePlayerStatus not find player pid=" + req.pid);
                return;
            }

            if (myClubMember.isLevelPromotion() && myClubMember.getClubMemberBO().getKicking() == 1) {
                SData_Result result = ClubMgr.getInstance().getClubMemberMgr().checkExistPromotionSubordinateLevel(req.clubId, req.pid, player.getPid());
                if (!ErrorCode.Success.equals(result.getCode())) {
                    request.error(result.getCode(), result.getMsg());
                    return;
                }
            } else {
                if (!myClubMember.isMinister()) {
                    request.error(ErrorCode.CLUB_NOTMINISTER, "CClubChangePlayerStatus you have not minister");
                    return;
                }
            }

            ClubMember tempMember = ClubMgr.getInstance().getClubMemberMgr().find(req.pid, req.clubId);
            if (null == tempMember) {
                request.error(ErrorCode.NotAllow, "CClubChangePlayerStatus not find clubmember pid=" + req.pid + ", clubId = " + req.clubId);
                return;
            }
            // 存在竞技点不为0
            if (checkExistSportsPointNotEqualZero(request, club, req.status, tempMember)) {
                return;
            }

            //存在保险箱不为0
            if (checkExistCaseSportsPointNotEqualZero(request, club, req, myClubMember, tempMember, player)) {
                return;
            }
            // 在游戏房间中
            if (checkExistRoom(request, club, req.status, tempMember)) {
                return;
            }
            request.response();
            tempMember.setStatus(tempplayer, club, req.status, player.getPid(), req.audit);
        }
    }

    /**
     * @param request
     * @param club
     * @param status
     * @param clubMember
     * @return
     */
    private boolean checkExistRoom(WebSocketRequest request, Club club, int status, ClubMember clubMember) {
        SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(clubMember.getClubMemberBO().getPlayerID());
        if (Objects.isNull(sharePlayer)) {
            request.error(ErrorCode.Player_PidError, "clubMember.getClubMemberBO().getPlayerID()");
            return true;
        }
        //如果是踢人的话 判断这个玩家是否在亲友圈游戏中
        if (Club_Player_Status.PLAYER_JUJIE.value() == status || Club_Player_Status.PLAYER_TUICHU.value() == status
                || status == Club_Player_Status.PLAYER_TICHU.value()
                || status == Club_Player_Status.PLAYER_TICHU_CLOSE.value()
                || Club_Player_Status.PLAYER_JUJIEYAOQING.value() == status) {
            if (VisitSignEnum.ROOM.equals(sharePlayer.getSignEnum()) && sharePlayer.getRoomInfo().getClubId() == clubMember.getClubID()) {
                CommLogD.error("player is in room play game == playId:{}", sharePlayer.getPlayerBO().getId());
                request.error(ErrorCode.CLUB_MEMBER_ROOM_ERROR, "CLUB_EXIST_PROMOTION_TICHU_ERROR");
                return true;
            }
        }
        return false;
    }

    /**
     * 存在竞技点不为0
     *
     * @param request
     * @param club
     * @param status
     * @param ClubMember
     * @return
     */
    private boolean checkExistSportsPointNotEqualZero(WebSocketRequest request, Club club, int status, ClubMember ClubMember) {
        if (status == Club_Player_Status.PLAYER_TICHU.value()) {
            if (ClubMember.isPromotion()) {
                request.error(ErrorCode.CLUB_EXIST_PROMOTION_TICHU_ERROR, "CLUB_EXIST_PROMOTION_TICHU_ERROR");
                return true;
            }
        }
        if (club.getClubListBO().getUnionId() > 0L) {
            if (status == Club_Player_Status.PLAYER_TUICHU.value() || status == Club_Player_Status.PLAYER_TICHU.value()) {
                if (ClubMember.getClubMemberBO().getSportsPoint() != 0D) {
                    request.error(ErrorCode.UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO, "UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO");
                    return true;
                }
            }
        }
        Player player = PlayerMgr.getInstance().getPlayer(ClubMember.getClubMemberBO().getPlayerID());
        if (Objects.isNull(player)) {
            request.error(ErrorCode.Player_PidError, "Player_PidError");
            return true;
        }
        if (player.getRoomInfo().getRoomId() > 0L && player.getRoomInfo().getClubId() == club.getClubListBO().getId()) {
            request.error(ErrorCode.CLUB_MEMBER_ROOM_ERROR, "CLUB_MEMBER_ROOM_ERROR");
            return true;
        }
        return false;
    }

    /**
     * 存在保险箱不为0
     *
     * @param request
     * @param club
     * @return
     */
    private boolean checkExistCaseSportsPointNotEqualZero(WebSocketRequest request, Club club, CClub_PlayerStatusChange req, ClubMember myClubMember, ClubMember tempClubMember, Player player) {
        if (req.status != Club_Player_Status.PLAYER_TICHU.value()) {
            return false;
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
        if (Objects.isNull(union)) {
//			request.error(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
            return false;
        }
        if (tempClubMember.getClubMemberBO().getCaseSportsPoint() != 0D) {
            if (myClubMember.isClubCreate()) {
                SharePlayer tempPlayer = SharePlayerMgr.getInstance().getSharePlayer(req.pid);
                if (Objects.isNull(tempPlayer)) {
                    request.error(ErrorCode.Player_PidError, "Player_PidError");
                    return true;
                }
                double preCasePointValue = tempClubMember.getClubMemberBO().getCaseSportsPoint();
                double preSportPoint = tempClubMember.getSportsPoint();
                //保险箱的分数转移到竞技点上
                tempClubMember.getClubMemberBO().saveCaseSportsPoint(tempPlayer, tempClubMember.getClubMemberBO().getCaseSportsPoint(), UnionDefine.UNION_EXEC_TYPE.PLAYER_CASE_SPORTS_POINT_SUB, club.getClubListBO().getUnionId());
                final double finalValue = CommMath.FormatDouble(tempClubMember.getClubMemberBO().getSportsPointRedis());
                //被踢出的人比赛分清零
                tempClubMember.getClubMemberBO().execSportsPointClear(club.getClubListBO().getUnionId());
                //圈主比赛分增加
                myClubMember.getClubMemberBO().execSportsPointUpdate(club.getClubListBO().getUnionId(), finalValue, ItemFlow.CLUB_CASE_SPORTS_POINT_TICHU, RoomTypeEnum.CLUB, union.getUnionBO().getOutSports());
                // 比赛分清0添加竞技动态
                UnionDynamicBO.insertCaseSportsRecord(req.pid, player.getPid(), club.getClubListBO().getId(), CommTime.nowSecond(),
                        UnionDefine.UNION_EXEC_TYPE.UNION_CASE_SPORTS_POINT_TICHU.value(), club.getClubListBO().getUnionId(), String.valueOf(finalValue), String.valueOf(myClubMember.getClubMemberBO().getCaseSportsPoint()), String.valueOf(myClubMember.getClubMemberBO().getSportsPoint()), ""
                        , String.valueOf(preCasePointValue), String.valueOf(tempClubMember.getClubMemberBO().getCaseSportsPoint()), String.valueOf(-preCasePointValue), String.valueOf(tempClubMember.getSportsPoint()), String.valueOf(preSportPoint), String.valueOf(preCasePointValue));

            } else {
                request.error(ErrorCode.UNION_EXIST_CASE_SPORTS_POINT_NOT_EQUAL_ZERO, "UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO");
                return true;
            }

        }
        return false;

    }

}
