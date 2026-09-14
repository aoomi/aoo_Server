package core.db.other;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class Criteria {

    private StringBuilder whereSql = new StringBuilder();
    private List<Object> params = new ArrayList<>();
    private long pageNum = -1;
    private long pageSize = -1;
    private long limit = -1;

    public Criteria(String sql, Object params) {
        this.whereSql.append(sql);
        this.params.add(params);
    }

    public Criteria(String sql, Object... params) {
        this.whereSql.append(sql);
        if (params != null) {
            this.params.addAll(Arrays.asList(params));
        }
    }

    public Criteria(LogicalType logicalType, Criteria... criteria) {
        List<Criteria> criteriaList = Arrays.stream(criteria).filter(c -> c != null).collect(Collectors.toList());
        for (int i = 0; i < criteriaList.size(); i++) {
            Criteria temp = criteriaList.get(i);
            if (i == 0) {
                whereSql.append(logicalType == LogicalType.Not ? " not (" : " (");
            }
            whereSql.append(temp.whereSql.toString());
            params.addAll(temp.params);
            if (i < criteriaList.size() - 1) {
                whereSql.append(" " + logicalType.getTypeValue() + " ");
            }
        }
        whereSql.append(") ");
    }

    /**
     * desc
     *
     * @param propertyName 属性名
     */
    public Criteria desc(String propertyName) {
        this.whereSql.append(String.format(" ORDER BY `%s` desc ", propertyName));
        return this;
    }

    /**
     * desc 自定义格式
     *
     * @param propertyName 属性名
     */
    public Criteria descFormat(String propertyName) {
        this.whereSql.append(String.format(" ORDER BY %s desc ", propertyName));
        return this;
    }

    /**
     * desc
     *
     * @param propertyName 属性名
     */
    public Criteria descJoin(String propertyName, String tb) {
        this.whereSql.append(String.format(" ORDER BY %s.`%s` desc ", tb, propertyName));
        return this;
    }

    /**
     * asc
     *
     * @param propertyName 属性名
     */
    public Criteria asc(String propertyName) {
        this.whereSql.append(String.format(" ORDER BY `%s` asc ", propertyName));
        return this;
    }

    /**
     * groupBy
     */
    public Criteria groupBy(String groupItemsName) {
        this.whereSql.append(String.format(" group by `%s` ", groupItemsName));
        return this;
    }

    /**
     * groupBy
     */
    public Criteria groupByJoin(String groupItemsName, String tb) {
        this.whereSql.append(String.format(" group by %s.`%s` ", tb, groupItemsName));
        return this;
    }

    /**
     * 设置极限值
     *
     * @param limit 极限值
     * @return
     */
    public Criteria setLimit(long limit) {
        this.limit = limit;
        return this;
    }

    /**
     * 设置pageNum
     *
     * @param pageNum 页数
     * @return
     */
    public Criteria setPageNum(long pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    /**
     * 设置pageSize
     *
     * @param pageSize 条数
     * @return
     */
    public Criteria setPageSize(long pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * 转成sql语句
     *
     * @return
     */
    public String toSql() {
        if (whereSql.toString().trim().startsWith("and")) {
            return whereSql.toString().trim().substring("and".length());
        }
        if (whereSql.toString().trim().startsWith("or")) {
            return whereSql.toString().trim().substring("or".length());
        }
        if (this.pageNum != -1 && this.pageSize != -1) {
            this.whereSql.append(" limit " + pageNum + "," + pageSize + " ");
        } else if (this.limit != -1) {
            this.whereSql.append(" limit " + limit + " ");
        }

        return whereSql.toString();
    }

    public void updateWhereSql(StringBuilder whereSql) {
        this.whereSql = whereSql;
    }

    public void updateParams(List<Object> params) {
        this.params = params;
    }

    public List<Object> getParams() {
        return params;
    }

    /**
     * 并立条件查询器
     */
    public enum LogicalType {
        And(1),
        Or(2),
        Not(3);
        int type;

        LogicalType(int type) {
            this.type = type;
        }

        public String getTypeValue() {
            switch (this.type) {
                case 1:
                    return "and";
                case 2:
                    return "or";
                case 3:
                    return "not";
                default:
                    return "null";
            }
        }
    }

}
