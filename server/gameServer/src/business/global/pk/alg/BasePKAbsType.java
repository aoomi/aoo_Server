package business.global.pk.alg;

import business.global.pk.alg.params.BaseOpCard;
import business.global.pk.alg.params.BasePKParameter;
import business.global.pk.alg.util.BasePKALGUtil;

/**
 * @author zhujianming
 * @date 2020-07-14 09:28
 * 算法抽象类型算法
 */
public abstract class BasePKAbsType<T extends BasePKALGUtil> {
    public abstract BaseOpCard getCardList(BasePKParameter cardType);

    public abstract BaseOpCard checkCardList(BasePKParameter cardType);

    public abstract T getBasePKALGUtil();
}
