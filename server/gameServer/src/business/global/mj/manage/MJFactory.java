package business.global.mj.manage;

import java.util.concurrent.ConcurrentHashMap;

public class MJFactory {

    // 听牌工厂列表
    private static ConcurrentHashMap<String, TingCard> allTingCard = new ConcurrentHashMap<String, TingCard>();
    // 胡牌工厂列表
    private static ConcurrentHashMap<String, HuCard> allHuCard = new ConcurrentHashMap<String, HuCard>();
    // 动作工厂列表
    private static ConcurrentHashMap<String, OpCard> allOpCard = new ConcurrentHashMap<String, OpCard>();

    public static TingCard getTingCard(Class<?> name) {
        TingCard tCard = allTingCard.get(name.getName());
        if (null != tCard) {
            return tCard;
        } else {
            tCard = new MJConcreteCreator().createTingProduct(name);
            allTingCard.put(name.getName(), tCard);
            return tCard;
        }
    }


    public static HuCard getHuCard(Class<?> name) {
        HuCard hCard = allHuCard.get(name.getName());
        if (null != hCard) {
            return hCard;
        } else {
            hCard = new MJConcreteCreator().createHuProduct(name);
            allHuCard.put(name.getName(), hCard);
            return hCard;
        }
    }


    public static OpCard getOpCard(Class<?> name) {
        OpCard oCard = allOpCard.get(name.getName());
        if (null != oCard) {
            return oCard;
        } else {
            oCard = new MJConcreteCreator().createOpProduct(name);
            allOpCard.put(name.getName(), oCard);
            return oCard;
        }
    }
}