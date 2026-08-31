package com.aoo.bcg.config;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryGameConfigurationRepository implements GameConfigurationRepository {
    private final ConcurrentHashMap<Key, PublishedGameConfiguration<?>> configurations = new ConcurrentHashMap<>();
    public void publish(PublishedGameConfiguration<?> configuration) {
        Key key = new Key(configuration.manifest().gameId(), configuration.manifest().playVersion());
        if (configurations.putIfAbsent(key, configuration) != null) throw new IllegalStateException("published version is immutable: " + key);
    }
    @Override @SuppressWarnings("unchecked")
    public <C> Optional<PublishedGameConfiguration<C>> find(int gameId, String playVersion) {
        return Optional.ofNullable((PublishedGameConfiguration<C>) configurations.get(new Key(gameId, playVersion)));
    }
    private record Key(int gameId, String playVersion) {}
}
