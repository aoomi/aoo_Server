package business.global.mj.cdxzmj;		
		
import business.global.mj.AbsMJSetPos;		
import business.global.mj.AbsMJSetRound;		
import business.global.mj.robot.MJRobotOpCard;		
import business.global.mj.set.MJOpCard;		
import business.global.mj.set.MJTemplate_OpCard;		
import business.global.mj.template.MJTemplateSetPos;		
import cenum.mj.HuType;		
import cenum.mj.MJCEnum;		
import cenum.mj.OpType;		
import com.ddm.server.common.utils.CommMath;		
import com.ddm.server.websocket.handler.requset.WebSocketRequestDelegate;		
import org.apache.commons.lang3.RandomUtils;		
		
import java.util.List;		
import java.util.Objects;		
		
/**		
 * 麻将机器人打牌		
 *		
 * @author Huaxing		
 */		
public class CDXZMJRobotOpCard extends MJRobotOpCard {		
		
    public CDXZMJRobotOpCard(AbsMJSetRound setRound) {		
        super(setRound);		
		
    }		
		
    /**		
     * 机器人摸打		
     * 能胡则胡，否则摸啥打啥		
     *		
     * @param mSetPos		
     * @param posID		
     */		
    @Override		
    public boolean moDa(AbsMJSetPos mSetPos, int posID) {		
        if (getSet().getRoom().isMoDa()) {		
            List<OpType> opTypes = this.getSetRound().getRoundPosDict().get(posID).getRecieveOpTypes();		
            OpType opType = opTypes.stream().filter(k -> !HuType.NotHu.equals(MJCEnum.OpHuType(k))).findAny().orElse(opTypes.get(CommMath.randomInt(0, opTypes.size() - 1)));		
            if (HuType.NotHu.equals(MJCEnum.OpHuType(opType))) {		
                boolean existPass = mSetPos.getPosOpRecord().getOpList().stream().anyMatch(n -> n == OpType.Pass);		
                if (existPass) {		
                    this.getSetRound().opCard(new WebSocketRequestDelegate(), posID, OpType.Pass, MJOpCard.OpCard(0));		
                    return true;		
                }		
                if (mSetPos.getPosOpRecord().getOpList().stream().anyMatch(n -> n == OpType.BuMo)) {		
                    this.getSetRound().opCard(new WebSocketRequestDelegate(), posID, OpType.BuMo, MJOpCard.OpCard(0));		
                    return true;		
                }		
                boolean existOut = mSetPos.getPosOpRecord().getOpList().stream().anyMatch(n -> n == OpType.Out);		
                if (existOut) {		
                    if (mSetPos.getHandCard() == null) {		
                        this.getSetRound().opCard(new WebSocketRequestDelegate(), posID, OpType.Out, MJOpCard.OpCard(mSetPos.getSetPosRobot().getAutoCard2()));		
                    } else {		
                        this.getSetRound().opCard(new WebSocketRequestDelegate(), posID, OpType.Out, MJOpCard.OpCard(outHandCard(mSetPos)));		
                    }		
                    return true;		
                }		
            } else {		
                return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(0)) >= 0;		
            }		
        }		
        return false;		
    }		
		
    /**		
     * 摸啥打啥		
     * 如果莫进来的那张是不能打出去的牌，需要重新找牌		
     *		
     * @param mSetPos		
     * @return		
     */		
    public int outHandCard(AbsMJSetPos mSetPos) {		
        if (Objects.isNull(mSetPos.getHandCard()) || mSetPos.getPosOpNotice().getBuNengChuList().stream().anyMatch(k -> mSetPos.getHandCard().type == k || mSetPos.getHandCard().cardID == k)) {		
            return mSetPos.getSetPosRobot().getAutoCard2();		
        }		
        return mSetPos.getHandCard().cardID;		
    }		
		
    /**		
     * 不存在首牌		
     *		
     * @return		
     */		
    @Override		
    public int notExistHandCard(List<OpType> opTypes, AbsMJSetPos mSetPos) {		
		
        if (opTypes.stream().allMatch(opType -> opType.equals(OpType.Wan) || opType.equals(OpType.Tiao) || opType.equals(OpType.Tong))) {		
            // 没有相应的动作直接过		
            return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opTypes.get(RandomUtils.nextInt(0, opTypes.size())), MJOpCard.OpCard(0));		
        }		
        if (opTypes.contains(OpType.HuanSanZhang)) {		
            // 没有相应的动作直接过		
            return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), OpType.HuanSanZhang, MJTemplate_OpCard.OpCard(0, ((MJTemplateSetPos) mSetPos).getFirstChangeCardList()));		
        }		
        return super.notExistHandCard(opTypes, mSetPos);		
    }		
		
    @Override		
    public int existHandCard(List<OpType> opTypes, AbsMJSetPos mSetPos) {		
        if (opTypes.stream().allMatch(opType -> opType.equals(OpType.Wan) || opType.equals(OpType.Tiao) || opType.equals(OpType.Tong))) {		
            // 没有相应的动作直接过		
            return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opTypes.get(RandomUtils.nextInt(0, opTypes.size())), MJOpCard.OpCard(0));		
		
        }		
        if (opTypes.contains(OpType.HuanSanZhang)) {		
            // 没有相应的动作直接过		
            return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), OpType.HuanSanZhang, MJTemplate_OpCard.OpCard(0, ((MJTemplateSetPos) mSetPos).getFirstChangeCardList()));		
        }		
        return super.existHandCard(opTypes, mSetPos);		
    }		
}		
