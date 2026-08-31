package com.aoo.bcg.poker;
import java.util.*;import java.util.stream.Collectors;
/** Source-backed 84-card Chongqing chuo-pai domain. */
final class CpRules{
 enum Type{PASS,SINGLE,PAIR,TRIPLE,FOUR,STRAIGHT,SHI_YAO_BA_CHANG}
 record Play(Type type,List<Integer>cards,int value,int chainLength){Play{cards=List.copyOf(cards);}}
 static List<Integer>deck(boolean ignored){List<Integer>d=new ArrayList<>(84);add(d,2,12);add(d,20,26);add(d,38,40);return List.copyOf(d);}private static void add(List<Integer>d,int a,int b){for(int r=a;r<=b;r++)for(int copy=1;copy<=4;copy++)d.add(r*100+copy);}
 static int rank(int c){return Math.floorDiv(c,100);}static int point510k(int c){return 0;}
 static Play classify(Type t,Collection<Integer>in){List<Integer>c=List.copyOf(in);if(c.isEmpty())throw new IllegalArgumentException("CP cards required");Map<Integer,Long>m=c.stream().collect(Collectors.groupingBy(CpRules::rank,TreeMap::new,Collectors.counting()));int min=m.keySet().stream().mapToInt(x->x).min().orElseThrow(),max=m.keySet().stream().mapToInt(x->x).max().orElseThrow();boolean seq=max-min+1==m.size();boolean ok=switch(t){case SINGLE->c.size()==1;case PAIR->c.size()==2&&m.size()==1;case TRIPLE->c.size()==3&&m.size()==1;case FOUR->c.size()==4&&m.size()==1;case STRAIGHT->c.size()>=5&&seq&&m.values().stream().allMatch(n->n==1);case SHI_YAO_BA_CHANG->c.size()>=8&&seq;case PASS->false;};if(!ok)throw new IllegalArgumentException("invalid CP "+t);return new Play(t,c,max,m.size());}
 static boolean beats(Play n,Play p){if(p==null)return true;if(n.type()==Type.SHI_YAO_BA_CHANG)return p.type()!=n.type()||n.cards().size()<p.cards().size()||n.value()>p.value();if(n.type()==Type.FOUR)return p.type()!=Type.FOUR&&p.type()!=Type.SHI_YAO_BA_CHANG||n.value()>p.value();return n.type()==p.type()&&n.cards().size()==p.cards().size()&&n.value()>p.value();}
 static int prize(Play p,boolean a,boolean b,boolean c){return p.type()==Type.FOUR?1:p.type()==Type.SHI_YAO_BA_CHANG?2:0;}
}
