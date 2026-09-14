package core.network.client2game.handler.cdxzmj;				
				
import java.io.IOException;				
import java.util.List;				
import java.util.stream.Collectors;				
				
import com.ddm.server.websocket.exception.WSException;				
import com.ddm.server.websocket.handler.requset.WebSocketRequest;				
				
import business.player.Player;				
import core.config.refdata.RefDataMgr;				
import core.config.refdata.ref.RefPractice;				
import core.network.client2game.handler.PlayerHandler;				
import core.server.cdxzmj.CDXZMJAPP;				
import jsproto.c2s.cclass.room.GoldItem;				
import jsproto.c2s.iclass.room.SGold_GameList;				
				
/**				
 * 练习场游戏列表				
 * 				
 * @author Huaxing				
 *				
 */				
public class CCDXZMJGoldList extends PlayerHandler {				
				
	@Override				
	public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {				
		List<GoldItem> goldItem = RefDataMgr.getAll(RefPractice.class).values().stream()				
				.filter(k -> CDXZMJAPP.GameType().getName().equals(k.getGameType().toUpperCase()))				
				.map(k -> new GoldItem(k.getId(), (int) (Math.random() * 9 + 1) + 100 + (int) (Math.random() * 100 + 1))).collect(Collectors.toList());				
    	request.response(SGold_GameList.make(goldItem));				
	}				
}				
