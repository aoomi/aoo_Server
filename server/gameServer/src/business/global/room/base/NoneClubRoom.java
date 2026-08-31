package business.global.room.base;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.room.NormalRoomMgr;
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
public class NoneClubRoom implements RoomImpl, Serializable {
    // 亲友圈ID
    private long clubID;
    // 游戏的公共配置
    @SuppressWarnings("rawtypes")
    private BaseRoomConfigure baseRoomConfigure;
    private String key;

    @SuppressWarnings("rawtypes")
    public NoneClubRoom(long clubID, BaseRoomConfigure baseRoomConfigure, String key) {
        super();
        this.clubID = clubID;
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
        return this.clubID;
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
            this.baseRoomConfigure.getClubRoomCfg().setRoomCard(0);
            NormalRoomMgr.getInstance().remove(getRoomKey());
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(this.getSpecialRoomId());
            if (null != club) {
                club.onClubRoomRemove(this.getBaseRoomConfigure().getBaseCreateRoom().getGameIndex(), getRoomKey(), RoomSortedEnum.NONE_CONFIG.ordinal());
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
    public SData_Result specialDissolveRoom(long clubID, RoomTypeEnum roomTypeEnum, int minister, String msg) {
        return SData_Result.make(ErrorCode.Success);
    }

    @Override
    public RoomTypeEnum getRoomTypeEnum() {
        return RoomTypeEnum.CLUB;
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
