package com.aoo.bcg.gateway;
public interface ConnectionGenerationStore { long next(String userId, String roomId, int seatId); }
