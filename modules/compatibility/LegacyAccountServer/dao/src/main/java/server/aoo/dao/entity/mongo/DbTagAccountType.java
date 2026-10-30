package server.aoo.dao.entity.mongo;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "DbTagAccountType")
@CompoundIndexes({
        @CompoundIndex(name = "accountTypeCharAccountUnique", def = "{'accountType':1,'charAccount':1}", unique = true),
        @CompoundIndex(name = "accountIdTypeUnique", def = "{'accountId':1,'accountType':1}", unique = true)
})
public class DbTagAccountType {
    /**
     * id
     * 1 + accounttype + accountId +
     */
    @Id
    private String id;
    /**
     * 账号id
     */
    private long accountId;
    /**
     * 账号类型
     */
    private int accountType;
    /**
     * 账号
     */
    private String charAccountPsw;
    /**
     * 密码
     */
    private String charAccount;
}
