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
 * 清一色：胡牌时手牌全是一色的牌（万筒条），
 *
 * @author Huaxing
 */
public class QingYiSeImpl extends BaseHuCard {

    /**
     * 检查胡牌返回
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return OpPointEnum.Not;
        }
        OpPointEnum oEnum = checkQYS(mSetPos, mCardInit);
        if (OpPointEnum.Not.equals(oEnum)) {
            return oEnum;
        }
        return OpPointEnum.QYS;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return OpPointEnum.QYS.equals(checkHuCardReturn(mSetPos, mCardInit));
    }


    /**
     * 检查一色
     *
     * @param mSetPos   玩家位置信息
     * @param mCardInit 玩家牌信息
     * @return
     */
    protected OpPointEnum checkQYS(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
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
        if (size == 1) {
            // 移除花牌
            map.remove(MJCardCfg.HUA.value());
            // 移除空牌
            map.remove(MJCardCfg.NOT.value());
            // 移除风牌，箭牌
            map.remove(MJCardCfg.FENG.value());
            // 检查是否还有牌
            if (map.size() == 1) {
                return OpPointEnum.Hu;
            }
        }
        return OpPointEnum.Not;
    }
}
