package business.global.mj.template;

import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetPosMgr;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.manage.MJFactory;
import business.global.mj.template.optype.MJTemplateBuHuaImpl;
import cenum.mj.FlowerEnum;
import cenum.mj.MJHuOpType;
import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.NextOpType;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;


public class MJTemplateSetPosMgr extends AbsMJSetPosMgr {

    public MJTemplateSetPosMgr(AbsMJSetRoom set) {
        super(set);
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
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            MJTemplateSetPos setPos = (MJTemplateSetPos) this.set.getMJSetPos(nextPos);
            //如果跟定缺的牌一样，不能吃碰杠胡
            if (setPos.checkIsQue(curCardID / 100)) {
                continue;
            }
            if (!setPos.checkPingHu(curOpPos, curCardID).equals(OpType.Not)) {
                setPos.setmHuOpType(MJHuOpType.QGHu);
                this.addOpTypeInfo(nextPos, OpType.QiangGangHu);
            }
        }

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
                checkOutOpType(curOpPos, curCardID);
                break;
            case Gang:
            case YaoGang:
                checkOpTypeQGH(curOpPos, curCardID);
                break;
            case TianTing:
                this.checkTianTing(curOpPos);
                break;
            case TiPai:
                this.check_QiangTiHu(curOpPos, curCardID);
                break;
            default:
                break;
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
        check_otherPingHu(curOpPos, curCardID);
        if (!getSet().getRoom().checkExistLastCardOnlyJiePao() || !((MJTemplateSetCard) this.set.getMJSetCard()).isPopCardNull()) {
            check_otherJieGang(curOpPos, curCardID);
            check_otherYaoGang(curOpPos, curCardID);
            check_otherPeng(curOpPos, curCardID);
            check_LowerChi(curOpPos, curCardID);
        }
    }

    public void check_otherYaoGang(int curOpPos, int curCardID) {
        if (MJTemplateRoomEnum.YaoGang.NOT.equals(((MJTemplateRoom) this.set.getRoom()).wanFa_YaoGang())) {
            return;
        }
        MJTemplateSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = (MJTemplateSetPos) this.set.getMJSetPos(nextPos);
            //如果跟定缺的牌一样，不能吃碰杠胡
            if (MJTemplateRoomEnum.DingQue.DING_QUE.equals(setPos.getRoom().wanFa_DingQue()) &&
                    setPos.checkIsQue(curCardID / 100)) {
                continue;
            }
            //摇杠过了 不能再杠
            if (setPos.isYaoGang) {
                continue;
            }
            // 检查接杠动作
            if (CollectionUtils.isNotEmpty(setPos.getJieGangList()) || setPos.checkOpType(curCardID, OpType.JieGang)) {
                setPos.getYaoGangList().addAll(setPos.getJieGangList());
                this.addOpTypeInfo(nextPos, OpType.YaoGang);
                if (!this.checkExistJGBG()) {
                    // 接杠时不可选择碰后再补杠。
                    this.set.getLastOpInfo().addBuGang(curCardID / 100, OpType.YaoGang);
                }
                return;
            }
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
            case YaoGang:
            case TianTing:
            case TiPai:
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

    public void check_QiangTiHu(int curOpPos, int curCardID) {
        MJTemplateSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = (MJTemplateSetPos) this.set.getMJSetPos(nextPos);
            //如果跟定缺的牌一样，不能吃碰杠胡
            if (setPos.checkIsQue(curCardID / 100)) {
                continue;
            }
            if (!setPos.checkPingHu(curOpPos, curCardID).equals(OpType.Not)) {
                setPos.setmHuOpType(MJHuOpType.QiangTiHu);
                this.addOpTypeInfo(nextPos, OpType.QiangTiHu);
            }
        }

    }

    /**
     * 检查天听 庄家打完牌的时候判断
     *
     * @param curOpPos
     * @return
     */

    protected void checkTianTing(int curOpPos) {
        AbsMJSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = this.set.getMJSetPos(nextPos);
            if (nextPos == this.set.getDPos()) {
                continue;
            }
            setPos.calcHuFan();
            if (!setPos.getHuCardTypes().isEmpty()) {// 闲家判断天听
                this.addOpTypeInfo(nextPos, OpType.TianTing);
            }
        }
    }

    /**
     * 检测其他人是否可以接杠
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    @Override
    protected void check_otherJieGang(int curOpPos, int curCardID) {
        MJTemplateSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = (MJTemplateSetPos) this.set.getMJSetPos(nextPos);
            //如果跟定缺的牌一样，不能吃碰杠胡
            if (MJTemplateRoomEnum.DingQue.DING_QUE.equals(setPos.getRoom().wanFa_DingQue()) &&
                    setPos.checkIsQue(curCardID / 100)) {
                continue;
            }
            if (setPos.isYaoGang) {
                continue;
            }
            // 检查接杠动作
            if (setPos.checkOpType(curCardID, OpType.JieGang)) {
                this.addOpTypeInfo(nextPos, OpType.JieGang);
                if (!this.checkExistJGBG()) {
                    // 接杠时不可选择碰后再补杠。
                    this.set.getLastOpInfo().addBuGang(curCardID / 100, OpType.JieGang);
                }
            }
        }
    }

    /**
     * 检测其他人是否可以碰
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    @Override
    protected void check_otherPeng(int curOpPos, int curCardID) {
        MJTemplateSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = (MJTemplateSetPos) this.set.getMJSetPos(nextPos);
            //如果跟定缺的牌一样，不能吃碰杠
            if (setPos.checkIsQue(curCardID / 100)) {
                continue;
            }
            //已经报听的玩家 不能吃碰
            if (setPos.isTing()) {
                continue;
            }
            //已经胡了的不能吃碰
            if (setPos.isHuCard()) {
                continue;
            }
            if (setPos.checkOpType(curCardID, OpType.Peng)) {
                int pengCard = curCardID / 100;
                // 是否重复牌类型
                if (!setPos.getPosOpRecord().isOpCardType(pengCard)) {
                    if (getSet().getRoom().checkExistLouPeng()) {
                        setPos.getPosOpRecord().setOpCardType(pengCard);
                    }
                    this.addOpTypeInfo(nextPos, OpType.Peng);
                }
            }
        }
    }

    /**
     * 检查下家 吃
     * c
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     * @return
     */
    protected void check_LowerChi(int curOpPos, int curCardID) {
        // 检查是否存在吃牌
        if (!this.checkExistChi()) {
            return;
        }

        int nextPos = (curOpPos + 1) % this.set.getRoom().getPlayerNum();
        MJTemplateSetPos setPos = (MJTemplateSetPos) this.set.getMJSetPos(nextPos);
        //如果跟定缺的牌一样，不能吃碰杠胡
        if (setPos.checkIsQue(curCardID / 100)) {
            return;
        }
        //已经报听的玩家 不能吃碰
        if (setPos.isTing()) {
            return;
        }
        //已经胡了的不能吃碰杠
        if (setPos.isHuCard()) {
            return;
        }
        if (setPos.checkOpType(curCardID, OpType.Chi)) {
            // 添加动作信息
            this.addOpTypeInfo(nextPos, OpType.Chi);
        }
    }

    /**
     * 检查是否存在一炮多响
     */
    @Override
    protected boolean checkExistYPDX() {
        return true;
    }

    /**
     * 检查是否存在吃牌
     */
    @Override
    protected boolean checkExistChi() {
        return true;
    }

    /**
     * 检查是否存在接杠补杠 T:接杠时可选择碰后再补杠。
     *
     * @return
     */
    @Override
    protected boolean checkExistJGBG() {
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


    @Override
    public void startSetApplique() {
        //没有需要补花的
        if (((MJTemplateRoom) this.set.getRoom()).getBuHuaTypeSet().isEmpty()) {
            return;
        }
        // 标识是否有开局补花
        boolean isStartSetApplique = false;
        for (int i = 0; i < this.set.getPlayerNum(); i++) {
            int index = (this.set.getDPos() + i) % this.set.getPlayerNum();
            AbsMJSetPos mPos = this.set.getMJSetPos(index);
            if (null == mPos) {
                continue;
            }
            if (MJFactory.getOpCard(MJTemplateBuHuaImpl.class).checkOpCard(mPos, FlowerEnum.PRIVATE.ordinal())) {
                isStartSetApplique = true;

            }
        }
        if (isStartSetApplique) {
            // 存在开局补花。
            startSetApplique();
        }
    }

    public MJTemplateRoomSet getSet() {
        return (MJTemplateRoomSet) this.set;
    }


}
