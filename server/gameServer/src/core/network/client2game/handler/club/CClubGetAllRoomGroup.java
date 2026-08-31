package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.room.RoomInfoItem;
import jsproto.c2s.iclass.club.CClub_GetAllRoom;
import jsproto.c2s.iclass.club.SClub_GetAllRoomGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 获取俱乐部房间
 *
 * @author zaf
 */
public class CClubGetAllRoomGroup extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_GetAllRoom req = new Gson().fromJson(message, CClub_GetAllRoom.class);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (null == club) {
            request.error(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
            return;
        }
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().checkExistUnion(club);
        if (!ErrorCode.NotAllow.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            request.error(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
            return;
        }

        request.response();
        if (clubMember.isBanGame()) {
            return;
        }
        //先初步排序 亲友圈默认为1
        List<RoomInfoItem> roomInfoItems = ClubMgr.getInstance().onClubGetAllRoom(req.getClubId(), req.getPageNum()).stream().sorted((x, y) -> x.compareTo1(y)).collect(Collectors.toList());
        List<RoomInfoItem> groupList = new ArrayList<>();
        int pageNum = 0;
        for (int i = 0; i < roomInfoItems.size(); i++) {
            groupList.add(roomInfoItems.get(i));
            //满十条的时候就发一条消息给客户端
            if (groupList.size() == 10) {
                pageNum++;
                player.pushProto(SClub_GetAllRoomGroup.make(club.getClubListBO().getId(), groupList, pageNum, i == roomInfoItems.size()));
                groupList.clear();
            }
        }
        //没有刚好的十条 最后一个消息
        if (groupList.size() != 0) {
            pageNum++;
            player.pushProto(SClub_GetAllRoomGroup.make(club.getClubListBO().getId(), groupList, pageNum, true));
        }

    }

}
