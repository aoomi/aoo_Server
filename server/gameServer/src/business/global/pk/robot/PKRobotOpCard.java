package business.global.pk.robot;

import BaseCommon.CommLog;
import business.global.pk.AbsPKRoundPos;
import business.global.pk.AbsPKSetPos;
import business.global.pk.AbsPKSetRoom;
import business.global.pk.AbsPKSetRound;
import cenum.PKOpType;
import cenum.PrizeType;
import com.ddm.server.common.utils.CommTime;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Data
public class PKRobotOpCard {
    /**
     * 回合信息
     */
    private AbsPKSetRound setRound;
    /**
     * 当局信息
     */
    private AbsPKSetRoom set;

    public PKRobotOpCard(AbsPKSetRound setRound) {
        this.setRound = setRound;
        this.setSet(this.getSetRound().getSet());
    }

    public void RobothandCrad(int posID) {
        // 获取当前操作位置
        AbsPKRoundPos roundPos = this.getSetRound().getRoundPosDict().get(posID);
        if (Objects.isNull(roundPos)) {
            // 检查超时等待时间
            this.checkWaitTime();
            return;
        }
        // 检查位置是否已经操作过
        if (Objects.nonNull(roundPos.getOpType())) {
            // 检查超时等待时间
            this.checkWaitTime();
            return;
        }
        // 获取玩家信息
        AbsPKSetPos mSetPos = roundPos.getPos();
        if (Objects.isNull(mSetPos)) {
            // 检查超时等待时间
            this.checkWaitTime();
            return;
        }
        // 获取玩家可操作列表
        List<PKOpType> opTypes = roundPos.getReceiveOpTypes();
        if (CollectionUtils.isEmpty(opTypes)) {
            // 检查超时等待时间
            this.checkWaitTime();
            return;
        }
        // 存在打牌
        this.existOutCard(opTypes, mSetPos);
    }

    /**
     * 存在首牌
     *
     * @return
     */
    public int existOutCard(List<PKOpType> opTypes, AbsPKSetPos mSetPos) {
        return 0;
    }


    /**
     * 检查超时等待时间
     */
    public void checkWaitTime() {
        if (CommTime.nowSecond() - this.setRound.getStartTime() >= 180) {
            if (PrizeType.Gold.equals(this.getSet().getRoom().getBaseRoomConfigure().getPrizeType())) {
                this.getSet().endSet();
            }
            CommLog.info("RobothandCrad RoomID:{},StartTime:{},EndTime:{},UpdateTime:{}", this.getSet().getRoom().getRoomID(), this.getSetRound().getStartTime(), this.getSetRound().getEndTime(), this.getSetRound().getUpdateTime());
        }
    }
}
