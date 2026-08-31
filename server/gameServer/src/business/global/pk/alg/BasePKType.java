package business.global.pk.alg;

import java.util.ArrayList;
import java.util.List;

/**
 * 目标牌容器
 */
public class BasePKType {
    public int cardValue;
    public List<Integer> bodyList = new ArrayList<>();//主体区
    public List<Integer> tailList = new ArrayList<>();//带牌区
    public int tripleNum = -1;//3带个数（仅限飞机牌型使用）
    public int compareValue = 0;//比较值

    public void setBodyList(int cardValue, List<Integer> bodyList) {
        this.cardValue = cardValue;
        this.bodyList = bodyList;
    }

    public void setPlane(int tripleNum, List<Integer> bodyList, int compareValue) {
        this.tripleNum = tripleNum;
        this.bodyList.addAll(bodyList);
        this.compareValue = compareValue;
    }
}
