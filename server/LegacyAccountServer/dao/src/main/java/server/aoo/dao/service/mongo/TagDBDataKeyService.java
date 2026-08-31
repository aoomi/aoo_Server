package server.aoo.dao.service.mongo;

import cenum.DefaultEnum;
import cenum.redis.RedisBydrKeyEnum;
import com.ddm.server.redis.jedis.RedisSource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import server.aoo.dao.entity.mongo.DbTagDBDataKey;

@Service
@Data
public class TagDBDataKeyService {
    @Value("${account.persistence.legacy-database:Account01_aoo}")
    private String legacyDatabase;
    /**
     * MongoDB 操作工具类
     */
    @Autowired
    private MongoDBHelper mongoDBHelper;


    /**
     * 修改字段值
     */
    public long addDataFieldByKeyID(){
        // 自增id
        long accountId = RedisSource.increment(RedisBydrKeyEnum.ACCOUNT_ID.getKey());
        this.getMongoDBHelper().save(DbTagDBDataKey.builder().id(DefaultEnum.DATA_KEY_ID.value()).AccountID(accountId).build(),DbTagDBDataKey.class.getSimpleName());
        return accountId;
    }

    /**
     * 修改字段值
     * @param accountId 账号Id
     */
    public void setDBDataKeyByProperty(long accountId){
        RedisSource.put(RedisBydrKeyEnum.ACCOUNT_ID.getKey(),String.valueOf(accountId));
        this.getMongoDBHelper().save(DbTagDBDataKey.builder().id(DefaultEnum.DATA_KEY_ID.value()).AccountID(accountId).build(),DbTagDBDataKey.class.getSimpleName());
    }

    /**
     * Ensure the distributed account sequence never falls behind either the
     * current model or the legacy production sequence. Incrementing the delta
     * is atomic; concurrent registrations may create harmless gaps but can
     * never move the sequence backwards or reuse an account id.
     */
    public long initializeAccountIdFloor() {
        String redisValue = RedisSource.get(RedisBydrKeyEnum.ACCOUNT_ID.getKey());
        long redisId = 0L;
        if (redisValue != null && !redisValue.isBlank()) {
            redisId = Long.parseLong(redisValue);
        }
        DbTagDBDataKey current = (DbTagDBDataKey) this.getMongoDBHelper().selectById(
                DefaultEnum.DATA_KEY_ID.value(), DbTagDBDataKey.class,
                DbTagDBDataKey.class.getSimpleName());
        long currentMongoId = current == null || current.getAccountID() == null
                ? 0L : current.getAccountID();
        long legacyMongoId = this.getMongoDBHelper().readLongField(
                legacyDatabase, "tagdbdatakeys", DefaultEnum.DATA_KEY_ID.value(), "AccountID");
        long floor = Math.max(redisId, Math.max(currentMongoId, legacyMongoId));
        if (redisId < floor) {
            Long synchronizedId = RedisSource.increment(
                    RedisBydrKeyEnum.ACCOUNT_ID.getKey(), floor - redisId);
            floor = synchronizedId == null ? 0L : synchronizedId;
        }
        if (floor <= 0L) {
            throw new IllegalStateException("account id sequence initialization failed");
        }
        this.getMongoDBHelper().save(
                DbTagDBDataKey.builder().id(DefaultEnum.DATA_KEY_ID.value()).AccountID(floor).build(),
                DbTagDBDataKey.class.getSimpleName());
        return floor;
    }

    public String databaseName() {
        return this.getMongoDBHelper().databaseName();
    }

    /**
     * 检查keyId是否存在
     * @param accountId 账号Id
     * @return
     */
    public boolean haveDBDataKey(long accountId) {
        return this.getMongoDBHelper().exists(Query.query(Criteria.where("AccountID").is(accountId)), DbTagDBDataKey.class, DbTagDBDataKey.class.getSimpleName());
    }

}
