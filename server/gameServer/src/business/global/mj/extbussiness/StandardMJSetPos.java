package business.global.mj.extbussiness;

import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.MJCard;
import business.global.mj.extbussiness.dto.*;
import business.global.mj.extbussiness.hutype.StandardMJTingImpl;
import business.global.mj.manage.MJFactory;
import business.global.mj.robot.MJSetPosRobot;
import business.global.room.mj.MJRoomPos;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.mj.HuType;
import cenum.mj.MJHuOpType;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommMath;
import jsproto.c2s.cclass.mj.BaseMJRoom_PosEnd;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.cclass.room.AbsBaseResults;
import jsproto.c2s.iclass.mj._Promptly;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 麻将 每一局每个位置信息
 *
 * @author Huaxing
 */
@Data
public class StandardMJSetPos extends AbsMJSetPos {
    /**
     * 胡牌信息
     */
    private Map<Integer, Integer> huInfo = new HashMap<>();
    /**
     * 听牌信息
     */
    private final Map<Integer, StandardMJTingInfo> tingInfoListMap = new ConcurrentHashMap<>();
    /**
     * 杠上炮标志
     */
    private boolean gangShangPao = false;
    /**
     * 抢杠胡标志
     */
    private boolean qiangGangHuFlag = false;

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
     * 吃牌列表
     */
    private List<Integer> chiCardList = new ArrayList<>();

    /**
     * 地胡标志
     */
    private boolean diHuFlag = true;
    /**
     * 是否听
     */
    private boolean isTing = false;

    /**
     * 中途杠分
     */
    private double midPrizeSport;
    /**
     * 中途分
     */
    private int midPoint;

    private int lastGangGetPos = -1;

    private Map<Integer, Double> singleGangPrizeSportMap = new HashMap<>();

    private Map<Integer, Integer> singleGangPrizeMap = new HashMap<>();

    private double singleGangPrizeSport;

    /**
     * 模板麻将换三张部分
     * 游戏开始时 服务端预先选的要换的，客户端默认弹起来的牌
     */
    protected List<Integer> firstChangeCardList;

    /**
     * 模板麻将换三张部分
     * 客户端发给服务端的要换的牌
     */
    protected List<Integer> changeCardList = new ArrayList<>();

    /**
     * 中码列表
     */
    public List<Integer> zhongList = new ArrayList<>();

    public StandardMJSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set) {
        super(posID, roomPos, set, ((StandardMJRoomSet) set).getTingClass());
        this.setOpHuType(null);
        this.setMSetOp(absSetOp());
        this.setCalcPosEnd(absCalcPosEnd());
    }

    /**
     * 清空操作状态
     */
    public void clearOp() {
        ((StandardMJSetOp) this.getmSetOp()).clearOp();
        this.getPosOpNotice().clearBuNengChuList();
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


    /**
     * 检测自摸胡
     */
    @Override
    public List<OpType> recieveOpTypes() {
        // 清空记录数据
        this.clearOutCard();
        this.getPosOpRecord().clearOpCardType();
        List<OpType> opTypes = new ArrayList<OpType>();
        if (checkOpType(0, OpType.Hu)) {
            opTypes.add(OpType.Hu);
        }
        if (opTypes.size() > 0) {
            // 可胡牌，下一轮检测是不是漏胡用
            if (((StandardMJRoom) getRoom()).isHaveLouHu() && ((StandardMJRoom) getRoom()).isHaveZiMoLouHu()) {
                this.getPosOpRecord().setHuCardType(1000);
            }
            this.setmHuOpType(MJHuOpType.ZiMo);
        }

        if (checkOpType(0, OpType.Ting)) {
            if (((StandardMJRoom) getRoom()).isBaoTing() && !isTing()) {
                opTypes.add(OpType.BaoTing);
            }
            opTypes.add(OpType.Ting);
        }

        if (checkOpType(0, OpType.AnGang)) {
            opTypes.add(OpType.AnGang);
        }
        if (checkOpType(0, OpType.Gang)) {
            opTypes.add(OpType.Gang);
        }
        opTypes.add(OpType.Out);
        return opTypes;
    }

    @Override
    public StandardMJSet_Pos getPlayBackNotify() {
        return (StandardMJSet_Pos) getNotify(true);
    }

    /**
     * 计算总结算信息
     */
    @Override
    public void calcResults() {
        StandardMJResults mResultsInfo = (StandardMJResults) this.mResultsInfo();
        this.setResults(mResultsInfo);
    }

    /**
     * 检测平胡
     */
    @Override
    public OpType checkPingHu(int curOpPos, int cardID) {
        // 清空操作牌初始									
        this.clearPaoHu();
        StandardMJPointItem itemOld = null;
        if (Objects.nonNull(this.getOpHuType())) {
            // 旧分数
            itemOld = ((StandardMJPointItem) this.getOpHuType()).deepClone();
        }
        if (checkOpType(cardID, OpType.Hu)) {
            if (this.getPosOpRecord().isHuCardType(1000)) {
                if (((StandardMJRoom) getRoom()).isLouHuOverRemove()) {
                    // 	漏胡：如果玩家弃胡，该圈内如果能胡的分数比之前弃胡的多才可以胡，不然只能摸牌后才可以胡；
                    // 新分数
                    StandardMJPointItem itemNew = (StandardMJPointItem) this.getOpHuType();
                    if (Objects.nonNull(itemOld) && Objects.nonNull(itemNew)) {
                        if (getLouHuPoint(itemOld) >= getLouHuPoint(itemNew)) {
                            return OpType.Not;
                        }
                    } else {
                        return OpType.Not;
                    }
                }else{
                    return OpType.Not;
                }
            }
            this.setmHuOpType(MJHuOpType.JiePao);
            // 可胡牌，下一轮检测是不是漏胡用
            if (((StandardMJRoom) getRoom()).isHaveLouHu()) {
                this.getPosOpRecord().setHuCardType(1000);
            }
            // 设置炮胡类型
            return OpType.JiePao;
        }
        return OpType.Not;
    }


    /**
     * 计算动作分数
     *
     * @param count
     */
    @Override
    public <T> void calcOpPointType(T opType, int count) {
        this.getCalcPosEnd().calcOpPointType(opType, count);
    }

    /**
     * 操作
     *
     * @return
     */
    public int isOpSize() {
        return ((StandardMJSetOp) this.getmSetOp()).isOpSize();
    }

    public boolean isQiangGangHuFlag() {
        return qiangGangHuFlag;
    }

    public void setQiangGangHuFlag(boolean qiangGangHuFlag) {
        this.qiangGangHuFlag = qiangGangHuFlag;
    }

    /**
     * 检查报听
     *
     * @param key
     * @return
     */
    public boolean checkBaotingGang(Integer key, OpType opType) {
        //非一口香玩法，	报听玩家此后不能碰牌；杠牌后如果听得牌不变则可以选择是否开杠；胡牌让玩家自由选择是否胡牌；
        List<MJCard> allCardInts = new
                ArrayList<>(allCards());
        allCardInts.removeIf(b -> b.getType() == key);
        StandardMJTingImpl tImpl = (StandardMJTingImpl) MJFactory.getTingCard(((StandardMJRoomSet) getSet()).getTingClass());
        if (tImpl.checkTingCardList(allCardInts, this)) {
            return true;
        }
        return false;
    }

    /**
     * 清空杠列表
     */
    public void clearBuGangList() {
        this.buGangList.clear();
    }

    /**
     * 清空接杠列表
     */
    public void clearJieGangList() {
        this.jieGangList.clear();
    }

    /**
     * 清空杠列表
     */
    public void clearGangList() {
        this.anGangList.clear();
    }

    @Override
    public void clearOpHuType() {
        this.setOpHuType(null);
    }

    /**
     * 获取不能出
     *
     * @return {@link List<Integer>}
     */
    public List<Integer> getBuNengChuList() {
        return getPosOpNotice().getBuNengChuList();
    }

    public Map<Integer, StandardMJTingInfo> getTingInfoListMap() {
        return tingInfoListMap;
    }

    /**
     * 添加ting信息列表
     *
     * @param tingInfo ting信息
     */
    public void addTingInfoList(StandardMJTingInfo tingInfo) {
        tingInfoListMap.put(tingInfo.getCardType(), tingInfo);
    }

    public void pidPointEnd() {
        if (((StandardMJRoom) getRoom()).getGuoFen() > 0) {
            this.getRoomPos().calcRoomPoint(this.getEndPoint());
            this.getRoomPos().setTempPoint(this.getRoomPos().getPoint());
            if (PrizeType.RoomCard.equals(getRoom().getBaseRoomConfigure().getPrizeType())) {
                this.getRoomPos().addCountPoint(this.getEndPoint());
            }
            if (this.getHuType() != HuType.NotHu && this.getHuType() != HuType.DianPao) {
                this.getRoomPos().setHuCnt(this.getRoomPos().getHuCnt() + 1);
            }
        }else{
            super.pidPointEnd();
        }
    }

    /**
     * 实时计算杠分
     *
     * @param point 奖分
     **/
    public void calcGangPoint(int point) {
        StandardMJSetPos setPos;
        for (int i = 0; i < this.getSet().getRoom().getPlayerNum(); i++) {
            setPos = (StandardMJSetPos) getSet().getMJSetPos(i);
            if (Objects.isNull(setPos)) {
                continue;
            }
            if (getRoom().isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(getRoom().getRoomTypeEnum())) {
                if(getRoomPos().getGameBeginSportsPoint()<=0){
                    return;
                }
            }
            if (setPos.getPosID() != getPosID()) {
                //输的分
                if (getRoom().calcFenUseYiKao() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
                    if (getRoom().isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() <= 0) {
                        continue;
                    }
                    double beiShu = Math.max(0D, this.getRoom().getRoomTyepImpl().getSportsDouble());
                    double sPoint = CommMath.mul(point, beiShu);
                    double sportPoint = sPoint;
                    if (getRoom().isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() - sPoint <= 0) {
                        sportPoint = setPos.getRoomPos().getRoomSportsPoint();
                    }
                    setPos.setDeductEndPoint(-sportPoint);
                    setPos.setMidPrizeSport(-sportPoint);
                    setDeductEndPoint(sportPoint);
                    setMidPrizeSport(sportPoint);
                }
                //输钱
                setPos.setMidPoint(-point);
                setPos.setPidSumPointEnd(-point);
                setPos.setDeductEndPoint(0);
                //赢钱
                setMidPoint(point);
                setPidSumPointEnd(point);
                setDeductEndPoint(0);
            }
        }
    }

    /**
     * 实时计算杠分
     *
     * @param point   奖分
     * @param losePos 付钱方
     **/
    public void calcGangPoint1V1(int point, int losePos) {
        StandardMJSetPos setPos;
        for (int i = 0; i < this.getSet().getRoom().getPlayerNum(); i++) {
            setPos = (StandardMJSetPos) getSet().getMJSetPos(i);
            if (Objects.isNull(setPos)) {
                continue;
            }
            if (getRoom().isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(getRoom().getRoomTypeEnum())) {
                if(getRoomPos().getGameBeginSportsPoint()<=0){
                    return;
                }
            }
            if (losePos == setPos.getPosID()) {
                //输的分
                if (getRoom().isRulesOfCanNotBelowZero() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
                    if (setPos.getRoomPos().getRoomSportsPoint() <= 0) {
                        continue;
                    }
                    double beiShu = Math.max(0D, this.getRoom().getRoomTyepImpl().getSportsDouble());
                    double sPoint = CommMath.mul(point, beiShu);
                    double sportPoint = sPoint;
                    if (setPos.getRoomPos().getRoomSportsPoint() - sPoint <= 0) {
                        sportPoint = setPos.getRoomPos().getRoomSportsPoint();
                    }
                    setPos.setDeductEndPoint(-sportPoint);
                    setPos.setMidPrizeSport(-sportPoint);
                    setDeductEndPoint(sportPoint);
                    setMidPrizeSport(sportPoint);
                    setLastGangGetPos(losePos);
                    setSingleGangPrizeSport(sportPoint);

                }
                //输钱
                setPos.setMidPoint(-point);
                setPos.setPidSumPointEnd(-point);
                setPos.setDeductEndPoint(0);
                //赢钱
                setMidPoint(point);
                setPidSumPointEnd(point);
                setDeductEndPoint(0);
            }
        }
    }

    /**
     * 实时计算杠分
     *
     * @param point      奖分
     * @param winPos     杠分回退玩家
     * @param sportPoint 回退的竞技点
     **/
    public void calcGangPoint1V1TuiFen(int point, int winPos, double sportPoint) {
        StandardMJSetPos setPos;
        for (int i = 0; i < this.getSet().getRoom().getPlayerNum(); i++) {
            setPos = (StandardMJSetPos) getSet().getMJSetPos(i);
            if (Objects.isNull(setPos)) {
                continue;
            }
            if (winPos == setPos.getPosID()) {
                //赢的分
                setPos.setDeductEndPoint(sportPoint);
                setPos.setMidPrizeSport(sportPoint);
                setDeductEndPoint(-sportPoint);
                setMidPrizeSport(-sportPoint);
                //赢钱
                setPos.setMidPoint(point);
                setPos.setPidSumPointEnd(point);
                setPos.setDeductEndPoint(0);
                //输钱
                setMidPoint(-point);
                setPidSumPointEnd(-point);
                setDeductEndPoint(0);
            }
        }
    }

    /**
     * 位置结算信息（实时算分）
     *
     * @return
     **/
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected BaseMJRoom_PosEnd posEndInfo() {
        StandardMJRoom_PosEnd ret = (StandardMJRoom_PosEnd) super.posEndInfo();
        if (((StandardMJRoom) getRoom()).isSSSG()) {
            // 固定算分总结算才有分数
            if (!getRoom().isGuDingSuanFen()) {
                // 竞技点分数
                if (this.getRoom().calcFenUseYiKao() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
                    //显示用
                    ret.setSportsPointTemp(CommMath.addDouble(getMidPrizeSport(), this.getDeductPointYiKao()));
                    // 比赛分
                    ret.setSportsPoint(this.getDeductPointYiKao());
                } else {
                    //显示用
                    ret.setSportsPointTemp(this.getRoomPos().setSportsPoint(this.getEndPoint() + getMidPoint()));
                    // 比赛分
                    ret.setSportsPoint(this.getRoomPos().setSportsPoint(this.getEndPoint()));
                }
            } else {
                if (RoomTypeEnum.UNION.equals(getRoom().getRoomTypeEnum())) {
                    ret.setSportsPoint(0D);
                    // 比赛分
                    ret.setSportsPointTemp(0D);
                }
            }
        }
        return ret;
    }


    //---------------下面方法根据需求自己扩展----------------------

    /**
     * 小局结算信息适配
     * 根据自己游戏决定是否扩展修改这个方法
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public BaseMJRoom_PosEnd calcPosEnd() {
        StandardMJCalcPosEnd calcPosEnd = (StandardMJCalcPosEnd) this.getCalcPosEnd();
        Map<String, Integer> huTypeMap = calcPosEnd.getEXTHuTypeMap();
        // 玩家当局分数结算
        this.getCalcPosEnd().calcPosEnd(this);
        // 位置结算信息
        StandardMJRoom_PosEnd ret = (StandardMJRoom_PosEnd) this.posEndInfo();
        ret.setEndPoint(this.getCalcPosEnd().getCalcPosEnd());
        ret.setGangFen(huTypeMap.getOrDefault(StandardMJRoomEnum.OpPoint.GangPoint.name(), 0));
        ret.setHuPoint(huTypeMap.getOrDefault(StandardMJRoomEnum.OpPoint.HuPoint.name(), 0));
        huTypeMap.remove(StandardMJRoomEnum.OpPoint.HuPoint.name());
        if (((StandardMJRoom) getRoom()).isSSSG()) {
            //与客户端约定 hufen 显示单局积分  point 客户端源码自己累加用
            ret.setHuFen(this.getEndPoint() + getMidPoint());
        }
        if (((StandardMJRoom) getRoom()).isBaoTing()) {
            ret.setTing(isTing);
        }
        ret.setPiao(((StandardMJRoomPos) getRoomPos()).getPiaoFenEnum().piao);
        if (!((StandardMJRoom) getRoom()).isSameZhongMa()) {
            ret.setZhongList(zhongList);
        }
        return ret;
    }

    /**
     * 玩家当局信息适配
     * 根据自己游戏决定是否扩展修改这个方法
     */
    @Override
    public BaseMJSet_Pos getNotify(boolean isSelf) {
        if (isRevealCard()) {
            isSelf = true;
        }
        StandardMJSet_Pos ret = this.newMJSetPos();
        // 玩家位置
        ret.setPosID(this.getPosID());
        // 手牌
        int length = sizePrivateCard();
        for (int i = 0; i < length; i++) {
            ret.getShouCard().add((getPCard(i).isLock || isSelf) ? getPCard(i).cardID : 0);
        }
        // 可胡的牌
        ret.setHuCard(isSelf ? this.getHuCardTypes() : null);
        if (this.getHandCard() != null) {
            // 首牌
            ret.setHandCard((isSelf || getHandCard().isLock) ? this.getHandCard().getCardID() : 5000);
        }
        // 打出的牌
        ret.setOutCard(this.getOutCardIDs());
        // 公共牌
        ret.setPublicCardList(this.getPublicCardList());
        // 掉线连接
        ret.setIsLostConnect(null);
        //竞技点分数
        ret.setSportsPoint(getRoomPos().getRoomSportsPoint());
        //分数
        ret.setPoint(getRoomPos().getPoint());
        ret.setTrusteeship(this.getRoomPos().isTrusteeship());
        // 公共牌列表
        ret.setPublicCardStrs(this.getPublicCardList().toString());
        ret.setTing(this.isTing);
        ret.setPiao(((StandardMJRoomPos) getRoomPos()).getPiaoFenEnum().piao);
        if (((StandardMJRoom) getRoom()).getHuanZhangNum() > 0) {
            ret.setChangeCardList(new ArrayList<>());
            if (Objects.nonNull(changeCardList)) {
                for (int card : changeCardList) {
                    ret.addChangeCardList(isSelf ? card : 0);
                }
            }
        }
        ret.setHuaList(getPosOpRecord().getHuaList());
        ret.setHuInfo(huInfo);
        return ret;
    }

    /**
     * 当局机器人适配（一般不需要改动）
     * 根据自己游戏，看需要不需要扩展该类，并重写这个方法
     */
    public MJSetPosRobot getSetPosRobot() {
        return new StandardMJSetPosRobot(this);
    }

    /**
     * 当局位置类适配
     * 根据自己游戏，看需要不需要扩展该类，并重写这个方法
     */
    protected StandardMJSetOp absSetOp() {
        return new StandardMJSetOp(this);
    }

    /**
     * 当局位置结算类类适配
     * 根据自己游戏，看需要不需要扩展该类，并重写这个方法
     */
    protected StandardMJCalcPosEnd absCalcPosEnd() {
        return new StandardMJCalcPosEnd(this);
    }


    /**
     * 位置信息适配
     * 根据自己游戏，看需要不需要扩展该类，并重写这个方法
     *
     * @return
     */
    @Override
    protected StandardMJSet_Pos newMJSetPos() {
        return new StandardMJSet_Pos();
    }

    /**
     * 位置信息结算适配
     * 用户根据结算信息是否需要添加决定是否要重写该类
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    protected StandardMJRoom_PosEnd newMJSetPosEnd() {
        return new StandardMJRoom_PosEnd();
    }

    /**
     * 点炮次数等信息适配
     * 根据游戏决定是否修改
     */
    @Override
    protected AbsBaseResults newResults() {
        return new StandardMJResults();
    }

    /**
     * 漏胡分
     *
     * @param item 项
     * @return int
     */
    public int getLouHuPoint(StandardMJPointItem item){
        return item.getLHPoint();
    }

    /**
     * 中途竞技点增减分
     *
     * @param sportPoint 运动点
     */
    public void setMidPrizeSport(double sportPoint) {
        this.midPrizeSport += sportPoint;
    }

    public void setMidPoint(int midPoint) {
        this.midPoint += midPoint;
    }

    /**
     * 实时算杠接杠
     */
    public void resolveSSSGJieGang(int jieGangPos) {
        //普通接杠后立马算分
        int point = StandardMJRoomEnum.OpPoint.JieGang.value();
        //结算杠分
        calcGangPoint(point);
        StandardMJRoomSet nSet = (StandardMJRoomSet)getSet();
        nSet.getRoomPlayBack().playBack2All(_Promptly.make(getSet().getRoom().getRoomID(), nSet.getPlayerInfoList(getPosID(), point, -1),getRoom().getGameName()));
    }

    /**
     * 实时算杠暗杠
     */
    public void resolveSSSGAnGang() {
        //普通暗杠立马结算分数
        int point = StandardMJRoomEnum.OpPoint.AnGang.value();
        //结算杠分
        calcGangPoint(point);
        StandardMJRoomSet nSet = (StandardMJRoomSet)getSet();
        nSet.getRoomPlayBack().playBack2All(_Promptly.make(getSet().getRoom().getRoomID(), nSet.getPlayerInfoList(getPosID(), point, -1),getRoom().getGameName()));
    }

    /**
     * 实时算杠补杠
     */
    public void resolveSSSGBuGang(int buGangPos,int cardID) {
        int point = StandardMJRoomEnum.OpPoint.Gang.value();
        //结算杠分
        calcGangPoint1V1(point,buGangPos);
        StandardMJRoomSet nSet = (StandardMJRoomSet)getSet();
        nSet.getRoomPlayBack().playBack2All(_Promptly.make(getSet().getRoom().getRoomID(), nSet.getPlayerInfoList(getPosID(), point, buGangPos),getRoom().getGameName()));
    }

    /**
     * 退补杠的钱
     */
    public void resolveSSSGTuiGang(int cardID) {
        //要退杠的人，把钱退给补杠时候的收过钱的人
        int lastOpPos = getSet().getLastOpInfo().getLastOpPos();
        int point = StandardMJRoomEnum.OpPoint.Gang.value();
        //结算杠分
        StandardMJSetPos mjSetPos = (StandardMJSetPos) getMJSetPos(lastOpPos);
        //退钱给mjSetPos.getLastGangGetPos()
        mjSetPos.calcGangPoint1V1TuiFen(point,mjSetPos.getLastGangGetPos(),mjSetPos.getSingleGangPrizeSport());
        StandardMJRoomSet nSet = (StandardMJRoomSet)getSet();
        nSet.getRoomPlayBack().playBack2All(_Promptly.make(getSet().getRoom().getRoomID(), nSet.getPlayerInfoList(mjSetPos.getLastGangGetPos(), point, mjSetPos.getPosID()),nSet.getRoom().getGameName()));
    }
}

