package com.aoo.bcg.poker.cd299;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class Cd299SettlementEngineTest {
 private final Cd299HandEvaluator e=new Cd299HandEvaluator(); private final Cd299SettlementEngine engine=new Cd299SettlementEngine();
 private Cd299HandEvaluator.SplitRank split(int a,int b,int c,int d){return e.split(List.of(a,b,c,d),true);}
 private Cd299SettlementEngine.Player p(int seat,long id,long base,long mango,long bet,boolean drop,boolean flower,Cd299HandEvaluator.SplitRank rank){return new Cd299SettlementEngine.Player(seat,id,base,mango,bet,drop,flower,rank);}
 @Test void strongestTailWinsMangoAndCompetitiveExposure(){var strong=split(203,506,212,412);var weak=split(202,204,107,307);var r=engine.settle(List.of(p(0,10,1,3,6,false,false,strong),p(1,11,1,3,6,false,false,weak)),false);assertEquals(0,r.scoreDelta().values().stream().mapToLong(Long::longValue).sum());assertTrue(r.scoreDelta().get(10L)>0);assertEquals(List.of(0),r.tailWinnerSeats());}
 @Test void equalHeadAndTailShareMangoWithoutEatingEachOther(){var rank=split(203,506,212,412);var r=engine.settle(List.of(p(0,10,1,3,6,false,false,rank),p(1,11,1,3,6,false,false,rank)),false);assertEquals(0,r.scoreDelta().get(10L));assertEquals(0,r.scoreDelta().get(11L));}
 @Test void lossIsCappedBySmallerExposure(){var strong=split(203,506,212,412);var weak=split(202,204,107,307);var r=engine.settle(List.of(p(0,10,1,0,2,false,false,strong),p(1,11,1,0,20,false,false,weak)),false);assertEquals(3,r.scoreDelta().get(10L));assertEquals(-3,r.scoreDelta().get(11L));}
 @Test void droppedPlayerPaysTailWinnerAndThreeFlowerRefundsBetButStillContributesMango(){var rank=split(203,506,212,412);var r=engine.settle(List.of(p(0,10,1,3,6,false,false,rank),p(1,11,1,3,6,true,false,null),p(2,12,1,3,6,false,true,null)),false);assertEquals(-r.mangoPool(),r.scoreDelta().values().stream().mapToLong(Long::longValue).sum());assertTrue(r.scoreDelta().get(10L)>0);assertEquals(-3,r.scoreDelta().get(12L));}
 @Test void allThreeFlowerPlayersMoveMangoIntoPersistentPool(){var r=engine.settle(List.of(p(0,10,1,3,6,false,true,null),p(1,11,1,3,6,false,true,null)),false);assertEquals(-3,r.scoreDelta().get(10L));assertEquals(-3,r.scoreDelta().get(11L));assertEquals(6,r.mangoPool());}
 @Test void carriedMangoPoolPaysOnlyUpToMaxBetAndPersistsRemainder(){var strong=split(203,506,212,412);var weak=split(202,204,107,307);var r=engine.settle(List.of(p(0,10,1,0,4,false,false,strong),p(1,11,1,0,4,false,false,weak)),false,10);assertEquals(6,r.mangoPool());assertEquals(10,r.mangoPoolBefore());assertEquals(4,r.scoreDelta().values().stream().mapToLong(Long::longValue).sum());}
 @Test void tiedWinnersSplitDroppedExposureByTheirOwnStakeAndFirstGetsRemainder(){var tied=split(203,506,212,412);var r=engine.settle(List.of(p(0,10,1,0,2,false,false,tied),p(1,11,1,0,8,false,false,tied),p(2,12,1,0,10,true,false,null)),false);assertEquals(3,r.scoreDelta().get(10L));assertEquals(8,r.scoreDelta().get(11L));assertEquals(-11,r.scoreDelta().get(12L));}
 @Test void eachLoserCannotLoseBeyondOwnExposureAcrossSeveralWinnerGroups(){var top=split(203,506,212,412);var middle=split(212,412,202,109);var low=split(202,204,107,307);var r=engine.settle(List.of(p(0,10,1,0,20,false,false,top),p(1,11,1,0,20,false,false,middle),p(2,12,1,0,2,false,false,low)),false);assertTrue(r.scoreDelta().get(12L)>=-3);assertEquals(0,r.scoreDelta().values().stream().mapToLong(Long::longValue).sum());}
}
