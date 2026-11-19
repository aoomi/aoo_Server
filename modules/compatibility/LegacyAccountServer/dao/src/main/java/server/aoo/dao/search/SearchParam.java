/*******************************************************************************
 * Copyright (c) 2005, 2014 zzy.cn
 *
 * 
 *******************************************************************************/
package server.aoo.dao.search;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询参数列表
 * 
 * @author Administrator
 */
public class SearchParam {

	private List<SearchFilter> filters = new ArrayList<SearchFilter>();

	public SearchParam add(String fieldName, Object value) {
		filters.add(new SearchFilter(fieldName, SearchFilter.Operator.EQ, value));
		return this;
	}

	public SearchParam add(String fieldName, Object value, SearchFilter.Operator operator) {
		filters.add(new SearchFilter(fieldName, operator, value));
		return this;
	}

	public List<SearchFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<SearchFilter> filters) {
		this.filters = filters;
	}
}
