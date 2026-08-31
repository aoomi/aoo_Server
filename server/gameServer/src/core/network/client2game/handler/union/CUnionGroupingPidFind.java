package core.network.client2game.handler.union;

import business.global.club.ClubMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.union.CUnion_GroupingPid;

import java.io.IOException;
import java.util.Objects;

/**
 * 赛事成员查询
 *
 * @author zaf
 */
public class CUnionGroupingPidFind extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_GroupingPid req = new Gson().fromJson(message, CUnion_GroupingPid.class);
        // 检查权限是否足够
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotUnionManage(player.getPid(), req.getClubId(), req.getUnionId())) {
            request.error(ErrorCode.UNION_NOT_MANAGE, "CUnionGroupingPidFind not club admin ClubID:{%d},UnionId:{%d}", req.getClubId(), req.getUnionId());
            return;
        }

        if (!ClubMgr.getInstance().getClubMemberMgr().checkExistByPidMember(UnionMgr.getInstance().getUnionMemberMgr().getUnionToClubIdList(req.getUnionId()), req.getPid())) {
            request.error(ErrorCode.UNION_NOT_EXIST_MEMBER, "CUnionGroupingPidFind not club admin ClubID:{%d},UnionId:{%d}", req.getClubId(), req.getUnionId());
            return;
        }

        Player toPlayer = PlayerMgr.getInstance().getPlayer(req.getPid());
        if (Objects.isNull(toPlayer)) {
            request.error(ErrorCode.Player_PidError, "CUnionGroupingPidFind toPlayer ClubID:{%d},UnionId:{%d}", req.getClubId(), req.getUnionId());
            return;
        }
        request.response(toPlayer.getShortPlayer());
    }
}
