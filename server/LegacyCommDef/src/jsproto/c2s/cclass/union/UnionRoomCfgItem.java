package jsproto.c2s.cclass.union;

import lombok.Data;

import java.util.List;

/**
 * 赛事房间玩法项
 */
@Data
public class UnionRoomCfgItem {
    /**
     * 0:未勾选,1:勾选
     */
    private int isSelect;
    /**
     * 配置Id
     */
    private long id;
    /**
     * 房间名称
     */
    private String roomName;
    /**
     * 游戏id
     */
    private int gameId;
    /**
     * 游戏中
     */
    private int playingCount;
    /**
     * 房间配置状态
     */
    private Integer status;

    /**
     * 序列Id
     */
    private int tagId;


    public UnionRoomCfgItem() {
    }

    public UnionRoomCfgItem(int isSelect, long id, String roomName, int gameId, int playingCount, int tagId) {
        this.isSelect = isSelect;
        this.id = id;
        this.roomName = roomName;
        this.gameId = gameId;
        this.playingCount = playingCount;
        this.tagId = tagId;
    }

    public UnionRoomCfgItem(int isSelect, long id, String roomName, int gameId, int playingCount, int status, int tagId) {
        this.isSelect = isSelect;
        this.id = id;
        this.roomName = roomName;
        this.gameId = gameId;
        this.playingCount = playingCount;
        this.status = status;
        this.tagId = tagId;
    }

    public long getIdMax(List<Long> unionNotGameList) {
        if (null == unionNotGameList || unionNotGameList.size() <= 0) {
            return this.getId();
        }
        if (unionNotGameList.contains(getId())) {
            return this.getId() * 10;
        } else {
            return this.getId();
        }
    }

}
