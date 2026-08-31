package jsproto.c2s.cclass.share;

import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/17 10:55
 * @description 共享亲友圈配置
 */
@Data
public class ShareClubRoomConfig {
    // 房主ID
    private long ownerID;
    // 房间key
    private String roomKey = "";
    // 消耗房卡数
    private int roomCard = 0;
    // 亲友圈名称
    public String clubName;
}
