package business.global.mj.template.xueZhan;


import business.global.mj.AbsMJRoundPos;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.AbsMJSetRound;
import business.global.mj.template.MJTemplateRoomSet;
import business.global.mj.template.MJTemplateSetPos;
import business.global.mj.template.MJTemplateSetRound;
import cenum.mj.OpType;

import java.util.List;
import java.util.Objects;


/**
 * 基础模板 回合逻辑 每一次等待操作，都是一个round
 *
 * @author Huaxing
 */

public abstract class MJTemplateXueZhanSetRound extends MJTemplateSetRound {

    public MJTemplateXueZhanSetRound(AbsMJSetRoom set, int roundID) {
        super(set, roundID);
    }


    /**
     * 血战模式胡牌 有玩家胡就不用再操做了
     *
     * @param preRound
     * @return
     */
    @Override
    protected boolean tryStartRoundHu(AbsMJSetRound preRound) {
        MJTemplateRoomSet roomSet = (MJTemplateRoomSet) this.set;
        //计算下次胡牌序号
        roomSet.calcNextHuCardType();
        List<Integer> huPostList = roomSet.getMHuInfo().getRoundHuPostList(preRound.getRoundID());

        int moPaiPos;
        if (huPostList.size() > 1) {
            moPaiPos = roomSet.getLastOpInfo().getLastOpPos();
        } else {
            moPaiPos = nextPopPosId(preRound.getOpPos());
        }
        if (moPaiPos == -1) {
            return false;
        }
        if (startWithGetCard(moPaiPos, true)) {
            notifyStart();
            return true;
        }
        return false;
    }

    @Override
    public boolean startWithGetCard(int pos, boolean isNormalMo) {
        pos = nextPopPosId(pos);
        if (pos == -1) {
            return false;
        }
        return super.startWithGetCard(pos, isNormalMo);
    }

    /**
     * 血战模式计算 下家摸牌位置（抄自 邵通麻将）
     *
     * @param pos
     * @return
     */
    public int nextPopPosId(int pos) {
        // 作弊情况下，已经有手牌
        MJTemplateSetPos setPos;
        int nextPosID;
        for (int i = 0; i < this.set.getPlayerNum(); i++) {
            nextPosID = (pos + i) % getSet().getPlayerNum();
            if (getSet().getMHuInfo().getHuPosList().contains(nextPosID)) {
                continue;
            }
            return nextPosID;
        }
        return -1;
    }


    /**
     * 检查平胡
     */
    @Override
    public boolean checkQtherPing() {
        // 继续抓牌
        int opPos = nextOpPos(this.set.getLastOpInfo().getLastOpPos() + 1);
        if (!startWithGetCard(opPos, true)) {
            return false;
        }
        notifyStart();
        return true;
    }

    /**
     * 检查是否直接过
     *
     * @return
     */
    @Override
    protected boolean checkPass() {
        MJTemplate_XueZhanSetPos mSetPos;
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            mSetPos = (MJTemplate_XueZhanSetPos) getSet().getMJSetPos(i);
            //胡牌的玩家不能出牌
            if (Objects.nonNull(mSetPos) && !mSetPos.isHuCard() && mSetPos.getHandCard() != null) {
                AbsMJRoundPos nextPos = this.nextRoundPos(i);
                nextPos.addOpType(OpType.Out);
                this.roundPosDict.put(nextPos.getOpPos(), nextPos);
                notifyStart();
                return true;
            }
        }
        return false;
    }

    /**
     * 下一个操作的位置
     *
     * @param checkPos
     * @return
     */
    protected int nextOpPos(int checkPos) {
        MJTemplate_XueZhanSetPos mSetPos;
        int nextPos;
        for (int i = 0; i < getSet().getPlayerNum(); i++) {
            nextPos = (checkPos + i) % this.room.getPlayerNum();
            //胡牌的玩家不能操作
            mSetPos = (MJTemplate_XueZhanSetPos) getSet().getMJSetPos(nextPos);
            if (Objects.nonNull(mSetPos) && !mSetPos.isHuCard()) {
                return nextPos;
            }
        }
        return checkPos;
    }

    //血战模式胡到只剩一个玩家结束
    @Override
    protected AbsMJRoundPos nextRoundPos(int pos) {
        return new MJTemplateXueZhanRoundPos(this, pos);
    }

}
