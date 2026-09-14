package com.aoo.bcg.wordcard;
import java.util.List;
import java.util.Set;
public interface WordCardRuleSet<T> { Set<WordCardOperation> allowedOperations(int seatId, T state); boolean canWin(int seatId, List<Integer> cards, int incomingCard, T state); }
