package com.aoo.bcg.poker.nn;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.poker.PokerFamilyProviderFactory;
import java.util.Optional;

/** ServiceLoader entry owned exclusively by the nationwide CN298 NiuNiu family. */
public final class CN298PokerFamilyProviderFactory implements PokerFamilyProviderFactory {
  @Override public Optional<GameProvider> create(GameDescriptor descriptor) {
    if (descriptor.category() != GameCategory.POKER
        || !NiuNiuRules.GAME_CODE.equals(descriptor.code())
        || !NiuNiuRules.FAMILY.equals(descriptor.family())
        || !NiuNiuRules.PLAY_VERSION.equals(descriptor.version())) return Optional.empty();
    return Optional.of(new CN298GameProvider(descriptor));
  }
}
