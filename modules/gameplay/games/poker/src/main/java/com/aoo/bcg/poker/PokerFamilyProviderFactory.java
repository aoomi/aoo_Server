package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import java.util.Optional;

/**
 * Runtime extension point for an independently packaged Poker family.
 *
 * <p>An implementation is discovered through {@link java.util.ServiceLoader} and must return a
 * provider only for descriptors it owns. A gameplay module publishes the implementation in
 * {@code META-INF/services/com.aoo.bcg.poker.PokerFamilyProviderFactory}; the module jar merely
 * needs to be present on the application classpath.
 */
public interface PokerFamilyProviderFactory {
    Optional<GameProvider> create(GameDescriptor descriptor);
}
