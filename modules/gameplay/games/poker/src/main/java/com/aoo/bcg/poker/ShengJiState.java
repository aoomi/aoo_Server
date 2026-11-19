package com.aoo.bcg.poker;
import java.util.*;
public record ShengJiState(Map<Integer,List<Integer>> hands,int dealerSeat,int currentSeat,int trickLeader,Map<Integer,Integer> trick,int defenderPoints,boolean finished){public ShengJiState{Map<Integer,List<Integer>> copied=new LinkedHashMap<>();hands.forEach((seat,cards)->copied.put(seat,List.copyOf(cards)));hands=Map.copyOf(copied);trick=Map.copyOf(trick==null?Map.of():trick);if(hands.size()!=4||!hands.containsKey(currentSeat)||defenderPoints<0)throw new IllegalArgumentException("invalid upgrade state");}}
