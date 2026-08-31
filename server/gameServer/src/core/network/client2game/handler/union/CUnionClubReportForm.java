package core.network.client2game.handler.union;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.union.CUnion_ScorePercentList;

import java.io.IOException;
import java.util.Objects;

/**
 * 赛事亲友圈圈主报表
 */
public class CUnionClubReportForm extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CUnion_ScorePercentList req = new Gson().fromJson(message, CUnion_ScorePercentList.class);
        UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.nonNull(club)) {
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
            if (Objects.nonNull(union)) {
                unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
            }
        }
        if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
            SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execUnionClubReportForm(req, player.getPid());
            if (ErrorCode.Success.equals(result.getCode())) {
                request.response(result.getData());
            } else {
                request.error(result.getCode(), result.getMsg());
            }
        } else if (UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType)) {
            SData_Result result = UnionMgr.getInstance().getUnionMemberMgr().execUnionClubReportFormZhongZhi(req, player.getPid());
            if (ErrorCode.Success.equals(result.getCode())) {
                request.response(result.getData());
            } else {
                request.error(result.getCode(), result.getMsg());
            }
        } else {
            request.response(SData_Result.make(ErrorCode.UNION_TYPE_NOT_EXIST));
        }

    }
}
