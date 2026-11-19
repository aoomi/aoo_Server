package server.aoo.dao.config.util;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.*;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * druid数据库操作工具封装
 *
 * @author zhujianming
 * @date 2021-07-08 13:51
 */
public abstract class BaseDaoImpl<T> extends DBUtil<T> {

    private QueryRunner queryRunner;

    /**
     * 自动生成操作的实体类
     */
    public BaseDaoImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        queryRunner = new QueryRunner();
    }

    /**
     * 连接
     *
     * @return {@link java.sql.Connection}
     */
    public java.sql.Connection connection(){
        return getEntityManager().unwrap(org.hibernate.Session.class).doReturningWork(connection -> connection);
    }

    /**
     * 插入
     *
     * @param sql  插入语句
     * @param args 插入值
     * @return
     */
    public long insertAndGetGeneratedKeys(String sql, Object... args) {
        java.sql.Connection connection = connection();
        try {
            Long id = queryRunner.insert(connection, sql, new ScalarHandler<>(), args);
            return id != null ? id : 0;
        } catch (Exception e) {
            stackTrace("db insertAndGetGeneratedKeys", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db insertAndGetGeneratedKeys", e);
//            }
        }
        return -1;
    }

    /**
     * 更新
     *
     * @param sql  更新语句
     * @param args 更新值
     * @return
     */
    protected int update(String sql, Object... args) {
        java.sql.Connection connection = connection();
        int i = -1;
        try {
            i = queryRunner.update(connection, sql, args);
        } catch (SQLException e) {
            stackTrace("db update", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db update", e);
//            }
        }
        return i;
    }


    /**
     * 原生语句操作
     *
     * @param sql  操作语句
     * @param args 操作参数
     * @return
     */
    protected int execute(String sql, Object... args) {
        java.sql.Connection connection = connection();
        int i = -1;
        try {
            i = queryRunner.execute(connection, sql, args);
        } catch (SQLException e) {
            stackTrace("db execute", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db execute", e);
//            }
        }
        return i;
    }

    /**
     * 批量更新数据
     *
     * @param sql    操作语句
     * @param params 二维参数
     * @return
     */
    public int[] batch(String sql, Object[][] params) {
        java.sql.Connection connection = connection();
        int[] i = new int[params.length];
        try {
            i = queryRunner.batch(connection, sql, params);
        } catch (SQLException e) {
            stackTrace("db batch", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db batch", e);
//            }
        }
        return i;
    }

    /**
     * 聚合查询<聚合值--总条数，求和....>
     *
     * @param sql  查询语句
     * @param args 查询值
     * @param <E>  返回聚合对象
     * @return （count，sum...）数组
     */
    public <E> List<E> listValue(String sql, Object... args) {
        java.sql.Connection connection = connection();
        try {
            return queryRunner.query(connection, sql, new ColumnListHandler<E>(), args);
        } catch (SQLException e) {
            stackTrace("db listValue", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db listValue", e);
//            }
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
    public Map<String, Object> getMap(String sql, Object... args) {
        java.sql.Connection connection = connection();
        try {
            return queryRunner.query(connection, sql, (MapHandler) createResultSetHandler(getDomainClass(), MapHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db getMap", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db getMap", e);
//            }
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
    public List<Map<String, Object>> listMap(String sql, Object... args) {
        java.sql.Connection connection = connection();
        try {
            return queryRunner.query(connection, sql, (MapListHandler) createResultSetHandler(getDomainClass(), MapListHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db listMap", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db listMap", e);
//            }
        }
        return null;
    }


    /**
     * 查询
     *
     * @param sql  查询语句
     * @param args 查询值
     * @return
     */
    public T getBean(String sql, Object... args) {
        java.sql.Connection connection = connection();
        try {
            return queryRunner.query(connection, sql, (BeanHandler<T>) createResultSetHandler(getDomainClass(), BeanHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db getBean", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db getBean", e);
//            }
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
        java.sql.Connection connection = connection();
        try {
            return queryRunner.query(connection, sql, (BeanListHandler<E>) createResultSetHandler(clazz, BeanListHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db listBeanByClass", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db listBeanByClass", e);
//            }
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
        java.sql.Connection connection = connection();
        try {
            return queryRunner.query(connection, sql, (BeanListHandler<T>) createResultSetHandler(getDomainClass(), BeanListHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db listBean", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db listBean", e);
//            }
        }
        return null;
    }

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
        java.sql.Connection connection = connection();
        try {
            return queryRunner.query(connection, sql, (BeanHandler<E>) createResultSetHandler(clazz, BeanHandler.class), args);
        } catch (SQLException e) {
            stackTrace("db getBeanByClass", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db getBeanByClass", e);
//            }
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
    public <E> E getValue(String sql, Object... args) {
        java.sql.Connection connection = connection();
        try {
            return queryRunner.query(connection, sql, new ScalarHandler<E>(), args);
        } catch (SQLException e) {
            stackTrace("db getValue", e);
        } finally {
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                stackTrace("db getValue", e);
//            }
        }
        return null;
    }

    /**
     * 得到表名
     *
     * @return {@link String}
     */
    public String getTableName(){
        EntityManagerFactory entityManagerFactory = getEntityManager().getEntityManagerFactory();
        SessionFactoryImpl sessionFactory = (SessionFactoryImpl)entityManagerFactory.unwrap(SessionFactory.class);
        EntityPersister entity = sessionFactory.getMappingMetamodel().getEntityDescriptor(getDomainClass().getName());
        return entity!=null?((SingleTableEntityPersister) entity).getTableName():"";
    }

}
