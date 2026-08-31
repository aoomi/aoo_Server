package core.network.client2game.handler.union;

import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.db.entity.clarkGame.UnionDynamicBO;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.union.CUnion_CreateGameSetChange;

import java.io.IOException;

/**
 * 获取游戏设置
 *
 * @author zaf
 */
public class CUnionCreateGameSetChangge extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_CreateGameSetChange req = new Gson().fromJson(message, CUnion_CreateGameSetChange.class);
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotManage(player.getPid(), req.getClubId(), req.getUnionId())) {
            // 	此按钮仅盟主、联盟管理可用；
            request.error(ErrorCode.UNION_NOT_MANAGE, "CUnionCreateGameSetChangge UNION_NOT_MANAGE pid:{%d},clubId:{%d},unionId:{%d}", player.getPid(), req.getClubId(), req.getUnionId());
            return;
        }
        // 获取赛事信息
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (null == union) {
            request.error(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
            return;
        }
        if (!union.getRoomConfigBOMap().containsKey((long) req.getUnionRoomCfgId())) {
            // 配置不存在
            request.error(ErrorCode.UNION_NOT_EXIST_ROOM_CFG_ID, "UNION_NOT_EXIST_ROOM_CFG_ID");
            return;
        }
        SData_Result result = union.createGameSetChange(req.getUnionRoomCfgId(), req.getStatus());
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }
        request.response();
        if (UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_DELETE.value() == req.getStatus()) {
            UnionDynamicBO.insertUnionGameConfig(player.getPid(), req.getClubId(), req.getUnionId(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_DISMISS_ROOM.value());
        }
    }

}
