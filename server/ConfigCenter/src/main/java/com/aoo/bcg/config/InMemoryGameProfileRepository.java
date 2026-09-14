package com.aoo.bcg.config;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe development repository. Production must provide the same contract
 * with database uniqueness on (game_id, version) and transactional activation.
 */
public final class InMemoryGameProfileRepository implements GameProfilePublicationService.ProfileRepository {
    private final Map<ProfileKey, PublishedGameProfile> versions = new ConcurrentHashMap<>();
    private final Map<Long, String> activeVersions = new ConcurrentHashMap<>();
    private final List<ProfileAuditEntry> auditLog = new CopyOnWriteArrayList<>();
    private final Clock clock;

    public InMemoryGameProfileRepository(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void insertImmutable(PublishedGameProfile profile, long operatorId, String reason) {
        Objects.requireNonNull(profile, "profile");
        ProfileKey key = new ProfileKey(profile.gameId(), profile.version());
        if (versions.putIfAbsent(key, profile) != null) {
            throw new IllegalStateException("published profile version already exists");
        }
        auditLog.add(new ProfileAuditEntry(profile.gameId(), profile.version(),
                ProfileAuditEntry.Action.INSERT_VERSION, operatorId, reason, clock.instant()));
    }

    @Override
    public synchronized void activate(long gameId, String version, long operatorId, String reason) {
        if (!versions.containsKey(new ProfileKey(gameId, version))) {
            throw new IllegalArgumentException("profile version not found");
        }
        activeVersions.put(gameId, version);
        auditLog.add(new ProfileAuditEntry(gameId, version,
                ProfileAuditEntry.Action.ACTIVATE_VERSION, operatorId, reason, clock.instant()));
    }

    @Override
    public Optional<PublishedGameProfile> find(long gameId, String version) {
        return Optional.ofNullable(versions.get(new ProfileKey(gameId, version)));
    }

    @Override
    public List<PublishedGameProfile> active() {
        List<PublishedGameProfile> result = new ArrayList<>();
        activeVersions.forEach((gameId, version) -> find(gameId, version).ifPresent(result::add));
        result.sort(Comparator.comparingLong(PublishedGameProfile::gameId));
        return List.copyOf(result);
    }

    public List<ProfileAuditEntry> auditLog() {
        return List.copyOf(auditLog);
    }

    private record ProfileKey(long gameId, String version) {
        private ProfileKey {
            if (gameId <= 0 || version == null || version.isBlank()) {
                throw new IllegalArgumentException("invalid profile key");
            }
        }
    }
}
