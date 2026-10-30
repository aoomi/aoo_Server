package com.aoo.bcg.longcard;

import java.util.Collection;
import java.util.Comparator;

/** Resolves simultaneous responses deterministically: operation priority, then turn distance. */
public final class LongCardResponseResolver {
    public record Response(int seatId, LongCardOperation operation) {
        public Response { if (seatId < 0 || operation == null) throw new IllegalArgumentException("invalid response"); }
    }
    private LongCardResponseResolver() {}
    public static Response resolve(Collection<Response> responses, int discarderSeat, int seatCount) {
        if (responses == null || responses.isEmpty() || discarderSeat < 0 || seatCount < 2) throw new IllegalArgumentException("invalid response window");
        return responses.stream().filter(r -> r.operation()!=LongCardOperation.PASS)
                .min(Comparator.comparingInt((Response r)->priority(r.operation())).reversed()
                        .thenComparingInt(r->distance(discarderSeat,r.seatId(),seatCount)))
                .orElseThrow(()->new IllegalStateException("all players passed"));
    }
    private static int priority(LongCardOperation operation){return switch(operation){case HU->100;case PENG->80;case CHI->60;case STEAL->50;case CALL->40;default->0;};}
    private static int distance(int from,int to,int count){int value=(to-from)%count;return value<=0?value+count:value;}
}
