//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package core.ioc;

import com.ddm.server.annotation.*;
import com.ddm.server.annotation.base.AutowiredManager;
import com.ddm.server.annotation.base.Component;
import com.ddm.server.annotation.base.TransactionManager;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.mongodb.MongoDBHelper;
import com.ddm.server.common.redis.RedisSource;
import com.ddm.server.common.task.MethodInvoke;
import com.ddm.server.common.task.TaskMgr;
import com.ddm.server.common.task.TaskTrigger;
import com.ddm.server.common.task.TaskUtil;
import core.proxy.ProxyUtil;
import org.quartz.SchedulerException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ContainerMgr {
    private Map<Class<?>, Object> map;
    //service的接口容器
    private Map<String, Object> serviceMap;
    //dao的接口容器
    private Map<String, Object> daoMap;
    private List<Class<?>> autowiredList;
    private List<Class<?>> componentList;
    //回滚方法
    private List<Method> transactionList;
    private List<TaskTrigger> taskList;

    private ContainerMgr() {
        this.map = new HashMap();
        this.serviceMap = new HashMap();
        this.autowiredList = new ArrayList();
        this.componentList = new ArrayList();
        this.transactionList = new ArrayList();
        this.taskList = new ArrayList();
        this.daoMap = new HashMap();
    }

    public static ContainerMgr get() {
        return ContainerMgr.SingleCase.INSTANCE;
    }

    /**
     * @param projectPackage
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void init(String projectPackage) throws InstantiationException, IllegalAccessException {
        CommLogD.info("container init...");
        Set<Class<?>> clazzs = ClassFind.getClasses(projectPackage);
        Iterator var4 = clazzs.iterator();

        while (var4.hasNext()) {
            Class<?> clazz = (Class) var4.next();
            if (!clazz.isAnonymousClass() && !clazz.isMemberClass() && !clazz.isLocalClass() && !clazz.isAnnotation() && !clazz.isInterface()) {
                this.analysis(clazz, clazz);
            }
            //面向接口编程的Dao,生成代理，代理接口
            if (clazz.isInterface() && clazz.isAnnotationPresent(Dao.class)) {
                this.analysis(clazz, clazz);
            }
        }

    }

    private void analysis(Class<?> clazz, Class<?> anno) throws InstantiationException, IllegalAccessException {
        if (anno.isAnnotationPresent(Bean.class)) {
            CommLogD.info("register bean {}", clazz.getSimpleName());
            this.map.put(clazz, clazz.newInstance());
            this.analysis(clazz, Bean.class);
        }

        if (anno.isAnnotationPresent(Dao.class)) {
            CommLogD.info("register dao {}", clazz.getSimpleName());
            if (clazz.isInterface() && clazz.isAnnotationPresent(Dao.class)) {
                //面向接口方式
                interfaceProxy(clazz);
            } else {
                //面向对象
                this.map.put(clazz, clazz.newInstance());
            }
            this.analysis(clazz, Dao.class);
        }

        if (anno.isAnnotationPresent(Service.class)) {
            CommLogD.info("register service {}", clazz.getSimpleName());
            this.map.put(clazz, clazz.newInstance());
            this.analysis(clazz, Service.class);
        }

        if (anno.isAnnotationPresent(AutowiredManager.class)) {
            this.autowiredList.add(clazz);
        }

        if (anno.isAnnotationPresent(Component.class)) {
            this.componentList.add(clazz);
        }

        if (anno.isAnnotationPresent(TransactionManager.class)) {
            Method[] methods = clazz.getDeclaredMethods();
            for (int var14 = 0; var14 < methods.length; ++var14) {
                Method method = methods[var14];
                if (method.isAnnotationPresent(Transaction.class)) {
                    CommLogD.info("open transaction {}.{}", clazz.getSimpleName(), method.getName());
                    this.transactionList.add(method);
                }
            }
        }

        if (anno.isAnnotationPresent(Task.class)) {
            Object obj = clazz.newInstance();
            map.put(clazz, obj);
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                CronTask cronTask = method.getDeclaredAnnotation(CronTask.class);
                if (cronTask != null) {
                    CommLogD.info("register cron[{}] timing {}.{}", cronTask.value(), clazz.getSimpleName(), method.getName());
                    MethodInvoke methodInvoke = ProxyUtil.getProxyRemote(new MethodInvoke());
                    methodInvoke.init(obj, method);
                    taskList.add(TaskUtil.getCronTask(cronTask.value(), methodInvoke));
                } else {
                    IntervalTask intervalTask = method.getDeclaredAnnotation(IntervalTask.class);
                    if (intervalTask != null) {
                        CommLogD.info("register interval[{}] timing {}.{}", intervalTask.value(), clazz.getSimpleName(), method.getName());
                        MethodInvoke methodInvoke = ProxyUtil.getProxyRemote(new MethodInvoke());
                        methodInvoke.init(obj, method);
                        taskList.add(TaskUtil.getIntervalTask(intervalTask.value(), intervalTask.delay(), methodInvoke));
                    }
                }
            }
            analysis(clazz, Task.class);
        }
    }

    /**
     * 接口代理
     *
     * @param clazz
     */
    public void interfaceProxy(Class<?> clazz) {
        //面向接口代理
        try {
            String typeName = clazz.getAnnotatedInterfaces()[0].getType().getTypeName();
            String[] simpleName = typeName.split("<");
            String entityName = simpleName[simpleName.length - 1];
            entityName = entityName.replace(">", "");
            Class<?> classZ = Class.forName(entityName);
            this.map.put(clazz, ProxyUtil.getProxyDao(classZ, clazz, clazz.getAnnotation(Dao.class).dataSource()));
        } catch (Exception e) {
            CommLogD.error(e.getMessage());
        }
    }

    public void autowired() throws IllegalArgumentException, IllegalAccessException {
        Iterator var2 = this.autowiredList.iterator();

        while (var2.hasNext()) {
            Class<?> clazz = (Class) var2.next();
            Object obj = this.map.get(clazz);
            Field[] fields = clazz.getDeclaredFields();

            for (int var6 = 0; var6 < fields.length; ++var6) {
                Field field = fields[var6];
                if (field.isAnnotationPresent(Autowired.class)) {
                    Class<?> type = field.getType();
                    if (this.componentList.contains(type)) {
                        CommLogD.info("autowired {}.{} success", clazz.getSimpleName(), field.getName());
                        field.setAccessible(true);
                        field.set(obj, this.map.get(type));
                    } else {
                        CommLogD.info("autowired {}.{} fail", clazz.getSimpleName(), field.getName());
                    }
                }
            }
        }
    }

    public void startTask() throws SchedulerException {
        for (TaskTrigger taskTrigger : taskList) {
            TaskMgr.get().start(taskTrigger);
        }
    }

    /**
     * service动态代理
     */
    public void proxyObj() {
        for (Map.Entry<Class<?>, Object> entry : this.map.entrySet()) {
            Class<?> clazz = entry.getKey();
            CommLogD.info("proxy object {}", clazz.getSimpleName());
            if (clazz.isAnnotationPresent(Service.class)) {
                Object proxy = ProxyUtil.getProxyService(entry.getValue());
                entry.setValue(proxy);
                this.serviceMap.put(clazz.getSimpleName(), proxy);
            }
            if (clazz.isAnnotationPresent(Dao.class) && clazz.isInterface()) {
                this.daoMap.put(clazz.getSimpleName(), entry.getValue());
            }
        }
        this.map.clear();
        this.autowiredList.clear();
        this.componentList.clear();
    }

    /**
     * 获取组件
     *
     * @param <T>
     * @return
     */
    public <T> T getComponent(String name) {
        return this.serviceMap.containsKey(name) ? (T) this.serviceMap.get(name) : null;
    }

    /**
     * 获取组件
     *
     * @param <T>
     * @return
     */
    public <T> T getComponent(Class<T> clazz) {
        String name = clazz.getSimpleName();
        return this.serviceMap.containsKey(name) ? (T) this.serviceMap.get(name) : null;
    }

    public RedisSource getRedis() {
        RedisSource source = (RedisSource) this.map.get(RedisSource.class);
        if (Objects.isNull(source)) {
            source = new RedisSource();
            addComponent(source);
        }
        return source;
    }

    public MongoDBHelper getMongoDb() {
        MongoDBHelper mongoDb = (MongoDBHelper) this.map.get(MongoDBHelper.class);
        if (Objects.isNull(mongoDb)) {
            mongoDb = new MongoDBHelper();
            addComponent(mongoDb);
        }
        return mongoDb;
    }

    public void addComponent(Object obj) {
        map.put(obj.getClass(), obj);
        componentList.add(obj.getClass());
    }

    public void removeComponent(Object obj) {
        map.remove(obj.getClass());
        componentList.remove(obj.getClass());
    }

    /**
     * 获取接口代理对象
     *
     * @param <T>
     * @return
     */
    public <T> T getDao(Class<T> clazz) {
        String name = clazz.getSimpleName();
        return this.daoMap.containsKey(name) ? (T) this.daoMap.get(name) : null;
    }

    /**
     * 获取接口代理对象
     *
     * @param <T>
     * @return
     */
    public <T> T getDao(String name) {
        return this.daoMap.containsKey(name) ? (T) this.daoMap.get(name) : null;
    }


    public boolean isTransaction(Method method) {
        return this.transactionList.contains(method);
    }


    private static class SingleCase {
        public static final ContainerMgr INSTANCE = new ContainerMgr();

        private SingleCase() {
        }
    }
}
