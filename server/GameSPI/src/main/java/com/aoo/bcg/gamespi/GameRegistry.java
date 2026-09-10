package com.aoo.bcg.gamespi;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GameRegistry {
    private final Map<Integer, Map<String,GameProvider>> byId = new LinkedHashMap<>();
    private final Map<String, Map<String,GameProvider>> byCode = new LinkedHashMap<>();

    public synchronized void register(GameProvider provider) {
        GameDescriptor descriptor = provider.descriptor();
        String normalizedCode = descriptor.code().toLowerCase();
        if (byId.getOrDefault(descriptor.gameId(),Map.of()).containsKey(descriptor.version())
                || byCode.getOrDefault(normalizedCode,Map.of()).containsKey(descriptor.version())) {
            throw new IllegalStateException("duplicate game registration: " + descriptor.gameId() + "/" + descriptor.code());
        }
        byId.computeIfAbsent(descriptor.gameId(),ignored->new LinkedHashMap<>()).put(descriptor.version(),provider);
        byCode.computeIfAbsent(normalizedCode,ignored->new LinkedHashMap<>()).put(descriptor.version(),provider);
    }

    public synchronized GameProvider require(int gameId) {
        Map<String,GameProvider> versions=byId.get(gameId);
        if (versions == null || versions.isEmpty()) throw new IllegalArgumentException("unknown gameId: " + gameId);
        if(versions.size()!=1)throw new IllegalStateException("playVersion required for multi-version gameId: "+gameId);
        return versions.values().iterator().next();
    }

    public synchronized GameProvider require(int gameId,String playVersion) {
        if(playVersion==null||playVersion.isBlank())throw new IllegalArgumentException("playVersion is required");
        GameProvider provider=byId.getOrDefault(gameId,Map.of()).get(playVersion);
        if(provider==null)throw new IllegalArgumentException("unknown gameId/playVersion: "+gameId+"/"+playVersion);
        return provider;
    }

    public synchronized GameProvider require(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("game code must not be blank");
        Map<String,GameProvider> versions=byCode.get(code.trim().toLowerCase());
        if(versions==null||versions.isEmpty())throw new IllegalArgumentException("unknown game code: "+code);
        if(versions.size()!=1)throw new IllegalStateException("playVersion required for multi-version game code: "+code);
        return versions.values().iterator().next();
    }

    public synchronized Collection<GameDescriptor> descriptors() {
        return byId.values().stream().flatMap(value->value.values().stream()).map(GameProvider::descriptor).toList();
    }
}
