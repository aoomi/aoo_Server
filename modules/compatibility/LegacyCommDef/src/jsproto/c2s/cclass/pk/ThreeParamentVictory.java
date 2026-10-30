package jsproto.c2s.cclass.pk;

public class ThreeParamentVictory {
    private int pos;
    private int parameter1;
    private int parameter2;


    public ThreeParamentVictory() {
        super();
    }

    public ThreeParamentVictory(int pos, int parameter1, int parameter2) {
        super();
        this.pos = pos;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
    }

    public int getPos() {
        return this.pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    /**
     * @return parameter1
     */
    public int getParameter1() {
        return this.parameter1;
    }

    /**
     * @param parameter1 要设置的 parameter1
     */
    public void setParameter1(int parameter1) {
        this.parameter1 = parameter1;
    }

    /**
     * @return parameter2
     */
    public int getParameter2() {
        return this.parameter2;
    }

    /**
     * @param parameter2 要设置的 parameter2
     */
    public void setParameter2(int parameter2) {
        this.parameter2 = parameter2;
    }

    /* （非 Javadoc）
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ThreeParamentVictory [pos=" + this.pos + ", parameter1=" + this.parameter1 + ", parameter2=" + this.parameter2 + "]";
    }


}
