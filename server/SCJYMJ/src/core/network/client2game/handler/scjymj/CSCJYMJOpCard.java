package core.network.client2game.handler.scjymj;

import business.global.mj.scjymj.SCJYMJRoom;
import business.global.mj.set.MJOpCard;
import business.global.room.RoomMgr;
import business.player.Player;
import cenum.mj.OpType;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.mj.CMJ_OpCard;

import java.io.IOException;

/**
 * 济宁打牌
 *
 * @author Huaxing
 */
public class CSCJYMJOpCard extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CMJ_OpCard req = new Gson().fromJson(message, CMJ_OpCard.class);
        long roomID = req.roomID;
        OpType opType = OpType.valueOf(req.opType);


        SCJYMJRoom room = (SCJYMJRoom) RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CSCJYMJOpCard not find room:" + roomID);
            return;
        }

        SData_Result result = room.opCard(request, player.getId(), req.setID, req.roundID, opType, MJOpCard.OpCard(req.cardID));
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
