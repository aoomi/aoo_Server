package business.global.room.mj;

import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRoom;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import cenum.RoomTypeEnum;
import com.ddm.server.common.utils.CommMath;

/**
 * 麻将房间位置信息
 *
 * @author Administrator
 */
public class MJRoomPos extends AbsRoomPos {
    private int cnt = 0;
    private int huCnt = 0; // 胡牌次数
    private int tempPoint = 0;

    public MJRoomPos(int posID, AbsBaseRoom room) {
        super(posID, room);
        // 玩过这个游戏
        this.setPlayTheGame(true);
    }

    public int getCnt() {
        return cnt;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public int getHuCnt() {
        return huCnt;
    }

    public void setHuCnt(int huCnt) {
        this.huCnt = huCnt;
    }

    public int getTempPoint() {
        return tempPoint;
    }

    public void setTempPoint(int tempPoint) {
        this.tempPoint = tempPoint;
    }

    /**
     * 计算房间分数
     */
    @Override
    public void calcRoomPoint(int point) {
        AbsMJSetRoom set = (AbsMJSetRoom) this.getRoom().getCurSet();
        AbsMJSetPos setPos = set.getMJSetPos(this.getPosID());
        this.setPoint(this.getPoint() + point);
        this.setPointYiKao(CommMath.addDouble(this.getPointYiKao(), setPos.getDeductEndPoint()));
        if (!getRoom().isGuDingSuanFen()) {
            this.calcRoomSportsPoint(point, setPos.getDeductEndPoint());
        }
        if (getRoom().isRulesOfCanNotBelowZero() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            //小于0的 直接重置0
            if (this.getRoomSportsPoint() <= 0) {
                this.setRoomSportsPoint(0D);
            }
        }
    }

    /**
     * 竞技点
     */
    @Override
    public Double sportsPoint() {
        if ((getRoom().calcFenUseYiKao() || getRoom().isRulesOfCanNotBelowZero()) && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            if (RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
                return CommMath.FormatDouble(this.getPointYiKao());
            } else {
                return null;
            }
        }
        return super.sportsPoint();
    }

}
