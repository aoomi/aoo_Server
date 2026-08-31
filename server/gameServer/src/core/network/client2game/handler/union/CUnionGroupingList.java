package core.network.client2game.handler.union;

import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.union.CUnion_GroupParam;

import java.io.IOException;

/**
 * 赛事分组列表
 *
 * @author zaf
 */
public class CUnionGroupingList extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_GroupParam req = new Gson().fromJson(message, CUnion_GroupParam.class);
        // 	“禁止同桌”页签赛事主裁判、副裁判、加盟亲友圈创建者（圈主）可见；
        if (!UnionMgr.getInstance().getUnionMemberMgr().anyMatchUnionOrUnionMgr(req.getClubId(), req.getUnionId(), player.getPid())) {
            request.error(ErrorCode.NotAllow, "not CUnionGroupingList admin ClubID:{%d}", req.getClubId());
            return;
        }
        // 获取指定的亲友圈信息
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (null == union) {
            request.error(ErrorCode.NotAllow, "null == club ClubID:{%d}", req.getUnionId());
            return;
        }
        // 获取玩家分组列表
        request.response(union.getUnionGroupingInfoList(req));
    }
}
