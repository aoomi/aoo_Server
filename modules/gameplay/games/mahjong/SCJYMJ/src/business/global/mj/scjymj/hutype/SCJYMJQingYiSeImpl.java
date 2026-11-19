package business.global.mj.scjymj.hutype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.hu.BaseHuCard;
import business.global.mj.hu.NormalHuCardImpl;
import business.global.mj.hu.PPHuCardImpl;
import business.global.mj.manage.MJFactory;
import business.global.mj.scjymj.SCJYMJRoomEnum;
import business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJOpPoint;
import business.global.mj.util.HuUtil;
import cenum.mj.MJCardCfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 清一色：胡牌时手牌全是一色的牌（万筒条），
 *
 * @author Huaxing
 */
public class SCJYMJQingYiSeImpl extends BaseHuCard {

    /**
     * 检查胡牌返回
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return SCJYMJOpPoint.Not;
        }
        SCJYMJOpPoint oEnum = checkQYS(mSetPos, mCardInit);
        if (SCJYMJOpPoint.Not.equals(oEnum)) {
            if (mSetPos.getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.YaoJiu) && checkAllYaoJiuCard(mSetPos, mCardInit)) {
                return SCJYMJOpPoint.YaoJiu;
            }
            return oEnum;
        }
        oEnum = returnHuCardType(mSetPos, mCardInit);
        if (oEnum != SCJYMJOpPoint.Not && mSetPos.getRoom().RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.YaoJiu) && checkAllYaoJiuCard(mSetPos, mCardInit)) {
            return SCJYMJOpPoint.QYaoJiu;
        }
        // 返回胡牌类型
        return returnHuCardType(mSetPos, mCardInit);
    }

    public SCJYMJOpPoint returnHuCardType(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        SCJYMJOpPoint oPoint = (SCJYMJOpPoint) MJFactory.getHuCard(SCJYMJDDHuCardImpl.class).checkHuCardReturn(mSetPos, mCardInit);
        if (SCJYMJOpPoint.LongQiDuiHu.equals(oPoint)) {
            return SCJYMJOpPoint.QYSLongQiDuiHu;
        } else if (SCJYMJOpPoint.QiDuiHu.equals(oPoint)) {
            return SCJYMJOpPoint.QYSQiDuiHu;
        }
        if (MJFactory.getHuCard(PPHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return SCJYMJOpPoint.QYSDDHu;
        }
        if (MJFactory.getHuCard(NormalHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return SCJYMJOpPoint.QYS;
        }
        return SCJYMJOpPoint.Not;

    }


    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return SCJYMJOpPoint.QYS.equals(checkHuCardReturn(mSetPos, mCardInit));
    }


    /**
     * 检查一色
     *
     * @param mSetPos   玩家位置信息
     * @param mCardInit 玩家牌信息
     * @return
     */
    protected SCJYMJOpPoint checkQYS(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        List<Integer> allInt = new ArrayList<>();
        // 获取牌列表
        allInt.addAll(mCardInit.getAllCardInts());
        // 获取顺子，刻子，杠组成的胡牌。
        allInt.addAll(mSetPos.publicCardTypeList());
        // 分组列表
        Map<Integer, Long> map = allInt.stream().collect(Collectors.groupingBy(p -> p >= 1000 ? (p / 1000) : (p / 10), Collectors.counting()));
        // 检查分组数据
        if (null == map || map.size() <= 0) {
            return SCJYMJOpPoint.Not;
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
                return SCJYMJOpPoint.PingHu;
            }
        }
        return SCJYMJOpPoint.Not;
    }

    /**
     * 全幺：胡牌玩家手牌、碰杠区的牌，每组牌都有幺牌；
     * 幺牌：1和9万/条/筒；
     * 顺子带幺九也算，例如123、789也满足条件；
     *
     * @param mCardInit
     * @param mSetPos
     * @return
     */
    private boolean checkAllYaoJiuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (mSetPos.publicCardTypeList().stream().anyMatch(k -> !checkYaoJiu(k))) {
            return false;
        }
        List<String> resultList = HuUtil.getInstance().findHuTypeList(mCardInit.getAllCardInts(), mCardInit.sizeJin());
        boolean math = resultList.stream().anyMatch(con ->
                Arrays.stream(con.split(":")).allMatch(k ->
                        Arrays.stream(k.split(",")).map(s -> Integer.parseInt(s.trim())).collect(Collectors.toList()).
                                stream().anyMatch(this::checkYaoJiu)));
        return math;
    }


    private boolean checkYaoJiu(Integer k) {
        if (k > 100) {
            k = k / 100;
        }
        if (k > 40) {
            return false;
        }
        return k % 10 == 1 || k % 10 == 9;
    }
}
