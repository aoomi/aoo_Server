package com.aoo.bcg.poker;
import java.util.List;
public interface PokerRuleSet<T> {
  CardCombination recognize(List<Integer> cards, T state);
  boolean canBeat(CardCombination candidate, CardCombination previous, T state);
}
