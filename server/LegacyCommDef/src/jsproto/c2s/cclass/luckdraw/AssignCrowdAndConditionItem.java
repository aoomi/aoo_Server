package jsproto.c2s.cclass.luckdraw;

import cenum.LuckDrawEnum.LuckDrawAssignCrowd;
import cenum.LuckDrawEnum.LuckDrawType;
import cenum.RoomTypeEnum;
import lombok.Data;

/**
 * 指定人群和抽奖条件
 */
@Data
public class AssignCrowdAndConditionItem {
    /**
     * 抽奖类型：0：免费，1：房卡消耗，2：局数，3：大赢家
     */
    private LuckDrawType luckDrawType;

    /**
     * 指定人群(0：所有人、1：代理、2：亲友圈、3：联赛)
     */
    private LuckDrawAssignCrowd assignCrowd;

    public AssignCrowdAndConditionItem(LuckDrawType luckDrawType, LuckDrawAssignCrowd assignCrowd) {
        this.luckDrawType = luckDrawType;
        this.assignCrowd = assignCrowd;
    }

    /**
     * 是否存在抽奖条件
     *
     * @param roomTypeEnum 房间类型
     * @return
     */
    public boolean isExistLuckDraw(RoomTypeEnum roomTypeEnum) {
        if (RoomTypeEnum.checkExistLuckDrawRoomType(roomTypeEnum)) {
            if (RoomTypeEnum.NORMAL.equals(roomTypeEnum)) {
                // 普通的钻石房间，亲友圈、联赛 不记录
                return !(LuckDrawAssignCrowd.CLUB.equals(assignCrowd) || LuckDrawAssignCrowd.UNION.equals(assignCrowd));
            } else if (RoomTypeEnum.CLUB.equals(roomTypeEnum)) {
                // 亲友圈房间不记录联赛
                return !(LuckDrawAssignCrowd.UNION.equals(assignCrowd));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 不存在抽奖条件
     *
     * @param roomTypeEnum 房间类型
     * @return
     */
    public boolean isNotExistLuckDraw(RoomTypeEnum roomTypeEnum) {
        return !isExistLuckDraw(roomTypeEnum);
    }
}
