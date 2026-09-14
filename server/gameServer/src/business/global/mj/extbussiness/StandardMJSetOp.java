package business.global.mj.extbussiness;

import business.global.mj.AbsMJSetOp;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.extbussiness.dto.StandardMJPointItem;
import business.global.mj.extbussiness.hutype.StandardMJTingImpl;
import business.global.mj.extbussiness.optype.*;
import business.global.mj.hu.QiangGangHuCardImpl;
import business.global.mj.manage.MJFactory;
import cenum.mj.MJHuOpType;
import cenum.mj.OpType;
import jsproto.c2s.iclass.mj._Promptly;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StandardMJSetOp extends AbsMJSetOp {
    // 操作列表
    private List<OpType> opTypes = new ArrayList<>();
    // 玩家信息
    private StandardMJSetPos mSetPos;

    public StandardMJSetOp(StandardMJSetPos mSetPos) {
        super();
        this.mSetPos = mSetPos;
    }

    public StandardMJSetPos getmSetPos() {
        return mSetPos;
    }

    @Override
    public boolean doOpType(int cardID, OpType opType) {
        boolean doOpType = false;
        switch (opType) {
            case AnGang:
                doOpType = MJFactory.getOpCard(getAnGangClass()).doOpCard(mSetPos, cardID);
                this.addOp(doOpType, OpType.AnGang, cardID / 100);
                break;
            case Gang:
                doOpType = MJFactory.getOpCard(getBuGangClass()).doOpCard(mSetPos, cardID);
                this.addOp(doOpType, OpType.Gang, cardID / 100);
                break;
            case JieGang:
                doOpType = MJFactory.getOpCard(getJieGangClass()).doOpCard(mSetPos, cardID);
                this.addOp(doOpType, OpType.JieGang, cardID / 100);
                break;
            case Peng:
                doOpType = MJFactory.getOpCard(getPengClass()).doOpCard(mSetPos, cardID);
                break;
            case Chi:
                doOpType = MJFactory.getOpCard(getChiClass()).doOpCard(mSetPos, cardID);
                break;
            case JiePao:
            case Hu:
            case TianHu:
                doOpType = doPingHu();
                break;
            case QiangGangHu:
                doOpType = MJFactory.getHuCard(QiangGangHuCardImpl.class).checkHuCard(mSetPos.getMJSetPos());
                if (doOpType) {
                    mSetPos.setmHuOpType(MJHuOpType.QGHu);
                }
                if(((StandardMJRoom)mSetPos.getRoom()).isSSSG()){
                    mSetPos.resolveSSSGTuiGang(mSetPos.getSet().getLastOpInfo().getLastOpCard());
                }
                break;
            default:
                break;
        }
        return doOpType;
    }

    @Override
    public boolean checkOpType(int cardID, OpType opType) {
        boolean isOpType = false;
        switch (opType) {
            case AnGang:
                isOpType = MJFactory.getOpCard(getAnGangClass()).checkOpCard(mSetPos, cardID);
                break;
            case Gang:
                isOpType = MJFactory.getOpCard(getBuGangClass()).checkOpCard(mSetPos, cardID);
                break;
            case Chi:
                isOpType = MJFactory.getOpCard(getChiClass()).checkOpCard(mSetPos, cardID);
                break;
            case JieGang:
                isOpType = MJFactory.getOpCard(getJieGangClass()).checkOpCard(mSetPos, cardID);
                break;
            case Peng:
                isOpType = MJFactory.getOpCard(getPengClass()).checkOpCard(mSetPos, cardID);
                break;
            case HuanSanZhang:
                isOpType = true;
                break;
            case Ting:
                isOpType = MJFactory.getTingCard(((StandardMJRoomSet)mSetPos.getSet()).getTingClass()).checkTingList(mSetPos);
                break;
            case BaoTing:
                if (((StandardMJRoom) mSetPos.getRoom()).isBaoTing() && !mSetPos.isTing()) {
                    isOpType = MJFactory.getTingCard(((StandardMJRoomSet)mSetPos.getSet()).getTingClass()).checkTingList(mSetPos);
                }
                break;
            case JiePao:
            case TianHu:
            case Hu:
            case QiangGangHu:
                //不能胡
                MJCardInit initCard = mSetPos.mCardInit(mSetPos.allCards(), 0, true);
                if (cardID > 0) {
                    initCard.addCardInts(cardID / 100);
                    initCard.setLastOutCard(cardID / 100);
                } else if (mSetPos.getHandCard() != null) {
                    initCard.setLastOutCard(mSetPos.getHandCard().getType());
                }
                StandardMJTingImpl tingCard = (StandardMJTingImpl) MJFactory.getTingCard(((StandardMJRoomSet) mSetPos.getSet()).getTingClass());
                if(((StandardMJRoom)mSetPos.getRoom()).isTingAnyCard()){
                    //有听任意牌走这个带任意排的方法
                    MJCardInit finalInitCard = mSetPos.mCardInit(mSetPos.getPrivateCards(), 0, true);
                    boolean tingAnyCard = tingCard.isTingAnyCard(finalInitCard,mSetPos);
                    return checkHuCardReturn(tingCard.getPointItem(mSetPos, initCard, cardID == 0,tingAnyCard));
                }else{
                    return checkHuCardReturn(tingCard.getPointItem(mSetPos, initCard, cardID == 0));
                }
            default:
                break;
        }
        return isOpType;
    }

    /**
     * 检查胡返回
     *
     * @return boolean
     */
    public boolean checkHuCardReturn(StandardMJPointItem item) {
        if (Objects.isNull(item)) {
            return false;
        }
        mSetPos.setOpHuType(item);
        return true;
    }


    /**
     * 清除所有动作
     */
    public void clearOp() {
        this.opTypes.clear();
    }


    /**
     * 添加动作
     *
     * @param doOpType 是否操作成功
     * @param opType   动作类型
     */
    public void addOp(boolean doOpType, OpType opType, int cardID) {
        if (doOpType) {
            this.opTypes.add(opType);
        }
    }

    public boolean doPingHu() {
        if (MJHuOpType.JiePao.equals(mSetPos.getmHuOpType())) {
            int lastOutCard = mSetPos.getSet().getLastOpInfo().getLastOutCard();
            if (lastOutCard > 0) {
                mSetPos.setHandCard(new MJCard(lastOutCard));
            }
        }
        return true;
    }

    public int isOpSize() {
        return this.opTypes.size();
    }

    @Override
    public void clear() {
        this.mSetPos = null;
    }

    //---------------下面方法根据需求自己扩展----------------------

    /**
     * 吃牌适配
     * 根据自己游戏决定是否重写吃牌类
     **/
    public Class<?> getChiClass() {
        return StandardMJChiCardImpl.class;
    }

    /**
     * 碰牌适配
     * 根据自己游戏决定是否重写碰牌类
     **/
    public Class<?> getPengClass() {
        return StandardMJPengCardImpl.class;
    }

    /**
     * 接杠适配
     * 根据自己游戏决定是否重写接杠类
     **/
    public Class<?> getJieGangClass() {
        return StandardMJJieGangCardImpl.class;
    }

    /**
     * 补杠适配
     * 根据自己游戏决定是否重写补杠类
     **/
    public Class<?> getBuGangClass() {
        return StandardMJBuGangCardImpl.class;
    }

    /**
     * 暗杠杠适配
     * 根据自己游戏决定是否重写暗杠类
     **/
    public Class<?> getAnGangClass() {
        return StandardMJAnGangCardImpl.class;
    }
}
