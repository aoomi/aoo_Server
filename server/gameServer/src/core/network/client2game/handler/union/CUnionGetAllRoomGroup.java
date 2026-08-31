package core.network.client2game.handler.union;

import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.union.Union;
import business.global.union.UnionMember;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.room.RoomInfoItem;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.union.CUnion_GetAllRoom;
import jsproto.c2s.iclass.union.SUnion_GetAllRoomGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 获取赛事房间
 *
 * @author zaf
 */
public class CUnionGetAllRoomGroup extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_GetAllRoom req = new Gson().fromJson(message, CUnion_GetAllRoom.class);
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            request.error(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
            return;
        }
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(req.getClubId(), req.getUnionId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == unionMember) {
            request.error(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER");
            return;
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            request.error(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
            return;
        }

        request.response();
        //	被禁止游戏的玩家（赛事、亲友圈）在进入房间列表时，需显示出房间列表的桌子，点击桌子时返回通用提示框：“您已被禁止游戏，请联系管理”;
//        if (clubMember.isBanGame()) {
//            return;
//        }
        if (clubMember.isUnionBanGame()) {
            return;
        }
        //获取排序方式
        int sort = union.getUnionBO().getSort();
        //先初步排序
        List<RoomInfoItem> roomInfoItems = UnionMgr.getInstance().onUnionGetAllRoom(req.getUnionId(), clubMember.getClubMemberBO().getUnionNotGameList(), clubMember.getClubMemberBO().getIsHideStartRoom(), req.getPageNum(), union.getUnionBO().getSort(), req.getGameType()).
                stream().collect(Collectors.toList());
        List<RoomInfoItem> roomInfoItemsSorted = new ArrayList<>();
        switch (sort) {
            case 0:
                roomInfoItemsSorted = roomInfoItems.stream().sorted((x, y) -> x.compareTo(y)).collect(Collectors.toList());
                break;
            case 1:
                roomInfoItemsSorted = roomInfoItems.stream().sorted((x, y) -> x.compareTo1(y)).collect(Collectors.toList());
                break;
            case 2:
                roomInfoItemsSorted = roomInfoItems.stream().sorted((x, y) -> x.compareTo2(y)).collect(Collectors.toList());
                break;
            case 3:
                roomInfoItemsSorted = roomInfoItems.stream().sorted((x, y) -> x.compareTo3(y)).collect(Collectors.toList());
                break;

        }
        List<RoomInfoItem> groupList = new ArrayList<>();

        int pageNum = 0;
        for (int i = 0; i < roomInfoItems.size(); i++) {
            groupList.add(roomInfoItems.get(i));
            //满十条的时候就发一条消息给客户端
            if (groupList.size() == 10) {
                pageNum++;
                player.pushProto(SUnion_GetAllRoomGroup.make(req.getClubId(), req.getUnionId(), groupList, pageNum, i == roomInfoItems.size()));
                groupList.clear();
            }
        }
        //没有刚好的十条 最后一个消息
        if (groupList.size() != 0) {
            pageNum++;
            player.pushProto(SUnion_GetAllRoomGroup.make(req.getClubId(), req.getUnionId(), groupList, pageNum, true));
        }
    }

}
