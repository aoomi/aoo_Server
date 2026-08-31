package core.network.client2game.handler.union;

import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.union.CUnion_GroupingPid;

import java.io.IOException;
import java.util.Objects;

/**
 * 赛事分组成员列表
 *
 * @author zaf
 */
public class CUnionGroupingMemberList extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_GroupingPid req = new Gson().fromJson(message, CUnion_GroupingPid.class);
        // 检查权限是否足够
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotUnionManage(player.getPid(), req.getClubId(), req.getUnionId())) {
            request.error(ErrorCode.UNION_NOT_MANAGE, "CUnionGroupingRemove not club admin ClubID:{%d},UnionId:{%d}", req.getClubId(), req.getUnionId());
            return;
        }
        // 获取指定的亲友圈信息
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            request.error(ErrorCode.UNION_NOT_EXIST, "CUnionGroupingPidRemove null == club ClubID:{%d},UnionId:{%d}", req.getClubId(), req.getUnionId());
            return;
        }
        // 获取玩家分组列表
        request.response(union.getUnionGroupingMemberList(req.getGroupingId()));
    }
}
