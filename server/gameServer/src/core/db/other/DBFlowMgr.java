package core.db.other;

import BaseCommon.CommClass;
import BaseCommon.CommLog;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.common.CommLogD;
import com.google.common.collect.Lists;
import core.db.DataBaseMgr;
import core.db.entity.BaseClarkLogEntity;
import core.db.entity.BaseEntity;
import core.db.entity.clarkLog.*;
import core.db.service.DBVersionInfoBOService;
import core.ioc.ContainerMgr;
import core.logger.flow.disruptor.log.BatchDbLogComponent;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class DBFlowMgr {
    private DBVersionInfoBOService dbVersionInfoBOService;

    private DBFlowMgr() {
        dbVersionInfoBOService = ContainerMgr.get().getComponent(DBVersionInfoBOService.class);
    }

    public static DBFlowMgr getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final DBFlowMgr INSTANCE = new DBFlowMgr();
    }


    /**
     * 添加
     *
     * @param DBLoggerBase
     */
    public void add(BaseClarkLogEntity DBLoggerBase) {
        BatchDbLogComponent.getInstance().publish(DBLoggerBase);
    }


    private Connection getConnection() {
        try {
            return DataBaseMgr.get("clark_log").getConnection();
        } catch (Exception e) {
            CommLogD.error("getConnection fail:clark_log");
        }
        return null;
    }

    public void roomPromotionPointLogLog() {
        try (Connection conn = getConnection()) {
            CommLog.info("==========创建明天的表 roomPromotionPointLog==============");
            // 添加数据库表
            addTable(RoomPromotionPointLogFlow.class);
            addTable(RoomPromotionPointLogNextDayFlow.class);
        } catch (Exception e) {
            CommLogD.error("升级数据库失败，原因" + e.getMessage(), e);
        }
        CommLog.info("==========创建明天的表数据库版本检测完毕 roomPromotionPointLog==============");
    }

    public void roomSportsPointChangeZhongZhiLog() {
        try (Connection conn = getConnection()) {
            CommLog.info("==========创建明天的表 roomPromotionPointLog==============");
            // 添加数据库表
            addTable(SportsPointChangeZhongZhiLogFlow.class);
            addTable(SportsPointChangeZhongZhiLogNextDayFlow.class);
        } catch (Exception e) {
            CommLogD.error("升级数据库失败，原因" + e.getMessage(), e);
        }
        CommLog.info("==========创建明天的表数据库版本检测完毕 roomPromotionPointLog==============");
    }

    public void clubLevelRoomLog() {
        try (Connection conn = getConnection()) {
            CommLog.info("==========创建明天的表 clubLevelRoomLog==============");
            // 添加数据库表
            addTable(ClubLevelRoomLogFlow.class);
            addTable(ClubLevelRoomLogNextDayFlow.class);
        } catch (Exception e) {
            CommLogD.error("升级数据库失败，原因" + e.getMessage(), e);
        }
        CommLog.info("==========创建明天的表数据库版本检测完毕 clubLevelRoomLog==============");
    }

    public void clubLevelRoomLogZhongZhi() {
        try (Connection conn = getConnection()) {
            CommLog.info("==========创建明天的表 roomPromotionPointLog==============");
            // 添加数据库表
            addTable(ClubLevelRoomLogZhongZhiFlow.class);
            addTable(ClubLevelRoomLogZhongZhiNextDayFlow.class);
        } catch (Exception e) {
            CommLogD.error("升级数据库失败，原因" + e.getMessage(), e);
        }
        CommLog.info("==========创建明天的表数据库版本检测完毕 roomPromotionPointLog==============");
    }

    public void execBatchDbLog(Map<String, List<BaseClarkLogEntity>> logFlows) {
        logFlows.entrySet().forEach(entry -> {
            //新new对象做操作
            List<BaseClarkLogEntity> list = Lists.newArrayList(entry.getValue());
            if (CollectionUtils.isNotEmpty(list)) {
                int length = list.get(0).addToBatch().length;
                String insetSql = list.get(0).getInsertSql();
                Object[][] parmas = new Object[list.size()][length];
                for (int i = 0; i < list.size(); i++) {
                    BaseClarkLogEntity entity = list.get(i);
                    parmas[i] = entity.addToBatch();
                }
                if (list.get(0).getLogBaseService() != null) {
                    list.get(0).getLogBaseService().batch(insetSql, parmas);
                }
            }
        });
    }


    @SuppressWarnings("unchecked")
    public void updateDB(String packagePath) {
        if (dbVersionInfoBOService == null) {
            dbVersionInfoBOService = ContainerMgr.get().getComponent(DBVersionInfoBOService.class);
        }
        if (dbVersionInfoBOService == null) {
            CommLogD.warn("跳过日志库版本检测：DBVersionInfoBOService 未注册");
            return;
        }
        try (Connection conn = getConnection()) {
            CommLogD.warn("==========开始检测[日志库]版本==============");

            // 读取当前所有的表
            dbVersionInfoBOService.setSourceName("clark_log");
            List<Map<String, Object>> mapList = dbVersionInfoBOService.showTables();
            List<String> tables = new ArrayList<String>();
            mapList.stream().forEach(table -> {
                tables.add(String.valueOf(table.values().toArray(new String[table.size()])[0]).toLowerCase());
            });

            List<Class<BaseClarkLogEntity>> tableToAdd = new ArrayList<>();
            List<String> fieldsToAdd = new ArrayList<String>();
            List<String> fieldsToUpdate = new ArrayList<String>();

            List<Class<?>> clazz = CommClass.getAllClassByInterface(BaseEntity.class, packagePath);

            for (Class<?> cs : clazz) {
                BaseClarkLogEntity bo = (BaseClarkLogEntity) CommClass.forName(cs.getName()).newInstance();
                String tableName = bo.getTableName().replace("`", "");
                if (!tables.contains(tableName.toLowerCase())) {
                    tableToAdd.add((Class<BaseClarkLogEntity>) bo.getClass());
                    continue;
                }
                mapList = dbVersionInfoBOService.desc(bo.getTableName());
                mapList = mapList.stream().filter(m -> m.containsKey("Field") || m.containsKey("Type"))
                        .collect(Collectors.toList());
                Map<String, String> fieldsInDB = new HashMap<String, String>();
                mapList.stream().forEach(fixField -> {
                    String field = String.valueOf(fixField.get("Field"));
                    String type = String.valueOf(fixField.get("Type"));
                    fieldsInDB.put(field, type);
                });

                // <field, getType> - 代码中的字段
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
                for (Map.Entry<String, DataBaseField> fieldInCode : fieldsInCode.entrySet()) {
                    DataBaseField fieldInfo = fieldInCode.getValue();
                    String fieldName = fieldInfo.fieldname();
                    // 针对list处理
                    if (fieldInfo.size() != 0) {
                        fieldName = fieldInCode.getKey();
                    }
                    if (!fieldsInDB.containsKey(fieldName.trim())) {
                        fieldsToAdd.add(String.format("ALTER TABLE `%s` ADD `%s` %s;", tableName, fieldName,
                                getFieldInfo(fieldInfo)));
                    } else if (checkFieldToUpdate(fieldInfo.type(), fieldsInDB.get(fieldName))) {
                        fieldsToUpdate.add(String.format("ALTER TABLE `%s` MODIFY COLUMN `%s`  %s ;", tableName,
                                fieldName, getFieldInfo(fieldInfo)));
                    }
                }
            }
            // 添加数据库表
            for (Class<BaseClarkLogEntity> table : tableToAdd) {
                CommLogD.info("添加新增数据库表 ：{}", table.getSimpleName().replace("Flow", ""));
                addTable(table);
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
            CommLogD.error("升级数据库失败，原因" + e.getMessage(), e);
            System.exit(-1);
        }
        CommLogD.warn("==========数据库版本检测完毕==============");
    }

    // =============== 通用的修改表结构接口 ===================
    public <T extends BaseClarkLogEntity> boolean addTable(Class<T> clz) {

        String tableName;
        String sql;

        try {
            T bo = clz.newInstance();
            tableName = (String) clz.getMethod("getTableName").invoke(bo);
            sql = (String) clz.getMethod("getCreateTableSQL").invoke(bo);
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

    private String getFieldInfo(DataBaseField dbinfo) {
        Object defaultValue = "";
        String type = dbinfo.type();
        if (type.startsWith("bytes") || type.startsWith("blob") || type.startsWith("text") || type.startsWith("longtext")) {
            return String.format("%s NOT NULL COMMENT '%s'", type, dbinfo.comment());
        } else if (type.startsWith("timestamp")) {
            return String.format("%s NULL DEFAULT NULL COMMENT '%s'", type, dbinfo.comment());
        } else if (type.startsWith("int") || type.startsWith("tinyint") || type.startsWith("bigint")) {
            defaultValue = 0;
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

        // 检测Int类型 新增int 转double
        List<String> intfields = Arrays.asList("tinyint(1)", "tinyint(2)", "tinyint(4)", "smallint(6)", "mediumint(9)",
                "int(11)", "bigint(20)", "double(11,2)");
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

}
