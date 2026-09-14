package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.club.Club_define.Club_CreateGameSetStatus;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.*;
import java.util.Objects;

/**
 * 创建房间配置
 *
 * @author Administrator
 */
public class ClubCreateGameSet implements Cloneable, Serializable {
    /**
     * 游戏配置
     */
    private BaseRoomConfigure bRoomConfigure;
    /**
     * 当前设置状态
     */
    private int status = Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value();
    /**
     * 创建的房间数
     */
    private int roomCount = 0;
    /**
     * 房间创建时间
     */
    private int createTime = 0;


    public boolean isExistClubRoomConfig() {
        return BooleanUtils.and(new Boolean[]{Objects.nonNull(getbRoomConfigure()), Objects.nonNull(getbRoomConfigure().getBaseCreateRoomT()), Objects.nonNull(getbRoomConfigure().getGameType())});
    }

    public BaseRoomConfigure getbRoomConfigure() {
        return this.bRoomConfigure;
    }

    public void setRoomConfigure(BaseRoomConfigure bRoomConfigure) {
        this.bRoomConfigure = bRoomConfigure;
    }

    public int getStatus() {
        if (Objects.isNull(bRoomConfigure) || Objects.isNull(bRoomConfigure.getBaseCreateRoom())) {
            return Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_DELETE.value();
        }
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getRoomCard() {
        return null != this.bRoomConfigure ? this.bRoomConfigure.getClubRoomCfg().getRoomCard() : 0;
    }

    public void setRoomCard(int roomCard) {
        this.bRoomConfigure.getClubRoomCfg().setRoomCard(roomCard);
    }

    public int getRoomCount() {
        return this.roomCount;
    }

    public void setRoomCount(int roomCount) {
        this.roomCount = roomCount;
    }

    public void addRoomCount() {
        this.roomCount++;
    }

    public void subRoomCount() {
        this.roomCount = Math.max(0, this.roomCount - 1);
    }

    public String getRoomKey() {
        return null != this.bRoomConfigure ? this.bRoomConfigure.getClubRoomCfg().getRoomKey() : "";
    }

    public void setRoomKey(String roomKey) {
        this.bRoomConfigure.getClubRoomCfg().setRoomKey(roomKey);
    }

    public GameType getGameType() {
        return this.bRoomConfigure.getGameType();
    }

    public long getGameIndex() {
        return null != this.bRoomConfigure ? this.bRoomConfigure.getBaseCreateRoom().getGameIndex() : 0L;
    }

    public void setGameIndex(long gameIndex) {
        this.bRoomConfigure.getBaseCreateRoom().setGameIndex(gameIndex);
    }

    public long getClubId() {
        return this.bRoomConfigure.getBaseCreateRoom().getClubId();
    }

    public int getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public void setTagId(int tagId) {
        this.getbRoomConfigure().setTagId(tagId);
    }

    /**
     * 对象之间的浅克隆【只负责copy对象本身，不负责深度copy其内嵌的成员对象】
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public ClubCreateGameSet clone() {
        try {
            return (ClubCreateGameSet) super.clone();
        } catch (CloneNotSupportedException ex) { throw new IllegalStateException("Clone contract violated", ex); }
    }

    /**
     * 实现对象间的深度克隆【从外形到内在细胞，完完全全深度copy】
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public ClubCreateGameSet deepClone() {
        // Anything 都是可以用字节流进行表示，记住是任何！
        ClubCreateGameSet cookBook = null;
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
            cookBook = (ClubCreateGameSet) ois.readObject();

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
