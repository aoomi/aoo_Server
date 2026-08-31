package core.db.persistence;

import core.db.other.Criteria;
import core.db.other.Restrictions;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

/**
 * 用户自定义操作层
 *
 * @param <T>
 */
public abstract class CustomerDao<T> extends BaseDao<T> implements Repository<T> {
    /**
     * 自动生成操作的实体类
     */
    protected CustomerDao() {
        super();
    }

    /**
     * 手动设置操作的实体类
     *
     * @param clz
     */
    protected CustomerDao(Class<T> clz) {
        super(clz);
    }

    /**
     * 新增
     *
     * @param element 新增实体
     * @param ignore  插入异常是否报错终止
     * @return
     */
    @Override
    public long add(T element, boolean ignore) {
        Class clazz = element.getClass();
        Field[] fields = clazz.getDeclaredFields();
        List<Object> params = new ArrayList<>();
        String sql = getInsertSql(element, params, ignore);
        long id = insertAndGetGeneratedKeys(sql, params.toArray(new Object[params.size()]));
        //新增后赋值主键
        if (id > 0) {
            Optional<Field> idFieldOp = Arrays.asList(fields).stream().filter(field -> "id".equalsIgnoreCase(field.getName())).findFirst();
            idFieldOp.ifPresent(field -> {
                try {
                    field.setAccessible(true);
                    field.set(element, id);
                } catch (Exception e) {
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
        List<Object> objects = new ArrayList<>();
        String sql = getUpdateSqlByEntity(element, objects);
        return super.update(sql, objects.toArray(new Object[objects.size()]));
    }

    /**
     * 原生语句
     *
     * @param sql 操作语句
     * @param obj 操作值
     * @return
     */
    @Override
    public int execute(String sql, Object... obj) {
        return super.execute(sql, obj);
    }

    /**
     * 统计
     *
     * @return
     */
    @Override
    public Long count() {
        BigDecimal count = getValue("select count(1) from " + getTableName());
        return count == null ? BigDecimal.ZERO.longValue() : count.longValue();
    }

    /**
     * 根据id删除
     *
     * @param id 主键值
     * @return
     */
    @Override
    public Integer delete(long id) {
        if (id <= 0) {
            return -1;
        }
        return delete(Arrays.asList(id));
    }

    /**
     * 查询
     *
     * @param id 主键值
     * @return
     */
    @Override
    public T findOne(long id) {
        String sql = "select * from " + getTableName() + " where id = ? limit 1";
        return getBean(sql, id);
    }

    /**
     * 查询所有
     *
     * @return
     */
    @Override
    public List<T> findAll() {
        return listBean("select * from " + getTableName());
    }

    /**
     * 加载
     *
     * @param clazz 结果类
     * @param sql   查询语句
     * @param obj   查询值
     * @param <E>
     * @return
     */
    @Override
    public <E> E loadOne(Class<E> clazz, String sql, Object... obj) {
        return getBeanByClass(sql, clazz == null ? this.clazz : clazz, obj);
    }

    /**
     * 加载所有
     *
     * @param clazz 结果类
     * @param sql   查询语句
     * @param obj   查询值
     * @param <E>
     * @return
     */
    @Override
    public <E> List<E> loadAll(Class<E> clazz, String sql, Object... obj) {
        return listBeanByClass(sql, clazz == null ? this.clazz : clazz, obj);
    }

    /**
     * 统计
     *
     * @param sql 统计语句
     * @param obj 统计值
     * @return
     */
    @Override
    public Long count(String sql, Object... obj) {
        BigDecimal count = getValue(sql, obj);
        return count == null ? BigDecimal.ZERO.longValue() : count.longValue();
    }

    /**
     * 查询分页
     *
     * @param pageNum  页码
     * @param pageSize 页长
     * @return
     */
    @Override
    public List<T> findPage(Integer pageNum, Integer pageSize) {
        Criteria criteria = new Criteria();
        criteria.setPageNum(pageNum);
        criteria.setPageSize(pageSize);
        return findAll(criteria, null, null);
    }

    /**
     * 查询分页
     *
     * @param clazz    结果类
     * @param pageNum  页码
     * @param pageSize 页长
     * @param sql      查询语句
     * @param obj      查询值
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
        return findAll(criteria, clazz, null);
    }

    /**
     * 查询分页
     *
     * @param clazz 结果类
     * @param page  页
     * @param sql   查询语句
     * @param obj   查询值
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
        List<E> pages = findAll(criteria, clazz, null);
        return new Page(pages, 10, 10, 10, 10);
    }

    /**
     * 删除
     *
     * @param ids 索引集合
     * @return
     */
    @Override
    public Integer delete(List<Long> ids) {
        return delete(ids, null);
    }

    /**
     * 根据查询策略删除
     *
     * @param criteria 策略
     * @return
     */
    @Override
    public Integer delete(Criteria criteria) {
        StringBuilder deleteSql = new StringBuilder();
        Object[] objects = null;
        deleteSql.append("delete  from " + getTableName());
        if (criteria != null) {
            deleteSql.append(" where " + criteria.toSql());
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        int i = super.update(deleteSql.toString(), objects);
        return i;
    }

    /**
     * 根据索引删除
     *
     * @param ids        索引集合
     * @param uniqueName 索引名
     * @return
     */
    @Override
    public Integer delete(List<Long> ids, String uniqueName) {
        String deleteSql = getDeleteSqlByIds(uniqueName, ids);
        int i = super.update(deleteSql, ids.toArray(new Object[ids.size()]));
        return i;
    }

    /**
     * 根据索引删除对象
     *
     * @param uniqueName 索引名
     * @param unique     索引值
     * @return
     */
    @Override
    public Integer delete(Long unique, String uniqueName) {
        if (unique <= 0) {
            return -1;
        }
        return delete(Arrays.asList(unique), uniqueName);
    }

    /**
     * 删除所有
     *
     * @return
     */
    @Override
    public int deleteAll() {
        return this.execute("delete from " + getTableName());
    }

    /**
     * 查询所有
     *
     * @param criteria   策略
     * @param clazz      结果类
     * @param selectHead 查询头
     * @return
     */
    @Override
    public <E> List<E> findAll(Criteria criteria, Class<E> clazz, String selectHead) {
        StringBuilder sql = new StringBuilder();
        Object[] objects = null;
        sql.append("select " + (!StringUtils.isEmpty(selectHead) ? selectHead : "*") + " from " + getTableName());
        if (criteria != null) {
            sql.append(" where " + criteria.toSql());
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        return listBeanByClass(sql.toString(), clazz == null ? this.clazz : clazz, objects);
    }

    /**
     * 查询
     *
     * @param criteria   策略
     * @param clazz      结果类
     * @param selectHead 查询头
     * @return
     */
    @Override
    public <E> E findOne(Criteria criteria, Class<E> clazz, String selectHead) {
        String sql = "select " + (!StringUtils.isEmpty(selectHead) ? selectHead : "*") + " from " + getTableName() + " where " + criteria.toSql() + " limit 1";
        Object[] objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        return getBeanByClass(sql, clazz == null ? this.clazz : clazz, objects);
    }

    /**
     * 查询
     *
     * @param id         主键值
     * @param selectHead 查询头
     * @return
     */
    @Override
    public T findOne(long id, String selectHead) {
        String sql = "select " + (!StringUtils.isEmpty(selectHead) ? selectHead : "*") + " from " + getTableName() + " where id = ? limit 1";
        return getBean(sql, id);
    }

    /**
     * 更新
     *
     * @param updateMap 更新值
     * @param id        主键值
     * @return
     */
    @Override
    public int update(Map<String, Object> updateMap, Object id) {
        return update(updateMap, Restrictions.eq("id", id));
    }

    /**
     * 更新
     *
     * @param updateMap 更新值
     * @param criteria  条件值
     * @return
     */
    @Override
    public int update(Map<String, Object> updateMap, Criteria criteria) {
        List<Object> params = new ArrayList<>();
        String updateSql = getUpdateSqlByUnique(updateMap, criteria, params);
        return update(updateSql, params.toArray(new Object[params.size()]));
    }

    /**
     * 统计条数
     *
     * @param criteria 策略
     * @return
     */
    @Override
    public Long count(Criteria criteria) {
        StringBuilder sql = new StringBuilder();
        Object[] objects = null;
        sql.append("select count(1) from " + getTableName());
        if (criteria != null) {
            sql.append(" where " + criteria.toSql());
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        BigDecimal count = getValue(sql.toString(), objects);
        return count == null ? BigDecimal.ZERO.longValue() : count.longValue();
    }

    /**
     * 聚合查询
     *
     * @return
     */
    @Override
    public <E> List<E> aggregationAll(String sql, Object... obj) {
        List<E> valueList = listValue(sql, obj);
        return valueList;
    }

    /**
     * 聚合查询
     *
     * @return
     */
    @Override
    public <E> E aggregation(String sql, Object... obj) {
        E value = getValue(sql, obj);
        return value;
    }

    @Override
    public int update(String sql, Object... obj) {
        int i = super.update(sql, obj);
        return i;
    }

    /**
     * sum统计
     *
     * @param criteria 策略
     * @return
     */
    @Override
    public Long sum(Criteria criteria, String property) {
        StringBuilder sql = new StringBuilder();
        Object[] objects = null;
        sql.append("select sum(" + property + ") from " + getTableName());
        if (criteria != null) {
            sql.append(" where " + criteria.toSql());
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        BigDecimal sum = getValue(sql.toString(), objects);
        return sum == null ? BigDecimal.ZERO.longValue() : sum.longValue();
    }

    /**
     * 创建或更新对象（方法准备废弃，尽量不走这个方法）
     *
     * @param element 对象
     * @return
     * @deprecated
     */
    @Deprecated
    public long saveOrUpDate(T element, boolean ignore) {
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
        } catch (Exception e) {
            stackTrace("saveOrUpDate{}", e);
        }
        return add(element, ignore);
    }


    /**
     * 插入
     *
     * @param sql 操作语句
     * @param obj 操作值
     * @return
     */
    @Override
    public Long insert(String sql, Object... obj) {
        return super.insertAndGetGeneratedKeys(sql, obj);
    }

    /**
     * 删除
     *
     * @param sql 操作语句
     * @param obj 操作值
     * @return
     */
    @Override
    public int delete(String sql, Object... obj) {
        return super.update(sql, obj);
    }

    /**
     * 查询union
     *
     * @return
     */
    public <E> List<E> findListUnion(String sqlOne, String sqlTwo, Class<E> clazz, Criteria criteria, Criteria criteriaOrder) {
        String sql = "(" + sqlOne + ") union all (" + sqlTwo + "";
        Object[] objects = null;
        StringBuilder order = new StringBuilder();
        //参数处理
        if (criteria != null) {
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        //拼接排序页码等sql
        if (criteriaOrder != null) {
            order.append(criteriaOrder.toSql());
        }
        return listBeanByClass(sql + order.toString(), clazz == null ? this.clazz : clazz, objects);
    }

    /**
     * 查询sql
     *
     * @return
     */

    public String findAllUnionSql(Criteria criteria, String selectHead) {
        StringBuilder sql = new StringBuilder();
        sql.append("select " + (!StringUtils.isEmpty(selectHead) ? selectHead : "*") + " from " + getTableName());
        if (criteria != null) {
            sql.append(" where " + criteria.toSql());
        }
        return sql.toString();
    }

}
