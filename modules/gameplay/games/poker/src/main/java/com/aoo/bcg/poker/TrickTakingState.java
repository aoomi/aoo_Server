package com.aoo.bcg.poker;
import java.util.*;
public record TrickTakingState(Map<Integer,List<Integer>> hands,int currentSeat,int leaderSeat,Map<Integer,Integer> trick,Map<Integer,Integer> scores,boolean finished){public TrickTakingState{Map<Integer,List<Integer>> copied=new LinkedHashMap<>();hands.forEach((seat,cards)->copied.put(seat,List.copyOf(cards)));hands=Map.copyOf(copied);trick=Map.copyOf(trick==null?Map.of():trick);scores=Map.copyOf(scores==null?Map.of():scores);if(hands.size()<2||!hands.containsKey(currentSeat))throw new IllegalArgumentException("invalid trick-taking state");}}
