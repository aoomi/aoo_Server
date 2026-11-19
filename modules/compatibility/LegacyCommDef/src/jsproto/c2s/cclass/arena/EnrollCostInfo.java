package jsproto.c2s.cclass.arena;

/**
 * 报名消耗信息
 *
 * @author Administrator
 */
public class EnrollCostInfo {
    private int freeType = 0;//免费类型
    private int freeValue = 0;//免费值
    private int enrollCostType = 0;//报名消耗类型
    private int enrollCostValue = 0;//报名消耗值

    public int getFreeType() {
        return freeType;
    }

    public void setFreeType(int freeType) {
        this.freeType = freeType;
    }

    public int getFreeValue() {
        return freeValue;
    }

    public void setFreeValue(int freeValue) {
        this.freeValue = freeValue;
    }

    public int getEnrollCostType() {
        return enrollCostType;
    }

    public void setEnrollCostType(int enrollCostType) {
        this.enrollCostType = enrollCostType;
    }

    public int getEnrollCostValue() {
        return enrollCostValue;
    }

    public void setEnrollCostValue(int enrollCostValue) {
        this.enrollCostValue = enrollCostValue;
    }


}
