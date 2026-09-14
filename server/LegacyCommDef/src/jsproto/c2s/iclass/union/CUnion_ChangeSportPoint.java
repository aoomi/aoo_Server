package jsproto.c2s.iclass.union;

public class CUnion_ChangeSportPoint extends CUnion_Base {

    private int type;
    private double value;
    //增减后的值
    private double changedValue;

    public static CUnion_ChangeSportPoint make(int type, double value) {
        CUnion_ChangeSportPoint ret = new CUnion_ChangeSportPoint();
        ret.setType(type);
        ret.setValue(value);
        return ret;
    }

    public static CUnion_ChangeSportPoint make(int type, double value, double changedValue) {
        CUnion_ChangeSportPoint ret = new CUnion_ChangeSportPoint();
        ret.setType(type);
        ret.setValue(value);
        ret.setChangedValue(changedValue);
        return ret;
    }

    public double getChangedValue() {
        return changedValue;
    }

    public void setChangedValue(double changedValue) {
        this.changedValue = changedValue;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
