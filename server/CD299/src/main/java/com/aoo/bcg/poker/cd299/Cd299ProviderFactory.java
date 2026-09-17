package com.aoo.bcg.poker.cd299;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.poker.PokerFamilyProviderFactory;
import java.util.Optional;

public final class Cd299ProviderFactory implements PokerFamilyProviderFactory {
    public Optional<GameProvider> create(GameDescriptor descriptor) {
        if (descriptor.category()!=GameCategory.POKER || !Cd299Rules.GAME_CODE.equals(descriptor.code()) || !Cd299Rules.FAMILY.equals(descriptor.family())) return Optional.empty();
        return Optional.of(new Cd299GameProvider(descriptor));
    }
}
