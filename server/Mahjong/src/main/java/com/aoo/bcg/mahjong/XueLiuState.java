package com.aoo.bcg.mahjong;
import java.util.Map;
public record XueLiuState(Map<Integer,Integer> winCounts, boolean finished) { public XueLiuState { winCounts=Map.copyOf(winCounts); } }
