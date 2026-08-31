package business.scjymj.c2s.cclass;

import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;

import java.util.HashMap;
import java.util.Map;

/**
 * 红中麻将 配置
 *
 * @author Clark
 */
// 一局中各位置的信息
public class SCJYMJSet_Pos extends BaseMJSet_Pos {
    private OpType dingQue = OpType.Not;
    private int value = -1;
    private boolean isTing;
    private Map<OpType, Integer> huMap = new HashMap<>();

    public OpType getDingQue() {
        return dingQue;
    }

    public void setDingQue(OpType dingQue) {
        this.dingQue = dingQue;
    }

    public int getPiao() {
        return value;
    }

    public void setPiao(int piao) {
        this.value = piao;
    }

    public void setTing(boolean ting) {
        this.isTing = ting;
    }


    public void addHuMap(OpType opType, int huCount) {
        huMap.put(opType, huCount);
    }
}
