package core.db.persistence;

import core.db.other.Criteria;

import java.util.List;
import java.util.Map;

public interface Repository<T> {
    long add(T t, boolean ignore);

    int update(T t);

    Integer delete(long id);

    Integer delete(List<Long> ids);

    Long count();

    List<T> findAll();

    T findOne(long id);

    <E> List<E> loadAll(Class<E> clazz, String sql, Object... obj);

    <E> E loadOne(Class<E> clazz, String sql, Object... obj);

    int execute(String sql, Object... obj);

    Long count(String sql, Object... obj);

    List<T> findPage(Integer firstIndex, Integer maxResults);

    <E> List<E> findPage(Class<E> clazz, Integer pageNum, Integer pageSize, String sql, Object... obj);

    <E> Page findPage(Class<E> clazz, Page page, String sql, Object... obj);

    Integer delete(Criteria criteria);

    int deleteAll();

    Long sum(Criteria criteria, String property);

    Long count(Criteria criteria);

    int update(Map<String, Object> updateMap, Object id);

    int update(Map<String, Object> updateMap, Criteria criteria);

    T findOne(long id, String selectHead);

    <E> E findOne(Criteria criteria, Class<E> clazz, String selectHead);

    <E> List<E> findAll(Criteria criteria, Class<E> clazz, String selectHead);

    Integer delete(List<Long> ids, String uniqueName);

    Integer delete(Long unique, String uniqueName);

    <E> List<E> aggregationAll(String sql, Object... obj);

    <E> E aggregation(String sql, Object... obj);

    int update(String sql, Object... obj);

    Long insert(String sql, Object... obj);

    int delete(String sql, Object... obj);

}
