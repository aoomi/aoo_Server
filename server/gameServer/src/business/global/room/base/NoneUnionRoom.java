package business.global.room.base;

import business.global.room.NormalRoomMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import cenum.ClassType;
import cenum.RoomSortedEnum;
import cenum.RoomTypeEnum;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 空的亲友圈房间占用
 *
 * @author Administrator
 */
@Data
public class NoneUnionRoom implements RoomImpl, Serializable {
    // 亲友圈ID
    private long unionID;
    // 游戏的公共配置
    @SuppressWarnings("rawtypes")
    private BaseRoomConfigure baseRoomConfigure;
    /**
     * 房间key
     */
    private String key;

    @SuppressWarnings("rawtypes")
    public NoneUnionRoom(long unionID, BaseRoomConfigure baseRoomConfigure, String key) {
        super();
        this.unionID = unionID;
        this.baseRoomConfigure = baseRoomConfigure;
        this.key = key;
    }

    /**
     * 是否空的亲友圈房间
     */
    @Override
    public boolean isNoneRoom() {
        return true;
    }

    @Override
    public long getSpecialRoomId() {
        return this.unionID;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public BaseRoomConfigure getBaseRoomConfigure() {
        return this.baseRoomConfigure;
    }


    /**
     * 修改房间公共配置
     *
     * @param baseRoomConfigure 房间公共配置
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void setBaseRoomConfigure(BaseRoomConfigure baseRoomConfigure) {
        this.baseRoomConfigure = baseRoomConfigure;
    }

    @Override
    public void doDissolveRoom(int clubDissloveRoom) {
        if (null != this.baseRoomConfigure) {
            this.baseRoomConfigure.getUnionRoomCfg().setRoomCard(0);
            NormalRoomMgr.getInstance().remove(getRoomKey());
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getSpecialRoomId());
            if (null != union) {
                union.onUnionRoomRemove(this.getBaseRoomConfigure().getBaseCreateRoom().getGameIndex(), getRoomKey(), RoomSortedEnum.NONE_CONFIG.ordinal());
            }
        }
    }

    @Override
    public String getRoomKey() {
        return key;
    }

    @Override
    public List<Long> getRoomPidAll() {
        return new ArrayList<>();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public SData_Result specialDissolveRoom(long unionID, RoomTypeEnum roomTypeEnum, int minister, String msg) {
        return SData_Result.make(ErrorCode.Success);
    }

    @Override
    public RoomTypeEnum getRoomTypeEnum() {
        return RoomTypeEnum.UNION;
    }

    @Override
    public long getConfigId() {
        return baseRoomConfigure.getBaseCreateRoom().getGameIndex();
    }

    @Override
    public boolean checkExistEmptyPos() {
        return true;
    }

    @Override
    public ClassType getClassType() {
        return this.getBaseRoomConfigure().getGameType().getType();
    }

}
