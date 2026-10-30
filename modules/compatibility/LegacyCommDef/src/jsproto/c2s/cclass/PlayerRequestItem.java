package jsproto.c2s.cclass;

import lombok.Data;

@Data
public class PlayerRequestItem {
    /**
     * 接口请求时间
     */
    private long requestTime;

    /**
     * 请求次数
     */
    private int requestValue;

    public PlayerRequestItem(long requestTime, int requestValue) {
        this.requestTime = requestTime;
        this.requestValue = requestValue;
    }

    public int addRequestValue() {
        this.requestValue++;
        return this.requestValue;
    }

    @Override
    public String toString() {
        return "PlayerRequestItem{" +
                "requestTime=" + requestTime +
                ", requestValue=" + requestValue +
                '}';
    }
}
