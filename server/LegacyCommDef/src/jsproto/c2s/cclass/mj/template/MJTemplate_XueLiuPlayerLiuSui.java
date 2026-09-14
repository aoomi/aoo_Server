package jsproto.c2s.cclass.mj.template;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 血流模式的流水
 * 没胡一次算一次
 */
@Data
public class MJTemplate_XueLiuPlayerLiuSui {
    public int posID;//玩家自己位置
    public int cardType;//得分的牌
    public int point;//输赢分
    public List<Integer> duiXiangList = new ArrayList<>();//出分对像
    public Map<Object, Integer> huTypeMap = new HashMap<>();

    public MJTemplate_XueLiuPlayerLiuSui(int posID) {
        this.posID = posID;
    }

    public MJTemplate_XueLiuPlayerLiuSui(int posID, Map<Object, Integer> huTypeMap) {
        this.posID = posID;
    }

    public MJTemplate_XueLiuPlayerLiuSui(int posID, int cardType, int point, List<Integer> deductPointPosList, Map<Object, Integer> huTypeMap) {
        this.posID = posID;
        this.cardType = cardType;
        this.point = point;
        this.duiXiangList.addAll(deductPointPosList);
        this.huTypeMap.putAll(huTypeMap);
    }

    public void addPoint(int point) {
        this.point += point;
    }
}
