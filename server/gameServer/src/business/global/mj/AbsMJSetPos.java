package business.global.mj;

import business.global.config.HuRewardConfigMgr;
import business.global.mj.manage.BaiBan;
import business.global.mj.manage.MJFactory;
import business.global.mj.op.BaiBanImpl;
import business.global.mj.robot.MJSetPosRobot;
import business.global.room.mj.MJRoomPos;
import business.player.Player;
import business.player.feature.PlayerCurrency;
import business.player.feature.PlayerFriendsHelpUnfoldRedPack;
import cenum.FriendsHelpUnfoldRedPackEnum.TargetType;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.mj.HuType;
import cenum.mj.MJEndType;
import cenum.mj.MJHuOpType;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Lists;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.player.FriendsHelpUnfoldRedPackEvent;
import jsproto.c2s.cclass.mj.BaseMJRoom_PosEnd;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.cclass.room.AbsBaseResults;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbsMJSetPos extends MJSetPos {

    public AbsMJSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set, Class<?> mActMrg) {
        super(posID, roomPos, set, mActMrg);
    }

    // 连庄数
    protected int lianZhuangNum = 0;

    @Override
    public void clear() {
        super.clear();
    }

    /**
     * 白板排序
     */
    @Override
    protected void sortBaiBanCards() {
        // 是否白板替金
        if (!this.getSet().isBaiBanTiJin()) {
            return;
        }
        BaiBan baiBan = this.getBaiBan();
        if (null == baiBan) {
            setBaiBan(new BaiBanImpl());
            baiBan = this.getBaiBan();
        }
        this.setPrivateCard(baiBan.baiBan(this, this.getSet().getmJinCardInfo().getJin(1).getType(), this.getSet().getmJinCardInfo().getJin(2).getType()));
    }

    /**
     * 胡法检测
     */
    @Override
    public void calcHuFan() {
        this.setHuCardTypes(MJFactory.getTingCard(this.getmActMrg()).checkTingCard(this, allCards()));
    }


    /**
     * 计算位置小局分数
     */
    public abstract void calcPosPoint();

    /**
     * 统计本局分数
     *
     * @return
     */
    public abstract BaseMJRoom_PosEnd<?> calcPosEnd();

    public BaseMJSet_Pos getNotify(int pos) {
        boolean isSelf = pos == this.getPosID();
        return getNotify(isSelf);
    }

    public BaseMJSet_Pos getNotify(long pid) {
        boolean isSelf = pid == this.getPid();
        return getNotify(isSelf);
    }

    /**
     * 获取手牌通知信息
     *
     * @param isSelf 是否本身
     * @return
     */
    public abstract BaseMJSet_Pos getNotify(boolean isSelf);

    /**
     * 新一局中各位置的信息
     *
     * @return
     */
    protected BaseMJSet_Pos newMJSetPos() {
        return new BaseMJSet_Pos();
    }

    /**
     * 获取手牌通知信息
     *
     * @param isSelf 是否本身
     * @return
     */
    protected BaseMJSet_Pos getNotifyInfo(final boolean isSelf) {
        BaseMJSet_Pos ret = this.newMJSetPos();
        // 玩家位置
        ret.setPosID(this.getPosID());
        // 手牌
        ret.setShouCard(getShouCard(isSelf));
        // 可胡的牌
        ret.setHuCard(isSelf ? this.getHuCardTypes() : null);
        if (this.getHandCard() != null) {
            // 首牌
            ret.setHandCard(isSelf ? this.getHandCard().getCardID() : 5000);
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
        //托管标志
        ret.setTrusteeship(getRoomPos().isTrusteeship());

        return ret;
    }

    /**
     * 手牌
     *
     * @param isSelf
     * @return
     */
    protected List<Integer> getShouCard(final boolean isSelf) {
        List<Integer> mCards = Lists.newArrayList();
        for (int i = 0, length = sizePrivateCard(); i < length; i++) {
            mCards.add(isSelf ? getPrivateCard().get(i).cardID : 0);
        }
        return mCards;
    }

    public BaseMJSet_Pos getPlayBackNotify() {
        BaseMJSet_Pos ret = this.newMJSetPos();
        ret.setPosID(this.getPosID());
        // 是自己
        ret.setShouCard(getShouCard(true));
        // 可胡牌的 type 和番数；私人独享
        ret.setHuCard(this.getHuCardTypes());
        // 首牌
        ret.setHandCard(this.getHandCard() != null ? this.getHandCard().getCardID() : -1);
        // 打牌列表
        ret.setOutCard(this.getOutCardIDs());
        // 公共牌列表
        ret.setPublicCardStrs(this.getPublicCardList().toString());
        // 掉线连接
        ret.setIsLostConnect(this.getRoomPos().isLostConnect());
        //托管标识
        ret.setTrusteeship(this.getRoomPos().isTrusteeship());
        //竞技点分数
        ret.setSportsPoint(getRoomPos().getRoomSportsPoint());
        //分数
        ret.setPoint(getRoomPos().getPoint());
        return ret;
    }

    /**
     * 操作类型
     *
     * @param cardID
     * @param opType
     * @return
     */
    public abstract boolean doOpType(int cardID, OpType opType);

    /**
     * 检测类型
     *
     * @param cardID
     * @param opType
     * @return
     */
    public abstract boolean checkOpType(int cardID, OpType opType);

    /**
     * 检测平胡
     *
     * @param cardID
     * @return
     */
    public abstract OpType checkPingHu(int curOpPos, int cardID);

    /**
     * 检测自摸胡
     *
     * @return
     */
    public abstract List<OpType> recieveOpTypes();

    public MJSetPosRobot getSetPosRobot() {
        return new MJSetPosRobot(this);
    }

    /**
     * 新位置结算信息
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    protected BaseMJRoom_PosEnd newMJSetPosEnd() {
        return new BaseMJRoom_PosEnd();
    }

    /**
     * 位置结算信息
     *
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected BaseMJRoom_PosEnd posEndInfo() {
        BaseMJRoom_PosEnd ret = this.newMJSetPosEnd();
        // 练习场结算
        this.goldEnd();
        // 位置
        ret.setPos(this.getPosID());
        // 记录用户PID
        ret.setPid(this.getPid());
        // 每个玩家的胡牌类型 0不胡 ；1自摸；2抢杠胡
        ret.setHuType(this.getHuType());
        // 设置是否是接炮
        ret.setJiePao(MJHuOpType.JiePao.equals(getmHuOpType()) || getHuType() == HuType.QGH);
        // 设置结算分数
        ret.setPoint(this.getEndPoint());
        // 设置手牌
        ret.setShouCard(this.getPrivateCard().stream().map(k -> k.getCardID()).collect(Collectors.toList()));
        // 设置首牌
        ret.setHandCard(this.getHandCard() == null ? 0 : this.getHandCard().getCardID());
        // 公共牌
        ret.setPublicCardList(this.getPublicCardList());
        // 花牌列表
        ret.setHuaList(this.getPosOpRecord().getHuaList());
        // 检查是否胡牌奖励
        ret.setReward(this.checkHuReward());
        //设置推广员ID
        ret.setUpLevelId(this.getRoomPos().getUpLevelId());
        // 固定算分总结算才有分数
        if (!getRoom().isGuDingSuanFen()) {
            // 竞技点分数
            if (this.getRoom().calcFenUseYiKao() && RoomTypeEnum.UNION.equals(this.getRoom().getRoomTypeEnum())) {
                ret.setSportsPoint(this.getDeductPointYiKao());
            } else {
                ret.setSportsPoint(this.getRoomPos().setSportsPoint(this.getEndPoint()));
            }
        } else {
            if (RoomTypeEnum.UNION.equals(getRoom().getRoomTypeEnum())) {
                ret.setSportsPoint(0D);
            }
        }
        // 房间分数叠加
        this.pidPointEnd();
        // 房间总结算叠加
        this.calcResults();
        ret.setRoomPoint(this.pidSumPointEnd());
        ret.setRoomSportsPoint(getRoomPos().getRoomSportsPoint());
        return ret;
    }


    /**
     * 练习场结算
     */
    public void goldEnd() {
        if (this.getSet().checkExistPrizeType(PrizeType.Gold)) {
            setEndPoint(this.getRoom().getBaseMark() * getEndPoint());
            if (!this.getRoomPos().isRobot()) {
                this.getRoomPos().getPlayer().getFeature(PlayerCurrency.class).goldRoomEnd(getEndPoint(), this.getRoom().getBaseMark(), this.getRoom().getBaseRoomConfigure().getGameType().getId());
            }
        }
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
    protected AbsBaseResults mResultsInfo() {
        AbsBaseResults mResults = this.getResults();
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
        // 记录胡牌次数
        if (!(HuType.NotHu.equals(this.getHuType()) || HuType.DianPao.equals(this.getHuType()))) {
            mResults.setHuCnt(this.pidHuCntEnd());
            mResults.addHuTypes(this.getHuType());
        }
        // 总分数
        mResults.setPoint(this.pidSumPointEnd());
        // 总竞技点分数
        mResults.setSportsPoint(this.getRoomPos().sportsPoint());
        // 点炮次数
        mResults.addDianPaoPoint(this.getHuType());
        // 接炮次数
        mResults.addJiePaoPoint(this.getHuType());
        // 自摸次数
        mResults.addZimoPoint(this.getHuType());
        return mResults;
    }

    /**
     * 计算总结算信息
     */
    public void calcResults() {
        // 获取总结算信息
        AbsBaseResults mResultsInfo = this.mResultsInfo();
        // 并且设置覆盖
        this.setResults(mResultsInfo);
    }

    public void setLianZhuangNum(int lianZhuangNum) {
        if (lianZhuangNum == 1) {
            this.lianZhuangNum += 1;
        }
    }

    /**
     * 获取打出公共牌列表
     * 取出的值牌id列表：1201四位数
     *
     * @return
     */
    public List<Integer> publicCardList() {
        // 已经亮出的牌
        List<List<Integer>> publicCardList = new ArrayList<List<Integer>>();
        publicCardList.addAll(this.getPublicCardList());
        // 获取公共牌的类型
        List<Integer> list = new ArrayList<Integer>();
        list.addAll(publicCardList.stream().map(k -> k.subList(3, k.size())).flatMap(k -> k.stream())
                .collect(Collectors.toList()));
        publicCardList = null;
        return list;
    }

    /**
     * 获取打出公共牌列表
     * 取出的值牌id：1201四位数
     *
     * @return
     */
    public List<Integer> publicCardTypeList() {
        // 已经亮出的牌
        List<List<Integer>> publicCardList = new ArrayList<List<Integer>>();
        publicCardList.addAll(this.getPublicCardList());
        // 获取公共牌的类型
        List<Integer> list = new ArrayList<Integer>();
        list.addAll(publicCardList.stream().map(k -> k.get(2)).collect(Collectors.toList()));
        publicCardList = null;
        return list;
    }

    /**
     * 胡牌奖励
     */
    protected boolean checkHuReward() {
        // 检查是否机器人；
        if (this.getRoomPos().isRobot()) {
            return false;
        }
        // 好友胡牌
        this.friendHuPai();
        // 如果不是房卡场则没有胡牌奖励
        if (!PrizeType.RoomCard.equals(this.getRoom().getBaseRoomConfigure().getPrizeType())) {
            return false;
        }
        // 胡牌奖励
        if (!(HuType.NotHu.equals(this.getHuType()) || HuType.DianPao.equals(this.getHuType()))) {
            // 检查胡牌奖励
            return HuRewardConfigMgr.getInstance().checkHuBeginTime(
                    // 房间ID
                    this.getSet().getRoom().getRoomID(),
                    // 局数
                    this.getSet().getSetID(),
                    // 玩家ID
                    this.getPid(),
                    // 游戏类型
                    this.getSet().getRoom().getBaseRoomConfigure().getGameType().getId(),
                    // 胡类型
                    this.getHuType().value(),
                    // 公会代理ID
                    this.getRoomPos().getPlayer().getFamiliID());
        }
        return false;
    }

    /**
     * 好友胡牌
     */
    protected void friendHuPai() {
        // 胡牌奖励
        if (!(HuType.NotHu.equals(this.getHuType()) || HuType.DianPao.equals(this.getHuType()))) {
            Player player = this.getRoomPos().getPlayer();
            // 没有玩家信息
            if (null == player) {
                return;
            }
            // 执行获取新人红包
            player.getFeature(PlayerFriendsHelpUnfoldRedPack.class).exeFirstRegisterLoginGame();
            // 获取推荐人ID
            int accountID = player.getPlayerBO().getRealReferer();
            // 检查是否有直接推荐人
            if (accountID > 0
                    && CommTime.hourTimeDifference(player.getPlayerBO().getCreateTime(), CommTime.nowMS()) <= 24) {
                DispatcherComponent.getInstance().publish(new FriendsHelpUnfoldRedPackEvent(this.getPid(), TargetType.FriendHuPai, accountID));
            }
        }
    }

    /**
     * 过手
     */
    public void clearPass() {
        // 清空漏胡类型列表
        this.getPosOpRecord().clearHuCardType();
        // 清空漏碰类型列表
        this.getPosOpRecord().clearOpCardType();
    }

    /**
     * 清空打牌之前记录数据
     */
    public void clearOutCard() {
        // 过手
        this.clearPass();
        // 清空操作胡列表
        this.getPosOpRecord().clearOpHuList();
        // 清空操作胡类型
        this.clearOpHuType();
        // 清空操作牌初始
        this.clearMCardInit();
        // 清空动作记录
        this.getSet().getSetPosMgr().cleanAllOpType();
        // 清空不能出的牌
        this.getPosOpNotice().clearBuNengChuList();
        // 清空听牌map
        this.getPosOpNotice().clearTingCardMap();
    }

    /**
     * 清空检查炮胡前记录数据
     */
    public void clearPaoHu() {
        // 清空操作胡列表
        this.getPosOpRecord().clearOpHuList();
        // 清空操作牌初始
        this.clearMCardInit();
    }


    /**
     * 计算动作分数类型
     *
     * @param <T>
     */
    public abstract <T> void calcOpPointType(T opType, int count);

    /**
     * 计算动作分数类型
     *
     * @param <T>
     */
    public <T> void calcOpPointType(T opType, int count, MJEndType endType) {
        getCalcPosEnd().calcOpPointType(opType, count, endType);
    }

}
