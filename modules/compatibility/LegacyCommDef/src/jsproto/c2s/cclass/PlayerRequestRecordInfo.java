package jsproto.c2s.cclass;

import lombok.Data;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 玩家请求记录的信息
 */
@Data
public class PlayerRequestRecordInfo {
    /**
     * 分钟内出现超高频率请求操作
     */
    private final AtomicInteger minuteUltrahighFrequencyCount = new AtomicInteger();
    /**
     * 分钟时间
     */
    private volatile long minuteTime;

    /**
     * 记录请求的接口和次数
     */
    Map<String, PlayerRequestItem> requestRecorMap = new ConcurrentHashMap<>();

    /**
     * 接口出现延迟的次数
     */
    private final AtomicInteger overtimeCount = new AtomicInteger();

    /**
     * 接口出现延迟时间
     */
    private volatile long overtime = 0;
    /**
     * 心跳接口
     */
    public final static String CHeartBeatHandler = "heartbeat.cheartbeathandler";

    /**
     * 1秒内请求超时的接口出现 10次则提出用户
     *
     * @param requestNowTime
     * @return
     */
    public synchronized boolean existOvertimeInterface(long requestNowTime) {
        if (this.overtime <= 0) {
            this.overtime = requestNowTime;
        }
        if (requestNowTime - this.overtime >= 1000) {
            this.overtimeCount.set(1);
            this.overtime = requestNowTime;
            return false;
        } else {
            return this.overtimeCount.incrementAndGet() >= 10;
        }
    }

    /**
     * 检查并增加记录时间
     *
     * @param handler        请求接口头部
     * @param requestNowTime 当前请求时间
     * @return
     */
    public synchronized boolean checkAndAddRequestConcurrentRecor(String handler, long requestNowTime) {
        if (!this.isOpen()) {
            // 没开启
            return true;
        }
        if (CHeartBeatHandler.equals(handler)) {
            // 如果是心跳就不限制
            return true;
        } else {
            if (this.minuteTime <= 0L) {
                this.minuteTime = requestNowTime;
            }
            this.minuteUltrahighFrequencyCount.incrementAndGet();
            PlayerRequestItem requestItem = this.requestRecorMap.get(handler);
            if (Objects.isNull(requestItem)) {
                this.requestRecorMap.put(handler, new PlayerRequestItem(requestNowTime, 1));
                return true;
            }
            if (requestNowTime - requestItem.getRequestTime() >= 200) {
                this.requestRecorMap.remove(handler);
                return true;
            }
            return requestItem.addRequestValue() <= 2;
        }

    }

    /**
     * 存在超过并发次数限制
     *
     * @return
     */
    public boolean existExceededConcurrencyLimit(String handler) {
        PlayerRequestItem requestItem = this.requestRecorMap.get(handler);
        return Objects.nonNull(requestItem) && requestItem.getRequestValue() >= 10;
    }

    /**
     * 分钟内出现超高频率请求操作
     *
     * @return
     */
    public boolean existMinuteUltrahighFrequencyLimit() {
        return this.minuteUltrahighFrequencyCount.get() >= 600;
    }

    /**
     * 分钟内出现超高频率请求次数
     *
     * @return
     */
    public synchronized int getMinuteUltrahighFrequencyCount(long requestNowTime) {
        int tempCount = this.minuteUltrahighFrequencyCount.get();
        if (requestNowTime - this.minuteTime >= 60000) {
            this.minuteTime = requestNowTime;
            this.minuteUltrahighFrequencyCount.set(1);
            return tempCount >= 200 ? tempCount : 0;
        }
        return 0;
    }

    /**
     * 清空
     */
    public synchronized void clear() {
        this.requestRecorMap.clear();
        this.overtime = 0;
        this.overtimeCount.set(0);
        this.minuteUltrahighFrequencyCount.set(0);
        this.minuteTime = 0L;
    }


    /**
     * 是否打开
     *
     * @return
     */
    public boolean isOpen() {
        return true;
    }

    public int getMinuteUltrahighFrequencyCount() {
        return minuteUltrahighFrequencyCount.get();
    }

    public void setMinuteUltrahighFrequencyCount(int count) {
        minuteUltrahighFrequencyCount.set(Math.max(0, count));
    }

    public int getOvertimeCount() {
        return overtimeCount.get();
    }

    public void setOvertimeCount(int count) {
        overtimeCount.set(Math.max(0, count));
    }


}
