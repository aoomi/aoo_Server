package com.ddm.server.common.mongodb;

import com.ddm.server.common.CommLogD;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MongoDB管理类
 */
public class MongoDBHelper<T> {
    //大于这个时间毫秒
    private static final int LONG_TIME = 50;

    /**
     * 显示创建集合
     */
    public void createCollection(String collectionName) {
        MongoDbMgr.get().getDatabase().createCollection(collectionName,
                new CreateCollectionOptions().capped(false));
    }

    // 删除集合
    public void deleteCollection(final String collectionName) {
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        collection.drop();
    }

    /**
     * 功能描述: 往对应的集合中插入一条数据
     *
     * @param info           存储对象
     * @param collectionName 集合名称
     * @return:void
     */
    public void insert(T info, String collectionName) {
        long startTime = System.currentTimeMillis();
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        collection.insertOne(BsonUtil.toDocument(info));
        if (System.currentTimeMillis() - startTime > LONG_TIME) {
            CommLogD.error("mongodb time {} ms, key={} method=insert", System.currentTimeMillis() - startTime, collectionName);
        }
    }


    /**
     * 功能描述: 往对应的集合中插入一条数据
     *
     * @param info 存储对象
     * @return:void
     */
    public void insert(T info) {
        insert(info, info.getClass().getSimpleName());
    }

    /**
     * 功能描述: 往对应的集合中批量插入数据，注意批量的数据中不要包含重复的id
     *
     * @param infos 对象列表
     * @return:void
     */

    public void insertMulti(List<T> infos, String collectionName) {
        long startTime = System.currentTimeMillis();
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        collection.insertMany(BsonUtil.toListDocument(infos));
        if (System.currentTimeMillis() - startTime > LONG_TIME) {
            CommLogD.error("mongodb time {} ms, key={} method=insertMulti", System.currentTimeMillis() - startTime, collectionName);
        }
    }

    /**
     * 功能描述: 使用索引信息精确更改某条数据
     *
     * @param id             唯一键
     * @param collectionName 集合名称
     * @param info           待更新的内容
     * @return:void
     */
    public void updateById(Object id, String collectionName, T info) {
        long startTime = System.currentTimeMillis();
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        //修改过滤器
        Bson filter = Filters.eq("_id", id);
        //修改多个文档
        collection.updateMany(filter, BsonUtil.toDocumentForUpdate(info));
        if (System.currentTimeMillis() - startTime > LONG_TIME) {
            CommLogD.error("mongodb time {} ms, key={} method=updateById", System.currentTimeMillis() - startTime, collectionName);
        }
    }

    /**
     * 功能描述: 根据id删除集合中的内容
     *
     * @param id             序列id
     * @param collectionName 集合名称
     * @return:void
     */

    public long deleteById(Object id, String collectionName) {
        long startTime = System.currentTimeMillis();
        // mongodb在删除对象的时候会判断对象类型，如果你不传入对象类型，只传入了集合名称，它是找不到的
        // 上面我们为了方便管理和提升后续处理的性能，将一个集合限制了一个对象类型，所以需要自行管理一下对象类型
        // 在接口传入时需要同时传入对象类型
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        //申明删除条件
        Bson filter = Filters.eq("_id", id);
        //删除与筛选器匹配的所有文档
        DeleteResult deleteResult = collection.deleteMany(filter);
        long count = deleteResult.getDeletedCount();
        if (System.currentTimeMillis() - startTime > LONG_TIME) {
            CommLogD.error("mongodb time {} ms, key={} method=deleteById", System.currentTimeMillis() - startTime, collectionName);
        }
        return count;
    }

    /**
     * 功能描述: 删除集合中所有的内容
     *
     * @param collectionName 集合名称
     * @return:void
     */

    public long deleteAll(String collectionName) {
        long startTime = System.currentTimeMillis();
        // mongodb在删除对象的时候会判断对象类型，如果你不传入对象类型，只传入了集合名称，它是找不到的
        // 上面我们为了方便管理和提升后续处理的性能，将一个集合限制了一个对象类型，所以需要自行管理一下对象类型
        // 在接口传入时需要同时传入对象类型
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        //申明删除条件
        Bson filter = new Document();
        //删除与筛选器匹配的所有文档
        DeleteResult deleteResult = collection.deleteMany(filter);
        long count = deleteResult.getDeletedCount();
        if (System.currentTimeMillis() - startTime > LONG_TIME) {
            CommLogD.error("mongodb time {} ms, key={} method=deleteAll", System.currentTimeMillis() - startTime, collectionName);
        }
        return count;
    }

    /**
     * 功能描述: 根据id查询信息
     *
     * @param id             注解
     * @param clazz          类型
     * @param collectionName 集合名称
     * @return:<T>
     */

    public T selectById(Object id, Class<T> clazz, String collectionName) {
        // 查询对象的时候，不仅需要传入id这个唯一键，还需要传入对象的类型，以及集合的名称
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        //指定查询过滤器
        Bson filter = Filters.eq("_id", id);
        Document document = collection.find(filter).first();
        if (document != null) {
            return BsonUtil.toBean(document, clazz);
        } else {
            return null;
        }
    }

    /**
     * 功能描述: 根据id查询信息
     *
     * @param id             注解
     * @param collectionName 集合名称
     * @return:<T>
     */

    public boolean existById(Object id, String collectionName) {
        long startTime = System.currentTimeMillis();
        // 查询对象的时候，不仅需要传入id这个唯一键，还需要传入对象的类型，以及集合的名称
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        //指定查询过滤器
        Bson filter = Filters.eq("_id", id);
        long count = collection.countDocuments(filter);
        if (System.currentTimeMillis() - startTime > LONG_TIME) {
            CommLogD.error("mongodb time {} ms, key={} method=existById", System.currentTimeMillis() - startTime, collectionName);
        }
        return count > 0;
    }

    /**
     * 功能描述: 分页查询列表信息
     *
     * @param collectionName 集合名称
     * @param clazz          对象类型
     * @param currentPage    当前页码
     * @param pageSize       分页大小
     * @return:java.util.List<T>
     */
    public List<T> selectList(String collectionName, Class<T> clazz, Integer currentPage, Integer pageSize) {
        long startTime = System.currentTimeMillis();
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        List<Document> results = new ArrayList<>();
        FindIterable<Document> iterables = collection.find();
        if (currentPage != null && pageSize != null) {
            iterables.skip((currentPage - 1) * pageSize).limit(pageSize);
        }
        MongoCursor<Document> cursor = iterables.iterator();
        while (cursor.hasNext()) {
            results.add(cursor.next());
        }
        if (System.currentTimeMillis() - startTime > LONG_TIME) {
            CommLogD.error("mongodb time {} ms, key={} method=selectList", System.currentTimeMillis() - startTime, collectionName);
        }
        return BsonUtil.toBeans(results, clazz);
    }

    /**
     * 功能描述: 根据条件查询集合
     *
     * @param collectionName 集合名称
     * @param filters        查询条件，目前查询条件处理的比较简单，仅仅做了相等匹配，没有做模糊查询等复杂匹配
     * @param clazz          对象类型
     * @param currentPage    当前页码
     * @param pageSize       分页大小
     * @return:java.util.List<T>
     */

    public List<T> selectByQuery(String collectionName, Bson filters, Bson sorts, Class<T> clazz, Integer currentPage, Integer pageSize) {
        Long startTime = System.currentTimeMillis();
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        List<Document> results = new ArrayList<>();
        FindIterable<Document> iterables;
        if (filters != null) {
            iterables = collection.find(filters);
        } else {
            iterables = collection.find();
        }
        if (currentPage != null && pageSize != null) {
            iterables.skip((currentPage - 1) * pageSize).limit(pageSize);
        }
        if (sorts != null) {
            iterables.sort(sorts);
        }
        MongoCursor<Document> cursor = iterables.iterator();
        while (cursor.hasNext()) {
            results.add(cursor.next());
        }
        if (System.currentTimeMillis() - startTime > LONG_TIME) {
            CommLogD.error("mongodb time {} ms, key={} method=selectByQuery", System.currentTimeMillis() - startTime, collectionName);
        }
        return BsonUtil.toBeans(results, clazz);
    }


    /**
     * 功能描述: 根据条件查询数量
     *
     * @param collectionName
     * @param filters
     * @return
     */
    public long count(String collectionName, Bson filters) {
        Long startTime = System.currentTimeMillis();
        MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
        long count = collection.countDocuments(filters);
        if (System.currentTimeMillis() - startTime > LONG_TIME) {
            CommLogD.error("mongodb time {} ms, key={} method=count", System.currentTimeMillis() - startTime, collectionName);
        }
        return count;
    }

    /**
     * 功能描述: 创建索引
     *
     * @param collectionName 集合名称
     * @return:void
     */
    public synchronized void createIndexes(String collectionName, Map<String, Map<String, Integer>> indexMap) {
        if (!indexMap.isEmpty()) {
            MongoCollection<Document> collection = MongoDbMgr.get().getCollection(collectionName);
            for (Map.Entry<String, Map<String, Integer>> indexes : indexMap.entrySet()) {
                IndexOptions indexOptions = new IndexOptions().name(indexes.getKey());
                BasicDBObject basicDBObject = new BasicDBObject();
                for (Map.Entry<String, Integer> index : indexes.getValue().entrySet()) {
                    basicDBObject.append(index.getKey(), index.getValue());
                }
                collection.createIndex(basicDBObject, indexOptions);
            }
        }
    }

}
