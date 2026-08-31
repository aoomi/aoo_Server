package business.global.mj.scjymj;

import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJOpPoint;
import business.global.mj.util.HuDuiUtil;
import business.global.mj.util.HuUtil;
import business.global.room.mj.MJRoomPos;
import business.scjymj.c2s.cclass.SCJYMJResults;
import business.scjymj.c2s.cclass.SCJYMJSet_Pos;
import cenum.mj.HuType;
import cenum.mj.MJCardCfg;
import cenum.mj.MJHuOpType;
import cenum.mj.OpType;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommMath;
import jsproto.c2s.cclass.mj.BaseMJRoom_PosEnd;
import jsproto.c2s.cclass.room.AbsBaseResults;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 安岳麻将 每一局每个位置信息
 *
 * @author Huaxing
 */

public class SCJYMJSetPos extends AbsMJSetPos {
    @SuppressWarnings("rawtypes")
    private SCJYMJRoom room;
    /**
     * 爆听
     */
    private boolean isTing = false;
    private List<Integer> gangList = new ArrayList<>();
    // 杠牌类型
    private int gangCardId = 0;
    // 接杠列表
    private List<Integer> jieGangList = new ArrayList<>();

    private boolean canHu;

    @SuppressWarnings("rawtypes")
    public SCJYMJSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set) {
        super(posID, roomPos, set, SCJYMJTingImpl.class);
        this.room = (SCJYMJRoom) this.getRoom();
        this.setMSetOp(new SCJYMJSetOp(this));
        this.setCalcPosEnd(new SCJYMJCalcPosEnd(this));
        this.canHu = true;
    }

    public boolean isCanHu() {
        return canHu;
    }

    public void setCanHu(boolean canHu) {
        this.canHu = canHu;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public SCJYMJSet_Pos getNotify(boolean isSelf) {
        if (isRevealCard()) {
            isSelf = true;
        }
        SCJYMJSet_Pos ret = new SCJYMJSet_Pos();
        ret.setPosID(this.getPosID());
        // 是自己
        int length = sizePrivateCard();
        for (int i = 0; i < length; i++) {
            ret.getShouCard().add(isSelf ? getPCard(i).cardID : 0);
        }
        ret.setHuCard(isSelf ? this.getHuCardTypes() : null);
        if (this.getHandCard() != null) {
            if (!OpType.Not.equals(this.getHuOpType())) {
                ret.setHandCard(this.getHandCard().cardID);
            } else {
                ret.setHandCard(isSelf ? this.getHandCard().cardID : 5000);
            }
        }
        ret.setOutCard(new ArrayList<>(this.getOutCardIDs())); // 打出的牌，这里不过虑被人接收的
        ret.setPublicCardList(this.publicCardList(isSelf, this.getPublicCardList()));
        ret.setPiao(((SCJYMJRoomPos) this.getRoomPos()).getaPiao().value());
        ret.setTing(this.isTing);
        ret.addHuMap(this.getHuOpType(), this.getHuCount());
        return ret;
    }

    private List<List<Integer>> publicCardList(boolean isSelf, List<List<Integer>> publicCardList) {
        if (isSelf) {
            return publicCardList;
        }
        List<List<Integer>> publicCards = new ArrayList<>();
        List<Integer> list = null;
        for (int i = 0, sizeI = publicCardList.size(); i < sizeI; i++) {
            list = publicCardList.get(i);
            publicCards.add(list);
        }
        return publicCards;
    }


    @Override
    public void calcPosPoint() {
        ((SCJYMJCalcPosEnd) this.getCalcPosEnd()).calcPosPoint();
    }

    /**
     * 结算分数
     */
    public void setPosEnd() {
        ((SCJYMJCalcPosEnd) this.getCalcPosEnd()).setPosEnd();
    }

    /**
     * 设置胡牌类型
     *
     * @param huType 胡牌类型
     * @param huPos  胡Pos
     */
    public void setHuCardType(HuType huType, int huPos, int roundId) {
        this.setHuType(huType);
        this.getSet().getMHuInfo().setHuPos(huPos);
    }

    @Override
    public boolean doOpType(int cardID, OpType opType) {
        if (getmSetOp().doOpType(cardID, opType)) {
            if (OpType.KouTing.equals(opType)) {
                this.setTing(true);
                ((SCJYMJCalcPosEnd) this.getCalcPosEnd()).calcOpPointType(SCJYMJOpPoint.BaoTing, 0);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean checkOpType(int cardID, OpType opType) {
        if (getmSetOp().checkOpType(cardID, opType)) {
            if (OpType.JieGang.equals(opType)) {
                // 记录接杠，	有直杠不杠，后续能杠（即能先碰后杠），但不收雨钱（不算杠分）；
                this.addJieGang(cardID);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<OpType> recieveOpTypes() {
        // 清空记录数据
        this.clearOutCard();
        this.getPosOpNotice().clearBuNengChuList();
        OpType opType = OpType.Not;
        List<OpType> opTypes = new ArrayList<OpType>();
        if (checkOpType(0, OpType.Hu)) {
            opType = OpType.Hu;
        }
        if (!OpType.Not.equals(opType)) {
            this.setmHuOpType(MJHuOpType.ZiMo);
            int opSize = this.isOpSize();
            // 检查天地胡
            this.checkTianDiHu();
            if (opSize > 0) {
                // 	杠上开花：杠之后补的牌正好胡牌；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.GSKH);
            }
            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.HaiDiLao) && ((SCJYMJRoomSet) this.getSet()).isLastCard()) {
                // 	海底捞：最后一张牌自摸；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.HDLY);
            }
            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.JinGouDiao) && this.jinGouDiao()) {
                // 	金钩钓：碰牌，杠牌后，手上只剩一张牌；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.JinGouDiao);
            }
            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.DuZhang) && duZhang(this.getHandCard().cardID, true)) {
                // 	独张：牌桌上已经出现了同一门花色的3张一样的牌（手上的牌不算），最后剩下的第四张牌就是独张；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.DuZhang);
            }
            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.BanBanGao) && banBanGao(0)) {
                // 	板板高：比如：同一门花色的778899 等连续3对牌，共计6张牌叫做板板高；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.BanBanGao);
            }

            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.JiaXinWu) && jiaXinWu(this.getHandCard().type)) {
                // 	夹心五：同一门花色4和6，下叫，胡牌只有夹心5。只能胡5的夹心牌就叫夹心五（小七对和龙七对除外）；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.JiaXinWu);
            }
            getPosOpRecord().setHuCardType(1000);
            opTypes.add(OpType.Hu);
        }
        if (((SCJYMJRoomSet) this.getSet()).isLastFourCard() && !OpType.Not.equals(opType)) {
            // 最后4张牌，能胡，只显示胡
            return opTypes;
        }
        boolean isLastCard = ((SCJYMJRoomSet) this.getSet()).isLastCard();
        if (checkOpType(0, OpType.Ting)) {
            opTypes.add(OpType.Ting);
        }
        if ((getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.HaiDiGang) || !isLastCard) && checkOpType(0, OpType.AnGang)) {
            opTypes.add(OpType.AnGang);
        }
        if ((getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.HaiDiGang) || !isLastCard) && checkOpType(0, OpType.Gang)) {
            opTypes.add(OpType.Gang);
        }
        opTypes.add(OpType.Out);
        return opTypes;
    }

    /**
     * 检查天胡地胡
     */
    private void checkTianDiHu() {
        if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.TianDiHu)) {
            // 检查是否天胡
            if (this.sizeOutCardIDs() <= 0 && this.getSet().getDPos() == this.getPosID()) {
                this.getPosOpRecord().clearOpHuList();
                // 天胡
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.TianHu);
            }
//            else {
//                if (this.isTing() &&this.sizeOutCardIDs() <= 0) {
//                    SCJYMJSetPos aPos = (SCJYMJSetPos) this.set.getMJSetPos(this.set.getDPos());
//                    if (aPos.sizeOutCardIDs() == 1) {
//                        this.cleanOpHuList();
//                        // 地胡
//                        this.addOpHuList(SCJYMJOpPoint.DiHu);
//                    }
//                }
//            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public BaseMJRoom_PosEnd<SCJYMJCalcPosEnd> calcPosEnd() {
        BaseMJRoom_PosEnd<SCJYMJCalcPosEnd> ret = this.posEndInfo();
        ret.setEndPoint(this.getCalcPosEnd().getCalcPosEnd());
        return ret;
    }

    @Override
    public OpType checkPingHu(int curOpPos, int cardID) {
        this.clearPaoHu();
        OpType opType = OpType.Not;
        if (checkOpType(cardID, OpType.Hu)) {
            opType = OpType.JiePao;
        }
        if (!OpType.Not.equals(opType)) {
            // 漏胡：如果玩家漏掉炮胡，则该玩家摸牌前禁止炮胡；（所有牌）,‘1000’ 只是作为标记，没有什么特殊意义。
            if (this.getPosOpRecord().isHuCardType(1000)) {
                CommLogD.info("checkPingHu isHuCardType Pid:{},RoomID:{},RoomKey:{},CardId:{},HuList:{}", getPid(), getRoom().getRoomID(), getRoom().getRoomKey(), cardID, getPosOpRecord().getOpHuList().toString());
                this.setCheckPao(false);
                return OpType.Not;
            }
            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.JinGouDiao) && this.jinGouDiao()) {
                // 	金钩钓：碰牌，杠牌后，手上只剩一张牌；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.JinGouDiao);
            }
            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.DuZhang) && duZhang(cardID, false)) {
                // 	独张：牌桌上已经出现了同一门花色的3张一样的牌（手上的牌不算），最后剩下的第四张牌就是独张；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.DuZhang);
            }
            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.BanBanGao) && banBanGao(cardID / 100)) {
                // 	板板高：比如：同一门花色的778899 等连续3对牌，共计6张牌叫做板板高；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.BanBanGao);
            }

            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.JiaXinWu) && jiaXinWu(cardID / 100)) {
                // 	夹心五：同一门花色4和6，下叫，胡牌只有夹心5。只能胡5的夹心牌就叫夹心五（小七对和龙七对除外）；，
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.JiaXinWu);
            }

            this.setCheckPao(true);
            this.getPosOpRecord().setHuCardType(1000);
            this.setmHuOpType(MJHuOpType.JiePao);
        }
        return opType;
    }


    public OpType checkDaJiao(int cardID) {
        this.clearPaoHu();
        OpType opType = OpType.Not;
        if (checkOpType(cardID, OpType.WuDangHu)) {
            opType = OpType.JiePao;
        }
        if (!OpType.Not.equals(opType)) {
            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.JinGouDiao) && this.jinGouDiao()) {
                // 	金钩钓：碰牌，杠牌后，手上只剩一张牌；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.JinGouDiao);
            }
            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.DuZhang) && duZhang(cardID, true)) {
                // 	独张：牌桌上已经出现了同一门花色的3张一样的牌（手上的牌不算），最后剩下的第四张牌就是独张；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.DuZhang);
            }
            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.BanBanGao) && banBanGao(cardID / 100)) {
                // 	板板高：比如：同一门花色的778899 等连续3对牌，共计6张牌叫做板板高；
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.BanBanGao);
            }

            if (getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.JiaXinWu) && jiaXinWu(cardID / 100)) {
                // 	夹心五：同一门花色4和6，下叫，胡牌只有夹心5。只能胡5的夹心牌就叫夹心五（小七对和龙七对除外）；，
                this.getPosOpRecord().addOpHuList(SCJYMJOpPoint.JiaXinWu);
            }
            this.setmHuOpType(MJHuOpType.JiePao);
        }
        return opType;
    }

    /**
     * 清空操作状态
     */
    public void cleanOp() {
        ((SCJYMJSetOp) this.getmSetOp()).cleanOp();
        this.getPosOpNotice().clearBuNengChuList();
    }

    /**
     * 操作
     *
     * @return
     */
    public int isOpSize() {
        return ((SCJYMJSetOp) this.getmSetOp()).isOpSize();
    }


    /**
     * 检查是否有指定动作类型
     *
     * @param opType
     * @return
     */
    public boolean isOpContains(OpType opType) {
        return ((SCJYMJSetOp) this.getmSetOp()).isOpContains(opType);
    }

    /**
     * 动作分数
     *
     * @param opPoint 动作分数
     * @param count
     */
    public void opPointType(SCJYMJOpPoint opPoint, int count) {
        ((SCJYMJCalcPosEnd) this.getCalcPosEnd()).calcOpPointType(opPoint, count);
    }

    public void removeOpPointType(SCJYMJOpPoint opPoint) {
        ((SCJYMJCalcPosEnd) this.getCalcPosEnd()).removeOpPointType(opPoint);
    }


    @Override
    public void calcResults() {
        SCJYMJResults lResults = (SCJYMJResults) this.mResultsInfo();
        lResults.setZiMoCount(((SCJYMJCalcPosEnd) this.getCalcPosEnd()).getOpPintCount(SCJYMJOpPoint.ZiMo));
        lResults.setJiePaoCount(((SCJYMJCalcPosEnd) this.getCalcPosEnd()).getOpPintCount(SCJYMJOpPoint.JiePao));
        lResults.setDiaoPaoCount(((SCJYMJCalcPosEnd) this.getCalcPosEnd()).getOpPintCount(SCJYMJOpPoint.DianPao));
        lResults.setAnGangCount(((SCJYMJCalcPosEnd) this.getCalcPosEnd()).getOpPintCount(SCJYMJOpPoint.AnGang));
        lResults.setMingGangCount(((SCJYMJCalcPosEnd) this.getCalcPosEnd()).getOpPintCount(SCJYMJOpPoint.Gang) + ((SCJYMJCalcPosEnd) this.getCalcPosEnd()).getOpPintCount(SCJYMJOpPoint.JieGang));
        lResults.setZhuangCount(this.getPosID() == this.getSet().getDPos());
        lResults.setWinPointCount(this.getEndPoint() > 0);
        lResults.setOwner(this.getPid() == getRoom().getOwnerID());
        this.setResults(lResults);
    }

    @Override
    public <T> void calcOpPointType(T opType, int count) {
        getCalcPosEnd().calcOpPointType(opType, count);
    }

    /**
     * 新一局中各位置的信息
     *
     * @return
     */
    @Override
    protected SCJYMJSet_Pos newMJSetPos() {
        return new SCJYMJSet_Pos();
    }

    @Override
    public SCJYMJSetPosRobot getSetPosRobot() {
        return new SCJYMJSetPosRobot(this);
    }

    @Override
    protected AbsBaseResults newResults() {
        return new SCJYMJResults();
    }

    /**
     * 检查报听
     *
     * @param key
     * @param allCardList
     * @return
     */
    public boolean checkBaotingGang(Integer key, List<Integer> allCardList) {
        List<Integer> allCardInts = new ArrayList<>();
        allCardInts.addAll(allCardList);

        Iterator<Integer> iter = allCardInts.iterator();
        while (iter.hasNext()) {
            Integer b = iter.next();
            if (b.equals(key)) {
                iter.remove();
            }
        }
        SCJYMJTingImpl tImpl = (SCJYMJTingImpl) MJFactory.getTingCard(SCJYMJTingImpl.class);
        if (tImpl.checkTingCardList(new MJCardInit(allCardInts, 0))) {
            // 添加可以杠的Key
            this.gangList.add(key);
            return true;
        }
        return false;
    }

    public boolean isTing() {
        return isTing;
    }

    public void setTing(boolean isTing) {
        this.isTing = isTing;
    }

    public int getGangCardId() {
        return this.gangCardId;
    }

    public void setGangCardId(int gangCardId) {
        this.gangCardId = gangCardId;
    }

    public boolean isHu() {
        return !OpType.Not.equals(this.getHuOpType());
    }

    /**
     * 查大叫
     *
     * @return
     */
    public boolean chaDaJiao() {
        List<Integer> huCardTypeList = this.getHuCardTypes();
        if (huCardTypeList.size() <= 0) {
            return false;
        }
        Map<Integer, List<SCJYMJOpPoint>> map = new HashMap<>();
        int cardId = 0;
        List<SCJYMJOpPoint> oEnums = null;
        for (Integer cardType : huCardTypeList) {
            cardId = cardType * 100;
            if (checkTingSi(cardType) || OpType.Not.equals(checkDaJiao(cardId))) {
                continue;
            }
            // 计算根
            this.calcDaJiaoGen(cardType);
            // 获取胡牌列表
            oEnums = getPosOpRecord().getOpHuList().stream().map(k -> (SCJYMJOpPoint) k).collect(Collectors.toList());
            // 求总番数
            map.put(SCJYMJCalcPosEnd.calcDaJiaoPoint(oEnums), oEnums);
        }
        if (null == map || map.size() <= 0) {
            return false;
        }
        this.getPosOpRecord().clearOpHuList();
        int key;
        if (SCJYMJRoomEnum.SCJYMJChajiao.valueOf(room.cfg.chajiao) == SCJYMJRoomEnum.SCJYMJChajiao.Chaxiao) {
            key = map.keySet().stream().min(Integer::compareTo).orElse(0);
        } else {
            key = map.keySet().stream().max(Integer::compareTo).orElse(0);
        }
        if (map.containsKey(key)) {
            this.getPosOpRecord().getOpHuList().addAll(map.get(key));
        }
        return true;
    }

    /**
     * 查大叫，计算根牌
     *
     * @param cardType
     */
    private void calcDaJiaoGen(int cardType) {
        // 检查指定牌型手上牌相同数量
        boolean isGen = (int) this.allCards().stream().filter(k -> k.getType() == cardType).count() == 3;
        if (!isGen) {
            // 手上牌型没有，检查碰、爆杠牌型
            isGen = this.getPublicCardList().stream().filter(k -> k.get(0) == OpType.Peng.value()).map(k -> k.get(2) / 100).filter(k -> k == cardType).findAny().isPresent();
        }
        // 统计手上的牌数
        if (isGen) {
            this.getPosOpRecord().getOpHuList().add(SCJYMJOpPoint.Gen);
        }
    }

    /**
     * 获取飘
     *
     * @return
     */
    public SCJYMJRoomEnum.SCJYMJPiao getPiao() {
        return ((SCJYMJRoomPos) this.getRoomPos()).getaPiao();
    }

    /**
     * 添加接杠列表
     * 	有直杠不杠，后续能杠（即能先碰后杠），但不收雨钱（不算杠分）；
     *
     * @param cardId 牌ID
     */
    public void addJieGang(int cardId) {
        this.jieGangList.add(cardId / 100);
    }

    /**
     * 获取接杠列表
     *
     * @return
     */
    public List<Integer> getGangList() {
        return gangList;
    }

    /**
     * 听数
     *
     * @return
     */
    public int getTingCount() {
        return this.isTing ? 1 : 0;
    }

    /**
     * 	独张：牌桌上已经出现了同一门花色的3张一样的牌（手上的牌不算），最后剩下的第四张牌就是独张；
     *
     * @return
     */
    private boolean duZhang(int cardId, boolean isZiMo) {
        int cardType = cardId / 100;
        long count = 0;
        long size = this.getPrivateCard().stream().filter(k -> k.type == cardType).count();
        if (size > 0L) {
            return false;
        }
        count += this.getSet().getPosDict().values().stream().flatMap(k -> k.getOutCardIDs().stream()).filter(k -> k / 100 == cardType).count();
        count += this.getSet().getPosDict().values().stream().filter(k -> ((SCJYMJSetPos) k).isHu() && null != k.getHandCard() && k.getHandCard().getType() == cardType).count();
        return isZiMo ? count == 3L : count == 4L;
    }

    /**
     * 	板板高：比如：同一门花色的778899 等连续3对牌，共计6张牌叫做板板高；
     *
     * @param cardType 牌类型
     * @return
     */
    private boolean banBanGao(int cardType) {
        if (this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.DDHu) || this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.QYSDDHu)) {
            // 对对胡和板板高不能同时存在
            return false;
        }

        MJCardInit mInit = this.mCardInit(cardType, false);
        // 按牌类型分组
        List<Integer> allCardInit = new ArrayList<>();
        allCardInit.addAll(mInit.getAllCardInts());
        Map<Integer, Long> map = allCardInit.stream().map(k -> k >= 1000 ? k / 100 : k).collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        List<Integer> keyList = new ArrayList<>();
        for (Map.Entry<Integer, Long> value : map.entrySet()) {
            // 相同牌数 >= 2记录下来
            if (value.getValue() >= 2L) {
                keyList.add(value.getKey());
            }
        }
        if (keyList.size() < 3) {
            // 	板板高：比如：同一门花色的778899 等连续3对牌，共计6张牌叫做板板高；
            return false;
        }
        // 牌需从小到大
        keyList = keyList.stream().sorted().collect(Collectors.toList());
        List<Integer> huBanBanGao = null;
        List<Integer> removeList = null;
        for (int i = 0, size = keyList.size(); i < size; i++) {
            int count = i + 3;
            if (count > size) {
                break;
            }
            List<Integer> subList = keyList.subList(i, count);
            // 截取3张牌检查是否顺子
            if (CommMath.isContinuous(subList)) {
                huBanBanGao = new ArrayList();
                huBanBanGao.addAll(allCardInit);
                removeList = new ArrayList<>();
                removeList.addAll(subList);
                removeList.addAll(subList);
                if (checkBanBanGao(huBanBanGao, removeList)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 检查板板高
     *
     * @param allCards
     * @param cardLong
     * @return
     */
    public boolean checkBanBanGao(List<Integer> allCards, List<Integer> cardLong) {
        List<Integer> aCards = new ArrayList<>(allCards);
        Integer mCard = null;
        Iterator<Integer> it = null;
        for (Integer cardType : cardLong) {
            it = aCards.iterator(); // 创建迭代器
            while (it.hasNext()) { // 循环遍历迭代器
                mCard = it.next();
                if (mCard.equals(cardType)) {
                    it.remove();
                    break;
                }
            }
        }

        if (this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.QiDuiHu) || this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.LongQiDuiHu) || this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.QYSQiDuiHu) || this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.QYSLongQiDuiHu)) {
            if (HuDuiUtil.getInstance().checkDuiHu(aCards, 0)) {
                return true;
            }
        } else {
            if (HuUtil.getInstance().checkHu(aCards, 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 	夹心五：同一门花色4和6，下叫，胡牌只有夹心5。只能胡5的夹心牌就叫夹心五（小七对和龙七对除外）；，
     *
     * @return
     */
    private boolean jiaXinWu(int cardType) {
        if (this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.DDHu) || this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.QYSDDHu) || this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.QiDuiHu) || this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.LongQiDuiHu) || this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.QYSQiDuiHu) || this.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.QYSLongQiDuiHu)) {
            // 对对胡和板板高不能同时存在
            return false;
        }


        if (cardType % 10 != 5) {
            return false;
        }
        List<Integer> huTypes = this.getHuCardTypes();
        if (null == huTypes || huTypes.size() <= 0) {
            return false;
        }
        MJCardInit mInit = this.mCardInit(cardType, false);
        List<Integer> cardInts = mInit.getAllCardInts().stream().sorted().collect(Collectors.toList());
        int type = cardType / 10;
        if (type == MJCardCfg.WANG.value()) {
            if (cardInts.containsAll(SCJYMJRoomEnum.WA_WU)) {
                return checkJiaXinWuHu(cardInts, SCJYMJRoomEnum.WA_WU);
            }
        } else if (type == MJCardCfg.TIAO.value()) {
            if (cardInts.containsAll(SCJYMJRoomEnum.TI_WU)) {
                return checkJiaXinWuHu(cardInts, SCJYMJRoomEnum.TI_WU);

            }
        } else if (type == MJCardCfg.TONG.value()) {
            if (cardInts.containsAll(SCJYMJRoomEnum.TO_WU)) {
                return checkJiaXinWuHu(cardInts, SCJYMJRoomEnum.TO_WU);
            }
        }
        return false;
    }

    /**
     * 检查夹心五胡牌
     *
     * @param allCards
     * @param cardLong
     * @return
     */
    public boolean checkJiaXinWuHu(List<Integer> allCards, List<Integer> cardLong) {
        List<Integer> aCards = new ArrayList<>(allCards);
        Integer mCard = null;
        Iterator<Integer> it = null;
        for (Integer cardType : cardLong) {
            it = aCards.iterator(); // 创建迭代器
            while (it.hasNext()) { // 循环遍历迭代器
                mCard = it.next();
                if (mCard.equals(cardType)) {
                    it.remove();
                    break;
                }
            }
        }
        if (HuUtil.getInstance().checkHu(aCards, 0)) {
            return true;
        }
        return false;
    }

    /**
     * 检查金钩钓
     *
     * @return
     */
    public boolean jinGouDiao() {
        return this.sizePrivateCard() == 1;
    }

    /**
     * 特殊情况，如果玩家A的胡牌提示，4筒0个，7筒0个。这个情况是不参与查大叫的。玩家A无法查其他未下叫的玩家，其他下叫的玩家也无法查玩家A的叫。意思就是玩家A不参与最后的查大叫。
     * 检查是否听死牌
     *
     * @return
     */
    private boolean checkTingSi(int cardType) {
        long count = 0;
        count += this.allCards().stream().filter(k -> k.type == cardType).count();
        count += this.getSet().getPosDict().values().stream().flatMap(k -> k.getOutCardIDs().stream()).filter(k -> k / 100 == cardType).count();
        count += this.getSet().getPosDict().values().stream().filter(k -> ((SCJYMJSetPos) k).isHu() && null != k.getHandCard() && k.getHandCard().getType() == cardType).count();
        return count >= 4L;
    }

    /**
     * 手牌牌序
     */
    @Override
    public void sortCards() {
        this.setPrivateCard(this.getPrivateCards().stream().sorted(Comparator.comparing(k -> k.getID(0))).collect(Collectors.toList()));
    }
}
