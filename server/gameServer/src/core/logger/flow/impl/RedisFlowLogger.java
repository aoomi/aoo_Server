package core.logger.flow.impl;

import core.logger.flow.GameFlowLogger;

public class RedisFlowLogger extends GameFlowLogger {

    @Override
    public boolean isOpen() {
        String ADDR = System.getProperty("Redis.ADDR");
        return ADDR != null && !ADDR.trim().isEmpty();
    }
}