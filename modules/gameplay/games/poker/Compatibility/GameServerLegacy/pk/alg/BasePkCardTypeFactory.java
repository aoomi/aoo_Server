package business.global.pk.alg;

import com.ddm.server.common.CommLogD;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhujianming
 * @date 2020-07-14 09:28
 * 算法抽象扑克牌工厂
 */
public class BasePkCardTypeFactory {

    private static Map<Class, BasePKAbsType> typeMap = new HashMap<>();

    public static synchronized BasePKAbsType getCardType(Class typeClass) {
        if (typeMap.get(typeClass) == null) {
            try {
                typeMap.put(typeClass, (BasePKAbsType) typeClass.newInstance());
            } catch (Exception e) {
                CommLogD.error(e.getMessage());
            }
        }
        return typeMap.get(typeClass);
    }
}
