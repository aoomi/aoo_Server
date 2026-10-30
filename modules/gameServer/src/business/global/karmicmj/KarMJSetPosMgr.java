package business.global.karmicmj;

import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetPosMgr;
import business.global.mj.AbsMJSetRoom;
import cenum.mj.OpType;

/**
 * 血战到底基础SetPos
 */
public abstract class KarMJSetPosMgr extends AbsMJSetPosMgr {

    public KarMJSetPosMgr(AbsMJSetRoom set) {
        super(set);
    }

    /**
     * 检测抢杠胡
     *
     * @param curOpPos  操作者位置
     * @param curCardID 操作牌
     */
    public void check_QiangGangHu(int curOpPos, int curCardID) {
        AbsMJSetPos setPos;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = this.set.getMJSetPos(nextPos);
            if (setPos.isHu()) {
                continue;
            }
            OpType oType = setPos.checkPingHu(curOpPos, curCardID);
            if (!OpType.Not.equals(oType)) {
                dealWithQiangGangHu(setPos, nextPos);
            }
        }
    }

    /**
     * 检测地胡
     *
     * @param curOpPos
     * @param curCardID
     * @return
     */
    public boolean check_otherDiHu(int curOpPos, int curCardID) {
        if (!isCanDiHu() || curOpPos != this.set.getDPos()) {
            return false;
        }
        AbsMJSetPos dSetPos = this.set.getMJSetPos(curOpPos);
        if (null == dSetPos) {
            return false;
        }
        if (dSetPos.sizeOutCardIDs() != 1) {
            return false;
        }
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            AbsMJSetPos setPos = this.set.getMJSetPos(nextPos);
            if (setPos.sizeOutCardIDs() > 0) {
                continue;
            }
            OpType oType = setPos.checkPingHu(curOpPos, curCardID);
            if (!OpType.Not.equals(oType)) {
                dealWithDiHu(setPos, nextPos, oType);
            }
        }
        return true;
    }

    /**
     * 检测平胡
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    public void check_otherPingHu(int curOpPos, int curCardID) {
        AbsMJSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = this.set.getMJSetPos(nextPos);
            if (setPos.isHu()) {
                continue;
            }
            OpType oType = setPos.checkPingHu(curOpPos, curCardID);
            if (!OpType.Not.equals(oType)) {
                dealWithPingHu(setPos, nextPos, oType, curOpPos);
            }
        }
    }

    /**
     * 检测碰
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    protected void check_otherPeng(int curOpPos, int curCardID) {
        AbsMJSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = this.set.getMJSetPos(nextPos);
            if (!isCanPeng(setPos, curCardID)) {
                continue;
            }
            // 检查碰动作
            if (setPos.checkOpType(curCardID, OpType.Peng)) {
                dealWithPeng(curCardID, nextPos, setPos);
                return;
            }
        }
    }

    /**
     * 检测接杠
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    protected void check_otherJieGang(int curOpPos, int curCardID) {
        AbsMJSetPos setPos;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = this.set.getMJSetPos(nextPos);
            if (!isCanJieGang(setPos, curCardID)) {
                continue;
            }
            // 检查接杠动作
            if (setPos.checkOpType(curCardID, OpType.JieGang)) {
                this.addOpTypeInfo(nextPos, OpType.JieGang);
                return;
            }
        }
    }

    /**
     * 抢杠胡处理，允许重写
     *
     * @param setPos
     * @param nextPos
     */
    public void dealWithQiangGangHu(AbsMJSetPos setPos, int nextPos) {
        this.addOpTypeInfo(nextPos, OpType.JiePao);
    }

    /**
     * 是否能地胡，允许重写
     *
     * @return
     */
    public boolean isCanDiHu() {
        return true;
    }

    /**
     * 处理地胡，允许重写
     *
     * @param setPos
     * @param nextPos
     * @param oType
     */
    public void dealWithDiHu(AbsMJSetPos setPos, int nextPos, OpType oType) {
        this.addOpTypeInfo(nextPos, oType);
    }

    /**
     * 处理平胡回合，允许重写
     *
     * @param setPos
     * @param nextPos
     * @param oType
     * @param curOpPos
     */
    public void dealWithPingHu(AbsMJSetPos setPos, int nextPos, OpType oType, int curOpPos) {
        this.addOpTypeInfo(nextPos, oType);
    }

    /**
     * 是否能碰,允许重写
     *
     * @param setPos
     * @param curCardID
     * @return
     */
    public boolean isCanPeng(AbsMJSetPos setPos, int curCardID) {
        return !setPos.isHu();
    }

    /**
     * 是否能碰,这里处理了漏碰，不需要的话重写
     *
     * @param curCardID
     * @param nextPos
     * @param setPos
     */
    public void dealWithPeng(int curCardID, int nextPos, AbsMJSetPos setPos) {
        int pengCard = curCardID / 100;
        // 是否重复牌类型
        if (!setPos.getPosOpRecord().isOpCardType(pengCard)) {
            setPos.getPosOpRecord().setOpCardType(pengCard);
            this.addOpTypeInfo(nextPos, OpType.Peng);
        }
    }

    /**
     * 是否能接杠，允许重写
     *
     * @param setPos
     * @param curCardID
     * @return
     */
    public boolean isCanJieGang(AbsMJSetPos setPos, int curCardID) {
        return !setPos.isHu();
    }

}
