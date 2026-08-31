package jsproto.c2s.cclass.mj;

import cenum.mj.OpType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下一个操作类型
 *
 * @author Administrator
 */
public class NextOpType {
    // 可操作玩家的动作列表{位置ID:[动作列表]}
    private Map<Integer, List<OpType>> posOpTypeListMap = new ConcurrentHashMap<>(4);

    public NextOpType(Map<Integer, List<OpType>> posOpTypeListMap) {
        super();
        this.posOpTypeListMap = posOpTypeListMap;
    }

    public Map<Integer, List<OpType>> getPosOpTypeListMap() {
        return posOpTypeListMap;
    }

}
