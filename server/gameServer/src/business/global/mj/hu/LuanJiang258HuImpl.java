package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpPointEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 乱将胡：手中牌全部为258牌即可，无需组成胡牌牌型，无需258做将； 可吃碰杠；
 *
 * @author Huaxing
 */
public class LuanJiang258HuImpl extends BaseHuCard {
    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        return OpPointEnum.Hu.equals(luanJiang258Hu(mSetPos, mCardInit));
    }

    /**
     * 检查乱将258胡
     *
     * @param mSetPos   玩家位置信息
     * @param mCardInit 玩家牌信息
     * @return
     */
    protected OpPointEnum luanJiang258Hu(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        List<Integer> allInt = new ArrayList<>();
        // 获取牌列表
        allInt.addAll(mCardInit.getAllCardInts());
        if (mSetPos.getPublicCardList().size() > 0) {
            for (List<Integer> publicCards : mSetPos.getPublicCardList()) {
                allInt.addAll(publicCards.subList(3, publicCards.size()).stream().map(p -> p / 100).collect(Collectors.toList()));
            }
        }

        // 如果存在风带则不能胡乱将
        if (allInt.stream().filter(k -> {
            if (k >= 1000) {
                return k >= 4000;
            } else {
                return k >= 40;
            }
        }).findAny().isPresent()) {
            return OpPointEnum.Not;
        }
        // 分组列表
        Map<Integer, Long> map = allInt.stream().filter(k -> {
            if (k >= 1000) {
                return k < 4000;
            } else {
                return k < 40;
            }
        }).collect(Collectors.groupingBy(p -> p >= 1000 ? (p / 100 % 10) : (p % 10), Collectors.counting()));
        // 检查分组数据
        if (null == map || map.size() <= 0) {
            return OpPointEnum.Not;
        }
        map.remove(2);
        map.remove(5);
        map.remove(8);
        if (map.size() <= 0) {
            return OpPointEnum.Hu;
        }
        return OpPointEnum.Not;
    }
}
