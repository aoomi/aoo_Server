package server.aoo.dao.service.mongo;


import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.aoo.dao.entity.mongo.DbTagAccountType;

import java.util.List;

@Service
@Data
public class TagAccountTypeService {
    /**
     * MongoDB 操作工具类
     */
    @Autowired
    private MongoDBHelper mongoDBHelper;



    /**
     * 获取Id
     * @param accountId 玩家Pid
     * @return
     */
    public final static String getId(int accountType,long accountId) {
        return String.format("1%02d%012d",accountType,accountId);
    }

    /**
     * 创建账号类型信息
     */
    public String insert(DbTagAccountType tagAccountType){
        this.getMongoDBHelper().insert(tagAccountType, DbTagAccountType.class.getSimpleName());
        return tagAccountType.getId();
    }

    public void save(DbTagAccountType tagAccountType) {
        this.getMongoDBHelper().save(tagAccountType, DbTagAccountType.class.getSimpleName());
    }


    /**
     * 获取账号信息列表
     * @return
     */
    public List<DbTagAccountType>  getTagAccountTypeList() {
        return this.getMongoDBHelper().selectList(DbTagAccountType.class.getSimpleName(), DbTagAccountType.class);
    }

    /**
     * 获取账号信息
     * @param id 账号类型Id
     * @return
     */
    public DbTagAccountType getDbTagAccountType(String id) {
        return (DbTagAccountType) this.getMongoDBHelper().selectById(id, DbTagAccountType.class, DbTagAccountType.class.getSimpleName());
    }

}
