/*******************************************************************************
 * Copyright (c) 2005, 2014 zzy.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *******************************************************************************/
package server.aoo.dao.mapper.game;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import server.aoo.dao.config.util.CustomerRepository;

/**
 * dao 基础接口
 * @author lll 2015年1月1日
 */
@NoRepositoryBean
public abstract interface BaseDao<T> extends PagingAndSortingRepository<T, Long>, JpaSpecificationExecutor<T>, CustomerRepository<T>  {

}
