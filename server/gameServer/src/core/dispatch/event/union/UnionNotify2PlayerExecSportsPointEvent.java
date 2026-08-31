package core.dispatch.event.union;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomPosInfoShort;
import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqRoomSportsPointNotifyBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.ConstEnum;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.iclass.room.SRoom_SportsPointChange;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * 赛事通知玩家竞技点更新
 * Ø 联盟管理增加成员的疲劳值实时生效；
 * Ø 联盟管理减少成员的疲劳值，在房间外实时生效，修改目标玩家在房间内时，等该玩家退出房间后生效；
 */
@Data
public class UnionNotify2PlayerExecSportsPointEvent implements BaseExecutor {
    /**
     * 通知信息
     */
    private BaseSendMsg baseSendMsg;
    /**
     * 玩家信息
     */
    private Player player = null;
    /**
     * 操作类型
     */
    private ConstEnum.ResOpType resOpType;
    /**
     * 值
     */
    private double value;

    /**
     * 成员Id
     */
    private long memberId;
    private long playerId;


    public UnionNotify2PlayerExecSportsPointEvent(long playerId, long memberId, BaseSendMsg baseSendMsg, ConstEnum.ResOpType resOpType, double value) {
        this.setPlayerId(playerId);
        this.setMemberId(memberId);
        this.setBaseSendMsg(baseSendMsg);
        this.setResOpType(resOpType);
        this.setValue(value);
    }


    @Override
    public void invoke() {
        if (Config.isShare()) {
            SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(getPlayerId());
            if (Objects.nonNull(sharePlayer)) {
                if (sharePlayer.getRoomInfo().getRoomId() > 0L && ConstEnum.ResOpType.Gain.equals(getResOpType())) {
                    // 通知子游戏变化
                    MqProducerMgr.get().send(MqTopic.ROOM_SPORTS_POINT_NOTIFY, new MqRoomSportsPointNotifyBo(getPlayerId(), getValue(), getMemberId(), sharePlayer.getRoomInfo().getRoomId()));
                }
                if (sharePlayer.notExistRoom()) {
                    sharePlayer.pushProtoMq(getBaseSendMsg());
                }
            }
        } else {
            Player player = Objects.isNull(getPlayer()) ? PlayerMgr.getInstance().getPlayer(getPlayerId()) : getPlayer();
            if (Objects.nonNull(player)) {
                if (player.getRoomInfo().getRoomId() > 0L && ConstEnum.ResOpType.Gain.equals(getResOpType())) {
                    // 在房间里并且增加房卡
                    AbsBaseRoom room = RoomMgr.getInstance().getRoom(player.getRoomInfo().getRoomId());
                    if (Objects.nonNull(room)) {
                        if (room.getRoomPosMgr().notify2RoomSportsPointChange(player.getPid(), getMemberId(), getValue())) {
                            return;
                        }
                    }
                }
                if (player.notExistRoom()) {
                    player.pushProto(getBaseSendMsg());
                }
            }
        }

    }

    /**
     * 通知所有人房间竞技点更新
     */
    private boolean notify2RoomSportsPointChange(ShareRoom shareRoom, long pid, long memberId, double value) {
        if (CollectionUtils.isEmpty(shareRoom.getPosList())) {
            // 玩家信息列表没数据
            return false;
        }
        Optional<ShareRoomPosInfoShort> posInfoShort = shareRoom.getPosList().stream().filter(k -> k.getPid() == pid).findAny();
        if (memberId > 0L && posInfoShort.isPresent()) {
            BaseSendMsg msg = SRoom_SportsPointChange.make(shareRoom.getRoomId(),
                    posInfoShort.orElseThrow().getPos(), pid, value);
            // 遍历通知所有玩家
            shareRoom.getPosList().forEach(key -> {
                if (Objects.nonNull(key) && key.getPid() > 0L) {
                    SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(key.getPid());
                    sharePlayer.pushProtoMq(msg);
                }
            });
            return true;
        }
        return false;
    }


    @Override
    public int threadId() {
        return DispatcherComponentEnum.CLUB_UNION.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.CLUB_UNION.bufferSize();
    }
}
