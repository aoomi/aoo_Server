package com.aoo.bcg.poker.cd299;

import java.util.*;

/** Deterministic, integer-unit settlement for XQP CXRoom's tail-first comparison model. */
public final class Cd299SettlementEngine {
    public record Player(int seat,long playerId,long base,long mango,long bet,boolean dropped,
            boolean threeFlower,Cd299HandEvaluator.SplitRank rank){
        public Player{if(seat<0||playerId<=0||base<0||mango<0||bet<0)throw new IllegalArgumentException("invalid CD299 settlement player");}
        long exposure(){return Math.addExact(base,bet);}
    }
    public record Result(Map<Long,Long> scoreDelta,long mangoPoolBefore,long mangoPool,List<Integer> tailWinnerSeats){
        public Result{scoreDelta=Map.copyOf(scoreDelta);tailWinnerSeats=List.copyOf(tailWinnerSeats);if(mangoPoolBefore<0||mangoPool<0||Math.addExact(scoreDelta.values().stream().mapToLong(Long::longValue).sum(),mangoPool-mangoPoolBefore)!=0)throw new IllegalArgumentException("CD299 settlement and mango pool must conserve value");}
    }
    public Result settle(List<Player> input,boolean headBigKeepsBase){
        return settle(input,headBigKeepsBase,0);
    }
    public Result settle(List<Player> input,boolean headBigKeepsBase,long carriedMangoPool){
        if(carriedMangoPool<0)throw new IllegalArgumentException("invalid carried CD299 mango pool");
        if(input==null||input.size()<2)throw new IllegalArgumentException("CD299 settlement needs players");
        List<Player> players=List.copyOf(input);Set<Integer>seats=new HashSet<>();Set<Long>ids=new HashSet<>();for(Player p:players)if(!seats.add(p.seat())||!ids.add(p.playerId()))throw new IllegalArgumentException("duplicate CD299 player");
        Map<Long,Long>delta=new LinkedHashMap<>();players.forEach(p->delta.put(p.playerId(),0L));
        List<Player> active=players.stream().filter(p->!p.dropped()&&!p.threeFlower()).toList();
        long mangoContributed=players.stream().mapToLong(Player::mango).sum();
        if(active.isEmpty()){players.forEach(p->delta.merge(p.playerId(),-p.mango(),Long::sum));return new Result(delta,carriedMangoPool,Math.addExact(carriedMangoPool,mangoContributed),List.of());}
        if(active.size()>1&&active.stream().anyMatch(p->p.rank()==null))throw new IllegalArgumentException("comparable CD299 players need split ranks");
        List<Player>tailWinners=active.size()==1?active:bestTailThenHead(active);
        long availableMango=Math.addExact(carriedMangoPool,mangoContributed);
        players.forEach(p->delta.merge(p.playerId(),-p.mango(),Long::sum));
        long mangoPayout=Math.min(availableMango,active.stream().mapToLong(Player::bet).max().orElse(0));
        Map<Player,Long>mangoAwards=cappedEqualAwards(tailWinners,mangoPayout);
        mangoAwards.forEach((p,value)->delta.merge(p.playerId(),value,Long::sum));
        Map<Integer,Long>lost=new HashMap<>();List<List<Player>>groups=rankGroups(active);
        for(int i=0;i<groups.size();i++){List<Player>winners=groups.get(i);long winnerCapacity=winners.stream().mapToLong(Player::exposure).sum();for(int j=i+1;j<groups.size();j++)for(Player loser:groups.get(j)){
            long remaining=Math.max(0,loser.exposure()-lost.getOrDefault(loser.seat(),0L));long amount=Math.min(winnerCapacity,remaining);
            if(headBigKeepsBase&&bestHead(active).contains(loser)&&!tailWinners.contains(loser))amount=Math.max(0,amount-loser.base());
            transferWeighted(delta,loser.playerId(),winners,amount);lost.merge(loser.seat(),amount,Long::sum);
        }}
        List<Player>dropped=players.stream().filter(Player::dropped).toList();for(Player loser:dropped)transferWeighted(delta,loser.playerId(),tailWinners,loser.exposure());
        return new Result(delta,carriedMangoPool,availableMango-mangoAwards.values().stream().mapToLong(Long::longValue).sum(),tailWinners.stream().map(Player::seat).toList());
    }
    static int compareTailFirst(Cd299HandEvaluator.SplitRank a,Cd299HandEvaluator.SplitRank b){int c=a.tail().compareTo(b.tail());return c!=0?c:a.head().compareTo(b.head());}
    private static List<Player>bestTailThenHead(List<Player>players){List<Player>sorted=new ArrayList<>(players);sorted.sort((a,b)->compareTailFirst(a.rank(),b.rank()));Player best=sorted.get(0);return sorted.stream().takeWhile(p->compareTailFirst(best.rank(),p.rank())==0).toList();}
    private static List<List<Player>>rankGroups(List<Player>players){List<Player>sorted=new ArrayList<>(players);sorted.sort((a,b)->compareTailFirst(a.rank(),b.rank()));List<List<Player>>out=new ArrayList<>();for(Player p:sorted){if(out.isEmpty()||compareTailFirst(out.getLast().getFirst().rank(),p.rank())!=0)out.add(new ArrayList<>());out.getLast().add(p);}return out;}
    private static List<Player>bestHead(List<Player>players){List<Player>sorted=new ArrayList<>(players);sorted.sort(Comparator.comparing(Player::rank,(a,b)->a.head().compareTo(b.head())));var best=sorted.get(0).rank().head();return sorted.stream().filter(p->best.compareTo(p.rank().head())==0).toList();}
    private static Map<Player,Long>cappedEqualAwards(List<Player>winners,long target){List<Player>ordered=winners.stream().sorted(Comparator.comparingInt(Player::seat)).toList();Map<Player,Long>awards=new LinkedHashMap<>();ordered.forEach(p->awards.put(p,0L));long remaining=target;while(remaining>0){List<Player>eligible=ordered.stream().filter(p->awards.get(p)<p.bet()).toList();if(eligible.isEmpty())break;long share=Math.max(1,remaining/eligible.size());for(Player p:eligible){long award=Math.min(Math.min(share,p.bet()-awards.get(p)),remaining);awards.merge(p,award,Long::sum);remaining-=award;if(remaining==0)break;}}return awards;}
    private static void transfer(Map<Long,Long>d,long from,long to,long amount){if(amount<=0)return;d.merge(from,-amount,Long::sum);d.merge(to,amount,Long::sum);}
    private static void transferWeighted(Map<Long,Long>d,long from,List<Player>winners,long amount){if(amount<=0||winners.isEmpty())return;long weight=winners.stream().mapToLong(Player::exposure).sum();if(weight<=0){transferShared(d,winners,amount,Map.of(from,-amount));return;}d.merge(from,-amount,Long::sum);long assigned=0;for(int i=1;i<winners.size();i++){long share=amount*winners.get(i).exposure()/weight;d.merge(winners.get(i).playerId(),share,Long::sum);assigned+=share;}d.merge(winners.getFirst().playerId(),amount-assigned,Long::sum);}
    private static void transferShared(Map<Long,Long>d,List<Player>winners,long amount,Map<Long,Long>pre){pre.forEach((id,v)->d.merge(id,v,Long::sum));if(amount<=0||winners.isEmpty())return;long each=amount/winners.size(),remainder=amount%winners.size();for(int i=0;i<winners.size();i++)d.merge(winners.get(i).playerId(),each+(i==0?remainder:0),Long::sum);}
}
