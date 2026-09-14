package com.aoo.bcg.wordcard;
import java.util.*;

/** Yichang Shang-da-ren rules ported from YCSDRHuUtil/YCSDRRoomEnum/YCSDRCalcPosEnd. */
final class YcsdrRules{
 enum Pattern{PI_HU(1),TAI_HU(3),TAI_HU_FAN(6),QING_HU(2),KU_HU(4),HEI_HU(8),PENG_PENG_HU(1);final int factor;Pattern(int f){factor=f;}}
 private YcsdrRules(){}
 static List<Integer>deck(boolean flowers){List<Integer>d=new ArrayList<>();for(int suit=1;suit<=4;suit++)for(int rank=1;rank<=9&&suit*10+rank<=47;rank++)for(int copy=1;copy<=4;copy++)d.add((suit*10+rank)*100+copy);if(flowers)for(int type=51;type<=58;type++)d.add(type*100+1);return d;}
 static int type(int physical){return physical/100;}static boolean flower(int physical){return type(physical)>=51;}
 /** Legacy YCSDR pair rule: the two eyes share rank, even across different suits. */
 static boolean canHu(Collection<Integer>physical,int jins){List<Integer>types=physical.stream().filter(c->!flower(c)).map(YcsdrRules::type).sorted().toList();if((types.size()+jins)%3!=2)return false;Set<Integer>first=new LinkedHashSet<>(types);for(int a:first){List<Integer>left=new ArrayList<>(types);left.remove(Integer.valueOf(a));for(int b:new LinkedHashSet<>(left)){if(a%10!=b%10)continue;List<Integer>rest=new ArrayList<>(left);rest.remove(Integer.valueOf(b));if(groups(rest,jins,new HashMap<>()))return true;}if(jins>0&&groups(left,jins-1,new HashMap<>()))return true;}return jins>=2&&groups(new ArrayList<>(types),jins-2,new HashMap<>());}
 private static boolean groups(List<Integer>c,int j,Map<String,Boolean>memo){if(c.isEmpty())return j%3==0;String key=c+":"+j;Boolean known=memo.get(key);if(known!=null)return known;int x=c.getFirst();boolean ok=take(c,j,List.of(x,x,x),memo)||x%10<=7&&take(c,j,List.of(x,x+1,x+2),memo);memo.put(key,ok);return ok;}
 private static boolean take(List<Integer>c,int j,List<Integer>need,Map<String,Boolean>memo){List<Integer>left=new ArrayList<>(c);int miss=0;for(int n:need)if(!left.remove(Integer.valueOf(n)))miss++;return miss<=j&&groups(left,j-miss,memo);}
 static long huPoint(Pattern pattern,int huShu,int mao,int kan,boolean selfDraw,boolean ziMoYouXi){if(huShu<0||mao<0||kan<0)throw new IllegalArgumentException("negative YCSDR score");long point=(long)Math.max(1,huShu)*pattern.factor+mao*2L+kan;if(selfDraw&&ziMoYouXi)point++;return point;}
 static boolean canPeng(Collection<Integer>cards,int incoming){return cards.stream().filter(c->type(c)==type(incoming)).count()>=2;}static boolean canGang(Collection<Integer>cards,int incoming){return cards.stream().filter(c->type(c)==type(incoming)).count()>=3;}
}
