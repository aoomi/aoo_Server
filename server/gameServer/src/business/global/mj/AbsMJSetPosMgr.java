package business.global.mj;

import cenum.mj.HuType;
import cenum.mj.MJCEnum;
import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.cclass.mj.NextOpType;
import jsproto.c2s.cclass.mj.OpTypeInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class AbsMJSetPosMgr {
    /**
     * 当局信息
     */
    protected AbsMJSetRoom set;
    /**
     * 动作信息列表
     */
    public List<OpTypeInfo> opTypeInfoList = Collections.synchronizedList(new ArrayList<>());

    // 动作存储的值
    protected Map<OpType, Map<Integer, Integer>> opValueMap = new ConcurrentHashMap<>();
    /**
     * 记录胡牌玩家位置列表
     */
    protected List<Integer> huPosList = new ArrayList<>(4);

    public AbsMJSetPosMgr(AbsMJSetRoom set) {
        this.set = set;
    }

    public List<OpTypeInfo> getOpTypeInfoList() {
        return opTypeInfoList;
    }

    /**
     * 开局补花
     */
    public abstract void startSetApplique();

    /**
     * 清空信息
     */
    public void clear() {
        this.set = null;
        this.opTypeInfoList = null;
        this.opValueMap = null;
        this.huPosList = null;
    }

    /**
     * 检查动作类型。
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     * @param opType    动作类型
     */
    public void checkOpType(int curOpPos, int curCardID, OpType opType) {
        // 清空所有动作类型操作
        this.cleanAllOpType();
        switch (opType) {
            case Out:
                checkOutOpType(curOpPos, curCardID);
                break;
            case Gang:
                checkOpTypeQGH(curOpPos, curCardID);
                break;
            default:
                break;
        }
    }

    /**
     * 下个动作类型。
     *
     * @param opType
     * @return
     */
    public NextOpType exeCardAction(OpType opType) {
        NextOpType nOpType = null;
        switch (opType) {
            case Out:
            case Gang:
                nOpType = opAllMapOutCard();
                break;
            default:
                break;
        }
        return nOpType;
    }

    /**
     * 获取所有玩家的牌 所有玩家的回放记录。
     *
     * @return
     */
    public List<BaseMJSet_Pos> getAllPlayBackNotify() {
        return this.set.getPosDict().values().stream().filter(k -> null != k).map(k -> k.getPlayBackNotify()).collect(Collectors.toList());
    }

    /**
     * 清空吃牌列表
     */
    public void clearChiList() {
        // 检查是否存在吃牌。
        if (!this.checkExistChi()) {
            // 不存在。
            return;
        }
        AbsMJSetPos setPos = null;
        for (int i = 0; i < this.set.getRoom().getPlayerNum(); i++) {
            setPos = this.set.getMJSetPos(i);
            if (null != setPos) {
                // 清空位置吃列表记录。
                setPos.getPosOpNotice().clearChiList();
            }
        }
    }

    /**
     * 打金
     *
     * @param curCardID
     * @return
     */
    protected boolean outJin(int curCardID) {
        return this.set.getmJinCardInfo().checkJinExist(curCardID / 100);
    }

    /**
     * 清空所有动作类型操作
     */
    public void cleanAllOpType() {
        this.opTypeInfoList.clear();
        this.opValueMap.clear();
        this.huPosList.clear();
    }

    /**
     * 操作打出的牌
     *
     * @return
     */
    protected NextOpType opAllMapOutCard() {
        if (CollectionUtils.isEmpty(this.opTypeInfoList)) {
            return null;
        }


        // 分组 位置记录的动作列表。
        Map<Integer, List<OpType>> posOpTypeListMap = this.opTypeInfoList.stream().collect(Collectors.groupingByConcurrent(OpTypeInfo::getPosId, Collectors.mapping(OpTypeInfo::getOpType, Collectors.toList())));
        if (MapUtils.isEmpty(posOpTypeListMap)) {
            return null;
        } else {
            return new NextOpType(posOpTypeListMap);
        }
    }

    /**
     * 检查抢杠胡。
     *
     * @param curOpPos  操作者位置
     * @param curCardID 操作牌
     */
    public void checkOpTypeQGH(int curOpPos, int curCardID) {
        this.check_QiangGangHu(curOpPos, curCardID);
    }

    /**
     * 检测可以抢杠胡的操作者
     *
     * @param curOpPos  操作者位置
     * @param curCardID 操作牌
     * @return
     */
    public abstract void check_QiangGangHu(int curOpPos, int curCardID);

    /**
     * 检查出牌后是否有人可以接手。
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    protected void checkOutOpType(int curOpPos, int curCardID) {
        check_otherPingHu(curOpPos, curCardID);
        check_otherJieGang(curOpPos, curCardID);
        check_otherPeng(curOpPos, curCardID);
        check_LowerChi(curOpPos, curCardID);
    }

    /**
     * 检查是否存在一炮多响
     *
     * @return
     */
    protected abstract boolean checkExistYPDX();

    /**
     * 检测其他人是否可以平胡
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    protected void check_otherPingHu(int curOpPos, int curCardID) {
        // 只能自摸
        if (!this.checkExistPingHu()) {
            return;
        }
        AbsMJSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = this.set.getMJSetPos(nextPos);
            OpType oType = setPos.checkPingHu(curOpPos, curCardID);
            if (!OpType.Not.equals(oType)) {
                this.addOpTypeInfo(nextPos, oType);
            }
        }
    }

    /**
     * 检查是否存在平胡
     *
     * @return
     */
    protected abstract boolean checkExistPingHu();

    /**
     * 检测其他人是否可以碰
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    protected void check_otherPeng(int curOpPos, int curCardID) {
        AbsMJSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = this.set.getMJSetPos(nextPos);
            // 检查碰动作
            if (setPos.checkOpType(curCardID, OpType.Peng)) {
                int pengCard = curCardID / 100;
                // 是否重复牌类型
                if (!setPos.getPosOpRecord().isOpCardType(pengCard)) {
                    setPos.getPosOpRecord().setOpCardType(pengCard);
                    this.addOpTypeInfo(nextPos, OpType.Peng);
                }
                return;
            }
        }
    }

    /**
     * 检测其他人是否可以接杠
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    protected void check_otherJieGang(int curOpPos, int curCardID) {
        AbsMJSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = this.set.getMJSetPos(nextPos);
            // 检查接杠动作
            if (setPos.checkOpType(curCardID, OpType.JieGang)) {
                this.addOpTypeInfo(nextPos, OpType.JieGang);
                if (!this.checkExistJGBG()) {
                    // 接杠时不可选择碰后再补杠。
                    this.set.getLastOpInfo().addBuGang(curCardID / 100, OpType.JieGang);
                }
                return;
            }
        }
    }

    /**
     * 检查是否存在接杠补杠 T:接杠时可选择碰后再补杠。
     *
     * @return
     */
    protected abstract boolean checkExistJGBG();

    /**
     * 检查下家 吃
     * c
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     * @return
     */
    protected void check_LowerChi(int curOpPos, int curCardID) {
        // 检查是否存在吃牌
        if (!this.checkExistChi()) {
            return;
        }
        int nextPos = (curOpPos + 1) % this.set.getRoom().getPlayerNum();
        AbsMJSetPos setPos = this.set.getMJSetPos(nextPos);
        if (setPos.checkOpType(curCardID, OpType.Chi)) {
            // 添加动作信息
            this.addOpTypeInfo(nextPos, OpType.Chi);
            return;
        }
    }

    /**
     * 检查是否存在吃牌
     * T:有吃牌，F:没有吃牌
     *
     * @return
     */
    protected abstract boolean checkExistChi();

    /**
     * 检查胡牌是否结束
     *
     * @return
     */
    public boolean checkHuEnd() {
        return this.huPosList.size() <= 0;
    }

    /**
     * 检查动作类型信息是否存在
     *
     * @return T 存在，F:不存在
     */
    public boolean checkOpTypeInfoExist() {
        return CollectionUtils.isNotEmpty(this.opTypeInfoList);

    }


    /**
     * 添加动作信息
     *
     * @param posId  位置
     * @param opType 动作类型
     */
    protected void addOpTypeInfo(Integer posId, OpType opType) {
        if (null == this.opTypeInfoList) {
            this.opTypeInfoList = Collections.synchronizedList(new ArrayList<>());
            ;
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
        // 添加动作信息
        this.opTypeInfoList.add(new OpTypeInfo(count + 1, posId, opType));
    }

    /**
     * 执行动作类型信息
     *
     * @return
     */
    public OpTypeInfo exeOpTypeInfo(Integer opPos, OpType opType) {
        // 检查是否有动作列表
        if (null == this.opTypeInfoList || this.opTypeInfoList.size() <= 0) {
            return null;
        }
        this.opTypeInfoList.sort((OpTypeInfo itme1, OpTypeInfo itme2) -> {
            if (itme1.getId() - itme2.getId() == 0) {
                // 排名一样，通过一样的位置排序(当前操作位置最前面)
                if (itme1.getPosId() == opPos) {
                    return -1;
                }
                // 排名一样,未操作往前移
                return itme1.getType() - itme2.getType();
            }
            // 排名排序(123...456)
            return itme1.getId() - itme2.getId();
        });
        // 移除对应的胡位置列表
        this.huPosList.remove(opPos);
        boolean isPass = false;
        // 动作信息
        OpTypeInfo oInfo = null;
        for (int i = 0, size = this.opTypeInfoList.size(); i < size; i++) {
            oInfo = this.opTypeInfoList.get(i);
            if (null == oInfo) {
                continue;
            }
            // 是否当前操作玩家
            if (oInfo.getPosId() == opPos) {
                // 如果是操作指定动作
                if (opType.equals(oInfo.getOpType())) {
                    // 设置类型
                    oInfo.setType(2);
                    this.opTypeInfoList.set(i, oInfo);
                } else {
                    // 否则,过动作
                    // 设置类型
                    oInfo.setType(1);
                    this.opTypeInfoList.set(i, oInfo);
                }
            }

            if (!isPass) {
                if (oInfo.getType() == 0) {
                    // 第一个没操作
                    isPass = true;
                } else if (oInfo.getType() == 2) {
                    // 操作信息
                    return oInfo;
                }
            }
        }
        if (!isPass) {
            // 全部玩家操作完-并且都点了过。
            return new OpTypeInfo(opPos, OpType.Pass);
        } else {
            // 排名前的玩家存在没操作的。
            return new OpTypeInfo(-1, OpType.Not);
        }
    }

    /**
     * 检查动作值
     *
     * @param opType 动作类型
     * @return
     */
    public int opValue(OpType opType, int posId) {
        // 检查动作
        if (this.opValueMap.containsKey(opType)) {
            // 获取动作值
            if (this.opValueMap.get(opType).containsKey(posId)) {
                return this.opValueMap.get(opType).get(posId);
            }
        }
        // 返回默认值
        return 0;
    }

    /**
     * 设置动作值
     *
     * @param opType 动作类型
     * @param value  值
     */
    public void setOpValue(OpType opType, int posId, int value) {
        // 检查动作类型值是否存在
        if (this.opValueMap.containsKey(opType)) {
            // 记录动作值
            this.opValueMap.get(opType).put(posId, value);
        } else {
            Map<Integer, Integer> map = new HashMap<>();
            map.put(posId, value);
            this.opValueMap.put(opType, map);

        }
    }

    /**
     * 清空动作
     */
    public void clearOpTypeInfoList() {
        if (null != this.opTypeInfoList) {
            this.opTypeInfoList.clear();
        }
    }

    /**
     * 检查不存在动作列表
     *
     * @return
     */
    public boolean checkNotExistOpTypeInfoList() {
        return CollectionUtils.isEmpty(opTypeInfoList);
    }

    public void setOpTypeInfoList(List<OpTypeInfo> opTypeInfoList) {
        this.opTypeInfoList = opTypeInfoList;
    }
}
