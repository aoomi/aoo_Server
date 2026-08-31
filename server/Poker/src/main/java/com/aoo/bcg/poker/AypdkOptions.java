package com.aoo.bcg.poker;

import java.util.*;

record AypdkOptions(int chupai, int heitaosanbichu, int zhadan, int daxiaoguan,
        Set<Integer> paixing, Set<Integer> teshu) {
    AypdkOptions {
        if (chupai < 0 || chupai > 2 || heitaosanbichu < 0 || heitaosanbichu > 1
                || zhadan < 0 || zhadan > 2 || daxiaoguan < 0 || daxiaoguan > 1)
            throw new IllegalArgumentException("invalid AYPDK option");
        paixing = validated(paixing, 0, 5, "paixing");
        teshu = validated(teshu, 0, 0, "teshu");
    }
    static AypdkOptions from(Map<String,?> rules) {
        return new AypdkOptions(integer(rules.get("chupai"),0), integer(rules.get("heitaosanbichu"),0),
                integer(rules.get("zhadan"),0), integer(rules.get("daxiaoguan"),0),
                integers(rules.get("paixing"), Set.of(0)), integers(rules.get("teshu"), Set.of()));
    }
    Map<String,Object> toMap() { return Map.of("chupai",chupai,"heitaosanbichu",heitaosanbichu,
            "zhadan",zhadan,"daxiaoguan",daxiaoguan,"paixing",List.copyOf(paixing),"teshu",List.copyOf(teshu)); }
    private static int integer(Object value,int fallback){return value instanceof Number n?n.intValue():fallback;}
    private static Set<Integer> integers(Object value,Set<Integer> fallback){if(value==null)return fallback;if(!(value instanceof Collection<?>c))throw new IllegalArgumentException("AYPDK option list required");Set<Integer>out=new LinkedHashSet<>();for(Object item:c)out.add(integer(item,-1));return out;}
    private static Set<Integer> validated(Set<Integer> values,int min,int max,String name){Set<Integer>copy=Set.copyOf(values);if(copy.stream().anyMatch(v->v<min||v>max))throw new IllegalArgumentException("invalid AYPDK "+name);return copy;}
}
