package core.network.client2game.handler.union;

import business.global.union.UnionMember;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.union.CUnion_ScorePercentBatchUpdate;

import java.io.IOException;

/**
 * 执行收益比例批量更新
 */
public class CUnionScorePercentBatchUpdate extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CUnion_ScorePercentBatchUpdate req = new Gson().fromJson(message, CUnion_ScorePercentBatchUpdate.class);
        SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execScorePercentBatchUpdate(req, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
            // 设置积分比例修改
            ((UnionMember) result.getData()).execUpdateUnionRoomConfigScorePercent(req, player.getPid());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
