package com.aoo.bcg.poker;
import java.util.*;
record HbpdkOptions(int players,int cardNum,int xianchu,Set<Integer> kexuan,int bombAlgorithm,int bombScore){
 HbpdkOptions{if(players<2||players>3||cardNum<0||cardNum>2||xianchu<0||xianchu>3||bombAlgorithm<0||bombAlgorithm>2||bombScore<0||bombScore>1)throw new IllegalArgumentException("invalid HBPDK option");kexuan=Set.copyOf(kexuan);if(kexuan.stream().anyMatch(v->!Set.of(4,11,13).contains(v)))throw new IllegalArgumentException("unsupported HBPDK option");}
 static HbpdkOptions from(Map<String,?>r){return new HbpdkOptions(i(r.get("playerNum"),i(r.get("seatLimit"),3)),i(r.get("cardNum"),2),i(r.get("xianchu"),1),set(r.get("kexuanwanfa")),i(r.get("zhadansuanfa"),1),i(r.get("zhadanfenshu"),0));}
 int handSize(){return switch(cardNum){case 0->12;case 1->15;default->16;};}
 Map<String,Object>map(){return Map.of("playerNum",players,"cardNum",cardNum,"xianchu",xianchu,"kexuanwanfa",List.copyOf(kexuan),"zhadansuanfa",bombAlgorithm,"zhadanfenshu",bombScore);}
 private static int i(Object v,int d){return v instanceof Number n?n.intValue():d;}private static Set<Integer>set(Object v){if(v==null)return Set.of(4,11,13);if(!(v instanceof Collection<?>c))throw new IllegalArgumentException("HBPDK list required");Set<Integer>s=new LinkedHashSet<>();for(Object x:c)s.add(i(x,-1));return s;}
}
