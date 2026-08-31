package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.room.NormalRoomMgr;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import cenum.RoomTypeEnum;
import cenum.room.RoomState;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.db.entity.clarkGame.UnionDynamicBO;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.ClubCreateGameSet;
import jsproto.c2s.cclass.club.Club_define.Club_CreateGameSetStatus;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.club.CClub_CreateGameSetChange;
import jsproto.c2s.iclass.club.SClub_CreateGameSetChange;

import java.io.IOException;
import java.util.Map;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class CClubCreateGameSetChangge extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_CreateGameSetChange req = new Gson().fromJson(message, CClub_CreateGameSetChange.class);

        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        if (null == club) {
            request.error(ErrorCode.NotAllow, "CClubGetCreateGameSet not find club clubId = " + req.clubId);
            return;
        }
        if (!club.getMCreateGamesetMap().containsKey(req.gameIndex)) {
            request.error(ErrorCode.NotAllow, "UnionCreateGameSet not find gameIndex = " + req.gameIndex);
            return;
        }
        if (req.status == Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value() && !club.checkCanCreateRoom()) {
            request.error(ErrorCode.CLUB_MAXCRATESET, "your set enough");
            return;
        }
        SData_Result result = club.createGameSetChange(req.gameIndex, req.status);
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }
        request.response();
        if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value() == req.status) {
            UnionDynamicBO.insertClubGameConfig(player.getPid(), req.clubId, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.Club_EXEC_DISMISS_ROOM.value());
        } else if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DISABLE.value() == req.status) {
            UnionDynamicBO.insertClubGameConfig(player.getPid(), req.clubId, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.Club_EXEC_BAN_ROOM.value());
        } else if (Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value() == req.status) {
            UnionDynamicBO.insertClubGameConfig(player.getPid(), req.clubId, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.Club_EXEC_NOMARL_ROOM.value());
        }
        ClubCreateGameSet createGameSet = (ClubCreateGameSet) result.getData();
        // 亲友圈房间类型分组
        Map<RoomState, Long> map;
        if (Config.isShare()) {
            map = ShareRoomMgr.getInstance().groupingBy(RoomTypeEnum.CLUB, club.getClubListBO().getId());
        } else {
            map = NormalRoomMgr.getInstance().groupingBy(RoomTypeEnum.CLUB, club.getClubListBO().getId());
        }
        ClubMgr.getInstance().getClubMemberMgr().notify2AllByClubMinister(req.clubId, SClub_CreateGameSetChange.make(req.clubId, player.getPid(), false, club.getClubCreateGameSetInfo(createGameSet),
                NormalRoomMgr.Value(map, RoomState.Init),
                NormalRoomMgr.Value(map, RoomState.Playing), club.getClubListBO().getMemberCreationRoom()));

    }

}
