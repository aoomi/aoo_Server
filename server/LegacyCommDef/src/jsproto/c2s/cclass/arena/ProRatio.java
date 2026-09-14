package jsproto.c2s.cclass.arena;

public class ProRatio {
    private int roundId;
    private int number;

    public ProRatio() {
        super();
    }

    public ProRatio(int roundId, int number) {
        super();
        this.roundId = roundId;
        this.number = number;
    }

    public int getRoundId() {
        return roundId;
    }

    public void setRoundId(int roundId) {
        this.roundId = roundId;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

}
