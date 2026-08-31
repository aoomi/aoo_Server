package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.player.Player;
import cenum.Page;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.club.ClubPlayerRoomAloneData;
import jsproto.c2s.iclass.club.CClub_SChoolReport;
import jsproto.c2s.iclass.club.SClub_SchoolReportIncludePage;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.Collections;

/**
 * 战绩统计成绩单
 *
 * @author Administrator
 */
public class CClubSChoolReport extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_SChoolReport req = new Gson().fromJson(message, CClub_SChoolReport.class);

        // 检查当前操作者是否管理员、创建者。
        if (!ClubMgr.getInstance().getClubMemberMgr().isMinister(req.getClubId(), player.getPid())) {
            request.error(ErrorCode.NotAllow, "not club admin ClubID:{}", req.getClubId());
            return;
        }
        // 获取指定的亲友圈信息
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (null == club) {
            request.error(ErrorCode.NotAllow, "null == club ClubID:{}", req.getClubId());
            return;
        }
        ClubPlayerRoomAloneData clubPlayerRoomAloneData = ClubMgr.getInstance().getClubRankMgr().schoolReport(req.getClubId(), req.getUnionId(), req.getRoomIDList(), req.isAll(), req.getGetType(), req.getPageNum(), req.getQuery());
        int pageNum = clubPlayerRoomAloneData.getTotal().intValue() % Page.PAGE_SIZE_8 == 0 ? clubPlayerRoomAloneData.getTotal().intValue() / Page.PAGE_SIZE_8 : clubPlayerRoomAloneData.getTotal().intValue() / Page.PAGE_SIZE_8 + 1;
        request.response(SClub_SchoolReportIncludePage.make(pageNum == 0 ? 1 : pageNum, CollectionUtils.isEmpty(clubPlayerRoomAloneData.getLogBOList()) ? Collections.emptyList() : clubPlayerRoomAloneData.getLogBOList()));

    }

}
