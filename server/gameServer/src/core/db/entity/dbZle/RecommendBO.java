package core.db.entity.dbZle;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "db_recommend")
@Data
@NoArgsConstructor
public class RecommendBO extends BaseEntity<RecommendBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "varchar(64)", fieldname = "wx_openid", comment = "")
    private String wx_openid;
    @DataBaseField(type = "bigint(20)", fieldname = "accountid", comment = "")
    private long accountid;
    @DataBaseField(type = "varchar(255)", fieldname = "name", comment = "")
    private String name;
    @DataBaseField(type = "varchar(20)", fieldname = "updatetime", comment = "")
    private String updatetime;
    @DataBaseField(type = "int(11)", fieldname = "referer", comment = "")
    private int referer;
    @DataBaseField(type = "bigint(20)", fieldname = "familyID", comment = "")
    private long familyID;
    @DataBaseField(type = "varchar(64)", fieldname = "wx_unionid", comment = "")
    private String wx_unionid;
    @DataBaseField(type = "bigint(20)", fieldname = "onelevel_agents_id", comment = "")
    private int onelevel_agents_id;
    @DataBaseField(type = "int(11)", fieldname = "top_agents_id", comment = "")
    private int top_agents_id;
    @DataBaseField(type = "int(11)", fieldname = "real_referer", comment = "")
    private int real_referer;
    @DataBaseField(type = "int(11)", fieldname = "promoteID", comment = "内部推广员id")
    private int promoteID;

    public void saveAccountid(long accountid) {
        if (accountid == this.accountid) {
            return;
        }
        this.accountid = accountid;
        getBaseService().update("accountid", accountid, id, new AsyncInfo(id));
    }

}
