package core.db.other;

import BaseTask.AsynTask.AsyncCallBackTaskBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 异步容器
 */
@Getter
@Setter
@AllArgsConstructor
public class AsyncInfo {
    private AsyncCallBackTaskBase asyncCallBackTaskBase;//回调
    private long asynTaskTag;
    private boolean isASync;

    public AsyncInfo(long asynTaskTag) {
        this.asynTaskTag = asynTaskTag;
        this.isASync = true;
    }
}
