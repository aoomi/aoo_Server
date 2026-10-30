package business.global.pk.xcpdk;

import jsproto.c2s.cclass.pk.Victory;

/** XCPDK-specific victory detail; keeps bomb scoring out of the shared protocol model. */
public final class XCPDKVictory extends Victory {
    private int bombScore;

    public XCPDKVictory(int pos, int num, int bombScore) {
        super(pos, num);
        this.bombScore = bombScore;
    }

    public int getBombScore() {
        return bombScore;
    }

    public void setBombScore(int bombScore) {
        this.bombScore = bombScore;
    }

    public static int bombScoreOf(Victory victory) {
        return victory instanceof XCPDKVictory detail ? detail.getBombScore() : 0;
    }
}
