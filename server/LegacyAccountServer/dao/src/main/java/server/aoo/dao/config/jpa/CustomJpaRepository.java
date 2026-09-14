package server.aoo.dao.config.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.util.Assert;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 自定义JpaRepository实现
 * 		pageNumber 超过最大值时，始终返回最后一页内容
 * @author Administrator
 */
public class CustomJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {
	
	/**
	 * Creates a new {@link SimpleJpaRepository} to manage objects of the given {@link JpaEntityInformation}.
	 * 
	 * @param entityInformation must not be {@literal null}.
	 * @param entityManager must not be {@literal null}.
	 */
	public CustomJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
	}

	public CustomJpaRepository(Class<T> domainClass, EntityManager em) {
		super(domainClass, em);
	}
	
	@Override
	protected <S extends T> Page<S> readPage(TypedQuery<S> query, Class<S> domainClass, Pageable pageable,
                                             Specification<S> spec) {
		query.setFirstResult((int)pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		Long total = executeCountQuery(getCountQuery(spec, domainClass));
		if(total <= pageable.getOffset()) {
			int pageNumber = (int) (total / pageable.getPageSize());
			pageable = PageRequest.of(total % pageable.getPageSize() == 0 ? (pageNumber == 0 ? 1 : (pageNumber - 1))
					: pageNumber, pageable.getPageSize(), pageable.getSort());
			query.setFirstResult((int)pageable.getOffset());
		}
		List<S> content = total > pageable.getOffset() ? query.getResultList() : Collections.<S> emptyList();

		return new PageImpl<S>(content, pageable, total);
	}

	/**
	 * Executes a count query and transparently sums up all values returned.
	 * 
	 * @param query must not be {@literal null}.
	 * @return
	 */
	private static Long executeCountQuery(TypedQuery<Long> query) {

		Assert.notNull(query, "query must not be null");

		List<Long> totals = query.getResultList();
		Long total = 0L;

		for (Long element : totals) {
			total += element == null ? 0 : element;
		}

		return total;
	}
}
