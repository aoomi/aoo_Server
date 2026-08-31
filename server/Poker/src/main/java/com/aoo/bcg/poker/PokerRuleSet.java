package com.aoo.bcg.poker;
import java.util.List;
public interface PokerRuleSet<T> { CardCombination recognize(List<Integer> cards, T state); boolean canBeat(CardCombination candidate, CardCombination previous, T state); List<CardCombination> hints(List<Integer> hand, CardCombination previous, T state); }
