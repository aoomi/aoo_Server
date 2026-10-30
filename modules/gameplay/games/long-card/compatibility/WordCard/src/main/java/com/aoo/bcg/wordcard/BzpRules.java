package com.aoo.bcg.wordcard;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Bao-zi-pai card authority ported from legacy BZPRoomEnum and bzp/optype implementations.
 * Cards retain the legacy hexadecimal encoding: low nibble is rank, high nibble is suit.
 */
final class BzpRules {
 enum Type { PASS(1), SINGLE(2), STRAIGHT(3), PAIR(4), TRIPLE_BOMB(5), LINKED_PAIRS(6), DOU(9), AIRPLANE(10), ROLLING_DRAGON(11); final int wire;Type(int wire){this.wire=wire;} }
 record Play(Type type,List<Integer> cards,int comparisonValue,int chainLength){Play{cards=List.copyOf(cards);}}
 private BzpRules(){}
 static int rank(int card){int raw=card&0x0f;return raw==1?14:raw==2?15:raw;}
 static int suit(int card){return(card>>>4)&0x0f;}
 static boolean trump(int card){return rank(card)>=17;}
 static int point510k(int card){return switch(card&0x0f){case 5->5;case 10,13->10;default->0;};}
 static int fourBombPrize(int size){if(size<=4)return 0;return 1<<Math.min(size-5,4);}
 static int eightBombPrize(int size){if(size<=4)return 0;if(size==10)return 24;if(size>=11)return 32;return 1<<(size-5);}
 static int kingBombPrize(int size){return switch(size){case 4->8;case 5->16;case 6->24;default->size>=7?32:0;};}
 static int prize510k(int size){return size==3?1:size==4?4:size>=5?16:0;}
 static Play classify(Type declared,Collection<Integer> input){if(input==null||input.isEmpty())throw new IllegalArgumentException("BZP cards required");List<Integer>cards=List.copyOf(input);Map<Integer,Long>counts=cards.stream().collect(Collectors.groupingBy(BzpRules::rank,TreeMap::new,Collectors.counting()));int min=counts.keySet().iterator().next();return switch(declared){
  case SINGLE->{require(cards.size()==1,"single");yield new Play(declared,cards,rank(cards.getFirst()),1);}
  case PAIR->{require(cards.size()==2&&counts.size()==1,"pair");yield new Play(declared,cards,min,1);}
  case TRIPLE_BOMB->{require(cards.size()==3&&counts.size()==1,"triple bomb");yield new Play(declared,cards,min,1);}
  case STRAIGHT->{require(cards.size()>=3&&!counts.containsKey(15)&&counts.values().stream().allMatch(n->n==1)&&continuous(counts.keySet()),"straight");yield new Play(declared,cards,min,cards.size());}
  case LINKED_PAIRS->{require(cards.size()>=4&&cards.size()%2==0&&counts.values().stream().allMatch(n->n==2)&&continuous(counts.keySet()),"linked pairs");yield new Play(declared,cards,min,counts.size());}
  case DOU->{require(cards.size()>=4&&counts.size()==1,"dou");yield new Play(declared,cards,min,1);}
  case AIRPLANE->{require(cards.size()>=6&&cards.size()%3==0&&counts.values().stream().allMatch(n->n==3)&&continuous(counts.keySet()),"airplane");yield new Play(declared,cards,min,counts.size());}
  case ROLLING_DRAGON->{require(cards.size()>=8&&counts.size()>=2&&counts.values().stream().distinct().count()==1&&counts.values().iterator().next()>=4&&continuous(counts.keySet()),"rolling dragon");yield new Play(declared,cards,min,counts.size());}
  case PASS->throw new IllegalArgumentException("pass has no cards");};}
 static boolean beats(Play next,Play previous){Objects.requireNonNull(next);if(previous==null)return true;if(next.type==previous.type){if(next.type==Type.DOU||next.type==Type.ROLLING_DRAGON)return next.cards.size()>previous.cards.size()||next.cards.size()==previous.cards.size()&&next.comparisonValue>previous.comparisonValue;return next.cards.size()==previous.cards.size()&&next.comparisonValue>previous.comparisonValue;}return power(next.type)>power(previous.type);}
 private static int power(Type t){return switch(t){case ROLLING_DRAGON->8;case AIRPLANE->7;case DOU->6;case LINKED_PAIRS->5;case TRIPLE_BOMB->4;default->1;};}
 private static boolean continuous(Collection<Integer> values){int last=-1;for(int v:values){if(last>=0&&v!=last+1)return false;last=v;}return true;}
 private static void require(boolean valid,String type){if(!valid)throw new IllegalArgumentException("invalid BZP "+type);}
}
