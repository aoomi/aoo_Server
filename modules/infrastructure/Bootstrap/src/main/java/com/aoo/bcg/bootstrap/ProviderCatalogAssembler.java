package com.aoo.bcg.bootstrap;

import com.aoo.bcg.common.readiness.ServiceReadinessGate;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.ParticipantProfileAware;
import com.aoo.bcg.gamespi.ParticipantProfileSource;
import com.aoo.bcg.gamespi.RuntimeMetadataProjectionSource;

import java.util.List;

/**
 * The single production registry assembly entry point used by Bootstrap and Gateway.
 *
 * <p>Compatibility callers must continue to opt into
 * {@link GameRegistryFactory#compatibility()} explicitly. This assembler never
 * loads compatibility providers into a production registry.</p>
 */
public final class ProviderCatalogAssembler {
    private ProviderCatalogAssembler() {}

    public static GameRegistry production() {
        return production((ParticipantProfileSource) null, CanonicalRuntimeMetadataManifestReader.bundled());
    }

    public static GameRegistry production(ParticipantProfileSource profileSource) {
        return production(profileSource, CanonicalRuntimeMetadataManifestReader.bundled());
    }

    /**
     * Strict release assembly. The caller must provide the build/release
     * projection; this overload never reads a checkout path or derives metadata
     * from a descriptor.
     */
    public static GameRegistry production(RuntimeMetadataProjectionSource metadataSource) {
        return production(null, metadataSource);
    }

    public static GameRegistry production(ParticipantProfileSource profileSource,
                                          RuntimeMetadataProjectionSource metadataSource) {
        if (metadataSource == null) throw new IllegalStateException("RUNTIME_METADATA_PROJECTION_REQUIRED");
        var projection = metadataSource.projection();
        GameRegistry[] assembled = new GameRegistry[1];
        ServiceReadinessGate readiness = new ServiceReadinessGate(List.of(
                ServiceReadinessGate.check("gameProviderDiscovery", () ->
                        assembled[0] = GameRegistryFactory.productionFromServiceLoader(
                                ProviderCatalogAssembler::isBootstrapDiscoverable,
                                registry -> {
                                    attachParticipantProfileSource(registry, profileSource);
                                    GameCatalogLoader.registerMissing(registry, profileSource);
                                }, RuntimeMetadataProjectionSource.of(projection))),
                ServiceReadinessGate.check("gameCatalogIndex", () -> {
                    GameRegistry registry = assembled[0];
                    if (registry == null) throw new IllegalStateException("production provider projection is missing");
                    if (registry.descriptors().isEmpty()) {
                        throw new IllegalStateException("no production GameProvider is available");
                    }
                    if (registry.descriptors().stream().anyMatch(descriptor ->
                            descriptor.category() == com.aoo.bcg.gamespi.GameCategory.WORD_CARD)) {
                        throw new IllegalStateException("WORD_CARD_PRODUCTION_PROJECTION_LEAK");
                    }
                    RuntimeIdentityValidator.validateProduction(registry, projection);
                })));
        readiness.verifyAndOpen();
        readiness.requireAcceptingTraffic();
        return assembled[0];
    }

    /**
     * Explicit historical projection. This path is not used by Bootstrap or Gateway
     * production assembly and retains WordCard rows for Compatibility callers.
     */
    public static GameRegistry compatibility() {
        return compatibility(null);
    }

    public static GameRegistry compatibility(ParticipantProfileSource profileSource) {
        GameRegistry registry = GameRegistryFactory.compatibility();
        GameCatalogLoader.registerMissing(registry, profileSource);
        if (registry.descriptors().isEmpty()) {
            throw new IllegalStateException("no Compatibility GameProvider is available");
        }
        return registry;
    }

    private static boolean isBootstrapDiscoverable(GameProvider provider) {
        return !com.aoo.bcg.mahjong.MahjongCatalogRuntimeRegistry.isInternalRuntimeProvider(provider)
                && !provider.getClass().getName().equals("business.global.pk.njpdk.NJPDKGameProvider");
    }

    private static void attachParticipantProfileSource(GameRegistry registry,
                                                       ParticipantProfileSource profileSource) {
        if (profileSource == null) return;
        registry.descriptors().forEach(descriptor -> {
            GameProvider provider = registry.require(descriptor.gameId(), descriptor.version());
            if (provider instanceof ParticipantProfileAware aware) {
                aware.attachParticipantProfileSource(profileSource);
            }
        });
    }
}
