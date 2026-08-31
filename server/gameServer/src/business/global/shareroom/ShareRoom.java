package business.global.shareroom;

import business.global.room.base.RoomImpl;
import business.shareplayer.ShareNode;
import cenum.RoomSortedEnum;
import cenum.RoomTypeEnum;
import cenum.room.RoomState;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xsj
 * @date 2020/8/10 17:11
 * @description 共享房间信息
 */
@Data
//@MongoDbs({@MongoDb(doc = @Document(collection = "shareRoomKey"), indexes = @CompoundIndexes({@CompoundIndex(name = "clubId_1",def = "{'clubId':1}"),@CompoundIndex(name = "unionId_1",def = "{'unionId':1}"),@CompoundIndex(name = "roomId_1",def = "{'roomId':1}"),@CompoundIndex(name = "ip_port",def = "{'nodeIp':1, 'nodePort':1}")}))})
public class ShareRoom implements Serializable {
    /**
     * ID
     */
    private long id;
    // 更新时间
    private long updateTime;
    /**
     * 房间名称
     */
    private String roomName;
    /**
     * 房间key
     */
    private String roomKey;
    /**
     * 类型Id
     */
    private Integer gameId;
    /**
     * 总局数
     */
    private Integer setCount;
    /**
     * 人数
     */
    private Integer playerNum;
    /**
     * 玩家信息
     */
    private List<ShareRoomPosInfoShort> posList = new ArrayList<>();
    /**
     * 房间创建时间
     */
    private Integer createTime;
    /**
     * 局数
     */
    private int setId;
    /**
     * 是否关闭
     */
    private boolean isClose;

    /**
     * 排序值（0:空配置、未满人房间,1:房间满过人,2:游戏中）
     */
    private int sort;

    /**
     * 房间配置
     */
    private Object roomCfg;

    /**
     * 配置Id
     */
    private int tagId;

    /**
     * 房间Id
     */
    private long roomId;

    /**
     * 密码
     */
    private String password;
    /**
     * 亲友圈ID
     */
    private long clubId;
    /**
     * 竞赛ID
     */
    private long unionId;
    /**
     * 房间类型
     */
    private RoomTypeEnum roomTypeEnum;
    /**
     * 当前节点
     */
    private ShareNode curShareNode;
    /**
     * 房间创建者
     */
    private long ownerID;
    /**
     * 公共配置
     */
    private ShareBaseRoomConfigure baseRoomConfigure;
    /**
     * 房间阶段
     */
    private RoomState roomState;
    /**
     * 所属游戏信息
     */
    private ShareRoomGameBo shareRoomGameBo;
    /**
     * 是否空房间
     */
    private boolean noneRoom;
    /**
     * 获取房间类型ID
     * 如果是亲友圈房间，则获取到亲友圈Id
     * 如果是大赛事房间，则获取到大赛事Id
     *
     * @return
     */
    private long specialRoomId;

    private RoomImpl room;

    private long configId;

    /**
     * 发布的主题
     */
    private String subjectTopic;

    public RoomState getRoomState() {
        return roomState == null ? RoomState.Init : roomState;
    }

    public List<Long> getRoomPidAll() {
        return this.getPosList().stream().filter(k -> Objects.nonNull(k) && k.getPid() > 0L).map(k -> k.getPid())
                .collect(Collectors.toList());
    }

    public int sorted() {
        //空配置房间
        if (isNoneRoom()) {
            return RoomSortedEnum.NONE_CONFIG.ordinal();
        }
        if (RoomState.Playing.equals(this.getRoomState()) || RoomState.End.equals(this.getRoomState())) {
            // 游戏中 或者结束
            return RoomSortedEnum.GAME_PLAYING.ordinal() + 5;
        }
        if (getFullPosCount() > 0 && getFullPosCount() < this.getPlayerNum()) {
            // 房间中有人
            return RoomSortedEnum.NONE_ROOM.ordinal();
        } else if (getFullPosCount() == this.getPlayerNum()) {
            // 房间满人
            return RoomSortedEnum.GAME_INIT.ordinal() + 3;
        } else {
            // 空房间
            return RoomSortedEnum.NONE_CONFIG.ordinal();
        }
    }

    /**
     * 获取空位数量
     *
     * @return
     */
    public int getEmptyPosCount() {
        return (int) this.getPosList().stream().filter((x) -> x.getPid() <= 0L).count();
    }

    /**
     * 获取坐在位置玩家的数量
     *
     * @return
     */
    public int getFullPosCount() {
        return (int) this.getPosList().stream().filter((x) -> x.getPid() > 0L).count();
    }

    public int getTagId() {
        return this.getBaseRoomConfigure().getTagId();
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }
}
