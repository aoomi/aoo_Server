package com.aoo.bcg.poker.cd299;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exact 510042/510043/510047 evaluator. Lower comparison value means the stronger pair. */
public final class Cd299HandEvaluator {
    private static final int[] TYPES=load("/cd299/card-type-order.txt"), SINGLES=load("/cd299/single-card-order.txt"), FLOWERS=load("/cd299/three-flower.txt");
    public record PairRank(int type,int point,int singleOrder,List<Integer>cards,int nameKey) implements Comparable<PairRank>{
        public PairRank{cards=List.copyOf(cards);if(cards.size()!=2)throw new IllegalArgumentException("CD299 pair requires two cards");}
        public int compareTo(PairRank other){int c=Integer.compare(type,other.type);if(c!=0)return c;c=Integer.compare(other.point,point);if(c!=0)return c;if(point==0)return 0;return Integer.compare(singleOrder,other.singleOrder);}
    }
    public record SplitRank(PairRank head,PairRank tail) implements Comparable<SplitRank>{
        public SplitRank{if(head.compareTo(tail)<0)throw new IllegalArgumentException("CD299 head cannot outrank tail");}
        public int compareTo(SplitRank other){int c=tail.compareTo(other.tail);return c!=0?c:head.compareTo(other.head);}
    }
    public PairRank pair(List<Integer>cards,boolean earthNineKing){
        if(cards==null||cards.size()!=2||cards.get(0).equals(cards.get(1)))throw new IllegalArgumentException("invalid CD299 pair");
        for(int i=0;i<TYPES.length;i+=8){if(!earthNineKing&&TYPES[i]==10)continue;Integer first=null;for(int card:cards){for(int z=i+2;z<i+8;z+=3)if(value(card)==TYPES[z+2]&&(suit(card)==TYPES[z]||suit(card)==TYPES[z+1])){if(first!=null)return new PairRank(TYPES[i],0,0,List.of(first,card),TYPES[i+1]);first=card;break;}}}
        List<int[]>found=new ArrayList<>();for(int i=0;i<SINGLES.length;i+=2)for(int c:cards)if(c==SINGLES[i+1]){found.add(new int[]{c,SINGLES[i]});break;}
        if(found.size()!=2)throw new IllegalArgumentException("card absent from CD299 510043 order: "+cards);
        int best=-1,order=-1,a=0,b=0;for(int i=0;i<found.size();i++)for(int j=i+1;j<found.size();j++){int p=(value(found.get(i)[0])+value(found.get(j)[0]))%10;if(p>best){best=p;order=found.get(i)[1];a=found.get(i)[0];b=found.get(j)[0];}}
        return new PairRank(100,best,order,List.of(a,b),0);
    }
    public SplitRank split(List<Integer>ordered,boolean earthNineKing){if(ordered==null||ordered.size()!=4||new HashSet<>(ordered).size()!=4)throw new IllegalArgumentException("invalid CD299 split");PairRank a=pair(ordered.subList(0,2),earthNineKing),b=pair(ordered.subList(2,4),earthNineKing);return a.compareTo(b)<=0?new SplitRank(b,a):new SplitRank(a,b);}
    public boolean isThreeFlower(List<Integer>cards){if(cards==null||cards.size()<3)return false;for(int i=0;i<FLOWERS.length;i+=9){boolean ok=true;for(int z=i;z<i+9;z+=3){boolean hit=false;for(int c:cards)if(value(c)==FLOWERS[z+2]&&(suit(c)==FLOWERS[z]||suit(c)==FLOWERS[z+1])){hit=true;break;}if(!hit){ok=false;break;}}if(ok){if(cards.size()>3){Set<Integer>s=new HashSet<>(cards);if(pairPresent(s,110,310)||pairPresent(s,210,410)||pairPresent(s,106,306)||pairPresent(s,206,406)||pairPresent(s,203,506)||pairPresent(s,111,311))return false;}return true;}}return false;}
    private static boolean pairPresent(Set<Integer>s,int a,int b){return s.contains(a)&&s.contains(b);}private static int suit(int c){return c/100;}private static int value(int c){return c%100;}
    private static int[]load(String path){try(var in=Cd299HandEvaluator.class.getResourceAsStream(path)){if(in==null)throw new IllegalStateException("missing "+path);String t=new String(in.readAllBytes(),StandardCharsets.UTF_8).trim();return Arrays.stream(t.split(",")).mapToInt(Integer::parseInt).toArray();}catch(IOException e){throw new IllegalStateException("cannot load "+path,e);}}
}
