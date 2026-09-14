package jsproto.c2s.cclass.share;

import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/17 10:56
 * @description 共享赛事房间配置
 */
@Data
public class ShareUnionRoomConfig {
    /**
     * 赛事名称
     */
    private String name;

    /**
     * 房主ID
     */
    private long ownerID;
    /**
     * 房间key
     */
    private String roomKey = "";
    /**
     * 消耗房卡数
     */
    private int roomCard = 0;
}
