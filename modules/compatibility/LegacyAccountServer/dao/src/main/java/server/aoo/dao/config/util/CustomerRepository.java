package server.aoo.dao.config.util;

import java.util.List;
import java.util.Map;

/**
 * 自定义操作接口
 * @param <T>
 */
public interface CustomerRepository<T> {
    /**
     * 添加
     * @param t 添加的实体
     * @param ignore 是否insert ignore
     * @return
     */
    long add(T t, boolean ignore);

    /**
     * 自定义主键插入
     * @param t
     * @param ignore
     * @return
     */
    long addById(T t, boolean ignore);

    /**
     * 更新,根据实体主键值更新
     * @param t
     * @return
     */
    int update(T t);

    /**
     * 根据主键删除
     * @param id
     * @return
     */
    Integer delete(long id);

    /**
     * 根据list删除实体
     * @param ids
     * @return
     */
    Integer delete(List<Long> ids);

    /**
     * 查询所有
     * @return
     */
    List<T> listAll();

    /**
     * 查询单实体
     * @param id
     * @return
     */
    T findOne(long id);

    /**
     * 获取自定义查询实体
     * @param clazz
     * @param sql
     * @param obj
     * @param <E>
     * @return
     */
    <E> List<E> loadAll(Class<E> clazz, String sql, Object... obj);

    /**
     *  获取自定义查询实体
     * @param clazz
     * @param sql
     * @param obj
     * @param <E>
     * @return
     */
    <E> E loadOne(Class<E> clazz, String sql, Object... obj);

    /**
     * 执行语句
     * @param sql
     * @param obj
     * @return
     */
    int execute(String sql, Object... obj);

    /**
     * 统计条数
     * @param sql
     * @param obj
     * @return
     */
    Long count(String sql, Object... obj);

    /**
     * 分页查询
     * @param firstIndex
     * @param maxResults
     * @return
     */
    List<T> findPage(Integer firstIndex, Integer maxResults);

    /**
     * 分页查询根据条件
     * @param clazz
     * @param pageNum
     * @param pageSize
     * @param sql
     * @param obj
     * @param <E>
     * @return
     */
    <E> List<E> findPage(Class<E> clazz, Integer pageNum, Integer pageSize, String sql, Object... obj);

    /**
     * 分页查询
     * @param clazz
     * @param page
     * @param sql
     * @param obj
     * @param <E>
     * @return
     */
    <E> Page findPage(Class<E> clazz, Page page, String sql, Object... obj);

    /**
     * 删除
     * @param criteria
     * @return
     */
    Integer delete(Criteria criteria);

    /**
     * 求和
     * @param criteria
     * @param property
     * @return
     */
    Long sum(Criteria criteria, String property);

    /**
     * 统计
     * @param criteria
     * @return
     */
    Long count(Criteria criteria);

    /**
     * 更新
     * @param updateMap
     * @param id
     * @return
     */
    int update(Map<String, Object> updateMap, Object id);

    /**
     * 更新
     * @param updateMap
     * @param criteria
     * @return
     */
    int update(Map<String,Object> updateMap, Criteria criteria);

    /**
     * 查询
     * @param id
     * @param selectHead
     * @return
     */
    T findOne(long id, String selectHead);

    /**
     * 查询
     * @param criteria
     * @param clazz
     * @param selectHead
     * @param <E>
     * @return
     */
    <E> E findOne(Criteria criteria, Class<E> clazz, String selectHead);

    /**
     * 查询所有
     * @param criteria
     * @param clazz
     * @param selectHead
     * @param <E>
     * @return
     */
    <E> List<E> findAll(Criteria criteria, Class<E> clazz, String selectHead);

    Integer delete(List<Long> ids, String uniqueName);

    Integer delete(Long unique, String uniqueName);

    <E> List<E> aggregationAll(String sql, Object... obj);

    <E> E aggregation(String sql, Object... obj);

    int update(String sql, Object... obj);

    Long insert(String sql, Object... obj);

    int delete(String sql, Object... obj);

}
