/*******************************************************************************
 * Copyright (c) 2005, 2014 zzy.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *******************************************************************************/
package server.aoo.dao.service.game;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import com.ddm.server.common.utils.CommLogD;
import jsproto.c2s.SData_Result;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import server.aoo.dao.entity.IdEntity;
import server.aoo.dao.search.DynamicSpecifications;
import server.aoo.dao.search.SearchFilter;
import server.aoo.dao.search.SearchParam;
import server.aoo.dao.search.SearchPredicate;


/**
 * service 基类
 * @author lll 2015年5月28日
 */
@Transactional(readOnly = false,transactionManager = "gameTransactionManager")
public class BaseGameService {

//	@Autowired
//	protected EntityManagerFactory emf;

	@PersistenceContext(unitName = "game")
	private EntityManager entityManager;

	@Transactional(readOnly = false,transactionManager = "gameTransactionManager")
	public <T extends IdEntity> void persist(T entity) {
		this.entityManager.persist(entity);
	}
	
	public <T extends IdEntity> void refresh(T entity) {
		this.entityManager.refresh(entity);
	}
	
	@Transactional(readOnly = false,transactionManager = "gameTransactionManager")
	public <T extends IdEntity> T merge(T entity) {
		return this.entityManager.merge(entity);
	}
	
	@Transactional(readOnly = false,transactionManager = "gameTransactionManager")
	public <T extends IdEntity> Collection<T> batchSave(Collection<T> entities) {
		return batchSaveInternal(entities, entityManager, true);
	}
	
	private <T extends IdEntity> Collection<T> batchSaveInternal(Collection<T> entities, EntityManager entityManager, boolean autoFlush) {
		final List<T> savedEntities = new ArrayList<T>(entities.size());
		int i = 0;
		for (T t : entities) {
			// 插入或更新
			savedEntities.add(persistOrMergeInternal(t, entityManager));
			i++;
			if (autoFlush && i % 50 == 0) {
				entityManager.flush();
				entityManager.clear();
			}
		}
		return savedEntities;
	}

	public List<Future<Integer>> concurrencyBatchSave(List list, ExecutorService executorService) {
		return submitBatchSaves(list, executorService, 50);
	}
	
	public List<Future<Integer>> concurrencyBatchSave(List list, ExecutorService executorService, int sliceSize) {
		return submitBatchSaves(list, executorService, sliceSize);
	}

	private List<Future<Integer>> submitBatchSaves(List list, ExecutorService executorService, int sliceSize) {

		List<Future<Integer>> resultList = new ArrayList<Future<Integer>>();
		BaseGameService service = this;
		int size = list.size();
		for (int i = 0; i < size; i = i + sliceSize) {
			int toIndex = (i + sliceSize < list.size() ? (i + sliceSize) : list.size());
			List sublist = list.subList(i, toIndex);
			// 使用ExecutorService执行Callable类型的任务，并将结果保存在future变量中
			Future<Integer> future = executorService.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
//					EntityManager em = emf.createEntityManager();
					EntityTransaction transaction = entityManager.getTransaction();
					transaction.begin();
					try {
						service.batchSaveInternal(sublist, entityManager, false);
						entityManager.flush();
						entityManager.clear();
						transaction.commit();
					} catch (Exception e) {
						transaction.rollback();
						CommLogD.error(e.getMessage(),e);
						throw e;
					} finally {
						entityManager.close();
					}
					return toIndex;
				}
			});
			// 将任务执行结果存储到List中
			resultList.add(future);
		}
		executorService.shutdown();
		return resultList;
	}

	@Transactional(readOnly = false,transactionManager = "gameTransactionManager")
	public <T extends IdEntity> T persistOrMerge(T t) {
		return persistOrMergeInternal(t, entityManager);
	}
	
	private <T extends IdEntity> T persistOrMergeInternal(T t, EntityManager entityManager) {
		if (t.getId() <=0) {
			// 插入数据
			entityManager.persist(t);
			return t;
		} else {
			// 更新数据
			return entityManager.merge(t);
		}
	}
	
	/**
	 * 创建动态查询条件组合.
	 */
	protected <T> Specification<T> createSpecification(SearchParam searchParam, Specification<T> spec, final Class<T> entityClass) {
		List<SearchFilter> filters = searchParam.getFilters();
		Specification<T> specT = DynamicSpecifications.bySearchFilter(filters, entityClass);
		
		if (spec != null) {
			return specT.and(spec);
		} else {
			return specT;
		}
		
	}
	
	/**
	 * 创建动态查询条件组合.
	 */
	protected <T> Specification<T> createSpecification(SearchParam searchParam, final Class<T> entityClass) {
		return createSpecification(searchParam, null, entityClass);
	}
	
	/**
	 * 创建动态查询条件组合.
	 */
	protected <T> Specification<T> createSpecification(Map<String, Object> searchParams, Specification<T> spec, final Class<T> entityClass) {
		Map<String, SearchFilter> filters = SearchFilter.parse(searchParams);
		SearchParam param = new SearchParam();
		param.setFilters(Arrays.asList(filters.values().toArray(new SearchFilter[filters.values().size()])));
		return createSpecification(param, spec, entityClass);
	}
	
	/**
	 * 创建动态查询条件组合.
	 */
	protected <T> Specification<T> createSpecification(Map<String, Object> searchParams, final Class<T> entityClass) {
		return this.createSpecification(searchParams, null, entityClass);
	}

	/**
	 * 创建分页请求.
	 * 
	 * @param pageNumber
	 *            页号
	 * @param pageSize
	 *            每页大小
	 * @param sortType
	 *            排序字段 默认：id 支持按多字段排序 "id:asc,name:desc" "id,name:desc"
	 * @param asc
	 *            是否升序
	 * @return PageRequest
	 */
	public PageRequest createPageRequest(int pageNumber, int pageSize, String sortType, boolean asc) {
		return PageRequest.of(pageNumber, pageSize, this.createSort(sortType, asc));
	}
	
	public PageRequest createPageRequest(SearchPredicate predicate) {
		return PageRequest.of(predicate.getPageNumber(), predicate.getPageSize(), this.createSort(predicate.getSort(), false));
	}

	/**
	 * 
	 * @param sortType
	 *            排序字段 默认：id 支持按多字段排序 "id:asc,name:desc" "id,name:desc"
	 * @param asc
	 *            是否升序
	 * @return
	 */
	protected Sort createSort(String sortType, boolean asc) {
		Sort sort = null;
		Direction defaultDirection = asc ? Direction.ASC : Direction.DESC;

		// 设置默认值
		if (StringUtils.isBlank(sortType)) {
			sortType = "id";
		}

		List<Sort.Order> orders = Lists.newArrayList();
		Direction otherDirection;
		for (String sortStr : sortType.split(",")) {
			String sortAndDirection[] = sortStr.split(":");
			if (sortAndDirection.length < 1) {
				continue;
			}
			if (sortAndDirection.length == 2) {
				otherDirection = "asc".equalsIgnoreCase(sortAndDirection[1]) ? Direction.ASC
						: Direction.DESC;
			} else {
				otherDirection = defaultDirection;
			}
			orders.add(new Sort.Order(otherDirection, sortAndDirection[0]));
		}
		sort = Sort.by(orders);
		return sort;
	}


	
	/**
	 * 统计数量
	 * @param entityClass
	 * @param spec
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public long countBySpec(Specification spec, Class entityClass) {
		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
		Root root = countQuery.from(entityClass);
		
		Predicate predicate = spec.toPredicate(root, countQuery, builder);
		countQuery.where(predicate);
		countQuery.distinct(true);
		countQuery.select(builder.count(root));
		
		List<Long> totals = entityManager.createQuery(countQuery).getResultList();
		Long total = 0L;
		for (Long element : totals) {
			total += element == null ? 0 : element;
		}
		return total;
	}

	public List<Map<String, Object>> queryBySql(String sql) {
		Query query = entityManager.createNativeQuery(sql, Tuple.class);
		@SuppressWarnings("unchecked")
		List<Tuple> rows = query.getResultList();
		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		for (Tuple row : rows) {
			Map<String, Object> values = new LinkedHashMap<>();
			row.getElements().forEach(element -> values.put(element.getAlias(), row.get(element)));
			list.add(values);
		}
		return list;
	}


	@Transactional(transactionManager = "gameTransactionManager")
	public void excuteSql(String sql) {
		Query query = entityManager.createNativeQuery(sql);
		query.executeUpdate();
	}


}
