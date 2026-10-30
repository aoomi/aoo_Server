package com.aoo.bcg.wordcard;

import java.util.Collection;
import java.util.Comparator;

/** Deterministic Pao-Hu-Zi response precedence. */
public final class WordCardResponseResolver {
    public record Response(int seatId,WordCardOperation operation){public Response{if(seatId<0||operation==null)throw new IllegalArgumentException("invalid response");}}
    private WordCardResponseResolver(){}
    public static Response resolve(Collection<Response> responses,int discarder,int seats){
        if(responses==null||responses.isEmpty()||discarder<0||seats<2)throw new IllegalArgumentException("invalid response window");
        return responses.stream().filter(r->r.operation()!=WordCardOperation.PASS)
                .min(Comparator.comparingInt((Response r)->priority(r.operation())).reversed().thenComparingInt(r->distance(discarder,r.seatId(),seats)))
                .orElseThrow(()->new IllegalStateException("all players passed"));
    }
    private static int priority(WordCardOperation op){return switch(op){case HU->100;case TI->90;case PAO->85;case WEI->80;case PENG->70;case CHI->60;default->0;};}
    private static int distance(int from,int to,int count){int value=(to-from)%count;return value<=0?value+count:value;}
}
