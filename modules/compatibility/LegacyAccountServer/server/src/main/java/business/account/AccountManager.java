package business.account;

import business.secret.SecretManager;
import cenum.sdk.SDKTypeEnum;
import com.ddm.server.enums.CommonEnum;
import com.ddm.server.exception.BizException;
import jsproto.c2s.cclass.token.AccountTokenInfo;
import jsproto.c2s.cclass.token.CreateAccountTokenInfo;
import jsproto.c2s.iclass.client.S0000_AccountLogin;
import org.apache.commons.lang3.StringUtils;
import server.aoo.dao.entity.mongo.DbTagAccountInfo;
import com.ddm.server.common.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.Data;
import org.springframework.stereotype.Component;
import server.aoo.dao.entity.mongo.DbTagAccountType;
import server.aoo.dao.service.mongo.TagAccountInfoService;
import server.aoo.dao.service.mongo.TagAccountTypeService;
import server.aoo.dao.service.mongo.TagDBDataKeyService;
import server.aoo.dao.utils.ConfigUtils;

import java.util.*;
/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 * 玩家管理
 *
 * @author Hxing
 */
@Data
@Component
public class AccountManager {

    /**
     * 秘钥管理
     */
    @Autowired
    private SecretManager secretManager;

    /**
     * 账号id生成
     */
    @Autowired
    private TagDBDataKeyService tagDBDataKeyService;

    /**
     * 保存账号数据
     */
    @Autowired
    private TagAccountInfoService tagAccountInfoService;

    /**
     * 保存账号类型数据
     */
    @Autowired
    private TagAccountTypeService tagAccountTypeService;

    /**
     * 账号管理
     */
    private final Map<Long,Account> accountMap = Maps.newConcurrentMap();

    /**
     * 账号对应账号Id
     */
    private Map<String,Long> charAccountMap = Maps.newConcurrentMap();


    /**
     * 开服初始化玩家信息
     */
    public void init() {
        CommLogD.info("[AccountManager.init] load account begin...]");
        long accountIdFloor = this.tagDBDataKeyService.initializeAccountIdFloor();
        List<DbTagAccountType> tagAccountTypeList = this.tagAccountTypeService.getTagAccountTypeList();
        tagAccountTypeList.forEach(tagAccountType -> this.charAccountMap.put(AccountManager.getCharAccountKey(tagAccountType.getAccountType(),tagAccountType.getCharAccount() ),tagAccountType.getAccountId() ));
        CommLogD.info("[AccountManager.init] load account success, count: {}, accountIdFloor: {}, mongoDatabase: {}",this.charAccountMap.size(), accountIdFloor, this.tagDBDataKeyService.databaseName());
    }


    /**
     * 创建随机密码
     * @return
     */
    private String createRandPsw() {
        return "123456";
    }

    /**
     * 账号Id
     * @return
     */
    public long getAccountId() {
        // 获取账号id
        long accountId = this.tagDBDataKeyService.addDataFieldByKeyID();
        if (accountId <= 0L) {
            throw BizException.Of(CommonEnum.KICKOUT_CREATENEWHEROERROR.getResultCode(),CommonEnum.KICKOUT_CREATENEWHEROERROR.getResultMsg());
        }
        return accountId;
    }

    /**
     * 游客-创建账号
     * @param ip ip
     * @return
     */
    public S0000_AccountLogin createAccountBySeqKey(String ip) {
        // 获取账号id
        long accountId = this.getAccountId();
        // 创建账号
        return this.createAccountByCharAccount(accountId,String.valueOf(accountId),this.createRandPsw(), SDKTypeEnum.SDKType_Company.getValue(),ip);
    }


    /**
     * 创建账号
     * @param charAccount 账号
     * @param charAccountPsw 密码
     * @param accountType 账号类型
     * @param playerIP ip
     * @return
     */
    public S0000_AccountLogin createAccountByCharAccount(String charAccount, String charAccountPsw, int accountType, String playerIP) {
        // 创建账号
        return this.createAccountByCharAccount(this.getAccountId(),charAccount,charAccountPsw,accountType,playerIP);
    }

    /**
     * 创建账号
     * @param accountId 账号Id
     * @param charAccount 账号
     * @param charAccountPsw 密码
     * @param accountType 账号类型
     * @param playerIP ip
     * @return
     */
    public S0000_AccountLogin createAccountByCharAccount(long accountId, String charAccount, String charAccountPsw, int accountType, String playerIP) {
        if (accountType == SDKTypeEnum.SDKType_Company.getValue()
                && !business.security.PasswordHasher.isEncoded(charAccountPsw)) {
            charAccountPsw = business.security.PasswordHasher.hash(charAccountPsw);
        }
        // 账号信息
        DbTagAccountInfo tagAccountInfo = DbTagAccountInfo.builder()
                .id(accountId)
                .loginTime(CommTime.nowMS())
                .accountState(0)
                .registerTime(CommTime.nowDate())
                .registerIP(playerIP)
                .loginIPList(Lists.newArrayList(playerIP))
                .build();
        // 账号类型
        DbTagAccountType tagAccountType = DbTagAccountType.builder()
                .id(TagAccountTypeService.getId(accountType,accountId ))
                .accountId(accountId)
                .charAccount(charAccount)
                .charAccountPsw(charAccountPsw)
                .build();
        // 创建新数据(创建内存和数据库数据)
        this.createNewDataByKey(tagAccountInfo,tagAccountType);
        // 获取指定账号的信息
        return this.getAccountLoginSendPack(accountId,accountType);
    }


    /**
     * 创建新数据(创建内存和数据库数据)
     *
     * @param tagAccountInfo 账号数据
     * @param tagAccountType 账号类型
     *
     */
    private void createNewDataByKey(DbTagAccountInfo tagAccountInfo,DbTagAccountType tagAccountType) {
        // 创建数据库数据
        this.tagAccountInfoService.insertAccountInfoAndType(tagAccountInfo,tagAccountType);
        // 记录账号对应账号id
        this.charAccountMap.put(AccountManager.getCharAccountKey(tagAccountType.getAccountType(),tagAccountType.getCharAccount()),tagAccountInfo.getId());
        // 账号信息
        Account account = new Account(tagAccountInfo.getId(), this, tagAccountInfo);
        // 增加账号类型
        account.tagAccountTypePut(tagAccountType.getId(), tagAccountType);
        this.accountMap.put(tagAccountInfo.getId(), account);
    }


    /**
     * 获取指定账号的信息
     * @param accountId
     * @return
     */
    public S0000_AccountLogin getAccountLoginSendPack(long accountId,int accountType) {
        // 获取内存数据
        Account account = this.getPlayerLogin(accountId,accountType);
        if (Objects.isNull(account)) {
            throw BizException.Of(CommonEnum.KICKOUT_ACCOUNTNOTFIND.getResultCode(),CommonEnum.KICKOUT_ACCOUNTNOTFIND.getResultMsg());
        }
        // 账号
        String chatAccount = account.getAccountTypeToChatAccount(accountType);
        // 密码
        String accountPsw = account.getAccountTypeToCharAccountPsw(accountType);
        // 创建账号token
        String tokenCredential = this.tokenCredential(accountPsw);
        String token = this.createAccountToken(account.getDbTagAccountInfo().getId(),accountType,chatAccount,tokenCredential);
        if(StringUtils.isEmpty(token)) {
            throw BizException.Of(CommonEnum.KICKOUT_CREATE_TOKEN_ERROR.getResultCode(),CommonEnum.KICKOUT_CREATE_TOKEN_ERROR.getResultMsg());
        }
        // 刷新存在时间
        account.refreshTime();
        // 记录账号token
        account.setAccountTokenInfo(AccountTokenInfo.Of(token,accountType,chatAccount,tokenCredential));
        return S0000_AccountLogin.make(account.getDbTagAccountInfo().getId(),chatAccount,accountType,"" ,token ,"" ,0 ,"" ,"" , "",this.secretManager.getPublicKey());
    }

    /**
     * 创建账号token
     * @param accountID 账号id
     * @param accountType 账号类型
     * @param charAccount 指定类型账号
     * @return
     */
    public String createAccountToken(long accountID,int accountType,String charAccount,String pwd) {
        try {
            String credential = this.tokenCredential(pwd);
            // aes 加密
            return AesEncryptUtils.encrypt(new StringBuilder().append(accountID).append("|").append(CommTime.nowMS()).append("|").append(accountType).append("|").append(charAccount == null ? "" : charAccount).append("|").append(credential).toString(), this.secretManager.getTokenSecret());
        } catch (Exception e) {
            throw BizException.Of(CommonEnum.KICKOUT_AES_ENCRYPT_TOKEN_ERROR.getResultCode(),CommonEnum.KICKOUT_AES_ENCRYPT_TOKEN_ERROR.getResultMsg(),e);
        }
    }

    public String tokenCredential(String source) {
        if (source != null && source.startsWith("cred-v1:")) return source;
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((source == null ? "" : source).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "cred-v1:" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }

    /**
     * 解析token
     * @param token
     * @return
     */
    public CreateAccountTokenInfo parseAccountToken(String token) {
        try {

            String result = AesEncryptUtils.decrypt(token,this.secretManager.getTokenSecret());
            String[]  split = result.split("\\|");
            return CreateAccountTokenInfo.Of(Long.parseLong(split[0]),Long.parseLong(split[1]),Integer.parseInt(split[2]) ,split[3],split[4]);
        } catch (Exception e) {
            throw BizException.Of(CommonEnum.KICKOUT_AES_DECRYPT_TOKEN_ERROR.getResultCode(),CommonEnum.KICKOUT_AES_DECRYPT_TOKEN_ERROR.getResultMsg(),e);
        }
    }

    /**
     * 根据账号获取账号id
     * @param charAccount 账号
     * @return
     */
    public long getAccountIDByCharAccount(int accountType,String charAccount) {
        return this.charAccountMap.get(AccountManager.getCharAccountKey(accountType,charAccount));
    }

    /**
     * 获取玩家登录信息
     * @param accountId 账号id
     * @return
     */
    public Account getPlayerLogin(long accountId,int accountType) {
        // 获取账号信息
        Account account = this.getAccountMap().get(accountId);
        if (Objects.nonNull(account)) {
            // 账号类型Id
            String accountTypeId = TagAccountTypeService.getId(accountType,accountId);
            if(account.getTagAccountTypeMap().containsKey(accountTypeId)) {
                // 存在账号
                return account;
            } else {
                DbTagAccountType dbTagAccountType = this.tagAccountTypeService.getDbTagAccountType(accountTypeId);
                if (Objects.isNull(dbTagAccountType)) {
                    return null;
                }
                // 增加账号类型
                account.tagAccountTypePut(dbTagAccountType.getId(), dbTagAccountType);
                return account;
            }
        } else  {
            DbTagAccountInfo dbTagAccountInfo = this.tagAccountInfoService.getDbTagAccountInfo(accountId);
            if (Objects.isNull(dbTagAccountInfo)) {
                // 找不到账号数据
                return null;
            }
            // 账号类型Id
            String accountTypeId = TagAccountTypeService.getId(accountType,accountId);
            // 获取账号类型
            DbTagAccountType dbTagAccountType = this.tagAccountTypeService.getDbTagAccountType(accountTypeId);
            if (Objects.isNull(dbTagAccountType)) {
                // 找不到账号类型数据
                return null;
            }
            account = new Account(dbTagAccountInfo.getId(), this, dbTagAccountInfo);
            account.tagAccountTypePut(dbTagAccountType.getId(),dbTagAccountType );
            this.accountMap.put(dbTagAccountInfo.getId(),account );
            return account;
        }
    }

    /**
     * 通过账号Id获取账号
     * @param accountId 账号id
     * @return
     */
    public String getAccountIDByCharAccount(long accountId,int accountType) {
        return Optional.ofNullable(this.getPlayerLogin(accountId,accountType)).filter(account -> Objects.nonNull(account)).map(account->account.getAccountTypeToChatAccount(accountType)).orElseGet(()->"");
    }

    /**
     * 获取账号key
     * @param accountType 账号类型
     * @param charAccount 账号
     * @return
     */
    public static final String getCharAccountKey(int accountType,String charAccount) {
       return String.format("TYPE:%d:CHAR:%s",accountType,charAccount);
    }

}
