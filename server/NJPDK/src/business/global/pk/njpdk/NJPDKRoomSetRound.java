package business.global.pk.njpdk;

import business.global.pk.njpdk.cardtype.NJPDKALGContainer;
import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.global.pk.njpdk.cardtype.NJPDKCardTypeFactory;
import business.global.pk.njpdk.cardtype.type.*;
import business.njpdk.c2s.cclass.NJPDK_define;
import business.njpdk.c2s.cclass.NJPDK_define.NJPDK_CARD_TYPE;
import business.njpdk.c2s.cclass.NJPDK_define.NJPDK_WANFA;
import business.njpdk.c2s.iclass.CNJPDK_OpCard;
import business.njpdk.c2s.iclass.SNJPDK_AddBombScore;
import business.njpdk.c2s.iclass.SNJPDK_OpCard;
import business.player.Robot.RobotMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.ddm.server.websocket.handler.requset.WebSocketRequestDelegate;
import jsproto.c2s.cclass.pk.BasePocker;
import jsproto.c2s.cclass.pk.BasePockerLogic;
import jsproto.c2s.cclass.pk.Victory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 安岳跑得快一回合游戏逻辑
 *
 * @author zaf
 */
public class NJPDKRoomSetRound {

    public NJPDKRoom room;
    public NJPDKRoomSet set;
    public int currentOpPos; //本回合等待操作玩家
    public int lastOpPos = -1;//本回合上轮最后打牌玩家
    public int lastOpCardType = NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value();  //本回合上轮最后打牌的类型
    public ArrayList<Integer> lastCardList = new ArrayList<>(); //本回合上轮最后打出的牌
    public boolean turnEnd = false; //回合结束
    public boolean m_bSetEnd = false; //是否局结束
    public int firstOpPos; //首出玩家
    public int maxBombPos = -1; //本轮最大炸弹玩家

    private static final int INTERVAL = 30000;//托管时间间隔
    private static final int ACE = 0x0E;//A的值
    private final int blackHeart3 = 0x33; //黑桃三

    public NJPDKRoomSetRound(NJPDKRoomSet set) {
        this.set = set;
        this.room = set.room;
        this.currentOpPos = set.getOpPos();
        this.firstOpPos = set.getOpPos();
    }

    public void clean() {
        this.room = null;
        this.set = null;
        this.lastCardList = null;
    }

    /**
     * 更新回合状态
     *
     * @return
     */
    public boolean update() {
        //是否一回合结束或者一局结束
        if (turnEnd || m_bSetEnd) return true;
        return false;
    }

    public List<Integer> autoCard(ArrayList<Integer> cardsList, int cardSize, int pos, NJPDKALGParameter parameter, int type) {
        List<Integer> outCardList;
        ArrayList<Integer> clonePrivateCards;
        parameter.setInputParameter(lastCardList, lastOpCardType, cardsList, cardSize);
        //飞架不带
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI34.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3BUDAIZUIHOU)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_Plane.class).generateCardList(parameter);
                if (outCardList.size() == cardsList.size() && outCardList.size() >= 6) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI34.value(), 0, outCardList);
                    return outCardList;
                }
            }
        }
        //飞机一对
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI32.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3DAI2) || this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_3DAI21)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_PlaneWithPairs.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI32.value(), parameter.outTailNumber, outCardList);
                    return outCardList;
                }
            }
        }
        //飞机两张
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI33.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_3DAI21)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_PlaneWithTwo.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI33.value(), parameter.outTailNumber, outCardList);
                    return outCardList;
                }
            }
        }
        //飞机带1张
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI31.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3DAI1)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_PlaneWithA.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI31.value(), parameter.outTailNumber, outCardList);
                    return outCardList;
                }
            }
        }
        //联队
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_LIANDUI.value()) {
            parameter.minStraightNumber = this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_LIANDUI2) ? 2 : 3;
            outCardList = NJPDKCardTypeFactory.getCardType(Type_MultiPairs.class).generateCardList(parameter);
            if (outCardList.size() > 2) {
                parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_LIANDUI.value(), 0, outCardList);
                return outCardList;
            }
        }
        //顺子
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_SHUNZI.value()) {
            outCardList = NJPDKCardTypeFactory.getCardType(Type_Straight.class).generateCardList(parameter);

            if (outCardList.size() > 0) {
                parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_SHUNZI.value(), 0, outCardList);
                return outCardList;
            }
        }
        //3不带最后
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3BUDAI.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3BUDAIZUIHOU) && cardSize == 3) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_ThreeZone.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3BUDAI.value(), 0, outCardList);
                    return outCardList;
                }
            }
        }
        //3不带最后
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3BUDAI.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3BUDAI)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_ThreeZone.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3BUDAI.value(), 0, outCardList);
                    return outCardList;
                }
            }
        }
        //3不带
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3BUDAI.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3BUDAI)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_ThreeWithOut.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3BUDAI.value(), 0, outCardList);
                    return outCardList;
                }
            }
        }
        //3带一对
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI21.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3DAI2) || this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_3DAI21)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_ThreeZoneWithPairs.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI21.value(), 2, outCardList);
                    return outCardList;
                }
            }
        }
        //3带2
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI2.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_3DAI21)) {
                if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3DAI2)) {
                    outCardList = NJPDKCardTypeFactory.getCardType(Type_ThreeZoneWithTwoNotPairs.class).generateCardList(parameter);
                } else {
                    outCardList = NJPDKCardTypeFactory.getCardType(Type_ThreeZoneWithTwo.class).generateCardList(parameter);
                }
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI2.value(), 2, outCardList);
                    return outCardList;
                }
            }
        }
        //3带1
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI1.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3DAI1)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_ThreeZoneWithA.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI1.value(), 1, outCardList);
                    return outCardList;
                }
            }
        }
        //3A
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_3A)) {
//                if (this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_4DAI1)) {
//                    outCardList = NJPDKCardTypeFactory.getCardType(Type_Bomb3AWith1.class).generateCardList(parameter);
//                    if (outCardList.size() > 0) {
//                        parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value(), 1, outCardList);
//                        return outCardList;
//                    }
//                } else {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_Bomb3A.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value(), 0, outCardList);
                    return outCardList;
                }
//                }
            }
        }
        //炸弹
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value()) {
            outCardList = NJPDKCardTypeFactory.getCardType(Type_Bomb.class).generateCardList(parameter);
            if (outCardList.size() > 0) {
                parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value(), 0, outCardList);
                return outCardList;
            }
        }
        //4带1
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI1.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_4DAI1)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_BombWith1.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI1.value(), 1, outCardList);
                    return outCardList;
                }
            }
        }
        //4带一对
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI21.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_4DAI21)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_FourZoneWithPairs.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI21.value(), 2, outCardList);
                    return outCardList;
                }
            }
        }
        //4带2
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI2.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_4DAI21)) {
//                if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_4DAI2)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_FourZoneWithTwoNotPairs.class).generateCardList(parameter);
//                }else {
//                    outCardList = NJPDKCardTypeFactory.getCardType(Type_FourZoneWithTwo.class).generateCardList(parameter);
//                }
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI2.value(), 2, outCardList);
                    return outCardList;
                }
            }
        }
        //4带3
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI3.value()) {
            if (this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_4DAI3)) {
                outCardList = NJPDKCardTypeFactory.getCardType(Type_FourZoneWithThree.class).generateCardList(parameter);
                if (outCardList.size() > 0) {
                    parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI3.value(), 3, outCardList);
                    return outCardList;
                }
            }
        }
        //对子
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_DUIZI.value()) {
            outCardList = NJPDKCardTypeFactory.getCardType(Type_APairs.class).generateCardList(parameter);
            if (outCardList.size() > 0) {
                parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_DUIZI.value(), 0, outCardList);
                return outCardList;
            }
        }
        //单张
        if (type == -1 || type == NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_SINGLECARD.value()) {
            clonePrivateCards = (ArrayList<Integer>) cardsList.clone();
            boolean isNextSingle = this.room.getRoomPosMgr().posList.stream().anyMatch(n -> ((NJPDKRoomPos) n).cards().size() == 1 && n.getPosID() != pos);
            outCardList = NJPDKCardTypeFactory.getCardType(Type_Single.class).generateCardList(parameter);
            if (outCardList.size() > 0) {
                if (isNextSingle) {
                    outCardList.clear();
                    outCardList.add(clonePrivateCards.get(0));
                }
            }
            if (outCardList.size() > 0) {
                parameter.setOutPutParameter(NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_SINGLECARD.value(), 0, outCardList);
                return outCardList;
            }
        }
        return new ArrayList<>();
    }

    /**
     * 根据权重选择牌型
     *
     * @param clonePrivateCards
     * @param size
     * @param pos
     * @return
     */
    public NJPDKALGParameter sortOutCardTypeList(ArrayList<Integer> clonePrivateCards, int size, int pos) throws Exception {
        List<NJPDKALGParameter> parameterList = new ArrayList<>();
        for (int type = NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_SINGLECARD.value(); type <= NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI34.value(); type++) {
            NJPDKALGParameter cardType = new NJPDKALGParameter();
            if (autoCard(clonePrivateCards, size, pos, cardType, type).size() > 0) {
                parameterList.add(cardType);
            }
        }
        if (parameterList.size() > 0) {
            int totalWeight = parameterList.stream().mapToInt(n -> TypeWeight.getWeight(n)).sum();
            List<NJPDKALGParameter> sortList = parameterList.stream().sorted((o1, o2) -> {
                if (o1.weight > o2.weight) {
                    return -1;
                }
                return 1;
            }).collect(Collectors.toList());
            Map<NJPDKALGParameter, Integer> chance = new HashMap<>();
            sortList.stream().forEach(n -> chance.put(n, n.weight));
            if (sortList.get(0).weight > TypeWeight.maxWeight) {
                return sortList.get(0);
            } else {
                NJPDKALGParameter randomCard = TypeWeight.getRandomCard(totalWeight, chance);
                if (randomCard.outCardType == 0) {
                    return sortList.get(0);
                } else {
                    return randomCard;
                }
            }
        }
        return new NJPDKALGParameter();
    }

    /**
     * 机器人托管
     *
     * @param pos
     */
    public void roomTrusteeship(int pos) {
        //不是金币房或者玩家已经赢了，或者不是你的回合，抛弃掉
        if (this.checkEndSet() || this.currentOpPos != pos) {
            return;
        }
        if (CommTime.nowMS() - this.set.startMS <= 100) {
            return;
        }
        NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(pos);
        //手牌排序
        ArrayList<Integer> clonePrivateCards = (ArrayList<Integer>) roomPos.cards().clone();
        clonePrivateCards.sort(BasePockerLogic.sorterBigToSmallNotTrump);
        NJPDKALGParameter cardType = new NJPDKALGParameter();
        List<Integer> outCardList = null;
        boolean isBlackHeart3 = false;
        //黑桃三先出
        if (set.isFirstOp() && set.m_FirstOpCard != 0 && roomPos.getPrivateCards().contains(blackHeart3)) {
            isBlackHeart3 = true;
            outCardList = firstThree(clonePrivateCards, cardType);
        }
        //不是黑桃三先出，或者手上没有可出的黑桃三的牌，随机出牌
        if (!isBlackHeart3 || cardType.outCardType == 0) {
            try {
                cardType = sortOutCardTypeList(clonePrivateCards, roomPos.cards().size(), pos);
                outCardList = cardType.outCardList;
            } catch (Exception e) {
                CommLogD.error(e.getMessage());
                cardType = new NJPDKALGParameter();
                outCardList = autoCard(clonePrivateCards, roomPos.cards().size(), pos, cardType, -1);
            }
        }
        //出不了牌，机器人
        if (null == outCardList || outCardList.size() <= 0) {
            cardType.outCardType = NJPDK_CARD_TYPE.PDK_CARD_TYPE_BUCHU.value();
            cardType.outTailNumber = 0;
            outCardList = new ArrayList<>();
        }
        WebSocketRequest request = new WebSocketRequestDelegate();
        boolean flag = this.onOpCard(request, CNJPDK_OpCard.make(this.room.getRoomID(), pos, cardType.outCardType, (ArrayList<Integer>) outCardList, cardType.outTailNumber, true));
        if (!flag) {
            if (CommTime.nowMS() - this.set.startMS > INTERVAL) {//出不了牌就不出
                cardType.outCardType = NJPDK_CARD_TYPE.PDK_CARD_TYPE_BUCHU.value();
                cardType.outTailNumber = 0;
                cardType.outCardList = new ArrayList<>();
                flag = this.onOpCard(request, CNJPDK_OpCard.make(this.room.getRoomID(), pos, cardType.outCardType, (ArrayList<Integer>) cardType.outCardList, cardType.outTailNumber, true));
                if (!flag) { //还是不行就流局
                    if (roomPos.getPid() > RobotMgr.getInstance().limitID && RobotMgr.getInstance().isRobot((int) roomPos.getPid())) {
                        this.set.endSet();
                    }
                }
                return;
            }
        }
    }

    /**
     * 黑桃三先出
     *
     * @param clonePrivateCards
     * @param cardType
     * @return
     */
    public ArrayList<Integer> firstThree(ArrayList<Integer> clonePrivateCards, NJPDKALGParameter cardType) {
        //带黑桃三
        Map<Integer, List<Integer>> cardGroupMap = clonePrivateCards.stream().collect(Collectors.groupingBy(p -> BasePocker.getCardValue(p)));//分组
        ArrayList<Integer> outCardList = new ArrayList<>();
        if (cardGroupMap.get(3) != null) {
            cardType.outCardList = outCardList;
            cardType.outTailNumber = 0;
            if (cardGroupMap.get(3).size() == 4) {
                outCardList.addAll(cardGroupMap.get(3));
                clonePrivateCards.removeAll(cardGroupMap.get(3));
//                if (this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_4DAI1)) {
//                    outCardList.add(clonePrivateCards.get(0));
//                    cardType.outTailNumber = 1;
//                }
                cardType.outCardType = NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value();
                return outCardList;
            }
            if (cardGroupMap.get(3).size() == 3) {
                outCardList.addAll(cardGroupMap.get(3));
                clonePrivateCards.removeAll(cardGroupMap.get(3));
                //3带一对
                if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3DAI2) || this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_3DAI21)) {
                    List<Integer> daiList = NJPDKALGContainer.getInstance().getTailList(outCardList, 1, 2, null, 0, 0);
                    outCardList.addAll(daiList);
                    if (outCardList.size() == 5) {
                        cardType.outCardType = NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI21.value();
                        cardType.outTailNumber = 2;
                        cardType.outCardList = outCardList;
                    }
                    return outCardList;
                }
                if (this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_3DAI21)) {
                    List<Integer> daiList = NJPDKALGContainer.getInstance().getTailList(outCardList, 2, 1, null, 0, 0);
                    outCardList.addAll(daiList);
                    if (outCardList.size() == 5) {
                        cardType.outCardType = NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI2.value();
                        cardType.outTailNumber = 2;
                        cardType.outCardList = outCardList;
                    }
                    return outCardList;

                }
                if (this.room.isWanFaByType(NJPDK_WANFA.PDK_WANFA_3DAI1)) {
                    outCardList.add(clonePrivateCards.get(0));
                    if (outCardList.size() == 4) {
                        cardType.outCardType = NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI1.value();
                        cardType.outTailNumber = 1;
                        cardType.outCardList = outCardList;
                        return outCardList;
                    }
                }
            }
            if (cardGroupMap.get(3).size() >= 2) {
                List<Integer> otherCard = cardGroupMap.get(3).stream().filter(m -> m != 0x33).limit(1).collect(Collectors.toList());
                outCardList.add(0x33);
                outCardList.add(otherCard.get(0));
                cardType.outCardType = NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_DUIZI.value();
                return outCardList;
            }
            if (cardGroupMap.get(3).size() >= 1) {
                List<Integer> otherCard = cardGroupMap.get(3).stream().filter(m -> m == 0x33).collect(Collectors.toList());
                if (otherCard.size() > 0) {
                    outCardList.addAll(otherCard);
                    cardType.outCardType = NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_SINGLECARD.value();
                }
                return outCardList;
            }
        }
        return outCardList;

    }

    /**
     * 打牌操作
     *
     * @param request 请求
     * @param opCard  操作
     * @return
     */
    public synchronized boolean onOpCard(WebSocketRequest request, CNJPDK_OpCard opCard) {
        //回合已结束,不允许再操作，等update更新下一回合
        if (turnEnd) {
            if (null != request) request.error(ErrorCode.NotAllow, "onOpCard error: turnEnd is ture");
            return false;
        }
        //请求操作的玩家是否是当前回合的操作者
        if (opCard.pos != currentOpPos) {
            if (null != request)
                request.error(ErrorCode.NotAllow, "onOpCard error: not current pos op oppos: " + currentOpPos);
            return false;
        }
        NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(opCard.pos);
        boolean hasGUan = false;
        int currentGuan = opCard.pos;
        for (int i = 0; i < room.getPlayerNum(); i++) {
            NJPDKRoomPos tempRoomPos = (NJPDKRoomPos) room.getRoomPosMgr().getPosByPosID(currentGuan);
            if (tempRoomPos.canGuan) {
                hasGUan = true;
            }
            currentGuan = (currentGuan + 1) % room.getPlayerNum();
        }
        if (hasGUan) {
            if (opCard.guanpai && roomPos.canGuan) {
                room.guanpai(roomPos);
                return true;
            } else if (!opCard.guanpai) {
                roomPos.canGuan = false;
            }
            int guanPos = room.getNextGuan(opCard.pos);
            if (-1 != guanPos) {
                set.setOpPos(guanPos);
            } else {
                set.setDefaultOutCardPos();
            }
            currentOpPos = set.m_OpPos;
            request.response();
            //通知打牌
            long runWaitSec = (CommTime.nowMS() - this.set.startMS) / 1000;
            NJPDKRoomPosMgr roomPosMgr = (NJPDKRoomPosMgr) this.room.getRoomPosMgr();
            for (int i = 0; i < this.room.getPlayerNum(); i++) {
                roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
                ArrayList<Integer> privateList = resolveCardList(roomPos.cards(), i, roomPos.getPosID());
                this.set.getRoomPlayBack().playBack2Pos(i, SNJPDK_OpCard.make(opCard.roomID, opCard.pos, -1, currentOpPos, new ArrayList<>(), true, 0, m_bSetEnd, privateList, -1, false, runWaitSec), roomPosMgr.getAllPlayBackNotify());
            }
            return true;
        }

        //校验牌是否是自己的
        if (NJPDK_CARD_TYPE.PDK_CARD_TYPE_BUCHU.value() != opCard.opCardType && !this.checkIsMyCard(opCard)) {//牌不是自己的
            if (null != request) request.error(ErrorCode.NotAllow, "onOpCard error:card is not myself");
            return false;
        }

        //操作结束
        if (NJPDK_CARD_TYPE.PDK_CARD_TYPE_BUCHU.value() == opCard.opCardType) {
            if (lastOpCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value()) { //回合首回合必出
                if (null != request) request.error(ErrorCode.NotAllow, "onOpCard error:PDK_WANFA_FEIBICHU");
                return false;
            } else {
                ArrayList<Integer> clonePrivateCards = (ArrayList<Integer>) roomPos.cards().clone();
                clonePrivateCards.sort(BasePockerLogic.sorterBigToSmallNotTrump);
                if (0 == this.checkHaveMaxCard(clonePrivateCards, clonePrivateCards, opCard.pos, -1)) { //有牌必出
                    //System.out.println("[" + opCard.pos + "]操作" + "[" + opCard.opCardType + "]" + sysCardList(opCard.cardList) + "" + "：上回合[" + lastOpPos + "]操作[" + lastOpCardType + "]" + sysCardList(lastCardList) + "");
                    if (null != request) request.error(ErrorCode.NotAllow, "onOpCard error:your have max card");
                    return false;
                }
            }
        } else if (NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value() == opCard.opCardType) {
            //校验是否是炸弹，且比上一轮大
            if (this.isBombBigThanLast(opCard.opCardType, opCard.cardList, roomPos.getPrivateCards().size())) {
                //炸弹个数新增
                if (NJPDK_define.BombScore.DOUBLE_.has(this.room.getRoomCfg().zhadan)) {
                    addBomb(opCard.pos);
                }
                //炸弹个数新增
                if (NJPDK_define.BombScore.ADD_TEN.has(this.room.getRoomCfg().zhadan) || NJPDK_define.BombScore.ADD_FIVE.has(this.room.getRoomCfg().zhadan)) {
                    maxBombPos = opCard.pos;
                }
            } else {
                if (null != request) {
                    //System.out.println("["+opCard.pos+"]操作"+"["+opCard.opCardType+"]"+ sysCardList(opCard.cardList)+""+"：上回合["+lastOpPos+"]操作["+lastOpCardType+"]"+ sysCardList(lastCardList)+"");
                    request.error(ErrorCode.NotAllow, "onOpCard error:card isBombBigThanLast fail");
                }
                return false;
            }
        } else {//不是炸弹类型
            //检验是否是同牌型的互比，带比带，对比对，单张比单张
            if (lastOpCardType != NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value() && this.lastOpCardType != opCard.opCardType) {
                if (null != request)
                    request.error(ErrorCode.NotAllow, "onOpCard error:optype do not op,this last opType:" + this.lastOpCardType + ",your optype:" + opCard.opCardType);
                return false;
            }
            ArrayList<Integer> clonePrivateCards = opCard.cardList;
            clonePrivateCards.sort(BasePockerLogic.sorterBigToSmallNotTrump);
            //校验牌是否满足牌型而且是否比上一回大，或者是首回合
            int result = this.checkHaveMaxCard(clonePrivateCards, roomPos.cards(), opCard.pos, opCard.opCardType);
            if (0 != result) {
                if (-1 == result) {
                    if (null != request) request.error(ErrorCode.AYPDK_HEITAO_THREE, "heitaosan bichu");
                    return false;
                }
                //System.out.println("checkCardList"+"["+opCard.pos+"]操作"+"["+opCard.opCardType+"]"+ sysCardList(opCard.cardList)+"");
                if (null != request) request.error(ErrorCode.NotAllow, "onOpCard error:card check fail");
                return false;
            }
            //打单张，如果有其他家只剩一张，则需要从大开打，否则不允许
            if (NJPDK_CARD_TYPE.PDK_WANFA_SINGLECARD.value() == opCard.opCardType && !this.checkNextIsOneCard(opCard.pos, opCard.cardList.get(0))) {
                if (null != request) request.error(ErrorCode.NotAllow, "onOpCard error:you must op max card");
                return false;
            }
        }

        //删牌
        if (opCard.cardList.size() > 0 && !roomPos.deleteCard(opCard.cardList)) {
            if (null != request)
                request.error(ErrorCode.NotAllow, "card delete error : your cards:" + opCard.cardList.toString() + ", posCard:" + roomPos.getPrivateCards().toString());
            return false;
        }

        //打对子，如果赢了，上一轮的玩家不符合标准必须包赔
        if (NJPDK_CARD_TYPE.PDK_CARD_TYPE_DUIZI.value() == opCard.opCardType && roomPos.cards().size() <= 0) {
            this.checkBaoPei(opCard.pos, opCard.opCardType);
        }
        //打单张，如果赢了，上一轮的玩家不符合标准必须包赔
        if (NJPDK_CARD_TYPE.PDK_WANFA_SINGLECARD.value() == opCard.opCardType && roomPos.cards().size() <= 0) {
            this.checkBaoPei(opCard.pos, opCard.opCardType);
        }

        //是否是首出
        this.set.setFirstOp(false);

        if (opCard.opCardType == NJPDK_CARD_TYPE.PDK_CARD_TYPE_BUCHU.value()) {
            //不出，可能会出现过了一圈，每人要的起，新增下一轮操作位
            roomPos.setLatelyOutCardTime(0L);
            this.addOpPos(true);
            //System.out.println("["+opCard.pos+"]操作完毕"+"["+opCard.opCardType+"]"+ sysCardList(opCard.cardList)+"");
        } else {
            //记牌器通知牌剩余
            this.set.setCard.subCardCount(opCard.cardList);
            //新增操作链表
            this.set.addOpCardList(opCard.cardList, opCard.opCardType, opCard.pos);
            //记录上一轮信息
            this.lastOpCardType = opCard.opCardType;
            //System.out.println("["+opCard.pos+"]操作完毕"+"["+opCard.opCardType+"]"+ sysCardList(opCard.cardList)+"");
            this.lastOpPos = opCard.pos;
            //记录上一轮操作的牌
            this.lastCardList = opCard.cardList;
            roomPos.setLatelyOutCardTime(0L);
            //普通设置下一轮操作者
            this.addOpPos(false);
            //是不是没牌了，结束
            if (this.checkEndSet()) {
                addBombScore(true);
            }
        }
        int baoDanType = -1;
        if (!roomPos.isBaoDan) {
            //两张报对
            if (room.getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.TwoCardReport.getType())) {
                if (roomPos.cards().size() == 2) {
                    roomPos.isBaoDan = true;
                    baoDanType = 2;
                }
            }
            //单牌报单
            if (roomPos.cards().size() == 1) {
                roomPos.isBaoDan = true;
                baoDanType = 1;
            }
        }
        long runWaitSec = (CommTime.nowMS() - this.set.startMS) / 1000;

        //通知打牌
        NJPDKRoomPosMgr roomPosMgr = (NJPDKRoomPosMgr) this.room.getRoomPosMgr();
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            ArrayList<Integer> privateList = resolveCardList(roomPos.cards(), i, roomPos.getPosID());
            if (0 == i) {
                this.set.getRoomPlayBack().playBack2Pos(i, SNJPDK_OpCard.make(opCard.roomID, opCard.pos, opCard.opCardType, currentOpPos, opCard.cardList, turnEnd, opCard.daiNum, m_bSetEnd, privateList, baoDanType, opCard.isFlash, runWaitSec), roomPosMgr.getAllPlayBackNotify());
            } else {
                this.room.getRoomPosMgr().notify2Pos(i, SNJPDK_OpCard.make(opCard.roomID, opCard.pos, opCard.opCardType, currentOpPos, opCard.cardList, turnEnd, opCard.daiNum, m_bSetEnd, privateList, baoDanType, opCard.isFlash, runWaitSec));
            }
        }
        return true;
    }

    /**
     * 处理私有牌显示
     *
     * @param cardsList  牌
     * @param currentPos 自己的位置
     * @param showOps    显示的位置
     * @return
     */
    public ArrayList<Integer> resolveCardList(ArrayList<Integer> cardsList, int currentPos, int showOps) {
        return com.aoo.bcg.common.reconnect.CardPerspective.hand(cardsList, showOps == currentPos);
    }

    /**
     * 检测牌局是否结束
     *
     * @return
     */
    public boolean checkEndSet() {
        boolean isEnd = this.room.getRoomPosMgr().posList.stream()
                .filter(roomPos -> ((NJPDKRoomPos) roomPos).cards() == null || ((NJPDKRoomPos) roomPos).cards().size() <= 0)
                .count() > 0;
        this.m_bSetEnd = isEnd || this.m_bSetEnd;
        return m_bSetEnd;
    }

    /**
     * 检测是否包赔
     *
     * @param pos
     * @return
     */
    public boolean checkBaoPei(int pos, int type) {
        NJPDKRoomPos currentRoomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(pos);
        if ((currentRoomPos.cards() == null || currentRoomPos.cards().size() <= 0) && lastCardList.size() > 0) {
            NJPDKRoomPos lastRoomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(lastOpPos);
            //合并牌组
            List<Integer> newList = Stream.concat(this.lastCardList.stream(), lastRoomPos.cards().stream()).collect(Collectors.toList());
            ArrayList<Integer> list = new ArrayList<>();
            list.addAll(newList);
            list.sort(BasePockerLogic.sorterBigToSmallNotTrump);
            Map<Integer, List<Integer>> cardGroupMap = list.stream().collect(Collectors.groupingBy(p -> BasePocker.getCardValue(p)));
            if (type == NJPDK_CARD_TYPE.PDK_CARD_TYPE_DUIZI.value()) {
                int maxNum = cardGroupMap.entrySet().stream().filter(m -> m.getValue().size() >= 2).map(n -> n.getKey()).max(Integer::compare).orElse(0);
                //如果你上次打了两张，剩下的手牌还有其他牌型，这时候包赔
                //自己首出包赔
                if (firstOpPos == lastOpPos) {
                    if (lastOpPos + 1 == pos || (lastOpPos - room.getPlayerNum() + 1) == pos) {
                        lastRoomPos.isBaoPei = true;
                        return true;
                    }
                } else {//其他玩家首出对，你需要从最大开始出，不然包赔
                    if (BasePocker.getCardValue(lastCardList.get(0)) < maxNum) {
                        lastRoomPos.isBaoPei = true;
                        return true;
                    }
                }
            } else {
                long singleCount = cardGroupMap.entrySet().stream().filter(m -> m.getValue().size() == 1).map(n -> n.getKey()).count();
                //自己首出，有其他牌型包赔
                if (firstOpPos == lastOpPos) {
                    //校验顺子
                    boolean haveShunZi = ((Type_Straight) NJPDKCardTypeFactory.getCardType(Type_Straight.class)).checkHaveStraight(list);
                    if ((singleCount != list.size() || haveShunZi) && (lastOpPos + 1 == pos || (lastOpPos - room.getPlayerNum() + 1) == pos)) {
                        lastRoomPos.isBaoPei = true;
                        return true;
                    }
                } else if (BasePocker.getCardValue(lastCardList.get(0)) < BasePocker.getCardValue(list.get(0))) {//其他玩家出单子，自己不出最大的包赔
                    lastRoomPos.isBaoPei = true;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 验证是否是下载最后一张单牌
     **/
    @SuppressWarnings("unchecked")
    public boolean checkNextIsOneCard(int pos, int card) {
        int nextPos = pos;
        NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(nextPos);
        //你只有最后一张，没有限制出牌
        if (roomPos.getPrivateCards().size() == 1) {
            return true;
        }
        //土办法，全部遍历所有人，如果有人只有一张，自己要选择最大的牌出
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            nextPos = (++nextPos) % this.room.getPlayerNum();
            NJPDKRoomPos tempRoomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(nextPos);
            int cardNum = tempRoomPos.getPrivateCards().size();
            if (cardNum > 0) {
                if (cardNum == 1) {
                    ArrayList<Integer> cardList = (ArrayList<Integer>) roomPos.getPrivateCards().clone();
                    cardList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
                    if (BasePocker.getCardValue(card) != BasePocker.getCardValue(cardList.get(0))) {
                        return false;
                    }
                }
                break;
            }
        }

        return true;
    }

    /**
     * 验证是否是自己的牌
     */
    public boolean checkIsMyCard(CNJPDK_OpCard opCard) {
        NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(opCard.pos);
        for (Integer byte1 : opCard.cardList) {
            if (!roomPos.cards().contains(byte1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据list获取联队list
     */
    public ArrayList<ArrayList<Integer>> getLianDuiList(ArrayList<ArrayList<Integer>> list, int size) {
        ArrayList<ArrayList<Integer>> tempList = new ArrayList<>();
        if (list == null || (null != list && list.size() < 2)) {
            return tempList;
        }
        int count = list.size();
        ArrayList<Integer> opList = new ArrayList<>();
        ;
        for (int i = 0; i < count; i++) {
            opList.add(list.get(i).get(0));
        }

        ArrayList<Integer> tempOpList = this.getShunZiByListEx(opList, size);
        if (tempOpList == null) {
            return tempList;
        }

        ArrayList<ArrayList<Integer>> lianDuiList = new ArrayList<>();
        for (Integer byte1 : tempOpList) {
            for (int i = 0; i < count; i++) {
                if (BasePocker.getCardValue(byte1) == BasePocker.getCardValue(list.get(i).get(0))) {
                    lianDuiList.add(list.get(i));
                    break;
                }
            }
        }
        return lianDuiList;
    }

    public List<Integer> getSameCardByNum(ArrayList<Integer> list, int num) {
        Map<Integer, List<Integer>> cardGroupMap = list.stream().collect(Collectors.groupingBy(p -> BasePocker.getCardValue(p)));
        return cardGroupMap.entrySet().stream().filter(m -> m.getValue().size() == num).map(n -> n.getKey()).collect(Collectors.toList());
    }

    /**
     * 判断炸弹是否正确且可出，并且比上一轮大
     *
     * @param opCardType 出牌类型
     * @param list       出牌列表
     * @return
     */
    public boolean isBombBigThanLast(int opCardType, ArrayList<Integer> list, int privateSize) {
        if (NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value() != opCardType) {
            return false;
        }
        if (list.size() < 3) {
            return false;
        }
        Map<Integer, List<Integer>> cardGroupMap = list.stream().collect(Collectors.groupingBy(p -> BasePocker.getCardValue(p)));
        List<Integer> zhaValue = getSameCardByNum(list, 4);
        long aCeCount = cardGroupMap.entrySet().stream().filter(m -> m.getValue().size() == 3 && m.getKey() == ACE).count();
//        int daiNum = this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_4DAI1) ? 1 : 0;
        int daiNum = 0;
        boolean haveFourZha = zhaValue != null && zhaValue.size() > 0 && (list.size() == 4 + daiNum || privateSize == 4);
        boolean is3AZha = this.room.isWanFaByType(NJPDK_WANFA.PDK_CARD_TYPE_3A) && aCeCount > 0 && (list.size() == 3 + daiNum || privateSize == 3);
        if (!haveFourZha && !is3AZha) {
            return false;
        }
        //上一轮最后出牌是炸弹
        if (lastOpCardType == NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value()) {
            boolean lastIsAZha = lastCardList.stream().filter(m -> BasePocker.getCardValue(m) == ACE).count() >= 3;
            List<Integer> lastZhaValue = getSameCardByNum(lastCardList, 4);
            if (lastIsAZha) {//上一轮是A炸，要不起
                return false;
            } else if (!is3AZha && lastZhaValue.get(0) > zhaValue.get(0)) {//同类型炸弹,炸弹比对方小
                return false;
            }
        }
        return true;
    }

    /**
     * 当前轮结束，下一轮操作者设置
     *
     * @param isCalcEndTurn 是否允许过一圈
     */
    public void addOpPos(boolean isCalcEndTurn) {
        //设置下一回合等待操作的玩家
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            currentOpPos = (++currentOpPos) % this.room.getPlayerNum();
            NJPDKRoomPos tempRoomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(currentOpPos);
            if (tempRoomPos.getPrivateCards().size() > 0) {
                break;
            }
        }
        this.set.startMS = CommTime.nowMS();
        //设置当局的等待的操作人
        this.set.setOpPos(currentOpPos);
        if (!isCalcEndTurn) return;

        //当前的操作人和最后一轮的操作人同一个，代表其他玩家都要不起
        if (currentOpPos == lastOpPos) {
            addBombScore(false);
            turnEnd = true;
        }
    }

    public void addBombScore(boolean lastHand) {
        //炸弹个数新增
        if (NJPDK_define.BombScore.ADD_TEN.has(this.room.getRoomCfg().zhadan) || NJPDK_define.BombScore.ADD_FIVE.has(this.room.getRoomCfg().zhadan)) {
            if (maxBombPos >= 0) {
                addBomb(maxBombPos);
                int score = 10;
                if (NJPDK_define.BombScore.ADD_FIVE.has(this.room.getRoomCfg().zhadan)) {
                    score = 5;
                }
                score *= room.cfg.getBeishu();
                // 加10分
                for (int i = 0; i < this.room.getPlayerNum(); i++) {
                    NJPDKRoomPos iRoomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
                    if (maxBombPos == i) {
                        set.pointList.set(i, set.pointList.get(i) + ((this.room.getPlayerNum() - 1) * score));
                        iRoomPos.bombPoint = iRoomPos.bombPoint + (this.room.getPlayerNum() - 1) * score;
                    } else {
                        set.pointList.set(i, set.pointList.get(i) - score);
                        iRoomPos.bombPoint = iRoomPos.bombPoint - score;
                    }
                }
                room.getRoomPosMgr().notify2All(SNJPDK_AddBombScore.make(room.getRoomID(), maxBombPos, lastHand));
                maxBombPos = -1;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Integer> getShunZiByListEx(ArrayList<Integer> cardList, int size) {
        ArrayList<Integer> list = (ArrayList<Integer>) cardList.clone();
        list.sort(BasePockerLogic.sorterBigToSmallNotTrump);
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.add(list.get(0));
        for (int i = 0; i < cardList.size() - 1; i++) {
            if (Math.abs(BasePocker.getCardValue(list.get(i)) - BasePocker.getCardValue(list.get(i + 1))) != 1) {
                tempList.clear();
                tempList.add(list.get(i + 1));
                continue;
            } else {
                tempList.add(list.get(i + 1));
            }
            if (tempList.size() == size) {
                tempList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
                return tempList;
            }
        }
        return null;
    }

    /**
     * 获取满足张数的牌集合
     *
     * @param cardList 牌集合[2,3,4,2,3,4,3,4,1]
     * @param sameNum  张数 2
     * @return 存在的组合[[2, 2], [3, 3], [4, 4]]
     */
    public ArrayList<ArrayList<Integer>> getSameCardGroupListBySameNum(ArrayList<Integer> cardList, int sameNum) {
        ArrayList<Integer> list = (ArrayList<Integer>) cardList.clone();
        ArrayList<ArrayList<Integer>> outList = new ArrayList<>();
        //获取相同牌到到二维列表outList
        BasePockerLogic.getPockerEqualValue(outList, list);
        for (int i = 0; i < outList.size(); i++) {
            if (outList.get(i).size() >= sameNum) {
                while (outList.get(i).size() > sameNum) {
                    outList.get(i).remove(0);
                }
            } else {
                outList.remove(i);
            }
        }
        return outList;
    }

    /**
     * @return currentOpPos
     */
    public int getOpPos() {
        return currentOpPos;
    }

    /**
     * @return lastOpPos
     */
    public int getLastOpPos() {
        return lastOpPos;
    }

    /**
     * @return lastOpCardType
     */
    public int getOpCardType() {
        return lastOpCardType;
    }

    /**
     * @return lastCardList
     */
    public ArrayList<Integer> getCardList() {
        return lastCardList;
    }

    /**
     * @return m_bSetEnd
     */
    public boolean isSetEnd() {
        return m_bSetEnd;
    }

    /**
     * 校验是不是有更大的牌
     *
     * @param cardsList 牌列表
     * @param card      牌
     * @param pos       位置
     * @param type      == -1 就是智能选取所有牌型校验，!=-1就是校验该牌型是否可以出
     * @return
     */
    public int checkHaveMaxCard(ArrayList<Integer> cardsList, ArrayList<Integer> card, int pos, int type) {
        NJPDKALGParameter cardType = new NJPDKALGParameter();
        if (set.isFirstOp() && set.m_FirstOpCard != 0 && card.contains(set.m_FirstOpCard) && !cardsList.contains(set.m_FirstOpCard)) {
            return -1;
        } else {
            List<Integer> outCardList = autoCard(cardsList, card.size(), pos, cardType, type);
            if (outCardList.size() > 0 && (type == -1 || cardType.outCardType == type)) {
                return 0;
            }
        }
        return 1;
    }

    /**
     * 炸弹数量新增
     *
     * @param pos
     */
    public void addBomb(int pos) {
        //炸弹个数新增
        Victory victory = this.set.roomZhaDanList.stream().filter(m -> m.getPos() == pos).findFirst().orElse(new Victory());
        if (victory.getNum() == 0) {
            victory.setPos(pos);
            victory.setNum(1);
            this.set.roomZhaDanList.add(victory);
        } else {
            victory.setNum(victory.getNum() + 1);
        }
    }

    public String sysCardList(List<Integer> cardList) {
        StringBuilder stringBuilder = new StringBuilder("[");
        for (Integer card : cardList) {
            stringBuilder.append(getCardSystem(card) + ",");
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    /**
     * 牌输出辅助器
     *
     * @param card
     * @return
     */
    public static String getCardSystem(int card) {
        int rank = card % 16;
        int color = card / 16;
        StringBuilder stringBuilder = new StringBuilder();
        switch (color) {
            case 0:
                stringBuilder.append("♦");
                break;
            case 1:
                stringBuilder.append("♣");
                break;
            case 2:
                stringBuilder.append("♥");
                break;
            case 3:
                stringBuilder.append("♠");
                break;
            case 4:
                stringBuilder.append("joker");
                break;
            default:
                stringBuilder.append("♦");
                break;
        }
        if (color != 4) {
            if (rank < 11) {
                stringBuilder.append(rank);
            } else {
                switch (rank) {
                    case 11:
                        stringBuilder.append("J");
                        break;
                    case 12:
                        stringBuilder.append("Q");
                        break;
                    case 13:
                        stringBuilder.append("K");
                        break;
                    case 14:
                        stringBuilder.append("A");
                        break;
                    case 15:
                        stringBuilder.append("2");
                        break;
                    default:
                        break;
                }
            }
        }
        return stringBuilder.toString();
    }

}
