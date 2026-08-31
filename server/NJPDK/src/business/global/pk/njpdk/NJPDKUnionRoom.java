package business.global.pk.njpdk;

import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.type.UnionRoom;
import business.njpdk.c2s.iclass.SNJPDK_SportsPointEnough;
import business.njpdk.c2s.iclass.SNJPDK_SportsPointNotEnough;
import cenum.RoomTypeEnum;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 赛事房间信息
 * 安岳跑的快特殊处理
 *
 * @author
 */
public class NJPDKUnionRoom extends UnionRoom {

    public NJPDKUnionRoom(AbsBaseRoom room) {
        super(room);
    }


    /**
     * 联赛继续游戏
     *
     * @return
     */
    public SData_Result unionContinueGame(AbsRoomPos roomPos) {
        if (RoomTypeEnum.UNION.equals(this.getRoomTypeEnum())) {
            double value = this.getBaseCreateRoom().getAutoDismiss();
            if (roomPos.isAutoDismiss(value)) {
                return SData_Result.make(ErrorCode.ROOM_SPORTS_POINT_NOT_ENOUGH, "ROOM_SPORTS_POINT_NOT_ENOUGH");
            }
            List<AbsRoomPos> roomPosList = this.getRoom().getRoomPosMgr().getPosList().stream().filter(k -> Objects.nonNull(k) && k.isAutoDismiss(value)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(roomPosList)) {
                StringBuilder builder = new StringBuilder();
                for (AbsRoomPos pos : roomPosList) {
                    builder.append(String.format("@%s", pos.getName())).append("', ");
                }
                builder.deleteCharAt(builder.length() - 2);
                roomPos.getPlayer().pushProto(SNJPDK_SportsPointEnough.make(this.getRoom().getRoomID(), builder.toString()));
            }
            return SData_Result.make(ErrorCode.Success);
        } else {
            return SData_Result.make(ErrorCode.Success);
        }
    }

    /**
     * 检查自动解散房间
     *
     * @return
     */
    @Override
    public boolean checkAutoDissolveRoom() {
        double value = this.getBaseCreateRoom().getAutoDismiss();
        NJPDKRoom hbmjRoom = (NJPDKRoom) this.getRoom();
        if (hbmjRoom.isAutoDismiss()) {
            return this.getRoom().getRoomPosMgr().getPosList().stream().anyMatch(k -> Objects.nonNull(k) && k.isAutoDismiss(value));
        }
        Map<Boolean, List<AbsRoomPos>> roomPosMap = this.getRoom().getRoomPosMgr().getPosList().stream().filter(k -> Objects.nonNull(k)).collect(Collectors.groupingBy(p -> p.isAutoDismiss(value)));
        if (MapUtils.isEmpty(roomPosMap)) {
            hbmjRoom.setAutoDismiss(true);
            return false;
        }
        List<AbsRoomPos> autoDismissValueTrueList = roomPosMap.get(Boolean.TRUE);
        if (CollectionUtils.isEmpty(autoDismissValueTrueList)) {
            hbmjRoom.setAutoDismiss(true);
            return false;
        }
        StringBuilder builder = new StringBuilder();
        for (AbsRoomPos pos : autoDismissValueTrueList) {
            builder.append(String.format("@%s", pos.getName())).append("', ");
            //  竞技点不足，确定已联系管理！取消则发起解散房间
            pos.getPlayer().pushProto(SNJPDK_SportsPointNotEnough.make(this.getRoom().getRoomID()));
        }
        builder.deleteCharAt(builder.length() - 2);
        List<AbsRoomPos> autoDismissValueFalseList = roomPosMap.get(Boolean.FALSE);
        if (CollectionUtils.isNotEmpty(autoDismissValueFalseList)) {
            for (AbsRoomPos pos : autoDismissValueFalseList) {
                // @玩家名称，@玩家名称，竞技点不足无法继续游戏，请联系管理
                pos.getPlayer().pushProto(SNJPDK_SportsPointEnough.make(this.getRoom().getRoomID(), builder.toString()));
            }
        }
        hbmjRoom.setAutoDismiss(true);
        return true;
    }

}
