/*******************************************************************************
 * Copyright (c) 2005, 2014 zzy.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *******************************************************************************/
package server.aoo.dao.search;

/**
 * 范围
 * 
 * @author lll 2015年11月19日
 */
public class Range {
	// 起始位
	private long start;
	// 结束位
	private long end;

	/**
	 * @param start
	 * @param end
	 */
	public Range(long start, long end) {
		super();
		this.start = start;
		this.end = end;
	}

	/**
	 * @return the start
	 */
	public long getStart() {
		return start;
	}

	/**
	 * @param start
	 *            the start to set
	 */
	public void setStart(long start) {
		this.start = start;
	}

	/**
	 * @return the end
	 */
	public long getEnd() {
		return end;
	}

	/**
	 * @param end
	 *            the end to set
	 */
	public void setEnd(long end) {
		this.end = end;
	}

}
