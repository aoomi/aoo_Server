package business.global.pk.njpdk.cardtype;

import business.global.pk.njpdk.cardtype.type.NJPDKAbsType;
import com.ddm.server.common.CommLogD;

import java.util.HashMap;
import java.util.Map;

public class NJPDKCardTypeFactory {

    private static Map<Class, NJPDKAbsType> typeMap = new HashMap<>();

    public static synchronized NJPDKAbsType getCardType(Class typeClass) {
        if (typeMap.get(typeClass) == null) {
            try {
                typeMap.put(typeClass, (NJPDKAbsType) typeClass.newInstance());
            } catch (Exception e) {
                CommLogD.error(e.getMessage());
            }
        }
        return typeMap.get(typeClass);
    }
}
