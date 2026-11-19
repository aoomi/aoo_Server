package com.ddm.server.protocol.v2;

import com.ddm.server.common.redis.RedisUtil;
import com.google.gson.Gson;

public final class RedisProtocolIdempotency {
    private static final Gson GSON = new Gson();
    private static final int TTL_SECONDS = 24 * 60 * 60;
    private static final String PROCESSING = "__PROCESSING__";

    private RedisProtocolIdempotency() {}

    public static ProtocolEnvelope previous(long accountId,String msgId,String roomId,int roundNo,String requestId) {
        if (accountId <= 0L || requestId == null) return null;
        String value = RedisUtil.get(key(accountId,msgId,roomId,roundNo,requestId));
        return value == null || value.isBlank() || PROCESSING.equals(value)
                ? null : GSON.fromJson(value, ProtocolEnvelope.class);
    }

    public static boolean acquire(long accountId,String msgId,String roomId,int roundNo,String requestId) {
        if (accountId <= 0L || requestId == null || requestId.isBlank()) return false;
        return RedisUtil.setNxEx(key(accountId,msgId,roomId,roundNo,requestId),PROCESSING,30);
    }

    public static void complete(long accountId,String msgId,String roomId,int roundNo,String requestId,ProtocolEnvelope response) {
        if (accountId <= 0L || requestId == null || response == null) return;
        RedisUtil.setex(key(accountId,msgId,roomId,roundNo,requestId),TTL_SECONDS,GSON.toJson(response));
    }

    public static void release(long accountId,String msgId,String roomId,int roundNo,String requestId) {
        if (accountId <= 0L || requestId == null) return;
        RedisUtil.del(key(accountId,msgId,roomId,roundNo,requestId));
    }

    private static String key(long accountId,String msgId,String roomId,int roundNo,String requestId) {
        return "protocol:idempotency:"+accountId+":"+msgId+":"+roomId+":"+roundNo+":"+requestId;
    }
}
