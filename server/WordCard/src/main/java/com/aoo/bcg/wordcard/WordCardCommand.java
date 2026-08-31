package com.aoo.bcg.wordcard;
import java.util.List;
public record WordCardCommand(WordCardOperation operation, int seatId, List<Integer> cards) {
    public WordCardCommand { if (operation == null || seatId < 0) throw new IllegalArgumentException("invalid word-card command"); cards = List.copyOf(cards == null ? List.of() : cards); }
    public static WordCardCommand of(WordCardOperation operation, int seatId, Integer... cards) { return new WordCardCommand(operation, seatId, List.of(cards)); }
}
