package server.aoo.dao.service.mongo;


import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.aoo.dao.entity.mongo.DbTagAccountInfo;
import server.aoo.dao.entity.mongo.DbTagAccountType;

import java.util.List;

@Service
@Data
public class TagAccountInfoService {
    /**
     * MongoDB 操作工具类
     */
    @Autowired
    private MongoDBHelper mongoDBHelper;

    @Autowired
    private TagAccountTypeService tagAccountTypeService;

    /**
     * 创建账号和类型信息
     */
    public void insertAccountInfoAndType(DbTagAccountInfo tagAccountInfo,DbTagAccountType dbTagAccountType){
        // 创建公共的账号信息
        this.getMongoDBHelper().insert(tagAccountInfo, DbTagAccountInfo.class.getSimpleName());
        // 创建指定账号类型信息
        this.getTagAccountTypeService().insert(dbTagAccountType);
    }


    /**
     * 获取账号信息列表
     * @return
     */
    public List<DbTagAccountInfo>  getTagAccountInfoList() {
        return this.getMongoDBHelper().selectList(DbTagAccountInfo.class.getSimpleName(), DbTagAccountInfo.class);
    }

    /**
     * 获取账号信息
     * @param accountId 账号id
     * @return
     */
    public DbTagAccountInfo getDbTagAccountInfo(long accountId) {
        return (DbTagAccountInfo) this.getMongoDBHelper().selectById(accountId, DbTagAccountInfo.class, DbTagAccountInfo.class.getSimpleName());
    }

}
