package core.db.persistence;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import core.db.other.Criteria;
import org.apache.commons.dbutils.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.*;

/**
 * druid工具类
 *
 * @param <T>
 */
public class DBUtil<T> {
    protected Class<T> clazz;

    /**
     * 自动生成操作的实体类
     */
    protected DBUtil() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) superClass;
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs != null && typeArgs.length > 0) {
                if (typeArgs[0] instanceof Class) {
                    clazz = (Class<T>) typeArgs[0];
                }
            }
        }
    }

    /**
     * 设置操作的实体类
     *
     * @param clz 操作实体类
     */
    protected DBUtil(Class<T> clz) {
        clazz = clz;
    }

    /**
     * 映射实体属性和数据库属性
     *
     * @param clazz 操作实体类
     * @return 返回映射Map
     */
    protected Map<String, String> mappingField(Class<?> clazz) {
        Map<String, String> convert = new HashMap<>();
        Field[] fields = clazz.getDeclaredFields();
        DataBaseField dataBaseField;
        for (Field field : fields) {
            if (field.isAnnotationPresent(DataBaseField.class)) {
                dataBaseField = field.getAnnotation(DataBaseField.class);
                convert.put(dataBaseField.fieldname(), field.getName());
            }
        }
        return convert;
    }

    /**
     * 生成结果处理器
     *
     * @param <E>
     * @param clazz         生成的实体类
     * @param handler       结果转化处理器：BeanListHandler，BeanHandler，MapListHandler，MapHandler...（实现ResultSetHandler）
     * @param beanProcessor bean转化器GenerousBeanProcessor（驼峰转化），BeanProcessor
     * @return 返回结果处理器对象
     */
    protected <E> Optional<ResultSetHandler<?>> createHandler(Class<E> clazz, Class handler, BeanProcessor beanProcessor) {
        try {
            switch (handler.getSimpleName()) {
                case "MapListHandler":
                case "MapHandler":
                    if (beanProcessor == null) {
                        return Optional.ofNullable((ResultSetHandler<?>) handler.getDeclaredConstructor().newInstance());
                    }
                    return generateHandler(handler, new Class[]{RowProcessor.class}, new Object[]{new BasicRowProcessor(beanProcessor)});
                case "BeanHandler":
                case "BeanListHandler":
                    if (beanProcessor == null) {
                        return generateHandler(handler, new Class[]{Class.class}, new Object[]{clazz});
                    }
                    return generateHandler(handler, new Class[]{Class.class, RowProcessor.class}, new Object[]{clazz, new BasicRowProcessor(beanProcessor)});
                default:
                    return generateHandler(handler, new Class[]{Class.class, RowProcessor.class}, new Object[]{clazz, new BasicRowProcessor(beanProcessor)});
            }
        } catch (Exception e) {
            CommLogD.error("createHandler:{}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 构造结果集处理器对象
     *
     * @param handler     欲生成的结果处理器类
     * @param classParams 构造方法参数
     * @param values      构造方法值
     * @return 生成的handler
     * @throws Exception
     */
    public Optional<ResultSetHandler<?>> generateHandler(Class handler, Class[] classParams, Object[] values) throws Exception {
        Constructor constructor = handler.getDeclaredConstructor(classParams);
        constructor.setAccessible(true);
        return Optional.ofNullable((ResultSetHandler<?>) constructor.newInstance(values));
    }

    /**
     * 创建结果处理器
     *
     * @param handler 结果转化处理器：BeanListHandler，BeanHandler，MapListHandler，MapHandler...（实现ResultSetHandler）
     * @param clazz   生成的实体类
     * @return 返回结果处理器 ResultSetHandler
     */
    protected ResultSetHandler<?> createResultSetHandler(Class<?> clazz, Class handler) {
        Map<String, String> mappingFields;
        if (clazz.isAnnotationPresent(TableName.class)) {
            TableName tableName = clazz.getAnnotation(TableName.class);
            //field的name和DataBaseField的fieldName不一致需映射属性
            //在返回结果时转化所需的field的name
            if (tableName.fieldMappingOverrides()) {
                mappingFields = mappingField(clazz);
                BeanProcessor beanProcessor = new BeanProcessor(mappingFields);
                Optional<ResultSetHandler<?>> resultSetHandler = createHandler(clazz, handler, beanProcessor);
                if (resultSetHandler.isPresent()) return resultSetHandler.orElseThrow();
            }
        }
        //使用默认驼峰处理器
        Optional<ResultSetHandler<?>> resultSetHandler = createHandler(clazz, handler, new GenerousBeanProcessor());
        return resultSetHandler.orElse(null);
    }

    /**
     * 获取表主键名
     *
     * @return
     */
    protected String getPrimaryKeyByUnique() {
        Field[] fields = clazz.getDeclaredFields();
        String idName = Arrays.asList(fields).stream().filter(field -> field.isAnnotationPresent(DataBaseField.class) && field.getAnnotation(DataBaseField.class).indextype() == DataBaseField.IndexType.Unique).map(field -> {
            DataBaseField dF = field.getAnnotation(DataBaseField.class);
            return String.format("`%s`", dF.fieldname());
        }).findFirst().orElse("");
        if (idName.length() == 0) {
            throw new RuntimeException(clazz + "没有指定@Id注解!");
        }
        return idName;
    }

    /**
     * 获取表名
     *
     * @return 返回数据表名
     */
    protected String getTableName() {
        boolean existTable = clazz.isAnnotationPresent(TableName.class);
        if (!existTable) {
            throw new RuntimeException(clazz + " 没有Table注解.");
        }
        TableName table = clazz.getAnnotation(TableName.class);
        TableName.DbDayEnum dayEnum = table.dbDay();
        if (TableName.DbDayEnum.NOT_DAY.equals(dayEnum)) {
            return String.format("`%s`", table.value());
        } else if (TableName.DbDayEnum.EVERY_DAY.equals(dayEnum)) {
            return String.format("`%s`", table.value() + CommTime.getNowTimeYMD());
        } else if (TableName.DbDayEnum.NEXT_DAY.equals(dayEnum)) {
            return String.format("`%s`", table.value() + CommTime.getNextTimeYMD());
        } else if (TableName.DbDayEnum.BEFORE_DAY.equals(dayEnum)) {
            return String.format("`%s`", table.value() + CommTime.getBeforeTimeYMD());
        } else if (TableName.DbDayEnum.EVERY_6DAY.equals(dayEnum)) {
            return String.format("`%s`", table.value() + CommTime.getCycleNowTime6YMD());
        } else if (TableName.DbDayEnum.NEXT_6DAY.equals(dayEnum)) {
            return String.format("`%s`", table.value() + CommTime.getNextTime6YMD());
        } else if (TableName.DbDayEnum.BEFORE_6DAY.equals(dayEnum)) {
            return String.format("`%s`", table.value() + CommTime.getBeforeTime6YMD());
        }
        return String.format("`%s`", table.value());
    }

    /**
     * 生成删除语句
     *
     * @param uniqueName 索引名
     * @param ids        删除的主键集
     * @return
     */
    protected String getDeleteSqlByIds(String uniqueName, List<Long> ids) {
        String tableName = getTableName();
        uniqueName = StringUtils.isNotEmpty(uniqueName) ? String.format("`%s`", uniqueName) : getPrimaryKeyByUnique();
        StringBuilder deleteSql = new StringBuilder();
        deleteSql.append("delete from ").append(tableName).append(" where ").append(uniqueName).append(ids.size() > 1 ? " in ( {0} )" : " = {0}");
        List<String> placeholder = new ArrayList<>(Collections.nCopies(ids.size(), "?"));
        return MessageFormat.format(deleteSql.toString(), StringUtils.join(placeholder, ","));
    }

    /**
     * 生成插入语句
     *
     * @param element    插入的对象
     * @param params     返回的插入参数
     * @param needIgnore 插入失败是否忽略异常
     * @return
     */
    protected String getInsertSql(T element, List<Object> params, boolean needIgnore) {
        StringBuilder sql = new StringBuilder();
        sql.append("insert ").append(needIgnore ? "ignore " : "").append("into ").append(getTableName()).append(" (");
        Field[] fields = clazz.getDeclaredFields();
        Arrays.asList(fields).forEach(field -> {
            try {
                DataBaseField dbInfo = field.getAnnotation(DataBaseField.class);
                if (dbInfo != null) {
                    field.setAccessible(true);
                    Object fieldValue = field.get(element);
                    String fieldName = dbInfo.fieldname();
                    sql.append(String.format("`%s`,", fieldName));
                    params.add(fieldValue instanceof StringBuilder ? fieldValue.toString() : fieldValue);
                }
            } catch (Exception e) {
                CommLogD.error("getInsertSql:{}", e.getMessage());
            }
        });
        sql.deleteCharAt(sql.length() - 1);
        sql.append(")values(");
        List<String> placeholder = new ArrayList<>(Collections.nCopies(params.size(), "?"));
        sql.append(StringUtils.join(placeholder, ","));
        sql.append(")");
        return sql.toString();
    }

    /**
     * 生成更新语句
     *
     * @param element 更新的对象
     * @param params  返回的更新参数
     * @return
     */
    protected String getUpdateSqlByEntity(T element, List<Object> params) {
        String idName = "";
        Object idValue = null;
        Field[] fields = clazz.getDeclaredFields();

        StringBuilder updateSql = new StringBuilder();
        updateSql.append("update ").append(getTableName()).append(" set ");

        try {
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                field.setAccessible(true);
                DataBaseField column = field.getAnnotation(DataBaseField.class);

                if (column != null) {
                    if (column.indextype() == DataBaseField.IndexType.Unique) { //唯一索引，以后新增注解@Id替换
                        idName = column.fieldname();  //唯一索引字段名
                        idValue = field.get(element); //唯一索引字段值
                        continue;
                    }
                    Object fieldValue = field.get(element);
                    String fieldName = column.fieldname();
                    updateSql.append(" ").append(String.format("`%s`", fieldName)).append(" = ?,");
                    params.add(fieldValue instanceof StringBuilder ? fieldValue.toString() : fieldValue);
                }
            }
        } catch (Exception e) {
            CommLogD.error("getUpdateSqlByEntity:{}", e.getMessage());
        }

        if (idValue != null) {
            params.add(idValue);
            updateSql.deleteCharAt(updateSql.length() - 1);
            updateSql.append(" where ").append(String.format("`%s`", idName)).append(" = ?");
            return updateSql.toString();
        }

        return "";
    }

    /**
     * 生成更新语句
     *
     * @param updateMap (key->value)  更新的map
     * @param params    返回的更新参数
     * @return
     */
    protected String getUpdateSqlByUnique(Map<String, Object> updateMap, Criteria criteria, List<Object> params) {
        StringBuilder updateSql = new StringBuilder();
        updateSql.append("update ").append(getTableName()).append(" set ");

        updateMap.entrySet().forEach(updateEntrySet -> {
            Object fieldValue = updateEntrySet.getValue();
            String fieldName = updateEntrySet.getKey();
            updateSql.append(" ").append(String.format("`%s`", fieldName)).append(" = ?,");
            params.add(fieldValue instanceof StringBuilder ? fieldValue.toString() : fieldValue);
        });

        updateSql.deleteCharAt(updateSql.length() - 1);
        updateSql.append(" where ");
        updateSql.append(criteria.toSql());
        params.addAll(criteria.getParams());
        return updateSql.toString();
    }

}
