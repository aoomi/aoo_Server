package business.shareplayer;

import cenum.RoomTypeEnum;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/7 14:32
 * @description 共享玩家房间信息
 */
@Data
public class SharePlayerRoomInfo {
    /**
     * 房间Id
     */
    private long roomId = 0L;
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 联赛Id
     */
    private long unionId;
    /**
     * 消耗卡
     */
    private int consumeCard = 0;
    /**
     * 配置Id
     */
    private long configId;

    /**
     * 房间类型
     */
    private RoomTypeEnum roomTypeEnum;
    /**
     * 密码
     */
    private String password;
    /**
     * 房间号
     */
    private String roomKey;
    /**
     * 发布的主题
     */
    private String subjectTopic;
}
