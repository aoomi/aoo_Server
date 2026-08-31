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
    private static final String RESOURCE = "/game-catalog-528.tsv";
    private GameCatalogLoader() {}

    static int registerMissing(GameRegistry registry) {
        Map<Integer,GameDescriptor> nativeIds = new HashMap<>();
        Map<String,GameDescriptor> nativeCodes = new HashMap<>();
        registry.descriptors().forEach(game -> { nativeIds.put(game.gameId(),game); nativeCodes.put(game.code(),game); });
        Map<Integer,String> catalogIds=new HashMap<>();Map<String,Integer> catalogCodes=new HashMap<>();
        int registered = 0;
        try (var input = GameCatalogLoader.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing " + RESOURCE);
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line == null) throw new IllegalStateException("empty game catalog");
                while ((line = reader.readLine()) != null) {
                    String[] v = line.split("\\t", -1);
                    if(v.length!=11)throw new IllegalStateException("invalid catalog column count");
                    int gameId = Integer.parseInt(v[0]);
                    GameDescriptor descriptor = new GameDescriptor(gameId, v[1], v[2],
                            StrictEnumDecoder.byName(GameCategory.class, v[3]), v[4],
                            StrictEnumDecoder.byName(RegionScope.class, v[5]), v[6], v[7], v[8]);
                    if(catalogIds.putIfAbsent(gameId,v[1])!=null||catalogCodes.putIfAbsent(v[1],gameId)!=null)throw new IllegalStateException("duplicate catalog mapping: "+gameId+"/"+v[1]);
                    GameDescriptor byId=nativeIds.get(gameId),byCode=nativeCodes.get(v[1]);
                    if(byId!=null||byCode!=null){if(byId==null||byCode==null||byId!=byCode||!sameMapping(byId,descriptor))throw new IllegalStateException("native/catalog mapping conflict: "+gameId+"/"+v[1]);continue;}
                    registry.register(MahjongCatalogRuntimeRegistry.providerFor(descriptor)
                            .or(() -> LongCardCatalogRuntimeRegistry.providerFor(descriptor))
                            .or(() -> WordCardCatalogRuntimeRegistry.providerFor(descriptor))
                            .or(() -> PokerCatalogRuntimeRegistry.providerFor(descriptor))
                            .orElseGet(() -> new CatalogGameProvider(descriptor, "1".equals(v[9]), v[10])));
                    registered++;
                }
            }
        } catch (IOException error) {
            throw new IllegalStateException("cannot load game catalog", error);
        }
        return registered;
    }
    private static boolean sameMapping(GameDescriptor left,GameDescriptor right){return left.gameId()==right.gameId()&&left.code().equals(right.code())&&left.category()==right.category()&&left.family().equals(right.family())&&left.regionScope()==right.regionScope()&&left.provinceCode().equals(right.provinceCode())&&left.cityCode().equals(right.cityCode())&&left.version().equals(right.version());}
}
