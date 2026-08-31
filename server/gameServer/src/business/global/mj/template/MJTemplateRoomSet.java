package business.global.mj.template;

import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetPosMgr;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.MJCard;
import business.global.mj.manage.MJFactory;
import business.global.mj.template.optype.MJTemplateBuHuaImpl;
import business.global.mj.template.wanfa.MJTemplateGenZhuang;
import business.global.mj.template.xueZhan.MJTemplate_XueZhanSetPos;
import cenum.mj.FlowerEnum;
import cenum.mj.HuType;
import cenum.room.RoomDissolutionState;
import cenum.room.SetState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.template.*;
import jsproto.c2s.cclass.pos.PlayerPosInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 模板麻将一局游戏逻辑
 *
 * @author Huaxing
 */
@Getter
@Setter
public abstract class MJTemplateRoomSet extends AbsMJSetRoom {
    public List<Integer> maList = new ArrayList<>();
    public List<Integer> zhongList = new ArrayList<>();
    /**
     * 刚上开花
     */
    protected boolean isGSP;
    protected boolean isDingQueFinish;
    /**
     * 开金本金 翻出来的那张,本金不是金
     */
    protected int benJin;
    protected MJTemplateGenZhuang genZhuang;
    protected MJTemplateRoomEnum.WaitingExType curWaitingExType = MJTemplateRoomEnum.WaitingExType.NOT;
    /**
     * 下次胡牌的顺序
     */
    public MJTemplateRoomEnum.HuCardEndType nextHuEndType = MJTemplateRoomEnum.HuCardEndType.FIRST;
    protected List<MJTemplateRoomEnum.WaitingExType> waitingExTypes;

    public MJTemplateRoomSet(int setID, MJTemplateRoom room, int dPos) {
        super(setID, room, dPos);
        this.startMS = CommTime.nowMS();
        this.addGameConfig();
        this.genZhuang = newMJGenZhuang();
        this.initPiaoFen(room);
        // 洗底牌
        this.absMJSetCard();
        this.initSetPos();
        this.setPosMgr = this.absMJSetPosMgr();
        this.initSetState();
    }

    public void initPiaoFen(MJTemplateRoom room) {
        if (room.isWanFa_WaitingEx()) {
            List<MJTemplateRoomEnum.WaitingExType> list = getRoom().addWaitingTypes();
            this.waitingExTypes = new ArrayList<>();
            if (room.wanFa_PiaoFen().equals(MJTemplateRoomEnum.PiaoFen.PIAO_FEN_FIRSER_SET)) {
                if (getSetID() == 1) {
                    this.waitingExTypes.addAll(list);
                }
            } else {
                this.waitingExTypes.addAll(list);

            }
            if (this.waitingExTypes.size() > 0) {
                curWaitingExType = this.waitingExTypes.get(0);
            }
        }
    }


    /**
     * 初始化跟庄
     *
     * @return
     */
    protected MJTemplateGenZhuang newMJGenZhuang() {
        return new MJTemplateGenZhuang(this);
    }

    /**
     * 默认选出要换的几张牌
     */
    public void initSanZhang() {
        if (MJTemplateRoomEnum.ChangeCardType.NOT.equals(getRoom().wanFa_ChangeCardType())) {
            return;
        }
        // 玩家初始发牌
        posDict.values().forEach(k -> ((MJTemplateSetPos) k).initFirstChangeCard());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MJTemplateRoomSet) {
            if (getSetID() == ((MJTemplateRoomSet) o).getSetID()) {
                return true;
            }
        }
        return false;
    }

    // 每200ms更新1次 秒
    @Override
    public boolean update(int sec) {
        if (this.state.equals(SetState.WaitingEx)) {
            doPiaoState();
            return false;
        }
        return super.update(sec);
    }

    // 一局结束，是否流局
    @Override
    public void endSet() {
        CommLogD.info("endSet id:{}", getSetID());
        if (this.state == SetState.End) {
            if (RoomDissolutionState.Dissolution.equals(getRoom().getRoomDissolutionState())
                    && business.global.billing.LegacySettlementGuard.beginDissolutionAdjustment(
                    this.room.getRoomID(), this.getSetID(), "mahjong-common-v1")) {
                this.calcDaJuFenShuRoomPoint();
            }
            return;
        }
        if (!business.global.billing.LegacySettlementGuard.beginRound(
                this.room.getRoomID(), this.getSetID(), "mahjong-common-v1")) {
            return;
        }
        this.state = SetState.End;
        setEnd(true);
        //计算买码列表
        this.calcMaiMa();
        // 结算算分
        this.calcPoint();
        // 广播
        this.getRoomPlayBack().playBack2All(this.setEnd(room.getRoomID(), this.getNotify_setEnd()));
        // 小局托管自动解散
        this.setTrusteeshipAutoDissolution();
        // 记录回放码
        this.roomPlayBack();

    }

    /**
     * 计算买码的列表
     * 中码计算 （抄自常熟麻将）
     * 各位置对应的中马：
     * 系数牌1、5、9，东风、红中、花（春），对应庄家位置；
     * 系数牌2、6，南风，发财、花（夏），对应庄家下家位置；
     * 系数牌3、7，西风，白板、花（秋），对应庄家对家位置；
     * 系数牌4、8，北风、花（冬），对应庄家上家位置；
     */
    public void calcMaiMa() {
        //如果不存在买码
        if (getRoom().wanFa_MaiMa().equals(MJTemplateRoomEnum.MaiMa.NOT)) {
            return;
        }
        this.maList = this.getSetCard().calcMaList();
        //计算中码情况
        int nextPos;
        MJTemplateSetPos setPos;
        List<Integer> zhongMaList;
        for (int i = 0; i < getPlayerNum(); i++) {
            nextPos = (this.dPos + i) % getPlayerNum();
            setPos = (MJTemplateSetPos) getMJSetPos(nextPos);
            if (Objects.isNull(setPos)) {
                continue;
            }
            //比较值系数
            int comparedValue = i + 1;
            //如果比较值系数等于 cardID/100 % 10 % 4 则是中码
            zhongMaList = maList.stream().filter(cardID -> cardID / 100 % 10 % 4 == comparedValue).collect(Collectors.toList());
            setPos.getZhongList().addAll(zhongMaList);
        }
    }

    /**
     * 初始状态
     */
    public void initSetState() {
        if (MJTemplateRoomEnum.PiaoFen.PIAO_FEN_EACH_SET.equals(getRoom().wanFa_PiaoFen()) || (MJTemplateRoomEnum.PiaoFen.PIAO_FEN_FIRSER_SET.equals(getRoom().wanFa_PiaoFen()) && getSetID() == 1)) {
            this.getRoom().getRoomPosMgr().setAllLatelyOutCardTime();
            setState(SetState.WaitingEx);
            setInitTime(CommTime.nowSecond());
        } else {
            setStateInit();
        }
    }

    /**
     * 飘分阶段
     */
    public void doPiaoState() {
        Boolean timeOver = CommTime.nowSecond() - getInitTime() > getRoom().wanFa_PiaoFen_Time();
        if (getRoom().isWanFa_WaitingEx()) {
            //不是-1 便算操作  如果是-2 就是
            boolean allMatch = posDict.values().stream().allMatch(k -> ((MJTemplateSetPos) k).getWaitingExInfoFieldValue(curWaitingExType) != MJTemplateRoomEnum.WaitingExOpType.WAITING_OP.value);
            if (allMatch || timeOver) {
                posDict.values().forEach(k -> {
                    MJTemplateSetPos setPos = (MJTemplateSetPos) k;
                    if (setPos.getWaitingExInfoFieldValue(curWaitingExType) == -1) {
                        setPos.setWaitingExInfoFieldValue(curWaitingExType, 0);
                    }
                });
                nextWaitingEx();
            }
        } else if (getRoom().isWanFa_PiaoFen()) {
            boolean allMatch = posDict.values().stream().allMatch(k -> ((MJTemplateSetPos) k).getPiaoFen() >= 0);
            if (allMatch || timeOver) {
                posDict.values().stream().filter(k -> ((MJTemplateSetPos) k).getPiaoFen() == -1).findAny().ifPresent(p -> ((MJTemplateSetPos) p).setPiaoFen(0));
                setStateInit();
            }
        }

    }

    public void nextWaitingEx() {
        if (waitingExTypes.isEmpty()) {
            curWaitingExType = MJTemplateRoomEnum.WaitingExType.NOT;
            setStateInit();
        } else {
            curWaitingExType = waitingExTypes.remove(0);
            setInitTime(CommTime.nowSecond());
            setState(SetState.WaitingEx);
        }
    }

    /**
     * 设置为init状态
     */
    public void setStateInit() {
        setState(SetState.Init);
        setInitTime(0);
        // 初始化玩家手上的牌
        if (getGodInfo().isGodCardMode()) {
            // 神牌模式下：（只允许内测时开启）
            this.initGodPosCard();
        } else {
            // 正常模式下：上线模式
            // 初始玩家身上的牌
            this.initPosCard();
        }
        // 通知本局开始
        this.notify2SetStart();
        // 一些基本数据初始，无需理会。
        exeStartSet();
    }

    /**
     * 获取飘分列表
     */
    public ArrayList<Integer> getPiaoFenList() {
        if (getRoom().isWanFa_PiaoFen()) {
            ArrayList<Integer> piaofenList = new ArrayList<>();
            piaofenList.addAll(this.posDict.values().stream().map(setPos -> ((MJTemplateSetPos) setPos).getPiaoFen()).collect(Collectors.toList()));
            return piaofenList;
        }
        return null;
    }

    /**
     * 获取飘分集合
     * key: 玩家id
     * value：飘分值
     */
    public Map<Long, Integer> getPiaoFenMap() {
        Map<Long, Integer> piaofenMap = new HashMap<>();
        posDict.values().forEach(setPos -> {
            piaofenMap.put(setPos.getPid(), ((MJTemplateSetPos) setPos).getPiaoFen());
        });
        return piaofenMap;
    }

    public Map<Long, MJTemplateWaitingExInfo> getPiaoFenInfoMap() {
        Map<Long, MJTemplateWaitingExInfo> piaofenMap = new HashMap<>();
        posDict.values().forEach(setPos -> {
            piaofenMap.put(setPos.getPid(), ((MJTemplateSetPos) setPos).getWaitingExInfo());
        });
        return piaofenMap;
    }

    public List<MJTemplateWaitingExInfo> getPiaoFenInfoList() {
        return posDict.values().stream().map(setPos -> ((MJTemplateSetPos) setPos).getWaitingExInfo()).collect(Collectors.toList());
    }

    /**
     * 本局结算， 计算下一局的坐庄信息
     */
    @Override
    public int calcNextDPos() {
        if (this.getMHuInfo().isHuNotEmpty()) {
            //一炮多响
            if (getMHuInfo().getHuPosList().size() > 1) {
                return calcYPDXNextDPos();
            } else {
                //单人胡轮庄
                return calcHuCardNextDPos();
            }
        } else {
            // 流局轮庄
            return calcLiuJuNextDPos();
        }
    }

    /**
     * 做庄规则4：流局时，庄家连庄；
     * 做庄规则5：流局时，庄家下家做庄；
     * 做庄规则6：流局时，摸最后一张牌的玩家做庄；
     * 注：4、5、6只能选择一个；
     *
     * @return
     */
    protected int calcLiuJuNextDPos() {
        //做庄规则4：流局时，庄家连庄；
        if (MJTemplateRoomEnum.LiuJuLunZhuang.ZHUANG_LIAN_ZHUANG.equals(getRoom().wanFa_liuJuLunZhuang())) {
            getRoom().setEvenDpos(getRoom().getEvenDpos() + 1);
            return dPos;
        }
        //做庄规则5：流局时，庄家下家做庄；
        if (MJTemplateRoomEnum.LiuJuLunZhuang.XIA_JIA_ZHUANG.equals(getRoom().wanFa_liuJuLunZhuang())) {
            getRoom().setEvenDpos(1);
            return (dPos + 1) % getPlayerNum();
        }
        //做庄规则6：流局时，摸最后一张牌的玩家做庄；
        //海底捞月最后每人摸一张，摸完后还轮到上轮的打出牌的那家摸牌
        if (MJTemplateRoomEnum.OutLastCard.YES.equals(getRoom().wanFa_OutLastCard())
                || MJTemplateRoomEnum.HaiDiLaoYue.EACH_DEAL_CARD.equals(getRoom().wanFa_HaiDiLaoYue())) {
            getRoom().setEvenDpos(getLastOpInfo().getLastPopPos() == dPos ? getRoom().getEvenDpos() + 1 : 1);
            return getLastOpInfo().getLastOpPos();
        }
        int nextPos = (getLastOpInfo().getLastOpPos() + 1) % getPlayerNum();
        getRoom().setEvenDpos(nextPos == dPos ? getRoom().getEvenDpos() + 1 : 1);
        return nextPos;
    }

    /**
     * 选庄家：坐庄家规则；
     * * 做庄规则1：不管谁胡牌或是否流局，固定下家做庄，下局庄家为本局庄家的下家；
     * * 做庄规则2：有玩家胡牌时，胡牌玩家做庄，即本局谁胡牌则下局庄家为谁，如果庄家胡牌则连庄；
     * * 做庄规则3：庄家胡，庄家连庄，其他玩家胡，下家坐庄
     * * 注：2与3只能选择一个；
     */
    protected int calcHuCardNextDPos() {
        //做庄规则1：不管谁胡牌或是否流局，固定下家做庄，下局庄家为本局庄家的下家；
        if (MJTemplateRoomEnum.HuCardLunZhuang.XIA_JIA_ZHUANG.equals(getRoom().wanFa_huCardLunZhuang())) {
            getRoom().setEvenDpos(1);
            return (dPos + 1) % getPlayerNum();
        }
        //做庄规则2：有玩家胡牌时，胡牌玩家做庄，即本局谁胡牌则下局庄家为谁，如果庄家胡牌则连庄；
        if (MJTemplateRoomEnum.HuCardLunZhuang.HU_JIA_ZHUANG.equals(getRoom().wanFa_huCardLunZhuang())) {
            //庄家胡牌则连庄；
            if (getMHuInfo().getHuPos() == dPos) {
                getRoom().setEvenDpos(getRoom().getEvenDpos() + 1);
                return dPos;
            }
            //固定下家做庄，
            getRoom().setEvenDpos(1);
            return getMHuInfo().getHuPos();
        }
        //做庄规则3：庄家胡，庄家连庄，其他玩家胡，下家坐庄
        //庄家胡牌则连庄；
        if (getMHuInfo().getHuPos() == dPos) {
            getRoom().setEvenDpos(getRoom().getEvenDpos() + 1);
            return dPos;
        }
        //固定下家做庄，
        getRoom().setEvenDpos(1);
        return (dPos + 1) % getPlayerNum();

    }

    /**
     * 一炮多响的轮庄
     * 做庄规则7：一炮多响时，点炮玩家做庄；
     * 做庄规则8：一炮多响时，庄家下家做庄；
     * 做庄规则9：一炮多响时，离点炮玩家近的胡牌玩家做庄；
     * 注：规则789只能选择一个；
     * 做庄规则10：一炮多响时，如果庄家有胡牌，则庄家连庄；
     * 注：如果同时选择了规则10和规则8、9中的一个，则规则10的优先级高；
     * 注：规则10与7不可同时选择；
     *
     * @return
     */
    public int calcYPDXNextDPos() {
        if (getRoom().isWanFa_XueZhanMoShi() || getRoom().isWanFa_XueLiuMoShi()) {
            return calcXueZhanYPDXNextDPos();
        }
        int nextDPos;
        if (MJTemplateRoomEnum.YPDXLunZhuang_ZhuangHu.YES.equals(getRoom().wanFa_YPDXLunZhuang_ZhuangHu())) {
            //做庄规则10：一炮多响时，如果庄家有胡牌，则庄家连庄；
            getRoom().setEvenDpos(getRoom().getEvenDpos() + 1);
            return dPos;
        }
        if (MJTemplateRoomEnum.YPDXLunZhuang.DIAN_PAO_ZHUANG.equals(getRoom().wanFa_YPDXLunZhuang())) {
            //一炮多响时，点炮玩家做庄；
            nextDPos = getLastOpInfo().getLastOpPos();
            getRoom().setEvenDpos(nextDPos == dPos ? getRoom().getEvenDpos() + 1 : 1);
            return nextDPos;
        } else if (MJTemplateRoomEnum.YPDXLunZhuang.XIA_JIA_ZHUANG.equals(getRoom().wanFa_YPDXLunZhuang())) {
            //一炮多响时，庄家下家做庄；
            getRoom().setEvenDpos(1);
            return (dPos + 1) % getPlayerNum();
        }
        //一炮多响时，离点炮玩家近的胡牌玩家做庄；
        int lastOpPos = getLastOpInfo().getLastOpPos();
        for (int i = 1; i < getPlayerNum(); i++) {
            nextDPos = (lastOpPos + i) % getPlayerNum();
            if (getMHuInfo().getHuPosList().contains(nextDPos)) {
                getRoom().setEvenDpos(nextDPos == dPos ? getRoom().getEvenDpos() + 1 : 1);
                return nextDPos;
            }
        }
        return dPos;
    }

    /**
     * 血战：有人胡牌，下局由第一个胡牌的玩家为庄；
     * 如果第一个胡牌的情况是一炮多响，则下局由点炮玩家为庄。
     *
     * @return
     */
    public int calcXueZhanYPDXNextDPos() {
        int nextDPos = dPos;
        if (getMHuInfo().getHuPosList().size() > 0) {
            List<AbsMJSetPos> firstHuList = posDict.values().stream().filter(k -> ((MJTemplate_XueZhanSetPos) k).getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.FIRST)).collect(Collectors.toList());
            //如果第一个胡牌的情况是一炮多响，则下局由点炮玩家为庄。
            if (firstHuList.size() > 0) {
                MJTemplate_XueZhanSetPos xueZhanSetPos = (MJTemplate_XueZhanSetPos) firstHuList.get(0);
                List<MJTemplateHuInfo> huInfos = xueZhanSetPos.getHuInfos();
                //一炮多响
                if (huInfos.size() > 0 && firstHuList.size() > 1) {
                    nextDPos = huInfos.get(0).getPosID();
                } else {
                    nextDPos = xueZhanSetPos.getPosID();
                }
            }
        }
        if (nextDPos == getDPos()) {
            getRoom().setEvenDpos(1 + getRoom().getEvenDpos());
        } else {
            getRoom().setEvenDpos(1);
        }
        return nextDPos;
    }

    /**
     * 摸牌
     *
     * @param opPos
     * @param isNormalMo
     * @return
     */
    @Override
    public MJCard getCard(int opPos, boolean isNormalMo) {
        // 设置当前的摸牌用户
        MJCard card = super.getCard(opPos, isNormalMo);
        MJTemplateSetPos setPos = (MJTemplateSetPos) this.posDict.get(opPos);
        // 通知房间内的所有玩家，指定玩家摸牌了。
        if (card == null) {
            return null;
        }
        while (getRoom().getBuHuaTypeSet().contains(card.type)) {
            MJFactory.getOpCard(MJTemplateBuHuaImpl.class).checkOpCard(setPos, FlowerEnum.HAND_CARD.ordinal());
            card = setPos.getHandCard();
            if (card == null) {
                return null;
            }
        }
        getLastOpInfo().setLastPopPos(opPos);
        return setPos.getHandCard();
    }


    /**
     * 本局玩家操作管理
     */
    @Override
    protected AbsMJSetPosMgr absMJSetPosMgr() {
        return new MJTemplateSetPosMgr(this);
    }

    /**
     * 计算当局每个pos位置的分数。
     */
    @Override
    protected void calcCurSetPosPoint() {
        // 计算圈
        this.calcCurSetQuan();
        // 计算位置小局分数
        this.getPosDict().values().forEach(k -> k.calcPosPoint());
    }

    /**
     * 计算圈
     */
    public void calcCurSetQuan() {
    }


    /**
     * 清空数据
     */
    @Override
    public void clear() {
        super.clear();
    }

    /**
     * 清空BO数据
     */
    @Override
    public void clearBo() {
        super.clearBo();
    }

    /**
     * 创建新的当局麻将信息
     */
    @Override
    protected MJTemplateRoomSetInfo newMJRoomSetInfo() {
        return new MJTemplateRoomSetInfo();
    }


    /**
     * 获取通知当局信息
     */
    @Override
    public MJTemplateRoomSetInfo getNotify_set(long pid) {
        MJTemplateRoomSetInfo ret = (MJTemplateRoomSetInfo) this.getMJRoomSetInfo(pid);
        // 金
        ret.setJin1(kaiJinNum() >= 1 ? this.getmJinCardInfo().getJin(1).getCardID() : null);
        ret.setJin2(kaiJinNum() >= 2 ? this.getmJinCardInfo().getJin(2).getCardID() : null);
        //第三个金
        ret.setJinJin(kaiJinNum() == 3 ? this.getmJinCardInfo().getJin(3).getCardID() : null);
        if (benJin > 0) {
            ret.setBenJin(benJin);
        }
        if (getRoom().isWanFa_PiaoFen()) {
            ret.setPiaoFenList(new ArrayList<>());
            ret.getPiaoFenList().addAll(getPiaoFenList());
        }
        if (getRoom().isWanFa_WaitingEx()) {
            ret.setWaitingExType(curWaitingExType.name().toLowerCase(Locale.ROOT));
            ret.setBiaoShiList(getPiaoFenInfoList());
        }
        return ret;
    }

    /**
     * 一局结束的信息
     */
    @Override
    public MJTemplateRoomSetEnd getNotify_setEnd() {
        MJTemplateRoomSetEnd setEndInfo = (MJTemplateRoomSetEnd) this.mRoomSetEnd();
        // 金
        setEndInfo.setJin1(kaiJinNum() >= 1 ? this.getmJinCardInfo().getJin(1).getCardID() : null);
        setEndInfo.setJin2(kaiJinNum() >= 2 ? this.getmJinCardInfo().getJin(2).getCardID() : null);
        //第三个金
        setEndInfo.setJinJin(kaiJinNum() == 3 ? this.getmJinCardInfo().getJin(3).getCardID() : null);
        if (benJin > 0) {
            setEndInfo.setBenJin(benJin);
        }
        //客户端买马情况 问客户端给值  默认第一种
        //一人买马
        //this.zhongMaList = setEnd["maList"];
        // this.zhongList = setEnd["zhongList"]
        //多人买马
        //this.zhongMaList = posResultInfo["maList"];
        //        this.zhongList = posResultInfo["zhongList"];
        //买马继承
        //this.zhongMaList = setEnd["maList"];
        //        this.posResultList = setEnd["posResultList"];
        //买马继承的 会比较不一样一点，maLlist是取setEnd下面的，zhongList
        //是取posResultList每个玩家身上的
        if (getRoom().isWanFa_MaiMa()) {
            setEndInfo.setMaList(maList);
            setEndInfo.setZhongList(zhongList);
            setEndInfo.setZhongMa(maList.size() > 0);
        }
        return setEndInfo;
    }

    /**
     * 麻将当局结算
     *
     * @return
     */
    @Override
    protected MJTemplateRoomSetEnd newMJRoomSetEnd() {
        return new MJTemplateRoomSetEnd();
    }


    @Override
    public boolean isConfigName() {
        return true;
    }

    /**
     * 开金数
     */
    @Override
    public int kaiJinNum() {
        return getRoom().wanFa_KaiJin().kaiJinNum;
    }

    /**
     * 换牌
     */
    public void changeSanZhangCards() {
        MJTemplateRoomEnum.ChangeCardOderBy changeCardOderBy = getRoom().wanFa_ChangeCardType_OrderBy();
        if (MJTemplateRoomEnum.ChangeCardOderBy.RANDOM_1TO1.equals(changeCardOderBy)) {
            //随机出与庄家互换对位置
            int changDPos = -1;
            while (changDPos == -1 || changDPos == dPos) {
                changDPos = RandomUtils.nextInt(0, getPlayerNum());
            }
            MJTemplateSetPos dSetPos;
            MJTemplateSetPos secondSetPos;
            if (getPlayerNum() == 3) {
                int secondPos = changDPos;
                MJTemplateSetPos thirdSetPos = (MJTemplateSetPos) posDict.values().stream().filter(k -> k.getPosID() != dPos && k.getPosID() != secondPos).findFirst().get();
                dSetPos = (MJTemplateSetPos) getMJSetPos(dPos);
                secondSetPos = (MJTemplateSetPos) getMJSetPos(secondPos);
                dSetPos.getChangeCardList().forEach(k -> secondSetPos.addPrivateCard(setCard.getCardByID(k)));
                secondSetPos.getChangeCardList().forEach(k -> thirdSetPos.addPrivateCard(setCard.getCardByID(k)));
                thirdSetPos.getChangeCardList().forEach(k -> dSetPos.addPrivateCard(setCard.getCardByID(k)));
            } else {
                int secondPos = changDPos;
                dSetPos = (MJTemplateSetPos) getMJSetPos(dPos);
                secondSetPos = (MJTemplateSetPos) getMJSetPos(secondPos);
                dSetPos.getChangeCardList().forEach(k -> secondSetPos.addPrivateCard(setCard.getCardByID(k)));
                secondSetPos.getChangeCardList().forEach(k -> dSetPos.addPrivateCard(setCard.getCardByID(k)));
                if (getPlayerNum() == 4) {
                    List<AbsMJSetPos> setPosList = posDict.values().stream().filter(k -> k.getPosID() != dPos && k.getPosID() != secondPos).collect(Collectors.toList());
                    ((MJTemplateSetPos) setPosList.get(0)).getChangeCardList().forEach(k -> setPosList.get(1).addPrivateCard(setCard.getCardByID(k)));
                    ((MJTemplateSetPos) setPosList.get(1)).getChangeCardList().forEach(k -> setPosList.get(0).addPrivateCard(setCard.getCardByID(k)));
                }
            }
            if (Objects.nonNull(dSetPos) && Objects.isNull(dSetPos.getHandCard())) {
                MJCard remove = dSetPos.getPrivateCard().remove(dSetPos.sizePrivateCard() - 1);
                dSetPos.setHandCard(remove);
            }
        } else {
            MJTemplateSetPos setPos;
            int changOrder = 1;
            if (MJTemplateRoomEnum.ChangeCardOderBy.NI_SHI_ZHEN.equals(changeCardOderBy)) {
                changOrder = -1;
                //只有四个人才有对家换
            } else if (MJTemplateRoomEnum.ChangeCardOderBy.DUI_JIA.equals(changeCardOderBy) && getPlayerNum() == 4) {
                changOrder = 2;
            }
            MJTemplateSetCard setCard = getSetCard();
            for (AbsMJSetPos abSetPos : posDict.values()) {
                setPos = (MJTemplateSetPos) abSetPos;
                MJTemplateSetPos setPos1 = (MJTemplateSetPos) getMJSetPos((abSetPos.getPosID() - changOrder + getPlayerNum()) % getPlayerNum());
                setPos.getChangeCardList().forEach(k -> setPos1.addPrivateCard(setCard.getCardByID(k)));
                setPos1.sortCards();
                //如果是庄家 并且手牌被拿去换了 需要补张回去
                if (setPos1.getPosID() == dPos && Objects.isNull(setPos1.getHandCard())) {
                    MJCard remove = setPos1.getPrivateCard().remove(setPos1.sizePrivateCard() - 1);
                    setPos1.setHandCard(remove);
                }
                setPos1.getFirstChangeCardList().clear();
                setPos1.getFirstChangeCardList().addAll(setPos.getChangeCardList());
                setPos.getOpCardList().clear();
                setPos.getChangeCardList().clear();
            }
        }
        posDict.values().forEach(k -> {
            if (Objects.isNull(k.getHandCard())) {
                k.calcHuFan();
            }
        });
        sendSetPosCard();
    }

    /**
     * 计算接下来第几个胡
     */
    public void calcNextHuCardType() {
        //如果是没有 则没有这个玩法 不用计算
        if (nextHuEndType.equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
            return;
        }
        int huCount = Math.toIntExact(posDict.values().stream().filter(k -> !k.getHuType().equals(HuType.NotHu) &&
                !k.getHuType().equals(HuType.DianPao)).count());
        this.nextHuEndType = MJTemplateRoomEnum.HuCardEndType.value2Of(huCount + 1);
    }

    public MJTemplateSetCard getSetCard() {
        return (MJTemplateSetCard) super.getSetCard();
    }

    @Override
    public MJTemplateRoom getRoom() {
        return (MJTemplateRoom) super.getRoom();
    }

    /**
     * 跟庄
     *
     * @param type
     * @param opPos
     */
    public void checkGenZhuang(int type, int opPos) {
        if (getRoom().wanFa_GenZhuangPLayerNum() > getPlayerNum() || getRoom().wanFa_GenZhuangPLayerNum() == 0) {
            return;
        }
        if (getGenZhuang().checkGenZhuang(type, opPos)) {
            this.room.getRoomPosMgr().notify2All(genZhuang(getRoom().getRoomID(), genZhuang.count));
        }
    }

    /**
     * 实时算分信息
     *
     * @param kouFenPos
     * @param point
     * @param losePos
     * @return
     */
    public List<? extends PlayerPosInfo> getPlayerInfoList(int kouFenPos, int point, int losePos) {
        List<PlayerPosInfo> playerPosInfoList = this.room.getRoomPosMgr().getPlayerPosInfoList();
        List<MJTemplatePlayerPosInfo> playerPosInfos = new ArrayList<>();
        for (PlayerPosInfo playerPosInfo : playerPosInfoList) {
            MJTemplateSetPos mSetPos = (MJTemplateSetPos) getMJSetPos(playerPosInfo.getPosID());
            if (Objects.isNull(mSetPos)) {
                continue;
            }
            MJTemplatePlayerPosInfo playerPosInfo1 = new MJTemplatePlayerPosInfo(playerPosInfo, mSetPos.getCurActualTimePoint());
            playerPosInfos.add(playerPosInfo1);
            //实时清零
            mSetPos.setCurActualTimePoint(0);
        }
        return playerPosInfos;
    }

    /**
     * 设置解散次数
     */
    public void initDissolveCount() {
    }

    /**
     * 获取麻将牌
     *
     * @return
     */
    public MJTemplateSetCard getMJSetCard() {
        return (MJTemplateSetCard) this.setCard;
    }

    protected abstract <T> BaseSendMsg genZhuang(long roomID, int count);

    public void sendSetPosQingQue() {
        this.isDingQueFinish = true;
        sendSetPosCard();
    }

    /**
     * 通知刷新出牌列表 比如被炮胡的那张牌
     *
     * @param dianPaoPos
     */
    public void notifyRefreshOutCard(MJTemplateSetPos dianPaoPos) {
        if (getRoom().isWanFa_XueZhanMoShi() || getRoom().isWanFa_XueLiuMoShi()) {
            //默认为 摸牌消息 会去刷新玩家出牌列表
            notifyGetCard(dianPaoPos);
        }
    }
}
