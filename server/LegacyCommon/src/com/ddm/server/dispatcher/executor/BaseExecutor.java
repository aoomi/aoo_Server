//-------------------------------------------------
// Litchi Game Server Framework
// Copyright(c) 2019 phantaci <phantacix@qq.com>
// MIT Licensed
//-------------------------------------------------
package com.ddm.server.dispatcher.executor;


/**
 * dispatch to target thread executor
 *
 * @author 0x737263
 */
public interface BaseExecutor {
    /**
     * 执行进程
     */
    void invoke();

    /**
     * 进程Id
     *
     * @return
     */
    int threadId();

    /**
     * 环大小
     *
     * @return
     */
    default int bufferSize() {
        return 1024;
    }

    ;

}
