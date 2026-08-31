package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import cenum.mj.MJSpecialEnum;

/**
 * 暗杠
 * 可暗杠金牌
 *
 * @author Administrator
 */
public class AnGangCardJinImpl extends BaseAnGang {

    /**
     * 检查过滤器
     */
    @Override
    protected boolean checkFilter(AbsMJSetPos mSetPos, int type) {
        return type < MJSpecialEnum.NOT_HUA.value();
    }


}
