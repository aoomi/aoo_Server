package business.global.mj.cdxzmj;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.cdxzmj.optype.CDXZMJDiHuImpl;
import business.global.mj.hu.DDHuCardImpl;
import business.global.mj.hu.MJTemplateTianHuImpl;
import business.global.mj.hu.PPHuCardImpl;
import business.global.mj.hu.QingYiSeImpl;
import business.global.mj.manage.MJFactory;
import business.global.mj.template.MJTemplateNormalHuCardImpl;
import business.global.mj.template.MJTemplateSetPos;
import business.global.mj.util.HuUtil;
import cenum.mj.OpPointEnum;
import cenum.mj.OpType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CDXZMJNormalHuCardImpl extends MJTemplateNormalHuCardImpl {


    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        mSetPos.getPosOpRecord().getOpHuList().clear();
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        //七对	
        OpPointEnum qidui = (OpPointEnum) MJFactory.getHuCard(DDHuCardImpl.class).checkHuCardReturn(mSetPos, mCardInit);
        boolean qys = MJFactory.getHuCard(QingYiSeImpl.class).checkHuCard(mSetPos, mCardInit);
        if (!qidui.equals(OpPointEnum.Not)) {
            //
            if (qys) {
                setPos.addOpPointEnum(qidui.equals(OpPointEnum.QDHu) ? OpPointEnum.QYSQD : OpPointEnum.QYSHDDHu);
            } else {
                setPos.addOpPointEnum(qidui.equals(OpPointEnum.QDHu) ? qidui : OpPointEnum.HDDHu);
            }
        } else {
            Map<Integer, Long> map = mCardInit.getAllCardInts().stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
           //带根：在玩家胡牌的手牌当中，有四张牌是一摸一样的（杠牌也算）；
            if (mSetPos.getPublicCardList().stream().anyMatch(k -> k.size() == 7) || map.values().stream().anyMatch(k -> k == 4)) {
                setPos.addOpPointEnum(OpPointEnum.GangPao);
            }
            if (MJFactory.getHuCard(PPHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
                //将对：玩家手上的牌是带二、五、八的对对胡，这样的牌型叫将对
                if (mCardInit.getAllCardInts().stream().allMatch(k -> check258(k)) &&
                        mSetPos.getPublicCardList().stream().allMatch(k -> k.get(0) != OpType.Chi.value() && check258(k.get(2) / 100))) {
                    setPos.addOpPointEnum(OpPointEnum.JYSPPH);
                } else if (qys) {
                    //清对：玩家手上的牌是清一色的对对胡
                    setPos.addOpPointEnum(OpPointEnum.QYSPPH);
                } else {
                    setPos.addOpPointEnum(OpPointEnum.PPH);
                }
            }
        }
        //幺九：在玩家手上的牌当中，全部是用1的连牌或者9的连牌组成的牌叫做幺九
        //牌型如：一一一二二二三三三万七八九九九筒；
        if (checkAllYaoJiuCard(mSetPos, mCardInit)) {
            if (qys) {
                setPos.addOpPointEnum(OpPointEnum.QingYaoJiu);
            } else {
                setPos.addOpPointEnum(OpPointEnum.HunYaoJiu);
            }
        }
        //清一色
        if (qys && setPos.getPosOpRecord().getOpHuList().stream().noneMatch(k -> ((OpPointEnum) k).name().contains("QYS"))) {
            setPos.addOpPointEnum(OpPointEnum.QYS);
        }
        if (setPos.getHuType().name().contains("ZiMo")) {
            setPos.addOpPointEnum((OpPointEnum) MJFactory.getHuCard(MJTemplateTianHuImpl.class).checkHuCardReturn(mSetPos, mCardInit));
            setPos.addOpPointEnum((OpPointEnum) MJFactory.getHuCard(CDXZMJDiHuImpl.class).checkHuCardReturn(mSetPos, mCardInit));
        }
        if (setPos.isTing()) {
            setPos.addOpPointEnum(OpPointEnum.BaoJiao);
        }
        if (setPos.isGSKH()) {
            setPos.addOpPointEnum(OpPointEnum.GSKH);
        }
        if (setPos.getTemplateRoomSet().isGSP()) {
            setPos.addOpPointEnum(OpPointEnum.GSP);
        }
        if (setPos.getPosOpRecord().getOpHuList().isEmpty()) {
            setPos.addOpPointEnum(OpPointEnum.PingHu);
        }
        return OpPointEnum.Not;
    }

    private boolean check258(int k) {
        return k % 10 == 2 || k % 10 == 5 || k % 10 == 8;
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
        if (k>100){
            k=k/100;
        }
        if (k > 40) {
            return false;
        }
        return k % 10 == 1 || k  % 10 == 9;
    }

    /**
     * @param mSetPos
     * @param mCardInit
     * @return
     */
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {

        if (Objects.isNull(mCardInit)) {
            return false;
        }
        if (Objects.isNull(mSetPos)) {
            return false;
        }
        if (checkNotQueYiMen(mSetPos, mCardInit)) {
            return false;
        }
        if (MJFactory.getHuCard(DDHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return true;
        }
        if (HuUtil.getInstance().checkHu(mCardInit.getAllCardInts(), mCardInit.sizeJin())) {
            return true;
        }
        return false;

    }

    public boolean checkNotQueYiMen(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        CDXZMJSetPos setPos = (CDXZMJSetPos) mSetPos;
        if (setPos.checkExistQue(mCardInit.getAllCardInts())) {
            return true;
        }
        List<Integer> allInt = new ArrayList<>();
        // 获取牌列表
        allInt.addAll(mCardInit.getAllCardInts());
        // 获取顺子，刻子，杠组成的胡牌。
        allInt.addAll(mSetPos.publicCardTypeList());
        // 分组列表
        Map<Integer, Long> map = allInt.stream().collect(Collectors.groupingBy(p -> p >= 1000 ? (p / 1000) : (p / 10), Collectors.counting()));
        // 检查分组数据
        if (null == map || map.size() <= 0) {
            return true;
        }
        int size = map.size();
        return size > 2;
    }
}	
