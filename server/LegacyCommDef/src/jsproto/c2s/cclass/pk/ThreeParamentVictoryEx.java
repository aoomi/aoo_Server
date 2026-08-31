package jsproto.c2s.cclass.pk;

public class ThreeParamentVictoryEx {
    private int num = -1;
    private int pos1 = -1;
    private int pos2 = -1;


    public ThreeParamentVictoryEx() {
        super();
    }

    public ThreeParamentVictoryEx(int num, int pos1, int pos2) {
        super();
        this.num = num;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public void setParamentVictory(int num, int pos1, int pos2) {
        this.num = num;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    /**
     * @return num
     */
    public int getNum() {
        return num;
    }

    /**
     * @param num 要设置的 num
     */
    public void setNum(int num) {
        this.num = num;
    }

    /**
     * @return pos1
     */
    public int getPos1() {
        return pos1;
    }

    /**
     * @param pos1 要设置的 pos1
     */
    public void setPos1(int pos1) {
        this.pos1 = pos1;
    }

    /**
     * @return pos2
     */
    public int getPos2() {
        return pos2;
    }

    /**
     * @param pos2 要设置的 pos2
     */
    public void setPos2(int pos2) {
        this.pos2 = pos2;
    }
}
