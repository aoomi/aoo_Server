package core.network.client2game.handler.union;

import business.global.club.ClubMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_GetMemberManage;

import java.io.IOException;

/**
 * 指定亲友圈成员的推广员树列表
 * 赛事所用
 *
 * @author zaf
 */
public class CUnionGetMembePromotionList extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_GetMemberManage req = new Gson().fromJson(message, CUnion_GetMemberManage.class);
        SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().checkUnionGetMemberPromotionList(req, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            result = ClubMgr.getInstance().getClubMemberMgr().getUnionMemberPromotionList(req, player.getPid());
            if (ErrorCode.Success.equals(result.getCode())) {
                request.response(result.getData());
            } else {
                request.error(result.getCode(), result.getMsg());
            }
        } else {
            request.error(result.getCode(), result.getMsg());
            return;
        }

    }

}
