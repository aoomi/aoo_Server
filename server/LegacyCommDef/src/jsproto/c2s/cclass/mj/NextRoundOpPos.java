package jsproto.c2s.cclass.mj;

import cenum.mj.OpType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下回合玩家位置
 *
 * @param <T>
 * @author Huaxing
 */
@Data
@NoArgsConstructor
public class NextRoundOpPos<T> {
    private T fromRound; // 来源于round的后续
    private List<Integer> opPos;
    // 可接受的操作
    private Map<Integer, List<OpType>> getPosOpTypeListMap = new ConcurrentHashMap<>(4);


    public NextRoundOpPos(Map<Integer, List<OpType>> getPosOpTypeListMap, T fromRound) {
        this.opPos = new ArrayList<>();
        this.opPos.addAll(getPosOpTypeListMap.keySet());
        this.getPosOpTypeListMap = getPosOpTypeListMap;
        this.fromRound = fromRound;
    }


    public void clear() {
        this.fromRound = null;

        if (null != this.opPos) {
            this.opPos.clear();
            this.opPos = null;
        }
        if (null != this.getPosOpTypeListMap) {
            this.getPosOpTypeListMap.clear();
            this.getPosOpTypeListMap = null;
        }
    }

}