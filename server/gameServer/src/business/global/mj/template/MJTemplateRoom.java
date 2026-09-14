package business.global.mj.template;

import business.global.mj.AbsMJSetRoom;
import business.global.mj.AbsMJSetRound;
import business.global.mj.hu.NormalHuCardImpl;
import business.global.room.base.AbsRoomPos;
import business.global.room.mj.MahjongRoom;
import cenum.mj.OpType;
import cenum.room.GaoJiTypeEnum;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.mj.BaseMJRoom_SetRound;
import jsproto.c2s.cclass.mj.template.MJTemplateRoomSetInfo;
import jsproto.c2s.cclass.mj.template.MJTemplateWaitingExInfo;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.GetRoomInfo;
import jsproto.c2s.iclass.S_GetRoomInfo;
import jsproto.c2s.iclass.mj.CMJ_OpPass;
import jsproto.c2s.iclass.mj.SMJ_OpPass;
import jsproto.c2s.iclass.mj._StartRound;

import java.util.*;


/**
 * 模板麻将游戏房间
 *
 * @author Administrator
 */
public abstract class MJTemplateRoom extends MahjongRoom {

    /**
     * 需要补花的牌 cardType
     */
    protected Set<Integer> buHuaTypeSet = new HashSet<>();
    protected Class<?> huCardImpl = null;

    protected MJTemplateRoom(BaseRoomConfigure baseRoomConfigure, String roomKey, long ownerID) {
        super(baseRoomConfigure, roomKey, ownerID);
        initHuCardClz();
    }

    public SData_Result setRoundInfo(WebSocketRequest request, long pid, int setID) {
        try {
            lock();
            AbsMJSetRoom curSet = (AbsMJSetRoom) getCurSet();
            if (curSet == null || curSet.getSetID() != setID) {
                return SData_Result.make(ErrorCode.NotAllow,
                        "not current setID:" + (curSet == null ? 0 : curSet.getSetID()));
            }
            AbsMJSetRound curRound = curSet.getCurRound();
            if (curRound == null) {
                return SData_Result.make(ErrorCode.NotAllow, "not current roundID:0");
            }
            AbsRoomPos roomPos = getRoomPosMgr().getPosByPid(pid);
            if (roomPos == null) {
                return SData_Result.make(ErrorCode.NotAllow, "not find your pos:");
            }
            BaseMJRoom_SetRound roundInfo = curRound.getNotify_RoundInfo(roomPos.getPosID());
            _StartRound<BaseMJRoom_SetRound> startRound = _StartRound.make(
                    getRoomID(), roundInfo, getBaseRoomConfigure().getGameType().getName());
            getRoomPosMgr().notify2Pos(roomPos.getPosID(), startRound);
            request.response();
            return SData_Result.make(ErrorCode.Success);
        } finally {
            unlock();
        }
    }


    public void initHuCardClz() {
        if (Objects.isNull(huCardImpl)) {
            try {
                String basePath = getClass().getName();
                basePath = basePath.replaceAll(getClass().getSimpleName(), getBaseRoomConfigure().getGameType().getName() + "NormalHuCardImpl");
                huCardImpl = Class.forName(basePath);
            } catch (ClassNotFoundException e) {
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            } finally {
                if (Objects.isNull(huCardImpl)) {
                    huCardImpl = NormalHuCardImpl.class;
                }
            }
        }

    }

    /**
     * 是否有定缺玩法
     *
     * @return
     */
    public boolean isWanFa_DingQue() {
        return MJTemplateRoomEnum.DingQue.DING_QUE.equals(wanFa_DingQue());
    }


    /**
     * 单个玩家胡牌轮庄
     *
     * @return
     */
    public MJTemplateRoomEnum.HuCardLunZhuang wanFa_huCardLunZhuang() {
        return MJTemplateRoomEnum.HuCardLunZhuang.HU_JIA_ZHUANG;
    }

    /**
     * 实时算分
     *
     * @return
     */
    public MJTemplateRoomEnum.ActualTimeCalcPoint wanFa_ActualTimeCalcPoint() {
        return MJTemplateRoomEnum.ActualTimeCalcPoint.NOT;
    }

    /**
     * 摸的最后一张牌
     *
     * @return
     */
    public MJTemplateRoomEnum.OutLastCard wanFa_OutLastCard() {
        return MJTemplateRoomEnum.OutLastCard.YES;
    }

    /**
     * 流局轮庄
     *
     * @return
     */
    public MJTemplateRoomEnum.LiuJuLunZhuang wanFa_liuJuLunZhuang() {
        return MJTemplateRoomEnum.LiuJuLunZhuang.ZHUANG_LIAN_ZHUANG;
    }

    /**
     * 一炮打响轮庄
     *
     * @return
     */
    public MJTemplateRoomEnum.YPDXLunZhuang wanFa_YPDXLunZhuang() {
        return MJTemplateRoomEnum.YPDXLunZhuang.DIAN_PAO_ZHUANG;
    }

    /**
     * 一炮多响并且庄胡
     *
     * @return
     */
    public MJTemplateRoomEnum.YPDXLunZhuang_ZhuangHu wanFa_YPDXLunZhuang_ZhuangHu() {
        return MJTemplateRoomEnum.YPDXLunZhuang_ZhuangHu.NOT;
    }

    /**
     * 定缺
     *
     * @return
     */
    public MJTemplateRoomEnum.DingQue wanFa_DingQue() {
        return MJTemplateRoomEnum.DingQue.NOT;
    }

    /**
     * 定缺时间  单位:s
     *
     * @return
     */
    public int wanFa_DingQue_Time() {
        return isWanFa_DingQue() ? 30 : 0;
    }

    /**
     * 票分倒计时
     *
     * @return
     */
    public int wanFa_PiaoFen_Time() {
        return MJTemplateRoomEnum.PiaoFen.NOT.equals(wanFa_PiaoFen()) ? 0 : 30;
    }

    /**
     * 闲加起手13张报听 玩法
     *
     * @return
     */
    public boolean isWanFa_TianTing() {
        return false;
    }


    /**
     * 打出一张能报听 玩法
     *
     * @return
     */
    public boolean isWanFa_BaoTing() {
        return false;
    }


    /**
     * 换张 同色玩法
     *
     * @return
     */
    public boolean isWanFa_ChangeCardType_SameColor() {
        return MJTemplateRoomEnum.ChangeCardType.SAME_COLOR.equals(wanFa_ChangeCardType());
    }

    /**
     * 换张 不同色
     *
     * @return
     */
    public boolean isWanFa_ChangeCardType_DifferentColor() {
        return MJTemplateRoomEnum.ChangeCardType.DIFFERENT_COLOR.equals(wanFa_ChangeCardType());
    }

    /**
     * 换张
     *
     * @return
     */
    public MJTemplateRoomEnum.ChangeCardType wanFa_ChangeCardType() {
        return MJTemplateRoomEnum.ChangeCardType.NOT;

    }

    /**
     * 换张 倒计时 单位:s
     *
     * @return
     */
    public int wanFa_ChangeCardType_Time() {
        return MJTemplateRoomEnum.ChangeCardType.NOT.equals(wanFa_ChangeCardType()) ? 0 : 30;
    }

    /**
     * 换张 顺序
     *
     * @return
     */
    public MJTemplateRoomEnum.ChangeCardOderBy wanFa_ChangeCardType_OrderBy() {
        return MJTemplateRoomEnum.ChangeCardOderBy.SHUN_SHI_ZHEN;
    }

    /**
     * 换张 张数
     *
     * @return
     */
    public int wanFa_ChangeCard_Num() {
        return MJTemplateRoomEnum.ChangeCardNum.THree.value;
    }

    /**
     * 客户端显示听牌的分
     *
     * @return
     */
    public boolean isWanFaShowTingHuPoint() {
        return false;
    }

    /**
     * 是否金牌不能打出
     *
     * @return
     */
    public boolean isWanFa_JinBuNengChu() {
        return false;
    }

    /**
     * 是否金牌不能吃碰杠
     * 默认金牌可以打就可以吃碰杠
     *
     * @return
     */
    public boolean isWanFa_JinBuKeChiPengGang() {
        return isWanFa_JinBuNengChu();
    }

    /**
     * 本玩法 客户端已经不支持，需要改成WaitingEx玩法，
     * 可以参考张家港麻将 ZJGMJ
     * 重写 步骤1： isWanFa_WaitingEx（）,保证这个玩法存在
     * 重写 步骤2： wanFa_PiaoFen（），
     * 重写 步骤3： addWaitingTypes（）
     * 重写 步骤4： isWanFa_PiaoFen（）改为false，不改不会影响游戏，但建议改
     *
     * @return
     */
    public boolean isWanFa_PiaoFen() {
        return !MJTemplateRoomEnum.PiaoFen.NOT.equals(wanFa_PiaoFen());
    }

    /**
     * 是否票分类型操作
     * 可以参考张家港麻将 ZJGMJ
     * 重写 步骤 看isWanFa_PiaoFen 注释
     *
     * @return
     */

    public boolean isWanFa_WaitingEx() {
        return false;
    }

    /**
     * 票分 飘花 下跑
     *
     * @return
     */
    public MJTemplateRoomEnum.PiaoFen wanFa_PiaoFen() {
        return MJTemplateRoomEnum.PiaoFen.NOT;
    }

    /**
     * 票分 飘花 下跑
     *
     * @return
     */
    public MJTemplateRoomEnum.PiaoFenNum wanFa_PiaoFenNum() {
        return MJTemplateRoomEnum.PiaoFenNum.NOT;
    }


    /**
     * 留牌可选
     *
     * @return
     */
    public MJTemplateRoomEnum.LiuPaiExtrasOptions wanFa_LiuPaiExtrasOptions() {
        return MJTemplateRoomEnum.LiuPaiExtrasOptions.NOT;
    }

    /**
     * 选中杠后留牌后 杠一次留几张牌
     *
     * @return
     */
    public MJTemplateRoomEnum.GangCardLiuPaiNum wanFa_GangCardLiuPaiNum() {
        return MJTemplateRoomEnum.GangCardLiuPaiNum.NOT;
    }

    /**
     * 留牌数量
     *
     * @return
     */
    public int wanFa_LiuPaiNum() {
        return 0;
    }

    /**
     * 海底捞月 玩法
     *
     * @return
     */
    public MJTemplateRoomEnum.HaiDiLaoYue wanFa_HaiDiLaoYue() {
        return MJTemplateRoomEnum.HaiDiLaoYue.NOT;
    }

    /**
     * 摇杠
     *
     * @return
     */
    public MJTemplateRoomEnum.YaoGang wanFa_YaoGang() {
        return MJTemplateRoomEnum.YaoGang.NOT;
    }

    public MJTemplateRoomEnum.MaiMa wanFa_MaiMa() {
        return MJTemplateRoomEnum.MaiMa.NOT;
    }


    /**
     * 跟庄人数
     *
     * @return
     */
    public int wanFa_GenZhuangPLayerNum() {
        return MJTemplateRoomEnum.GenZhuangPlayerNum.NOT.value;
    }

    public boolean isWanFa_MaiMa() {
        return !MJTemplateRoomEnum.MaiMa.NOT.equals(wanFa_MaiMa());
    }

    /**
     * 有啥添加啥 可参考cdxzmj 成都血战麻将
     *
     * @return
     */
    public List<MJTemplateRoomEnum.AutoOpType> wanFa_AutoTypeList() {
        return new ArrayList<>();
    }

    /**
     * 当前游戏的模式
     * 如果没有房间配置 例：return MJTemplateRoomEnum.MoShi.XUE_LIU
     *
     * @return
     */
    public MJTemplateRoomEnum.MoShi wanFa_MoShi() {
        return MJTemplateRoomEnum.MoShi.NORMAL;
    }

    /**
     * 血流模式
     *
     * @return
     */
    public boolean isWanFa_XueLiuMoShi() {
        return MJTemplateRoomEnum.MoShi.XUE_LIU.equals(wanFa_MoShi());
    }

    /**
     * 血战模式
     *
     * @return
     */
    public boolean isWanFa_XueZhanMoShi() {
        return MJTemplateRoomEnum.MoShi.XUE_ZHAN.equals(wanFa_MoShi());
    }

    /**
     * 开金选项
     * 如果没房间配置 例：return MJTemplateRoomEnum.KaiJinWanFa.BENSHEN_3ZHANG
     *
     * @return
     */
    public MJTemplateRoomEnum.KaiJinWanFa wanFa_KaiJin() {
        return MJTemplateRoomEnum.KaiJinWanFa.NOT;
    }


    /**
     * 机器人托管是否摸牌打牌
     * true:摸啥打啥
     * false:智能托管
     *
     * @return
     */
    @Override
    public boolean isMoDa() {
        return true;
    }

    /**
     * @param pid 玩家PID
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public GetRoomInfo getRoomInfo(long pid) {
        S_GetRoomInfo ret = new S_GetRoomInfo();
        // 设置房间公共信息
        this.getBaseRoomInfo(ret);
        if (null != this.getCurSet()) {
            ret.setSet(this.getCurSet().getNotify_set(pid));
        } else {
            MJTemplateRoomSetInfo roomSetInfo = new MJTemplateRoomSetInfo();
            if (isWanFa_PiaoFen()) {
                roomSetInfo.setPiaoFenList(new ArrayList<>());
            }
            if (isWanFa_WaitingEx()) {
                roomSetInfo.setWaitingExType("Not");
                roomSetInfo.setBiaoShiList(new ArrayList<>());
            }
            ret.setSet(roomSetInfo);
        }
        return ret;
    }

    /**
     * 清除记录。
     */
    @Override
    public void clearEndRoom() {
        super.clear();
        this.buHuaTypeSet.clear();
        this.buHuaTypeSet = null;
    }


    /**
     * 新一局
     */
    @Override
    public void startNewSet() {
        if (isWanFa_WaitingEx()) {
            List<MJTemplateRoomEnum.WaitingExType> waitingExTypes = addWaitingTypes();
            if (MJTemplateRoomEnum.PiaoFen.PIAO_FEN_EACH_SET.equals(wanFa_PiaoFen()) || (MJTemplateRoomEnum.PiaoFen.PIAO_FEN_FIRSER_SET.equals(wanFa_PiaoFen()) && getCurSetID() == 0)) {
                getRoomPosMgr().posList.forEach(k -> {
                    MJTemplateRoomPos roomPos = (MJTemplateRoomPos) k;
                    roomPos.setWaitingExInfo(new MJTemplateWaitingExInfo());
                    //默认-1  如果这个玩家不用操作 重写本方法，改为-2 （TJTGMJ又这个玩法）
                    waitingExTypes.forEach(a -> roomPos.setWaitingExInfoFieldValue(a, MJTemplateRoomEnum.WaitingExOpType.WAITING_OP.value));
                });
            } else {
                int value = wanFa_PiaoFenNum().value;
                if (value > -1 && getCurSetID() == 0) {
                    //固定飘第一局给值
                    getRoomPosMgr().posList.forEach(k -> {
                        MJTemplateRoomPos roomPos = (MJTemplateRoomPos) k;
                        roomPos.setWaitingExInfo(new MJTemplateWaitingExInfo());
                        waitingExTypes.forEach(a -> roomPos.setWaitingExInfoFieldValue(a, value));
                    });
                }
            }
        }
        Map<Long, Integer> piaoFenMap = new HashMap<>();
        //首轮票分 需要吧本轮票分数据 赋值给下轮位置
        if (this.getCurSet() != null && MJTemplateRoomEnum.PiaoFen.PIAO_FEN_FIRSER_SET.equals(wanFa_PiaoFen())) {
            if (!isWanFa_WaitingEx()) {
                piaoFenMap.putAll(((MJTemplateRoomSet) getCurSet()).getPiaoFenMap());
            }
        }
        this.setCurSetID(this.getCurSetID() + 1);
        // / 计算庄位
        if (this.getCurSetID() == 1) {
            setDPos(0);
        } else if (this.getCurSet() != null) {
            AbsMJSetRoom mRoomSet = (AbsMJSetRoom) this.getCurSet();
            // 根据上一局计算下一局庄家
            setDPos(mRoomSet.calcNextDPos());
            mRoomSet.clear();
        }
        // 每个位置，清空准备状态
        this.getRoomPosMgr().clearGameReady();
        // 通知局数变化
        this.getRoomTyepImpl().roomSetIDChange();
        this.setCurSet(this.newMJRoomSet(this.getCurSetID(), this, this.getDPos()));
        if (this.getCurSet() != null && MJTemplateRoomEnum.PiaoFen.PIAO_FEN_FIRSER_SET.equals(wanFa_PiaoFen()) && getCurSetID() > 1) {
            if (!isWanFa_WaitingEx()) {
                ((MJTemplateRoomSet) getCurSet()).getPosDict().values().stream().forEach(p -> ((MJTemplateSetPos) p).setPiaoFen(piaoFenMap.get(p.getPid())));
            }
        }
    }

    /**
     * 票分类型添加方法
     */
    public List<MJTemplateRoomEnum.WaitingExType> addWaitingTypes() {
        return new ArrayList<>();
    }

    /**
     * 30秒未准备自动退出
     *
     * @return
     */
    @Override
    public boolean is30SencondTimeOut() {
        return checkGaoJiXuanXiang(GaoJiTypeEnum.SECOND_TIMEOUT_30);
    }

    /**
     * 自动准备游戏 玩家加入房间时，自动进行准备。
     */
    @Override
    public boolean autoReadyGame() {
        return false;
    }

    public Set<Integer> getBuHuaTypeSet() {
        return buHuaTypeSet;
    }

    public void setBuHuaTypeSet(Set<Integer> buHuaTypeSet) {
        this.buHuaTypeSet = buHuaTypeSet;
    }

    public Class<?> getHuCardImpl() {
        return huCardImpl;
    }

    public void setHuCardImpl(Class<?> huCardImpl) {
        this.huCardImpl = huCardImpl;
    }

    /**
     * 是否存在漏碰
     *
     * @return
     */
    public boolean checkExistLouPeng() {
        return true;
    }

    /**
     * 最后一张只能炮胡，不能吃碰杠
     *
     * @return
     */
    public boolean checkExistLastCardOnlyJiePao() {
        return false;
    }


    /**
     * 客户端操作pass  本pass 与流程没有关系
     */
    public void opPass(WebSocketRequest request, long pid, CMJ_OpPass opPass, SMJ_OpPass msg) {
        if (null == this.getCurSet()) {
            request.error(ErrorCode.NotAllow, "");
            return;
        }
        if (opPass.opType == 0) {
            request.error(ErrorCode.NotAllow, "");
            return;
        }
        if (Objects.isNull(msg)) {
            request.error(ErrorCode.NotAllow, "");
            return;
        }
        MJTemplateRoomSet roomSet = (MJTemplateRoomSet) this.getCurSet();
        AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
        if (Objects.isNull(roomPos)) {
            return;
        }
        MJTemplateSetPos setPos = (MJTemplateSetPos) roomSet.getMJSetPos(roomPos.getPosID());
        if (setPos.clientOpPassFlag) {
            return;
        }
        setPos.setClientOpPassFlag(true);
        msg.make(getRoomID(), setPos.getPosID(), OpType.valueOf(opPass.opType), CommTime.nowMS());
        roomSet.getRoomPlayBack().addPlaybackList(msg, null);
    }


}
