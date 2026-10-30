package com.aoo.bcg.mahjong;

import java.util.Map;

/** Data-driven implementations shared by region profiles; no region-code branches. */
final class MahjongRegionRulePolicies {
    private MahjongRegionRulePolicies() {}
    static int baseUnit(Map<String,Object> rules){int value=number(rules,"diFen",0);return switch(value){case 1->2;case 2->3;case 3->5;default->1;};}
    static long scoreCap(Map<String,Object> rules){int value=number(rules,"fengDing",3);return switch(value){case 0->30;case 1->50;case 2->100;default->Long.MAX_VALUE;};}
    static int nextWildcard(int indicator){if(indicator>=11&&indicator<=19)return indicator==19?11:indicator+1;if(indicator>=21&&indicator<=29)return indicator==29?21:indicator+1;if(indicator>=31&&indicator<=39)return indicator==39?31:indicator+1;if(indicator>=41&&indicator<=44)return indicator==44?41:indicator+1;if(indicator>=45&&indicator<=47)return indicator==47?45:indicator+1;throw new IllegalArgumentException("unsupported wildcard indicator");}
    static long applyUnitAndCap(long delta,Map<String,Object>rules){long scaled=Math.multiplyExact(delta,baseUnit(rules));long cap=scoreCap(rules);return Math.max(-cap,Math.min(cap,scaled));}
    private static int number(Map<String,Object>rules,String key,int fallback){return rules.get(key)instanceof Number n?n.intValue():fallback;}
}
