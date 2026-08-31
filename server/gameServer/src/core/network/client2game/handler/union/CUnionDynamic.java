package core.network.client2game.handler.union;

import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.union.UnionMember;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.union.CUnion_Dynamic;

import java.io.IOException;
import java.util.Objects;

/**
 * 赛事动态
 *
 * @author zaf
 */
public class CUnionDynamic extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_Dynamic req = new Gson().fromJson(message, CUnion_Dynamic.class);
        //获取当前操作者的亲友圈成员信息
        ClubMember execClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.getClubId(), player.getPid());
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(player.getPid(), req.getClubId(), req.getUnionId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (unionMember == null) {
            if (execClubMember == null || !execClubMember.isUnionMgr()) {
                request.error(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER");
                return;
            }
            request.response(UnionMgr.getInstance().unionDynamic(req.getUnionId(), req.getPageNum(), req.getGetType(), req.getPid(), req.getExecPid(), player.getPid()));
            return;
        }

        //如果是赛事的创建者和管理 正常走之前的
        if ((Objects.nonNull(execClubMember) && execClubMember.isUnionMgr()) || unionMember.isManage()) {
            request.response(UnionMgr.getInstance().unionDynamic(req.getUnionId(), req.getPageNum(), req.getGetType(), req.getPid(), req.getExecPid(), player.getPid()));
        } else {//普通的亲友圈创建者  手动设置被操作者id
            request.response(UnionMgr.getInstance().unionDynamicByClubCreate(unionMember.getClubId(), req.getUnionId(), req.getPageNum(), req.getGetType(), req.getPid(), req.getExecPid(), player.getPid()));
        }
    }

}
