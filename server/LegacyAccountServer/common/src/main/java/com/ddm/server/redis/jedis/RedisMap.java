package com.ddm.server.redis.jedis;

import com.ddm.server.common.utils.*;
import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.beans.BeanMap;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RedisMap implements Map<Object, Object> {

    private String sourceKey;

    RedisMap(String sourceKey) {
        this.sourceKey = sourceKey;
    }


    @Override
    public int size() {
        return RedisUtil.opsForHash().size(this.sourceKey).intValue();
    }

    @Override
    public boolean isEmpty() {
        return RedisUtil.opsForHash().size(this.sourceKey).intValue() <= 0;
    }

    public boolean isNotEmpty() {
        return !this.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return RedisUtil.opsForHash().hasKey(this.sourceKey,BeanUtils.getKeyStr(key) );
    }

    @Override
    public boolean containsValue(Object value) {
        return RedisUtil.opsForHash().values(this.sourceKey).contains(value);
    }

    @Override
    public Object get(Object key) {
        return RedisUtil.opsForHash().get(this.sourceKey,BeanUtils.getKeyStr(key) );
    }

    @Override
    public Object put(Object key, Object value) {
        RedisUtil.opsForHash().put(this.sourceKey, BeanUtils.getKeyStr(key),value );
        return value;
    }

    public boolean putIf(Object key, Object value) {
        try {
            RedisUtil.opsForHash().put(this.sourceKey, BeanUtils.getKeyStr(key),value );
        } catch (Exception e){
            CommLog.error("RedisMap putIf key:{},value:{},message:{},e", BeanUtils.getKeyStr(key),value,e.getMessage(),e);
            return false;
        }
        return true;
    }

    public boolean hincrByIf(Object key,long number) {
        Long o = RedisUtil.opsForHash().increment(this.sourceKey, BeanUtils.getKeyStr(key),number );
        return Objects.nonNull(o);
    }

    public Long hincrBy(Object key, long number) {
        return RedisUtil.opsForHash().increment(this.sourceKey, BeanUtils.getKeyStr(key),number );
    }

    public Double increment(Object key, double number) {
        return RedisUtil.opsForHash().increment(this.sourceKey, BeanUtils.getKeyStr(key),number);
    }


    @Override
    public Object remove(Object key) {
        return RedisUtil.opsForHash().delete(this.sourceKey,BeanUtils.getKeyStr(key));
    }

    @Override
    public void putAll(Map<?, ?> m) {
        if(MapUtils.isEmpty(m)) {
            return;
        }
        RedisUtil.opsForHash().putAll(this.sourceKey,m);
    }

    public boolean putAllIf(Map<?, ?> m) {
        if(MapUtils.isEmpty(m)) {
            return false;
        }
        try {
            RedisUtil.opsForHash().putAll(this.sourceKey,m);
        } catch (Exception e){
            CommLog.error("RedisMap putAllIf map:{},message:{},e", m.toString(),e.getMessage(),e);
            return false;
        }
        return true;
    }

    @Override
    public void clear() {
        RedisUtil.getRedisTemplate().delete(this.sourceKey);

    }

    @Override
    public Set<Object> keySet() {
        return RedisUtil.opsForHash().keys(this.sourceKey);
    }

    @Override
    public Collection<Object> values() {
        return RedisUtil.opsForHash().values(this.sourceKey);
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return RedisUtil.opsForHash().entries(this.sourceKey).entrySet();
    }

    /**
     * 获取string 的值
     * @param key
     * @return
     */
    public String getStr(Object key) {
        Object o  = this.get(key);
        return Objects.nonNull(o) ? o.toString():"";
    }

    /**
     * 获取int 的值
     * @param key
     * @return
     */
    public int getInt(Object key) {
        Object o  = this.get(key);
        return Objects.nonNull(o) && StringUtils.isNumeric(o.toString()) ? Integer.parseInt(o.toString()):0;
    }

    /**
     * 获取long 的值
     * @param key
     * @return
     */
    public long getLong(Object key) {
        Object o  = this.get(key);
        return Objects.nonNull(o) && StringUtils.isNumeric(o.toString()) ? Long.parseLong(o.toString()):0L;
    }

    /**
     * 获取double 的值
     * @param key
     * @return
     */
    public double getDouble(Object key) {
        Object o  = this.get(key);
        return Objects.nonNull(o) && StringUtil.isNumber(o.toString()) ? Double.parseDouble(o.toString()):0D;
    }

    /**
     * 获取一个实体
     * @param key
     * @param <T>
     * @return
     */
    public <T> T getBean(Object key, Class<T> classOfT) {
        Object o  = this.get(key);
        return Objects.nonNull(o) && StringUtils.isNotEmpty(o.toString()) ? GsonUtils.stringToBean(o.toString().trim(), classOfT):null;
    }

    /**
     * 获取一个实体
     * @param key
     * @param genericType 实体类型
     * @param <T>
     * @return
     */
    public <T> T getBean(Object key, Type genericType) {
        Object o  = this.get(key);
        return Objects.nonNull(o) && StringUtils.isNotEmpty(o.toString()) ? GsonUtils.stringToBean(o.toString().trim(), genericType):null;
    }

    /**
     * 获取一个实体
     * @param key
     * @param genericType 实体类型
     * @param <T>
     * @return
     */
    public <T> List<T>  getBeanList(Object key, TypeToken genericType) {
        Object o  = this.get(key);
        return Objects.nonNull(o) && StringUtils.isNotEmpty(o.toString()) ? GsonUtils.stringToBean(o.toString(),genericType.getType()):Collections.emptyList();
    }

    /**
     * 获取一个实体
     * @param key
     * @return
     */
    public Map<String,String>  getMap(Object key) {
        Object o  = this.get(key);
        return Objects.nonNull(o) && StringUtils.isNotEmpty(o.toString()) ? GsonUtils.stringToBean(o.toString(),Map.class):Collections.emptyMap();
    }

    public void expire(long timeout) {
        RedisUtil.getRedisTemplate().expire(this.sourceKey, timeout, TimeUnit.SECONDS);
    }

    public Map<String,String> toMap() {
        Map<String,String> map = Maps.newHashMap();
        for (Map.Entry<Object, Object> entry : this.entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return map;
    }

    /**
     * 将对象转换为map
     *
     * @param bean
     * @return
     */
    public <T> boolean beanToMap(T bean) {
        Map<String, Object> map = Maps.newHashMap();
        BeanMap beanMap = BeanMap.create(bean);
        for (Object key : beanMap.keySet()) {
            Object o  = BeanUtils.getObjDefault(beanMap.get(key));
            if (Objects.nonNull(o)) {
                map.put(key.toString(), o);
            }
        }
        if (MapUtils.isNotEmpty(map)) {
            return this.putAllIf(map);
        }
        return false;
    }



    /**
     * 将Map对象通过反射机制转换成Bean对象
     *
     * @param clazz 待转换的class
     * @return 转换后的Bean对象
     * @throws Exception 异常
     *                   异常暂时保留
     */
    public <T> T mapToBean(Class<T> clazz) {
        try {
            if (this.size() > 0) {
                T obj = clazz.getDeclaredConstructor().newInstance();
                for (Map.Entry<Object, Object> entry : this.entrySet()) {
                    //属性名
                    String propertyName = entry.getKey().toString();
                    Object value = entry.getValue();
                    String setMethodName = "set"
                            + propertyName.substring(0, 1).toUpperCase()
                            + propertyName.substring(1);
                    Field field = BeanUtils.getClassField(clazz, propertyName);
                    if (field == null) {
                        continue;
                    }
                    Class<?> fieldTypeClass = field.getType();
                    value = BeanUtils.convertValType(value, fieldTypeClass,field.getGenericType());
                    clazz.getMethod(setMethodName, field.getType()).invoke(obj, value);
                }
                return obj;
            }
        } catch (Exception e) {
            CommLog.error("mapToBean error:{}",e.getMessage(),e );
        }
        return null;
    }
}
