package core.network.client2game.handler.club;

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
import jsproto.c2s.iclass.club.CClub_FindPIDAdd;

import java.io.IOException;
import java.util.Objects;

/**
 * 获取推广员报表
 */
public class CClubPromotionLevelReportForm extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        CClub_FindPIDAdd req = new Gson().fromJson(message, CClub_FindPIDAdd.class);
        UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.nonNull(club)) {
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
            if (Objects.nonNull(union)) {
                unionType = UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType());
            }
        }
        if (UnionDefine.UNION_TYPE.NORMAL.equals(unionType)) {
            SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getPromotionLevelReportForm(req, player.getPid());
            if (ErrorCode.Success.equals(result.getCode())) {
                request.response(result.getData());
            } else {
                request.error(result.getCode(), result.getMsg());
            }
        } else if (UnionDefine.UNION_TYPE.ZhongZhi.equals(unionType)) {
            SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getPromotionLevelReportFormZhongZhi(req, player.getPid());
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
