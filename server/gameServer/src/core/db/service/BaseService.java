package core.db.service;

import core.db.other.AsyncInfo;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础Service，可扩展接口
 *
 * @param <T> 操作实体类
 * @see AsyncInfo,Restrictions, CustomerDao
 */
public interface BaseService<T> {

    /**
     * 默认Dao操作层
     *
     * @return
     */
    CustomerDao<T> getDefaultDao();

    /**
     * 更新操作（异步）
     *
     * @param fieldName  字段名
     * @param fieldValue 字段值
     * @param id         主键
     * @param asyncInfo  异步容器
     * @return 返回操作结果
     */
    default int update(String fieldName, Object fieldValue, Object id, AsyncInfo asyncInfo) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(fieldName, fieldValue);
        return getDefaultDao().update(updateMap, id);
    }

    /**
     * 更新操作（同步）
     *
     * @param fieldName  字段名
     * @param fieldValue 字段值
     * @param id         主键
     * @return 返回操作结果
     */
    default int update(String fieldName, Object fieldValue, Object id) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(fieldName, fieldValue);
        return getDefaultDao().update(updateMap, id);
    }

    /**
     * 更新操作（异步）
     *
     * @param updateMap (key->value)  key是数据表字段名，value是欲更新的字段值
     * @param id        主键
     * @param asyncInfo 异步容器
     * @return 返回操作结果
     */
    default int update(Map<String, Object> updateMap, Object id, AsyncInfo asyncInfo) {
        if (null == updateMap || updateMap.size() <= 0) {
            return 0;
        }
        return getDefaultDao().update(updateMap, id);
    }

    /**
     * 更新操作（同步）
     *
     * @param updateMap (key->value)  key是数据表字段名，value是欲更新的字段值
     * @param id        主键
     * @return 返回操作结果
     */
    default int update(Map<String, Object> updateMap, Object id) {
        if (null == updateMap || updateMap.size() <= 0) {
            return 0;
        }
        return getDefaultDao().update(updateMap, id);
    }

//    public int update(Map<String,Object> updateMap, Criteria criteria) {


    /**
     * 更新操作（同步）
     *
     * @param updateMap (key->value)  key是数据表字段名，value是欲更新的字段值
     * @param criteria  主键
     * @return 返回操作结果
     */
    default int update(Map<String, Object> updateMap, Criteria criteria) {
        if (null == updateMap || updateMap.size() <= 0) {
            return 0;
        }
        return getDefaultDao().update(updateMap, criteria);
    }

    /**
     * 更新操作（异步）
     *
     * @param updateMap (key->value)  key是数据表字段名，value是欲更新的字段值
     * @param criteria  主键
     * @param asyncInfo 异步容器
     * @return 返回操作结果
     */
    default int update(Map<String, Object> updateMap, Criteria criteria, AsyncInfo asyncInfo) {
        if (null == updateMap || updateMap.size() <= 0) {
            return 0;
        }
        return getDefaultDao().update(updateMap, criteria);
    }


    /**
     * 保存或者更新（异步）
     *
     * @param element   欲操作实体
     * @param asyncInfo 异步容器
     * @return 返回操作结果
     */
    default long saveOrUpDate(T element, AsyncInfo asyncInfo) {
        return getDefaultDao().saveOrUpDate(element, false);
    }

    /**
     * 保存或者更新（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    default long saveOrUpDate(T element) {
        return getDefaultDao().saveOrUpDate(element, false);
    }

    /**
     * 保存或者更新（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    default long saveIgnoreOrUpDate(T element) {
        return getDefaultDao().saveOrUpDate(element, true);
    }

    /**
     * 保存或者更新（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    default long saveIgnoreOrUpDate(T element, AsyncInfo asyncInfo) {
        return getDefaultDao().saveOrUpDate(element, true);
    }

    /**
     * 保存操作（异步）
     *
     * @param element   欲操作实体
     * @param asyncInfo 异步容器
     * @return 返回操作结果
     */
    default long save(T element, AsyncInfo asyncInfo) {
        return getDefaultDao().add(element, false);
    }

    /**
     * 保存操作（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    default long save(T element) {
        return getDefaultDao().add(element, false);
    }

    default Long insert(String sql, AsyncInfo asyncInfo, Object... obj) {
        return getDefaultDao().insert(sql, obj);
    }

    default Long insert(String sql, Object... obj) {
        return getDefaultDao().insert(sql, obj);
    }


    /**
     * 如果是用主键primary或者唯一索引unique区分了记录的唯一性,避免重复插入记录可以使用：
     * 保存操作（异步）
     *
     * @param element   欲操作实体
     * @param asyncInfo 异步容器
     * @return 返回操作结果
     */
    default long saveIgnore(T element, AsyncInfo asyncInfo) {
        return getDefaultDao().add(element, true);
    }

    /**
     * 如果是用主键primary或者唯一索引unique区分了记录的唯一性,避免重复插入记录可以使用：
     * 保存操作（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    default long saveIgnore(T element) {
        return getDefaultDao().add(element, true);
    }

    /**
     * 更新操作（异步）
     *
     * @param element   欲操作实体
     * @param asyncInfo 异步容器
     * @return 返回操作结果
     */
    default int update(T element, AsyncInfo asyncInfo) {
        return getDefaultDao().update(element);
    }

    /**
     * 更新操作（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    default int update(T element) {
        return getDefaultDao().update(element);
    }

    /**
     * 查询所有（异步）
     *
     * @param criteria   策略器
     * @param selectHead 查询头，自己拼接，没有就null
     * @param asyncInfo  异步容器
     * @return 返回查询实体
     * @see Restrictions
     */
    default List<T> findAll(Criteria criteria, String selectHead, AsyncInfo asyncInfo) {
        return getDefaultDao().findAll(criteria, null, selectHead);
    }

    /**
     * 查询所有（同步）
     *
     * @param criteria   策略器
     * @param selectHead 查询头，自己拼接，没有就null
     * @return 返回查询实体
     * @see Restrictions
     */
    default List<T> findAll(Criteria criteria, String selectHead) {
        return getDefaultDao().findAll(criteria, null, selectHead);
    }

    /**
     * 查询所有（同步）
     *
     * @param criteria criteria 策略器
     * @return
     */
    default List<T> findAll(Criteria criteria) {
        // createResultSetHandler 是否需要转化,正常false即可,selectHead 查询头，自己拼接，没有就null
        return getDefaultDao().findAll(criteria, null, null);
    }

    /**
     * 查询所有（异步）
     *
     * @param criteria  criteria 策略器
     * @param asyncInfo 异步容器
     * @return
     */
    default List<T> findAll(Criteria criteria, AsyncInfo asyncInfo) {
        return getDefaultDao().findAll(criteria, null, null);
    }

    /**
     * 查询所有（同步
     *
     * @param criteria   criteria 策略器
     * @param clazz      欲执行查询类
     * @param selectHead 查询头，自己拼接，没有就null
     * @return
     */
    default <E> List<E> findAllE(Criteria criteria, Class<E> clazz, String selectHead) {
        return getDefaultDao().findAll(criteria, clazz, selectHead);
    }

    /**
     * 查询所有（异步
     *
     * @param criteria   criteria 策略器
     * @param clazz      欲执行查询类
     * @param selectHead 查询头，自己拼接，没有就null
     * @return
     */
    default <E> List<E> findAllE(Criteria criteria, Class<E> clazz, String selectHead, AsyncInfo asyncInfo) {
        return getDefaultDao().findAll(criteria, clazz, selectHead);
    }

    /**
     * 查询单一（异步）
     *
     * @param criteria   策略器
     * @param selectHead 查询头，自己拼接，没有就null
     * @param asyncInfo  异步容器
     * @return 返回查询实体
     * @see Restrictions
     */
    default T findOne(Criteria criteria, String selectHead, AsyncInfo asyncInfo) {
        return getDefaultDao().findOne(criteria, null, selectHead);
    }

    /**
     * 查询单一（同步）
     *
     * @param criteria   策略器
     * @param selectHead 查询头，自己拼接，没有就null
     * @return 返回查询实体
     * @see Restrictions
     */
    default T findOne(Criteria criteria, String selectHead) {
        return getDefaultDao().findOne(criteria, null, selectHead);
    }


    /**
     * 查询单一（同步）
     *
     * @param <E>
     * @param criteria   查询策略
     * @param clazz      欲执行查询类
     * @param selectHead 查询头，自己拼接，没有就null
     * @return 返回查询实体
     */
    default <E> E findOneE(Criteria criteria, Class<E> clazz, String selectHead) {
        return getDefaultDao().findOne(criteria, clazz, selectHead);
    }

    /**
     * 查询单一（异步）
     *
     * @param id         主键值
     * @param selectHead 查询头，自己拼接，没有就null
     * @param asyncInfo  异步容器
     * @return 返回查询实体
     */
    default T findOne(long id, String selectHead, AsyncInfo asyncInfo) {
        return getDefaultDao().findOne(id, selectHead);
    }

    /**
     * 查询单一（同步）
     *
     * @param id         主键值
     * @param selectHead 查询头，自己拼接，没有就null
     * @return 返回查询实体
     */
    default T findOne(long id, String selectHead) {
        return getDefaultDao().findOne(id, selectHead);
    }

    /**
     * 删除（异步）
     *
     * @param ids       主键值列表
     * @param asyncInfo 异步容器
     * @return 返回操作结果
     */
    default Integer delete(List<Long> ids, AsyncInfo asyncInfo) {
        return getDefaultDao().delete(ids);
    }

    /**
     * 删除（同步）
     *
     * @param ids 主键值列表
     * @return 返回操作结果
     */
    default Integer delete(List<Long> ids) {
        return getDefaultDao().delete(ids);
    }

    /**
     * 删除(同步)
     *
     * @param criteria 查询策略
     * @return
     */
    default Integer delete(Criteria criteria) {
        return getDefaultDao().delete(criteria);
    }

    /**
     * 删除(同步)
     *
     * @param criteria 查询策略
     * @return
     */
    default Integer delete(Criteria criteria, AsyncInfo asyncInfo) {
        return getDefaultDao().delete(criteria);
    }

    /**
     * 删除（异步）
     *
     * @param id        主键值
     * @param asyncInfo 异步容器
     * @return 返回操作结果
     */
    default Integer delete(long id, AsyncInfo asyncInfo) {
        return getDefaultDao().delete(id);
    }

    /**
     * 删除（同步）
     *
     * @param id 主键值
     * @return 返回操作结果
     */
    default Integer delete(long id) {
        return getDefaultDao().delete(id);
    }

    /**
     * 删除所有（异步）
     *
     * @param asyncInfo 异步容器
     * @return 返回操作结果
     */
    default Integer deleteAll(AsyncInfo asyncInfo) {
        return getDefaultDao().deleteAll();
    }


    /**
     * 删除所有（同步）
     *
     * @return 返回操作结果
     */
    default Integer deleteAll() {
        return getDefaultDao().deleteAll();
    }

    /**
     * 删除（异步）
     *
     * @param unique    主键值
     * @param unique    主键名
     * @param asyncInfo 异步容器
     * @return 返回操作结果
     */
    default Integer delete(Long unique, String uniqueName, AsyncInfo asyncInfo) {
        return getDefaultDao().delete(unique, uniqueName);
    }

    /**
     * 删除（同步）
     *
     * @param unique 主键值
     * @param unique 主键名
     * @return 返回操作结果
     */
    default Integer delete(Long unique, String uniqueName) {
        return getDefaultDao().delete(unique, uniqueName);
    }

    /**
     * 批量操作
     *
     * @param sql    操作的语句
     * @param params 操作的二维参数
     * @return
     */
    default int[] batch(String sql, Object[][] params) {
        return getDefaultDao().batch(sql, params);
    }

    /***
     * 查询所有（同步）UNION
     * @param sqlOne  第一条查询语句
     * @param sqlTwo 第二条查询语句
     * @param criteria 所有的参数条件
     * @param criteriaOrder 排序页码等条件
     * @return
     */
    default <E> List<E> findListUnion(String sqlOne, String sqlTwo, Criteria criteria, Criteria criteriaOrder, Class<E> clazz) {
        // createResultSetHandler 是否需要转化,正常false即可,selectHead 查询头，自己拼接，没有就null
        return getDefaultDao().findListUnion(sqlOne, sqlTwo, clazz, criteria, criteriaOrder);
    }

    /**
     * 查询所有（同步）UNION
     *
     * @param criteria criteria 参数条件
     * @return
     */
    default String findListUnionSql(Criteria criteria) {
        return getDefaultDao().findAllUnionSql(criteria, null);
    }
}
