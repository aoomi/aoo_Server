package business.global.mj.manage;

import com.ddm.server.common.CommLogD;

/**
 * 麻将创建工程
 *
 * @author Administrator
 */
public class MJConcreteCreator extends MJCreator {

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TingCard> T createTingProduct(Class<?> clazz) {
        TingCard tCard = null;
        try {
            tCard = (TingCard) Class.forName(clazz.getName()).newInstance();
        } catch (Exception e) { //异常处理  
            CommLogD.error("[MJConcreteCreator createTingProduct]:[{}] error:{}", clazz.getName(), e.getMessage(), e);
        }
        return (T) tCard;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends HuCard> T createHuProduct(Class<?> clazz) {
        HuCard hCard = null;
        try {
            hCard = (HuCard) Class.forName(clazz.getName()).newInstance();
        } catch (Exception e) { //异常处理  
            CommLogD.error("[MJConcreteCreator createHuProduct]:[{}] error:{}", clazz.getName(), e.getMessage(), e);
        }
        return (T) hCard;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T extends OpCard> T createOpProduct(Class<?> clazz) {
        OpCard oCard = null;
        try {
            oCard = (OpCard) Class.forName(clazz.getName()).newInstance();
        } catch (Exception e) { //异常处理  
            CommLogD.error("[MJConcreteCreator createOpProduct]:[{}] error:{}", clazz.getName(), e.getMessage(), e);
        }
        return (T) oCard;
    }

    @Override
    public <T extends OpCard> T createTOpProduct(Class<?> clazz) {
        // TODO Auto-generated method stub
        return null;
    }


} 