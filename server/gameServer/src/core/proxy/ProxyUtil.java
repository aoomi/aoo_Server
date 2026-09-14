package core.proxy;

import BaseTask.AsynTask.AsyncTaskManager;
import com.ddm.server.annotation.Service;
import com.ddm.server.common.CommLogD;
import core.db.DataBaseMgr;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.dao.dbZle.BaseDbZleDao;
import core.db.other.AsyncInfo;
import core.db.persistence.CustomerDao;
import core.db.persistence.DaoMethodMapping;
import core.db.persistence.Repository;
import core.ioc.ContainerMgr;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class ProxyUtil {

    /**
     * 代理Service
     *
     * @param service
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxyService(T service) {
        Object obj = ProxyFactory.getProxyInstance(service, (Object o, Method method, Object[] args) -> distributeMethod(o, method, args));
        return (T) obj;
    }

    /**
     * 同步或者异步处理方法
     *
     * @param obj    对象
     * @param method 方法
     * @param args   参数
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Object distributeMethod(Object obj, Method method, Object[] args) {
        AsyncInfo asyncInfo = null;
        if (args.length > 0) {
            Object object = Arrays.asList(args).stream().filter(o -> o instanceof AsyncInfo).findFirst().orElse(null);
            asyncInfo = object != null ? (AsyncInfo) object : null;
        }
        if (asyncInfo != null && asyncInfo.isASync()) {
            AsyncTaskManager.getDefaultMultQueue().regAsynTask(() -> {
                try {
                    return serviceMethodInvoke(obj, method, args);
                } catch (Throwable e) {
                    CommLogD.error("serviceMethodInvoke:{}", e.getMessage());
                }
                return null;
            }, asyncInfo.getAsyncCallBackTaskBase(), asyncInfo.getAsynTaskTag());
            return primitiveDefault(method.getReturnType());
        } else {
            try {
                return serviceMethodInvoke(obj, method, args);
            } catch (Throwable e) {
                CommLogD.error("serviceMethodInvoke failed: {}.{}", obj.getClass().getName(), method.getName(), e);
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (e instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException("Service invocation failed: " + method, e);
            }
        }
    }

    private static Object primitiveDefault(Class<?> returnType) {
        if (!returnType.isPrimitive() || returnType == void.class) return null;
        if (returnType == boolean.class) return false;
        if (returnType == char.class) return '\0';
        if (returnType == byte.class) return (byte) 0;
        if (returnType == short.class) return (short) 0;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0F;
        if (returnType == double.class) return 0D;
        throw new IllegalArgumentException("Unsupported primitive return type: " + returnType);
    }

    /**
     * service方法代理
     *
     * @param obj    对象
     * @param method 方法
     * @param args   参数
     * @return
     * @throws Throwable
     */
    public static Object serviceMethodInvoke(Object obj, Method method, Object[] args) throws Throwable {
        if ("setSourceName".equals(method.getName())) {
            return method.invoke(obj, args);
        }
        boolean transaction = false;
        Service service = obj.getClass().getAnnotation(Service.class);
        String source = service.source();

        try {
//			DataBaseMgr.get(source).getLock().lock();
            if (ContainerMgr.get().isTransaction(method)) {
                DataBaseMgr.get(source).openTransaction();
                transaction = true;
            }
            DataBaseMgr.get(source).setDiscardConnectionLevelForService();
            Object res = method.invoke(obj, args);
            if (transaction) {
                DataBaseMgr.get(source).getConnection().commit();
            }
            return res;
        } catch (Exception e) {
            if (transaction) {
                DataBaseMgr.get(source).getConnection().rollback();
            }
            if (e instanceof InvocationTargetException invocation && invocation.getTargetException() != null) {
                throw invocation.getTargetException();
            }
            throw e;
        } finally {
            DataBaseMgr.get(source).discardConnectionFromService();
//			DataBaseMgr.get(source).getLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getProxyRemote(T remote) {
        Object obj = ProxyFactory.getProxyInstance(remote, (Object o, Method method, Object[] args) -> {
            try {
                Object res = method.invoke(o, args);
                return res;
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } catch (Exception e) {
                throw e;
            }
        });
        return (T) obj;
    }

    /**
     * 动态获取代理源
     *
     * @param dao        dao实体类
     * @param dataSource 数据源
     * @param <T>
     * @return
     */
    public static <T> Repository getDataSourceDao(Class<T> dao, String dataSource) {
        CustomerDao<T> customerDao;
        switch (dataSource) {
            case "clark_game":
                customerDao = new BaseClarkGameDao<>(dao);
                break;
            case "db_zle":
                customerDao = new BaseDbZleDao<>(dao);
                break;
            case "clark_log":
                customerDao = new BaseClarkLogDao<>(dao);
                break;
            default:
                customerDao = new BaseClarkLogDao<>(dao);
                break;
        }
        return customerDao;
    }

    /**
     * 获取Dao代理
     *
     * @param entity     dao实体类
     * @param dao        dao接口类
     * @param dataSource 数据源
     * @param <T>
     * @param <E>
     * @return
     */
    public static <T, E> T getProxyDao(Class<E> entity, Class<T> dao, String dataSource) {
        Repository<E> customerDao = getDataSourceDao(entity, dataSource);
        T repositoryProxy = (T) Proxy.newProxyInstance(customerDao.getClass().getClassLoader(), new Class[]{dao, Repository.class},
                (object, method, params) -> DaoMethodMapping.get().methodAdapter(method, customerDao, params));
        return repositoryProxy;
    }
}
