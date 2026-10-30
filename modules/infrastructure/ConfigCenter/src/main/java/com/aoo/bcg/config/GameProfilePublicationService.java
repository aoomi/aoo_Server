package com.aoo.bcg.config;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class GameProfilePublicationService {
    private final ProfileRepository repository;
    private final RegionalGameCatalog catalog;
    private final Clock clock;

    public GameProfilePublicationService(ProfileRepository repository, RegionalGameCatalog catalog, Clock clock) {
        this.repository = Objects.requireNonNull(repository); this.catalog = Objects.requireNonNull(catalog); this.clock = Objects.requireNonNull(clock);
    }

    public PublishedGameProfile publish(PublishedGameProfile draft, long operatorId, String reason) {
        return publish(draft, operatorId, reason, null);
    }

    public PublishedGameProfile publish(PublishedGameProfile draft, long operatorId, String reason, String requestId) {
        requireReason(operatorId, reason);
        if (draft.publishedAt().isAfter(clock.instant())) throw new IllegalArgumentException("publishedAt cannot be in the future");
        repository.publishAtomic(draft, operatorId, reason, requestId);
        return draft;
    }

    public PublishedGameProfile rollback(long gameId, String previousVersion, long operatorId, String reason) {
        return rollback(gameId, previousVersion, operatorId, reason, null);
    }

    public PublishedGameProfile rollback(long gameId, String previousVersion, long operatorId, String reason, String requestId) {
        requireReason(operatorId, reason);
        PublishedGameProfile previous = repository.find(gameId, previousVersion)
                .orElseThrow(() -> new IllegalArgumentException("rollback version not found"));
        repository.activateAtomic(gameId, previousVersion, operatorId, reason, requestId);
        return previous;
    }

    public List<PublishedGameProfile> catalog(String provinceCode, String cityCode) {
        return catalog.merge(provinceCode, cityCode, repository.active());
    }

    private void requireReason(long operatorId, String reason) {
        if (operatorId <= 0 || reason == null || reason.isBlank()) throw new IllegalArgumentException("operator and reason required");
    }

    public interface ProfileRepository {
        void insertImmutable(PublishedGameProfile profile, long operatorId, String reason);
        void activate(long gameId, String version, long operatorId, String reason);
        default void publishAtomic(PublishedGameProfile profile, long operatorId, String reason, String requestId) {
            insertImmutable(profile, operatorId, reason);
            activate(profile.gameId(), profile.version(), operatorId, reason);
        }
        default void activateAtomic(long gameId, String version, long operatorId, String reason, String requestId) {
            activate(gameId, version, operatorId, reason);
        }
        java.util.Optional<PublishedGameProfile> find(long gameId, String version);
        List<PublishedGameProfile> active();
    }
}
