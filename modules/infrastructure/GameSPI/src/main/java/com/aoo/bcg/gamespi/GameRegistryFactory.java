package com.aoo.bcg.gamespi;

import java.util.Objects;
import java.util.Set;
import java.util.ServiceLoader;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The only factory allowed to choose a provider-registry admission policy.
 * Production assembly must fail closed for compatibility identities; historical
 * migration code must opt into the compatibility registry explicitly.
 */
public final class GameRegistryFactory {
    private static final Set<CompatibilityIdentity> HISTORICAL_COMPATIBILITY_ALLOWLIST = Set.of(
            new CompatibilityIdentity(516, "chengdu1001", GameCategory.MAHJONG,
                    "mahjong-xue-zhan", RegionScope.CITY, "SC", "CD", "legacy-equivalent-1"),
            new CompatibilityIdentity(618, "xcpdk", GameCategory.POKER,
                    "poker:pao-de-kuai", RegionScope.CITY, "AH", "XC", "legacy-equivalent-1"),
            new CompatibilityIdentity(628, "scjymj", GameCategory.MAHJONG,
                    "mahjong-standard", RegionScope.CITY, "SC", "JY", "legacy-equivalent-1"));

    private GameRegistryFactory() { }

    public static GameRegistry production() {
        return new GameRegistry(GameRegistry.RegistrationMode.PRODUCTION);
    }

    public static GameRegistry compatibility() {
        return new GameRegistry(GameRegistry.RegistrationMode.COMPATIBILITY);
    }

    /**
     * Discovers the providers visible to the production runtime through the
     * single GameSPI admission path. Compatibility-only providers are not
     * registered in this projection. Stale or unknown non-compatibility rows
     * are excluded by the same current-publication policy. If discovery yields
     * no current provider, startup fails closed instead of silently falling back
     * to a historical provider or an empty registry.
     */
    public static GameRegistry productionFromServiceLoader() {
        return productionFromServiceLoader((Consumer<GameRegistry>) null);
    }

    /**
     * Runtime consumers that do not have the authoritative catalog completion
     * input must fail closed.  In particular, gameServer must not reinterpret
     * its raw ServiceLoader view as the Bootstrap/Gateway completed projection.
     */
    public static GameRegistry requireCompletedProductionProjection() {
        throw new IllegalStateException(
                "PRODUCTION_COMPLETION_REQUIRED consumer=gameServer source=bootstrap-catalog");
    }

    /**
     * Canonical production projection assembly.  The optional completion hook is
     * reserved for an authoritative catalog adapter (Bootstrap uses it to add
     * catalog-backed providers); policy, filtering and completeness validation
     * remain here so no caller can create a second production projection.
     */
    public static GameRegistry productionFromServiceLoader(Consumer<GameRegistry> completion) {
        return productionFromServiceLoader(provider -> true, completion);
    }

    /**
     * Canonical production projection assembly with a caller-owned source filter.
     * The filter may exclude a known non-production SPI source, but it cannot
     * admit compatibility or unpublished identities; those checks remain here.
     */
    public static GameRegistry productionFromServiceLoader(
            Predicate<? super GameProvider> sourceFilter,
            Consumer<GameRegistry> completion) {
        Objects.requireNonNull(sourceFilter, "sourceFilter");
        GameRegistry registry = production();
        discoverInto(registry, ServiceLoader.load(GameProvider.class), sourceFilter);
        if (completion != null) completion.accept(registry);
        if (registry.descriptors().isEmpty()) {
            throw new IllegalStateException("NO_CURRENT_PRODUCTION_PROVIDER");
        }
        requireCompleteProductionProjection(registry);
        return registry;
    }

    /** Strict production assembly bound to a release-owned metadata projection. */
    public static GameRegistry productionFromServiceLoader(RuntimeMetadataProjectionSource metadataSource) {
        return productionFromServiceLoader(provider -> true, null, metadataSource);
    }

    public static GameRegistry productionFromServiceLoader(
            Predicate<? super GameProvider> sourceFilter,
            Consumer<GameRegistry> completion,
            RuntimeMetadataProjectionSource metadataSource) {
        Objects.requireNonNull(metadataSource, "runtime metadata projection source");
        GameRegistry registry = productionFromServiceLoader(sourceFilter, completion);
        RuntimeMetadataProjection projection = RuntimeMetadataProjectionValidator.validate(
                Objects.requireNonNull(metadataSource.load(), "runtime metadata projection"));
        registry.bindRuntimeMetadataProjection(projection);
        return registry;
    }

    /**
     * Discovers historical providers for an explicitly compatibility-scoped
     * caller. This method never shares the production registry projection.
     */
    public static GameRegistry compatibilityFromServiceLoader() {
        return compatibilityFrom(ServiceLoader.load(GameProvider.class));
    }

    /**
     * Testable/embedded equivalent of {@link #productionFromServiceLoader()}.
     * The iterable is intentionally supplied by the caller so a production
     * runtime cannot accidentally reuse a compatibility registry.
     */
    public static GameRegistry productionFrom(Iterable<? extends GameProvider> providers) {
        GameRegistry registry = production();
        discoverInto(registry, providers, provider -> true);
        if (registry.descriptors().isEmpty()) {
            throw new IllegalStateException("NO_CURRENT_PRODUCTION_PROVIDER");
        }
        return registry;
    }

    public static GameRegistry productionFrom(Iterable<? extends GameProvider> providers,
                                              RuntimeMetadataProjectionSource metadataSource) {
        Objects.requireNonNull(metadataSource, "runtime metadata projection source");
        GameRegistry registry = productionFrom(providers);
        registry.bindRuntimeMetadataProjection(RuntimeMetadataProjectionValidator.validate(
                Objects.requireNonNull(metadataSource.load(), "runtime metadata projection")));
        return registry;
    }

    /** Validate the shared production projection after all canonical inputs are supplied. */
    public static void requireCompleteProductionProjection(GameRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("production registry is required");
        Set<Integer> currentIds = new HashSet<>();
        for (GameDescriptor descriptor : registry.descriptors()) {
            if (!GameIdentityPolicy.isCurrentPublished(descriptor.gameId(), descriptor.version())) continue;
            validateCurrentDescriptor(descriptor);
            if (!currentIds.add(descriptor.gameId())) {
                throw new IllegalStateException("PRODUCTION_PROJECTION_DUPLICATE gameId="
                        + descriptor.gameId() + " playVersion=" + descriptor.version());
            }
        }
        if (currentIds.size() != 32) {
            throw new IllegalStateException("PRODUCTION_PROJECTION_INCOMPLETE currentCount="
                    + currentIds.size() + " expected=32");
        }
    }

    /**
     * Testable/embedded equivalent of {@link #compatibilityFromServiceLoader()}.
     * Historical providers remain available only when this path is selected
     * explicitly.
     */
    public static GameRegistry compatibilityFrom(Iterable<? extends GameProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        GameRegistry registry = compatibility();
        for (GameProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            GameDescriptor descriptor = Objects.requireNonNull(provider.descriptor(), "provider.descriptor");
            // ServiceLoader may expose current and historical providers in one classpath.
            // Unapproved descriptors are excluded from the Compatibility projection; a
            // projection with no approved provider still fails closed below.
            if (isCompatibilityIdentityAllowed(descriptor)) registry.registerCompatibility(provider);
        }
        if (registry.descriptors().isEmpty()) {
            throw new IllegalStateException("NO_ALLOWLISTED_COMPATIBILITY_PROVIDER");
        }
        return registry;
    }

    static void requireCompatibilityIdentity(GameDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "provider.descriptor");
        if (!isCompatibilityIdentityAllowed(descriptor)) {
            throw new IllegalArgumentException("COMPATIBILITY_IDENTITY_NOT_ALLOWLISTED gameId="
                    + descriptor.gameId() + " gameCode=" + descriptor.code()
                    + " family=" + descriptor.family() + " playVersion=" + descriptor.version());
        }
    }

    static boolean isCompatibilityIdentityAllowed(GameDescriptor descriptor) {
        return descriptor != null && HISTORICAL_COMPATIBILITY_ALLOWLIST.contains(CompatibilityIdentity.from(descriptor));
    }

    private static void discoverInto(GameRegistry registry,
                                     Iterable<? extends GameProvider> providers,
                                     Predicate<? super GameProvider> sourceFilter) {
        Objects.requireNonNull(providers, "providers");
        for (GameProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            if (!sourceFilter.test(provider)) continue;
            GameDescriptor descriptor = Objects.requireNonNull(provider.descriptor(), "provider.descriptor");
            if (GameIdentityPolicy.isCompatibilityOnly(descriptor)
                    || !GameIdentityPolicy.isCurrentPublished(descriptor.gameId(), descriptor.version())) {
                continue;
            }
            validateCurrentDescriptor(descriptor);
            registry.register(provider);
        }
    }

    private static void validateCurrentDescriptor(GameDescriptor descriptor) {
        GameIdentityPolicy.PublishedIdentity expected = GameIdentityPolicy.currentById(descriptor.gameId())
                .orElseThrow(() -> new IllegalStateException("PRODUCTION_IDENTITY_NOT_PUBLISHED gameId="
                        + descriptor.gameId() + " playVersion=" + descriptor.version()));
        if (!expected.gameCode().equals(descriptor.code())
                || !expected.playVersion().equals(descriptor.version())
                || !expected.family().equals(descriptor.family())) {
            throw new IllegalStateException("PRODUCTION_IDENTITY_CONFLICT gameId=" + descriptor.gameId()
                    + " gameCode=" + descriptor.code() + " family=" + descriptor.family()
                    + " playVersion=" + descriptor.version());
        }
        if ((descriptor.regionScope() == RegionScope.CITY || descriptor.regionScope() == RegionScope.PROVINCE)
                && descriptor.provinceCode().isBlank()) {
            throw new IllegalStateException("PRODUCTION_REGION_REQUIRED gameId=" + descriptor.gameId()
                    + " gameCode=" + descriptor.code());
        }
    }

    private record CompatibilityIdentity(int gameId, String code, GameCategory category,
                                         String family, RegionScope regionScope,
                                         String provinceCode, String cityCode, String version) {
        private static CompatibilityIdentity from(GameDescriptor descriptor) {
            return new CompatibilityIdentity(descriptor.gameId(), descriptor.code(), descriptor.category(),
                    descriptor.family(), descriptor.regionScope(), descriptor.provinceCode(),
                    descriptor.cityCode(), descriptor.version());
        }
    }
}
