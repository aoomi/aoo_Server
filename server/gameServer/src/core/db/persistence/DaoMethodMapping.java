package core.db.persistence;

import com.ddm.server.annotation.Query;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.ClassUtil;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * dao代理的方法Aop适配
 */
public class DaoMethodMapping {

    public static final DaoMethodMapping instance = new DaoMethodMapping();

    public static DaoMethodMapping get() {
        return instance;
    }

    /**
     * dao方法适配代理
     *
     * @param method 代理的接口方法
     * @param dao    原实现dao
     * @param params 参数
     * @return
     */
    public Object methodAdapter(Method method, Object dao, Object[] params) {
        try {
            Query query = method.getAnnotation(Query.class);
            Class<?> returnType = method.getReturnType();
            Method[] methods = dao.getClass().getMethods();
            if (query != null) {
                QueryAdapter queryAdapter = new QueryAdapter(params, query, returnType, methods, dao);
                return queryAdapter.resolveQuery();
            }
            Method mappingMethod = dao.getClass().getMethod(method.getName(), method.getParameterTypes());
            if (returnType != Void.TYPE) {
                Object retVal = mappingMethod.invoke(dao, params);
                return retVal;
            }
        } catch (Exception e) {
            CommLogD.error(e.getMessage());
        }
        return -1;
    }

    /**
     * 接口查询处理器
     */
    class QueryAdapter {
        Object[] params;
        Query query;
        Class<?> returnType;
        Method[] methods;
        Object dao;
        int pageNum;
        int pageSize;

        public QueryAdapter(Object[] params, Query query, Class<?> returnType, Method[] methods, Object dao) {
            this.params = params;
            this.query = query;
            this.returnType = returnType;
            this.methods = methods;
            this.dao = dao;
        }

        /**
         * 处理Dao接口请求
         *
         * @return
         */
        public Object resolveQuery() {
            String sql = query.value().trim();
            if (isAggregation(sql)) {//聚合查询
                return resolveAggregation();
            }
            if (sql.startsWith("select ")) {//普通查询
                return resolveNormalSelect();
            } else if (sql.startsWith("update ")) {//更新
                return resolveIUD(dao, params, query.value(), "update");
            } else if (sql.startsWith("insert ")) {//插入
                return resolveIUD(dao, params, query.value(), "insert");
            } else if (sql.startsWith("delete ")) {//删除
                return resolveIUD(dao, params, query.value(), "delete");
            } else {
                return -1;
            }
        }

        /**
         * 普通查询方法处理
         *
         * @return
         */
        public Object resolveNormalSelect() {
            if (!isReturnCollection() && returnType != Page.class) {
                return selectMethod(new ArrayList(Arrays.asList(methods)), params, query, dao, returnType, "loadOne");
            } else {
                return selectMethod(new ArrayList(Arrays.asList(methods)), params, query, dao, returnType, "loadAll");
            }
        }

        /**
         * 聚合查询
         *
         * @return
         */
        public Object resolveAggregation() {
            //聚合查询
            if (!isReturnCollection()) {
                return selectMethod(new ArrayList(Arrays.asList(methods)), params, query, dao, returnType, "aggregation");
            } else {
                return selectMethod(new ArrayList(Arrays.asList(methods)), params, query, dao, returnType, "aggregationAll");
            }
        }

        /**
         * 增更删方法处理
         *
         * @param dao    操作实体
         * @param params 参数
         * @param query  预语句
         * @return
         * @throws Exception
         */
        public Object resolveIUD(Object dao, Object[] params, String query, String method) {
            try {
                Method mappingMethod = dao.getClass().getMethod(method, String.class, Object[].class);
                String resultSql = generateSql(params, query);
                return mappingMethod.invoke(dao, resultSql, null);
            } catch (Exception e) {
                CommLogD.error(e.getMessage());
                return -1;
            }
        }

        /**
         * 查询方法映射
         *
         * @param methodsList 方法列表
         * @param params      参数
         * @param query       查询语句
         * @param object      对象
         * @return
         */
        public Object selectMethod(List<Method> methodsList, Object[] params, Query query, Object object, Class<?> returnType, String methodName) {
            try {
                String resultSql = generateSql(params, query.value());
                Method selectMethod = methodsList.stream().filter(m -> m.getName().equals(methodName)).findFirst().orElse(null);
                Object retVal = null;
                if ("loadAll".equals(methodName) || "loadOne".equals(methodName)) {//普通查询和复杂聚合和分页查询
                    retVal = selectMethod.invoke(object, query.returnClass() == Void.class ? returnType : query.returnClass(), resultSql, null);
                } else if ("aggregation".equals(methodName) || "aggregationAll".equals(methodName)) {//普通聚合查询
                    retVal = selectMethod.invoke(object, resultSql, null);
                }
                return resolveResult(retVal, resultSql);
            } catch (Exception e) {
                CommLogD.error(e.getMessage());
            }
            return null;
        }

        /**
         * 结果处理器
         *
         * @param retVal    结果
         * @param resultSql 查询语句
         * @return
         */
        public Object resolveResult(Object retVal, String resultSql) {
            try {
                if (!isReturnCollection() && retVal instanceof Collection && returnType != Page.class) {
                    return ((Collection) retVal).stream().findFirst().orElse(null);
                }
                //分页构建
                if (returnType == Page.class) {
                    List<Method> methodList = new ArrayList(Arrays.asList(methods));
                    Method method = methodList.stream().filter(m -> "aggregation".equals(m.getName())).findFirst().orElse(null);
                    resultSql = resultSql.substring(resultSql.indexOf("from"), resultSql.indexOf("limit"));
                    resultSql = "select count(1) " + resultSql;
                    Long count = (Long) method.invoke(dao, resultSql, null);
                    int counts = count.intValue() % pageSize > 0 ? count.intValue() / pageSize + 1 : count.intValue() / pageSize;
                    Page page = new Page((List) retVal, pageNum, pageSize, counts, counts);
                    return page;
                }
            } catch (Exception e) {
                CommLogD.error(e.getMessage());
            }
            return retVal;
        }

        /**
         * 参数转化成sql语句，替换占位符
         *
         * @param params 参数
         * @param query  原生sql的占位语句
         * @return
         */
        public String generateSql(Object[] params, String query) {
            if (params != null) {
                boolean isPage = Arrays.asList(params).stream().anyMatch(n -> n instanceof Page);
                String[] valueList = new String[params.length + (isPage ? 1 : 0)];
                for (int i = 0; i < params.length; i++) {
                    if (params[i] instanceof Collection) {//参数是列表
                        Object[] paramsArray = new Object[((Collection) params[i]).size()];
                        ((Collection) params[i]).toArray(paramsArray);
                        String paramsStr = StringUtils.join(paramsArray, ",");
                        valueList[i] = paramsStr;
                    } else if (params[i] instanceof Page) { //参数是分页
                        pageNum = ((Page) params[i]).getPageNumber();
                        pageSize = ((Page) params[i]).getPageSize();
                        valueList[i] = ((pageNum - 1) * pageSize) + "";
                        valueList[i + 1] = pageSize + "";
                        i = i + 1;
                    } else if (ClassUtil.isPrimitive(params[i].getClass())) { //参数是普通类型
                        valueList[i] = params[i].toString();
                    }
                }
                return MessageFormat.format(query, (Object[]) valueList); //格式化sql语句，去掉占位符
            }
            return query;
        }


        /**
         * 是否返回列表
         *
         * @return
         */
        public boolean isReturnCollection() {
            return Collection.class.isAssignableFrom(returnType);
        }

        /**
         * 判断是否是聚合函数
         *
         * @param query
         * @return
         */
        public boolean isAggregation(String query) {
            try {
                query = query.substring(query.indexOf("select"), query.indexOf("from"));
                List<String> aggregationList = Arrays.asList("count(", "sum(", "max(", "min(", "avg(", "first(", "last(");
                if (query.trim().indexOf(",") != -1) {
                    return false;
                }
                for (String aggregation : aggregationList) {
                    if (query.trim().indexOf(aggregation) != -1) {
                        return true;
                    }
                }
            } catch (Exception e) { /* exception-policy: unparseable optional aggregation expression is not an aggregation */ }
            return false;
        }
    }

}
