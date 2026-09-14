package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import cenum.mj.MJSpecialEnum;

/**
 * 检查暗杠
 *
 * @author Administrator
 */
public class AnGangCardImpl extends BaseAnGang {

    /**
     * 检查过滤器
     */
    @Override
    protected boolean checkFilter(AbsMJSetPos mSetPos, int type) {
        return !mSetPos.getSet().getmJinCardInfo().checkJinExist(type) && type < MJSpecialEnum.NOT_HUA.value();
    }


}
