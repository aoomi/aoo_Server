package business.global.pk;

import com.ddm.server.common.CommLogD;

import java.util.concurrent.ConcurrentHashMap;

public class PKFactory {

    // 胡牌工厂列表
    private static ConcurrentHashMap<String, CardType> cardTypeMap = new ConcurrentHashMap<>();

    public static CardType getCardType(Class<?> name) {
        CardType hCard = cardTypeMap.get(name.getName());
        if (null != hCard) {
            return hCard;
        } else {
            return createCardTypeProduct(name);
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends CardType> T createCardTypeProduct(Class<?> clazz) {
        if (cardTypeMap.get(clazz.getName()) != null) {
            return (T) cardTypeMap.get(clazz.getName());
        }
        try {
            CardType tCard = (CardType) Class.forName(clazz.getName()).newInstance();
            cardTypeMap.put(clazz.getName(), tCard);
            return (T) tCard;
        } catch (Exception e) { //异常处理
            CommLogD.error("[createCardTypeProduct]:[{}] error:{}", clazz.getName(), e.getMessage(), e);
        }
        return null;
    }
}