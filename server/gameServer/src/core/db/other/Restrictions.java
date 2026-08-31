package core.db.other;

import com.ddm.server.common.utils.AssertsUtil;
import com.ddm.server.websocket.def.ErrorCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.Map;


/**
 * 查询策略
 */
@NoArgsConstructor
@Getter
@Setter
public class Restrictions {

    public static Criteria and(Criteria... criteria) {
        return new Criteria(Criteria.LogicalType.And, criteria);
    }

    public static Criteria or(Criteria... criteria) {
        return new Criteria(Criteria.LogicalType.Or, criteria);
    }

    /**
     * id相等
     *
     * @param value
     */
    public static Criteria idEq(Object value) {
        return new Criteria(" id = ? ", value);
    }

    /**
     * 相等
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria eq(String propertyName, Object value) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " = ? ", value);
    }

    /**
     * 相等
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria eqJoin(String propertyName, Object value, String tb) {
        propertyName = String.format("%s.`%s`", tb, propertyName);
        return new Criteria(" " + propertyName + " = ? ", value);
    }

    /**
     * 等于或者空
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria eqOrIsNull(String propertyName, Object value) {
        return or(eq(propertyName, value), isNull(propertyName));
    }

    /**
     * 不等于
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria ne(String propertyName, Object value) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " <> ? ", value);
    }

    /**
     * 不等于或者为空
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria neOrIsNotNull(String propertyName, Object value) {
        return or(ne(propertyName, value), isNotNull(propertyName));
    }

    /**
     * 模糊查询
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria like(String propertyName, Object value) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " like ? ", value);
    }

    /**
     * 模糊查询
     *
     * @param propertyName 属性名
     * @param value        属性值
     * @param matchMode    模糊查询器
     */
    public static Criteria like(String propertyName, String value, MatchMode matchMode) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " like ? ", matchMode.toMatchString(value));
    }

    /**
     * 大于
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria gt(String propertyName, Object value) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " > ? ", value);
    }

    /**
     * 大于
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria gtJoin(String propertyName, Object value, String tb) {
        propertyName = String.format("%s.`%s`", tb, propertyName);
        return new Criteria(" " + propertyName + " > ? ", value);
    }

    /**
     * 小于
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria lt(String propertyName, Object value) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " < ? ", value);
    }

    /**
     * 小于等于
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria le(String propertyName, Object value) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " <= ? ", value);
    }

    /**
     * 大于等于
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria ge(String propertyName, Object value) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " >= ? ", value);
    }

    /**
     * 大于等于
     *
     * @param propertyName 属性名
     * @param value        属性值
     */
    public static Criteria geJoin(String propertyName, Object value, String tb) {
        propertyName = String.format("%s.`%s`", tb, propertyName);
        return new Criteria(" " + propertyName + " >= ? ", value);
    }

    /**
     * 中间between
     *
     * @param propertyName 属性名
     * @param lo           最小值
     * @param hi           最大值
     */
    public static Criteria between(String propertyName, Object lo, Object hi) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " between ? and ? ", lo, hi);
    }

    /**
     * in查询
     *
     * @param propertyName 属性名
     * @param values       参数数组
     */
    public static Criteria in(String propertyName, Object[] values) {
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            temp.append("?");
            if (i < values.length - 1) {
                temp.append(",");
            }
        }
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " in (" + temp.toString() + ") ", values);
    }

    /**
     * not in查询
     *
     * @param propertyName 属性名
     * @param values       参数数组
     */
    public static Criteria notin(String propertyName, Object[] values) {
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            temp.append("?");
            if (i < values.length - 1) {
                temp.append(",");
            }
        }
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " not in (" + temp.toString() + ") ", values);
    }

    /**
     * not in查询
     *
     * @param propertyName 属性名
     * @param values       属性值
     */
    public static Criteria notin(String propertyName, Collection values) {
        return notin(propertyName, values.toArray(new Object[values.size()]));
    }

    /**
     * in查询
     *
     * @param propertyName 属性名
     * @param values       属性值
     */
    public static Criteria in(String propertyName, Collection values) {
        return in(propertyName, values.toArray(new Object[values.size()]));
    }

    /**
     * 为空
     *
     * @param propertyName 属性名
     */
    public static Criteria isNull(String propertyName) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" ISNULL(" + propertyName + ") ");
    }

    /**
     * 不为空
     *
     * @param propertyName 属性名
     */
    public static Criteria isNotNull(String propertyName) {
        propertyName = String.format("`%s`", propertyName);
        return new Criteria(" " + propertyName + " is not null ");
    }

    /**
     * 属性名相等
     *
     * @param propertyName      属性名
     * @param otherPropertyName 属性名
     * @return
     */
    public static Criteria eqProperty(String propertyName, String otherPropertyName) {
        propertyName = String.format("`%s`", propertyName);
        otherPropertyName = String.format("`%s`", otherPropertyName);
        return new Criteria(" " + propertyName + " = " + otherPropertyName);
    }

    /**
     * 属性名不等
     *
     * @param propertyName      属性名
     * @param otherPropertyName 属性名
     */
    public static Criteria neProperty(String propertyName, String otherPropertyName) {
        propertyName = String.format("`%s`", propertyName);
        otherPropertyName = String.format("`%s`", otherPropertyName);
        return new Criteria(" " + propertyName + " <> " + otherPropertyName);
    }

    /**
     * 属性小于属性
     *
     * @param propertyName      属性名
     * @param otherPropertyName 属性名
     */
    public static Criteria ltProperty(String propertyName, String otherPropertyName) {
        propertyName = String.format("`%s`", propertyName);
        otherPropertyName = String.format("`%s`", otherPropertyName);
        return new Criteria(" " + propertyName + " < " + otherPropertyName);
    }

    /**
     * 属性小于等于属性
     *
     * @param propertyName      属性名
     * @param otherPropertyName 属性名
     */
    public static Criteria leProperty(String propertyName, String otherPropertyName) {
        propertyName = String.format("`%s`", propertyName);
        otherPropertyName = String.format("`%s`", otherPropertyName);
        return new Criteria(" " + propertyName + " <= " + otherPropertyName);
    }

    /**
     * 属性大于属性
     *
     * @param propertyName      属性名
     * @param otherPropertyName 属性名
     */
    public static Criteria gtProperty(String propertyName, String otherPropertyName) {
        propertyName = String.format("`%s`", propertyName);
        otherPropertyName = String.format("`%s`", otherPropertyName);
        return new Criteria(" " + propertyName + " > " + otherPropertyName);
    }

    /**
     * 属性大于等于属性
     *
     * @param propertyName      属性名
     * @param otherPropertyName 属性名
     */
    public static Criteria geProperty(String propertyName, String otherPropertyName) {
        propertyName = String.format("`%s`", propertyName);
        otherPropertyName = String.format("`%s`", otherPropertyName);
        return new Criteria(" " + propertyName + " >= " + otherPropertyName);
    }

    /**
     * 查询策略 case where ... then ... else ..
     *
     * @param whenCondition when 条件
     * @param elseCondition else 条件
     */
    public static Criteria caseWhen(Map<String, String> whenCondition, String elseCondition) {
        AssertsUtil.notNull(whenCondition, ErrorCode.Object_IsNull, "查询条件是空的");
        AssertsUtil.notNull(elseCondition, ErrorCode.Object_IsNull, "查询条件是空的");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" case ");
        whenCondition.entrySet().stream().forEach(entry -> {
            stringBuilder.append(" when ");
            stringBuilder.append(entry.getKey());
            stringBuilder.append(" then ");
            stringBuilder.append(entry.getValue());
            stringBuilder.append(" ");
        });
        stringBuilder.append(" else ");
        stringBuilder.append(elseCondition);
        stringBuilder.append(" end ");
        return new Criteria(stringBuilder.toString());
    }

    /**
     * 添加原生sql
     *
     * @param rawSql 属性名
     */
    public static Criteria addRawWhere(String rawSql) {
        return new Criteria(" " + rawSql + " ");
    }
}
