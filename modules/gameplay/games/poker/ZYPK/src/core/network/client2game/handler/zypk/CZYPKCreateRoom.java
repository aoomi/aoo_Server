package core.network.client2game.handler.zypk;

import java.io.IOException;

import jsproto.c2s.cclass.BaseRoomConfigure;
import jsproto.c2s.cclass.zypk.ZYPKRoom_Cfg;
import jsproto.c2s.iclass.zypk.CZYPK_CreateRoom;
import business.global.room.base.RoomClassMgr;
import business.player.Player;
import business.player.feature.PlayerRoom;
import cenum.CEnum.GameType;
import cenum.PrizeType;

import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;

import core.network.client2game.handler.PlayerHandler;

/**
 * 自由扑克
 * @author Huaxing
 *
 */
public class CZYPKCreateRoom extends PlayerHandler{

	@Override
	public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
		final CZYPK_CreateRoom clientPack = new Gson().fromJson(message, CZYPK_CreateRoom.class);
    	
		final ZYPKRoom_Cfg roomCfg= (ZYPKRoom_Cfg) RoomClassMgr.getInstance().RoomCfgAbstract(clientPack,new ZYPKRoom_Cfg());

		roomCfg.chouMa = clientPack.chouMa;
		roomCfg.chuPais = clientPack.chuPais;
		roomCfg.liuPai = clientPack.liuPai;
		roomCfg.liPai = clientPack.liPai;
		roomCfg.anNius = clientPack.anNius;
		roomCfg.zhuagnJia = clientPack.zhuangJia;
		roomCfg.xuanZhuang = clientPack.xuanZhuang;
        roomCfg.kongPai = clientPack.kongPai;
        roomCfg.moShi = clientPack.moShi;
        roomCfg.setCount = 100;
        
    	BaseRoomConfigure bRoomConfigure = new BaseRoomConfigure();
    	bRoomConfigure.setGameType(GameType.ZYPK);
    	bRoomConfigure.setPrizeType(PrizeType.RoomCard);
    	bRoomConfigure.setContinue(clientPack.isContinue);
		bRoomConfigure.setRoomCfg(roomCfg);	
		

		bRoomConfigure = RoomClassMgr.getInstance().RoomConfigure(bRoomConfigure);
		if (null == bRoomConfigure) {
			request.error(ErrorCode.NotAllow, "bRoomConfigure not null.");
			return;
		}
		player.getFeature(PlayerRoom.class).createRoom(request, bRoomConfigure);

    	
	}

}
