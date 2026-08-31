package server.aoo.dao.config.util;
 
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import server.aoo.dao.mapper.game.BaseDao;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

/**
 * 自定义Dao数据库操作工具
 * @param <T>
 */
public class CustomerDaoImpl<T> extends BaseDaoImpl<T>
        implements BaseDao<T> {

    public CustomerDaoImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
    }

    /**
     * 新增
     * @param element 新增实体
     * @param ignore 插入异常是否报错终止
     * @return
     */
    @Override
    public long add(T element, boolean ignore) {
        Class clazz = element.getClass();
        Field[] fields = clazz.getDeclaredFields();
        List<Object> params = new ArrayList<>();
        String sql = getInsertSql(element,params,ignore,false);
        long id = insertAndGetGeneratedKeys(sql, params.toArray(new Object[params.size()]));
        //新增后赋值主键
        if(id>0){
            Optional<Field> idFieldOp = Arrays.asList(fields).stream().filter(field->"id".equalsIgnoreCase(field.getName())).findFirst();
            idFieldOp.ifPresent(field->{
                try {
                    field.setAccessible(true);
                    field.set(element,id);
                }catch (Exception e){
                    stackTrace("add", e);
                }
            });
        }
        return id;
    }

    /**
     * 新增
     * @param element 新增实体
     * @param ignore 插入异常是否报错终止
     * @return
     */
    @Override
    public long addById(T element, boolean ignore) {
        Class clazz = element.getClass();
        Field[] fields = clazz.getDeclaredFields();
        List<Object> params = new ArrayList<>();
        String sql = getInsertSql(element,params,ignore,true);
        long id = insertAndGetGeneratedKeys(sql, params.toArray(new Object[params.size()]));
        //新增后赋值主键
        if(id>0){
            Optional<Field> idFieldOp = Arrays.asList(fields).stream().filter(field->"id".equalsIgnoreCase(field.getName())).findFirst();
            idFieldOp.ifPresent(field->{
                try {
                    field.setAccessible(true);
                    field.set(element,id);
                }catch (Exception e){
                    stackTrace("add", e);
                }
            });
        }
        return id;
    }

    /***
     * 更新
     * @param element 更新对象
     * @return
     */
    @Override
    public int update(T element) {
        List<Object> params = new ArrayList<>();
        String sql = getUpdateSqlByEntity(element,params);
        return super.update(sql, params.toArray(new Object[params.size()]));
    }

    @Override
    public int update(String sql, Object... obj) {
        return super.update(sql, obj);
    }

    /**
     * 根据id删除
     * @param id 主键值
     * @return
     */
    @Override
    public Integer delete(long id) {
        if(id<=0){
            return -1;
        }
        return delete(Arrays.asList(id));
    }

    /**
     * 删除
     * @param ids 索引集合
     * @return
     */
    @Override
    public Integer delete(List<Long> ids) {
        return delete(ids,null);
    }

    /**
     * 查询
     * @param id 主键值
     * @return
     */
    @Override
    public T findOne(long id) {
        String sql = "select * from "+getTableName()+" where id = ? limit 1";
        return getBean(sql,id);
    }

    /**
     * 加载所有
     * @param clazz 结果类
     * @param sql 查询语句
     * @param obj 查询值
     * @param <E>
     * @return
     */
    @Override
    public <E> List<E> loadAll(Class<E> clazz, String sql, Object... obj) {
        return listBeanByClass(sql,clazz==null?getDomainClass():clazz,obj);
    }

    /**
     * 加载
     * @param clazz 结果类
     * @param sql 查询语句
     * @param obj 查询值
     * @param <E>
     * @return
     */
    @Override
    public <E> E loadOne(Class<E> clazz, String sql, Object... obj) {
        return getBeanByClass(sql,clazz==null?getDomainClass():clazz, obj);
    }

    /**
     * 统计
     * @param sql 统计语句
     * @param obj 统计值
     * @return
     */
    @Override
    public Long count(String sql, Object... obj) {
        return getValue(sql,obj);
    }

    /**
     * 查询分页
     * @param pageNum 页码
     * @param pageSize 页长
     * @return
     */
    @Override
    public List<T> findPage(Integer pageNum, Integer pageSize) {
        Criteria criteria = new Criteria();
        criteria.setPageNum(pageNum);
        criteria.setPageSize(pageSize);
        return findAll(criteria,null,null);
    }

    /**
     * 查询所有
     * @return
     */
    @Override
    public List<T> listAll() {
        return listBean("select * from "+getTableName());
    }

    /**
     * 查询分页
     * @param clazz 结果类
     * @param pageNum 页码
     * @param pageSize 页长
     * @param sql 查询语句
     * @param obj 查询值
     * @param <E>
     * @return
     */
    @Override
    public <E> List<E> findPage(Class<E> clazz, Integer pageNum, Integer pageSize, String sql, Object... obj) {
        Criteria criteria = new Criteria();
        criteria.setPageNum(pageNum);
        criteria.setPageSize(pageSize);
        criteria.updateWhereSql(new StringBuilder(sql));
        criteria.updateParams(Arrays.asList(obj));
        return findAll(criteria,clazz,null);
    }

    /**
     * 查询分页
     * @param clazz 结果类
     * @param page 页
     * @param sql 查询语句
     * @param obj 查询值
     * @param <E>
     * @return
     */
    @Override
    public <E> Page findPage(Class<E> clazz, Page page, String sql, Object... obj) {
        Criteria criteria = new Criteria();
        criteria.setPageNum(page.getPageNumber());
        criteria.setPageSize(page.getPageSize());
        criteria.updateWhereSql(new StringBuilder(sql));
        criteria.updateParams(Arrays.asList(obj));
        List<E> pages = findAll(criteria,clazz,null);
        return new Page(pages,page.getPageNumber(),page.getPageSize(),pages.size(),pages.size());
    }

    /**
     * 根据查询策略删除
     * @param criteria 策略
     * @return
     */
    @Override
    public Integer delete(Criteria criteria) {
        StringBuilder deleteSql = new StringBuilder();
        Object[] objects = null;
        deleteSql.append("delete  from ").append(getTableName());
        if(criteria!=null){
            deleteSql.append(" where ").append(criteria.toSql());
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        return update(deleteSql.toString(), objects);
    }

    /**
     * sum统计
     * @param criteria 策略
     * @return
     */
    @Override
    public Long sum(Criteria criteria, String property) {
        StringBuilder sql = new StringBuilder();
        Object[] objects = null;
        sql.append("select sum(").append(property).append(") from ").append(getTableName());
        if(criteria!=null){
            sql.append(" where "+criteria.toSql());
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        BigDecimal sum = getValue(sql.toString(),objects);
        return sum == null ? BigDecimal.ZERO.longValue(): sum.longValue();
    }

    /**
     * 统计条数
     * @param criteria 策略
     * @return
     */
    @Override
    public Long count(Criteria criteria) {
        StringBuilder sql = new StringBuilder();
        Object[] objects = null;
        sql.append("select count(1) from ").append(getTableName());
        if(criteria!=null){
            sql.append(" where "+criteria.toSql());
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        return getValue(sql.toString(),objects);
    }

    /**
     * 更新
     * @param updateMap 更新值
     * @param id 主键值
     * @return
     */
    @Override
    public int update(Map<String, Object> updateMap, Object id) {
        return update(updateMap, Restrictions.eq("id",id));
    }

    /**
     * 更新
     * @param updateMap 更新值
     * @param criteria 条件值
     * @return
     */
    @Override
    public int update(Map<String, Object> updateMap, Criteria criteria) {
        List<Object> params = new ArrayList<>();
        String updateSql = getUpdateSqlByUnique(updateMap,criteria,params);
        return update(updateSql,params.toArray(new Object[params.size()]));
    }

    /**
     * 查询
     * @param id 主键值
     * @param selectHead 查询头
     * @return
     */
    @Override
    public T findOne(long id, String selectHead) {
        String sql = "select "+(!StringUtils.isEmpty(selectHead)?selectHead:"*")+" from "+getTableName()+" where id = ? limit 1";
        return getBean(sql,id);
    }

    /**
     * 查询
     * @param criteria 策略
     * @param clazz 结果类
     * @param selectHead 查询头
     * @return
     */
    @Override
    public <E> E findOne(Criteria criteria, Class<E> clazz, String selectHead) {
        String sql = "select "+(!StringUtils.isEmpty(selectHead)?selectHead:"*")+" from "+getTableName()+" where "+criteria.toSql()+" limit 1";
        Object[] objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        return getBeanByClass(sql,clazz==null?getDomainClass():clazz, objects);
    }

    /**
     * 查询所有
     * @param criteria 策略
     * @param clazz 结果类
     * @param selectHead 查询头
     * @return
     */
    @Override
    public <E> List<E> findAll(Criteria criteria, Class<E> clazz, String selectHead) {
        StringBuilder sql = new StringBuilder();
        Object[] objects = null;
        sql.append("select "+ (!StringUtils.isEmpty(selectHead)?selectHead:"*")+" from "+getTableName());
        if(criteria!=null){
            String s = criteria.toSql();
            if(s!=null&&s.trim().startsWith("limit")){
                sql.append(" "+s);
            }else{
                sql.append(" where "+s);
            }
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        return listBeanByClass(sql.toString(),clazz==null?getDomainClass():clazz,objects);
    }

    /**
     * 根据索引删除
     * @param ids 索引集合
     * @param uniqueName 索引名
     * @return
     */
    @Override
    public Integer delete(List<Long> ids, String uniqueName) {
        if(CollectionUtils.isEmpty(ids)){
            return -1;
        }
        String deleteSql = getDeleteSqlByIds(uniqueName,ids);
        return super.update(deleteSql, ids.toArray(new Object[ids.size()]));
    }

    /**
     * 原生语句
     * @param sql 操作语句
     * @param obj 操作值
     * @return
     */
    @Override
    public int execute(String sql, Object... obj) {
        return super.execute(sql, obj);
    }

    /**
     * 根据索引删除对象
     * @param uniqueName 索引名
     * @param unique 索引值
     * @return
     */
    @Override
    public Integer delete(Long unique, String uniqueName) {
        if(unique<=0){
            return -1;
        }
        return delete(Arrays.asList(unique),uniqueName);
    }

    /**
     * 聚合查询
     * @return
     */
    @Override
    public <E> List<E> aggregationAll(String sql, Object... obj) {
        return listValue(sql,obj);
    }

    /**
     * 聚合查询
     * @return
     */
    @Override
    public <E> E aggregation(String sql, Object... obj) {
        return getValue(sql,obj);
    }

    /**
     * 插入
     * @param sql 操作语句
     * @param obj 操作值
     * @return
     */
    @Override
    public Long insert(String sql, Object... obj) {
        return insertAndGetGeneratedKeys(sql, obj);
    }

    /**
     * 删除
     * @param sql 操作语句
     * @param obj 操作值
     * @return
     */
    @Override
    public int delete(String sql, Object... obj) {
        return update(sql, obj);
    }

    /**
     * 创建或更新对象（方法准备废弃，尽量不走这个方法）
     * @deprecated
     * @param element 对象
     * @return
     */
    @Deprecated
    public long saveOrUpDate(T element,boolean ignore){
        try {
            Field field = element.getClass().getDeclaredField("id");
            field.setAccessible(true);
            Object id = field.get(element);
            if (Objects.nonNull(id) && Long.parseLong(id.toString()) > 0L) {
                T object = getBean("select id from " + getTableName() + " where id = ? limit 1", id);
                if (object != null) {
                    return update(element);
                }
            }
        }catch (Exception e){
            stackTrace("saveOrUpDate{}", e);
        }
        return add(element,ignore);
    }
}