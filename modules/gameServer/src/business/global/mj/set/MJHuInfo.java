package business.global.mj.set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 麻将胡牌操作信息
 */
@Data
@NoArgsConstructor
public class MJHuInfo {
    /**
     * 胡牌位置
     */
    private Set<Integer> huPosList = Sets.newHashSet();
    /**
     * 自摸pos
     */
    private int huPos = -1;

    /**
     * 荒庄最后摸牌pos
     */
    private int huangPos = -1;

    /**
     * 胡位置字典
     * K:胡牌回合Id,V:胡牌位置列表
     */
    private Map<Integer, List<Integer>> huPosMap = new ConcurrentHashMap<>(3);

    /**
     * 不存在胡牌玩家
     *
     * @return
     */
    public boolean isHuEmpty() {
        return MapUtils.isEmpty(this.getHuPosMap());
    }

    /**
     * 存在胡牌玩家
     *
     * @return
     */
    public boolean isHuNotEmpty() {
        return MapUtils.isNotEmpty(this.getHuPosMap());
    }

    public void setHuPos(int huPos) {
        this.huPos = huPos;
        this.huPosList.add(huPos);
    }

    /**
     * 获取指定回合第一胡牌位置
     *
     * @return
     */
    public int getRoundFirstHuPos(int roundId) {
        if (this.getHuPosMap().containsKey(roundId)) {
            return this.getHuPosMap().get(roundId).get(0);
        }
        return -1;
    }


    /**
     * 获取回合胡牌列表
     *
     * @param roundId 回合Id
     * @return
     */
    public List<Integer> getRoundHuPostList(int roundId) {
        if (this.getHuPosMap().containsKey(roundId)) {
            return this.getHuPosMap().get(roundId);
        }
        return Collections.emptyList();
    }

    /**
     * 检查指定回合是否存在胡牌位置
     *
     * @param roundId 回合Id
     * @return
     */
    public boolean checkRoundExistHuPos(int roundId) {
        return this.getHuPosMap().containsKey(roundId);
    }

    /**
     * 增加胡牌位置
     *
     * @param roundId 回合Id
     * @param huPos   胡牌位置
     */
    public void addHuPos(int roundId, int huPos) {
        if (this.getHuPosMap().containsKey(roundId)) {
            if (!this.getHuPosMap().get(roundId).contains(huPos)) {
                // 追加本回合胡牌位置
                this.getHuPosMap().get(roundId).add(huPos);
            }
        } else {
            this.getHuPosMap().put(roundId, Lists.newArrayList(huPos));
        }
    }

    /**
     * 获取最大回合胡牌玩家列表
     *
     * @return
     */
    public List<Integer> getRoundMaxHuPosList() {
        return this.getHuPosMap().entrySet().stream().max((p1, p2) -> p1.getKey().compareTo(p2.getKey())).map(k -> k.getValue()).orElse(Collections.emptyList());
    }

    /**
     * 获取最小回合胡牌玩家列表
     *
     * @return
     */
    public List<Integer> getRoundMinHuPosList() {
        return this.getHuPosMap().entrySet().stream().min((p1, p2) -> p1.getKey().compareTo(p2.getKey())).map(k -> k.getValue()).orElse(Collections.emptyList());
    }

}
