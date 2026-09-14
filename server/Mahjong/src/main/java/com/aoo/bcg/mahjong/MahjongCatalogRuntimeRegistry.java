package com.aoo.bcg.mahjong;

import com.aoo.bcg.gamespi.*;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Catalog-to-family runtime router. Catalog rows are configurations, not SPI implementations. */
public final class MahjongCatalogRuntimeRegistry {
    private MahjongCatalogRuntimeRegistry() {}

    public static Optional<GameProvider> providerFor(GameDescriptor descriptor) {
        if (descriptor.category() != GameCategory.MAHJONG) return Optional.empty();
        return MahjongRegionRuntimeProfiles.find(descriptor.code())
                .filter(MahjongRegionRuntimeProfile::sourceAvailable)
                .map(profile -> new CatalogMahjongFamilyProvider(descriptor,profile));
    }

    public static int profileCount(){return MahjongRegionRuntimeProfiles.all().size();}
    public static int runnableProfileCount(){return (int)MahjongRegionRuntimeProfiles.all().stream().filter(MahjongRegionRuntimeProfile::sourceAvailable).count();}
    public static Set<String> registeredFamilies(){return MahjongRegionRuntimeProfiles.all().stream().filter(MahjongRegionRuntimeProfile::sourceAvailable).map(MahjongRegionRuntimeProfile::family).collect(java.util.stream.Collectors.toUnmodifiableSet());}

    /** Prevent the internal runtime module's historical per-code SPI file from bypassing catalog routing. */
    public static boolean isInternalRuntimeProvider(GameProvider provider) {
        var registrySource = MahjongCatalogRuntimeRegistry.class.getProtectionDomain().getCodeSource();
        var providerSource = provider.getClass().getProtectionDomain().getCodeSource();
        return registrySource != null && providerSource != null
                && registrySource.getLocation().equals(providerSource.getLocation());
    }
}
