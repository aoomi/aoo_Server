/*******************************************************************************
 * Copyright (c) 2005, 2014 springside.github.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *******************************************************************************/
package server.aoo.dao.entity;

import java.io.Serializable;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import lombok.Builder;
import lombok.Data;

import org.apache.commons.lang3.builder.ToStringBuilder;
/**
 * 统一定义id的entity基类.
 * 
 * 基类统一定义id的属性名称、数据类型、列名映射及生成策略.
 * Oracle需要每个Entity独立定义id的SEQUCENCE时，不继承于本类而改为实现一个Idable的接口。
 * 
 * @author calvin
 */
// JPA 基类的标识
@Data
@MappedSuperclass
public abstract class IdEntity implements Serializable{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long id;

	public IdEntity() {
	}

	public IdEntity(long id) {
		super();
		this.id = id;
	}

	public String getTableName() {
		return this.getClass().getSimpleName();
	}

	public int threadId() {
		return 0;
	}

	/**
	 * 插入前数据初始化 (主要用于设置默认值，如日期等对象的初始化)
	 */
	@PrePersist
	public abstract void prepareForInsert();

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("id",id).toString();
	}

}
