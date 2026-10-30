package com.ddm.server.common.redis;

import redis.clients.jedis.resps.Tuple;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class RedisSetTuple implements Set<Tuple> {

	private String sourceKey;

	RedisSetTuple(String sourceKey) {
		this.sourceKey = sourceKey;
	}


	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean contains(Object o) {
		return false;
	}

	@Override
	public Iterator<Tuple> iterator() {
		return null;
	}

	@Override
	public Object[] toArray() {
		return new Object[0];
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return null;
	}

	@Override
	public boolean add(Tuple tuple) {
		RedisUtil.zadd(this.sourceKey,tuple.getScore() ,tuple.getElement() );
		return false;
	}

	@Override
	public boolean remove(Object o) {
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends Tuple> c) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return false;
	}

	@Override
	public void clear() {

	}

	public Set<Tuple> rangeWithScoresAll() {
		return RedisUtil.zrangeWithScores(this.sourceKey,0,-1);
	}

	public double zscore(String member) {
		return RedisUtil.zscore(this.sourceKey,member);
	}
}
