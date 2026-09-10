package com.aoo.bcg.poker;

import java.util.List;
import java.util.Objects;

/** Immutable, version-bound rule snapshot used by dealing, turns and settlement. */
public record PokerRuleProfile(String version, int deckSize, int minimumPlayers, int maximumPlayers,
        FirstLead firstLead, Integer requiredFirstCard, int minimumStraightLength, int minimumPairRunLength,
        boolean allowTwoInRuns, boolean allowJokersInRuns, boolean mustBeatWhenPossible,
        boolean forceHighestSingleAgainstReportedSingle, int bombUnit, int multiplierCap, int minimumPlaneLength, int maximumHandSize,
        List<Integer> canonicalDeck) {
    public enum FirstLead { REQUIRED_CARD_HOLDER, MINIMUM_CARD_HOLDER, PREVIOUS_WINNER, RANDOM, ROOM_OWNER }

    public PokerRuleProfile {
        if (version == null || version.isBlank()) throw new IllegalArgumentException("version required");if(deckSize<8||deckSize>54)throw new IllegalArgumentException("deck size must be between 8 and 54");
        canonicalDeck=List.copyOf(canonicalDeck);if(canonicalDeck.size()!=deckSize||new java.util.HashSet<>(canonicalDeck).size()!=deckSize)throw new IllegalArgumentException("invalid canonical deck schema");canonicalDeck.forEach(PokerCardCodec::validate);
        if (minimumPlayers < 2 || maximumPlayers > 4 || minimumPlayers > maximumPlayers) throw new IllegalArgumentException("invalid player range");
        Objects.requireNonNull(firstLead);
        if (minimumStraightLength < 3 || minimumPairRunLength < 2 || minimumPlaneLength < 2 || maximumHandSize < 1 || bombUnit < 0 || multiplierCap < 1) throw new IllegalArgumentException("invalid rule boundary");
        if (firstLead == FirstLead.REQUIRED_CARD_HOLDER && requiredFirstCard == null) throw new IllegalArgumentException("required lead card missing");
        if (requiredFirstCard != null && !canonicalDeck.contains(requiredFirstCard)) throw new IllegalArgumentException("required card absent from deck");
    }
    public PokerRuleProfile(String version,int deckSize,int minimumPlayers,int maximumPlayers,FirstLead firstLead,Integer requiredFirstCard,int minimumStraightLength,int minimumPairRunLength,boolean allowTwoInRuns,boolean allowJokersInRuns,boolean mustBeatWhenPossible,boolean forceHighestSingleAgainstReportedSingle,int bombUnit,int multiplierCap,int minimumPlaneLength,int maximumHandSize){this(version,deckSize,minimumPlayers,maximumPlayers,firstLead,requiredFirstCard,minimumStraightLength,minimumPairRunLength,allowTwoInRuns,allowJokersInRuns,mustBeatWhenPossible,forceHighestSingleAgainstReportedSingle,bombUnit,multiplierCap,minimumPlaneLength,maximumHandSize,PokerCardCodec.deck(deckSize));}
    public List<Integer> deck(){return canonicalDeck;}

    public void validatePlayerCount(int players) {
        validatePlayerCount(players, 0);
    }

    public void validatePlayerCount(int players, int cardsPerPlayer) {
        if (players < minimumPlayers || players > maximumPlayers) throw new IllegalStateException("player count outside rule snapshot");
        if (cardsPerPlayer < 0) throw new IllegalStateException("cards per player is invalid");
        if (cardsPerPlayer == 0 && deckSize % players != 0) throw new IllegalStateException("deck cannot be dealt evenly");
        int handSize = cardsPerPlayer == 0 ? deckSize / players : cardsPerPlayer;
        if ((long) handSize * players > deckSize) throw new IllegalStateException("deck does not contain enough cards");
        if (handSize > maximumHandSize) throw new IllegalStateException("hand exceeds complete-hint budget");
    }

    public void validateRun(List<Integer> ranks, boolean pairs) {
        int minimum = pairs ? minimumPairRunLength : minimumStraightLength;
        if (ranks.size() < minimum) throw new IllegalArgumentException("run is too short");
        for (int rank : ranks) {
            if ((!allowTwoInRuns && rank == 15) || (!allowJokersInRuns && rank >= 16)) throw new IllegalArgumentException("rank excluded from run");
        }
        for (int i = 1; i < ranks.size(); i++) if (ranks.get(i) != ranks.get(i - 1) + 1) throw new IllegalArgumentException("run is not consecutive");
    }

    public static PokerRuleProfile paoDeKuai(String version, int deckSize, Integer requiredFirstCard) {
        int players=switch(deckSize){case 48,54->3;case 52->4;default->throw new IllegalArgumentException("unsupported deck");};return new PokerRuleProfile(version, deckSize, players, players,
                requiredFirstCard == null ? FirstLead.RANDOM : FirstLead.REQUIRED_CARD_HOLDER,
                requiredFirstCard, 5, 2, false, false, true, true, 1, 16, 2, 20,PokerCardCodec.deck(deckSize));
    }
    public static PokerRuleProfile fromConfig(String version,int deckSize,PaoDeKuaiConfig c){PokerRuleProfile b=paoDeKuai(version,deckSize,c.requiredFirstCard());return new PokerRuleProfile(b.version(),b.deckSize(),b.minimumPlayers(),b.maximumPlayers(),b.firstLead(),b.requiredFirstCard(),c.minimumStraightLength(),c.minimumPairRunLength(),b.allowTwoInRuns(),b.allowJokersInRuns(),b.mustBeatWhenPossible(),c.forceHighestSingleAgainstReportedSingle(),b.bombUnit(),b.multiplierCap(),b.minimumPlaneLength(),b.maximumHandSize(),b.deck());}
    public PokerRuleProfile withDeck(List<Integer> deck){return new PokerRuleProfile(version,deck.size(),minimumPlayers,maximumPlayers,firstLead,requiredFirstCard,minimumStraightLength,minimumPairRunLength,allowTwoInRuns,allowJokersInRuns,mustBeatWhenPossible,forceHighestSingleAgainstReportedSingle,bombUnit,multiplierCap,minimumPlaneLength,maximumHandSize,deck);}
}
