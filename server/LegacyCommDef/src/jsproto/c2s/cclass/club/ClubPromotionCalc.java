package jsproto.c2s.cclass.club;

/**
 * 计算推广累计值
 */
public class ClubPromotionCalc {
    /**
     * 数量
     */
    private int number = 1;
    /**
     * 值
     */
    private double value;

    public ClubPromotionCalc(double value) {
        this.value = value;
    }

    public ClubPromotionCalc setValue(double value) {
        this.number++;
        this.value = value;
        return this;
    }

    public double getValue() {
        return this.value;
    }

    public int getNumber() {
        return this.number;
    }
}
