package jsproto.c2s.cclass.room;

import BaseCommon.CommLog;
import cenum.PrizeType;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import jsproto.c2s.cclass.GameType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.*;

/**
 * 公共房间配置
 *
 * @author Administrator
 */
@SuppressWarnings("serial")
public class BaseRoomConfigure<T> implements Cloneable, Serializable {
    // 消耗类型 -房卡 ,金币
    private PrizeType prizeType;
    // 小类型
    private GameType gameType;
    // 房间创建时初始配置
    private T baseCreateRoom = null;
    // 房间创建时初始配置类名
    private String baseCreateRoomClassName = null;
    // 共享方式创建
    private String shareBaseCreateRoom = null;
    // 亲友圈房间配置
    private ClubRoomConfig clubRoomCfg = null;
    // 赛事房间配置
    private UnionRoomConfig unionRoomCfg = null;
    // 机器人房间配置
    private RobotRoomConfig robotRoomCfg = null;
    // 竞技场房间配置
    private ArenaRoomConfig<?> arenaRoomCfg = null;
    /**
     * 标记Id
     */
    private int tagId;

    /**
     * 清空
     */
    public void clear() {
        this.prizeType = null;
        this.gameType = null;
        this.baseCreateRoom = null;
        this.clubRoomCfg = null;
        this.unionRoomCfg = null;
        this.robotRoomCfg = null;
        this.arenaRoomCfg = null;
    }

    public BaseRoomConfigure(PrizeType prizeType, GameType gameType, T baseCreateRoom, String shareBaseCreateRoom) {
        super();
        this.prizeType = prizeType;
        this.gameType = gameType;
        this.baseCreateRoom = baseCreateRoom;
        this.shareBaseCreateRoom = shareBaseCreateRoom;
        this.baseCreateRoomClassName = baseCreateRoom.getClass().getName();
    }

    public BaseRoomConfigure(PrizeType prizeType, GameType gameType, T baseCreateRoom) {
        super();
        this.prizeType = prizeType;
        this.gameType = gameType;
        this.baseCreateRoom = baseCreateRoom;
        this.baseCreateRoomClassName = baseCreateRoom.getClass().getName();
    }

    public BaseRoomConfigure(PrizeType prizeType, GameType gameType, T baseCreateRoom, RobotRoomConfig robotRoomCfg) {
        super();
        this.prizeType = prizeType;
        this.gameType = gameType;
        this.baseCreateRoom = baseCreateRoom;
        this.robotRoomCfg = robotRoomCfg;
        this.baseCreateRoomClassName = baseCreateRoom.getClass().getName();
    }

    public BaseRoomConfigure(PrizeType prizeType, GameType gameType, T baseCreateRoom, ClubRoomConfig clubRoomCfg) {
        super();
        this.prizeType = prizeType;
        this.gameType = gameType;
        this.baseCreateRoom = baseCreateRoom;
        this.clubRoomCfg = clubRoomCfg;
        this.baseCreateRoomClassName = baseCreateRoom.getClass().getName();
    }

    public BaseRoomConfigure(PrizeType prizeType, GameType gameType, T baseCreateRoom, UnionRoomConfig unionRoomConfig) {
        super();
        this.prizeType = prizeType;
        this.gameType = gameType;
        this.baseCreateRoom = baseCreateRoom;
        this.unionRoomCfg = unionRoomConfig;
        this.baseCreateRoomClassName = baseCreateRoom.getClass().getName();
    }


    public BaseRoomConfigure(PrizeType prizeType, GameType gameType, T baseCreateRoom, ArenaRoomConfig<?> arenaRoomConfig) {
        super();
        this.prizeType = prizeType;
        this.gameType = gameType;
        this.baseCreateRoom = baseCreateRoom;
        this.arenaRoomCfg = arenaRoomConfig;
        this.baseCreateRoomClassName = baseCreateRoom.getClass().getName();
    }

    public PrizeType getPrizeType() {
        return prizeType;
    }

    public void setPrizeType(PrizeType prizeType) {
        this.prizeType = prizeType;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public BaseCreateRoom getBaseCreateRoom() {
        if (baseCreateRoom instanceof LinkedTreeMap) {
            Gson gson = new Gson();
            try {
                return (BaseCreateRoom) gson.fromJson(gson.toJson(baseCreateRoom), Class.forName(baseCreateRoomClassName));
            } catch (ClassNotFoundException e) {
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                CommLog.error(e.getMessage(), e);
            }
            return null;
        } else {
            return (BaseCreateRoom) baseCreateRoom;
        }
    }

    public T getBaseCreateRoomT() {
        return this.baseCreateRoom;
    }


    public void setBaseCreateRoom(T baseCreateRoom) {
        this.baseCreateRoom = baseCreateRoom;
        this.baseCreateRoomClassName = baseCreateRoom.getClass().getName();
    }

    public ClubRoomConfig getClubRoomCfg() {
        return clubRoomCfg;
    }

    public void setClubRoomCfg(ClubRoomConfig clubRoomCfg) {
        this.clubRoomCfg = clubRoomCfg;
    }

    public RobotRoomConfig getRobotRoomCfg() {
        return robotRoomCfg;
    }

    public void setRobotRoomCfg(RobotRoomConfig robotRoomCfg) {
        this.robotRoomCfg = robotRoomCfg;
    }

    public ArenaRoomConfig getArenaRoomCfg() {
        return arenaRoomCfg;
    }

    public void setArenaRoomCfg(ArenaRoomConfig<?> arenaRoomCfg) {
        this.arenaRoomCfg = arenaRoomCfg;
    }

    public UnionRoomConfig getUnionRoomCfg() {
        return unionRoomCfg;
    }

    public void setUnionRoomConfig(UnionRoomConfig unionRoomCfg) {
        this.unionRoomCfg = unionRoomCfg;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public String getShareBaseCreateRoom() {
        return shareBaseCreateRoom;
    }

    public void setShareBaseCreateRoom(String shareBaseCreateRoom) {
        this.shareBaseCreateRoom = shareBaseCreateRoom;
    }

    //	/**
//	 * 获取房间key
//	 * @return
//	 */
//	public String roomKey(RoomTypeEnum roomTypeEnum) {
//		if(RoomTypeEnum.CLUB.equals(roomTypeEnum)) {
//			return null == this.clubRoomCfg ? "" : this.clubRoomCfg.getRoomKey();
//		} else if (RoomTypeEnum.UNION.equals(roomTypeEnum)) {
//			return null == this.unionRoomCfg ? "" : this.unionRoomCfg.getRoomKey();
//		}
//		return null;
//	}

    /**
     * 对象之间的浅克隆【只负责copy对象本身，不负责深度copy其内嵌的成员对象】
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public BaseRoomConfigure clone() {
        try {
            return (BaseRoomConfigure) super.clone();
        } catch (CloneNotSupportedException ex) { throw new IllegalStateException("Clone contract violated", ex); }
    }

    /**
     * 实现对象间的深度克隆【从外形到内在细胞，完完全全深度copy】
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public BaseRoomConfigure deepClone() {
        // Anything 都是可以用字节流进行表示，记住是任何！
        BaseRoomConfigure cookBook = null;
        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            // 将当前的对象写入baos【输出流 -- 字节数组】里
            oos.writeObject(this);

            // 从输出字节数组缓存区中拿到字节流
            byte[] bytes = baos.toByteArray();

            // 创建一个输入字节数组缓冲区
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            // 创建一个对象输入流
            ObjectInputStream ois = new ObjectInputStream(bais);
            // 下面将反序列化字节流 == 重新开辟一块空间存放反序列化后的对象
            cookBook = (BaseRoomConfigure) ois.readObject();

        } catch (Exception e) { throw new IllegalStateException("Deep clone failed", e); }
        return cookBook;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}
