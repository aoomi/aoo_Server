package business.global.room.base;

import cenum.ClassType;
import cenum.RoomSortedEnum;
import cenum.RoomTypeEnum;
import cenum.room.RoomState;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.util.List;

/**
 * 房间接口
 *
 * @author Administrator
 */
public interface RoomImpl {
    /**
     * 是否空亲友圈房间
     *
     * @return
     */
    default boolean isNoneRoom() {
        return false;
    }

    ;

    /**
     * 获取房间类型ID
     * 如果是亲友圈房间，则获取到亲友圈Id
     * 如果是大赛事房间，则获取到大赛事Id
     *
     * @return
     */
    public long getSpecialRoomId();

    /**
     * 获取房间公共配置
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public BaseRoomConfigure getBaseRoomConfigure();

    /**
     * 修改房间公共配置
     *
     * @param baseRoomConfigure 房间公共配置
     */
    @SuppressWarnings("rawtypes")
    public void setBaseRoomConfigure(BaseRoomConfigure baseRoomConfigure);

    /**
     * 房间当前状态
     *
     * @return
     */
    default RoomState getRoomState() {
        return RoomState.Init;
    }

    ;

    /**
     * 操作解散亲友圈房间
     */
    public void doDissolveRoom(int dissolveNoticeType);

    /**
     * 获取房间key
     *
     * @return
     */
    public String getRoomKey();

    /**
     * 查询房间内的所有玩家PID
     *
     * @return
     */
    public List<Long> getRoomPidAll();


    /**
     * 操作特殊房间解散
     *
     * @param id 类型ID
     */
    @SuppressWarnings("rawtypes")
    public SData_Result specialDissolveRoom(long id, RoomTypeEnum roomTypeEnum, int manage, String msg);


    /**
     * 房间类型
     *
     * @return
     */
    public RoomTypeEnum getRoomTypeEnum();

    /**
     * 获取房间配置Id
     *
     * @return
     */
    public long getConfigId();

    /**
     * 检查是否存在空位置房间
     *
     * @return
     */
    public boolean checkExistEmptyPos();

    /**
     * 筛选排序
     *
     * @return
     */
    default int sorted() {
        return RoomSortedEnum.NONE_CONFIG.ordinal();
    }

    ;

    /**
     * 游戏类型：扑克、麻将
     *
     * @return
     */
    public ClassType getClassType();

    /**
     * 房间是否结束了
     *
     * @return
     */
    default boolean isEndRoom() {
        return false;
    }


    /**
     * 获取空位数量
     *
     * @return
     */
    default int getEmptyPosCount() {
        return 0;
    }

    default boolean isWatchRoom() {
        return false;
    }
}
