package business.global.mj.extbussiness;
						
import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRound;
import business.global.mj.extbussiness.dto.StandardMJOpCard;
import business.global.mj.robot.MJRobotOpCard;
import cenum.mj.OpType;
import com.ddm.server.websocket.handler.requset.WebSocketRequestDelegate;

/**
 * 麻将机器人
 */
public class StandardMJRobotOpCard extends MJRobotOpCard {
						
    public StandardMJRobotOpCard(AbsMJSetRound setRound) {
        super(setRound);
    }

    public boolean needHuMoDa() {
        return true;
    }

    /**
     * 机器人摸打
     * @param mSetPos
     * @param posID
     */
    public boolean moDa(AbsMJSetPos mSetPos, int posID){
        boolean result = super.moDa(mSetPos, posID);
        if(!result){
            boolean existHuan = mSetPos.getPosOpRecord().getOpList().stream().anyMatch(n -> n==OpType.HuanSanZhang);
            if(existHuan){
                this.getSetRound().opCard(new WebSocketRequestDelegate(), posID, OpType.HuanSanZhang, StandardMJOpCard.OpCard(0, ((StandardMJSetPos)mSetPos).getFirstChangeCardList()));
                return true;
            }
        }
        return result;
    }
}
