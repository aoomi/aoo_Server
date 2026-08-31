package business.global.mj.template.xueliu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRound;
import business.global.mj.template.MJTemplateRoundPos;
import cenum.mj.HuType;
import cenum.mj.OpType;
import cenum.mj.TryEndRoundEnum;
import com.ddm.server.common.CommLogD;

/**
 * 一个round回合中，可能同时等待多个pos进行操作，eg:抢杠胡
 *
 * @author Administrator
 */
public class MJTemplateXueLiuRoundPos extends MJTemplateRoundPos {

    public MJTemplateXueLiuRoundPos(AbsMJSetRound round, int opPos) {
        super(round, opPos);
    }

    /**
     * 操作动作类型,并且尝试结束本回合
     *
     * @param posId       操作玩家ID
     * @param opType      动作类型
     * @param huType      胡类型
     * @param tryEndRound 尝试结束回合状态
     */
    @Override
    public void opTypeTryEndRound(int posId, OpType opType, HuType huType, TryEndRoundEnum tryEndRound) {
        // 获取指定用户信息
        AbsMJSetPos oPos = getOpSetPos(posId);
        if (null == oPos) {
            CommLogD.error("setOpType null == oPos ");
            return;
        }
        // 记录本回合操作动作
        this.round.setOpType(opType);
        // 记录本回合当前位置操作动作
        this.opType = opType;
        // 清除最近出手的时间
        this.pos.getRoomPos().setLatelyOutCardTime(0L);
        if (!HuType.NotHu.equals(huType)) {
            // 记录胡牌玩家胡牌类型和胡牌位置;
            oPos.setHuCardType(huType, oPos.getPosID(), this.getRound().getRoundID());
            // 血流 需要所有玩家都要胡才算结束
            round.setSetHuEnd(roomSet().checkXiuLiuEnd());
            if (this.round.tryEndRound(this.setPosMgr.checkHuEnd())) {
                this.getRound().setExeOpPos(oPos.getPosID());
            }
            return;
        }
        // 尝试结束本回合
        if (this.tryEndRound(huType, tryEndRound)) {
            this.getRound().setExeOpPos(oPos.getPosID());
        }
    }

    public MJTemplate_XueLiuRoomSet roomSet() {
        return (MJTemplate_XueLiuRoomSet) this.set;
    }
}
