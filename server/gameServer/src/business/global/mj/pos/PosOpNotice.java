package business.global.mj.pos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 位置内部操作通知
 * 记录内容： 吃列表、不能出列表、打出每张牌可听的牌列表。
 * 该类记录的内容，会随着摸牌、打牌、回合开始等操作动作下发给玩家。
 *
 * @author Administrator
 */
public class PosOpNotice {
    /**
     * 吃列表
     */
    private List<List<Integer>> chiList = new ArrayList<List<Integer>>(3);
    /**
     * 指定不能出的牌列表
     */
    private List<Integer> buNengChuList = new ArrayList<Integer>(17);
    /**
     * 打出每张牌可听的牌列表
     */
    private HashMap<Integer, List<Integer>> tingCardMap = new HashMap<>(17);

    /**
     * 清空
     */
    public void clear() {
        // 吃列表
        this.chiList.clear();
        this.chiList = null;
        // 指定不能出的牌列表
        this.buNengChuList.clear();
        this.buNengChuList = null;
        // 打出每张牌可听的牌列表。
        this.tingCardMap.clear();
        this.tingCardMap = null;
    }


    /**
     * 设置吃列表
     *
     * @param chis 吃列表
     */
    public void setChiList(List<List<Integer>> chis) {
        this.chiList = chis;
    }

    /**
     * 获取吃列表
     *
     * @return
     */
    public List<List<Integer>> getChiList() {
        return this.chiList;
    }

    /**
     * 清空吃列表
     */
    public void clearChiList() {
        this.chiList.clear();
    }


    /**
     * 听牌列表
     *
     * @param cardType 牌型
     * @param tingList 听列表
     */
    public void addTingCardList(int cardType, List<Integer> tingList) {
        this.tingCardMap.put(cardType, tingList);
    }

    /**
     * 清空-打出每张牌可听的牌列表。
     */
    public void clearTingCardMap() {
        this.tingCardMap.clear();
    }

    /**
     * 打出每张牌可听的牌列表。
     *
     * @return
     */
    public HashMap<Integer, List<Integer>> getTingCardMap() {
        return tingCardMap;
    }

    /**
     * 不能出牌的列表
     *
     * @return
     */
    public List<Integer> getBuNengChuList() {
        return this.buNengChuList;
    }

    /**
     * 添加牌型
     */
    public void addBuNengChuList(int cardType) {
        if (!this.buNengChuList.contains(cardType)) {
            this.buNengChuList.add(cardType);
        }
    }

    /**
     * 清空不能出的牌
     */
    public void clearBuNengChuList() {
        buNengChuList.clear();
    }

    /**
     * 设置不能出的牌
     *
     * @param buNengChuList 要设置的 buNengChuList
     */
    public void setBuNengChuList(List<Integer> buNengChuList) {
        this.buNengChuList = buNengChuList;
    }

}
