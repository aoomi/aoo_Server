package com.aoo.bcg.wordcard;

import java.util.*;

/**萍乡跑胡子规则。牌值、红牌、胡息和名堂来自旧 PXPHZ 的 CardCfg/HuXiEnum/hutype。*/
public final class PxphzRules {
    public record Config(int playerCount,int drawRemoval,int charge,boolean selfDrawMandatory,
            boolean oppositeCannotChi,boolean guChou,boolean scoreFloorZero) {
        public Config { if(playerCount<2||playerCount>3)throw new IllegalArgumentException("PXPHZ supports 2/3 players");if(!Set.of(0,10,15,20).contains(drawRemoval))throw new IllegalArgumentException("invalid chou-pai");if(charge<0||charge>4)throw new IllegalArgumentException("invalid chong"); }
        public static Config standard(){return new Config(2,0,0,false,false,false,false);}
    }
    public enum ColorWin { NONE, RED, BLACK }
    private static final Set<Integer> RED=Set.of(2,7,10,102,107,110);
    private PxphzRules(){}
    /**小一至小十、大壹至大拾，每种四张，严格 80 张。*/
    public static List<Integer> deck(){List<Integer>x=new ArrayList<>(80);for(int suit:new int[]{0,100})for(int rank=1;rank<=10;rank++)for(int copy=1;copy<=4;copy++)x.add((suit+rank)*10+copy);return List.copyOf(x);}
    public static int type(int card){return card/10;}
    public static boolean red(int card){return RED.contains(type(card));}
    public static ColorWin colorWin(Collection<Integer>cards){long red=cards.stream().filter(PxphzRules::red).count();return red==0?ColorWin.BLACK:red>=10?ColorWin.RED:ColorWin.NONE;}
    public static boolean legalChi(Collection<Integer>hand,int exposed,Set<Integer>passedChi,boolean oppositeCannotChi,boolean fromOpposite){if(oppositeCannotChi&&fromOpposite||passedChi.contains(type(exposed)))return false;List<Integer>types=hand.stream().map(PxphzRules::type).toList();int t=type(exposed),rank=t%100;return contains(types,t-1,t+1)||contains(types,t+1,t+2)||(Set.of(2,7,10).contains(rank)&&Set.of(2,7,10).stream().filter(r->r!=rank).allMatch(r->types.contains(t-rank+r)));}
    private static boolean contains(List<Integer>x,int a,int b){return x.contains(a)&&x.contains(b);}
    public static int meldHuXi(WordCardOperation op,int card){boolean big=type(card)>100;return switch(op){case CHI->0;case PENG->big?3:1;case WEI->big?6:3;case PAO->big?9:6;case TI->big?12:9;default->0;};}
    public static boolean canHu(Collection<Integer>cards,int huXi){return huXi>=18&&cards.size()%3==2&&groups(cards.stream().map(PxphzRules::type).sorted().toList());}
    private static boolean groups(List<Integer>types){if(types.size()==2)return Objects.equals(types.get(0),types.get(1));int f=types.get(0);List<Integer>a=new ArrayList<>(types);if(remove(a,f,f,f)&&groups(a))return true;a=new ArrayList<>(types);int r=f%100;if(r<=8&&remove(a,f,f+1,f+2)&&groups(a))return true;if(Set.of(2,7,10).contains(r)){a=new ArrayList<>(types);int base=f-r;if(remove(a,base+2,base+7,base+10)&&groups(a))return true;}return false;}
    private static boolean remove(List<Integer>x,int...v){for(int n:v)if(!x.remove(Integer.valueOf(n)))return false;return true;}
    public static long winnerScore(int huXi,ColorWin color,int charge,int winnerPiao,int loserPiao){long tun=Math.max(1,(huXi-15)/3);if(color!=ColorWin.NONE)tun*=2;return tun+charge+winnerPiao+loserPiao;}
}
