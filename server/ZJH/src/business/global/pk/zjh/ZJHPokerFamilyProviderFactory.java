package business.global.pk.zjh;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.poker.ComparePokerFamily;
import com.aoo.bcg.poker.PokerFamilyProviderFactory;
import java.util.Optional;

/** Binds the independently packaged CN297 game to its compare-hand Poker family. */
public final class ZJHPokerFamilyProviderFactory implements PokerFamilyProviderFactory {
    @Override
    public Optional<GameProvider> create(GameDescriptor descriptor) {
        if (descriptor == null
                || descriptor.category() != GameCategory.POKER
                || !ZJHGameProvider.GAME_CODE.equals(descriptor.code())
                || !ComparePokerFamily.CODE.equals(descriptor.family())) {
            return Optional.empty();
        }
        return Optional.of(new ZJHGameProvider());
    }
}
