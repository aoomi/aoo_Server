package core.network.client2game.handler.scjymj;

import business.player.Player;
import business.player.feature.PlayerGoldRoom;
import business.scjymj.c2s.iclass.CSCJYMJ_CreateRoom;
import cenum.PrizeType;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.config.refdata.RefDataMgr;
import core.config.refdata.ref.RefPractice;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import core.server.scjymj.SCJYMJAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.RobotRoomConfig;
import jsproto.c2s.iclass.room.CBase_GoldRoom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 创建房间
 *
 * @author Administrator
 */
public class CSCJYMJGoldRoom extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {

        final CBase_GoldRoom clientPack = new Gson().fromJson(message, CBase_GoldRoom.class);
        RefPractice data = RefDataMgr.get(RefPractice.class, clientPack.getPracticeId());
        if (data == null) {
            request.error(ErrorCode.NotAllow, "CSCJYMJGoldRoom do not find practiceId");
            return;
        }
        // 游戏配置
        CSCJYMJ_CreateRoom createClientPack = new CSCJYMJ_CreateRoom();
        createClientPack.setPlayerNum(4);
        createClientPack.paishu = 0;//两牌房
        createClientPack.hutype = 0;//自摸加翻
        createClientPack.fengDing = 2;//4番
        createClientPack.piao = 2;//飘在内（坐飘）
        createClientPack.other = new ArrayList<>(Arrays.asList(0)); //庄闲玩法不勾
        createClientPack.setKexuanwanfa(new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8))); //可选玩法全钩起来
        createClientPack.setXianShi(1); //15秒
        // 公共房间配置
        BaseRoomConfigure<CSCJYMJ_CreateRoom> configure = new BaseRoomConfigure<CSCJYMJ_CreateRoom>(PrizeType.Gold,
                SCJYMJAPP.GameType(), createClientPack.clone(), new RobotRoomConfig(data.getBaseMark(), data.getMin(), data.getMax(), clientPack.getPracticeId()));
        SData_Result resule = player.getFeature(PlayerGoldRoom.class).createAndQuery(configure);
        if (ErrorCode.Success.equals(resule.getCode())) {
            request.response(resule.getData());
        } else {
            request.error(resule.getCode(), resule.getMsg());
        }
    }
}
