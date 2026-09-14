package business.global.mj.template;

import business.global.mj.AbsMJSetOp;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.hu.MJTemplateQiangGangHuCardImpl;
import business.global.mj.hu.MJTemplateQiangTiHuCardImpl;
import business.global.mj.manage.MJFactory;
import business.global.mj.op.ChiCardNormalImpl;
import business.global.mj.template.optype.*;
import cenum.mj.HuType;
import cenum.mj.MJHuOpType;
import cenum.mj.OpType;
import lombok.Data;

/**
 * 当局操作动作
 *
 * @author
 */
@Data
public class MJTemplateSetOp extends AbsMJSetOp {
    /**
     * 玩家信息
     */
    public MJTemplateSetPos mSetPos;

    public MJTemplateSetOp(MJTemplateSetPos mSetPos) {
        super();
        this.mSetPos = mSetPos;
    }

    @Override
    public boolean doOpType(int cardID, OpType opType) {
        boolean doOpType = false;
        switch (opType) {
            case AnGang:
                doOpType = MJFactory.getOpCard(MJTemplateAnGangCardImpl.class).doOpCard(mSetPos, cardID);
                break;
            case YaoGang:
                doOpType = MJFactory.getOpCard(MJTemplateYaoGangCardImpl.class).doOpCard(mSetPos, cardID);
                break;
            case Gang:
                doOpType = MJFactory.getOpCard(MJTemplateGangCardImpl.class).doOpCard(mSetPos, cardID);
                break;
            case JieGang:
                doOpType = MJFactory.getOpCard(MJTemplateJieGangCardImpl.class).doOpCard(mSetPos, cardID);
                break;
            case Peng:
                doOpType = MJFactory.getOpCard(MJTemplatePengCardImpl.class).doOpCard(mSetPos, cardID);
                break;
            case Chi:
                doOpType = MJFactory.getOpCard(ChiCardNormalImpl.class).doOpCard(mSetPos, cardID);
                break;
            case QiangGangHu:
                doOpType = MJFactory.getHuCard(MJTemplateQiangGangHuCardImpl.class).checkHuCard(mSetPos.getMJSetPos());
                break;
            case QiangTiHu:
                doOpType = MJFactory.getHuCard(MJTemplateQiangTiHuCardImpl.class).checkHuCard(mSetPos.getMJSetPos());
                break;
            case JiePao:
            case Hu:
            case GSKH:
                doOpType = doPingHu();
                break;
            default:
                break;
        }
        doResult(opType, doOpType);
        return doOpType;
    }

    /**
     * 处理操作结果
     * 1.杠上开花
     * 2.杠上跑
     * 3.杠后增加留牌数
     *
     * @param opType
     * @param doOpType
     */
    public void doResult(OpType opType, boolean doOpType) {
        if (!doOpType) {
            return;
        }
        //杠上开花
        if (opType == OpType.Gang || opType == OpType.JieGang || opType == OpType.AnGang || opType == OpType.YaoGang) {
            ((MJTemplateRoomSet) mSetPos.getSet()).setGSP(false);
            getMSetPos().setGSKH(true);
            //  3.杠后增加留牌数
            ((MJTemplateSetCard) mSetPos.getSet().getSetCard()).addLiuPai();
            //实时算分
            getMSetPos().actualTimeCalcGangPoint(opType);
        }
        if (opType == OpType.Peng || opType == OpType.Chi) {
            ((MJTemplateRoomSet) mSetPos.getSet()).setGSP(false);
        }
    }

    public boolean doPingHu() {
        if (MJHuOpType.JiePao.equals(mSetPos.getmHuOpType())) {
            int lastOutCard = mSetPos.getSet().getLastOpInfo().getLastOutCard();
            MJTemplateSetPos dianPaoPos = (MJTemplateSetPos) mSetPos.getMJSetPos(mSetPos.getSet().getLastOpInfo().getLastOpPos());
            if (dianPaoPos.getHuType().equals(HuType.NotHu)) {
                dianPaoPos.setHuType(HuType.DianPao);
            }
            //点炮的那张要清掉
            if (dianPaoPos.removeOutCardIDs(lastOutCard)) {
                //通知客户端刷新
                dianPaoPos.getTemplateRoomSet().notifyRefreshOutCard(dianPaoPos);
            }
            if (lastOutCard > 0 && !getMSetPos().getRoom().isWanFa_XueLiuMoShi()) {
                mSetPos.setHandCard(new MJCard(lastOutCard));
            }
        }
        return true;
    }


    @Override
    public boolean checkOpType(int cardID, OpType opType) {
        boolean isOpType = false;
        switch (opType) {
            case AnGang:
                isOpType = MJFactory.getOpCard(MJTemplateAnGangCardImpl.class).checkOpCard(mSetPos, 0);
                break;
            case Gang:
                isOpType = MJFactory.getOpCard(MJTemplateGangCardImpl.class).checkOpCard(mSetPos, cardID);
                break;
            case YaoGang:
                isOpType = MJFactory.getOpCard(MJTemplateYaoGangCardImpl.class).checkOpCard(mSetPos, cardID);
                break;
            case JieGang:
                isOpType = MJFactory.getOpCard(MJTemplateJieGangCardImpl.class).checkOpCard(mSetPos, cardID);
                break;
            case Peng:
                isOpType = MJFactory.getOpCard(MJTemplatePengCardImpl.class).checkOpCard(mSetPos, cardID);
                break;
            case Chi:
                isOpType = MJFactory.getOpCard(ChiCardNormalImpl.class).checkOpCard(mSetPos, cardID);
                break;
            case Ting:
                isOpType = MJFactory.getTingCard(getMSetPos().getmActMrg()).checkTingList(mSetPos);
                break;
            case TianTing:
            case Wan:
            case Tiao:
            case Tong:
            case HuanSanZhang:
                isOpType = true;
                break;
            case Hu:
            case JiePao:
            case QiangGangHu:
            case QiangTiHu:
            case GSKH:
                MJCardInit mjCardInit = mSetPos.mjCardInit(true);
                mjCardInit.addCardInts(cardID / 100);
                return MJFactory.getHuCard(getMSetPos().getRoom().getHuCardImpl()).checkHuCard(mSetPos, mjCardInit);
            default:
                break;
        }
        return isOpType;
    }


    @Override
    public void clear() {
        this.mSetPos = null;
    }


}
