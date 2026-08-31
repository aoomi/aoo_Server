package business.global.pk.njpdk.cardtype;

import java.util.ArrayList;
import java.util.List;
import com.ddm.server.common.semantic.IdentitySemantics;

/**
 * 安岳跑的快牌型算法参数
 */
/** Mutable candidate used only as an identity key inside one algorithm invocation. */
@IdentitySemantics
public class NJPDKALGParameter {
    //---------------输入参数-------------------
    //最小顺子张数
    public int minStraightNumber;
    //手上剩余牌数
    public int leftCardSize = 16;
    //本回合牌列表
    private ArrayList<Integer> cardList;
    //上回合操作类型
    public int previousOpType;
    //上回合牌列表
    public ArrayList<Integer> previousCardList;
    //---------------输出参数------------------
    //牌列表
    public List<Integer> outCardList = new ArrayList<>();
    //牌类型
    public int outCardType = 0;
    //带牌张数
    public int outTailNumber;
    //权重
    public int weight;

    /**
     * 获取克隆牌型
     *
     * @return
     */
    public ArrayList<Integer> getCloneCardList() {
        return (ArrayList<Integer>) cardList.clone();
    }

    /**
     * 获取原来手牌
     *
     * @return
     */
    public ArrayList<Integer> getCardList() {
        return cardList;
    }

    /**
     * 设置输入参数
     *
     * @param previousCardList
     * @param previousOpType
     * @param cardsList
     * @param cardSize
     */
    public void setInputParameter(ArrayList<Integer> previousCardList, int previousOpType, ArrayList<Integer> cardsList, int cardSize) {
        this.leftCardSize = cardSize;
        this.previousCardList = previousCardList;
        this.previousOpType = previousOpType;
        this.cardList = cardsList;
    }

    /**
     * 设置输出参数
     *
     * @param outCardType
     * @param outTailNumber
     * @param outCardList
     */
    public void setOutPutParameter(int outCardType, int outTailNumber, List<Integer> outCardList) {
        this.outCardType = outCardType;
        this.outTailNumber = outTailNumber;
        this.outCardList = outCardList;
    }
}
