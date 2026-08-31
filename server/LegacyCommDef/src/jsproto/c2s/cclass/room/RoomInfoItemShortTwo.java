package jsproto.c2s.cclass.room;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 房间信息
 *
 * @param <T>
 * @author * 简化版 2021/11/1傅哥要求
 */
@Data
public class RoomInfoItemShortTwo<T> implements Comparable<RoomInfoItemShortTwo>, Serializable {
    /**
     * ID
     */
    private long id;
    /**
     * 房间名称
     */
    private String roomName;
    /**
     * 房间key
     */
    private String roomKey;
    /**
     * 总局数
     */
    private Integer setCount;

    /**
     * 玩家信息
     */
    private List<RoomPosInfoShort> posList = new ArrayList<>();


    /**
     * 密码
     */
    private String password;
    /**
     * 房间竞技点门槛
     */
    private Double roomSportsThreshold;

    @Override
    public int compareTo(RoomInfoItemShortTwo o) {
        return 0;
    }


    /**
     * 房间竞技点门槛
     */
}




