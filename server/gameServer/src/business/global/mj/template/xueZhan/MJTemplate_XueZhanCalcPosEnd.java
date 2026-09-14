package business.global.mj.template.xueZhan;

import business.global.mj.AbsMJSetPos;
import business.global.mj.template.MJTemplateCalcPosEnd;
import business.global.mj.template.MJTemplateRoomEnum;
import com.ddm.server.common.CommLogD;

/**
 * 模板麻将
 */
public abstract class MJTemplate_XueZhanCalcPosEnd extends MJTemplateCalcPosEnd {


    public MJTemplate_XueZhanCalcPosEnd(AbsMJSetPos mSetPos) {
        super(mSetPos);
    }


    /**
     * 动作分数 1V1扣分。
     *
     * @param opType    动作类型
     * @param lastOpPos 输分玩家的位置
     * @param calcPoint 分数
     */
    @Override
    public void calc1V1Op(int lastOpPos, Object opType, int calcPoint) {
        // 输分玩家的位置信息
        MJTemplate_XueZhanSetPos fromPos = (MJTemplate_XueZhanSetPos) this.getMSetPos().getMJSetPos(lastOpPos);
        if (null == fromPos) {
            // 没找到
            CommLogD.error("calc1V1Op lastOpPos :{}", lastOpPos);
            return;
        }
        if (!getMSetPos().getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT) && !fromPos.getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
            //如果是后胡的
            if (fromPos.getHuCardEndType().ordinal() >= getMSetPos().getHuCardEndType().ordinal()) {
                return;
            }
        }
        // 赢的分数计算。
        calcOpPointType(opType, calcPoint);
        this.getMSetPos().setDeductPoint(this.getMSetPos().getDeductPoint() + calcPoint);

        // 输的分数计算。
        fromPos.calcOpPointType(opType, (-calcPoint));
        fromPos.setDeductPoint(fromPos.getDeductPoint() - calcPoint);
    }

    /**
     * 动作分数 1V3扣分。 1人加分，3人扣分
     *
     * @param opType    动作类型
     * @param calcPoint 分数
     */
    @Override
    public void calc1V3Op(Object opType, int calcPoint) {

        // 其他玩家信息
        MJTemplate_XueZhanSetPos mOSetPos = null;
        for (int i = 0; i < this.getMSetPos().getPlayerNum(); i++) {
            // 遍历玩家
            mOSetPos = (MJTemplate_XueZhanSetPos) this.getMSetPos().getMJSetPos(i);
            if (mOSetPos == null) {
                // 找不到玩家直接跳过。
                continue;
            }
            if (!getMSetPos().getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT) && !mOSetPos.getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
                //如果是后胡的
                if (mOSetPos.getHuCardEndType().ordinal() >= getMSetPos().getHuCardEndType().ordinal()) {
                    continue;
                }
            }
            if (mOSetPos.getPid() == this.getMSetPos().getPid()) {
                // 胡牌玩家本身跳过。
                continue;
            } else {
                // 赢的分数计算。
                calcOpPointType(opType, calcPoint);
                this.getMSetPos().setDeductPoint(this.getMSetPos().getDeductPoint() + calcPoint);

                // 输的分数计算。
                mOSetPos.calcOpPointType(opType, (-calcPoint));
                mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() - calcPoint);
            }
        }
    }

    @Override
    public MJTemplate_XueZhanSetPos getMSetPos() {
        return (MJTemplate_XueZhanSetPos) super.getMSetPos();
    }
}
