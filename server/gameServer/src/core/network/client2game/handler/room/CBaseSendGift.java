package core.network.client2game.handler.room;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.player.Player;
import business.player.feature.PlayerCurrency;
import cenum.ItemFlow;
import cenum.PrizeType;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.config.refdata.RefDataMgr;
import core.config.refdata.ref.RefGift;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CBase_SendGift;
import jsproto.c2s.iclass.mj._SendGift;

import java.io.IOException;

public abstract class CBaseSendGift extends PlayerHandler {
			
	@Override			
	public void handle(Player player, WebSocketRequest request, String message)			
			throws WSException, IOException {			
    	final CBase_SendGift req = new Gson().fromJson(message, CBase_SendGift.class);			
    	long roomID = req.roomID;			
			
			
		AbsBaseRoom baseRoom = RoomMgr.getInstance().getRoom(roomID);			
    	if (null == baseRoom){			
    		request.error(ErrorCode.NotAllow, "CBaseSendGift not find room:"+roomID);
    		return;			
    	}			
		if (baseRoom.isNotGift()) {			
			request.error(ErrorCode.Not_Send_Gift, "Not_Send_Gift not find room:"+roomID);			
			return;			
		}			
			
		AbsRoomPos roomPos = baseRoom.getRoomPosMgr().getPosByPosID(req.pos);			
    	if (null == roomPos) {			
    		request.error(ErrorCode.NotAllow, "CBaseSendGift not find pos player in this room pos:"+req.pos);
    		return;			
		}			
    				
    	RefGift data = RefDataMgr.get(RefGift.class, req.productId);			
    	if (null == data){			
    		request.error(ErrorCode.NotAllow, "CBaseSendGift not find productId:"+req.productId);
    		return;			
    	}			
			
    	AbsRoomPos sendRoomPos = baseRoom.getRoomPosMgr().getPosByPid(player.getPid());			
    	if(null == sendRoomPos){			
    		request.error(ErrorCode.NotAllow, "CBaseSendGift not find pid player in this room pid:"+player.getPid());
    		return;			
    	}			
			
        if(data.Num > 0) {			
	        if(!player.getFeature(PlayerCurrency.class).checkAndConsumeItemFlow(PrizeType.valueOf(data.Type), data.Num,  ItemFlow.SendGift)){			
	            request.error(ErrorCode.NotAllow, "CBaseSendGift your money not enought  type:"+ PrizeType.valueOf(data.Type) +",num "+data.Num);
	            return;			
	        }			
        }			
        request.response();			
	 	baseRoom.getRoomPosMgr().notify2All(_SendGift.make(req.roomID, sendRoomPos.getPosID(), req.pos, req.productId,
	 			baseRoom.getBaseRoomConfigure().getGameType().getName()));
	}			
			
			
}			
			
