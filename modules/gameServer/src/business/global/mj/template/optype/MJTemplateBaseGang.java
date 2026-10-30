package business.global.mj.template.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import business.global.mj.manage.OpCard;
import business.global.mj.template.MJTemplateRoom;
import business.global.mj.template.MJTemplateSetPos;
import business.global.mj.template.xueliu.MJTemplate_XueLiuSetPos;
import business.global.mj.ting.AbsTing;
import business.global.mj.util.HuUtil;
import cenum.mj.MJSpecialEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class MJTemplateBaseGang implements OpCard {

    /**
     * 检测杠玩 是否还能听牌
     *
     * @return
     */
    public boolean checkGang(MJTemplateSetPos mSetPos, int type) {
        if (!checkFilter(mSetPos, type)) {
            return false;
        }
        if (mSetPos.getRoom().isWanFa_XueLiuMoShi()) {
            return checkXueLiuGang(mSetPos, type);
        }
        //没有报听则可以碰杠
        if (checkNormalGang(mSetPos, type)) {
            return true;
        }
        MJCardInit mCardInit = mSetPos.mjCardInit(true);
        List<Integer> collect;
        if (mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
            collect = mCardInit.getJins().stream().filter(k -> k == type).collect(Collectors.toList());
            mCardInit.getJins().removeAll(collect);
        } else {
            collect = mCardInit.getAllCardInts().stream().filter(k -> k == type).collect(Collectors.toList());
            mCardInit.getAllCardInts().removeAll(collect);
        }
        mCardInit.getJins().add(MJSpecialEnum.NOT_JIN.value());
        if (((AbsTing) MJFactory.getTingCard(mSetPos.getmActMrg())).tingHu(mSetPos, mCardInit)) {
            return true;
        }
        return false;
    }

    /**
     * 血流完玩法检查是否可以杠
     *
     * @param mSetPos
     * @param type
     * @return
     */
    public boolean checkXueLiuGang(MJTemplateSetPos mSetPos, int type) {
        //没有报听则可以碰杠
        if (checkNormalGang(mSetPos, type) && ((MJTemplate_XueLiuSetPos) mSetPos).getHuInfos().size() == 0) {
            return true;
        }
        List<Integer> huCardTypes = new ArrayList<>(mSetPos.getHuCardTypes());
        MJCardInit mCardInit = mSetPos.mjCardInit(true);
        List<Integer> collect;
        if (mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
            collect = mCardInit.getJins().stream().filter(k -> k == type).collect(Collectors.toList());
            mCardInit.getJins().removeAll(collect);
        } else {
            collect = mCardInit.getAllCardInts().stream().filter(k -> k == type).collect(Collectors.toList());
            mCardInit.getAllCardInts().removeAll(collect);
        }
        //保证杠完后听的还是一样
        boolean hu;
        for (int cardType : HuUtil.CheckTypes) {
            MJCardInit newInit = new MJCardInit(mCardInit.getAllCardInts(), cardType);
            newInit.addAllJins(mCardInit.getJins());
            hu = ((AbsTing) MJFactory.getTingCard(mSetPos.getmActMrg())).tingHu(mSetPos, newInit);
            if (hu && !huCardTypes.contains(cardType)) {
                return false;
            }
            if (!hu && huCardTypes.contains(cardType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否正常杠
     *
     * @param mSetPos
     * @param type
     * @return
     */
    public boolean checkNormalGang(MJTemplateSetPos mSetPos, int type) {
        return !mSetPos.isTing();
    }


    /**
     * 检查过滤器
     */
    protected boolean checkFilter(AbsMJSetPos mSetPos, int type) {
        if (((MJTemplateRoom) mSetPos.getRoom()).isWanFa_JinBuKeChiPengGang() && mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
            return false;
        }
        if (((MJTemplateSetPos) mSetPos).checkIsQue(type)) {
            return false;
        }
        return type < MJSpecialEnum.NOT_HUA.value();
    }
}
