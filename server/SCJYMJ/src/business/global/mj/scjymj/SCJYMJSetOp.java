package business.global.mj.scjymj;

import business.global.mj.AbsMJSetOp;
import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.hu.NormalHuCardImpl;
import business.global.mj.hu.PPHuCardImpl;
import business.global.mj.manage.MJFactory;
import business.global.mj.op.PengCardImpl;
import business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJOpPoint;
import business.global.mj.scjymj.hutype.SCJYMJDDHuCardImpl;
import business.global.mj.scjymj.hutype.SCJYMJLongHuCardImpl;
import business.global.mj.scjymj.hutype.SCJYMJQiangGangHuCardImpl;
import business.global.mj.scjymj.hutype.SCJYMJQingYiSeImpl;
import business.global.mj.scjymj.optype.SCJYMJAnGangCardImpl;
import business.global.mj.scjymj.optype.SCJYMJGangCardImpl;
import business.global.mj.scjymj.optype.SCJYMJJieGangCardImpl;
import cenum.mj.MJHuOpType;
import cenum.mj.OpType;

import java.util.ArrayList;
import java.util.List;

import static cenum.mj.OpType.WuDangHu;

/**
 * 江苏扬中麻将
 *
 * @author Administrator
 */
public class SCJYMJSetOp extends AbsMJSetOp {
    // 操作
    private List<OpType> opTypes = new ArrayList<>();
    // 玩家信息
    private SCJYMJSetPos mSetPos;

    public SCJYMJSetOp(SCJYMJSetPos mSetPos) {
        super();
        this.mSetPos = mSetPos;
    }

    @Override
    public boolean doOpType(int cardID, OpType opType) {
        boolean doOpType = false;
        switch (opType) {
            case AnGang:
                doOpType = MJFactory.getOpCard(SCJYMJAnGangCardImpl.class).doOpCard(mSetPos, cardID);
                this.addOp(mSetPos, doOpType, OpType.AnGang);
                break;
            case Gang:
                doOpType = MJFactory.getOpCard(SCJYMJGangCardImpl.class).doOpCard(mSetPos, cardID);
                this.addOp(mSetPos, doOpType, OpType.Gang);
                break;
            case JieGang:
                doOpType = MJFactory.getOpCard(SCJYMJJieGangCardImpl.class).doOpCard(mSetPos, cardID);
                this.addOp(mSetPos, doOpType, OpType.JieGang);
                break;
            case Peng:
                doOpType = MJFactory.getOpCard(PengCardImpl.class).doOpCard(mSetPos, cardID);
                break;
            case QiangGangHu:
                doOpType = MJFactory.getHuCard(SCJYMJQiangGangHuCardImpl.class).checkHuCard(mSetPos.getMJSetPos());
                if (doOpType) {
                    mSetPos.setmHuOpType(MJHuOpType.QGHu);
                }
                break;
            case KouTing:
                return true;
            case JiePao:
            case Hu:
                if (mSetPos.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.QGHu)) {
                    doOpType = MJFactory.getHuCard(SCJYMJQiangGangHuCardImpl.class).checkHuCard(mSetPos.getMJSetPos());
                    if (doOpType) {
                        mSetPos.setmHuOpType(MJHuOpType.QGHu);
                    }
                } else {
                    doOpType = doPingHu(mSetPos);
                }
                break;
            default:
                break;
        }
        return doOpType;
    }

    @Override
    public boolean checkOpType(int cardID, OpType opType) {
        int cardType = cardID / 100;
        boolean isOpType = false;
        switch (opType) {
            case AnGang:
                isOpType = MJFactory.getOpCard(SCJYMJAnGangCardImpl.class).checkOpCard(mSetPos,
                        cardID);
                break;
            case Gang:
                isOpType = MJFactory.getOpCard(SCJYMJGangCardImpl.class).checkOpCard(mSetPos, cardID);
                break;
            case JieGang:
                isOpType = MJFactory.getOpCard(SCJYMJJieGangCardImpl.class).checkOpCard(mSetPos, cardID);
                break;
            case Peng:
                isOpType = MJFactory.getOpCard(PengCardImpl.class).checkOpCard(mSetPos, cardID);
                break;
            case JiePao:
            case Hu:
            case WuDangHu:
                if (mSetPos.getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.Long) && MJFactory.getHuCard(SCJYMJLongHuCardImpl.class).checkHuCard(mSetPos, mSetPos.mCardInit(cardType, false))) {
                    // 	一条龙：同一门花色的123456789等连续牌叫一条龙
                    mSetPos.getPosOpRecord().addOpHuList(SCJYMJOpPoint.Long);
                }
                if (checkHuCardReturn(mSetPos, MJFactory.getHuCard(SCJYMJQingYiSeImpl.class).checkHuCardReturn(mSetPos,
                        mSetPos.mCardInit(cardType, false)))) {
                    // 清一色3番（x8）,清一色对对胡、龙七对、清一色七对 4番（x16）；清一色龙七对5番（x32）
                    return true;
                }
                if (checkHuCardReturn(mSetPos, MJFactory.getHuCard(SCJYMJDDHuCardImpl.class).checkHuCardReturn(mSetPos,
                        mSetPos.mCardInit(cardType, false)))) {
                    // 七对3番（x8）；龙七对 4番（x16）；
                    return true;
                }
                if (MJFactory.getHuCard(PPHuCardImpl.class).checkHuCard(mSetPos, mSetPos.mCardInit(cardType, false))) {
                    // 对对胡1番（x2）；
                    mSetPos.getPosOpRecord().addOpHuList(SCJYMJOpPoint.DDHu);
                    return true;
                }

                if ((cardType != 0 && (mSetPos.getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.DianPaoPingHu) || opType == WuDangHu)) || (cardType == 0)) {
                    if (MJFactory.getHuCard(NormalHuCardImpl.class).checkHuCard(mSetPos, mSetPos.mCardInit(cardType, false))) {
                        // 平胡0番（x1）；
                        mSetPos.getPosOpRecord().addOpHuList(SCJYMJOpPoint.PingHu);
                        return true;
                    }
                }


                return false;
            case Ting:
                isOpType = MJFactory.getTingCard(SCJYMJTingImpl.class).checkTingList(mSetPos);
                break;
            default:
                break;
        }
        return isOpType;
    }

    @Override
    public void clear() {
        this.mSetPos = null;
    }

    public boolean checkHuCardReturn(AbsMJSetPos mSetPos, Object object) {
        if (SCJYMJOpPoint.Not.equals(object)) {
            return false;
        }
        mSetPos.getPosOpRecord().addOpHuList(object);
        return true;
    }

    public boolean doPingHu(AbsMJSetPos mSetPos) {
        if (MJHuOpType.JiePao.equals(mSetPos.getmHuOpType())) {
            int lastOutCard = mSetPos.getSet().getLastOpInfo().getLastOutCard();
            SCJYMJRoomSet set = (SCJYMJRoomSet) mSetPos.getSet();
            if (lastOutCard > 0) {
                set.setLastOutCard(lastOutCard);
                mSetPos.setHandCard(new MJCard(lastOutCard));
                clearOpCardType(lastOutCard);

            } else if (set.getLastOutCard() > 0) {
                mSetPos.setHandCard(new MJCard(set.getLastOutCard()));
                clearOpCardType(set.getLastOutCard());
            }
        }
        return true;
    }

    /**
     * 清空本回合的漏碰，清除
     *
     * @param lastOutCard
     */
    public void clearOpCardType(int lastOutCard) {
        for (int i = 0; i < this.mSetPos.getPlayerNum(); i++) {
            AbsMJSetPos mOSetPos = this.mSetPos.getMJSetPos(i);
            if (mOSetPos == null) {
                continue;
            }
            //存在漏碰
            if (mOSetPos.getPosOpRecord().isOpCardType(lastOutCard)) {
                mOSetPos.getPosOpRecord().removeOpCardType(lastOutCard);
            }
        }
    }

    /**
     * 清除所有动作
     */
    public void cleanOp() {
        this.opTypes.clear();
    }

    /**
     * 添加动作
     *
     * @param doOpType 是否操作成功
     * @param opType   动作类型
     */
    public void addOp(AbsMJSetPos mSetPos, boolean doOpType, OpType opType) {
        if (doOpType) {
            this.opTypes.add(opType);
            ((SCJYMJRoomSet) mSetPos.getSet()).setGangPos(mSetPos.getPosID());
        }
    }

    /**
     * 检查动作
     *
     * @return
     */
    public boolean isOpContains(OpType opType) {
        return this.opTypes.contains(opType);
    }

    /**
     * 检查动作
     *
     * @return
     */
    public int isOpSize() {
        return this.opTypes.size();
    }

}
