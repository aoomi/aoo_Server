package com.aoo.bcg.wordcard;
import java.util.Set;
public record PaoHuZiProfile(String moduleCode,int minimumHuXi,String mingTangNamespace,
        boolean redBlackScoring,Set<WordCardOperation> allowedOperations){
    public PaoHuZiProfile{if(moduleCode==null||moduleCode.isBlank()||minimumHuXi<0||mingTangNamespace==null||mingTangNamespace.isBlank()||allowedOperations==null||allowedOperations.isEmpty())throw new IllegalArgumentException("invalid pao-hu-zi profile");moduleCode=moduleCode.toUpperCase(java.util.Locale.ROOT);allowedOperations=Set.copyOf(allowedOperations);}
}
