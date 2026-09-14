/*******************************************************************************
 * Copyright (c) 2005, 2014 zzy.cn
 *
 * 
 *******************************************************************************/
package server.aoo.dao.search;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.data.jpa.domain.Specification;
import com.google.common.collect.Lists;

/**
 * 
 * @author zyh 2015年1月15日
 */
public class DynamicSpecifications {
	public static <T> Specification<T> bySearchFilter(final Collection<SearchFilter> filters, final Class<T> entityClazz) {
		return new Specification<T>() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
				if (CollectionUtils.isNotEmpty(filters)) {

					List<Predicate> predicates = Lists.newArrayList();
					for (SearchFilter filter : filters) {
						String[] names = StringUtils.split(filter.fieldName, ".");
						Path expression = root.get(names[0]);
						for (int i = 1; i < names.length; i++) {
							expression = expression.get(names[i]);
						}

						// logic operator
						switch (filter.operator) {
							case EQ:
								predicates.add(builder.equal(expression, filter.value));
								break;
							case NEQ:
								predicates.add(builder.notEqual(expression, filter.value));
								break;
							case LIKE:
								predicates.add(builder.like(expression, "%" + ((String) filter.value).replaceAll("\\\\", "\\\\\\\\")
										.replaceAll("%", "\\\\%").replaceAll("_", "\\\\_") + "%"));
								break;
							case GT:
								predicates.add(builder.greaterThan(expression, (Comparable) filter.value));
								break;
							case LT:
								predicates.add(builder.lessThan(expression, (Comparable) filter.value));
								break;
							case GTE:
								predicates.add(builder.greaterThanOrEqualTo(expression, (Comparable) filter.value));
								break;
							case LTE:
								predicates.add(builder.lessThanOrEqualTo(expression, (Comparable) filter.value));
								break;
							case NE:
								predicates.add(builder.notEqual(expression, filter.value));
								break;
							case IN:
							{
								predicates.add(expression.in((Collection<Long>)filter.value));
								break;
							}
							case INSTR:
							{
								predicates.add(expression.in((Collection<String>)filter.value));
								break;
							}
							case NOTIN:
							{
								predicates.add(builder.not(expression.in((Collection<Long>)filter.value)));
								break;
							}
							case ISNULL:
							{
								if ((Boolean)filter.value) {
									predicates.add(0,expression.isNull());
								} else {
									predicates.add(0,expression.isNotNull());
								}
								break;
							}
							case CONTAINS:
							{
								predicates.add(builder.isMember(filter.value, expression));
								break;
							}
							case SIZE:
							{
								Validate.isTrue(filter.value instanceof Range, "错误类型");
								Range range = (Range)filter.value;
								if (range.getStart() == 0) {
									predicates.add(builder.le(builder.size(expression), range.getEnd()));
								} else if (range.getEnd() == 0) {
									predicates.add(builder.ge(builder.size(expression), range.getStart()));
								} else {								
									predicates.add(builder.between(builder.size(expression), (int)range.getStart(), (int)range.getEnd()));
								}
								break;
							}							
						}
					}

					if (!predicates.isEmpty()) {
						return builder.and(predicates.toArray(new Predicate[predicates.size()]));
					}
				}

				return builder.conjunction();
			}
		};
	}
	
}
