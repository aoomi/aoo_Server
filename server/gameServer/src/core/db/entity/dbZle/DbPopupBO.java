package core.db.entity.dbZle;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "db_popup")
@Data
@NoArgsConstructor
public class DbPopupBO extends BaseEntity<DbPopupBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "", indextype = DataBaseField.IndexType.Unique)
    private long id;
    private String title = ""; //标题
    private long star;  //开始时间
    private long end; //结束时间
    private long pupopType = -1; //弹窗类型  1.不弹窗 2.每次登陆弹窗 3.每天首次登陆弹窗
    private int crowdtype; //可见人群 1.全部玩家  2.代理及名下玩家 3.除代理及名下玩家外其他玩家 4.指定亲友圈
    private String crowdlist; //可见人群列表
    private String propaganda; //宣传图
    private int sort; //排序
    private long createtime; //创建时间
    private String beizhu; //备注

}
