package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.*;
import com.aoo.bcg.mahjong.MahjongCatalogRuntimeRegistry;
import com.aoo.bcg.longcard.LongCardCatalogRuntimeRegistry;
import com.aoo.bcg.wordcard.WordCardCatalogRuntimeRegistry;
import com.aoo.bcg.poker.PokerCatalogRuntimeRegistry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class GameCatalogLoader {
    private static final String CURRENT_RESOURCE = "/game-catalog-current-32.tsv";
    private static final String COMPATIBILITY_RESOURCE = "/game-catalog-528.tsv";
    private GameCatalogLoader() {}

    static String catalogResourceFor(boolean production) {
        return production ? CURRENT_RESOURCE : COMPATIBILITY_RESOURCE;
    }

    static int registerMissing(GameRegistry registry) { return registerMissing(registry,null); }
    static int registerMissing(GameRegistry registry,ParticipantProfileSource profileSource) {
        if (registry == null) throw new IllegalArgumentException("registry is required");
        Map<Integer,GameDescriptor> nativeIds = new HashMap<>();
        Map<String,GameDescriptor> nativeCodes = new HashMap<>();
        registry.descriptors().forEach(game -> { nativeIds.put(game.gameId(),game); nativeCodes.put(game.code(),game); });
        Map<Integer,String> catalogIds=new HashMap<>();Map<String,Integer> catalogCodes=new HashMap<>();
        int registered = 0;
        String resource = catalogResourceFor(registry.isProduction());
        try (var input = GameCatalogLoader.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("missing " + resource);
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line == null) throw new IllegalStateException("empty game catalog");
                while ((line = reader.readLine()) != null) {
                    String[] v = line.split("\\t", -1);
                    if(v.length!=11)throw new IllegalStateException("invalid catalog column count");
                    int gameId = Integer.parseInt(v[0]);
                    requireCatalogIdentity(v, gameId);
                    GameDescriptor descriptor = new GameDescriptor(gameId, v[1], v[2],
                            StrictEnumDecoder.byName(GameCategory.class, v[3]), v[4],
                            StrictEnumDecoder.byName(RegionScope.class, v[5]), v[6], v[7], v[8]);
                    requireCategoryFamilyIdentity(descriptor);
                    if(catalogIds.putIfAbsent(gameId,v[1])!=null||catalogCodes.putIfAbsent(v[1],gameId)!=null)throw new IllegalStateException("duplicate catalog mapping: "+gameId+"/"+v[1]);
                    GameDescriptor byId=nativeIds.get(gameId),byCode=nativeCodes.get(v[1]);
                    if(byId!=null||byCode!=null){if(byId==null||byCode==null||byId!=byCode||!sameMapping(byId,descriptor))throw new IllegalStateException("native/catalog mapping conflict: "+gameId+"/"+v[1]);continue;}
                    if (registry.isProduction() && GameIdentityPolicy.isCompatibilityOnly(descriptor)) continue;
                    GameProvider provider = MahjongCatalogRuntimeRegistry.providerFor(descriptor)
                            .or(() -> LongCardCatalogRuntimeRegistry.providerFor(descriptor))
                            .or(() -> PokerCatalogRuntimeRegistry.providerFor(descriptor))
                            .orElse(null);
                    if (descriptor.category() == GameCategory.WORD_CARD) {
                        provider = WordCardCatalogRuntimeRegistry.providerFor(descriptor).orElseThrow(() ->
                                new IllegalStateException("WORD_CARD_COMPATIBILITY_PROVIDER_MISSING gameId="
                                        + descriptor.gameId() + " gameCode=" + descriptor.code()));
                    }
                    if (provider == null) provider = new CatalogGameProvider(descriptor, "1".equals(v[9]), v[10]);
                    if(profileSource!=null&&provider instanceof ParticipantProfileAware aware)aware.attachParticipantProfileSource(profileSource);
                    registry.register(provider);
                    registered++;
                }
            }
        } catch (IOException error) {
            throw new IllegalStateException("cannot load game catalog", error);
        }
        return registered;
    }

    static void requireCatalogIdentity(String[] fields, int gameId) {
        if (gameId <= 0 || fields[1].isBlank() || fields[2].isBlank() || fields[3].isBlank()
                || fields[4].isBlank() || fields[5].isBlank() || fields[8].isBlank() || fields[10].isBlank()) {
            throw new IllegalStateException("CATALOG_IDENTITY_UNRESOLVED gameId=" + gameId);
        }
    }
    static void requireCategoryFamilyIdentity(GameDescriptor descriptor) {
        boolean wordCardFamily = descriptor.family().startsWith("word-card:");
        if (descriptor.category() == GameCategory.WORD_CARD && !"word-card:pao-hu-zi".equals(descriptor.family())) {
            throw new IllegalStateException("CATALOG_IDENTITY_CONFLICT category=WORD_CARD gameId="
                    + descriptor.gameId() + " family=" + descriptor.family());
        }
        if (descriptor.category() != GameCategory.WORD_CARD && wordCardFamily) {
            throw new IllegalStateException("CATALOG_IDENTITY_CONFLICT category=" + descriptor.category()
                    + " gameId=" + descriptor.gameId() + " family=" + descriptor.family());
        }
        if (descriptor.category() == GameCategory.LONG_CARD && "long-card:pao-hu-zi".equals(descriptor.family())) return;
    }
    private static boolean sameMapping(GameDescriptor left,GameDescriptor right){return left.gameId()==right.gameId()&&left.code().equals(right.code())&&left.category()==right.category()&&left.family().equals(right.family())&&left.regionScope()==right.regionScope()&&left.provinceCode().equals(right.provinceCode())&&left.cityCode().equals(right.cityCode())&&left.version().equals(right.version());}
}
