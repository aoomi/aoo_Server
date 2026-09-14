package com.ddm.server.redis.jedis;

import com.ddm.server.common.utils.BeanUtils;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.*;

public class RedisSetTuple implements Set<ZSetOperations.TypedTuple<Object>> {

	private String sourceKey;

	RedisSetTuple(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	@Override
	public int size() {
		return RedisUtil.opsForZSet().size(this.sourceKey).intValue();
	}

	@Override
	public boolean isEmpty() {
		return this.size() <= 0;
	}

	@Override
	public Iterator<ZSetOperations.TypedTuple<Object>> iterator() {
		return RedisUtil.opsForZSet().reverseRangeWithScores(this.sourceKey,0 ,-1 ).iterator();
	}

	public Set<ZSetOperations.TypedTuple<Object>> zrevrangeWithScores(int start ,int end) {
		return RedisUtil.opsForZSet().reverseRangeWithScores(this.sourceKey,start ,end );
	}

	public Set<ZSetOperations.TypedTuple<Object>> rangeWithScores(int start ,int end) {
		return RedisUtil.opsForZSet().rangeWithScores(this.sourceKey,start ,end );
	}

	public Set<Object> rangeByScore(double start ,double end, long offset, long count) {
		return RedisUtil.opsForZSet().rangeByScore(this.sourceKey,start ,end,offset,count);
	}


	public Set<Object> rangeByScore(double start ,double end) {
		return RedisUtil.opsForZSet().rangeByScore(this.sourceKey,start ,end);
	}


	public long getCurZrevrank(Object key) {
		  Long o= RedisUtil.opsForZSet().reverseRank(this.sourceKey, BeanUtils.getKeyStr(key));
		  return Objects.nonNull(o) ? o.longValue():-1;
	}

	public double getCurScore(Object key) {
		Double o= RedisUtil.opsForZSet().score(this.sourceKey, BeanUtils.getKeyStr(key));
		return Objects.nonNull(o) ? o.doubleValue():0D;
	}

	@Override
	public boolean add(ZSetOperations.TypedTuple<Object> stringTypedTuple) {
		 return RedisUtil.opsForZSet().incrementScore(this.sourceKey,BeanUtils.getKeyStr(stringTypedTuple.getValue()) ,stringTypedTuple.getScore()).doubleValue() > 0D;
	}

	public boolean zadd(ZSetOperations.TypedTuple<Object> stringTypedTuple) {
		return RedisUtil.opsForZSet().add(this.sourceKey,BeanUtils.getKeyStr(stringTypedTuple.getValue()) ,stringTypedTuple.getScore()).booleanValue();
	}



	public Long add(Set<ZSetOperations.TypedTuple<Object>> typedTupleSet) {
		return RedisUtil.opsForZSet().add(this.sourceKey,typedTupleSet);
	}

	@Override
	public boolean remove(Object o) {
		return RedisUtil.opsForZSet().remove(this.sourceKey, BeanUtils.getKeyStr(o)).intValue() > 0L;
	}


	@Override
	public boolean addAll(Collection<? extends ZSetOperations.TypedTuple<Object>> values) {
;		Long o = RedisUtil.opsForZSet().add(this.sourceKey, new HashSet<>(values));
		return Objects.nonNull(o);
	}

	@Override
	public void clear() {
		RedisUtil.getRedisTemplate().delete(this.sourceKey);
	}


	@Override
	public Object[] toArray() {
		return RedisUtil.opsForZSet().reverseRangeWithScores(this.sourceKey,0 ,-1 ).toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return RedisUtil.opsForZSet().reverseRangeWithScores(this.sourceKey,0 ,-1 ).toArray(a);
	}

	@Override
	public boolean contains(Object key) {
		Long o =  RedisUtil.opsForZSet().rank(this.sourceKey,BeanUtils.getKeyStr(key) );
		return Objects.nonNull(o) ;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return RedisUtil.opsForZSet().reverseRangeWithScores(this.sourceKey,0 ,-1 ).containsAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		String[] strs1=c.toArray(new String[c.size()]);
		int count = RedisUtil.opsForZSet().remove(this.sourceKey, (Object[]) strs1).intValue();
		return  count == c.size();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return false;
	}



}
