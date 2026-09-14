package business.global.mj.template;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.hu.*;
import business.global.mj.manage.MJFactory;
import cenum.mj.HuType;
import cenum.mj.OpPointEnum;

import java.util.Objects;

public class MJTemplateNormalHuCardImpl extends BaseHuCard {

    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {

        if (Objects.isNull(mCardInit)) {
            return false;
        }
        if (Objects.isNull(mSetPos)) {
            return false;
        }
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        //定缺玩法 存在缺牌的家不能胡
        if (setPos.checkExistQue(mCardInit.getAllCardInts())) {
            return false;
        }
        if (MJFactory.getHuCard(DDHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return true;
        }
        return super.checkHuCard(mSetPos, mCardInit);
    }

    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        if (mSetPos.getHuType().equals(HuType.ZiMo)) {
            setPos.addOpPointEnum(OpPointEnum.ZiMo);
        } else if (mSetPos.getHuType().equals(HuType.JiePao)) {
            setPos.addOpPointEnum(OpPointEnum.JiePao);
        } else if (mSetPos.getHuType().equals(HuType.QGH)) {
            setPos.addOpPointEnum(OpPointEnum.QGH);
        }
        //清一色
        setPos.addOpPointEnum((OpPointEnum) MJFactory.getHuCard(QingYiSeImpl.class).checkHuCardReturn(mSetPos, mCardInit));
        //字一色
        setPos.addOpPointEnum((OpPointEnum) MJFactory.getHuCard(ZiYiSeImpl.class).checkHuCardReturn(mSetPos, mCardInit));
        //碰碰胡｜对对胡
        if (MJFactory.getHuCard(PPHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            setPos.addOpPointEnum(OpPointEnum.PPH);
        }
        //七对
        if (MJFactory.getHuCard(DDHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            setPos.addOpPointEnum(OpPointEnum.QDHu);
        }
        return OpPointEnum.Not;
    }

}
