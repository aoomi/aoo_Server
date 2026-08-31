package com.aoo.bcg.wordcard;
import java.util.Set;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
final class BuiltinPaoHuZiFamiliesTest{
 @Test void registersTwelveAuditedModulesAndRejectsOtherCardFamilies(){assertEquals(Set.of("AHPHZ","BYZP","DYZP","GLZP","HNDZP","JSZP","LCZP","LHZP","PXPHZ","XPPHZ","YCHP","YZCHZ"),BuiltinPaoHuZiFamilies.all().keySet());for(String excluded:Set.of("BZP","YCSDR","ZGSDR"))assertThrows(IllegalArgumentException.class,()->BuiltinPaoHuZiFamilies.require(excluded));}
 @Test void keepsRegionalMingTangAndRedBlackPolicyIsolated(){var anhua=BuiltinPaoHuZiFamilies.require("AHPHZ");var boyang=BuiltinPaoHuZiFamilies.require("BYZP");assertNotEquals(anhua.profile().mingTangNamespace(),boyang.profile().mingTangNamespace());assertTrue(anhua.profile().redBlackScoring());assertFalse(boyang.profile().redBlackScoring());assertFalse(anhua.ruleSet().canWin(0,java.util.List.of(),-1,new WordCardRuleContext(14,0)));assertTrue(anhua.ruleSet().canWin(0,java.util.List.of(),-1,new WordCardRuleContext(15,0)));}
}
