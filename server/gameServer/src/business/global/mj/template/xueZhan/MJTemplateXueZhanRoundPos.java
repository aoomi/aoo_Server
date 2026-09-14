package business.global.mj.template.xueZhan;

import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRound;
import business.global.mj.template.MJTemplateRoomSet;
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
public class MJTemplateXueZhanRoundPos extends MJTemplateRoundPos {

    public MJTemplateXueZhanRoundPos(AbsMJSetRound round, int opPos) {
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
            // 记录胡牌玩家胡牌类型和胡牌位置
            ((MJTemplate_XueZhanSetPos) oPos).setHuCardEndType(roomSet().nextHuEndType);
            oPos.setHuCardType(calcHuType(huType), oPos.getPosID(), this.getRound().getRoundID());
            // 玩家胡了，
            round.setSetHuEnd(this.set.getPosDict().values().stream().filter(k -> k.getHuType().equals(HuType.NotHu) || k.getHuType().equals(HuType.DianPao)).count() <= 1);
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

    /**
     * 计算当前的 胡牌顺序（血战）
     *
     * @param huType
     * @return
     */
    public HuType calcHuType(HuType huType) {
        MJTemplateRoomSet roomSet = (MJTemplateRoomSet) getRound().getSet();

        if (huType.equals(HuType.ZiMo)) {
            switch (roomSet.getNextHuEndType()) {
                case FIRST:
                    huType = HuType.ZiMoOne;
                    break;
                case SECOND:
                    huType = HuType.ZiMoTwo;
                    break;
                case THIRD:
                    huType = HuType.ZiMoThree;
                    break;
                case FORTH:
                    huType = HuType.ZiMoFour;
                    break;
                default:
                    break;
            }
        } else {
            switch (roomSet.getNextHuEndType()) {
                case FIRST:
                    huType = HuType.HuOne;
                    break;
                case SECOND:
                    huType = HuType.HuTwo;
                    break;
                case THIRD:
                    huType = HuType.HuThree;
                    break;
                case FORTH:
                    huType = HuType.HuFour;
                    break;
                default:
                    break;
            }
        }
        return huType;
    }
}
