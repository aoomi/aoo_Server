package core.db.persistence;

import com.ddm.server.common.CommLogD;
import core.db.DataBaseMgr;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * druid操作层
 *
 * @param <T>
 */
public abstract class BaseDao<T> extends DBUtil<T> {

    private QueryRunner queryRunner;

    /**
     * 自动生成操作的实体类
     */
    protected BaseDao() {
        super();
        queryRunner = new QueryRunner();
    }

    /**
     * 手动设置操作的实体类
     *
     * @param clz
     */
    protected BaseDao(Class<T> clz) {
        super(clz);
        queryRunner = new QueryRunner();
    }

    /**
     * 更新
     *
     * @param sql  更新语句
     * @param args 更新值
     * @return
     */
    protected int update(String sql, Object... args) {
        int i = -1;
        try {
            i = queryRunner.update(getDataSource().getConnection(), sql, args);
        } catch (SQLException e) {
            stackTrace(String.format("db update sql:{%s},params:{%s}", sql, paramSql(args)), e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace(String.format("db update discardConnectionFromDao sql:{%s},params:{%s}", sql, paramSql(args)), e);
            }
        }
        return i;
    }

    /**
     * 插入
     *
     * @param sql  插入语句
     * @param args 插入值
     * @return
     */
    protected long insertAndGetGeneratedKeys(String sql, Object... args) {
        try {
            Number id = queryRunner.insert(getDataSource().getConnection(), sql, new ScalarHandler<>(), args);
            return id != null ? id.longValue() : 0;
        } catch (Exception e) {
            stackTrace(String.format("db insertAndGetGeneratedKeys sql:{%s},params:{%s}", sql, paramSql(args)), e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace(String.format("db insertAndGetGeneratedKeys discardConnectionFromDao sql:{%s},params:{%s}", sql, paramSql(args)), e);

            }
        }
        return -1;
    }

    /**
     * 查询
     *
     * @param sql  查询语句
     * @param args 查询值
     * @return
     */
    protected T getBean(String sql, Object... args) {
        try {
            return queryRunner.query(getDataSource().getConnection(), sql, (BeanHandler<T>) createResultSetHandler(clazz, BeanHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db getBean", e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace("db getBean", e);
            }
        }
        return null;
    }

    /**
     * 查询多条
     *
     * @param sql  查询语句
     * @param args 参数数组
     * @return 返回查询后的数组
     */
    public List<T> listBean(String sql, Object... args) {
        try {
            return queryRunner.query(getDataSource().getConnection(), sql, (BeanListHandler<T>) createResultSetHandler(clazz, BeanListHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db listBean", e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace("db listBean", e);
            }
        }
        return null;
    }

    /**
     * 聚合查询<聚合值--总条数，求和....>
     *
     * @param sql  查询语句
     * @param args 查询值
     * @param <E>  返回聚合对象
     * @return count，sum...
     */
    protected <E> E getValue(String sql, Object... args) {
        try {
            return queryRunner.query(getDataSource().getConnection(), sql, new ScalarHandler<E>(), args);
        } catch (SQLException e) {
            stackTrace("db getValue", e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace("db getValue", e);
            }
        }
        return null;
    }

    /**
     * 聚合查询<聚合值--总条数，求和....>
     *
     * @param sql  查询语句
     * @param args 查询值
     * @param <E>  返回聚合对象
     * @return （count，sum...）数组
     */
    protected <E> List<E> listValue(String sql, Object... args) {
        try {
            return queryRunner.query(getDataSource().getConnection(), sql, new ColumnListHandler<E>(), args);
        } catch (SQLException e) {
            stackTrace("db listValue", e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace("db listValue", e);
            }
        }
        return null;
    }

    /**
     * simple查询
     *
     * @param sql  查询语句
     * @param args 查询值
     * @return map对象
     */
    protected Map<String, Object> getMap(String sql, Object... args) {
        try {
            return queryRunner.query(getDataSource().getConnection(), sql, (MapHandler) createResultSetHandler(clazz, MapHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db getMap", e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace("db getMap", e);
            }
        }
        return null;
    }

    /**
     * simple查询
     *
     * @param sql  查询语句
     * @param args 查询值
     * @return map对象数组
     */
    protected List<Map<String, Object>> listMap(String sql, Object... args) {
        try {
            return queryRunner.query(getDataSource().getConnection(), sql, (MapListHandler) createResultSetHandler(clazz, MapListHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db listMap", e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace("db listMap", e);
            }
        }
        return null;
    }

    //----------------------------------------查询扩展部分------------------------------------

    /**
     * 查询
     *
     * @param sql   查询语句
     * @param clazz 结果类
     * @param args  查询值
     * @param <E>
     * @return
     */
    @SuppressWarnings({"unchecked"})
    public <E> E getBeanByClass(String sql, Class<?> clazz, Object... args) {
        try {
            return queryRunner.query(getDataSource().getConnection(), sql, (BeanHandler<E>) createResultSetHandler(clazz, BeanHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db getBeanByClass", e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace("db getBeanByClass", e);
            }
        }
        return null;
    }

    /**
     * 查询多条
     *
     * @param sql   查询语句
     * @param clazz 结果类
     * @param args  查询值
     * @param <E>
     * @return
     */
    public <E> List<E> listBeanByClass(String sql, Class<?> clazz, Object... args) {
        try {
            return queryRunner.query(getDataSource().getConnection(), sql, (BeanListHandler<E>) createResultSetHandler(clazz, BeanListHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db listBeanByClass", e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace("db listBeanByClass", e);
            }
        }
        return null;
    }

    /**
     * 原生语句操作
     *
     * @param sql  操作语句
     * @param args 操作参数
     * @return
     */
    protected int execute(String sql, Object... args) {
        int i = -1;
        try {
            i = queryRunner.execute(getDataSource().getConnection(), sql, args);
        } catch (SQLException e) {
            stackTrace(String.format("db execute sql:{%s},params:{%s}", sql, paramSql(args)), e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace(String.format("db execute discardConnectionFromDao sql:{%s},params:{%s}", sql, paramSql(args)), e);
            }
        }
        return i;
    }


    private String paramSql(Object[] params) {
        if (Objects.isNull(params)) {
            return "";
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (int j = 0; j < params.length; ++j) {
            stringBuffer.append("'").append(params[j]).append("', ");
        }
        stringBuffer.deleteCharAt(stringBuffer.length() - 2);
        stringBuffer.insert(0, "(");
        stringBuffer.append(")");
        return stringBuffer.toString();
    }

    /**
     * 批量更新数据
     *
     * @param sql    操作语句
     * @param params 二维参数
     * @return
     */
    public int[] batch(String sql, Object[][] params) {
        int[] i = new int[params.length];
        try {
            i = queryRunner.batch(getDataSource().getConnection(), sql, params);
        } catch (SQLException e) {
            stackTrace(String.format("db batch sql:{%s},params:{%s}", sql, batch(params)), e);
        } finally {
            try {
                getDataSource().discardConnectionFromDao();
            } catch (SQLException e) {
                stackTrace(String.format("db batch discardConnectionFromDao sql:{%s},params:{%s}", sql, batch(params)), e);
            }
        }
        return i;
    }


    private String batch(Object[][] params) {
        if (Objects.isNull(params)) {
            return "";
        }
        StringBuffer paramSql = new StringBuffer();
        for (int i = 0; i < params.length; ++i) {
            Object[] param = params[i];
            StringBuffer stringBuffer = new StringBuffer();
            for (int j = 0; j < param.length; ++j) {
                stringBuffer.append("'").append(param[j]).append("', ");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 2);
            stringBuffer.insert(0, "(");
            stringBuffer.append("), ");
            paramSql.append(stringBuffer.toString());
        }
        paramSql.deleteCharAt(paramSql.length() - 2);
        return paramSql.toString();
    }


    /**
     * 堆栈输出
     *
     * @param mainError 主错误输出
     * @param e         异常
     */
    public static void stackTrace(String mainError, Exception e) {
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(e.getStackTrace()).forEach(m -> {
            stringBuilder.append("(" + m.getClassName() + "---->" + m.getFileName() + "---->" + m.getMethodName() + "--->" + m.getLineNumber() + ")");
        });
        CommLogD.error(mainError + ":{}        {}", e.getMessage(), stringBuilder.toString());
    }

    /**
     * 跟踪日志
     */
    public static void stackTrace() {
        StringBuilder stringBuilder = new StringBuilder();
        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                stringBuilder.append("(" + stackElements[i].getClassName() + "---->" + stackElements[i].getFileName() + "--->" + stackElements[i].getLineNumber() + "--->" + stackElements[i].getMethodName() + ")" + "\n");
            }
        }
        CommLogD.error(stringBuilder.toString());
    }

    /**
     * 跟踪endSet日志
     */
    public static String stackTrace1() {
        StringBuilder stringBuilder = new StringBuilder();
        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements != null) {
            int l = 8;
            for (int i = 0; i < stackElements.length; i++) {
                l--;
                stringBuilder.append("(" + stackElements[i].getFileName() + "---" + stackElements[i].getLineNumber() + ")");
                if (l == 0) {
                    break;
                }
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 输出所有线程信息
     */
    public static void systemOutAllThreadInfo() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup topGroup = group;
        while (group != null) {
            topGroup = group;
            group = group.getParent();
        }
        int estimatedSize = topGroup.activeCount() * 2;
        Thread[] slackList = new Thread[estimatedSize];
        int actualSize = topGroup.enumerate(slackList);
        Thread[] list = new Thread[actualSize];
        System.arraycopy(slackList, 0, list, 0, actualSize);
        CommLogD.error("Thread list size == " + list.length);
        for (Thread thread : list) {
            stackTrace(thread.getStackTrace(), thread);
        }
    }

    /**
     * 跟踪日志
     */
    public static void stackTrace(StackTraceElement[] stackElements, Thread thread) {
        StringBuilder stringBuilder = new StringBuilder();
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                stringBuilder.append("(" + stackElements[i].getClassName() + "---->" + stackElements[i].getFileName() + "--->" + stackElements[i].getLineNumber() + "--->" + stackElements[i].getMethodName() + ")" + "\n");
            }
        }
        CommLogD.error("\n-------------------------\n线程名(" + thread.getName() + ") \n线程状态(" + thread.getState() + ")\n" + stringBuilder.toString() + "-------------------------\n");
    }

    /**
     * 跟踪日志
     */
    public static void testSys(String testStr) {
        CommLogD.debug(testStr);
    }

    public abstract DataBaseMgr getDataSource();
}
