package business.global.mj;

import java.util.ArrayList;
import java.util.List;

/**
 * 麻将牌的初始信息
 *
 * @author Administrator
 */
public class MJCardInit implements Cloneable {
    // 牌列表
    private List<Integer> allCardInts = new ArrayList<Integer>();
    // 宝牌列表
    private List<Integer> jins = new ArrayList<Integer>();
    // 最后出的牌
    private Integer lastOutCard = 0;

    /**
     * 添加牌列表
     *
     * @param allCardInts 添加所有牌列表
     * @return
     */
    public boolean addAllCardInts(List<Integer> allCardInts) {
        return null != allCardInts ? this.allCardInts.addAll(allCardInts) : false;
    }

    /**
     * 添加牌列表
     *
     * @param cardType 类型
     * @return
     */
    public boolean addCardInts(int cardType) {
        return cardType > 0 ? this.allCardInts.add(cardType) : false;
    }

    /**
     * 添加所有宝牌列表
     *
     * @param jins 宝牌列表
     * @return
     */
    public boolean addAllJins(List<Integer> jins) {
        return null != jins ? this.jins.addAll(jins) : false;
    }


    /**
     * 添加宝牌列表
     *
     * @param cardType 牌类型
     * @return
     */
    public boolean addJins(int cardType) {
        return cardType > 0 ? this.jins.add(cardType) : false;
    }

    public List<Integer> getAllCardInts() {
        return allCardInts;
    }

    public List<Integer> getJins() {
        return jins;
    }

    public MJCardInit() {

    }

    public MJCardInit(List<Integer> allCardInts, int type) {
        super();
        this.allCardInts = new ArrayList<Integer>();
        this.allCardInts.addAll(allCardInts);
        if (type > 0) {
            this.allCardInts.add(type);
        }
    }

    public MJCardInit(List<Integer> allCardInts, List<Integer> jins, Integer lastOutCard) {
        this.allCardInts.addAll(allCardInts);
        this.jins.addAll(jins);
        this.lastOutCard = lastOutCard;
    }

    /**
     * 获取所有牌数
     *
     * @return
     */
    public int sizeAllCardInts() {
        return null != this.allCardInts ? this.allCardInts.size() : 0;
    }


    /**
     * 获取宝牌数
     *
     * @return
     */
    public int sizeJin() {
        return null != this.jins ? this.jins.size() : 0;
    }

    public Integer getLastOutCard() {
        return lastOutCard;
    }

    public void setLastOutCard(Integer lastOutCard) {
        this.lastOutCard = lastOutCard;
    }

    public MJCardInit clone() {
        return new MJCardInit(new ArrayList<>(this.allCardInts), new ArrayList<>(this.jins), this.lastOutCard);
    }

    @Override
    public String toString() {
        return "MJCardInit{" +
                "allCardInts=" + allCardInts +
                ", jins=" + jins +
                ", lastOutCard=" + lastOutCard +
                '}';
    }
}
