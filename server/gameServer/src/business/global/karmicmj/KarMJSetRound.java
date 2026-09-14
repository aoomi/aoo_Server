package business.global.karmicmj;

import business.global.mj.AbsMJRoundPos;
import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.AbsMJSetRound;
import business.global.mj.set.MJOpCard;
import cenum.mj.HuType;
import cenum.mj.MJCEnum;
import cenum.mj.MJOpCardError;
import cenum.mj.OpType;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;

import java.util.List;

/**
 * 血战到底基础回合
 */
public abstract class KarMJSetRound extends AbsMJSetRound {

    public KarMJSetRound(AbsMJSetRoom set, int roundID) {
        super(set, roundID);
    }

    /**
     * 获取当前回合胡牌列表
     *
     * @return
     */
    public List<Integer> getHuList() {
        return getSet().getMHuInfo().getRoundHuPostList(getSet().getCurRound().getRoundID());
    }

    @Override
    public synchronized int opCard(WebSocketRequest request, int opPos, OpType opType, MJOpCard mOpCard) {
        if (this.getEndTime() > 0) {
            request.error(ErrorCode.NotAllow, "end Time opPos has no round power");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        AbsMJRoundPos pos = this.roundPosDict.get(opPos);
        if (null == pos) {
            request.error(ErrorCode.NotAllow, "opPos has no round power");
            return MJOpCardError.ROUND_POS_ERROR.value();
        }
        int opCardRet = pos.op(request, opType, mOpCard);
        if (opCardRet >= 0) {
            if (!HuType.NotHu.equals(MJCEnum.OpHuType(this.getOpType()))) {
                List<Integer> huList = getHuList();
                AbsMJSetPos setPos;
                for (Integer huPos : huList) {
                    setPos = this.set.getMJSetPos(huPos);
                    if (null == setPos) {
                        continue;
                    }
                    if (setPos.isHu()) {
                        continue;
                    }
                    //设置是否胡牌
                    setPos.setHuOpType(this.getOpType());
                    //设置第几胡
                    setPos.setHuCount((this.getSet()).getHuCount());
                    setPos.getCalcPosEnd().calcPosEnd(setPos);
                }
            }
            this.posOpCardRet(opCardRet, false);
        }
        return opCardRet;
    }

    protected abstract boolean tryStartRoundOtherKar(AbsMJSetRound preRound);

    /**
     * 尝试开始其他回合 如果 没有其他特殊回合 默认返回 false 否则 对其他特殊操作类型进行操作检查
     */
    @Override
    protected boolean tryStartRoundOther(AbsMJSetRound preRound) {
        // 胡牌
        if (OpType.Hu.equals(preRound.getOpType())) {
            return tryStartZiMo(preRound);
        }
        // 接炮 || 抢杠胡
        if (OpType.JiePao.equals(preRound.getOpType()) || OpType.QiangGangHu.equals(preRound.getOpType())) {
            return tryStartJiePao(preRound);
        }
        return tryStartRoundOtherKar(preRound);
    }

    /**
     * 检查平胡
     *
     * @return 1：其他人可以抢杠胡 2、没有人，3、不是抢杠胡操作
     */
    @Override
    public boolean checkQtherPing() {
        AbsMJSetPos aPos;
        for (int i = 1; i < this.room.getPlayerNum(); i++) {
            int opPos = (this.set.getLastOpInfo().getLastOpPos() + i) % this.room.getPlayerNum();
            aPos = this.set.getMJSetPos(opPos);
            if (null == aPos || aPos.isHu()) {
                continue;
            }
            if (!startWithGetCard(opPos, true)) {
                return false;
            }
            notifyStart();
            return true;
        }
        return false;
    }

    /**
     * 本回合的操作玩
     *
     * @return
     */
    protected boolean tryStartZiMo(AbsMJSetRound preRound) {
        AbsMJSetPos aPos;
        for (int i = 1; i < this.room.getPlayerNum(); i++) {
            int nexPos = (preRound.getExeOpPos() + i) % this.room.getPlayerNum();
            aPos = set.getMJSetPos(nexPos);
            if (aPos.isHu() || !startWithGetCard(aPos.getPosID(), true)) {
                continue;
            }
            notifyStart();
            return true;
        }
        CommLogD.error("tryStartZiMo RoomID:{},RoomKey:{},ExeOpPos:{},gangDianPao:{} ", this.room.getRoomID(),
                this.room.getRoomKey(), preRound.getExeOpPos());
        return false;
    }

    /**
     * 本回合的操作接炮
     *
     * @return
     */
    protected boolean tryStartJiePao(AbsMJSetRound preRound) {
        List<Integer> huPosList = set.getMHuInfo().getRoundHuPostList(preRound.getRoundID());
        AbsMJSetPos aPos;
        for (int i = 1; i < this.room.getPlayerNum(); i++) {
            int nexPos = (preRound.getExeOpPos() + i) % this.room.getPlayerNum();
            aPos = set.getMJSetPos(nexPos);
            if (aPos.isHu() || huPosList.contains(nexPos) || !startWithGetCard(aPos.getPosID(), true)) {
                continue;
            }
            notifyStart();
            return true;
        }
        CommLogD.error("tryStartJiePao RoomID:{},RoomKey:{},ExeOpPos:{}", this.room.getRoomID(), this.room.getRoomKey(),
                preRound.getExeOpPos());
        return false;
    }

    /**
     * 打牌
     *
     * @param preRound
     * @return
     */
    @Override
    protected boolean tryStartRoundOut(AbsMJSetRound preRound) {
        int opPos = preRound.getExeOpPos();
        // 出牌对应的接手操作
        if (preRound.checkExistNextRoundOp()) {
            // 检查下回合操作位置
            return this.checkNextRoundOpPos(preRound);
        }
        // 无人接手
        else {
            AbsMJSetPos aPos = null;
            for (int i = 1; i < this.set.getPlayerNum(); i++) {
                // 只能顺序的抓牌，打牌
                int nextPos = (opPos + i) % this.room.getPlayerNum();
                aPos = this.set.getMJSetPos(nextPos);
                if (null == aPos || aPos.isHu()) {
                    continue;
                }
                if (!startWithGetCard(nextPos, true)) {
                    return false;
                }
                notifyStart();
                return true;
            }
            return false;
        }
    }

}
