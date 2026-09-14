package business.account;

import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Maps;
import jsproto.c2s.cclass.token.AccountTokenInfo;
import lombok.Data;
import server.aoo.dao.entity.mongo.DbTagAccountInfo;
import server.aoo.dao.entity.mongo.DbTagAccountType;
import server.aoo.dao.service.mongo.TagAccountInfoService;
import server.aoo.dao.service.mongo.TagAccountTypeService;

import java.util.Map;

/**
 * 账号信息
 */
@Data
public class Account {
    /**
     * 账号id
     */
    private long accountId;
    /**
     * 账号管理
     */
    private AccountManager accountManager;

    /**
     * 账号存储信息
     */
    private DbTagAccountInfo dbTagAccountInfo;

    /**
     * 账号Token信息
     */
    private AccountTokenInfo accountTokenInfo;

    /**
     * 账号类型管理
     */
    private final Map<String,DbTagAccountType> tagAccountTypeMap = Maps.newConcurrentMap();

    /**
     * 刷新时间（单位：s）
     * 用来判断用户是否存在过期
     */
    private int refreshTime;

    public Account(long accountId, AccountManager accountManager, DbTagAccountInfo dbTagAccountInfo) {
        this.accountId = accountId;
        this.accountManager = accountManager;
        this.dbTagAccountInfo = dbTagAccountInfo;
    }

    /**
     * 账号信息数据操作
     * @return
     */
    public TagAccountInfoService getTagAccountInfoService() {
        return this.accountManager.getTagAccountInfoService();
    }

    /**
     * 账号类型信息数据操作
     * @return
     */
    public TagAccountTypeService getTagAccountTypeService() {
        return this.accountManager.getTagAccountTypeService();
    }

    /**
     * 刷新存在时间
     */
    public void refreshTime() {
         this.setRefreshTime(CommTime.nowSecond());
    }

    /**
     * 账号类型
     * @param accountTypeId 账号类型id
     * @param tagAccountType 账号类型信息
     */
    public void tagAccountTypePut(String accountTypeId,DbTagAccountType tagAccountType) {
        this.tagAccountTypeMap.put(accountTypeId,tagAccountType );
    }

    /**
     * 获取账号类型的账号
     * @param accountType 账号类型
     * @return
     */
    public String getAccountTypeToChatAccount(int accountType) {
        return this.tagAccountTypeMap.get(TagAccountTypeService.getId(accountType, this.accountId)).getCharAccount();
    }

    /**
     * 获取账号类型的账号密码
     * @param accountType 账号类型
     * @return
     */
    public String getAccountTypeToCharAccountPsw(int accountType) {
        return this.tagAccountTypeMap.get(TagAccountTypeService.getId(accountType, this.accountId)).getCharAccountPsw();
    }

    public void saveAccountPasswordHash(int accountType, String passwordHash) {
        DbTagAccountType accountTypeInfo = this.tagAccountTypeMap.get(TagAccountTypeService.getId(accountType, this.accountId));
        accountTypeInfo.setCharAccountPsw(passwordHash);
        this.getTagAccountTypeService().save(accountTypeInfo);
    }

    /**
     * 增加账号类型
     * @param accountType  账号类型
     * @param uid uid
     * @param charAccountPsw 账号
     */
    public void newAccountTypePut(int accountType,String uid,String charAccountPsw) {
        if (accountType == cenum.sdk.SDKTypeEnum.SDKType_Company.getValue()
                && !business.security.PasswordHasher.isEncoded(charAccountPsw)) {
            charAccountPsw = business.security.PasswordHasher.hash(charAccountPsw);
        }
        DbTagAccountType dbTagAccountType = DbTagAccountType.builder().id(TagAccountTypeService.getId(accountType,this.accountId )).accountId(this.accountId).accountType(accountType).charAccount(uid).charAccountPsw(charAccountPsw).build();
        this.getTagAccountTypeService().insert(dbTagAccountType);
        this.tagAccountTypePut(dbTagAccountType.getId(),dbTagAccountType);
    }

}
