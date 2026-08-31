package business.global.mj.manage;

/**
 * 胡牌工厂
 *
 * @author Administrator
 */
public abstract class MJCreator {

    /**
     * 创建“听牌”工厂方法
     *
     * @param clazz 类
     * @return
     */
    public abstract <T extends TingCard> T createTingProduct(Class<?> clazz);

    /**
     * 创建“胡牌”工厂方法
     *
     * @param clazz 类
     * @return
     */
    public abstract <T extends HuCard> T createHuProduct(Class<?> clazz);

    /**
     * 创建“常规动作”工厂方法
     *
     * @param clazz 类
     * @return
     */
    public abstract <T extends OpCard> T createOpProduct(Class<?> clazz);

    /**
     * 创建“特殊动作”工厂方法
     *
     * @param clazz 类
     * @return
     */
    public abstract <T extends OpCard> T createTOpProduct(Class<?> clazz);

}  