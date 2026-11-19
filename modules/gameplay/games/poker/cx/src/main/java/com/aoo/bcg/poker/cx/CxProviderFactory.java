package com.aoo.bcg.poker.cx;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.poker.PokerFamilyProviderFactory;
import java.util.Optional;

public final class CxProviderFactory implements PokerFamilyProviderFactory {
    public Optional<GameProvider> create(GameDescriptor descriptor) {
        if (descriptor.category()!=GameCategory.POKER || !CxRules.GAME_CODE.equals(descriptor.code()) || !CxRules.FAMILY.equals(descriptor.family())) return Optional.empty();
        return Optional.of(new CxGameProvider(descriptor));
    }
}
