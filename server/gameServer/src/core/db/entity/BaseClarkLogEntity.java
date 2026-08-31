package core.db.entity;

import cenum.DispatcherComponentLogEnum;
import lombok.NoArgsConstructor;

import java.sql.SQLException;

/**
 * 日志基础实体
 *
 * @param <T>
 */
@NoArgsConstructor
public abstract class BaseClarkLogEntity<T> extends BaseEntity<T> {

    /**
     * 获取插入头部语句
     *
     * @return
     */
    public abstract String getInsertSql();

    /**
     * 添加参数
     *
     * @throws SQLException
     */
    public abstract Object[] addToBatch();


    /**
     * 进程Id
     *
     * @return
     */
    public int threadId() {
        return DispatcherComponentLogEnum.BATCH_BD_LOG.id();
    }

    /**
     * 环大小
     *
     * @return
     */
    public int bufferSize() {
        return DispatcherComponentLogEnum.BATCH_BD_LOG.bufferSize();
    }

    /**
     * 结束或批量
     *
     * @return
     */
    public boolean endOfBatch() {
        return false;
    }
}
