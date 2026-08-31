package core.dispatch.event.room;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubListMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.google.gson.Gson;
import jsproto.c2s.iclass.SBase_RoomInvitation;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class RoomInvitationOperationEvent<T> implements BaseExecutor {
    /**
     * 邀请的Pid
     */
    private long invitationPid;
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 房间号
     */
    private String roomKey;
    /**
     * 游戏id
     */
    private int gameId;
    /**
     * 玩家名称
     */
    private String name;
    /**
     * 房间配置
     */
    private T baseCreateRoom;
    /**
     * 玩家pid列表
     */
    private List<Long> pidList;

    public RoomInvitationOperationEvent(long invitationPid, long pid, long clubId, long unionId, String roomKey, int gameId, String name, T baseCreateRoom, List<Long> pidList) {
        this.invitationPid = invitationPid;
        this.pid = pid;
        this.clubId = clubId;
        this.unionId = unionId;
        this.roomKey = roomKey;
        this.gameId = gameId;
        this.name = name;
        this.baseCreateRoom = baseCreateRoom;
        this.pidList = pidList;
    }

    @Override
    public void invoke() {
        if (PlayerMgr.getInstance().checkExistOnlinePlayerByPid(getInvitationPid())) {
            if (this.getUnionId() <= 0L) {
                // 亲友圈房间邀请通知
                this.clubInvitationOperation();
            } else {
                // 赛事房间邀请通知
                this.unionInvitationOperation();
            }
        }

    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.ROOM.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.ROOM.bufferSize();
    }

    /**
     * 亲友圈房间邀请通知
     */
    private void clubInvitationOperation() {
        Club club = null;
        if (Config.isShare()) {
            club = ShareClubListMgr.getInstance().getClub(this.getClubId());
        } else {
            club = ClubMgr.getInstance().getClubListMgr().findClub(this.getClubId());
        }
        if (Objects.isNull(club)) {
            CommLogD.error("CLUB_NOT_EXIST ClubId:{}", this.getClubId());
            return;
        }
        if (StringUtils.isNotEmpty(club.checkGroupingBan(this.getInvitationPid(), getPidList()))) {
            // 出现不能同组的玩家
            CommLogD.error("clubInvitationOperation checkGroupingBan ClubId:{},invitationPid:{}", this.getClubId(), this.getInvitationPid());
            return;
        }
        Boolean notBanGame;
        if (Config.isShare()) {
            notBanGame = ShareClubMemberMgr.getInstance().anyMatchNotBanGame(this.getClubId(), this.getInvitationPid()) && ShareClubMemberMgr.getInstance().anyMatchNotBanGame(this.getClubId(), this.getPid());
        } else {
            notBanGame = ClubMgr.getInstance().getClubMemberMgr().anyMatchNotBanGame(this.getClubId(), this.getInvitationPid()) && ClubMgr.getInstance().getClubMemberMgr().anyMatchNotBanGame(this.getClubId(), this.getPid());
        }
        if (notBanGame) {
            if (com.ddm.server.common.Config.isShare()) {
                SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayerByOnline(this.getInvitationPid());
                //玩家邀请状态
                if (Objects.nonNull(sharePlayer) && sharePlayer.getRoomInfo().getRoomId() <= 0L && sharePlayer.isInviteFlag()) {
                    Gson gson = new Gson();
                    sharePlayer.pushProtoMq(SBase_RoomInvitation.make(getClubId(), null, getRoomKey(), getGameId(), club.getClubListBO().getName(), getName(), gson.fromJson(gson.toJson(getBaseCreateRoom()), Map.class)));
                }
            } else {
                Player invitationPlayer = PlayerMgr.getInstance().getOnlinePlayerByPid(this.getInvitationPid());
                if (Objects.nonNull(invitationPlayer) && invitationPlayer.getRoomInfo().getRoomId() <= 0L && invitationPlayer.isInviteFlag()) {
                    // 我是{clubName}亲友圈的{name},{gameId},{roomKey}
                    Gson gson = new Gson();
                    invitationPlayer.pushProto(SBase_RoomInvitation.make(getClubId(), null, getRoomKey(), getGameId(), club.getClubListBO().getName(), getName(), gson.fromJson(gson.toJson(getBaseCreateRoom()), Map.class)));
                }
            }
        }

    }

    /**
     * 赛事房间邀请通知
     */
    private void unionInvitationOperation() {
        Union union = null;
        if (Config.isShare()) {
            union = ShareUnionListMgr.getInstance().getUnion(this.getUnionId());
        } else {
            union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionId());
        }
        if (Objects.isNull(union)) {
            CommLogD.error("UNION_NOT_EXIST UnionId:{}", this.getUnionId());
            return;
        }
        if (StringUtils.isNotEmpty(union.checkGroupingBan(this.getInvitationPid(), getPidList()))) {
            // 出现不能同组的玩家
            CommLogD.error("unionInvitationOperation checkGroupingBan UnionId:{},invitationPid:{}", this.getUnionId(), this.getInvitationPid());
            return;
        }

        Club club = ClubMgr.getInstance().getClubListMgr().findClub(this.getClubId());
        if (Objects.isNull(club)) {
            CommLogD.error("CLUB_NOT_EXIST ClubId:{}", this.getClubId());
            return;
        }
        if (StringUtils.isNotEmpty(club.checkGroupingBan(this.getInvitationPid(), getPidList()))) {
            // 出现不能同组的玩家
            CommLogD.error("unionInvitationOperation checkGroupingBan UnionId:{},invitationPid:{}", this.getUnionId(), this.getInvitationPid());
            return;
        }
        // 亲友圈列表
        final List<Long> clubIdList = UnionMgr.getInstance().getUnionMemberMgr().getUnionToClubIdList(this.getUnionId());
        if (CollectionUtils.isEmpty(clubIdList)) {
            CommLogD.error("unionInvitationOperation clubIdList UnionId:{}", this.getUnionId());
            return;
        }
        // 检查指定赛事成员是否存在
        Boolean checkExist;
        if (Config.isShare()) {
            checkExist = ShareClubMemberMgr.getInstance().checkExistByPidMember(clubIdList, getPid());
        } else {
            checkExist = ClubMgr.getInstance().getClubMemberMgr().checkExistByPidMember(clubIdList, getPid());
        }
        if (checkExist) {
            // 亲友圈id
            long clubId;
            if (Config.isShare()) {
                clubId = ShareClubMemberMgr.getInstance().getMemberMaxSportsPointClubId(clubIdList, getInvitationPid());
            } else {
                clubId = ClubMgr.getInstance().getClubMemberMgr().getMemberMaxSportsPointClubId(clubIdList, getInvitationPid());
            }
            if (clubId <= 0L) {
                CommLogD.error("unionInvitationOperation clubId UnionId:{}", this.getUnionId());
                return;
            }
            if (com.ddm.server.common.Config.isShare()) {
                SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayerByOnline(this.getInvitationPid());
                //添加玩家邀请状态判断
                if (Objects.nonNull(sharePlayer) && sharePlayer.getRoomInfo().getRoomId() <= 0L && sharePlayer.isInviteFlag()) {
                    Gson gson = new Gson();
                    sharePlayer.pushProtoMq(SBase_RoomInvitation.make(clubId, getUnionId(), getRoomKey(), getGameId(), union.getUnionBO().getName(), getName(), gson.fromJson(gson.toJson(getBaseCreateRoom()), Map.class)));
                }
            } else {
                Player invitationPlayer = PlayerMgr.getInstance().getOnlinePlayerByPid(this.getInvitationPid());
                if (Objects.nonNull(invitationPlayer) && invitationPlayer.getRoomInfo().getRoomId() <= 0L && invitationPlayer.isInviteFlag()) {
                    // 我是{clubName}亲友圈的{name},{gameId},{roomKey}
                    Gson gson = new Gson();
                    invitationPlayer.pushProto(SBase_RoomInvitation.make(clubId, getUnionId(), getRoomKey(), getGameId(), union.getUnionBO().getName(), getName(), gson.fromJson(gson.toJson(getBaseCreateRoom()), Map.class)));
                }
            }

        }
    }
}
