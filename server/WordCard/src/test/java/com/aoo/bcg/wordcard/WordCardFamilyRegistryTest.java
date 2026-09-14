package com.aoo.bcg.wordcard;
import com.aoo.bcg.gamespi.*;import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
final class WordCardFamilyRegistryTest{
 @Test void allTenRowsUseOneFamilyProvider(){for(String code:List.of("byzp","bzp","ycsdr","pxphz","xpphz","yzchz","glzp","ychp","ahphz","lczp")){var d=new GameDescriptor(1,code,code,GameCategory.WORD_CARD,"word-card-pao-hu-zi",RegionScope.NATIONAL,"","","1.0.0");var p=WordCardCatalogRuntimeRegistry.providerFor(d).orElseThrow();assertEquals(WordCardFamilyProvider.class,p.getClass());assertEquals(d,p.descriptor());assertTrue(p.eventReplayProvider().isPresent());assertNotNull(p.roomFactory());}}
 @Test void rejectsUnknownCode(){assertTrue(WordCardCatalogRuntimeRegistry.providerFor(new GameDescriptor(1,"x","x",GameCategory.WORD_CARD,"x",RegionScope.NATIONAL,"","","1")).isEmpty());}
}
