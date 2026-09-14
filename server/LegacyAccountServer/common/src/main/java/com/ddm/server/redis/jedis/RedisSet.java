package com.ddm.server.redis.jedis;

import com.ddm.server.common.utils.BeanUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RedisSet implements Set<Object> {

	private String sourceKey;

	RedisSet(String sourceKey) {
		this.sourceKey = sourceKey;
	}


	@Override
	public int size() {
		return RedisUtil.opsForSet().size(this.sourceKey).intValue();
	}

	@Override
	public boolean isEmpty() {
		return this.size() <=0;
	}

	@Override
	public boolean contains(Object o) {
		return RedisUtil.opsForSet().isMember(this.sourceKey, BeanUtils.getKeyStr(o));
	}


	@Override
	public boolean add(Object s) {
		RedisUtil.opsForSet().add(this.sourceKey,BeanUtils.getKeyStr(s));
		return true;
	}

	@Override
	public boolean remove(Object o) {
		RedisUtil.opsForSet().remove(this.sourceKey,BeanUtils.getKeyStr(o));
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends Object> values) {
		if (values == null) {
			throw new NullPointerException();
		}
		if (values.size() == 0) {
			return false;
		}
		String[] strs = new String[values.size()];
		values.toArray(strs);
		int count = RedisUtil.opsForSet().add(this.sourceKey, (Object[]) strs).intValue();
		return count == values.size();
	}

	@Override
	public void clear() {
		RedisUtil.getRedisTemplate().delete(this.sourceKey);
	}

	public String getRandom() {
		Object o = RedisUtil.opsForSet().pop(this.sourceKey);
		return Objects.isNull(o) ? "":o.toString();
	}

	@Override
	public Iterator<Object> iterator() {
		return RedisUtil.opsForSet().members(this.sourceKey).iterator();
	}

	@Override
	public Object[] toArray() {
		return RedisUtil.opsForSet().members(this.sourceKey).toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return RedisUtil.opsForSet().members(this.sourceKey).toArray(a);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return RedisUtil.opsForSet().members(this.sourceKey).containsAll(c);
	}


	@Override
	public boolean removeAll(Collection<?> values) {
		if (values == null) {
			throw new NullPointerException();
		}
		if (values.size() == 0) {
			return false;
		}
		String[] strs = new String[values.size()];
		values.toArray(strs);
		int count = RedisUtil.opsForSet().remove(this.sourceKey,values).intValue();
		return count > 0;
	}

	@Override
	public boolean retainAll(Collection<?> values) {
		Set<Object> set = RedisUtil.opsForSet().members(this.sourceKey);
		set.retainAll(values);
		String[] strs = new String[set.size()];
		set.toArray(strs);
		int count = RedisUtil.opsForSet().remove(this.sourceKey,values).intValue();
		return count > 0;
	}







}
