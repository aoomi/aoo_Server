package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameIdentityPolicy;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.RuntimeMetadataProjection;
import com.aoo.bcg.gamespi.RuntimeMetadataProjectionSource;
import com.aoo.bcg.gamespi.RuntimeMetadataProjectionValidator;

import java.util.HashSet;
import java.util.Set;

/** Fail-closed validation for the production registry projection. */
public final class RuntimeIdentityValidator {
    private static final int CURRENT_IDENTITY_COUNT = 32;

    private RuntimeIdentityValidator() {}

    public static void validateProduction(GameRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("production registry is required");
        if (registry.descriptors().isEmpty()) throw new IllegalStateException("production registry is empty");

        Set<Integer> currentIds = new HashSet<>();
        for (GameDescriptor descriptor : registry.descriptors()) {
            GameIdentityPolicy.requireProduction(descriptor);
            if (GameIdentityPolicy.isCurrentPublished(descriptor.gameId(), descriptor.version())) {
                if (!currentIds.add(descriptor.gameId())) {
                    throw new IllegalStateException("duplicate current production identity: "
                            + descriptor.gameId() + "/" + descriptor.version());
                }
                GameProvider provider = registry.require(descriptor.gameId(), descriptor.version());
                if (provider.descriptor() != descriptor) {
                    throw new IllegalStateException("registry descriptor/provider mismatch: "
                            + descriptor.gameId() + "/" + descriptor.version());
                }
            }
        }
        if (currentIds.size() != CURRENT_IDENTITY_COUNT) {
            throw new IllegalStateException("current production identity count invalid: "
                    + currentIds.size() + ", expected " + CURRENT_IDENTITY_COUNT);
        }
    }

    /** Admission gate for the canonical release/build projection. */
    public static void validateProduction(GameRegistry registry,
                                          RuntimeMetadataProjectionSource metadataSource) {
        if (metadataSource == null) throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_REQUIRED");
        validateProduction(registry, metadataSource.projection());
    }

    public static void validateProduction(GameRegistry registry,
                                          RuntimeMetadataProjection projection) {
        RuntimeMetadataProjectionValidator.validate(registry, projection);
        RuntimeMetadataProjection bound = registry.requireRuntimeMetadataProjection();
        if (!bound.equals(projection)) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_SOURCE_CONFLICT");
        }
    }
}
