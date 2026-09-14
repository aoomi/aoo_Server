package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.MJCardCfg;
import cenum.mj.OpPointEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 	混一色：手上牌全部是同一花色（万筒条）和字牌，且需要组成胡牌牌型；
 */
public class HunYiSeImpl extends BaseHuCard {
    /**
     * 检查胡牌返回
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return OpPointEnum.Not;
        }
        OpPointEnum oEnum = checkHYS(mSetPos, mCardInit);
        if (OpPointEnum.Not.equals(oEnum)) {
            return oEnum;
        }
        return OpPointEnum.HYS;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return OpPointEnum.HYS.equals(checkHuCardReturn(mSetPos, mCardInit));
    }


    /**
     * 检查一色
     *
     * @param mSetPos   玩家位置信息
     * @param mCardInit 玩家牌信息
     * @return
     */
    protected OpPointEnum checkHYS(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        List<Integer> allInt = new ArrayList<>();
        // 获取牌列表
        allInt.addAll(mCardInit.getAllCardInts());
        // 获取顺子，刻子，杠组成的胡牌。
        allInt.addAll(mSetPos.publicCardTypeList());
        // 分组列表
        Map<Integer, Long> map = allInt.stream().collect(Collectors.groupingBy(p -> p >= 1000 ? (p / 1000) : (p / 10), Collectors.counting()));
        // 检查分组数据
        if (null == map || map.size() <= 0) {
            return OpPointEnum.Not;
        }
        int size = map.size();
        // 获取分数数
        if (size == 2) {
            // 移除花牌
            map.remove(MJCardCfg.HUA.value());
            // 移除空牌
            map.remove(MJCardCfg.NOT.value());
            // 检查是否还有牌
            if (map.size() == 2 && map.containsKey(MJCardCfg.FENG.value())) {
                return OpPointEnum.Hu;
            }
        }
        return OpPointEnum.Not;
    }

}
