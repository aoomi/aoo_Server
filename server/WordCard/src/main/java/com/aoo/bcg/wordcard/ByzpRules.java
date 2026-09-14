package com.aoo.bcg.wordcard;

import java.util.*;

/** BYZP-specific rules ported from BYZPRoomEnum/BYZPHuUtil, independent of generic word-card defaults. */
public final class ByzpRules {
    public enum WakeMode { UPPER, OWN, LOWER, FOUR_CHAIN }
    public record Config(WakeMode wakeMode,boolean oilContest,boolean mandatoryHu,boolean doubleShot,
            boolean ghostCard,boolean autoReady,boolean dissolveOnTrusteeship,int oilsPerPoint,int multiplier) {
        public Config { Objects.requireNonNull(wakeMode);if(oilsPerPoint!=3&&oilsPerPoint!=5)throw new IllegalArgumentException("BYZP oilsPerPoint must be 3 or 5");if(!Set.of(1,2,5,10).contains(multiplier))throw new IllegalArgumentException("invalid BYZP multiplier"); }
        public static Config standard(){return new Config(WakeMode.UPPER,false,false,false,false,false,false,5,1);}
    }
    private ByzpRules(){}
    /** Legacy deck: values 10..29, four physical copies each, plus the single 5101 flower. */
    public static List<Integer> deck(){List<Integer>cards=new ArrayList<>(81);for(int value=10;value<=29;value++)for(int copy=1;copy<=4;copy++)cards.add(value*100+copy);cards.add(5101);return List.copyOf(cards);}
    public static int type(int physicalCard){return physicalCard/100;}
    public static boolean isRed(int physicalCard){return Set.of(12,17,20,27).contains(type(physicalCard));}
    public static int wakeType(int indicator,WakeMode mode){int type=type(indicator);if(type<10||type>29)throw new IllegalArgumentException("normal BYZP indicator required");return switch(mode){case OWN->type;case UPPER->type==19?10:type==29?20:type+1;case LOWER->type==10?19:type==20?29:type-1;case FOUR_CHAIN->type;};}
    public static int wakeCount(Collection<Integer>cards,int indicator,WakeMode mode){int wake=wakeType(indicator,mode);int count=(int)cards.stream().filter(c->type(c)==wake).count();return mode==WakeMode.FOUR_CHAIN?count*4:count;}
    /** Exact 3n+2 evaluator with optional ghosts, porting the legacy recursive pair/triple/sequence search. */
    public static boolean canHu(Collection<Integer>physicalCards,int ghosts){if(ghosts<0)throw new IllegalArgumentException("negative ghosts");List<Integer>types=physicalCards.stream().map(ByzpRules::type).sorted().toList();if((types.size()+ghosts)%3!=2)return false;if(types.isEmpty())return ghosts>=2&&(ghosts-2)%3==0;Set<Integer>pairs=new LinkedHashSet<>(types);for(int pair:pairs){List<Integer>left=new ArrayList<>(types);left.remove(Integer.valueOf(pair));int g=ghosts;if(!left.remove(Integer.valueOf(pair))){if(g==0)continue;g--;}if(groups(left,g,new HashMap<>()))return true;}return ghosts>=2&&groups(new ArrayList<>(types),ghosts-2,new HashMap<>());}
    private static boolean groups(List<Integer>cards,int ghosts,Map<String,Boolean>memo){if(cards.isEmpty())return ghosts%3==0;String key=cards+":"+ghosts;Boolean known=memo.get(key);if(known!=null)return known;int first=cards.getFirst();boolean ok=take(cards,ghosts,List.of(first,first,first),memo)||take(cards,ghosts,sequence(first),memo);memo.put(key,ok);return ok;}
    private static boolean take(List<Integer>cards,int ghosts,List<Integer>need,Map<String,Boolean>memo){List<Integer>left=new ArrayList<>(cards);int missing=0;for(int type:need)if(!left.remove(Integer.valueOf(type)))missing++;return missing<=ghosts&&groups(left,ghosts-missing,memo);}
    private static List<Integer>sequence(int first){int rank=first%10;if(rank==0)return List.of(first,first+1,first+2);if(rank>=8)return List.of(first,first-1,first-2);return List.of(first,first+1,first+2);}
    public static long winnerScore(int oils,int wakeCount,Config config,boolean selfDraw){if(oils<0||wakeCount<0)throw new IllegalArgumentException("negative BYZP score input");long points=oils/config.oilsPerPoint()+wakeCount;if(selfDraw)points++;return Math.multiplyExact(points,config.multiplier());}
}
