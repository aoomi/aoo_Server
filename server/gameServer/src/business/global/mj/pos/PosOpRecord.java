package business.global.mj.pos;

import cenum.mj.MJSpecialEnum;
import cenum.mj.OpType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 位置内部操作记录
 * 记录内容：
 * 漏碰、漏胡、托管\机器人（用户可操作的动作）、操作列表、补花列表.
 *
 * @author Administrator
 */
public class PosOpRecord {
    /**
     * 漏碰等动作记录
     */
    private List<Integer> opCardTypeList = new ArrayList<Integer>();
    /**
     * 漏胡列表
     */
    private List<Integer> huCardTypeList = new ArrayList<Integer>();
    /**
     * 操作胡列表
     */
    private List<Object> opHuList = new ArrayList<>();
    /**
     * 用户可操作的动作
     */
    private List<OpType> opList = new ArrayList<>();
    /**
     * 花列表
     */
    private List<Integer> huaList = new ArrayList<Integer>(8);

    /**
     * 清空
     */
    public void clear() {
        // 漏碰等动作记录
        this.opCardTypeList.clear();
        this.opCardTypeList = null;
        // 漏胡列表
        this.huCardTypeList.clear();
        this.huCardTypeList = null;
        // 操作胡列表
        this.opHuList.clear();
        this.opHuList = null;
        // 花列表
        this.huaList.clear();
        this.huaList = null;
        // 用户可操作的动作
        this.opList.clear();
        this.opList = null;
    }

    /**
     * 获取动作列表
     *
     * @return
     */
    public List<OpType> getOpList() {
        return this.opList;
    }

    /**
     * 清空动作列表
     */
    public void cleanOpList() {
        this.opList.clear();
    }

    /**
     * 设置动作列表
     *
     * @param tmp
     */
    public void setOpList(List<OpType> tmp) {
        this.opList = new ArrayList<>();
        if (null == tmp || tmp.size() <= 0) {
            return;
        }
        this.opList.addAll(tmp);
    }

    /**
     * 是否重复牌类型
     *
     * @param opCardType
     * @return
     */
    public boolean isOpCardType(int opCardType) {
        if (this.opCardTypeList.contains(opCardType)) {
            return true;
        }
        return false;
    }

    /**
     * 是否是重复牌类型，opCardTypeList存放cardID，不是type
     *
     * @param opCardType
     * @return
     */
    public boolean isOpCardType2(int opCardType) {
        if (this.opCardTypeList.stream().anyMatch(n -> n.intValue() / 100 == opCardType)) {
            return true;
        }
        return false;
    }

    /**
     * 设置牌类型
     *
     * @param opCardType
     */
    public void setOpCardType(int opCardType) {
        this.opCardTypeList.add(opCardType);
    }

    /**
     * 清空牌类型
     */
    public void clearOpCardType() {
        this.opCardTypeList.clear();
    }

    /**
     * 移除漏碰的牌
     *
     * @param opCardType
     */
    public void removeOpCardType(Integer opCardType) {
        this.opCardTypeList.remove(opCardType);
    }

    /**
     * 是否重复胡相同类型
     *
     * @param opCardType
     * @return
     */
    public boolean isHuCardType(int opCardType) {
        if (this.huCardTypeList.contains(opCardType)) {
            return true;
        }
        return false;
    }

    /**
     * 设置相同胡牌类型
     *
     * @param opCardType
     */
    public void setHuCardType(int opCardType) {
        this.huCardTypeList.add(opCardType);
    }

    /**
     * 获取重复胡的牌
     *
     * @return
     */
    public List<Integer> getHuCardTypeList() {
        return huCardTypeList;
    }

    /**
     * 清空相同胡牌类型
     */
    public void clearHuCardType() {
        this.huCardTypeList.clear();
    }

    /**
     * 操作胡列表
     *
     * @return
     */
    public List<Object> getOpHuList() {
        return opHuList;
    }

    public void setOpHuList(List<Object> opHuList) {
        this.opHuList = opHuList;
    }

    /**
     * 添加操作胡
     *
     * @param opHuType
     */
    public void addOpHuList(Object opHuType) {
        this.opHuList.add(opHuType);
    }

    /**
     * 添加操作胡
     * =
     */
    public void addOpHuList(Collection c) {
        this.opHuList.addAll(c);
    }

    /**
     * 移除指定操作胡
     *
     * @param opHuType 操作胡类型
     */
    public void removeOpHuList(Object opHuType) {
        this.opHuList.remove(opHuType);
    }

    /**
     * 清空操作胡列表
     */
    public void clearOpHuList() {
        this.opHuList.clear();
    }

    /**
     * 添加花
     *
     * @param cardId 牌ID
     */
    public void addHua(int cardId) {
        int cardType = cardId / 100;
        if (cardType > MJSpecialEnum.NOT_HUA.value()) {
            this.huaList.add(cardId);
        }
    }

    /**
     * 添加花
     *
     * @param cardId 牌ID
     */
    public void addHua(int cardId, int huaType) {
        int cardType = cardId / 100;
        if (cardType >= huaType) {
            this.huaList.add(cardId);
        }
    }

    /**
     * 花数量
     *
     * @return
     */
    public int sizeHua() {
        return this.huaList.size();
    }

    /**
     * 花列表
     *
     * @return
     */
    public List<Integer> getHuaList() {
        return this.huaList;
    }

    public List<Integer> getOpCardTypeList() {
        return opCardTypeList;
    }


}
