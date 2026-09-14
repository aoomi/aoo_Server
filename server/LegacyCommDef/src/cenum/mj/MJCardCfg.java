package cenum.mj;

public enum MJCardCfg {
    NOT(0), WANG(1), // 万
    WANG_1_9(1),// 只添加幺九万（韶关麻将）
    TIAO(2), // 条
    TONG(3), // 筒
    FENG(4), // 风
    JIAN(4), // 箭
    ZHONG(4), // 红中
    FA(4), // 发
    BAI(4), // 白
    HUA(5) // 花

    ;
    private int value;

    private MJCardCfg(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
};