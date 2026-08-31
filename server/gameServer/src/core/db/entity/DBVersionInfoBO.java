package core.db.entity;

import com.ddm.server.annotation.Bean;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@TableName(value = "TableVersionInfo", fieldMappingOverrides = true)
@Bean
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DBVersionInfoBO extends BaseEntity {

    @DataBaseField(type = "bigint(50)", fieldname = "ID", comment = "主键", indextype = DataBaseField.IndexType.Unique)
    private long id = 0;
    @DataBaseField(type = "varchar(50)", fieldname = "Version", comment = "版本")
    private String version = "0.0.0";
    @DataBaseField(type = "varchar(50)", fieldname = "Info", comment = "信息")
    private String info = "";

    public DBVersionInfoBO(DBVersionInfoBO _bo) {
        this.id = _bo.id;
        this.version = _bo.version;
        this.info = _bo.info;
    }

    public String createTableSql() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (%s int(11) NOT NULL, %s varchar(64) NOT NULL, %s varchar(64), PRIMARY KEY (%s));", this.getTableName(), "ID",
                "Version", "Info", "ID");
    }
}
