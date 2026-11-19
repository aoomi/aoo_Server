package business.global.pk.alg.params;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhujianming
 * @date 2020-07-14 09:28
 * 算法抽象类型算法
 */
public class BasePKParameter {
    //剩余的手牌
    public int remainingCardSize = 17;
    //该玩家手上的所有牌
    public ArrayList<Integer> privateCardsList = new ArrayList<>();
    //本次的手牌
    private ArrayList<Integer> opTypeCardsList = new ArrayList<>();
    //本次的癞子替代牌
    public ArrayList<Integer> substituteCard = new ArrayList<>();
    //上次的操作类型
    public int previousOpType;
    //上次的牌
    public ArrayList<Integer> previousCardList = new ArrayList<>();
    //上次的对比值
    public int compareValue = 0;
    //上次的飞机长度或者顺子长度
    public int lastTripleNum = 0;

    //机器人部分
    public int weight;
    public Object robotInfo;

    //中间参数
    public int targetType;//目标类型

    /**
     * 获取克隆牌型
     *
     * @return 克隆牌
     */
    public ArrayList<Integer> getCloneOpTypeCardList() {
        return new ArrayList<>(opTypeCardsList);
    }

    public ArrayList<Integer> getClonePrivateCardsList() {
        return new ArrayList<>(privateCardsList);
    }

    /**
     * 获取手牌
     *
     * @return 手牌
     */
    public ArrayList<Integer> getOpTypeCardsList() {
        return opTypeCardsList;
    }

    /**
     * 初始化手牌，上回合操作牌
     *
     * @param previousCardList  上回合操作牌
     * @param previousOpType    上回合操作类型
     * @param opTypeCardsList   操作的牌，客户端操作的牌
     * @param remainingCardSize 剩余手牌张数
     */
    public void init(List<Integer> previousCardList, int previousOpType, int compareValue, int lastTripleNum, ArrayList<Integer> opTypeCardsList, int remainingCardSize) {
        this.remainingCardSize = remainingCardSize;
        this.previousCardList = new ArrayList<>(previousCardList);
        this.previousOpType = previousOpType;
        this.opTypeCardsList = opTypeCardsList;
        this.compareValue = compareValue;
        this.lastTripleNum = lastTripleNum;
    }

}
