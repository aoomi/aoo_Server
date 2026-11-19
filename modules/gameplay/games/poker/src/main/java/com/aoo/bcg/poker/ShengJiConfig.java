package com.aoo.bcg.poker;
public record ShengJiConfig(int trumpSuit,int levelRank,int playerCount){public ShengJiConfig{if(trumpSuit<0||levelRank<=0||playerCount!=4)throw new IllegalArgumentException("upgrade currently requires four players");}}
