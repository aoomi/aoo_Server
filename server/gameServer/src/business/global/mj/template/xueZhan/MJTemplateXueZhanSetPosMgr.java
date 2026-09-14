package business.global.mj.template.xueZhan;

import business.global.mj.AbsMJSetRoom;
import business.global.mj.template.MJTemplateRoomEnum;
import business.global.mj.template.MJTemplateSetPos;
import business.global.mj.template.MJTemplateSetPosMgr;
import cenum.mj.HuType;
import cenum.mj.OpType;


public class MJTemplateXueZhanSetPosMgr extends MJTemplateSetPosMgr {

    public MJTemplateXueZhanSetPosMgr(AbsMJSetRoom set) {
        super(set);
    }

    /**
     * 检测其他人是否可以接杠
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    @Override
    protected void check_otherJieGang(int curOpPos, int curCardID) {
        MJTemplateSetPos setPos = null;
        for (int i = 1; i < this.set.getRoom().getPlayerNum(); i++) {
            int nextPos = (curOpPos + i) % this.set.getRoom().getPlayerNum();
            setPos = (MJTemplateSetPos) this.set.getMJSetPos(nextPos);
            //如果跟定缺的牌一样，不能吃碰杠胡
            if (MJTemplateRoomEnum.DingQue.DING_QUE.equals(setPos.getRoom().wanFa_DingQue()) &&
                    setPos.checkIsQue(curCardID / 100)) {
                continue;
            }
            if (setPos.isYaoGang() || (!setPos.getHuType().equals(HuType.NotHu) && !setPos.getHuType().equals(HuType.DianPao))) {
                continue;
            }
            // 检查接杠动作
            if (setPos.checkOpType(curCardID, OpType.JieGang)) {
                this.addOpTypeInfo(nextPos, OpType.JieGang);
                if (!this.checkExistJGBG()) {
                    // 接杠时不可选择碰后再补杠。
                    this.set.getLastOpInfo().addBuGang(curCardID / 100, OpType.JieGang);
                }
            }
        }
    }

}
