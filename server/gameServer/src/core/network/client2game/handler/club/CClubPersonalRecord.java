package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.club.ClubRecordInfo;
import jsproto.c2s.iclass.club.CClub_PersonalRecord;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 个人战绩详情列表
 *
 * @author Administrator
 */
public class CClubPersonalRecord extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_PersonalRecord req = new Gson().fromJson(message, CClub_PersonalRecord.class);

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
        List<ClubRecordInfo> logBOs = ClubMgr.getInstance().getClubRankMgr().getPersonalRecord(req.getPid(), req.getClubId(), req.getUnionId(), req.getRoomIDList(), req.isAll(), req.getGetType(), req.getPageNum(), req.getQuery());
        request.response(CollectionUtils.isEmpty(logBOs) ? Collections.emptyList() : logBOs);

    }

}
