package com.aoo.bcg.wordcard;
import java.util.*;
/** 溆浦跑胡子规则，移植 XPPHZHuXiEnum 与六种 hutype。 */
public final class XpphzRules{
 public record Config(int playerCount,int drawRemoval,int charge,boolean selfDrawMandatory,boolean oppositeCannotChi,boolean guChou,boolean scoreFloorZero){public Config{if(playerCount<2||playerCount>3)throw new IllegalArgumentException("XPPHZ supports 2/3 players");if(!Set.of(0,10,15,20).contains(drawRemoval)||charge<0||charge>4)throw new IllegalArgumentException("invalid XPPHZ room rule");}}
 public enum HuType{NONE,RED,BLACK,SIXTEEN_SMALL,ALL_TRIPLE,POINT_HU}
 private XpphzRules(){}
 /** 红/黑两色一至十，各四张。编码：色(1/2)、点数、物理副本。 */
 public static List<Integer>deck(){List<Integer>x=new ArrayList<>(80);for(int color=1;color<=2;color++)for(int rank=1;rank<=10;rank++)for(int copy=1;copy<=4;copy++)x.add(color*1000+rank*10+copy);return List.copyOf(x);}
 public static int color(int c){return c/1000;}public static int rank(int c){return c/10%100;}public static boolean red(int c){return color(c)==1;}
 public static int type(int c){return color(c)*100+rank(c);}public static boolean legalChi(Collection<Integer>hand,int exposed,Set<Integer>passed,boolean oppositeCannotChi,boolean fromOpposite){if(oppositeCannotChi&&fromOpposite||passed.contains(type(exposed)))return false;List<Integer>r=hand.stream().filter(c->color(c)==color(exposed)).map(XpphzRules::rank).toList();int n=rank(exposed);return r.contains(n-1)&&r.contains(n+1)||r.contains(n+1)&&r.contains(n+2)||Set.of(2,7,10).contains(n)&&Set.of(2,7,10).stream().filter(x->x!=n).allMatch(r::contains);}
 public static int meldHuXi(WordCardOperation op,int card){boolean r=red(card);return switch(op){case PENG->r?3:1;case WEI->r?6:3;case PAO->r?9:6;case TI->r?12:9;default->0;};}
 public static int chiHuXi(List<Integer>cards){if(cards.size()!=3)throw new IllegalArgumentException("three cards required");List<Integer>r=cards.stream().map(XpphzRules::rank).sorted().toList();boolean red=cards.stream().allMatch(XpphzRules::red);if(r.equals(List.of(2,7,10)))return red?6:3;if(r.get(1)==r.get(0)+1&&r.get(2)==r.get(1)+1&&r.get(2)==2)return red?6:3;return 0;}
 public static HuType huType(Collection<Integer>cards,boolean allTriples,boolean pointHu){long reds=cards.stream().filter(XpphzRules::red).count();if(pointHu)return HuType.POINT_HU;if(allTriples)return HuType.ALL_TRIPLE;if(cards.stream().filter(c->rank(c)<=5).count()>=16)return HuType.SIXTEEN_SMALL;if(reds==0)return HuType.BLACK;if(reds>=10)return HuType.RED;return HuType.NONE;}
 public static long score(int huXi,HuType type,int charge,int piao){if(huXi<15)throw new IllegalArgumentException("minimum 15 hu-xi");long base=Math.max(1,(huXi-12)/3);if(type!=HuType.NONE)base*=2;return base+charge+piao;}
 public static boolean canHu(Collection<Integer>cards,int huXi){return huXi>=15&&cards.size()%3==2;}public static HuType colorWin(Collection<Integer>cards){return huType(cards,false,false);}public static long winnerScore(int huXi,HuType type,int charge,int winnerPiao,int loserPiao){return score(huXi,type,charge,winnerPiao+loserPiao);}
}
