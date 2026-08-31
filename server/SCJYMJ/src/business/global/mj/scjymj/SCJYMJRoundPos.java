package business.global.mj.scjymj;

import business.global.mj.AbsMJRoundPos;
import business.global.mj.AbsMJSetRound;
import business.global.mj.MJCard;
import business.global.mj.manage.MJFactory;
import business.global.mj.set.MJOpCard;
import cenum.mj.*;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 江苏扬中麻将
 * 一个round回合中，可能同时等待多个pos进行操作，eg:抢杠胡
 *
 * @author Administrator
 */
public class SCJYMJRoundPos extends AbsMJRoundPos {
    SCJYMJRoomSet bSet;
    SCJYMJSetPos bSetPos;

    public SCJYMJRoundPos(AbsMJSetRound round, int opPos) {
        super(round, opPos);
        this.bSet = (SCJYMJRoomSet) set;
        this.bSetPos = (SCJYMJSetPos) this.pos;
    }

    // / ==================================================
    // / 2 操作手牌
    // 2.1打牌
    public int op_OutCard(WebSocketRequest request, OpType opType, int cardID) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return -1;
        }

        // pos 出牌
        // 通过CardID,获取指定的牌
        MJCard card = getCardByID(cardID);
        if (null == card) {
            request.error(ErrorCode.NotAllow, "1not find cardID:" + cardID);
            return -1;
        }

        AbsMJSetRound preRound = set.getPreRound();
        if (this.bSetPos.isTing() && null != preRound && 1 == preRound.getRoundID() && (preRound.getOpType() == null || preRound.getOpType() == OpType.Pass || preRound.getOpType() == OpType.KouTing)) {
            List<MJCard> allCards = pos.allCards();
            boolean allow = false;
            if (14 == allCards.size()) {
                List<MJCard> tempCards = new ArrayList<>(allCards);
                MJCard chuCard = null;
                for (MJCard cardTemp : tempCards) {
                    if (cardTemp.getCardID() == cardID) {
                        chuCard = cardTemp;
                        break;
                    }
                }
                tempCards.remove(chuCard);
                List<Integer> tingCards = MJFactory.getTingCard(getPos().getmActMrg()).checkTingCard(getPos(), tempCards);
                if (tingCards.size() > 0) {
                    allow = true;
                }
            }
            if (!allow) {
                request.error(ErrorCode.NotAllow, "opPos cannot this card");
                return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
            }
        } else {
            if (this.bSetPos.isTing()) {
                if (null != this.bSetPos.getHandCard()) {
                    if (this.bSetPos.getHandCard().getCardID() != cardID) {
                        request.error(ErrorCode.NotAllow, "4not find cardID:" + cardID);
                        return -1;
                    }
                } else {
                    request.error(ErrorCode.NotAllow, "5not find cardID:" + cardID);
                    return -1;
                }
            }
        }

        // 检查指定的牌是否可以打出
        if (!outCard(card)) {
            request.error(ErrorCode.NotAllow, "3not find cardID:" + cardID);
            return -1;
        }

        // =====================================
        this.bSetPos.cleanOp();
        this.bSetPos.getPosOpRecord().setOpCardType(cardID / 10);

        // 记录当前回合操作的牌
        this.setOpCard(cardID);
        // 执行动作
        return this.exeCardAction(opType);
    }

    // 3 接打牌
    // 3.1过
    public int op_Pass(WebSocketRequest request, OpType opType) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        this.pos.getPosOpNotice().clearBuNengChuList();
        // 执行操作
        return opErrorReturn(request, opType, this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE));
    }

    // 3.2报听
    public int op_BaoTing(WebSocketRequest request, OpType opType) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return -1;
        }
        // 执行操作
        int ret = this.opReturn(opType, 0, TryEndRoundEnum.ALL_WAIT);
        if (ret < 0) {
            request.error(ErrorCode.NotAllow, "op :{%s},ret :{%d}", opType.toString(), ret);
            return -1;
        }
        return ret;
    }

    /**
     * 手上有门牌的操作。
     *
     * @param opType
     * @param cardID
     */
    @Override
    protected int getCardOpPos(OpType opType, int cardID) {
        if (OpType.Hu.equals(opType)) {
            // 操作动作
            if (!doOpType(cardID, opType)) {
                return -1;
            }
        } else {
            return -1;
        }
        // 记录操作的动作，并且尝试结束本回合
        this.opTypeTryEndRound(this.opPos, opType, MJCEnum.OpHuType(opType), TryEndRoundEnum.ALL_WAIT);
        return this.opPos;
    }

    @Override
    public int op(WebSocketRequest request, OpType opType, MJOpCard mOpCard) {
        int opCardRet = -1;
        if (this.getOpType() != null) {
            request.error(ErrorCode.NotAllow, "opPos has opered");
            return opCardRet;
        }
        int opCard = mOpCard.getOpCard();
        switch (opType) {
            case Out:
                opCardRet = op_OutCard(request, opType, opCard);
                break;
            case AnGang:
                opCardRet = opAnGang(request, opType, opCard);
                break;
            case JieGang:
                opCardRet = opJieGang(request, opType);
                break;
            case Gang:
                opCardRet = opGang(request, opType, opCard);
                break;
            case Peng:
                opCardRet = opPeng(request, opType);
                break;
            case Pass:
                opCardRet = op_Pass(request, opType);
                if (errorOpType(request, opType) > 0 && ((SCJYMJSetPos) round.getSet().getPosDict().get(getPos().getPosID())).isTing()) {
                    if (0 <= opCardRet) {
                        ((SCJYMJSetPos) round.getSet().getPosDict().get(getPos().getPosID())).setCanHu(false);
                    }
                }
                break;
            case KouTing:
                opCardRet = op_BaoTing(request, opType);
                break;
            case Hu:
            case QiangGangHu:
            case JiePao:
                opCardRet = opHuType(request, opType);
                break;
            default:
                break;
        }
        request.response();
        return opCardRet;
    }

    /**
     * 胡
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @return
     */
    public int opHuType(WebSocketRequest request, OpType opType) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        int isReturn = this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE);
        if (isReturn >= MJOpCardError.SUCCESS.value()) {
            // 执行操作
            if (!HuType.NotHu.equals(MJCEnum.OpHuType(opType))) {
                // 添加胡牌
                ((SCJYMJRoomSet) this.getSet()).addHuMap(bSetPos.getPosID());
            }
        }
        return opErrorReturn(request, opType, isReturn);
    }

    /**
     * 本回合位置操作 添加动作类型
     *
     * @param opType
     */
    @Override
    public void addOpType(OpType opType) {
        if (null == opType) {
            return;
        }
        if (((SCJYMJRoomSet) this.set).isLastFourCard()) {
            if (this.recieveOpTypes.contains(OpType.Hu)) {
                this.recieveOpTypes.clear();
                this.recieveOpTypes.add(OpType.Hu);
                return;
            } else if (this.recieveOpTypes.contains(OpType.JiePao)) {
                this.recieveOpTypes.clear();
                this.recieveOpTypes.add(OpType.JiePao);
                return;
            }
        }
        this.recieveOpTypes.add(opType);
        this.publicWait = this.recieveOpTypes.contains(OpType.Out);
    }
}
