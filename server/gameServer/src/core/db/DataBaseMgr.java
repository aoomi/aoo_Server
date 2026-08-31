package core.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 数据源管理器
 */
public class DataBaseMgr {
    private DruidDataSource dataSource; //数据源
    private ThreadLocal<DataBaseMgr.ConnInfo> local; //本地数据源管理
    private ReentrantLock lock = new ReentrantLock();

    private DataBaseMgr() {
        this.local = new ThreadLocal<DataBaseMgr.ConnInfo>() {
            @Override
            protected DataBaseMgr.ConnInfo initialValue() {
                return new DataBaseMgr.ConnInfo();
            }
        };
    }

    /**
     * Db配置初始化
     *
     * @param name   源标识
     * @param config 配置
     * @throws Exception
     */
    public static void init(String name, Map<String, String> config) throws Exception {
        DataSourceMgr.init(name, config);
    }

    /**
     * 获取源管理器
     *
     * @param name
     * @return
     */
    public static DataBaseMgr get(String name) {
        return DataSourceMgr.get(name);
    }

    /**
     * 获取连接
     *
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        Connection conn = this.local.get().getConn();
        if (conn == null || conn.isClosed()) {
            conn = this.dataSource.getConnection();
            if (this.local.get().isTransaction()) {
                conn.setAutoCommit(false);
            }

            this.local.get().setConn(conn);
        }

        return conn;
    }

    public synchronized Lock getLock() {
        return lock;
    }

    /** Exposes the managed pool to modern transactional repositories. */
    public javax.sql.DataSource dataSource() {
        return dataSource;
    }

    /**
     * 断开连接
     *
     * @throws SQLException
     */
    private void discardConnection() throws SQLException {
        Connection conn = this.local.get().getConn();
        if (conn != null && !conn.isClosed()) {
            conn.setAutoCommit(true);
            // A pooled JDBC close returns the connection to Druid. discardConnection()
            // physically destroys it and caused a create/destroy storm under startup load.
            conn.close();
        }

        this.local.get().dispose();
        this.local.remove();
    }

    /**
     * dao断开连接
     *
     * @throws SQLException
     */
    public void discardConnectionFromDao() throws SQLException {
        if (!this.local.get().isServiceDiscard()) {
            this.discardConnection();
        }

    }

    /**
     * service断开连接
     *
     * @throws SQLException
     */
    public void discardConnectionFromService() throws SQLException {
        this.discardConnection();
    }

    /**
     * 开启事务
     */
    public void openTransaction() {
        this.local.get().setTransaction(true);
    }

    /**
     * service开启占用连接
     */
    public void setDiscardConnectionLevelForService() {
        this.local.get().setServiceDiscard(true);
    }

    private static class ConnInfo {
        private boolean serviceDiscard; //service是否占用连接
        private Connection conn; //连接
        private boolean transaction; //是否事务自动提交

        public ConnInfo() {
            this.dispose();
        }

        public boolean isServiceDiscard() {
            return this.serviceDiscard;
        }

        public void setServiceDiscard(boolean serviceDiscard) {
            this.serviceDiscard = serviceDiscard;
        }

        public Connection getConn() {
            return this.conn;
        }

        public void setConn(Connection conn) {
            this.conn = conn;
        }

        public boolean isTransaction() {
            return this.transaction;
        }

        public void setTransaction(boolean transaction) {
            this.transaction = transaction;
        }

        public void dispose() {
            this.serviceDiscard = false;
            this.conn = null;
            this.transaction = false;
        }
    }

    /**
     * 多源管理
     */
    private static class DataSourceMgr {
        public static final Map<String, DataBaseMgr> dataSourceMap = new HashMap<>();

        private DataSourceMgr() {
        }

        protected static DataBaseMgr get(String name) {
            return dataSourceMap == null ? null : dataSourceMap.get(name);
        }

        /**
         * 数据源初始化
         *
         * @param name   源标识
         * @param config 配置
         * @throws Exception
         */
        protected static void init(String name, Map<String, String> config) throws Exception {
            DruidDataSource dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(config);
            if (dataSource == null) {
                throw new Exception();
            }
            DataBaseMgr dataMgr = new DataBaseMgr();
            dataMgr.dataSource = dataSource;
            dataSourceMap.put(name, dataMgr);
        }
    }
}
