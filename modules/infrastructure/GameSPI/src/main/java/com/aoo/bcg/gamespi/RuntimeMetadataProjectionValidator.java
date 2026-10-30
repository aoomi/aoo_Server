package com.aoo.bcg.gamespi;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Fail-closed admission checks for canonical runtime metadata projections. */
public final class RuntimeMetadataProjectionValidator {
    private RuntimeMetadataProjectionValidator() { }

    public static RuntimeMetadataProjection validate(RuntimeMetadataProjection projection) {
        Objects.requireNonNull(projection, "runtime metadata projection");
        if (projection.rowCount() != RuntimeMetadataProjection.REQUIRED_ROW_COUNT) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_INCOMPLETE rows="
                    + projection.rowCount() + " expected=" + RuntimeMetadataProjection.REQUIRED_ROW_COUNT);
        }
        return projection;
    }

    /**
     * Validates the complete registry against the exact identity tuple carried
     * by the release projection. No field is filled from a descriptor, game id,
     * environment, database, or a default value.
     */
    public static RuntimeMetadataProjection validate(GameRegistry registry,
                                                      RuntimeMetadataProjection projection) {
        validate(projection);
        if (registry == null || !registry.isProduction()) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_PRODUCTION_REGISTRY_REQUIRED");
        }
        Set<Integer> registryIds = new HashSet<>();
        for (GameDescriptor descriptor : registry.descriptors()) {
            if (!GameIdentityPolicy.isCurrentPublished(descriptor.gameId(), descriptor.version())) continue;
            if (!registryIds.add(descriptor.gameId())) {
                throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_DUPLICATE_REGISTRY_ID gameId="
                        + descriptor.gameId());
            }
            RuntimeMetadataProjection.Entry row = projection.require(descriptor.gameId());
            requireIdentity(descriptor, row);
        }
        if (registryIds.size() != RuntimeMetadataProjection.REQUIRED_ROW_COUNT) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_REGISTRY_INCOMPLETE currentCount="
                    + registryIds.size() + " expected=" + RuntimeMetadataProjection.REQUIRED_ROW_COUNT);
        }
        for (RuntimeMetadataProjection.Entry row : projection.rows()) {
            if (!registryIds.contains(row.gameId())) {
                throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_ROW_NOT_REGISTERED gameId="
                        + row.gameId());
            }
        }
        return projection;
    }

    public static RuntimeMetadataProjection.Entry validate(GameDescriptor descriptor,
                                                            RuntimeMetadataProjection projection) {
        Objects.requireNonNull(descriptor, "descriptor");
        validate(projection);
        if (!GameIdentityPolicy.isCurrentPublished(descriptor.gameId(), descriptor.version())) {
            throw new IllegalStateException("RUNTIME_METADATA_IDENTITY_NOT_CURRENT gameId="
                    + descriptor.gameId() + " playVersion=" + descriptor.version());
        }
        RuntimeMetadataProjection.Entry row = projection.require(descriptor.gameId());
        requireIdentity(descriptor, row);
        return row;
    }

    private static void requireIdentity(GameDescriptor descriptor,
                                        RuntimeMetadataProjection.Entry row) {
        if (!row.gameCode().equals(descriptor.code())
                || !row.family().equals(descriptor.family())
                || !row.playVersion().equals(descriptor.version())) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_IDENTITY_MISMATCH gameId="
                    + descriptor.gameId() + " gameCode=" + descriptor.code()
                    + " playVersion=" + descriptor.version());
        }
    }
}
