package business.global.pk;

import business.global.pk.robot.PKSetPosRobot;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.player.feature.PlayerCurrency;
import cenum.PKOpType;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.room.RoomDissolutionState;
import cenum.room.RoomEndPointEnum;
import com.ddm.server.common.utils.CommMath;
import jsproto.c2s.cclass.pk.base.BasePKRoom_PosEnd;
import jsproto.c2s.cclass.pk.base.BasePKSet_Pos;
import jsproto.c2s.cclass.room.AbsBaseResults;
import jsproto.c2s.cclass.room.BaseResults;
import lombok.Data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Data
public abstract class AbsPKSetPos {

    /**
     * 位置ID
     */
    private int posID = 0;
    /**
     * 玩家房间信息
     */
    private AbsRoomPos roomPos = null;
    /**
     * 房间当局信息
     */
    private AbsPKSetRoom set = null;
    /**
     * 手牌
     */
    private ArrayList<Integer> privateCards = new ArrayList<>();
    /**
     * 用户结算分数
     */
    private int endPoint = 0;
    /**
     * 扣分
     */
    private int deductPoint = 0;
    /**
     * 动作的牌
     */
    private int opCardId = 0;
    /**
     * 玩家动作信息
     */
    private AbsPKSetOp mSetOp;
    /**
     * 玩家位置结算
     */
    private AbsPKCalcPosEnd calcPosEnd;
    /**
     * 标记用户明牌
     */
    public boolean isRevealCard = false;

    /**
     * 机器人位置操作
     */
    private PKSetPosRobot setPosRobot;

    /**
     * 一考的扣分
     */
    private double deductPointYiKao;
    /**
     * 一考的结算分
     */
    private double deductEndPoint;

    public AbsPKSetPos(int posID, AbsRoomPos roomPos, AbsPKSetRoom set) {
        this.posID = posID;
        this.roomPos = roomPos;
        this.set = set;
    }

    /**
     * 强制发牌
     *
     * @param cards 私有牌
     */
    public void forcePopCard(List<Integer> cards) {
        privateCards.addAll(cards);
    }

    /**
     * @param cards 私有牌
     */
    public void addCard(Integer cards) {
        privateCards.add(cards);
    }

    /**
     * 获取手牌的数量
     *
     * @return
     */
    public int sizePrivateCard() {
        return this.privateCards.size();
    }

    /**
     * 获取用户Pid
     *
     * @return
     */
    public long getPid() {
        if (null != this.getRoomPos()) {
            return this.getRoomPos().getPid();
        }
        return 0;
    }

    /**
     * 初始化手牌
     *
     * @param cards 私有牌
     */
    public void init(List<Integer> cards) {
        this.privateCards = new ArrayList<>(cards);
    }

    /**
     * 获取位置
     *
     * @param posID 指定
     * @return
     */
    public AbsPKSetPos getPKSetPos(int posID) {
        return this.set.getPKSetPos(posID);
    }

    /**
     * 获取玩家人数
     *
     * @return
     */
    public int getPlayerNum() {
        return this.set.getRoom().getPlayerNum();
    }

    /**
     * 获取房间信息
     *
     * @return
     */
    public AbsBaseRoom getRoom() {
        return this.set.getRoom();
    }

    /**
     * 获取指定的手牌
     *
     * @param i
     * @return
     */
    public Integer getPCard(int i) {
        if (i >= sizePrivateCard()) {
            return null;
        }
        if (i <= -1) {
            return null;
        }
        return this.privateCards.get(i);
    }


    /**
     * 新位置结算信息
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    protected BasePKRoom_PosEnd newPKSetPosEnd() {
        return new BasePKRoom_PosEnd();
    }

    /**
     * 练习场结算
     */
    public void goldEnd() {
        if (this.getSet().checkExistPrizeType(PrizeType.Gold)) {
            setEndPoint(this.getRoom().getBaseMark() * getEndPoint());
            if (!this.getRoomPos().isRobot()) {
                this.getRoomPos().getPlayer().getFeature(PlayerCurrency.class).goldRoomEnd(getEndPoint(),
                        this.getRoom().getBaseMark(), this.getRoom().getBaseRoomConfigure().getGameType().getId());
            }
        }
    }

    /**
     * 位置结算信息
     *
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected BasePKRoom_PosEnd posEndInfo() {
        BasePKRoom_PosEnd ret = this.newPKSetPosEnd();
        // 练习场结算
        this.goldEnd();
        // 位置
        ret.setPos(this.getPosID());
        // 记录用户PID
        ret.setPid(this.getPid());
        // 设置结算分数
        ret.setPoint(this.getEndPoint());
        // 设置手牌
        ret.setShouCard(this.getPrivateCards());
        // 竞技点分数
        if (this.getRoom().calcFenUseYiKao() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
            ret.setSportsPoint(this.getDeductPointYiKao());
        } else {
            ret.setSportsPoint(this.getRoomPos().setSportsPoint(this.getEndPoint()));
        }
        ret.setNickname(this.getRoomPos().getName());
        // 房间分数叠加
        this.pidPointEnd();
        // 房间总结算叠加
        this.calcResults();
        ret.setRoomPoint(this.pidSumPointEnd());
        ret.setUpLevelId(this.getRoomPos().getUpLevelId());
        return ret;
    }

    /**
     * 获取结果数据
     *
     * @return
     */
    public BaseResults getResults() {
        return roomPos.getResults();
    }

    /**
     * 新结算
     *
     * @return
     */
    protected abstract AbsBaseResults newResults();

    /**
     * 结算信息
     *
     * @return
     */
    protected BaseResults mResultsInfo() {
        BaseResults mResults = this.getResults();
        if (null == mResults) {
            // new 总结算
            mResults = this.newResults();
            // 用户PID
            mResults.setPid(this.getPid());
            // 位置
            mResults.setPosId(this.getPosID());
            // 是否房主
            mResults.setOwner(this.getPid() == this.getRoom().getOwnerID());
        }
        // 总分数
        mResults.setPoint(this.pidSumPointEnd());
        // 总竞技点分数
        mResults.setSportsPoint(this.getRoomPos().sportsPoint());
        return mResults;
    }

    public int pidSumPointEnd() {
        return this.getRoomPos().getPoint();
    }

    /**
     * 计算总结算信息
     */
    public void calcResults() {
        // 获取总结算信息
        BaseResults mResultsInfo = this.mResultsInfo();
        // 并且设置覆盖
        this.setResults(mResultsInfo);
    }

    /**
     * 设置结果数据
     *
     * @param results
     */
    public void setResults(BaseResults results) {
        if (null != results) {
            this.getRoomPos().setResults(results);
        }
    }

    /**
     * 玩家总分
     */
    public void pidPointEnd() {
        this.getRoomPos().calcRoomPoint(this.endPoint);
        if (PrizeType.RoomCard.equals(getRoom().getBaseRoomConfigure().getPrizeType())) {
            this.roomPos.addCountPoint(this.endPoint);
        }
    }

    /**
     * 清空信息
     */
    public void clear() {
        if (null != this.privateCards) {
            this.privateCards.clear();
            this.privateCards = null;
        }
        this.roomPos = null;
        this.set = null;
    }

    /**
     * 新一局中各位置的信息
     *
     * @return
     */
    protected BasePKSet_Pos newPKSetPos() {
        return new BasePKSet_Pos();
    }


    public BasePKSet_Pos getNotify(long pid) {
        return getNotify(pid == this.getPid());
    }


    public BasePKSet_Pos getPlayBackNotify() {
        BasePKSet_Pos ret = newPKSetPos();
        ret.setPid(this.getPid());
        ret.setPosID(this.getPosID());
        ret.setShouCard(new ArrayList<>(this.getPrivateCards()));
        ret.setIsLostConnect(this.getRoomPos().isLostConnect());
        ret.setTrusteeship(this.getRoomPos().isTrusteeship());
        return ret;
    }

    /**
     * 获取手牌通知信息
     *
     * @param isSelf 是否本身
     * @return
     */
    public abstract BasePKSet_Pos getNotify(boolean isSelf);

    /**
     * 检测自摸胡
     *
     * @return
     */
    public abstract List<PKOpType> receiveOpTypes();

    /**
     * 操作类型
     *
     * @param opCard
     * @param opType
     * @return
     */
    public abstract boolean doOpType(PKOpCard opCard, PKOpType opType);


    /**
     * 统计本局分数
     *
     * @return
     */
    public abstract BasePKRoom_PosEnd<?> calcPosEnd();

    public void flushUnionSportPoint() {
//        if (getRoom().getRoomTypeEnum().equals(RoomTypeEnum.UNION)) {
//            AbsRoomPos roomPos = getRoomPos();
//        }
    }


    public PKSetPosRobot newPKSetPosRobot() {
        return new PKSetPosRobot(this);
    }

    public PKSetPosRobot getSetPosRobot() {
        if (Objects.isNull(this.setPosRobot)) {
            this.setPosRobot = this.newPKSetPosRobot();
        }
        return this.setPosRobot;
    }


    /**
     * 移除手牌
     *
     * @return
     */
    public int removePrivateCard(int cardId) {
        if (cardId <= 0) {
            return 0;
        }
        // 创建迭代器
        Iterator<Integer> it = this.privateCards.iterator();
        // 循环遍历迭代器
        while (it.hasNext()) {
            if (it.next() == cardId) {
                it.remove();
                return cardId;
            }
        }
        return 0;
    }


    /**
     * 移除手牌列表
     *
     * @param tmp
     */
    public void removeAllPrivateCard(List<Integer> tmp) {
        if (null == tmp) {
            return;
        }
        Iterator<Integer> tmpIt = tmp.iterator();
        while (tmpIt.hasNext()) {
            removePrivateCard(tmpIt.next());
        }
    }

    /**
     * 计算大局分数
     */
    public void calcDaJuFenShu() {
        if (this.getRoom().checkDaJuFenShu()) {
            if (RoomDissolutionState.Dissolution.equals(getRoom().getRoomDissolutionState()) || this.getSet().getSetID() == getRoom().getBaseRoomConfigure().getBaseCreateRoom().getSetCount()) {
                //最少要赢10分
                int roomPoint = this.getRoomPos().getPoint() + this.getEndPoint();
                if (roomPoint < 0) {
                    Double addPoint;
                    if (RoomEndPointEnum.RoomEndPointEnum_Ten_Enough.equals(RoomEndPointEnum.valueOf(this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getDajusuanfen()))) {
                        addPoint = new Double(10 - Math.abs(roomPoint));
                    } else {
                        addPoint = new Double(Math.abs(roomPoint));
                    }
                    if (this.getSet().getRoom().isRulesOfCanNotBelowZero()) {
                        addPoint = this.getRoomPos().getRoomSportsPoint() >= addPoint ? addPoint : this.getRoomPos().getRoomSportsPoint();
                    }
                    this.getRoomPos().setPointYiKao(CommMath.subDouble(this.getRoomPos().getPointYiKao(), addPoint));
                    this.getResults().setSportsPoint(this.getRoomPos().sportsPoint());
                    int subPosID = (this.getPosID() + 1) % getRoom().getPlayerNum();
                    AbsPKSetPos subAbsPKSetPos = this.getPKSetPos(subPosID);
                    subAbsPKSetPos.getRoomPos().setPointYiKao(CommMath.addDouble(subAbsPKSetPos.getRoomPos().getPointYiKao(), addPoint));
                    subAbsPKSetPos.getResults().setSportsPoint(subAbsPKSetPos.getRoomPos().sportsPoint());
                }
            }
        }
    }

    public void setPidSumPointEnd(int point) {
        this.getRoomPos().calcRoomPoint(point);
    }
}
