package core.network.client2game.handler.union;

import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.union.Union;
import business.global.union.UnionMember;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.feature.PlayerWallet;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.union.CUnion_GetAllRoom;
import jsproto.c2s.iclass.union.SUnion_DiamondsNotEnough;
import jsproto.c2s.iclass.union.SUnion_GetAllRoom;

import java.io.IOException;
import java.util.Objects;

/**
 * 获取赛事房间
 *
 * @author zaf
 */
public class CUnionGetAllRoom extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_GetAllRoom req = new Gson().fromJson(message, CUnion_GetAllRoom.class);
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        ClubMember clubMember1 = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.getClubId(), Club_define.Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN);
        if (null == clubMember) {
            if (Objects.nonNull(clubMember1)) {
                request.error(ErrorCode.CLUB_CLUBMEMBER_EXIST_APPLOVE, "CLUB_CLUBMEMBER_EXIST_APPLOVE");
                return;
            }
            request.error(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
            return;
        }
        UnionMember unionMember = UnionMgr.getInstance().getUnionMemberMgr().find(req.getClubId(), req.getUnionId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU);
        if (null == unionMember) {
            request.error(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER");
            return;
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            request.error(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
            return;
        }
        request.response();
        //	被禁止游戏的玩家（赛事、亲友圈）在进入房间列表时，需显示出房间列表的桌子，点击桌子时返回通用提示框：“您已被禁止游戏，请联系管理”;
//        if (clubMember.isBanGame()) {
//            return;
//        }
        if (clubMember.isUnionBanGame()) {
            return;
        }
        player.pushProto(SUnion_GetAllRoom.make(req.getClubId(), req.getUnionId(), UnionMgr.getInstance().onUnionGetAllRoom(req.getUnionId(), clubMember.getClubMemberBO().getUnionNotGameList(), clubMember.getClubMemberBO().getIsHideStartRoom(), req.getPageNum(), union.getUnionBO().getSort(), req.getGameType()), req.getPageNum()));

        this.checkDiamondsAttention(union, unionMember, clubMember, player);
    }

    /**
     * 钻石消耗通知 全员 或者只通知管理
     *
     * @param
     */
    private void checkDiamondsAttention(Union union, UnionMember unionMember, ClubMember clubMember, Player player) {
        //获取亲友圈圈主的钻石
        int diamondsValue = union.getOwnerPlayer().getFeature(PlayerWallet.class).balance();
        if ((unionMember.getUnionMemberBO().getClubOwnerId() == player.getPid()) || clubMember.isUnionMgr()) {
            if (union.getUnionBO().getUnionDiamondsAttentionMinister() > diamondsValue && !player.isUnionDiamondsAttentionMinister()) {
                player.setUnionDiamondsAttentionMinister(true);
                player.pushProto(SUnion_DiamondsNotEnough.make(union.getUnionBO().getClubId(), union.getUnionBO().getName(), union.getUnionBO().getUnionDiamondsAttentionMinister()));
            }
        } else {
            // 如果钻石小于设定的值则发起通知
            if (union.getUnionBO().getUnionDiamondsAttentionAll() > diamondsValue && !player.isUnionDiamondsAttentionAll()) {
                player.setUnionDiamondsAttentionAll(true);
                player.pushProto(SUnion_DiamondsNotEnough.make(union.getUnionBO().getClubId(), union.getUnionBO().getName(), union.getUnionBO().getUnionDiamondsAttentionAll()));
            }
        }
        //如果钻石数量超过的时候 把通知的标志设置回来
        if (diamondsValue > union.getUnionBO().getUnionDiamondsAttentionAll() && player.isUnionDiamondsAttentionAll()) {
            player.setUnionDiamondsAttentionAll(false);
        }
        if (diamondsValue > union.getUnionBO().getUnionDiamondsAttentionMinister() && player.isUnionDiamondsAttentionMinister()) {
            player.setUnionDiamondsAttentionMinister(false);
        }
    }

}
