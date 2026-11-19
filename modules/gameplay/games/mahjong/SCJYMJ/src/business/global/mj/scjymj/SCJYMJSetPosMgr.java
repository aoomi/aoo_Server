package business.global.mj.scjymj;

import business.global.karmicmj.KarMJSetPosMgr;
import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJOpPoint;
import cenum.mj.HuType;
import cenum.mj.MJCEnum;
import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.cclass.mj.NextOpType;
import jsproto.c2s.cclass.mj.OpTypeInfo;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 安岳麻将
 *
 * @author Administrator
 */
public class SCJYMJSetPosMgr extends KarMJSetPosMgr {

    public SCJYMJSetPosMgr(AbsMJSetRoom set) {
        super(set);
    }

    @Override
    public void startSetApplique() {
    }

    @Override
    public List<BaseMJSet_Pos> getAllPlayBackNotify() {
        List<BaseMJSet_Pos> setPosList = new ArrayList<BaseMJSet_Pos>();
        for (int i = 0; i < this.set.getRoom().getPlayerNum(); i++) {
            AbsMJSetPos setPos = this.set.getMJSetPos(i);
            if (null != setPos) {
                setPosList.add(setPos.getPlayBackNotify());
            }
        }
        return setPosList;
    }

    @Override
    public void clearChiList() {
        for (int i = 0; i < this.set.getRoom().getPlayerNum(); i++) {
            AbsMJSetPos setPos = this.set.getMJSetPos(i);
            if (null != setPos) {
                setPos.getPosOpNotice().clearChiList();
            }
        }
    }

    @Override
    public void checkOpType(int curOpPos, int curCardID, OpType opType) {
        // 清空所有动作类型操作
        this.cleanAllOpType();
        switch (opType) {
            case KouTing:
                this.checkBaoTing();
                break;
            case Out:
                checkOutOpType(curOpPos, curCardID);
                break;
            case Gang:
                check_QiangGangHu(curOpPos, curCardID);
                break;
            default:
                break;
        }
    }

    @Override
    public NextOpType exeCardAction(OpType opType) {
        NextOpType nOpType = null;
        switch (opType) {
            case Out:
            case Gang:
            case KouTing:
                nOpType = opAllMapOutCard();
                break;
            default:
                break;
        }
        return nOpType;
    }

    public void checkOutOpType(int curOpPos, int curCardID) {
        if (!check_otherDiHu(curOpPos, curCardID)) {
            // 检查玩家点炮胡
            check_otherPingHu(curOpPos, curCardID);
        }
        // 检查玩家接杠
        check_otherJieGang(curOpPos, curCardID);
        // 检查玩家碰
        check_otherPeng(curOpPos, curCardID);
    }

    /**
     * 是否能碰,这里处理了漏碰，不需要的话重写
     *
     * @param curCardID
     * @param nextPos
     * @param setPos
     */
    @Override
    public void dealWithPeng(int curCardID, int nextPos, AbsMJSetPos setPos) {
        int pengCard = curCardID / 100;
        // 是否重复牌类型
        if (!setPos.getPosOpRecord().isOpCardType2(pengCard)) {
            setPos.getPosOpRecord().setOpCardType(curCardID);
            this.addOpTypeInfo(nextPos, OpType.Peng);
        }
    }

    /**
     * 检测地胡
     *
     * @param curOpPos
     * @param curCardID
     * @return
     */
    public boolean check_otherDiHu(int curOpPos, int curCardID) {
        if (!isCanDiHu() || curOpPos != this.set.getDPos()) {
            return false;
        }
        AbsMJSetPos dSetPos = this.set.getMJSetPos(curOpPos);
        if (null == dSetPos) {
            return false;
        }
        if (dSetPos.sizeOutCardIDs() != 1) {
            return false;
        }
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            AbsMJSetPos setPos = this.set.getMJSetPos(nextPos);
            if (setPos.sizeOutCardIDs() > 0) {
                continue;
            }
            OpType oType = setPos.checkPingHu(curOpPos, curCardID);
            if (!OpType.Not.equals(oType)) {
                if (((SCJYMJRoomSet) this.set).getGangPos() == curOpPos) {
                    setPos.getPosOpRecord().addOpHuList(SCJYMJOpPoint.GSP);
                }
                // 清空杠上炮位置
                ((SCJYMJRoomSet) this.set).setGangPos(-1);
                dealWithDiHu(setPos, nextPos, oType);
            }
        }
        return true;
    }

    public void check_otherPingHu(int curOpPos, int curCardID) {
//        super.check_otherPingHu(curOpPos, curCardID);
        SCJYMJSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = (SCJYMJSetPos) this.set.getMJSetPos(nextPos);
            if (setPos.isHu() || !setPos.isCanHu()) {
                continue;
            }
            OpType oType = setPos.checkPingHu(curOpPos, curCardID);
            if (!OpType.Not.equals(oType)) {
                dealWithPingHu(setPos, nextPos, oType, curOpPos);
            }
        }
        // 清空杠上炮位置
        ((SCJYMJRoomSet) this.set).setGangPos(-1);
    }

    /**
     * 检查报听
     */
    private void checkBaoTing() {
        this.set.getPosDict().values().forEach(k -> k.calcHuFan());
        SCJYMJSetPos setPos;
        for (int i = 1; i < this.set.getPlayerNum(); i++) {
            int nextPos = (this.set.getDPos() + i) % this.set.getPlayerNum();
            setPos = (SCJYMJSetPos) this.set.getMJSetPos(nextPos);
            if (null == setPos || setPos.sizeHuCardTypes() <= 0) {
                continue;
            }
            this.addOpTypeInfo(setPos.getPosID(), OpType.KouTing);
        }
    }

    /**
     * 添加动作信息
     *
     * @param posId  位置
     * @param opType 动作类型
     */
    @Override
    protected void addOpTypeInfo(Integer posId, OpType opType) {
        if (null == this.opTypeInfoList) {
            this.opTypeInfoList = new ArrayList<>();
        }
        int count = this.opTypeInfoList.size();
        // 检查是否存在一炮多响
        if (this.checkExistYPDX()) {
            // 存在一炮多响
            if (!HuType.NotHu.equals(MJCEnum.OpHuType(opType))) {
                count = 0;
                this.huPosList.add(posId);
            }
        }
        if (OpType.KouTing.equals(opType)) {
            count = 0;
            this.huPosList.add(posId);
        }
        // 添加动作信息
        this.opTypeInfoList.add(new OpTypeInfo(count + 1, posId, opType));
    }

    public void addBaoTingInfo(Integer posId) {
        if (null == this.opTypeInfoList) {
            this.opTypeInfoList = new ArrayList<>();
        }
        int count = 0;
        this.huPosList.add(posId);
        // 添加动作信息
        this.opTypeInfoList.add(new OpTypeInfo(count + 1, posId, OpType.KouTing));
    }

    /**
     * 处理抢杠胡
     *
     * @param setPos
     * @param nextPos
     */
    public void dealWithQiangGangHu(AbsMJSetPos setPos, int nextPos) {
        setPos.getPosOpRecord().addOpHuList(SCJYMJOpPoint.QGHu);
        this.addOpTypeInfo(nextPos, OpType.JiePao);
    }

    /**
     * 处理地胡
     *
     * @param setPos
     * @param nextPos
     * @param oType
     */
    public void dealWithDiHu(AbsMJSetPos setPos, int nextPos, OpType oType) {
        this.addOpTypeInfo(nextPos, oType);
        if (((SCJYMJSetPos) setPos).isTing()) {
            setPos.getPosOpRecord().clearOpHuList();
            setPos.getPosOpRecord().addOpHuList(SCJYMJOpPoint.DiHu);
        }
    }

    /**
     * 处理平胡
     *
     * @param setPos
     * @param nextPos
     * @param oType
     * @param curOpPos
     */
    public void dealWithPingHu(AbsMJSetPos setPos, int nextPos, OpType oType, int curOpPos) {
        this.addOpTypeInfo(nextPos, oType);
        if (((SCJYMJRoomSet) this.set).getGangPos() == curOpPos) {
            setPos.getPosOpRecord().addOpHuList(SCJYMJOpPoint.GSP);
        }
    }

    /**
     * 是否能碰
     *
     * @param setPos
     * @param curCardID
     * @return
     */
    public boolean isCanPeng(AbsMJSetPos setPos, int curCardID) {
        SCJYMJSetPos aPos = ((SCJYMJSetPos) setPos);
        if (aPos.isTing() || setPos.isHu()) {
            return false;
        }
        return true;
    }

    /**
     * 是否能接杠
     *
     * @param setPos
     * @param curCardID
     * @return
     */
    public boolean isCanJieGang(AbsMJSetPos setPos, int curCardID) {
        return !setPos.isHu();
    }

    /**
     * 是否能地胡
     *
     * @return
     */
    public boolean isCanDiHu() {
        if (!this.set.getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.TianDiHu)) {
            // 没有勾选天地胡
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkExistPingHu() {
        return true;
    }

    @Override
    protected boolean checkExistJGBG() {
        return true;
    }

    @Override
    protected boolean checkExistChi() {
        return false;
    }

    @Override
    protected boolean checkExistYPDX() {
        return true;
    }


    /**
     * 执行动作类型信息
     *
     * @return
     */
    @Override
    public OpTypeInfo exeOpTypeInfo(Integer opPos, OpType opType) {
        return super.exeOpTypeInfo(opPos, opType);
    }

}
