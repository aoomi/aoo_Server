package server.aoo.dao.config.util;

import com.ddm.server.common.utils.CommLog;
import com.ddm.server.common.utils.CommLogD;
import org.apache.commons.dbutils.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import server.aoo.dao.config.jpa.CustomJpaRepository;
import server.aoo.dao.config.util.Criteria;
import server.aoo.dao.config.util.ReflectionUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;

/**
 * druid工具类
 * @param <T>
 */
public class DBUtil<T> extends CustomJpaRepository<T,Long> {
    private final EntityManager entityManager;

    public DBUtil(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
    }

    /**
     * 生成结果处理器
     * @param <E>
     * @param clazz 生成的实体类
     * @param handler 结果转化处理器：BeanListHandler，BeanHandler，MapListHandler，MapHandler...（实现ResultSetHandler）
     * @param beanProcessor bean转化器GenerousBeanProcessor（驼峰转化），BeanProcessor
     * @return 返回结果处理器对象
     */
    protected <E> Optional<ResultSetHandler<?>> createHandler(Class<E> clazz, Class handler, BeanProcessor beanProcessor){
        try {
            switch (handler.getSimpleName()){
                case "MapListHandler":
                case "MapHandler":
                    if(beanProcessor==null){
                        return Optional.ofNullable((ResultSetHandler<?>) handler.getDeclaredConstructor().newInstance());
                    }
                    return generateHandler(handler,new Class[]{RowProcessor.class},new Object[]{new BasicRowProcessor(beanProcessor)});
                case "BeanHandler":
                case "BeanListHandler":
                    if(beanProcessor==null){
                        return generateHandler(handler,new Class[]{Class.class},new Object[]{clazz});
                    }
                    return generateHandler(handler,new Class[]{Class.class, RowProcessor.class},new Object[]{clazz,new BasicRowProcessor(beanProcessor)});
                default:
                    return generateHandler(handler,new Class[]{Class.class, RowProcessor.class},new Object[]{clazz,new BasicRowProcessor(beanProcessor)});
            }
        }catch (Exception e){
            CommLogD.error("createHandler:{}", e.getMessage());
        }
        return  Optional.empty();
    }

    /**
     * 构造结果集处理器对象
     * @param handler 欲生成的结果处理器类
     * @param classParams 构造方法参数
     * @param values 构造方法值
     * @return 生成的handler
     * @throws Exception
     */
    public Optional<ResultSetHandler<?>> generateHandler(Class handler,Class[] classParams,Object[] values) throws Exception{
        Constructor constructor = handler.getDeclaredConstructor(classParams);
        constructor.setAccessible(true);
        return Optional.ofNullable((ResultSetHandler<?>)constructor.newInstance(values));
    }

    /**
     * 创建结果处理器
     * @param handler 结果转化处理器：BeanListHandler，BeanHandler，MapListHandler，MapHandler...（实现ResultSetHandler）
     * @param clazz 生成的实体类
     * @return 返回结果处理器 ResultSetHandler
     */
    protected ResultSetHandler<?> createResultSetHandler(Class<?> clazz, Class handler){
        //使用默认驼峰处理器
        Optional<ResultSetHandler<?>> resultSetHandler = createHandler(clazz,handler,new GenerousBeanProcessor());
        return resultSetHandler.orElse(null);
    }

    /**
     * 生成删除语句
     * @param uniqueName 索引名
     * @param ids 删除的主键集
     * @return
     */
    protected String getDeleteSqlByIds(String uniqueName, List<Long> ids) {
        EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
        SessionFactoryImpl sessionFactory = (SessionFactoryImpl)entityManagerFactory.unwrap(SessionFactory.class);
        EntityPersister entity = sessionFactory.getMappingMetamodel().getEntityDescriptor(getDomainClass().getName());
        if(entity!=null){
            SingleTableEntityPersister persister = (SingleTableEntityPersister)entity;
            String[] identifierColumnNames = persister.getIdentifierColumnNames();
            if (identifierColumnNames.length == 0) {
                throw new RuntimeException(getDomainClass().getSimpleName() + "没有指定@Id注解!");
            }
            String tableName = persister.getTableName();
            uniqueName = StringUtils.isNotEmpty(uniqueName) ? String.format("`%s`",uniqueName) : identifierColumnNames[0];
            StringBuilder deleteSql = new StringBuilder();
            deleteSql.append("delete from ").append(tableName).append(" where ").append(uniqueName).append( ids.size() > 1 ? " in ( {0} )":" = {0}");
            List<String> placeholder = new ArrayList<>(Collections.nCopies(ids.size(), "?"));
            return  MessageFormat.format(deleteSql.toString(), StringUtils.join(placeholder,","));
        }
        return "";
    }

    /**
     * 生成插入语句
     * @param element 插入的对象
     * @param params 返回的插入参数
     * @param needIgnore 插入失败是否忽略异常
     * @param needID 是否插入语句带插入主键
     * @return
     */
    protected String getInsertSql(T element,List<Object> params,boolean needIgnore,boolean needID) {
        StringBuilder sql = new StringBuilder();
        try {
            EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
            SessionFactoryImpl sessionFactory = (SessionFactoryImpl)entityManagerFactory.unwrap(SessionFactory.class);
            EntityPersister entity = sessionFactory.getMappingMetamodel().getEntityDescriptor(element.getClass().getName());
            SingleTableEntityPersister persister = (SingleTableEntityPersister)entity;
            String tableName = persister.getTableName();
            sql.append("insert ").append(needIgnore?"ignore ":"").append("into ").append(tableName).append(" (");
            if(needID){
                //主键
                SingleTableEntityPersister entityItem = (SingleTableEntityPersister)entity;
                String[] identifierColumnNames = entityItem.getIdentifierColumnNames();
                String idName = identifierColumnNames[0];
                Field idGen = ReflectionUtils.getDeclaredField(getDomainClass(),entityItem.getIdentifierPropertyName());
                idGen.setAccessible(true);
                Object idValue = idGen.get(element);
                if(idValue!=null){
                    //添加到sql和参数
                    sql.append(String.format("`%s`,",idName));
                    params.add(idValue instanceof StringBuilder ? idValue.toString() : idValue);
                }
            }
            //所有属性
            String[] propertyNames = persister.getPropertyNames();
            for (String propertyName : propertyNames) {
                //对应数据库表中的字段名
                String[] columnName = persister.getPropertyColumnNames(propertyName);
                //获取对应的属性值
                Field field = ReflectionUtils.getDeclaredField(getDomainClass(),propertyName);
                field.setAccessible(true);
                Object fieldValue = field.get(element);
                //添加到sql和参数
                sql.append(String.format("`%s`,",columnName[0]));
                params.add(fieldValue instanceof StringBuilder ? fieldValue.toString() : fieldValue);
            }
            sql.deleteCharAt(sql.length() - 1);
            sql.append(")values(");
            List<String> placeholder = new ArrayList<>(Collections.nCopies(params.size(), "?"));
            sql.append(StringUtils.join(placeholder,","));
            sql.append(")");
        }catch (Exception e){
            CommLogD.error("getInsertSql:{}",e.getMessage());
        }
        return sql.toString();
    }

    /**
     * 生成更新语句
     * @param element 更新的对象
     * @param params 返回的更新参数
     * @return
     */
    protected String getUpdateSqlByEntity(T element, List<Object> params) {
        String idName = "";
        Object idValue = null;
        StringBuilder updateSql = new StringBuilder();
        try {
            EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
            SessionFactoryImpl sessionFactory = (SessionFactoryImpl)entityManagerFactory.unwrap(SessionFactory.class);
            EntityPersister entity = sessionFactory.getMappingMetamodel().getEntityDescriptor(element.getClass().getName());
            SingleTableEntityPersister entityItem = (SingleTableEntityPersister)entity;
            //Entity对应的表的英文名
            String tableName = entityItem.getTableName();
            updateSql.append("update ").append(tableName).append(" set ");
            //所有属性
            String[] propertyNames = entityItem.getPropertyNames();
            //主键
            String[] identifierColumnNames = entityItem.getIdentifierColumnNames();
            idName = identifierColumnNames[0];
            Field idGen = ReflectionUtils.getDeclaredField(getDomainClass(),entityItem.getIdentifierPropertyName());
            idGen.setAccessible(true);
            idValue = idGen.get(element);
            for (String propertyName : propertyNames) {
                //对应数据库表中的字段名
                String[] columnName = entityItem.getPropertyColumnNames(propertyName);
                //获取对应的属性值
                Field field = ReflectionUtils.getDeclaredField(getDomainClass(),propertyName);
                field.setAccessible(true);
                Object fieldValue = field.get(element);
                updateSql.append(" ").append(String.format("`%s`",columnName[0])).append( " = ?,");
                params.add(fieldValue instanceof StringBuilder ? fieldValue.toString() : fieldValue);
            }
        }catch (Exception e){
            CommLogD.error("getUpdateSqlByEntity:{}",e.getMessage());
        }

        if(idValue!=null){
            params.add(idValue);
            updateSql.deleteCharAt(updateSql.length()-1);
            updateSql.append(" where ").append(String.format("`%s`",idName)).append(" = ?");
            return updateSql.toString();
        }

        return "";
    }

    /**
     * 生成更新语句
     * @param updateMap (key->value)  更新的map
     * @param params 返回的更新参数
     * @return
     */
    protected String getUpdateSqlByUnique(Map<String,Object> updateMap, Criteria criteria, List<Object> params){
        EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
        SessionFactoryImpl sessionFactory = (SessionFactoryImpl)entityManagerFactory.unwrap(SessionFactory.class);
        EntityPersister entity = sessionFactory.getMappingMetamodel().getEntityDescriptor(getDomainClass().getName());
        SingleTableEntityPersister entityItem = (SingleTableEntityPersister)entity;
        StringBuilder updateSql = new StringBuilder();
        updateSql.append("update ").append(entityItem.getTableName()).append(" set ");
        updateMap.forEach((fieldName, fieldValue) -> {
            updateSql.append(" ").append(String.format("`%s`", fieldName)).append(" = ?,");
            params.add(fieldValue instanceof StringBuilder ? fieldValue.toString() : fieldValue);
        });

        updateSql.deleteCharAt(updateSql.length()-1);
        updateSql.append(" where ");
        updateSql.append(criteria.toSql());
        params.addAll(criteria.getParams());
        return updateSql.toString();
    }

    /**
     * 堆栈输出
     * @param mainError 主错误输出
     * @param e 异常
     */
    public static void stackTrace(String mainError,Exception e){
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(e.getStackTrace()).forEach(m->{
            stringBuilder.append("("+m.getClassName()+"---->"+m.getFileName()+"---->"+m.getMethodName()+"--->"+m.getLineNumber()+")");
        });
        CommLogD.error(mainError+":{}        {}",e.getMessage(),stringBuilder.toString());
    }

    /**
     * 跟踪日志
     */
    public static void stackTrace(){
        StringBuilder stringBuilder = new StringBuilder();
        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                stringBuilder.append("("+stackElements[i].getClassName()+"---->"+stackElements[i].getFileName()+"--->"+stackElements[i].getLineNumber()+"--->"+stackElements[i].getMethodName()+")"+"\n");
            }
        }
        CommLog.error(stringBuilder.toString());
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

}
