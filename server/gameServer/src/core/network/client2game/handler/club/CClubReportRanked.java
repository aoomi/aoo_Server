package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_RankedZhongZhi;

import java.io.IOException;

/**
 * 战绩统计成绩单
 *
 * @author Administrator
 */
public class CClubReportRanked extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_RankedZhongZhi req = new Gson().fromJson(message, CClub_RankedZhongZhi.class);
        SData_Result result = ClubMgr.getInstance().getClubRankMgr().rankedZhongZhi(req.getClubId(), req.getUnionId(), req.getGetType(), req.getPageNum(), req.getType(), req.getGameType(), player);
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result);
        } else {
            request.error(result.getCode(), "CClubReportRanked error");
        }


    }

}
