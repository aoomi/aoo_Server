package business.global.mj.cdxzmj;


import business.cdxzmj.c2s.iclass.*;
import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetPosMgr;
import business.global.mj.AbsMJSetRound;
import business.global.mj.MJCard;
import business.global.mj.manage.MJFactory;
import business.global.mj.template.MJTemplateRoomSet;
import business.global.room.mj.MJRoomPos;
import cenum.mj.HuType;
import cenum.mj.MJSpecialEnum;
import cenum.mj.OpType;
import cenum.room.SetState;
import com.ddm.server.common.utils.CommTime;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;


/**
 * 模板麻将一局游戏逻辑
 *
 * @author Huaxing
 */
@Getter
@Setter
public class CDXZMJRoomSet extends MJTemplateRoomSet {


    public CDXZMJRoomSet(int setID, CDXZMJRoom room, int dPos) {
        super(setID, room, dPos);

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
        getSetPosMgr().startSetApplique();
        // 通知本局开始		
        this.notify2SetStart();
        // 一些基本数据初始，无需理会。		
        exeStartSet();
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
        AbsMJSetPos setPos = this.posDict.get(opPos);
        // 随机摸牌		
        MJCard card = this.setCard.pop(isNormalMo, this.getGodInfo().godHandCard(setPos));
        if (Objects.isNull(card)) {
            // 黄庄位置		
            this.getMHuInfo().setHuangPos(opPos);
            return null;
        }
        // 设置牌		
        setPos.getCard(card);
        // 通知房间内的所有玩家，指定玩家摸牌了。		
        if (setPos.getPosOpRecord().sizeHua() == 8) {
            setPos.setHuCardType(HuType.ZiMo, setPos.getPosID(), getRoundId());
            return null;
        }
        this.notifyGetCard(setPos);
        return setPos.getHandCard();
    }

    /**
     * 检查小局托管自动解散
     */
    @Override
    public boolean checkSetEndTrusteeshipAutoDissolution() {
        if (this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getFangjian().contains(CDXZMJRoomEnum.CDXZMJGameRoomConfigEnum.TuoGuanJieSan2.ordinal())) {
            return ((CDXZMJRoomMgr) this.getRoom().getRoomPosMgr()).checkSetEndTrusteeship();
        }
        return this.getRoom().getBaseRoomConfigure().getBaseCreateRoom().getFangjian().contains(CDXZMJRoomEnum.CDXZMJGameRoomConfigEnum.TuoGuanJieSan.ordinal());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CDXZMJRoomSet) {
            if (getSetID() == ((CDXZMJRoomSet) o).getSetID()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置setstate
     */
    @Override
    public void setState(SetState setState) {
        this.state = setState;
        this.startMS = CommTime.nowMS();
        this.room.getRoomPosMgr().notify2All(SCDXZMJ_ChangeStatus.make(this.room.getRoomID(), this.getSetID(), this.getState(), this.dPos, getPiaoFenList()));
    }


    /**
     * 开金通知
     */
    @Override
    public void kaiJinNotify(MJCard jinCard, MJCard jinCard2) {
        getRoomPlayBack().playBack2All(SCDXZMJ_Jin.make(getRoom().getRoomID(), getmJinCardInfo().getJin(0).cardID, getMJSetCard().getRandomCard().getNormalMoCnt(), getMJSetCard().getRandomCard().getGangMoCnt()));

    }

    /**
     * 麻将补花
     */
    @Override
    public void MJApplique(int pos) {
        AbsMJSetPos setPos = posDict.get(pos);
        BaseMJSet_Pos posInfoOther = setPos.getNotify(false);
        BaseMJSet_Pos posInfoSelf = setPos.getNotify(true);
        getRoomPlayBack().playBack2Pos(pos,
                SCDXZMJ_Applique.make(getRoom().getRoomID(), pos, OpType.Out, 0, false, posInfoSelf, getSetCard().getRandomCard().getNormalMoCnt(), getSetCard().getRandomCard().getGangMoCnt()), null);
        for (int i = 0; i < getRoom().getPlayerNum(); i++) {
            if (i == pos)
                continue;
            getRoom().getRoomPosMgr().notify2Pos(i,
                    SCDXZMJ_Applique.make(getRoom().getRoomID(), pos, OpType.Out, 0, false, posInfoOther, getSetCard().getRandomCard().getNormalMoCnt(), getSetCard().getRandomCard().getGangMoCnt()));
        }
    }


    /**
     * 摸牌消息
     */
    @Override
    protected <T> BaseSendMsg posGetCard(long roomID, int pos, int normalMoCnt, int gangMoCnt, T set_Pos) {
        return SCDXZMJ_PosGetCard.make(roomID, pos, normalMoCnt, gangMoCnt, set_Pos);
    }

    /**
     * 下回合操作位置
     */
    @Override
    protected AbsMJSetRound nextSetRound(int roundID) {
        return new CDXZMJSetRound(this, roundID);
    }

    /**
     * 小局结算消息
     */
    @Override
    protected <T> BaseSendMsg setEnd(long roomID, T setEnd) {
        return SCDXZMJ_SetEnd.make(roomID, setEnd);
    }

    /**
     * 玩家位置信息
     */
    @Override
    protected AbsMJSetPos absMJSetPos(int posID) {
        return new CDXZMJSetPos(posID, (MJRoomPos) this.room.getRoomPosMgr().getPosByPosID(posID),
                this);

    }

    /**
     * 牌局开始消息通知
     */
    @Override
    protected <T> BaseSendMsg setStart(long roomID, T setInfo) {
        return SCDXZMJ_SetStart.make(roomID, setInfo);
    }

    @Override
    protected <T> BaseSendMsg genZhuang(long roomID, int count) {
        return SCDXZMJ_GenZhuang.make(roomID, getDPos(), count);
    }

    /**
     * 本局玩家操作管理
     */
    @Override
    protected AbsMJSetPosMgr absMJSetPosMgr() {
        return new CDXZMJSetPosMgr(this);
    }

    /**
     * 计算圈
     */
    public void calcCurSetQuan() {
    }

    /**
     * 牌数
     */
    @Override
    public int cardSize() {
        return MJSpecialEnum.SIZE_13.value();
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
     * 是否白板替金
     */
    @Override
    public boolean isBaiBanTiJin() {
        return false;
    }

    /**
     * 回放记录添加游戏配置
     */
    @Override
    public void addGameConfig() {
        this.getRoomPlayBack().addPlaybackList(SCDXZMJ_Config.make(this.getRoom().getCfg(), this.getRoom().getRoomTyepImpl().getRoomTypeEnum()), null);
    }

    /**
     * 本局牌管理
     */
    @Override
    protected void absMJSetCard() {
        // 设置当局牌			
        this.setSetCard(new CDXZMJSetCard(this));
    }

    /**
     * 发送设置位置的牌
     */
    @Override
    public void sendSetPosCard() {
        for (int i = 0; i < room.getPlayerNum(); i++) {
            AbsMJSetPos setPos = posDict.get(i);
            setPos.sortCards();
        }
        for (int i = 0; i < room.getPlayerNum(); i++) {
            long pid = this.room.getRoomPosMgr().getPosByPosID(i).getPid();
            if (i == 0) {
                this.getRoomPlayBack().playBack2Pos(i,
                        SCDXZMJ_SetPosCard.make(this.room.getRoomID(), this.setPosCard(pid)), null);
            } else {
                this.room.getRoomPosMgr().notify2Pos(i,
                        SCDXZMJ_SetPosCard.make(this.room.getRoomID(), this.setPosCard(pid)));
            }
        }
    }

    @Override
    public boolean isConfigName() {
        return true;
    }

    @Override
    public CDXZMJRoom getRoom() {
        return (CDXZMJRoom) super.getRoom();
    }

    /**
     * 小局托管自动解散回放记录 注意：需要自己重写
     *
     * @param roomId  房间id
     * @param pidList 托管玩家Pid
     * @param sec     记录时间
     * @return
     */
    @Override
    public BaseSendMsg DissolveTrusteeship(long roomId, List<Long> pidList, int sec) {
        return CDXZMJ_DissolveTrusteeship.make(roomId, pidList, sec);
    }

    /**
     * 计算当局每个pos位置的分数。
     */
    @Override
    protected void calcCurSetPosPoint() {
        // 计算圈
        this.calcCurSetQuan();
        // 计算位置小局分数
        this.getPosDict().values().forEach(k -> k.setHuCardTypes(MJFactory.getTingCard(k.getmActMrg()).checkTingCard(k, k.getPrivateCard())));
        this.getPosDict().values().forEach(k -> k.calcPosPoint());

    }
}