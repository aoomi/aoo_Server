package business.global.mj.pos;

/**
 * 打金信息
 *
 * @author Administrator
 */
public class PosOpJinInfo {
    // 标记
    private int outCardIDInt = -1;
    // 临时记录是否打金
    private int outCardIDtem = -2;
    // 打金总计
    private int outCardJinCount = 0;

    /**
     * 打出金牌
     *
     * @param card
     */
    public void setOutJinCard(boolean jinCard) {
        // 标记 + 1
        this.outCardIDInt++;
        // 金牌
        if (jinCard) {
            // 临时记录标记。
            this.outCardIDtem = this.outCardIDInt;
            // 记录打金数
            this.addOutCardJinCount();
        }
    }


    /**
     * 是否打金牌
     *
     * @return
     */
    public boolean isOutJinCard() {
        // 标记 == 临时记录打金标记
        if (this.outCardIDtem == this.outCardIDInt) {
            return true;
        }
        return false;
    }

    /**
     * 获取打金总计
     *
     * @return
     */
    public int getOutCardJinCount() {
        return this.outCardJinCount;
    }

    /**
     * 记录打金数
     */
    private void addOutCardJinCount() {
        this.outCardJinCount += 1;
    }
}
