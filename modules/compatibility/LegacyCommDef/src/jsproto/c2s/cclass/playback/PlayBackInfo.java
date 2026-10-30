package jsproto.c2s.cclass.playback;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class PlayBackInfo implements Serializable {
    /**
     * 房间ID
     */
    private long roomID;
    /**
     * 第几局
     */
    private int setID;
    /**
     * 结束时间
     */
    private int endTime;
    /**
     * 回放信息
     */
    private Object playBackRes = "";
    /**
     * 庄家位置
     */
    private int dPos = -1;
    /**
     * 玩家头像列表
     */
    private String playerList = "";
    /**
     * 回放码
     */
    private int playBackCode = 0;
    /**
     * 共几局
     */
    private int setCount = 0;
    /**
     * 房间号
     */
    private String roomKey = "";
    /**
     * 游戏
     */
    private int gameType = -1;


}
