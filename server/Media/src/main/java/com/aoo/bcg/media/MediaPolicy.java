package com.aoo.bcg.media;

import java.util.Map;
import java.util.Set;

public final class MediaPolicy {
    public enum Kind { AVATAR, VOICE, EVIDENCE }
    public record Rule(long maxBytes,long maxDurationMillis,int chunkBytes,Set<String> mimeTypes) {}
    private final Map<Kind,Rule> rules=Map.of(
            Kind.AVATAR,new Rule(5L*1024*1024,0,1024*1024,Set.of("image/jpeg","image/png","image/webp")),
            Kind.VOICE,new Rule(20L*1024*1024,5*60_000,1024*1024,Set.of("audio/aac","audio/mpeg","audio/ogg","audio/wav","audio/webm")),
            Kind.EVIDENCE,new Rule(50L*1024*1024,0,5*1024*1024,Set.of("image/jpeg","image/png","image/webp","audio/aac","audio/mpeg","audio/ogg","video/mp4","application/pdf","application/octet-stream")));
    public Rule validate(Kind kind,String mime,long bytes,long durationMillis,String sha256){
        Rule rule=rules.get(kind);if(rule==null)throw new IllegalArgumentException("unsupported media kind");
        if(mime==null||!rule.mimeTypes().contains(mime))throw new IllegalArgumentException("content type rejected for "+kind);
        if(bytes<=0||bytes>rule.maxBytes())throw new IllegalArgumentException("media size rejected for "+kind);
        if(kind==Kind.VOICE&&(durationMillis<=0||durationMillis>rule.maxDurationMillis()))throw new IllegalArgumentException("voice duration rejected");
        if(kind!=Kind.VOICE&&durationMillis!=0)throw new IllegalArgumentException("duration is only valid for voice");
        if(sha256==null||!sha256.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("lowercase SHA-256 required");return rule;
    }
}
