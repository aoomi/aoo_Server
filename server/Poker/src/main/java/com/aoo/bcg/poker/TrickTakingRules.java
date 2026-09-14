package com.aoo.bcg.poker;
import java.util.Map;
public interface TrickTakingRules { int category(int card); int winner(Map<Integer,Integer> trick,int leaderSeat); int points(Map<Integer,Integer> trick); }
