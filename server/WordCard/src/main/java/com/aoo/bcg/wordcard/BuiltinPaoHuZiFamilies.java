package com.aoo.bcg.wordcard;
import java.util.LinkedHashMap;import java.util.Map;import java.util.Set;
public final class BuiltinPaoHuZiFamilies{
    private static final Set<WordCardOperation> ALL=Set.of(WordCardOperation.values());private static final Map<String,PaoHuZiFamily> FAMILIES=create();private BuiltinPaoHuZiFamilies(){}
    public static Map<String,PaoHuZiFamily> all(){return FAMILIES;}public static PaoHuZiFamily require(String module){var result=FAMILIES.get(module==null?"":module.toUpperCase(java.util.Locale.ROOT));if(result==null)throw new IllegalArgumentException("unknown pao-hu-zi module: "+module);return result;}
    private static Map<String,PaoHuZiFamily> create(){Map<String,PaoHuZiFamily> map=new LinkedHashMap<>();
        add(map,"AHPHZ","anhua",true);add(map,"BYZP","boyang",false);add(map,"DYZP","daye",true);add(map,"GLZP","guilin",true);
        add(map,"HNDZP","hunan",true);add(map,"JSZP","jiangsu",false);add(map,"LCZP","linchuan",true);add(map,"LHZP","liuhe",true);
        add(map,"PXPHZ","pingxiang",true);add(map,"XPPHZ","xupu",true);add(map,"YCHP","yichang-huapai",false);add(map,"YZCHZ","yongzhou-chehuzi",true);
        return Map.copyOf(map);}
    private static void add(Map<String,PaoHuZiFamily> map,String module,String namespace,boolean redBlack){map.put(module,new PaoHuZiFamily(new PaoHuZiProfile(module,15,namespace,redBlack,ALL)));}
}
