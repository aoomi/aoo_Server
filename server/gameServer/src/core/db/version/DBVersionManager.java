package core.db.version;

import BaseCommon.CommClass;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.common.CommLogD;
import core.db.entity.BaseEntity;
import core.db.entity.DBVersionInfoBO;
import core.db.service.DBVersionInfoBOService;
import core.ioc.ContainerMgr;
import core.ioc.ClassFind;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/**
 * The implementation class of DB version manager and automatic updates
 *
 * @author Aaron
 */
public abstract class DBVersionManager extends AbstractDBVersionManager {

    private boolean m_bInitializeOk = false;
    private String m_newestVersion = "0.0.0";
    private DBVersionInfoBO m_versionBO = new DBVersionInfoBO();
    private DBVersionInfoBOService dbVersionInfoBOService;

    public DBVersionManager() {
        dbVersionInfoBOService = ContainerMgr.get().getComponent(DBVersionInfoBOService.class);
    }

    private void ensureDBVersionInfoBOService() {
        if (dbVersionInfoBOService == null) {
            dbVersionInfoBOService = ContainerMgr.get().getComponent(DBVersionInfoBOService.class);
        }
        if (dbVersionInfoBOService == null) {
            throw new IllegalStateException("DBVersionInfoBOService has not been initialized");
        }
    }

    @Override
    protected boolean initCurrentVersion() {
        ensureDBVersionInfoBOService();
        if (m_bInitializeOk) {
            return true;
        }

        dbVersionInfoBOService.setSourceName(getSourceName());
        String isExistTableversioninfo = dbVersionInfoBOService.existTable();

        List<DBVersionInfoBO> resultList = isExistTableversioninfo != null && isExistTableversioninfo.length() > 0 ? dbVersionInfoBOService.findAll(null, "") : null;

        if (resultList != null && resultList.size() > 0) {
            m_versionBO = resultList.get(0);
        } else {
            if (resultList == null) {
                dbVersionInfoBOService.createTableSql();
            }
            if (dbVersionInfoBOService.saveIgnoreOrUpDate(m_versionBO) <= -1) {
                CommLogD.error("initialize: insert bo failed!!!");
            }
        }

        m_bInitializeOk = true;
        return true;
    }

    @Override
    public String getCurVersion() {
        return m_versionBO.getVersion();
    }

    @Override
    protected boolean _setCurVersion(String version) {
        ensureDBVersionInfoBOService();
        m_versionBO.setVersion(version);
        return dbVersionInfoBOService.saveIgnoreOrUpDate(m_versionBO) >= 0;
    }

    @Override
    public String getNewestVersion() {
        return m_newestVersion;
    }

    public void setNewestVersion(String ver) {
        m_newestVersion = ver;
    }

    public boolean runAutoVersionUpdate(String packagePath) {
        regAllUpdate(packagePath);
        return this.run();
    }

    @SuppressWarnings("unchecked")
    public boolean checkAndUpdateVersion(String packagePath) {
        ensureDBVersionInfoBOService();

        try {
            // Clear a connection retained in the current thread during IOC startup.
            // Holding this connection for the whole schema scan deadlocks a
            // single-connection local pool when DAO methods request their own.
            try (Connection ignored = getConnection()) {
                // Closing immediately returns it to Druid before DAO access.
            }
            CommLogD.warn("==========开始检测数据库版本==============");
            // 读取当前所有的表
            dbVersionInfoBOService.setSourceName("clark_game");
            List<Map<String, Object>> mapList = dbVersionInfoBOService.showTables();
            List<String> tables = new ArrayList<String>();
            mapList.stream().forEach(table -> {
                tables.add(String.valueOf(table.values().toArray(new String[table.size()])[0]).toLowerCase());
            });

            List<Class<BaseEntity>> tableToAdd = new ArrayList<>();
            List<String> fieldsToAdd = new ArrayList<String>();
            List<String> fieldsToUpdate = new ArrayList<String>();

            List<Class<?>> clazz = getAllClassesByType(BaseEntity.class, packagePath);

            for (Class<?> cs : clazz) {
                BaseEntity bo = (BaseEntity) CommClass.forName(cs.getName()).newInstance();
                String tableName = bo.getTableName();
                if (!tables.contains(tableName.toLowerCase())) {
                    tableToAdd.add((Class<BaseEntity>) bo.getClass());
                    continue;
                }
                mapList = dbVersionInfoBOService.desc(bo.getTableName());
                mapList = mapList.stream().filter(m -> m.containsKey("Field") || m.containsKey("Type")).collect(Collectors.toList());
                Map<String, String> fieldsInDB = new HashMap<String, String>();
                mapList.stream().forEach(fixField -> {
                    String field = String.valueOf(fixField.get("Field"));
                    String type = String.valueOf(fixField.get("Type"));
                    fieldsInDB.put(field, type);
                });

                // <field, type> - 代码中的字段
                Map<String, DataBaseField> fieldsInCode = new HashMap<>();
                for (Field curField : cs.getDeclaredFields()) {
                    DataBaseField dbinfo = curField.getAnnotation(DataBaseField.class);
                    if (dbinfo == null) {
                        continue;
                    }
                    if (dbinfo.size() == 0) { // 非列表类型
                        fieldsInCode.put(curField.getName(), dbinfo);
                    } else {// 列表类型
                        for (int i = 0; i < dbinfo.size(); ++i) {
                            fieldsInCode.put(curField.getName() + "_" + i, dbinfo);
                        }
                    }
                }
                // 记录需要升级的字段
                for (Entry<String, DataBaseField> fieldInCode : fieldsInCode.entrySet()) {
                    DataBaseField fieldInfo = fieldInCode.getValue();
                    String fieldName = fieldInfo.fieldname();
                    //针对list处理
                    if (fieldInfo.size() != 0) {
                        fieldName = fieldInCode.getKey();
                    }

                    if (!fieldsInDB.containsKey(fieldName.trim())) {
                        fieldsToAdd.add(String.format("ALTER TABLE `%s` ADD `%s` %s;", tableName, fieldName, getFieldInfo(fieldInfo)));
                    } else if (checkFieldToUpdate(fieldInfo.type(), fieldsInDB.get(fieldName))) {
                        fieldsToUpdate.add(String.format("ALTER TABLE `%s` MODIFY COLUMN `%s`  %s ;", tableName, fieldName, getFieldInfo(fieldInfo)));
                    }
                }
            }
            // 添加数据库表
            for (Class<BaseEntity> table : tableToAdd) {
                CommLogD.info("添加新增数据库表 ：{}", table.getSimpleName().replace("BO", ""));
                this.addTable(table);
            }
            // 添加字段
            if (!fieldsToAdd.isEmpty()) {
                for (String fieldSql : fieldsToAdd) {
                    CommLogD.info("添加字段, SQL：" + fieldSql);
                    dbVersionInfoBOService.execute(fieldSql);
                }
            }
            // 升级字段
            if (!fieldsToUpdate.isEmpty()) {
                for (String fieldSql : fieldsToUpdate) {
                    CommLogD.info("升级字段, SQL：" + fieldSql);
                    dbVersionInfoBOService.execute(fieldSql);
                }
            }
        } catch (Exception e) {
            CommLogD.error("升级数据库失败，请确认数据库连接配置，原因" + e.getMessage(), e);
            System.exit(-1);
        }
        CommLogD.warn("==========数据库版本检测完毕==============");
        return true;
    }

    private String getFieldInfo(DataBaseField dbinfo) {
        Object defaultValue = dbinfo.defaultValue();
        String type = dbinfo.type();
        if (type.startsWith("bytes") || type.startsWith("blob") || type.startsWith("text")) {
            return String.format("%s NOT NULL COMMENT '%s'", type, dbinfo.comment());
        } else if (type.startsWith("timestamp")) {
            return String.format("%s NULL DEFAULT NULL COMMENT '%s'", type, dbinfo.comment());
        } else if (type.startsWith("int") || type.startsWith("tinyint") || type.startsWith("bigint")) {
            defaultValue = StringUtils.isNumeric(String.valueOf(defaultValue)) ? defaultValue : 0;
        } else if (type.startsWith("varchar")) {
            defaultValue = "";
        } else if (type.startsWith("float") || type.startsWith("double")) {
            defaultValue = "0.00";
        } else {
            defaultValue = "";
        }
        return String.format("%s NOT NULL DEFAULT '%s' COMMENT '%s'", type, defaultValue, dbinfo.comment());
    }

    private boolean checkFieldToUpdate(String fieldInCode, String fieldInDB) {
        if (fieldInCode.equals(fieldInDB)) {
            return false;
        }

        // 检测Int类型
        List<String> intfields = Arrays.asList("tinyint(1)", "tinyint(2)", "tinyint(4)", "smallint(6)", "mediumint(9)", "int(11)", "bigint(20)");
        int idxIntCode = intfields.indexOf(fieldInCode);
        int idxIntDB = intfields.indexOf(fieldInDB);
        if (idxIntCode >= 0 && idxIntDB >= 0) {
            return idxIntCode > idxIntDB;
        }

        // 检测文本类型
        String sCodeTxtType = fieldInCode.startsWith("varchar") ? "varchar" : fieldInCode;
        String sDBTxtType = fieldInDB.startsWith("varchar") ? "varchar" : fieldInDB;
        // 检测varchar
        if ("varchar".equals(sDBTxtType) && "varchar".equals(sCodeTxtType)) {
            return Integer.parseInt(fieldInCode.split("\\W", 3)[1]) > Integer.parseInt(fieldInDB.split("\\W", 3)[1]);
        }
        List<String> textFields = Arrays.asList("varchar", "text", "mediumtext", "longtext");
        int idxTxtCode = textFields.indexOf(sCodeTxtType);
        int idxTxtDB = textFields.indexOf(sDBTxtType);
        if (idxTxtCode >= 0 && idxTxtDB >= 0) {
            return idxTxtCode > idxTxtDB;
        }
        return false;
    }

    protected void regAllUpdate(String path) {
        List<Class<?>> dealers = getAllClassesByType(IUpdateDBVersion.class, path);

        for (Class<?> cs : dealers) {
            IUpdateDBVersion dealer = null;
            try {
                dealer = (IUpdateDBVersion) CommClass.forName(cs.getName()).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) { /* exception-policy: unloadable optional migration provider is skipped */ }

            if (null == dealer) {
                continue;
            }

            regVersionUpdate(dealer);
        }
    }

    private static List<Class<?>> getAllClassesByType(Class<?> parentType, String packagePath) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> candidate : ClassFind.getClasses(packagePath)) {
            if (candidate != parentType && parentType.isAssignableFrom(candidate)
                    && !candidate.isInterface()
                    && !java.lang.reflect.Modifier.isAbstract(candidate.getModifiers())) {
                result.add(candidate);
            }
        }
        return result;
    }

    public <T extends BaseEntity> boolean addTable(Class<T> clz) {

        String tableName;
        String sql;

        try {
            T bo = clz.newInstance();
            tableName = (String) clz.getMethod("getTableName").invoke(bo);
            sql = (String) clz.getMethod("getSql_TableCreate").invoke(bo);
        } catch (Throwable ex) {
            return false;
        }

        try {
            if (dbVersionInfoBOService.execute(sql) < 0) {
                CommLogD.error(tableName + " Create Fail!");
                return false;
            }
        } catch (Exception e) {
            CommLogD.error(tableName + " Create Fail!:{}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public String getCatalog() {
        try (Connection connection = getConnection()) {
            return connection == null ? null : connection.getCatalog();
        } catch (SQLException e) {
            CommLogD.error(e.getMessage(), e);
        }
        return null;
    }
}
