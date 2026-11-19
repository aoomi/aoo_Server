package com.aoo.bcg.common.config;

import java.time.Instant;
import java.util.Map;
import com.aoo.bcg.gamespi.RulePayload;

public record RoomRuleSnapshot(long configId, int gameId, String playVersion, String componentVersion,
                               String roomProfileVersion, String flowVersion, String scoreVersion,
                               String uiProfileVersion, Instant publishedAt, RulePayload immutableRules) {
    public RoomRuleSnapshot {
        if (configId <= 0 || gameId <= 0 || playVersion == null || playVersion.isBlank()) throw new IllegalArgumentException("invalid rule snapshot");
        if (componentVersion == null || componentVersion.isBlank() || publishedAt == null) throw new IllegalArgumentException("incomplete rule snapshot");
        if (roomProfileVersion == null || roomProfileVersion.isBlank()
                || flowVersion == null || flowVersion.isBlank()
                || scoreVersion == null || scoreVersion.isBlank()
                || uiProfileVersion == null || uiProfileVersion.isBlank()) {
            throw new IllegalArgumentException("all room snapshot versions are required");
        }
        immutableRules = immutableRules == null ? RulePayload.empty() : immutableRules;
    }
    public RoomRuleSnapshot(long configId,int gameId,String playVersion,String componentVersion,
                            String roomProfileVersion,String flowVersion,String scoreVersion,
                            String uiProfileVersion,Instant publishedAt,Map<String,?> immutableRules) {
        this(configId,gameId,playVersion,componentVersion,roomProfileVersion,flowVersion,scoreVersion,
                uiProfileVersion,publishedAt,RulePayload.copyOf(immutableRules));
    }
}
