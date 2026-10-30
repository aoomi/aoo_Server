package business.global.mj.extbussiness;

import business.global.mj.AbsMJSetRoom;
import business.global.mj.extbussiness.StandardMJRoomEnum.GameRoomConfigEnum;
import business.global.mj.extbussiness.StandardMJRoomEnum.HuanZhangOderBy;
import business.global.mj.extbussiness.StandardMJRoomEnum.KaiJin;
import business.global.mj.extbussiness.dto.StandardMJRoomSetInfo;
import business.global.mj.extbussiness.dto.iclass.CStandardMJ_PiaoFen;
import business.global.mj.extbussiness.dto.iclass.SStandardMJ_GetRoomInfo;
import business.global.room.base.AbsBaseRoom;
import business.global.room.mj.MahjongRoom;
import cenum.ChatType;
import cenum.ClassType;
import cenum.room.GaoJiTypeEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.proto._ChatMessage;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.RoomEndResult;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.GetRoomInfo;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.iclass.mj.*;
import jsproto.c2s.iclass.room.SBase_Dissolve;
import jsproto.c2s.iclass.room.SBase_PosLeave;
import lombok.Data;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * 麻将游戏房间
 *
 * @author Administrator
 * @apiNote 子游戏请继承这个类
 */
@Data
public abstract class StandardMJRoom<T extends StandardMJRoomSet, E extends StandardMJRoomPosMgr> extends MahjongRoom {

    public boolean isEnd = false;

    private int setCount = 0;

    /**
     * 最后位置的玩家
     */
    protected int lastPosId = -1;

    /**
     * 圈数
     */
    public int circleNum = 0;

    protected StandardMJRoom(BaseRoomConfigure<?> baseRoomConfigure, String roomKey, long ownerID) {
        super(baseRoomConfigure, roomKey, ownerID);
        this.setSetCount(baseRoomConfigure.getBaseCreateRoom().getSetCount());
    }

    @Override
    public boolean is30SencondTimeOut() {
        return checkGaoJiXuanXiang(GaoJiTypeEnum.SECOND_TIMEOUT_30);
    }

    public boolean needAtOnceOpCard() {
        return true;
    }

    @Override
    public void clearEndRoom() {
        super.clear();
    }

    @Override
    public boolean autoStartGame() {
        return true;
    }

    @Override
    public boolean existLeaveClearAllPosReady() {
        return true;
    }

    @Override
    public boolean isDisAbleVoice() {
        return checkGaoJiXuanXiang(GaoJiTypeEnum.DISABLE_VOICE);
    }

    public boolean isFangJianConstant(GameRoomConfigEnum configEnum) {
        return this.getBaseRoomConfigure().getBaseCreateRoom().getFangjian().contains(getAdapterFangJian(configEnum.name()));
    }

    public boolean isConnectClearTrusteeship() {
        return false;
    }

    public boolean isMoDa() {
        return true;
    }

    @Override
    public void startNewSet() {
        // 更新连续托管局数
        for (int i = 0; i < getPlayerNum(); i++) {
            StandardMJRoomPos extRoomPos = (StandardMJRoomPos) getRoomPosMgr().getPosByPosID(i);
            if (extRoomPos.isTrusteeship()) { // 托管
                extRoomPos.addTuoGuanSetCount();
            }
        }
        this.setCurSetID(this.getCurSetID() + 1);
        // / 计算庄位
        if (this.getCurSetID() == 1) {
            setDPos(0);
            //第一圈
            if (getQuanSetCount().contains(getCount())) {
                circleNum = 1;
            }
        } else if (this.getCurSet() != null) {
            AbsMJSetRoom mRoomSet = (AbsMJSetRoom) this.getCurSet();
            // 根据上一局计算下一局庄家									
            int beforeDpos = mRoomSet.getDPos();
            int dPosInt = mRoomSet.calcNextDPos();
            //庄数增加
            if (getQuanSetCount().contains(getCount()) && beforeDpos != dPosInt && beforeDpos == lastPosId) {
                circleNum++;
            }
            // 根据上一局计算下一局庄家
            setDPos(dPosInt);
            mRoomSet.clear();
        }
        // 每个位置，清空准备状态
        this.getRoomPosMgr().clearGameReady();
        // 通知局数变化									
        this.getRoomTyepImpl().roomSetIDChange();
        this.setCurSet(this.newMJRoomSet(this.getCurSetID(), this, this.getDPos()));
    }

    @Override
    public GetRoomInfo getRoomInfo(long pid) {
        SStandardMJ_GetRoomInfo ret = new SStandardMJ_GetRoomInfo();
        // 设置房间公共信息									
        this.getBaseRoomInfo(ret);
        if (null != this.getCurSet()) {
            ret.setSet(this.getCurSet().getNotify_set(pid));
            ret.setQuanShu(getCircleNum());
        } else {
            ret.setSet(childrenMJRoomSetInfo());
            ret.setQuanShu(0);
        }
        return ret;
    }

    @Override
    public BaseSendMsg Trusteeship(long roomID, long pid, int pos, boolean trusteeship) {
        return _Trusteeship.make(roomID, pid, pos, trusteeship, getGameName());
    }

    @Override
    public BaseSendMsg PosLeave(SBase_PosLeave posLeave) {
        return _PosLeave.make(posLeave, getGameName());
    }

    @Override
    public BaseSendMsg LostConnect(long roomID, long pid, boolean isLostConnect, boolean isShowLeave) {
        return _LostConnect.make(roomID, pid, isLostConnect, isShowLeave, getGameName());
    }

    @Override
    public BaseSendMsg PosContinueGame(long roomID, int pos) {
        return _PosContinueGame.make(roomID, pos, getGameName());
    }

    @Override
    public BaseSendMsg PosUpdate(long roomID, int pos, RoomPosInfo posInfo, int custom) {
        return _PosUpdate.make(roomID, pos, posInfo, custom, getGameName());
    }

    @Override
    public BaseSendMsg PosReadyChg(long roomID, int pos, boolean isReady) {
        return _PosReadyChg.make(roomID, pos, isReady, getGameName());
    }

    @Override
    public BaseSendMsg Dissolve(SBase_Dissolve dissolve) {
        return _Dissolve.make(dissolve, getGameName());
    }

    @Override
    public BaseSendMsg StartVoteDissolve(long roomID, int createPos, int endSec) {
        return _StartVoteDissolve.make(roomID, createPos, endSec, getGameName());
    }

    @Override
    public BaseSendMsg PosDealVote(long roomID, int pos, boolean agreeDissolve, int endSec) {
        return _PosDealVote.make(roomID, pos, agreeDissolve, getGameName());
    }

    @Override
    public BaseSendMsg Voice(long roomID, int pos, String url) {
        return _Voice.make(roomID, pos, url, getGameName());
    }

    @Override
    public <T> BaseSendMsg RoomRecord(List<T> records) {
        return _RoomRecord.make(records, getGameName());
    }

    @Override
    public <T> BaseSendMsg RoomEnd(T record, RoomEndResult<?> sRoomEndResult) {
        return _RoomEnd.make(this.getMJRoomRecordInfo(), this.getRoomEndResult(), getGameName());
    }

    @Override
    public BaseSendMsg XiPai(long roomID, long pid, ClassType cType) {
        return _XiPai.make(roomID, pid, cType, getGameName());
    }

    @Override
    public BaseSendMsg ChatMessage(long pid, String name, String content, ChatType type, long toCId, int quickID) {
        return _ChatMessage.make(pid, name, content, type, toCId, quickID, getGameName());
    }

    @Override
    public BaseSendMsg ChangePlayerNum(long roomID, int createPos, int endSec, int playerNum) {
        return _ChangePlayerNum.make(roomID, createPos, endSec, playerNum, getGameName());
    }

    @Override
    public BaseSendMsg ChangePlayerNumAgree(long roomID, int pos, boolean agreeChange) {
        return _ChangePlayerNumAgree.make(roomID, pos, agreeChange, getGameName());
    }

    @Override
    public BaseSendMsg ChangeRoomNum(long roomID, String roomKey, int createType) {
        return _ChangeRoomNum.make(roomID, roomKey, createType, getGameName());
    }

    @Override
    public boolean isEnd() {
        return isEnd;
    }

    @Override
    public int getJieShanShu() {
        ArrayList<Integer> fangjian = this.getBaseRoomConfigure().getBaseCreateRoom().getFangjian();
        if (fangjian.contains(getAdapterFangJian(GameRoomConfigEnum.JieSanCishu3.name()))) {
            return 3;
        } else if (fangjian.contains(getAdapterFangJian(GameRoomConfigEnum.JieSanCishu5.name()))) {
            return 5;
        }
        return 0;
    }

    @Override
    public boolean needDissolveCount() {
        return getJieShanShu() > 0;
    }

    @Override
    public boolean autoReadyGame() {
        return isFangJianConstant(GameRoomConfigEnum.ZiDongZhunBei);
    }

    @Override
    public boolean ownerNeedReady() {
        return !isFangJianConstant(GameRoomConfigEnum.ZiDongZhunBei);
    }

    @Override
    public boolean isCanChangePlayerNum() {
        return isFangJianConstant(GameRoomConfigEnum.FangJianQieHuanRenShu);
    }

    public boolean isSetAutoJieSan() {
        return isFangJianConstant(GameRoomConfigEnum.SetAutoJieSan);
    }

    public void opPiaoFen(WebSocketRequest request, long pid, CStandardMJ_PiaoFen data) {
        try {
            lock();
            if (null == this.getCurSet()) {
                request.error(ErrorCode.NotAllow, "");
                return;
            }
            StandardMJRoomSet roomSet = (StandardMJRoomSet) this.getCurSet();
            roomSet.opPiaoFen(request, pid, data);
        } catch (Exception e) {
            CommLogD.error(e.getMessage());
        } finally {
            unlock();
        }
    }

    @Override
    protected StandardMJRoomSet newMJRoomSet(int curSetID, MahjongRoom room, int dPos) {
//        return new StandardMJRoomSet(curSetID, (StandardMJRoom) room, dPos);
        try {
            // 以下调用带参的、私有构造函数
            Class<T> actualTypeArgument = (Class<T>) (((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
            Constructor constructor = actualTypeArgument
                    .getDeclaredConstructor(new Class[]{int.class, StandardMJRoom.class, int.class});
            // 私有构造函数中的成员变量为private,故必须进行此操作
            constructor.setAccessible(true);
            return (StandardMJRoomSet) constructor.newInstance(new Object[]{curSetID, room, dPos});
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            return null;
//            return new StandardMJRoomSet(curSetID, (StandardMJRoom) room, dPos);
        }
    }

    @Override
    public StandardMJRoomPosMgr initRoomPosMgr() {
        try {
            // 以下调用带参的、私有构造函数
            Class<E> actualTypeArgument = (Class<E>) (((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1]);
            Constructor constructor = actualTypeArgument
                    .getDeclaredConstructor(new Class[]{AbsBaseRoom.class});
            // 私有构造函数中的成员变量为private,故必须进行此操作
            constructor.setAccessible(true);
            return (StandardMJRoomPosMgr) constructor.newInstance(new Object[]{this});
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            return null;
//            return new StandardMJRoomPosMgr(this);
        }
    }

    public StandardMJRoomSetInfo childrenMJRoomSetInfo() {
        return new StandardMJRoomSetInfo();
    }

    public int getAdapterFangJian(String configEnum) {
        return GameRoomConfigEnum.getOrdinalByName(configEnum);
    }

    public int getSetCount() {
        // 打圈模式（这里显示真正的圈数）
        return getCount() - 100;
    }

    public List<Integer> getQuanSetCount() {
        return new ArrayList<>();
    }

    /**
     * 获取总局数
     *
     * @return
     */
    @Override
    public int getEndCount() {
        if (getQuanSetCount().contains(getCount())) {
            // 打圈模式(不靠这里结束，靠isEnd)
            return getCurSetID() + 100;
        }
        return this.getBaseRoomConfigure().getBaseCreateRoom().getSetCount();
    }

    /**
     * 补花适配
     * 根据自己游戏写补花牌的列表
     **/
    public List<Integer> getHuaList() {
        return new ArrayList<>();
    }

    /**
     * private补花是否是一次性
     * @return
     */
    public boolean isOneTimeBuHua() {
        return false;
    }

    /**
     * private补花每家间隔时间
     * @return
     */
    public int getCustomerBuHuaTime(){
        return 2000;
    }

    /**
     * 锅分适配
     * 根据自己游戏以及方法内的枚举自己重写个枚举（GuoFen）
     **/
    public int getGuoFen() {
//        return StandardMJRoomEnum.GuoFen.getValue(0);
        return 0;
    }

    /**
     * 开金适配
     * 根据自己游戏调用枚举
     */
    public KaiJin getJinType() {
        return KaiJin.NotJin;
    }

    /**
     * 杠后留牌适配
     * 根据自己游戏决定是否需要杠后留牌这个功能
     */
    public boolean isGangHouLiuPai() {
        return false;
    }

    /**
     * 吃碰无牌适配
     * 吃碰后手里只有吃碰的牌，是否允许吃碰。
     * 根据自己游戏决定是否需要吃碰无牌这个功能
     *
     * @return
     */
    public boolean isChiPengWuPai() {
        return false;
    }

    /**
     * 封顶适配
     * 根据自己游戏以及方法内的枚举自己重写个枚举（FengDing）
     */
    public int getFengDing() {
//        return StandardMJRoomEnum.FengDing.getValue(3);
        return 0;
    }

    /**
     * 抄庄适配
     * 根据自己游戏创建玩法决定是不是开启抄庄
     **/
    public boolean isChaoZhuang() {
        return false;
    }

    /**
     * 实时算杠分适配
     * 根据自己游戏决定是否需要实时算杠分这个功能
     */
    public boolean isSSSG() {
        return false;
    }

    /**
     * 报听适配
     * 根据自己游戏决定是否需要报听这个功能
     */
    public boolean isBaoTing() {
        return false;
    }

    /**
     * 漏胡适配
     * 根据自己游戏决定是否需要漏胡这个功能
     */
    public boolean isHaveLouHu() {
        return false;
    }

    /**
     * 自摸不胡算漏胡适配
     * 根据自己游戏决定是否需要自摸不胡算漏胡这个功能
     */
    public boolean isHaveZiMoLouHu() {
        return false;
    }

    /**
     * 漏胡超分解除适配
     * 根据自己游戏决定是否需要漏胡超分解除这个功能
     */
    public boolean isLouHuOverRemove() {
        return false;
    }

    /**
     * 是否每局漂适配
     *
     * @return
     */
    public boolean isPiaoFenEvery() {
//        return Piao.getPiao(3).value == 0;
        return false;
    }

    /**
     * 是否固定漂适配
     *
     * @return
     */
    public boolean isPiaoFenGuDing() {
//        return Piao.getPiao(3).value > 0;
        return false;
    }

    /**
     * 固定漂适配
     *
     * @return
     */
    public int getPiaoFenGuDing() {
//        return Piao.getPiao(3).value;
        return 0;
    }

    /**
     * 吃碰杠清掉漏胡适配
     */
    public boolean isCheckExistClearPass() {
        return true;
    }

    /**
     * 换n张适配（该玩法抄自模板麻将）
     * 根据自己游戏以及方法内的枚举自己重写个枚举（HuanZhang）
     */
    public boolean isHuanTongSe() {
        //默认不换张
//        return HuanZhang.getHuanZhang(0).tongSe;
        return false;
    }

    /**
     * 换n张适配（该玩法抄自模板麻将）
     * 根据自己游戏以及方法内的枚举自己重写个枚举（HuanZhang）
     */
    public int getHuanZhangNum() {
        //默认不换张
//        return HuanZhang.getHuanZhang(0).value;
        return 0;
    }

    /**
     * 换张顺序适配
     *
     * @return
     */
    public HuanZhangOderBy huanZhangOderBy() {
        return HuanZhangOderBy.ShunShiZhen;
    }

    /**
     * 奖马适配
     *
     * @return
     */
    public int getMaiMa() {
//        return StandardMJRoomEnum.MaiMa.valueOf(0).value();
        return 0;
    }

    /**
     * 同中码适配
     * 根据游戏中码规则，是各自中自己还是统一中一样
     *
     * @return
     */
    public boolean isSameZhongMa() {
//        return StandardMJRoomEnum.MaiMa.valueOf(0).isSameZhongMa();
        return false;
    }

    /**
     * 是否吃大字牌
     *
     * @return
     */
    public boolean isChiDaZhi() {
        return false;
    }

    /**
     * 是否胡大字
     *
     * @return
     */
    public boolean isHuDaZi() {
        return false;
    }

    /**
     * 漏碰
     * @return
     */
    public boolean isLouPeng() {
        return true;
    }


    /**
     * 是否需要胡牌信息
     * @return
     */
    public boolean needHuInfo(){
        return false;
    }

    /**
     * 是否需要检测精吊那种乱七八遭的
     * 需要重写听胡的那个Ting中带isTingAnyCard方法和getPointItem带isTingAnyCard的方法
     * @return
     */
    public boolean isTingAnyCard(){
        return false;
    }
}
