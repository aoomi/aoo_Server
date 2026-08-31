package com.aoo.bcg.common.turn;

import java.util.List;

/** Single authority for dealer and operation seat across deal, play, settlement and recovery. */
public final class RoundSeatAuthority {
    public enum Phase { WAITING, PLAYING, SETTLING }
    private final List<Integer> seats;
    private int dealerSeat;
    private int operationSeat;
    private long revision;
    private Phase phase;

    public RoundSeatAuthority(List<Integer> seats, int dealerSeat) { this(seats, dealerSeat, dealerSeat, 0, Phase.WAITING); }
    private RoundSeatAuthority(List<Integer> seats, int dealerSeat, int operationSeat, long revision, Phase phase) {
        this.seats = List.copyOf(seats == null ? List.of() : seats);
        if (this.seats.size() < 2 || this.seats.stream().distinct().count() != this.seats.size()
                || !this.seats.contains(dealerSeat) || !this.seats.contains(operationSeat) || revision < 0 || phase == null)
            throw new IllegalArgumentException("invalid round seat authority");
        this.dealerSeat = dealerSeat; this.operationSeat = operationSeat; this.revision = revision; this.phase = phase;
    }
    public static RoundSeatAuthority restore(List<Integer> seats, int dealerSeat, int operationSeat, long revision, Phase phase) {
        return new RoundSeatAuthority(seats, dealerSeat, operationSeat, revision, phase);
    }
    public synchronized int beginDeal() { require(Phase.WAITING); operationSeat = dealerSeat; phase = Phase.PLAYING; revision = increment(); return operationSeat; }
    public synchronized int advanceFrom(int actorSeat) {
        require(Phase.PLAYING); if (actorSeat != operationSeat) throw new IllegalStateException("not operation seat");
        operationSeat = seats.get((seats.indexOf(actorSeat) + 1) % seats.size()); revision = increment(); return operationSeat;
    }
    public synchronized void beginSettlement() { require(Phase.PLAYING); phase = Phase.SETTLING; revision = increment(); }
    public synchronized void nextRound(int nextDealerSeat) {
        require(Phase.SETTLING); if (!seats.contains(nextDealerSeat)) throw new IllegalArgumentException("dealer is not seated");
        dealerSeat = nextDealerSeat; operationSeat = nextDealerSeat; phase = Phase.WAITING; revision = increment();
    }
    public synchronized int dealerSeat() { return dealerSeat; }
    public synchronized int operationSeat() { return operationSeat; }
    public synchronized long revision() { return revision; }
    public synchronized Phase phase() { return phase; }
    private void require(Phase expected) { if (phase != expected) throw new IllegalStateException("expected " + expected + " but was " + phase); }
    private long increment() { return com.aoo.bcg.common.math.ExactDomainMath.increment(revision, "seat authority revision"); }
}
