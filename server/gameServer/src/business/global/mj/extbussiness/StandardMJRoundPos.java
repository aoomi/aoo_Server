package business.global.mj.extbussiness;

import business.global.mj.AbsMJRoundPos;
import business.global.mj.AbsMJSetRound;
import business.global.mj.MJCard;
import business.global.mj.extbussiness.dto.StandardMJOpCard;
import business.global.mj.set.MJOpCard;
import cenum.mj.*;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 麻将 一个round回合中，可能同时等待多个pos进行操作，eg:抢杠胡
 *
 * @author Administrator
 */
public class StandardMJRoundPos extends AbsMJRoundPos {

    private StandardMJSetPos pxPos;

    public StandardMJRoundPos(AbsMJSetRound round, int opPos) {
        super(round, opPos);
        this.pxPos = (StandardMJSetPos) this.getPos();
    }

    /**
     * 打牌
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @param cardID  牌值
     * @return
     */
    public int opOutCard(WebSocketRequest request, OpType opType, int cardID) {
        // 操作错误s									
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 检查牌是否存在									
        MJCard card = getCardByID(cardID);
        if (null == card) {
            request.error(ErrorCode.NotAllow, "1not find cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        if (pxPos.isTing()) {
            if (null != pxPos.getHandCard()) {
                if (pxPos.getHandCard().getCardID() != cardID) {
                    request.error(ErrorCode.NotAllow, "4not find cardID:" + cardID);
                    return -1;
                }
            } else {
                request.error(ErrorCode.NotAllow, "5not find cardID:" + cardID);
                return -1;
            }
        }
        if (pxPos.getBuNengChuList().contains(cardID / 100)) {
            request.error(ErrorCode.NotAllow, "getBunengChu:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 是不是自己身上的牌
        if (!outCard(card)) {
            request.error(ErrorCode.NotAllow, "2not find cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // =====================================
        this.setOpCard(cardID);
        pxPos.clearOp();
        ((StandardMJRoomSet) set).outCard(this.opPos, cardID);
        // 执行动作
        return this.exeCardAction(opType);
    }


    /**
     * 手上有门牌的操作。
     *
     * @param cardID
     */
    @Override
    protected int getCardOpPos(OpType opType, int cardID) {
        if (OpType.Hu.equals(opType) || OpType.DuiBao.equals(opType) || OpType.MoBao.equals(opType)) {
            // 操作动作			
            if (!doOpType(cardID, opType)) {
                return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
            }
        } else {
            return MJOpCardError.NONE.value();
        }
        // 记录操作的动作，并且尝试结束本回合			
        this.opTypeTryEndRound(this.opPos, opType, (opType == OpType.DuiBao || opType == OpType.MoBao) ? HuType.ZiMo : MJCEnum.OpHuType(opType), TryEndRoundEnum.ALL_WAIT);

        return this.opPos;
    }

    @Override
    public int op(WebSocketRequest request, OpType opType, MJOpCard mOpCard) {
        if (mOpCard instanceof StandardMJOpCard) {
            return op(request, opType, (StandardMJOpCard) mOpCard);
        } else {
            return op(request, opType, new StandardMJOpCard(mOpCard.getOpCard()));
        }
    }

    public int op(WebSocketRequest request, OpType opType, StandardMJOpCard mOpCard) {
        int opCardRet = -1;
        if (this.getOpType() != null) {
            request.error(ErrorCode.NotAllow, "opPos has opered");
            return MJOpCardError.REPEAT_EXECUTE.value();
        }
        switch (opType) {
            case Out:
                opCardRet = opOutCard(request, opType, mOpCard.getOpCard());
                break;
            case AnGang:
                opCardRet = op_AnGang(request, opType, mOpCard);
                break;
            case JieGang:
                opCardRet = op_JieGang(request, opType, mOpCard);
                break;
            case Gang:
                opCardRet = opGang(request, opType, mOpCard);
                break;
            case Pass:
                opCardRet = opPass(request, opType);
                break;
            case Peng:
                opCardRet = opPeng(request, opType);
                break;
            case Chi:
                opCardRet = opChi(request, opType, mOpCard.getGangCardList());
                break;
            case BaoTing:
                opCardRet = op_BaoTing(request, opType, mOpCard.getOpCard());
                break;
            case HuanSanZhang: // 换三张
                // 确定选牌
                opCardRet = opChangeCard(request, opType, mOpCard.getGangCardList());
                break;
            // 自摸.
            case Hu:
            case QiangGangHu:
            case JiePao:
            case TianHu:
                opCardRet = opHuType(request, opType);
                break;
            default:
                break;
        }
        request.response();
        return opCardRet;
    }

    @Override
    public int opHuType(WebSocketRequest request, OpType opType) {
        // 操作错误									
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 执行操作									
        return opErrorReturn(request, opType, this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE));
    }

    // 3.0接杠
    public int op_JieGang(WebSocketRequest request, OpType opType, StandardMJOpCard mOpCard) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return -1;

        }
        List<Integer> chiList = mOpCard.getGangCardList();
        // 检查是否有吃牌列表
        if (CollectionUtils.isEmpty(((StandardMJSetPos) this.getPos()).getJieGangList()) || CollectionUtils.isEmpty(chiList)) {
            request.error(ErrorCode.NotAllow, "not angang");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 检查是否有指定要吃的牌
        if (pxPos.getJieGangList().stream().noneMatch(k -> k.size() == chiList.size() && chiList.containsAll(k))) {
            request.error(ErrorCode.NotAllow, "isRet not chi");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 记录操作值
        this.getSetPosMgr().setOpValue(opType, this.getOpPos(), chiList.get(0));
        // 设置吃牌列表
        ((StandardMJSetPos) this.pos).setChiCardList(chiList);
        this.opCard = this.getLastOutCard();
        this.setPosMgr.setOpValue(opType, this.getOpPos(), this.getLastOutCard());
        // 执行操作
        int ret = this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE);
        if (ret < 0) {
            request.error(ErrorCode.NotAllow, "op :{%s},ret :{%d}", opType.toString(), ret);
            return -1;
        }
        return ret;
    }

    /**
     * 吃
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @param chiList 吃牌列表
     * @return
     */
    public int opChi(WebSocketRequest request, OpType opType, List<Integer> chiList) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 检查是否有吃牌列表
        if (CollectionUtils.isEmpty(this.getPos().getPosOpNotice().getChiList()) || CollectionUtils.isEmpty(chiList)) {
            request.error(ErrorCode.NotAllow, "not chi");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 检查是否有指定要吃的牌
        if (!this.getPos().getPosOpNotice().getChiList().stream().anyMatch(k -> k.contains(chiList.get(0)))) {
            request.error(ErrorCode.NotAllow, "isRet not chi");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 记录操作值
        this.getSetPosMgr().setOpValue(opType, this.getOpPos(), chiList.get(0));
        // 设置吃牌列表
        ((StandardMJSetPos) this.pos).setChiCardList(chiList);
        // 操作返回
        return opErrorReturn(request, opType, this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE));
    }

    // 3.2报听
    public int op_BaoTing(WebSocketRequest request, OpType opType, int cardID) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return -1;
        }
        //闲家没手牌扣听
        if (getPos().getHandCard() == null) {
            //闲家扣听
            // 执行操作
            int ret = this.opReturn(opType, 0, TryEndRoundEnum.ALL_WAIT);
            if (ret < 0) {
                request.error(ErrorCode.NotAllow, "op :{%s},ret :{%d}", opType.toString(), ret);
                return -1;
            }
            this.pxPos.setTing(true);
            return ret;
        } else {
            Set<Integer> tingList = getPos().getPosOpNotice().getTingCardMap().keySet();
            if (CollectionUtils.isEmpty(tingList)) {
                request.error(ErrorCode.NotAllow, "null == pos.getTingList() || pos.getTingList().size() <= 0");
                return -1;
            }
            if (!tingList.contains(cardID / 100)) {
                request.error(ErrorCode.NotAllow, "pos.getTingList().contains(cardID)");
                return -1;
            }
            // pos 出牌
            MJCard card = getCardByID(cardID);
            if (null == card) {
                request.error(ErrorCode.NotAllow, "1not find cardID:" + cardID);
                return -1;
            }
            this.pxPos.setTing(true);
            if (!outCard(card)) {
                this.pxPos.setTing(false);
                request.error(ErrorCode.NotAllow, "2not find cardID:" + cardID);
                return -1;
            }
            // =====================================
            // 记录当前回合操作的牌
            this.setOpCard(cardID);
            // 执行动作
            return this.exeCardAction(OpType.BaoTing);
        }
    }

    // 2.3暗杠
    public int op_AnGang(WebSocketRequest request, OpType opType, StandardMJOpCard mOpCard) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return -1;
        }
        List<Integer> chiList = mOpCard.getGangCardList();
        // 检查是否有吃牌列表
        if (CollectionUtils.isEmpty(((StandardMJSetPos) this.getPos()).getAnGangList()) || CollectionUtils.isEmpty(chiList)) {
            request.error(ErrorCode.NotAllow, "not angang");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 检查是否有指定要吃的牌
        if (pxPos.getAnGangList().stream().noneMatch(k -> k.size() == chiList.size() && chiList.containsAll(k))) {
            request.error(ErrorCode.NotAllow, "isRet not chi");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 记录操作值
        this.getSetPosMgr().setOpValue(opType, this.getOpPos(), chiList.get(0));
        // 设置吃牌列表
        ((StandardMJSetPos) this.pos).setChiCardList(chiList);
        // pos 碰牌
        if (!doOpType(mOpCard.getOpCard(), opType)) {
            request.error(ErrorCode.NotAllow, "not op_AnGang");
            return -1;
        }
        // 记录操作的牌ID
        this.setOpCard(mOpCard.getOpCard());
        // 记录操作的动作，并且尝试结束本回合
        this.opNotHu(this.opPos, opType, TryEndRoundEnum.ALL_WAIT);
        return this.opPos;
    }

    /**
     * 明杠
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @return
     */
    public int opGang(WebSocketRequest request, OpType opType, StandardMJOpCard mOpCard) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        List<Integer> chiList = mOpCard.getGangCardList();
        // 检查是否有吃牌列表
        if (CollectionUtils.isEmpty(((StandardMJSetPos) this.getPos()).getBuGangList()) || CollectionUtils.isEmpty(chiList)) {
            request.error(ErrorCode.NotAllow, "not angang");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 检查是否有指定要吃的牌
        if (pxPos.getBuGangList().stream().noneMatch(k -> k.size() == chiList.size() && chiList.containsAll(k))) {
            request.error(ErrorCode.NotAllow, "isRet not chi");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 记录操作值
        this.getSetPosMgr().setOpValue(opType, this.getOpPos(), chiList.get(0));
        // 设置吃牌列表
        ((StandardMJSetPos) this.pos).setChiCardList(chiList);
        // 执行明杠操作
        if (!doOpType(mOpCard.getOpCard(), opType)) {
            request.error(ErrorCode.NotAllow, "not op_Gang");
            return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
        }
        // 记录操作的牌ID
        this.setOpCard(mOpCard.getOpCard());
        // 记录操作的动作，并且尝试结束本回合
        return this.exeCardAction(opType);
    }

    /**
     * 换三张
     *
     * @param request
     * @param opType
     * @param cardList
     * @return
     */
    public int opChangeCard(WebSocketRequest request, OpType opType, List<Integer> cardList) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        //如果不存在玩法
        if (((StandardMJRoom) pxPos.getRoom()).getHuanZhangNum() <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
            //同色牌检测
        } else if (((StandardMJRoom) pxPos.getRoom()).isHuanTongSe() && cardList.stream().collect(Collectors.groupingBy(k -> k / 1000, Collectors.counting())).size() != 1) {
            request.error(ErrorCode.NotAllow, " ext opHuanSanZhang must SameColorCard");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        //换牌数量是否一致
        if (cardList == null || cardList.size() != ((StandardMJRoom) pxPos.getRoom()).getHuanZhangNum()) {
            request.error(ErrorCode.NotAllow, " ext  opHuanSanZhang  error cardList is null or size!=3");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        //是否换完牌了了
        if (!pxPos.getChangeCardList().isEmpty()) {
            request.error(ErrorCode.NotAllow, " ext opHuanSanZhang not allow op again");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        //检测换牌是否是自己的牌
        if (!cardList.stream().allMatch(k -> pxPos.allCards().stream().anyMatch(mjCard -> mjCard.cardID == k))) {
            request.error(ErrorCode.NotAllow, " ext opHuanSanZhang cardList not contains error");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        pxPos.removeAllPrivateCards(cardList);
        if (!Objects.isNull(pxPos.getHandCard()) && cardList.contains(pxPos.getHandCard().getCardID())) {
            pxPos.cleanHandCard();
        }
        pxPos.setChangeCardList(new ArrayList<>(cardList));
        return this.exeCardAction(opType);
    }

    public StandardMJSetPos getPxPos() {
        return pxPos;
    }
}
