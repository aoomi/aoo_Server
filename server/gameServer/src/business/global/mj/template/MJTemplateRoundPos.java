package business.global.mj.template;

import business.global.mj.AbsMJRoundPos;
import business.global.mj.AbsMJSetRound;
import business.global.mj.MJCard;
import business.global.mj.set.MJOpCard;
import business.global.mj.set.MJTemplate_OpCard;
import cenum.mj.MJCEnum;
import cenum.mj.MJOpCardError;
import cenum.mj.OpType;
import cenum.mj.TryEndRoundEnum;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import jsproto.c2s.cclass.mj.BaseMJRoom_RoundPos;
import jsproto.c2s.cclass.mj.template.MJTemplateRoom_RoundPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 一个round回合中，可能同时等待多个pos进行操作，eg:抢杠胡
 *
 * @author Administrator
 */
public class MJTemplateRoundPos extends AbsMJRoundPos {

    public MJTemplateRoundPos(AbsMJSetRound round, int opPos) {
        super(round, opPos);
    }

    // / ==================================================
    // / 2 操作手牌
    // 2.1打牌
    @Override
    public int opOutCard(WebSocketRequest request, OpType opType, int cardID) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        if (getPos().getRoom().isWanFa_JinBuNengChu() && getSet().getmJinCardInfo().checkJinExist(cardID)) {
            request.error(ErrorCode.NotAllow, "jin can't out :" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // pos 出牌
        MJCard card = getCardByID(cardID);
        if (null == card) {
            request.error(ErrorCode.NotAllow, "1not find cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        if (pos.getPosOpNotice().getBuNengChuList().contains(cardID)) {
            request.error(ErrorCode.NotAllow, "opOutCard curCard not allow out, cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        if (!outCard(card)) {
            request.error(ErrorCode.NotAllow, "2not find cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        //出牌后如果上次是杠
        ((MJTemplateRoomSet) getPos().getSet()).setGSP(false);
        if (getPos().isGSKH()) {
            getPos().setGSKH(false);
            ((MJTemplateRoomSet) getPos().getSet()).setGSP(true);
        }
        // =====================================
        // 记录当前回合操作的牌
        this.setOpCard(cardID);
        // 执行动作
        return this.exeCardAction(opType);
    }


    /**
     * 手上有门牌的操作。
     *
     * @param opType 操作类型
     * @param cardID
     */
    @Override
    protected int getCardOpPos(OpType opType, int cardID) {
        if (OpType.Hu.equals(opType) || OpType.GSKH.equals(opType)) {
            // 操作动作
            if (!doOpType(cardID, opType)) {
                return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
            }
        } else {
            return MJOpCardError.NONE.value();
        }
        // 记录操作的动作，并且尝试结束本回合
        this.opTypeTryEndRound(this.opPos, opType, MJCEnum.OpHuType(opType), TryEndRoundEnum.ALL_WAIT);
        return this.opPos;
    }


    @SuppressWarnings("unused")
    @Override
    public int op(WebSocketRequest request, OpType opType, MJOpCard mOpCard) {
        int opCardRet = -1;
        if (this.getOpType() != null) {
            request.error(ErrorCode.NotAllow, "opPos has opered");
            return MJOpCardError.REPEAT_EXECUTE.value();
        }
        MJTemplate_OpCard templateOpCard;
        boolean checkFlashOut = false;
        switch (opType) {
            case Out:
                opCardRet = opOutCard(request, opType, mOpCard.getOpCard());
                break;
            case AnGang:
                templateOpCard = (MJTemplate_OpCard) mOpCard;
                opCardRet = opAnGang(request, opType, templateOpCard.getCardList());
                break;
            case JieGang:
                templateOpCard = (MJTemplate_OpCard) mOpCard;
                opCardRet = opJieGang(request, opType, templateOpCard.getCardList());
                break;
            case Gang:
                templateOpCard = (MJTemplate_OpCard) mOpCard;
                opCardRet = opGang(request, opType, templateOpCard.getCardList());
                break;
            case YaoGang:
                templateOpCard = (MJTemplate_OpCard) mOpCard;
                opCardRet = opYaoGang(request, opType, templateOpCard.getCardList());
                break;
            case Pass:
                opCardRet = opPass(request, opType);
                break;
            case Peng:
                opCardRet = opPeng(request, opType);
                break;
            case Chi:
                MJTemplate_OpCard opCard = (MJTemplate_OpCard) mOpCard;
                opCardRet = opChi(request, opType, opCard.getCardList());
                break;
            case BaoTing:
                opCardRet = opBaoTing(request, opType, mOpCard.getOpCard());
                break;
            case TianTing:
                opCardRet = opTianTing(request, opType);
                break;
            case JingDiao:
                opCardRet = opJingDiao(request, opType, mOpCard.getOpCard());
                break;
            case HuanSanZhang: // 换三张
                // 确定选牌
                opCardRet = opChangeCard(request, opType, ((MJTemplate_OpCard) mOpCard).getCardList());
                break;
            case Wan:
            case Tiao:
            case Tong:
                opCardRet = opDingQue(request, opType);
                break;
            // 自摸.
            case Hu:
            case QiangGangHu:
            case JiePao:
            case QiangTiHu:
            case GSKH:
                opCardRet = opHuType(request, opType);
                break;

            default:
                break;
        }
        request.response();
        return opCardRet;
    }


    /**
     * 暗杠
     *
     * @param request    连接请求
     * @param opType     动作类型
     * @param opCardList 牌值
     * @return
     */
    public int opAnGang(WebSocketRequest request, OpType opType, List<Integer> opCardList) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        if (Objects.isNull(opCardList) || !getPos().getAnGangList().contains(opCardList)) {
            request.error(ErrorCode.NotAllow, "not op_Gang opCardList not the same opCardList={}", opCardList.toString());
            return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
        }
        getPos().setOpCardList(opCardList);
        // 执行暗杠操作
        if (!doOpType(opCardList.get(0), opType)) {
            request.error(ErrorCode.NotAllow, "not op_AnGang");
            return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
        }
        // 记录操作的牌ID
        this.setOpCard(opCardList.get(0));
        // 记录操作的动作，并且尝试结束本回合
        this.opNotHu(this.opPos, opType, TryEndRoundEnum.ALL_WAIT);
        return this.opPos;
    }

    /**
     * 接杠
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @return
     */
    public int opJieGang(WebSocketRequest request, OpType opType, List<Integer> opCardList) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        if (Objects.isNull(opCardList) || !getPos().getJieGangList().contains(opCardList)) {
            request.error(ErrorCode.NotAllow, "not op_Gang opCardList not the same opCardList={}", opCardList.toString());
            return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
        }
        getPos().setOpCardList(opCardList);
        // 设置动作值
        this.setPosMgr.setOpValue(opType, this.getOpPos(), this.getLastOutCard());
        // 执行操作
        return opErrorReturn(request, opType, this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE));
    }

    /**
     * 补杠
     *
     * @param request    连接请求
     * @param opType     动作类型
     * @param opCardList 牌值
     * @return
     */
    public int opGang(WebSocketRequest request, OpType opType, List<Integer> opCardList) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        if (Objects.isNull(opCardList) || !getPos().getBuGangList().contains(opCardList)) {
            request.error(ErrorCode.NotAllow, "not op_Gang opCardList not the same opCardList={}", opCardList.toString());
            return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
        }
        getPos().setOpCardList(opCardList);
        // 执行明杠操作
        if (!doOpType(opCardList.get(0), opType)) {
            request.error(ErrorCode.NotAllow, "not op_Gang");
            return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
        }
        // 记录操作的牌ID
        this.setOpCard(opCardList.get(0));
        // 记录操作的动作，并且尝试结束本回合
        return this.exeCardAction(opType);
    }

    /**
     * 摇杠
     *
     * @param request    连接请求
     * @param opType     动作类型
     * @param opCardList 牌值
     * @return
     */
    public int opYaoGang(WebSocketRequest request, OpType opType, List<Integer> opCardList) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        if (Objects.isNull(opCardList) || !getPos().getYaoGangList().contains(opCardList)) {
            request.error(ErrorCode.NotAllow, "not op_Gang opCardList not the same opCardList={}", opCardList.toString());
            return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
        }
        int cardID = opCardList.get(0);
        List<Integer> cards = getPos().allCardIDs().stream().filter(k -> k / 100 == cardID / 100).collect(Collectors.toList());
        if (cards.size() == 4) {
            return opAnGang(request, opType, opCardList);
        } else if (cards.size() == 1) {
            // 记录操作的动作，并且尝试结束本回合
            return opGang(request, opType, opCardList);
        }
        return opJieGang(request, opType, opCardList);
    }

    /**
     * 2.3定缺
     */
    public int opDingQue(WebSocketRequest request, OpType opType) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        if (!getPos().getDingQue().equals(OpType.Not)) {
            request.error(ErrorCode.NotAllow, " MJTemplate opDingQue had op");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        getPos().opDingQue(opType);
        // 记录操作的动作，并且尝试结束本回合
        this.opNotHu(this.opPos, opType, TryEndRoundEnum.ALL_WAIT);
        return this.opPos;
    }

    /**
     * 报听
     *
     * @param request
     * @param opType
     * @return
     */
    protected int opBaoTing(WebSocketRequest request, OpType opType, int cardID) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        if (getPos().getRoom().isWanFa_JinBuNengChu() && getSet().getmJinCardInfo().checkJinExist(cardID)) {
            request.error(ErrorCode.NotAllow, "jin can't out :" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 检查牌是否存在
        MJCard card = getCardByID(cardID);
        if (null == card) {
            request.error(ErrorCode.NotAllow, "1not find cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 是不是自己身上的牌
        if (!outCard(card)) {
            request.error(ErrorCode.NotAllow, "2not find cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        getPos().setTing(true);
        // 记录当前回合操作的牌
        this.setOpCard(cardID);
        // 记录操作的动作，并且尝试结束本回合
        this.opNotHu(this.opPos, opType, TryEndRoundEnum.ALL_WAIT);
        return this.opPos;
    }

    /**
     * 精吊 手上有两张以上的金 打出金后听所有的牌
     *
     * @param request
     * @param opType
     * @return
     */
    protected int opJingDiao(WebSocketRequest request, OpType opType, int cardID) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        MJCard card = getCardByID(cardID);
        // 检查牌是否存在 并且是不是金牌
        if (null == card || getPos().getSet().getmJinCardInfo().checkJinExist(cardID)) {
            request.error(ErrorCode.NotAllow, "1not find cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 是不是自己身上的牌
        if (!outCard(card)) {
            request.error(ErrorCode.NotAllow, "2not find cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 记录当前回合操作的牌
        this.setOpCard(cardID);
        // 记录操作的动作，并且尝试结束本回合
        this.opNotHu(this.opPos, opType, TryEndRoundEnum.ALL_WAIT);
        return this.opPos;
    }

    /**
     * 报听
     *
     * @param request
     * @param opType
     * @return
     */
    protected int opTianTing(WebSocketRequest request, OpType opType) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        getPos().setTing(true);
        // 记录当前回合操作的牌
        // 执行动作
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
        if (MJTemplateRoomEnum.ChangeCardType.NOT.equals(roomSet().getRoom().wanFa_ChangeCardType())) {
            return MJOpCardError.ERROR_OP_TYPE.value();
            //同色牌检测
        } else if (getPos().getRoom().isWanFa_ChangeCardType_SameColor() && cardList.stream().collect(Collectors.groupingBy(k -> k / 1000, Collectors.counting())).size() != 1) {
            request.error(ErrorCode.NotAllow, " MJTemplate opHuanSanZhang must SameColorCard");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        //换牌数量是否一致
        if (cardList == null || cardList.size() != getPos().getRoom().wanFa_ChangeCard_Num()) {
            request.error(ErrorCode.NotAllow, " MJTemplate  opHuanSanZhang  error cardList is null or size!=3");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        //是否换完牌了了
        if (!getPos().getChangeCardList().isEmpty()) {
            request.error(ErrorCode.NotAllow, " MJTemplate opHuanSanZhang not allow op again");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        //检测换牌是否是自己的牌
        if (!cardList.stream().allMatch(k -> getPos().allCards().stream().anyMatch(mjCard -> mjCard.cardID == k))) {
            request.error(ErrorCode.NotAllow, " MJTemplate opHuanSanZhang cardList not contains error");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        getPos().removeAllPrivateCards(cardList);
        if (!Objects.isNull(getPos().getHandCard()) && cardList.contains(getPos().getHandCard().getCardID())) {
            getPos().cleanHandCard();
        }
        getPos().setChangeCardList(new ArrayList<>(cardList));
        return this.exeCardAction(opType);

    }

    /**
     * 检查指定的牌是否可以打出
     *
     * @param card 牌
     * @return
     */
    @Override
    public boolean outCard(MJCard card) {
        if (pos.outCard(card)) {
            ((MJTemplateRoomSet) getPos().getSet()).checkGenZhuang(card.type, opPos);
            return true;
        }
        return false;
    }

    public BaseMJRoom_RoundPos roomRoundPosInfo(boolean isSelf, boolean isBuChiFuDaFu) {
        MJTemplateRoom_RoundPos roundPos = (MJTemplateRoom_RoundPos) newMJRoom_RoundPos();
        MJTemplateSetPos setPos = this.getPos();
        roundPos.setOpList(this.getRecieveOpTypes());
        roundPos.setChiList(this.getPos().getPosOpNotice().getChiList());
        roundPos.setLastOpCard(roundPos.getOpList().contains(OpType.QiangGangHu) ? this.getLastOpCard() : this.getLastOutCard());
        roundPos.setWaitOpPos(this.getOpPos());
        roundPos.setTingCardMap(isSelf ? this.getPos().getPosOpNotice().getTingCardMap() : null);
        if (isBuChiFuDaFu) {
            roundPos.setBuChuList(isSelf ? this.getPos().getPosOpNotice().getBuNengChuList() : null);
        }
        roundPos.setBuGangList(isSelf ? setPos.getBuGangList() : null);
        roundPos.setJieGangList(isSelf ? setPos.getJieGangList() : null);
        roundPos.setAnGangList(isSelf ? setPos.getAnGangList() : null);
        roundPos.setYaoGangList(isSelf ? setPos.getYaoGangList() : null);
        if (Objects.nonNull(setPos.getTingInfoList()) && isSelf) {
            roundPos.setTingInfoList(setPos.getTingInfoList());
        }
        for (int card : setPos.getFirstChangeCardList()) {
            roundPos.getFirstChangeCardList().add(isSelf ? card : 0);
        }
        return roundPos;
    }

    public BaseMJRoom_RoundPos newMJRoom_RoundPos() {
        return new MJTemplateRoom_RoundPos();
    }

    public MJTemplateRoomSet roomSet() {
        return (MJTemplateRoomSet) this.set;
    }

    /**
     * 重置位置操作
     */
    public void reSetRoomPosOp() {
        if (getOpType() != null) {
            return;
        }
        if (getRecieveOpTypes().isEmpty()) {
            return;
        }
        // 设置动作列表
        getPos().getPosOpRecord().setOpList(getRecieveOpTypes());
        if (getPos().getRoom().isConnectClearTrusteeship()) {
            // 重新记录打牌时间
            getPos().getRoomPos().setLatelyOutCardTime(CommTime.nowMS());
        }
        // 设置最后操作时间
        this.set.getLastOpInfo().setLastShotTime(CommTime.nowSecond());
    }

    public MJTemplateSetPos getPos() {
        return (MJTemplateSetPos) super.getPos();
    }

}
