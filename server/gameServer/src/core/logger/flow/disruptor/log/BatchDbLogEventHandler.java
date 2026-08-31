package core.logger.flow.disruptor.log;

import BaseCommon.CommLog;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lmax.disruptor.EventHandler;
import core.db.entity.BaseClarkLogEntity;
import core.db.other.DBFlowMgr;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BatchDbLogEventHandler implements EventHandler<BatchDbLogBuffer> {
    private final static int DB_BATCH_SIZE = 50;
    private final static int RING_BATCH_SIZE = 1024;
    private Map<String, List<BaseClarkLogEntity>> cache = Maps.newHashMap();

    @Override
    public void onEvent(BatchDbLogBuffer batchDbLogBuffer, long sequence, boolean endOfBatch) throws Exception {
        try {
            saveMetricData(batchDbLogBuffer, sequence, endOfBatch);
        } catch (Exception e) {
            CommLog.error("Exception ", e);
        }
    }


    private void saveMetricData(BatchDbLogBuffer value, long sequence, boolean endOfBatch) {
        addLogFlowMap(value.getExecutor());
        if ((sequence + 1) % DB_BATCH_SIZE == 0) {
            DBFlowMgr.getInstance().execBatchDbLog(getCache());
            getCache().clear();
        }
        if (endOfBatch) {
            if ((sequence + 1) % RING_BATCH_SIZE != 0) {
                DBFlowMgr.getInstance().execBatchDbLog(getCache());
                getCache().clear();
            }
        }
    }

    private void addLogFlowMap(BaseClarkLogEntity entity) {
        if (getCache().containsKey(entity.getTableName())) {
            getCache().get(entity.getTableName()).add(entity);
        } else {
            getCache().put(entity.getTableName(), Lists.newArrayList(entity));
        }
    }

}
