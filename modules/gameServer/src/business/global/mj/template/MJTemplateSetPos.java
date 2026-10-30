package business.global.mj.template;

import business.global.mj.*;
import business.global.mj.robot.MJSetPosRobot;
import business.global.mj.robot.MJTemplateSetPosRobot;
import business.global.mj.template.wanfa.MJTemplateHuPointTingImpl;
import business.global.mj.template.wanfa.MJTemplateLouHu;
import business.global.room.mj.MJRoomPos;
import cenum.RoomTypeEnum;
import cenum.mj.*;
import com.ddm.server.common.utils.CommMath;
import jsproto.c2s.cclass.mj.BaseMJRoom_PosEnd;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.cclass.mj.template.MJTemplateRoom_PosEnd;
import jsproto.c2s.cclass.mj.template.MJTemplateSet_Pos;
import jsproto.c2s.cclass.mj.template.MJTemplateTingInfo;
import jsproto.c2s.cclass.mj.template.MJTemplateWaitingExInfo;
import jsproto.c2s.cclass.room.AbsBaseResults;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板麻将 每一局每个位置信息
 *
 * @author Huaxing
 */
@Getter
@Setter
public abstract class MJTemplateSetPos extends AbsMJSetPos {
    /**
     * 客户端点击了过,需要保存到回放
     */
    protected boolean clientOpPassFlag;

    /**
     * 吃牌列表
     */
    protected List<Integer> chiCardList = new ArrayList<>();
    /**
     * 标示列表
     */
    protected List<Integer> biaoShiList = new ArrayList<>();
    /**
     * 报听
     */
    protected boolean isTing;
    /**
     * 摇杠
     */
    protected boolean isYaoGang;
    /**
     * 票分操作
     */
    protected int piaoFen;
    /**
     * 暗杠二维
     */
    protected List<List<Integer>> anGangList = new ArrayList<>();
    /**
     * 补杠二维
     */
    protected List<List<Integer>> buGangList = new ArrayList<>();
    /**
     * 接杠二维
     */
    protected List<List<Integer>> jieGangList = new ArrayList<>();
    /**
     * 摇杠二维
     */
    protected List<List<Integer>> yaoGangList = new ArrayList<>();
    /**
     * 操作列表 一般用于吃碰杠操作
     */
    protected List<Integer> opCardList = new ArrayList<>();
    /**
     * 客户端发给服务端的要换的牌
     */
    protected List<Integer> changeCardList = new ArrayList<>();
    /**
     * 游戏开始时 服务端预先选的要换的，客户端默认弹起来的牌
     */
    protected List<Integer> firstChangeCardList;
    /**
     * 漏壶
     */
    protected MJTemplateLouHu louHu;
    /**
     * 定缺
     */
    protected OpType dingQue;

    /**
     * 中码
     */
    protected List<Integer> zhongList = new ArrayList<>();
    protected boolean isGSKH;
    /**
     * 实时扣分
     */
    private int actualTimePoint;
    private int curActualTimePoint;
    /***
     * 实时扣的比赛分
     */
    private double actualTimeSportsPoint;
    /**
     * 听牌信息
     */
    private List<MJTemplateTingInfo> tingInfoList;

    /**
     * 胡牌信息
     */
    private Map<Integer, Integer> huInfo;
    //自动胡牌
    private Integer autoHu = null;
    //自动打牌
    private Integer autoOut = null;


    public MJTemplateSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set, Class<?> mActMrg) {
        super(posID, roomPos, set, mActMrg);
        this.initSetPosData();
    }

    public MJTemplateSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set) {
        super(posID, roomPos, set, MJTemplateHuPointTingImpl.class);
        initSetPosData();
        getRoom().initHuCardClz();
    }

    protected void initSetPosData() {
        setMSetOp(newMJSetOp());
        louHu = newMJLouHu();
        piaoFen = getRoom().wanFa_PiaoFenNum().value;
        changeCardList = new ArrayList<>(getRoom().wanFa_ChangeCard_Num());
        firstChangeCardList = new ArrayList<>(getRoom().wanFa_ChangeCard_Num());
        dingQue = getRoom().wanFa_DingQue().equals(MJTemplateRoomEnum.DingQue.NOT) ? null : OpType.Not;
        initHuPointTip();
        List<MJTemplateRoomEnum.AutoOpType> autoOpTypes = getRoom().wanFa_AutoTypeList();
        if (autoOpTypes.contains(MJTemplateRoomEnum.AutoOpType.Hu)) {
            setAutoHu(0);
        }
        if (autoOpTypes.contains(MJTemplateRoomEnum.AutoOpType.Out)) {
            setAutoOut(0);
        }
    }

    public void setWaitingExInfoFieldValue(MJTemplateRoomEnum.WaitingExType k, int value) {
        ((MJTemplateRoomPos) getRoomPos()).setWaitingExInfoFieldValue(k, value);
    }

    public Integer getWaitingExInfoFieldValue(MJTemplateRoomEnum.WaitingExType k) {
        return ((MJTemplateRoomPos) getRoomPos()).getWaitingExInfoFieldValue(k);
    }

    protected AbsMJSetOp newMJSetOp() {
        return new MJTemplateSetOp(this);
    }


    /**
     * 初始化 胡牌分提示  来源景德镇麻将玩法 胡牌的对应有胡牌分
     */
    protected void initHuPointTip() {
        //初tingInfoList  huInfo
        if (getRoom().isWanFaShowTingHuPoint()) {
            setHuInfo(new HashMap<>());
            setTingInfoList(new ArrayList<>());
        }
    }

    /**
     * 漏壶
     *
     * @return
     */
    protected MJTemplateLouHu newMJLouHu() {
        return null;
    }


    /**
     * 添加胡牌牌型
     *
     * @param opPointEnum
     */
    public void addOpPointEnum(OpPointEnum opPointEnum) {
        if (!opPointEnum.equals(OpPointEnum.Not) && !getPosOpRecord().getOpHuList().contains(opPointEnum)) {
            getPosOpRecord().addOpHuList(opPointEnum);
        }

    }


    /**
     * 实时计算杠分 1v1
     *
     * @param point 奖分
     */
    public void calcGangPoint1v1(int point, int losePos) {
        if (losePos == getPosID()) {
            return;
        }
        MJTemplateSetPos setPos = (MJTemplateSetPos) getMJSetPos(losePos);
        if (Objects.isNull(setPos)) {
            return;
        }
        if (setPos.getPosID() != getPosID()) {
            //输的分
            if (getRoom().calcFenUseYiKao() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
                if (getRoom().isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() <= 0) {
                    return;
                }
                double beiShu = Math.max(0D, this.getRoom().getRoomTyepImpl().getSportsDouble());
                double sPoint = CommMath.mul(point, beiShu);
                double sportPoint = sPoint;
                if (getRoom().isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() - sPoint <= 0) {
                    sportPoint = setPos.getRoomPos().getRoomSportsPoint();
                }
                setPos.setDeductEndPoint(-sportPoint);
                setPos.addActualSportsPoint(-sportPoint);
                setDeductEndPoint(sportPoint);
                addActualSportsPoint(sportPoint);
            }
            //输钱
            setPos.addActualPoint(-point);
            setPos.addCurActualPoint(-point);
            setPos.setPidSumPointEnd(-point);
            setPos.setDeductEndPoint(0);
            //赢钱
            addActualPoint(point);
            addCurActualPoint(point);
            setPidSumPointEnd(point);
            setDeductEndPoint(0);
        }

    }

    /**
     * 实时计算杠分 1v3
     *
     * @param point 奖分
     */
    public void calcGangPoint1v3(int point) {
        for (int i = 0; i < this.getSet().getRoom().getPlayerNum(); i++) {
            calcGangPoint1v1(point, i);
        }
    }

    /**
     * 实时分
     *
     * @param point
     */
    public void addActualPoint(int point) {
        this.actualTimePoint += point;
    }

    /**
     * 实时分
     *
     * @param point
     */
    public void addCurActualPoint(int point) {
        this.curActualTimePoint += point;
    }

    /**
     * 实时分
     *
     * @param sportsPoint
     */
    public void addActualSportsPoint(Double sportsPoint) {
        this.actualTimeSportsPoint += sportsPoint;
    }

    /**
     * 计算位置小局分数
     */
    @Override
    public void calcPosPoint() {
        this.getCalcPosEnd().calcPosPoint(this);
    }

    /**
     * 操作类型
     */
    @Override
    public boolean doOpType(int cardID, OpType opType) {
        return this.getmSetOp().doOpType(cardID, opType);
    }

    /**
     * 检测类型
     */
    @Override
    public boolean checkOpType(int cardID, OpType opType) {
        return this.getmSetOp().checkOpType(cardID, opType);
    }


    @Override
    public MJTemplateSet_Pos getNotify(boolean isSelf) {
        if (isRevealCard()) {
            isSelf = true;
        }
        MJTemplateSet_Pos setPos = (MJTemplateSet_Pos) this.getNotifyInfo(isSelf);
        return getCommonNotify(setPos, isSelf);
    }

    /**
     * 新一局中各位置的信息
     *
     * @return
     */
    @Override
    protected BaseMJSet_Pos newMJSetPos() {
        return new MJTemplateSet_Pos();
    }

    public MJTemplateSet_Pos getCommonNotify(MJTemplateSet_Pos setPos, boolean isSelf) {

        setPos.setPiaoFen(getRoom().isWanFa_PiaoFen() ? piaoFen : null);
        setPos.setTing(isTing);
        if (!getRoom().wanFa_ChangeCardType().equals(MJTemplateRoomEnum.ChangeCardType.NOT)) {
            setPos.setChangeCardList(new ArrayList<>());
            if (Objects.nonNull(changeCardList)) {
                for (int card : changeCardList) {
                    setPos.addChangeCardList(isSelf ? card : 0);
                }
            }
        }
        setPos.setDingQue(clientDingQue());
        setPos.setHuInfo(getRoom().isWanFaShowTingHuPoint() && isSelf ? huInfo : null);
        if (getRoom().isWanFa_WaitingEx()) {
            setPos.setPao(getWaitingExInfo().getPao());
            setPos.setPiao(getWaitingExInfo().getPiao());
            setPos.setMai(getWaitingExInfo().getMai());
            setPos.setBao(getWaitingExInfo().getBao());
        }
        setPos.setAutoHu(autoHu);
        setPos.setAutoOut(autoOut);
        return setPos;
    }

    @Override
    public MJTemplateSet_Pos getPlayBackNotify() {
        MJTemplateSet_Pos setPos = (MJTemplateSet_Pos) super.getPlayBackNotify();
        return getCommonNotify(setPos, true);

    }

    /**
     * 打牌、出牌（抄自六盘水）
     *
     * @param card
     * @return
     */
    public boolean outCard(MJCard card) {
        if (!checkIsQue(card.type)) {
            //如果出的不是定缺的牌 并且存在定缺的牌
            MJCardInit init = mjCardInit(false);
            if (init == null) {
                return false;
            }
            if (checkExistQue(init.getAllCardInts())) {
                return false;
            }
        }
        boolean ret = false;
        if (this.getHandCard() == card) {
            this.cleanHandCard();
            ret = true;
        } else if (0 < this.removePrivateCard(card)) {
            if (this.getHandCard() != null) {
                this.getPrivateCards().add(this.getHandCard());
                this.sortCards();
            }
            ret = true;
        }
        if (ret) {
            this.cleanHandCard();
            this.getOutCardIDs().add(card.cardID);
            this.getSet().getLastOpInfo().setLastOutCard(card.cardID);
            calcHuFan();
            this.getOutJinInfo().setOutJinCard(this.getSet().getmJinCardInfo().checkJinExist(card.type));
            setHaveOutCard(true);
            clearOpHuType();
            clearPass();
            if (Objects.nonNull(louHu)) {
                louHu.addOutCardType(card.type);
            }
            return true;
        }
        return false;
    }

    /**
     * 手牌牌序
     */
    @Override
    public void sortCards() {
        Collections.sort(this.getPrivateCards(), new Comparator<MJCard>() {
            @Override
            public int compare(MJCard o1, MJCard o2) {
                // 金最前
                if (getSet().getmJinCardInfo().checkJinExist(o2.getType())
                        && !getSet().getmJinCardInfo().checkJinExist(o1.getType())) {
                    return 1;
                }
                if (getSet().getmJinCardInfo().checkJinExist(o1.getType())
                        && !getSet().getmJinCardInfo().checkJinExist(o2.getType())) {
                    return -1;
                }
                //缺最后
                if (getRoom().isWanFa_DingQue()) {
                    if (checkIsQue(o1.type) && !checkIsQue(o2.type)) {
                        return -1;
                    } else if (!checkIsQue(o1.type) && checkIsQue(o2.type)) {
                        return 1;
                    } else if (checkIsQue(o1.type) && checkIsQue(o2.type)) {
                        return o1.cardID - o2.cardID;
                    }
                }

                // 从小到大
                return o1.cardID - o2.cardID;
            }
        });
        this.sortBaiBanCards();
    }

    /**
     * 检测是否还存在缺（抄自六盘水）
     *
     * @param allCardInts
     * @return
     */
    public boolean checkExistQue(List<Integer> allCardInts) {
        return allCardInts.stream().anyMatch(mjCard -> checkIsQue(mjCard));
    }

    /**
     * 传一张牌 检测是不是缺 （抄自六盘水）
     *
     * @param cardType 必须是两位数
     * @return
     */
    public boolean checkIsQue(int cardType) {
        if (getRoom().isWanFa_JinBuNengChu() && getSet().getmJinCardInfo().checkJinExist(cardType)) {
            return false;
        }
        if (cardType > 100) {
            cardType = cardType / 100;
        }
        int type = getDingQueTypeValue();
        return type == cardType / 10;
    }

    /**
     * 获取定缺类型值（1:万,2:条,3:筒） （抄自六盘水）
     *
     * @return
     */
    public int getDingQueTypeValue() {
        if (OpType.Wan.equals(this.getDingQue())) {
            return MJCardCfg.WANG.value();
        } else if (OpType.Tiao.equals(this.getDingQue())) {
            return MJCardCfg.TIAO.value();
        } else if (OpType.Tong.equals(this.getDingQue())) {
            return MJCardCfg.TONG.value();
        }
        return MJCardCfg.NOT.value();
    }

    /**
     * 清空打牌之前记录数据
     */
    @Override
    public void clearOutCard() {
        super.clearOutCard();
        //补杠 暗杠清掉
        this.anGangList.clear();
        this.buGangList.clear();
        this.yaoGangList.clear();
    }

    /**
     * 过手
     */
    public void clearPass() {
        // 清空漏胡类型列表
        this.getPosOpRecord().clearHuCardType();
        // 清空漏碰类型列表
        this.getPosOpRecord().clearOpCardType();
        //补杠清掉
        this.jieGangList.clear();
    }

    /**
     * 检测自摸胡
     */
    @Override
    public List<OpType> recieveOpTypes() {
        this.clearOutCard();
        //不能出的牌
        this.addBuNengChuList();
        List<OpType> opTypes = new ArrayList<OpType>();
        if (checkOpType(0, OpType.Ting)) {
            opTypes.add(OpType.Ting);
            if (!isTing && getRoom().isWanFa_BaoTing()) {
                opTypes.add(OpType.BaoTing);
            }
        }
        if (checkOpType(0, OpType.Hu)) {
            opTypes.add(OpType.Hu);
            this.setmHuOpType(MJHuOpType.ZiMo);
        }
        if (!this.isYaoGang || !getRoom().checkExistLastCardOnlyJiePao() || !getMJSetCard().isPopCardNull()) {
            if (checkOpType(0, OpType.AnGang)) {
                opTypes.add(OpType.AnGang);
            }
            if (checkOpType(0, OpType.Gang)) {
                opTypes.add(OpType.Gang);
            }

            //存在摇杠玩法 并且为摇杠
            if (MJTemplateRoomEnum.YaoGang.YAO_GANG.equals(getRoom().wanFa_YaoGang())) {
                yaoGangList.addAll(buGangList);
                yaoGangList.addAll(anGangList);
                if (CollectionUtils.isNotEmpty(yaoGangList)) {
                    opTypes.add(OpType.YaoGang);
                }
            }
        }
        opTypes.add(OpType.Out);
        return opTypes;
    }

    /**
     * 找出不能出的手牌
     * 1.报听
     * 2.定缺的牌
     * 3.金牌
     */
    protected void addBuNengChuList() {
        getPosOpNotice().clearBuNengChuList();
        //1.报听后 除了摸进来的牌 其他的都不能打出去
        if (isTing) {//此时锁的是cardID
            Set<Integer> protectedCardTypes = getPrivateCards().stream().map(k -> k.cardID).collect(Collectors.toSet());
            protectedCardTypes.stream().forEach(k -> getPosOpNotice().addBuNengChuList(k));
            return;
        }
        Set<Integer> allCardType = allCardType();
        //2。添加定缺的不能出的牌
        if (Objects.nonNull(dingQue)) {
            List<Integer> dingQueList = allCardType.stream().filter(k -> checkIsQue(k)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(dingQueList)) {
                List<Integer> buChuList = allCardType.stream().filter(k -> !checkIsQue(k)).collect(Collectors.toList());
                buChuList.stream().forEach(k -> getPosOpNotice().addBuNengChuList(k));
            }
        }
        //3。添加金牌不能出的牌
        if (getRoom().isWanFa_JinBuNengChu()) {
            List<Integer> jinList = allCardType.stream().filter(k -> getSet().getmJinCardInfo().checkJinExist(k)).collect(Collectors.toList());
            jinList.stream().forEach(k -> getPosOpNotice().addBuNengChuList(k));
        }
    }

    public List<Integer> allCardIDs() {
        return allCards().stream().map(MJCard::getCardID).collect(Collectors.toList());
    }

    public Set<Integer> allCardType() {
        return allCards().stream().map(MJCard::getType).collect(Collectors.toSet());
    }

    /**
     * 检测平胡
     */
    @Override
    public OpType checkPingHu(int curOpPos, int cardID) {
        this.clearPaoHu();
        int preCalcHuCardPoint = preCalcHuCardPoint(cardID);
        if (Objects.nonNull(louHu) && louHu.checkLou(cardID, preCalcHuCardPoint)) {
            return OpType.Not;
        }
        // 有胡牌分 说明可以胡
        if (preCalcHuCardPoint > 0 || checkOpType(cardID, OpType.JiePao)) {
            this.setmHuOpType(MJHuOpType.JiePao);
            if (Objects.nonNull(louHu)) {
                louHu.addPassHuCard(cardID, preCalcHuCardPoint);
            }
            return OpType.JiePao;
        }
        return OpType.Not;
    }


    /**
     * 统计本局分数
     *
     * @return
     */
    @Override
    public BaseMJRoom_PosEnd<?> calcPosEnd() {
        // 玩家当局分数结算
        this.getCalcPosEnd().calcPosEnd(this);
        // 位置结算信息
        MJTemplateRoom_PosEnd ret = (MJTemplateRoom_PosEnd) this.posEndInfo();
        ret.setHuTypeMap(this.getCalcPosEnd().getHuTypeMap());
        ret.setPosId(getPosID());
        ret.setPiaoFen(getRoom().isWanFa_PiaoFen() ? piaoFen : null);
        ret.setDingQue(getRoom().isWanFa_DingQue() ? dingQue : null);
        if (getRoom().isWanFa_WaitingEx()) {
            ret.setPao(getWaitingExInfo().getPao());
            ret.setPiao(getWaitingExInfo().getPiao());
            ret.setMai(getWaitingExInfo().getMai());
            ret.setBao(getWaitingExInfo().getBao());
        }
        return ret;
    }

    /**
     * 新位置结算信息
     *
     * @return
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected BaseMJRoom_PosEnd newMJSetPosEnd() {
        return new MJTemplateRoom_PosEnd();
    }

    /**
     * 计算动作分数
     *
     * @param opType
     * @param count
     */
    @Override
    public <T> void calcOpPointType(T opType, int count) {
        this.getCalcPosEnd().calcOpPointType(opType, count);
    }

    /**
     * 新结算
     */
    @Override
    protected AbsBaseResults newResults() {
        return new AbsBaseResults();
    }

    /**
     * 换张（不同色）：可以是不同花色的三张牌，也可以是同花色的三张牌
     *
     * @param value
     */
    protected ChangeCardData findChangeCardDifferentColor(List<Integer> value) {
        ChangeCardData data = new ChangeCardData();
        int changeCardNum = getRoom().wanFa_ChangeCard_Num();
        if (value.size() < changeCardNum) {
            return data;
        }
        Map<Integer, List<Integer>> collect = value.stream().collect(Collectors.groupingBy(k -> k / 100));
        Set<Integer> allShunZiTypes = findAllShunZiTypes();
        for (int i = 1; i <= 4; i++) {
            int finalI = i;
            data.maxSameCardNum = finalI;
            List<Integer> curList = new ArrayList<>(changeCardNum);
            //系统需自动弹起三张：优先弹起相同张数少的牌；
            collect.values().stream().forEach(k -> {
                if (k.size() == finalI) {
                    curList.addAll(k);
                }
            });
            if (curList.size() + data.cards.size() == changeCardNum) {
                data.cards.addAll(curList);
                return data;
            } else if (curList.size() + data.cards.size() > changeCardNum) {
                //相同张数少的有多张，优先弹起没办法组成顺子的牌；
                List<Integer> removeList = curList.stream().filter(k -> allShunZiTypes.contains(k / 100)).collect(Collectors.toList());
                curList.removeAll(removeList);
                //如果还是多张，从中随机弹起三张；
                if (curList.size() + data.cards.size() >= changeCardNum) {
                    data.cards.addAll(curList.subList(0, changeCardNum - data.cards.size()));
                    return data;
                } else {
                    data.cards.addAll(curList);
                    data.cards.addAll(removeList.subList(0, changeCardNum - data.cards.size()));
                    data.containsShunZi = true;
                    return data;
                }
            } else {
                data.cards.addAll(curList);
            }

        }
        data.cards.addAll(value.subList(0, changeCardNum));
        return data;
    }

    /**
     * 找出所有的顺子 对应的cardType
     */
    protected Set<Integer> findAllShunZiTypes() {
        Set<Integer> typeSet = new HashSet<>();
        List<Integer> cardList = getPrivateCard().stream().map(card -> card.type).collect(Collectors.toList());

        for (int i = 1; i < 4; i++) {
            for (int j = 1; j < 7; j++) {
                if (cardList.contains(i * 10 + j) && cardList.contains(i * 10 + j + 1) && cardList.contains(i * 10 + j + 2)) {
                    typeSet.add(i * 10 + j);
                    typeSet.add(i * 10 + j + 1);
                    typeSet.add(i * 10 + j + 2);
                }
            }
        }
        return typeSet;
    }

    /**
     * 换张（同色）：只能是同花色的几张；
     *
     * @param checkList
     */
    protected List<Integer> findChangeCardSameColor(List<Integer> checkList) {
        if (checkList.size() < getRoom().wanFa_ChangeCard_Num()) {
            return checkList;
        }

        Map<Integer, List<Integer>> tongSeMap = checkList.stream().collect(Collectors.groupingBy(k -> k / 1000));
        List<ChangeCardData> dataList = new ArrayList<>();
        tongSeMap.values().forEach(k -> {
            ChangeCardData data = findChangeCardDifferentColor(k);
            if (data.cards.size() == getRoom().wanFa_ChangeCard_Num()) {
                dataList.add(data);
            }
        });
        if (dataList.size() == 1) {
            return dataList.get(0).cards;
        } else if (dataList.size() > 1) {
            int minNum = dataList.stream().mapToInt(k -> k.maxSameCardNum).min().getAsInt();
            List<ChangeCardData> collect = dataList.stream().filter(k -> k.maxSameCardNum == minNum).collect(Collectors.toList());
            //牌数一样
            if (collect.size() == 1) {
                return collect.get(0).cards;
            }
            List<ChangeCardData> collect1 = dataList.stream().filter(k -> k.containsShunZi == false).collect(Collectors.toList());
            //不拆顺子
            if (collect1.size() == 1) {
                return collect1.get(0).cards;
            }
            return dataList.get(0).cards;
        }
        return checkList.subList(0, getRoom().wanFa_ChangeCard_Num());
    }

    /**
     * 找出系统默认要换的牌，机器人托管要换的牌
     */
    public void initFirstChangeCard() {
        List<Integer> jins = new ArrayList<>();
        List<Integer> allCardIDs = allCardIDs();
        if (getSet().getmJinCardInfo().sizeJin() > 0) {
            jins = allCardIDs.stream().filter(k -> getSet().getmJinCardInfo().checkJinExist(k)).collect(Collectors.toList());
        }
        allCardIDs.removeAll(jins);
        List<Integer> changCardList;
        if (getRoom().isWanFa_ChangeCardType_DifferentColor()) {
            changCardList = findChangeCardDifferentColor(allCardIDs).cards;
        } else {
            changCardList = findChangeCardSameColor(allCardIDs);
        }
        int changeCardNum = getRoom().wanFa_ChangeCard_Num();
        if (changCardList.size() < changeCardNum) {
            changCardList.addAll(jins.subList(0, changeCardNum - changCardList.size()));
        }
        setFirstChangeCardList(changCardList);
    }

    public void addBuGangList(List<Integer> buGangList) {
        this.buGangList.add(buGangList);
    }

    public void addJieGangList(List<Integer> jieGangList) {
        this.jieGangList.add(jieGangList);
    }

    public void addAnGangList(List<Integer> anGangList) {
        this.anGangList.add(anGangList);
    }

    public void addYaoGangList(List<List<Integer>> gangList) {
        this.yaoGangList.addAll(gangList);
    }

    /**
     * 默认算1分 有需要的自己重写此方法
     */
    public void actualTimeCalcGangPoint() {
        if (getRoom().wanFa_ActualTimeCalcPoint().equals(MJTemplateRoomEnum.ActualTimeCalcPoint.CALC_GANG_POINT)) {
            int dianPos = getPublicCardList().get(sizePublicCardList() - 1).get(1);
            if (dianPos != getPosID()) {
                calcGangPoint1v1(1, dianPos);
            } else {
                calcGangPoint1v3(1);
            }
//            MJTemplateRoomSet set = (MJTemplateRoomSet) getSet();
//            //这个要自己改成自己的游戏类 不是每个游戏必要的  自己添加
//            set.getRoomPlayBack().playBack2All(SDEMOMJ_Promptly.make(getRoom().getRoomID(), set.getPlayerInfoList(getPosID(), 1, dianPos)));
            //实时算分需要清零

        }

    }

    /**
     * 默认算1分 有需要的自己重写此方法
     *
     * @param opType
     */
    public void actualTimeCalcGangPoint(OpType opType) {
        actualTimeCalcGangPoint();

    }

    /**
     * 提前预算胡牌分
     *
     * @param init
     * @return
     */
    public int preCalcPosPoint(MJCardInit init) {
        return 0;
    }

    /**
     * /**
     * 提前预算胡牌分
     *
     * @param init
     * @param type 当前检测的牌
     * @return
     */
    public int preCalcPosPoint(MJCardInit init, int type) {
        return preCalcPosPoint(init);
    }

    /**
     * 预先计算的牌型分 这个可以前 听牌对应的胡分
     * 如果有维护听牌分 可以直接拿听牌分来判断
     * 如果没有  需要重写
     *
     * @param cardID
     * @return
     */
    protected int preCalcHuCardPoint(int cardID) {
        int type = cardID > 100 ? cardID / 100 : cardID;
        if (Objects.nonNull(huInfo)) {
            Integer point = huInfo.get(type);
            return Objects.nonNull(point) ? point : 0;
        }
        return 0;
    }

    public void addTingInfoList(MJTemplateTingInfo tingInfo) {
        if (this.tingInfoList.stream().filter(k -> k.getCardType() == tingInfo.getCardType()).count() > 0) {
            return;
        }
        this.tingInfoList.add(tingInfo);
    }

    public class ChangeCardData {
        public List<Integer> cards = new ArrayList<>();
        public int maxSameCardNum;//相同牌最多的
        public boolean containsShunZi;//牌是否包含顺子
    }

    @Override
    public MJSetPosRobot getSetPosRobot() {
        return new MJTemplateSetPosRobot(this);
    }

    @Override
    public MJTemplateRoom getRoom() {
        return (MJTemplateRoom) super.getRoom();
    }

    /**
     * 位置结算信息（实时算分）
     *
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected BaseMJRoom_PosEnd posEndInfo() {
        MJTemplateRoom_PosEnd ret = (MJTemplateRoom_PosEnd) super.posEndInfo();
        if (getRoom().wanFa_ActualTimeCalcPoint().equals(MJTemplateRoomEnum.ActualTimeCalcPoint.CALC_GANG_POINT)) {
            // 设置结算分数(需要加上中途结算分数)
            ret.setPoint(this.getEndPoint() + getActualTimePoint());
            // 固定算分总结算才有分数
            if (!getRoom().isGuDingSuanFen()) {
                // 竞技点分数
                if (this.getRoom().calcFenUseYiKao() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
                    // 比赛分
                    ret.setSportsPoint(CommMath.addDouble(getActualTimeSportsPoint(), this.getDeductPointYiKao()));
                } else {
                    //显示用
                    ret.setSportsPoint(this.getRoomPos().setSportsPoint(this.getEndPoint() + getActualTimePoint()));
                }
            } else {
                if (RoomTypeEnum.UNION.equals(getRoom().getRoomTypeEnum())) {
                    ret.setSportsPoint(0D);
                }
            }
        }
        return ret;
    }

    public MJTemplateRoomSet getTemplateRoomSet() {
        return (MJTemplateRoomSet) getSet();
    }

    /**
     * 检测是否缺一门
     *
     * @param mCardInit
     * @return
     */
    public boolean checkQueYiMen(MJCardInit mCardInit) {
        if (checkExistQue(mCardInit.getAllCardInts())) {
            return false;
        }
        List<Integer> allInt = new ArrayList<>();
        // 获取牌列表
        allInt.addAll(mCardInit.getAllCardInts());
        // 获取顺子，刻子，杠组成的胡牌。
        allInt.addAll(publicCardTypeList());
        // 分组列表
        Map<Integer, Long> map = allInt.stream().collect(Collectors.groupingBy(p -> p >= 1000 ? (p / 1000) : (p / 10), Collectors.counting()));
        // 检查分组数据
        if (null == map || map.size() <= 0) {
            return true;
        }
        // 移除花牌
        map.remove(MJCardCfg.HUA.value());
        // 移除空牌
        map.remove(MJCardCfg.NOT.value());
        // 移除风牌，箭牌
        map.remove(MJCardCfg.FENG.value());
        int size = map.size();
        return size <= 2;
    }

    /**
     * 获取麻将牌
     *
     * @return
     */
    public MJTemplateSetCard getMJSetCard() {
        return this.getTemplateRoomSet().getMJSetCard();
    }

    /**
     * 是否已经胡牌了
     *
     * @return
     */
    public boolean isHuCard() {
        return !(getHuType().equals(HuType.NotHu) || getHuType().equals(HuType.DianPao));
    }

    public MJTemplateWaitingExInfo getWaitingExInfo() {
        return ((MJTemplateRoomPos) getRoomPos()).getWaitingExInfo();
    }

    public void opDingQue(OpType dingQue) {
        this.dingQue = dingQue;
        calcHuFan();
        sortCards();
    }

    /**
     * 通知客户端定缺
     * 定缺需要全部定缺完才显示
     *
     * @return
     */
    public OpType clientDingQue() {
        if (getRoom().isWanFa_DingQue()) {
            if (getTemplateRoomSet().isDingQueFinish()) {
                return dingQue;
            } else if (dingQue.equals(OpType.Not)) {
                return OpType.Not;
            } else {
                return OpType.NotShow;
            }
        }
        return null;
    }
}
