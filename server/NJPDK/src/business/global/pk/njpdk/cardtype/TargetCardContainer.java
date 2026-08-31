package business.global.pk.njpdk.cardtype;

import java.util.ArrayList;
import java.util.List;

/**
 * 目标牌容器
 */
public class TargetCardContainer {
    public int cardValue;
    public List<Integer> bodyList = new ArrayList<>();//主体区
    public List<Integer> tailKeyList = new ArrayList<>();//带牌区的key
    public List<Integer> tailList = new ArrayList<>();//带牌区
    public int planeLength = -1;//飞机长度(333444=2)(333444555=3)
    public int planeCompareValue = 0;//飞机比较值(333444=4)

    void setCard(int cardValue, List<Integer> bodyList) {
        this.cardValue = cardValue;
        this.bodyList = bodyList;
    }

    void setPlane(int planeLength, List<Integer> bodyList, int planeCompareValue) {
        this.planeLength = planeLength;
        this.bodyList.addAll(bodyList);
        this.planeCompareValue = planeCompareValue;
    }
}
