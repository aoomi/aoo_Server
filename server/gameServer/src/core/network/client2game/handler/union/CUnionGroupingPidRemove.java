package core.network.client2game.handler.union;

import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_GroupingPid;
import jsproto.c2s.iclass.union.SUnion_GroupingInfo;

import java.io.IOException;
import java.util.Objects;

/**
 * 赛事分组移除
 *
 * @author zaf
 */
public class CUnionGroupingPidRemove extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_GroupingPid req = new Gson().fromJson(message, CUnion_GroupingPid.class);
        // 检查权限是否足够
        SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().checkUnionRights(req.getUnionId(), req.getClubId(), player.getPid(), req.getPid());
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }
        // 获取指定的亲友圈信息
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            request.error(ErrorCode.UNION_NOT_EXIST, "CUnionGroupingPidRemove null == club ClubID:{%d},UnionId:{%d}", req.getClubId(), req.getUnionId());
            return;
        }

        Player opPlayer = PlayerMgr.getInstance().getPlayer(req.getPid());
        if (null == opPlayer) {
            request.error(ErrorCode.Player_PidError, "removeClubGroupingPid null == opPlayer  ClubID:{%d},UnionId:{%d}", req.getClubId(), req.getUnionId());
            return;
        }
        result = union.removeUnionGroupingPid(req.getGroupingId(), req.getPid());
        // 将玩家添加到指定组中。
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }
        request.response(SUnion_GroupingInfo.make(req.getUnionId(), req.getClubId(), req.getGroupingId(), opPlayer.getShortPlayer()));
    }
}
