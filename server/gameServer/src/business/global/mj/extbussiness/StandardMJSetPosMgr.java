package business.global.mj.extbussiness;

import business.global.mj.AbsMJSetPosMgr;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.AbsMJSetRound;
import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.NextOpType;

import java.util.Objects;

/**
 * 麻将局位置信息
 */
public class StandardMJSetPosMgr extends AbsMJSetPosMgr {

    public StandardMJSetPosMgr(AbsMJSetRoom set) {
        super(set);
    }

    @Override
    public void startSetApplique() {

    }

    /**
     * 检查动作类型。
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     * @param opType    动作类型
     */
    @Override
    public void checkOpType(int curOpPos, int curCardID, OpType opType) {
        // 清空所有动作类型操作									
        this.cleanAllOpType();
        switch (opType) {
            case Out:
            case BaoTing:
                this.checkOutOpType(curOpPos, curCardID);
                break;
            case Gang:
                checkOpTypeQGH(curOpPos, curCardID);
                break;
            default:
                break;
        }
    }


    /**
     * 下个动作类型。
     *
     * @param opType
     * @return
     */
    @Override
    public NextOpType exeCardAction(OpType opType) {
        NextOpType nOpType = null;
        switch (opType) {
            case Out:
            case Gang:
            case BaoTing:
                nOpType = opAllMapOutCard();
                break;
            default:
                break;
        }
        return nOpType;
    }

    /**
     * 检测可以抢杠胡的操作者
     *
     * @param curOpPos  操作者位置
     * @param curCardID 操作牌
     * @return
     */
    @Override
    public void check_QiangGangHu(int curOpPos, int curCardID) {
        // 只能自摸
        if (!this.checkExistPingHu()) {
            return;
        }
        StandardMJSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = (StandardMJSetPos) this.set.getMJSetPos(nextPos);
            setPos.setQiangGangHuFlag(true);
            // 清空操作牌初始
            if (!OpType.Not.equals(setPos.checkPingHu(curOpPos, curCardID))) {
                this.set.getLastOpInfo().setLastOutCard(curCardID);
                this.addOpTypeInfo(nextPos, OpType.QiangGangHu);
            }

            setPos.setQiangGangHuFlag(false);
        }
    }

    /**
     * 检查是否存在接杠补杠 T:接杠时可选择碰后再补杠。
     *
     * @return
     */
    @Override
    protected boolean checkExistJGBG() {
        return false;
    }

    @Override
    protected void check_otherPingHu(int curOpPos, int curCardID) {
        // 只能自摸
        if (!this.checkExistPingHu()) {
            return;
        }
        boolean isGSP = false;
        AbsMJSetRound preRound = set.getPreRound();
        if(preRound!=null && preRound.getOpPos() == curOpPos && (OpType.AnGang.equals(preRound.getOpType()) || OpType.Gang.equals(preRound.getOpType()) || OpType.JieGang.equals(preRound.getOpType()))){
            //上回合是杠，并且本轮补牌后打牌出去后被胡就是杠上炮
            isGSP = true;
        }
        StandardMJSetPos setPos;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = (StandardMJSetPos) this.set.getMJSetPos(nextPos);
            setPos.setGangShangPao(isGSP);
            OpType oType = setPos.checkPingHu(curOpPos, curCardID);
            if (!OpType.Not.equals(oType)) {
                this.addOpTypeInfo(nextPos, oType);
            }
            setPos.setGangShangPao(false);

        }
    }

    @Override
    protected void check_otherPeng(int curOpPos, int curCardID) {
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            StandardMJSetPos modulemjSetPos = (StandardMJSetPos) this.set.getMJSetPos(nextPos);
            int pengCard = curCardID / 100;
            if (!modulemjSetPos.getPosOpRecord().isOpCardType(pengCard) && modulemjSetPos.checkOpType(curCardID, OpType.Peng)) {
                // 检查是否有可打的私有牌
                // 检查是否有可打的私有牌
                if(((StandardMJRoom)set.getRoom()).isLouPeng()){
                    modulemjSetPos.getPosOpRecord().setOpCardType(pengCard);
                }
                this.addOpTypeInfo(nextPos, OpType.Peng);
                return;
            }
        }
    }

    /**
     * 检查出牌后是否有人可以接手。
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    @Override
    protected void checkOutOpType(int curOpPos, int curCardID) {
        if (!check_otherDiHu(curOpPos, curCardID)) {
            check_otherPingHu(curOpPos, curCardID);
        }
        check_otherJieGang(curOpPos, curCardID);
        check_otherPeng(curOpPos, curCardID);
        check_LowerChi(curOpPos, curCardID);
    }

    /**
     * 检测其他人是否可以接杠
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    protected void check_otherJieGang(int curOpPos, int curCardID) {
        StandardMJSetPos setPos;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = (StandardMJSetPos) this.set.getMJSetPos(nextPos);
            // 检查接杠动作
            if (setPos.checkOpType(curCardID, OpType.JieGang)) {
                this.addOpTypeInfo(nextPos, OpType.JieGang);
                return;
            }
        }
    }

    /**
     * 检测其他人是否可以平胡
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    private boolean check_otherDiHu(int curOpPos, int curCardID) {
        if (this.set.getDPos() != curOpPos) {
            // 不是庄家打的牌不检查
            return false;
        }
        StandardMJSetPos setPos = (StandardMJSetPos) this.set.getMJSetPos(curOpPos);
        if (Objects.isNull(setPos)) {
            // 找不到位置
            return false;
        }
        if (setPos.sizeOutCardIDs() != 1) {
            // 庄家打了不止一张牌
            return false;
        }
        boolean existDiHu = false;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = (StandardMJSetPos) this.set.getMJSetPos(nextPos);
            OpType oType = setPos.checkPingHu(nextPos, curCardID);
            if (!OpType.Not.equals(oType)) {
                existDiHu = true;
                this.addOpTypeInfo(nextPos, oType);
            }
            setPos.setDiHuFlag(false);
        }
        return existDiHu;
    }

    //---------------下面方法根据需求自己扩展----------------------
    /**
     * 检查是否存在一炮多响
     */
    @Override
    protected boolean checkExistYPDX() {
        return false;
    }

    /**
     * 检查是否存在吃牌
     */
    @Override
    protected boolean checkExistChi() {
        return true;
    }

    /**
     * 检查是否存在平胡
     *
     * @return
     */
    @Override
    protected boolean checkExistPingHu() {
        return true;
    }


}										
