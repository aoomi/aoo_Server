package com.aoo.bcg.poker.nn;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.poker.PokerFamilyProviderFactory;
import java.util.Optional;

/** ServiceLoader entry owned exclusively by the nationwide NN Nn family. */
public final class NnPokerFamilyProviderFactory implements PokerFamilyProviderFactory {
  @Override public Optional<GameProvider> create(GameDescriptor descriptor) {
    if (descriptor.category() != GameCategory.POKER
        || !NnRules.GAME_CODE.equals(descriptor.code())
        || !NnRules.FAMILY.equals(descriptor.family())
        || !NnRules.PLAY_VERSION.equals(descriptor.version())) return Optional.empty();
    return Optional.of(new NnGameProvider(descriptor));
  }
}
