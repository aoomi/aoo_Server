package com.aoo.bcg.gamespi;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class GameRegistry {
    public enum RegistrationMode { PRODUCTION, COMPATIBILITY }

    private final Map<Integer, Map<String,GameProvider>> byId = new LinkedHashMap<>();
    private final Map<String, Map<String,GameProvider>> byCode = new LinkedHashMap<>();
    private final RegistrationMode mode;
    private RuntimeMetadataProjection runtimeMetadataProjection;

    /**
     * Package-private compatibility constructor. Production and external
     * modules must choose a mode through {@link GameRegistryFactory}.
     */
    GameRegistry() { this(RegistrationMode.COMPATIBILITY); }

    GameRegistry(RegistrationMode mode) {
        if (mode == null) throw new IllegalArgumentException("REGISTRATION_MODE_REQUIRED");
        this.mode = mode;
    }

    /**
     * Exposes the admission projection to catalog loaders without exposing
     * the mutable registry internals.  Production and Compatibility are
     * deliberately separate projections; callers must not infer the mode
     * from a descriptor or from the presence of a provider.
     */
    public boolean isProduction() {
        return mode == RegistrationMode.PRODUCTION;
    }

    public synchronized void register(GameProvider provider) {
        GameDescriptor descriptor = provider.descriptor();
        if (mode == RegistrationMode.PRODUCTION) GameIdentityPolicy.requireProduction(descriptor);
        if (runtimeMetadataProjection != null) {
            RuntimeMetadataProjectionValidator.validate(descriptor, runtimeMetadataProjection);
        }
        registerUnchecked(provider, descriptor);
    }

    synchronized void registerCompatibility(GameProvider provider) {
        if (mode != RegistrationMode.COMPATIBILITY) {
            throw new IllegalStateException("COMPATIBILITY_REGISTRY_REQUIRED");
        }
        GameDescriptor descriptor = provider.descriptor();
        GameRegistryFactory.requireCompatibilityIdentity(descriptor);
        registerUnchecked(provider, descriptor);
    }

    private void registerUnchecked(GameProvider provider, GameDescriptor descriptor) {
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
        return admit(versions.values().iterator().next());
    }

    public synchronized GameProvider require(int gameId,String playVersion) {
        if(playVersion==null||playVersion.isBlank())throw new IllegalArgumentException("playVersion is required");
        GameProvider provider=byId.getOrDefault(gameId,Map.of()).get(playVersion);
        if(provider==null)throw new IllegalArgumentException("unknown gameId/playVersion: "+gameId+"/"+playVersion);
        return admit(provider);
    }

    public synchronized GameProvider require(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("game code must not be blank");
        Map<String,GameProvider> versions=byCode.get(code.trim().toLowerCase());
        if(versions==null||versions.isEmpty())throw new IllegalArgumentException("unknown game code: "+code);
        if(versions.size()!=1)throw new IllegalStateException("playVersion required for multi-version game code: "+code);
        return admit(versions.values().iterator().next());
    }

    private GameProvider admit(GameProvider provider) {
        if (mode == RegistrationMode.PRODUCTION) {
            GameIdentityPolicy.requireCurrentPublished(provider.descriptor().gameId(), provider.descriptor().version());
        }
        return provider;
    }

    public synchronized Collection<GameDescriptor> descriptors() {
        return byId.values().stream().flatMap(value->value.values().stream()).map(GameProvider::descriptor).toList();
    }

    /**
     * Binds the release/build projection after the registry has been assembled.
     * The projection is immutable and cannot be replaced, which prevents a
     * second source from silently superseding the signed release input.
     */
    public synchronized void bindRuntimeMetadataProjection(RuntimeMetadataProjection projection) {
        Objects.requireNonNull(projection, "runtime metadata projection");
        if (mode != RegistrationMode.PRODUCTION) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_PRODUCTION_REGISTRY_REQUIRED");
        }
        RuntimeMetadataProjectionValidator.validate(this, projection);
        if (runtimeMetadataProjection != null && !runtimeMetadataProjection.equals(projection)) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_ALREADY_BOUND");
        }
        runtimeMetadataProjection = projection;
    }

    /** Alias for adapters that call the operation an attachment. */
    public synchronized void attachRuntimeMetadataProjection(RuntimeMetadataProjection projection) {
        bindRuntimeMetadataProjection(projection);
    }

    public synchronized Optional<RuntimeMetadataProjection> runtimeMetadataProjection() {
        return Optional.ofNullable(runtimeMetadataProjection);
    }

    public synchronized RuntimeMetadataProjection requireRuntimeMetadataProjection() {
        if (runtimeMetadataProjection == null) {
            throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_REQUIRED");
        }
        return runtimeMetadataProjection;
    }
}
