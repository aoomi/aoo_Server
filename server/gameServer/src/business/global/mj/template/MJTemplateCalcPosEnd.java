package business.global.mj.template;

import business.global.mj.AbsCalcPosEnd;
import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import cenum.mj.HuType;
import cenum.mj.MJEndType;
import cenum.mj.OpPointEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板麻将
 */
public abstract class MJTemplateCalcPosEnd extends AbsCalcPosEnd {

    public MJTemplateCalcPosEnd(AbsMJSetPos mSetPos) {
        super(mSetPos);
    }


    @Override
    public void calcPosEnd(AbsMJSetPos mSetPos) {
        mSetPos.setEndPoint(mSetPos.getEndPoint() + mSetPos.getDeductPoint());
    }

    @Override
    public void calcPosPoint(AbsMJSetPos mSetPos) {
        if (!getMSetPos().getHuType().equals(HuType.NotHu) && !getMSetPos().getHuType().equals(HuType.DianPao)) {
            calcHuType();
            calcHu();
        }

    }

    /**
     * 计算胡牌类型
     */
    protected void calcHuType() {
        MJCardInit mjCardInit = getMSetPos().mjCardInit(true);
        MJTemplateSetPos setPos = (MJTemplateSetPos) getMSetPos();
        setPos.getPosOpRecord().getOpHuList().clear();
        MJFactory.getHuCard(getMSetPos().getRoom().getHuCardImpl()).checkHuCardReturn(getMSetPos(), mjCardInit);
    }

    /**
     * 计算胡芬
     */
    protected void calcHu() {

        boolean calc1v1 = calc1v1();
        int calcHuPoint = calcHuPoint();
        if (calc1v1) {
            calc1V1Op(getMSetPos().getSet().getLastOpInfo().getLastOpPos(), OpPointEnum.Not, calcHuPoint);
        } else {
            calc1V3Op(OpPointEnum.Not, calcHuPoint);
        }
    }

    /**
     * 计算牌型分
     *
     * @return
     */
    protected int calcHuPoint() {
        int calcPoint = 0;
        int point = 0;
        List<Object> opHuList = getMSetPos().getPosOpRecord().getOpHuList();
        OpPointEnum opPointEnum;
        for (Object obj : opHuList) {
            opPointEnum = (OpPointEnum) obj;
            point = point(opPointEnum);
            calcOpPointType(opPointEnum, point);
            calcPoint += point;
        }
        return calcPoint;
    }

    /**
     * 胡分对应的分数
     *
     * @param opPointEnum
     * @return
     */
    protected int point(OpPointEnum opPointEnum) {
        int point = 0;
        switch (opPointEnum) {
            case QYS:
            case QDD:
            case PPH:
            case ZiMo:
            case QGH:
            case JiePao:
            case PingHu:
            case ZYS:
                break;
            default:
                point = 0;
                break;
        }
        return point;
    }

    /**
     * 是否是一家付
     *
     * @return
     */
    protected boolean calc1v1() {
        return getMSetPos().getHuType().equals(HuType.JiePao) || getMSetPos().getHuType().equals(HuType.QGH);
    }

    @Override
    public int calcPoint(boolean isZhuang, Object... params) {
        return 0;
    }

    protected class CalcPosEnd {
        @SuppressWarnings("unused")
        protected Map<OpPointEnum, Integer> huTypeMap = new HashMap<>();

        public CalcPosEnd(Map<OpPointEnum, Integer> huTypeMap) {
            this.huTypeMap = huTypeMap;
        }
    }

    @Override
    public <T> void calcOpPointType(T opType, int count) {
        OpPointEnum opPoint = (OpPointEnum) opType;
        switch (opPoint) {
            case Not:
                break;
            default:
                this.addhuType(opPoint, count, MJEndType.PLUS);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCalcPosEnd() {
        return (T) new CalcPosEnd(this.huTypeMap);
    }

    @Override
    public MJTemplateSetPos getMSetPos() {
        return (MJTemplateSetPos) super.getMSetPos();
    }
}
