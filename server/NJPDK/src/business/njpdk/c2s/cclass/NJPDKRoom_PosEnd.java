package business.njpdk.c2s.cclass;

import java.util.ArrayList;

/**
 * 资阳跑得快房间结束
 *
 * @author zaf
 */

// 位置结束的信息
public class NJPDKRoom_PosEnd {
    public int pos = 0; //位置
    public long pid = 0;//玩家pid
    public int point = 0; // 本局积分变更
    public int addDouble = 0;//加倍
    public int type;                    //0:没有 1:关牌 2：春天 3: 反春
    public ArrayList<Integer> surplusCardList = new ArrayList<Integer>();        //剩余牌数
    /**
     * 竞技点
     */
    public Double sportsPoint;
}
