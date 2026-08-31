package com.aoo.bcg.wordcard;import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class YcsdrRulesTest{
 @Test void eyesMayShareRankAcrossSuits(){assertTrue(YcsdrRules.canHu(List.of(1101,2101,1201,1202,1203,1301,1302,1303),0));}
 @Test void rejectsIncompleteAndPortsMaoKanSelfDraw(){assertFalse(YcsdrRules.canHu(List.of(1101,1201,1301,1401),0));assertEquals(12,YcsdrRules.huPoint(YcsdrRules.Pattern.TAI_HU,2,2,1,true,true));}
 @Test void pengGangUsePhysicalCardType(){assertTrue(YcsdrRules.canPeng(List.of(1101,1102),1103));assertTrue(YcsdrRules.canGang(List.of(1101,1102,1103),1104));}
}
