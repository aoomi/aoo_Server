package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/** Resolves exactly one built-in or independently packaged Poker provider factory. */
public final class PokerFamilyProviderFactories {
    private static final PokerFamilyProviderFactory BUILT_INS = descriptor -> {
        if (descriptor.category() != GameCategory.POKER) return Optional.empty();
        GameProvider provider = PaoDeKuaiFamily.CODE.equals(descriptor.family())
                ? new PdkGameProvider(descriptor)
                : switch (descriptor.code()) {
                    case "cp" -> new CpGameProvider();
                    case "hndzp" -> new HndzpGameProvider();
                    case "lhzp" -> new LhzpGameProvider();
                    default -> null;
                };
        return Optional.ofNullable(provider);
    };

    private PokerFamilyProviderFactories() {}

    public static Optional<GameProvider> providerFor(GameDescriptor descriptor) {
        return providerFor(descriptor, ServiceLoader.load(PokerFamilyProviderFactory.class));
    }

    static Optional<GameProvider> providerFor(
            GameDescriptor descriptor, Iterable<PokerFamilyProviderFactory> extensions) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(extensions, "extensions");
        if (descriptor.category() != GameCategory.POKER) return Optional.empty();

        List<GameProvider> matches = new ArrayList<>();
        BUILT_INS.create(descriptor).ifPresent(matches::add);
        for (PokerFamilyProviderFactory extension : extensions) {
            Objects.requireNonNull(extension, "Poker family provider factory")
                    .create(descriptor)
                    .ifPresent(matches::add);
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("duplicate Poker provider factory binding: "
                    + descriptor.code() + "/" + descriptor.family());
        }
        return matches.stream().findFirst();
    }
}
