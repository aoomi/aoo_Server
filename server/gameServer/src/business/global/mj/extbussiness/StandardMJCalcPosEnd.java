package business.global.mj.extbussiness;

import business.global.mj.AbsCalcPosEnd;
import business.global.mj.AbsMJSetPos;
import business.global.mj.extbussiness.dto.StandardMJPointItem;
import cenum.RoomTypeEnum;
import cenum.mj.HuType;
import cenum.mj.MJEndType;
import cenum.mj.MJHuOpType;
import cenum.mj.OpType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import business.global.mj.extbussiness.StandardMJRoomEnum.OpPoint;
import com.ddm.server.common.CommLogD;

/**
 * 麻将结算
 */
public class StandardMJCalcPosEnd extends AbsCalcPosEnd {
    private StandardMJRoomSet set;
    public Map<String, Integer> extHuTypeMap = new LinkedHashMap<>();
    public StandardMJSetPos mSetPos;

    public StandardMJCalcPosEnd(AbsMJSetPos mSetPos) {
        super(mSetPos);
        this.mSetPos = (StandardMJSetPos) mSetPos;
        this.set = (StandardMJRoomSet) mSetPos.getSet();
    }

    public StandardMJRoomSet getSet() {
        return set;
    }

    public StandardMJSetPos getmSetPos() {
        return mSetPos;
    }

    @Override
    public int calcPoint(boolean isZhuang, Object... params) {
        return 0;
    }


    @Override
    public void calcPosEnd(AbsMJSetPos mSetPos) {
        this.calcPosEndYiKao();
    }

    /**
     * 计算倒分
     *
     * @param mSetPos 玩家信息
     */
    @Override
    public void calcPosPoint(AbsMJSetPos mSetPos) {
        if (this.set.getMHuInfo().getHuPos() < 0) {
            return;
        }
        //抄庄分
        if (mSetPos.getPosID() == set.getDPos() && set.getZhuiZhuangNum() >= 1) {
            this.calChaoZhuang();
        }
        //算杠分
        this.calcGang();
        // 计算胡牌
        this.calcHu();
    }

    /**
     * 抄庄分数
     **/
    public void calChaoZhuang() {
        this.calc1V3Op(OpPoint.ChaoZhuang, OpPoint.ChaoZhuang.value() * set.getZhuiZhuangNum());
    }

    /**
     * 计算杠
     */
    public void calcGang() {
        this.getMSetPos().getPublicCardList().forEach(k -> {
            if (k.get(0) == OpType.AnGang.value()) {
                if (!((StandardMJRoom) mSetPos.getRoom()).isSSSG()) {
                    calc1V3Op(OpPoint.GangPoint, OpPoint.AnGang.value());
                }
//                calcOpPointType(MODULEMJOpPoint.AnGang,1);
            } else if (k.get(0) == OpType.Gang.value()) {
                if (!((StandardMJRoom) mSetPos.getRoom()).isSSSG()) {
                    calc1V3Op(OpPoint.GangPoint, OpPoint.Gang.value());
                }
//                calcOpPointType(MODULEMJOpPoint.MingGang,1);
            } else if (k.get(0) == OpType.JieGang.value()) {
                if (!((StandardMJRoom) mSetPos.getRoom()).isSSSG()) {
                    calc1V3Op(OpPoint.GangPoint, OpPoint.JieGang.value());
                }
//                calcOpPointType(MODULEMJOpPoint.MingGang,1);
            }
        });
    }

    /**
     * 动作分数 1V1扣分。
     *
     * @param opType    动作类型
     * @param lastOpPos 输分玩家的位置
     * @param calcPoint 分数
     */
    public void calc1V1Op(int lastOpPos, Object opType, int calcPoint) {
        // 输分玩家的位置信息
        AbsMJSetPos fromPos = this.mSetPos.getMJSetPos(lastOpPos);
        if (null == fromPos) {
            // 没找到
            CommLogD.error("calc1V1Op lastOpPos :{}", lastOpPos);
            return;
        }

        calcOpPointType(opType, calcPoint);
        fromPos.calcOpPointType(opType, (-calcPoint));

        if (this.set.getRoom().isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(this.set.getRoom().getRoomTypeEnum())) {
            if(calcPoint>0){
                if(this.mSetPos.getRoomPos().getGameBeginSportsPoint()<=0){
                    return;
                }
            }else if(calcPoint<0){
                if(fromPos.getRoomPos().getGameBeginSportsPoint()<=0){
                    return;
                }
            }
        }

        this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + calcPoint);
        fromPos.setDeductPoint(fromPos.getDeductPoint() - calcPoint);
    }

    /**
     * 动作分数 1V3扣分。 1人加分，3人扣分
     *
     * @param opType    动作类型
     * @param calcPoint 分数
     */
    public void calc1V3Op(Object opType, int calcPoint) {
        // 其他玩家信息
        AbsMJSetPos mOSetPos = null;
        for (int i = 0; i < this.mSetPos.getPlayerNum(); i++) {
            // 遍历玩家
            mOSetPos = this.mSetPos.getMJSetPos(i);
            if (mOSetPos == null) {
                // 找不到玩家直接跳过。
                continue;
            }
            if (mOSetPos.getPid() == this.mSetPos.getPid()) {
                // 胡牌玩家本身跳过。
                continue;
            } else {
                calcOpPointType(opType, calcPoint);
                mOSetPos.calcOpPointType(opType, (-calcPoint));

                if (this.set.getRoom().isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(this.set.getRoom().getRoomTypeEnum())) {
                    if(calcPoint>0){
                        if(this.mSetPos.getRoomPos().getGameBeginSportsPoint()<=0){
                            return;
                        }
                    }else if(calcPoint<0){
                        if(mOSetPos.getRoomPos().getGameBeginSportsPoint()<=0){
                            return;
                        }
                    }
                }

                this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + calcPoint);
                mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() - calcPoint);
            }
        }
    }

    /**
     * 动作分数 1V3扣分。 1人加分，3人扣分
     *
     * @param opType    动作类型
     * @param calcPoint 分数
     */
    public void calc1V3Op(Object opType, final int calcPoint, int baoPeiPos) {
        // 其他玩家信息
        AbsMJSetPos mOSetPos;
        boolean yiKao = ((StandardMJRoom) mSetPos.getRoom()).getGuoFen() > 0;
        for (int i = 0; i < this.mSetPos.getPlayerNum(); i++) {
            // 遍历玩家
            mOSetPos = this.mSetPos.getMJSetPos(i);
            if (mOSetPos == null) {
                // 找不到玩家直接跳过。
                continue;
            }
            int score = calcPoint;
            if (mOSetPos.getPid() != this.mSetPos.getPid()) {
                mOSetPos = baoPeiPos >= 0 ? mOSetPos.getMJSetPos(baoPeiPos) : mOSetPos;
                int notLimitScore = score;
                score = this.loseLimitPoint(score, mOSetPos.getDeductPoint());

                if(yiKao){
                    //remark 分数达到上限
                    if (mOSetPos.getRoomPos().getTempPoint() + set.getLimitScore() - score < 0) {
                        score = mOSetPos.getRoomPos().getTempPoint() + set.getLimitScore();
                    }
                }
                calcOpPointType(opType, notLimitScore);
                mOSetPos.calcOpPointType(opType, (-notLimitScore));

                if (this.set.getRoom().isOnlyWinRightNowPoint() && RoomTypeEnum.UNION.equals(this.set.getRoom().getRoomTypeEnum())) {
                    if(score>0){
                        if(this.mSetPos.getRoomPos().getGameBeginSportsPoint()<=0){
                            return;
                        }
                    }else if(score<0){
                        if(mOSetPos.getRoomPos().getGameBeginSportsPoint()<=0){
                            return;
                        }
                    }
                }

                if(yiKao){
                    // remark 更新临时分数
                    mSetPos.getRoomPos().setTempPoint(mSetPos.getRoomPos().getTempPoint() + score);
                    mOSetPos.getRoomPos().setTempPoint(mOSetPos.getRoomPos().getTempPoint() - score);
                }
                this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + score);
                mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() - score);

            }
        }
    }

    /**
     * 获取庄家分数
     *
     * @param mOSetPos
     * @return
     */
    private int zhuangJia(AbsMJSetPos mOSetPos) {
        if (this.getMSetPos().getPosID() == this.set.getDPos() || mOSetPos.getPosID() == this.set.getDPos()) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * 计算胡
     */
    public void calcHu() {
        switch (this.mSetPos.getHuType()) {
            case NotHu:
            case DianPao:
                break;
            case ZiMo:
            case JiePao:
            case QGH:
                this.calcHuPoint();
                break;
            default:
                break;
        }
    }

    /**
     * 输分限制
     *
     * @param calcPoint 分数
     * @return
     */
    public int loseLimitPoint(int calcPoint, int deductPoint) {
        int value = ((StandardMJRoom) mSetPos.getRoom()).getFengDing();
        if (value <= 0) {
            // 底分 <= 0 ,没有限制分数
            return calcPoint;
        } else {
            int calcSumPoint = deductPoint <= 0 ? calcPoint + Math.abs(deductPoint) : calcPoint - deductPoint;
            if (calcSumPoint <= value) {
                return calcPoint;
            }
            return value - (calcSumPoint - calcPoint);
        }
    }

    /**
     * 计算胡分
     */
    private void calcHuPoint() {
        if (HuType.NotHu.equals(this.getMSetPos().getHuType()) || HuType.DianPao.equals(this.getMSetPos().getHuType())) {
            // 没有胡牌
            return;
        }
        StandardMJPointItem item = (StandardMJPointItem) this.mSetPos.getOpHuType();
        if (Objects.isNull(item)) {
            return;
        }
        //包赔
        int baoPeiPos = -1;
        //这里自己写包赔的位置获取
//        if(baoPeiPos>=0){
//            //杠爆全包
//            mSetPos.getMJSetPos(baoPeiPos).calcOpPointType(MODULEMJOpPoint.BP, 0);
//        }
        //胡牌分
        if (MJHuOpType.JiePao.equals(this.getMSetPos().getmHuOpType())) {
            this.setDianPao();
            this.calc1V1Op(this.mSetPos.getSet().getLastOpInfo().getLastOpPos(), OpPoint.HuPoint, item.getPoint());
        } else if (MJHuOpType.QGHu.equals(this.getMSetPos().getmHuOpType())) {
            mSetPos.setHuType(HuType.QGH);
            this.setDianPao();
            this.calc1V1Op(this.mSetPos.getSet().getLastOpInfo().getLastOpPos(), OpPoint.HuPoint, item.getPoint());
        } else {
            this.calc1V3Op(OpPoint.HuPoint, item.getPoint(), baoPeiPos);
        }
        extHuTypeMap.putAll(item.getMap());
    }

    @Override
    public <T> void calcOpPointType(T opType, int count) {
        if (opType == null) {
            return;
        }
        OpPoint opPoint = (OpPoint) opType;
        // 添加胡类型
        this.addhuType(opPoint.name(), count, MJEndType.PLUS);
    }

    private class CalcPosEnd {
        @SuppressWarnings("unused")
        private Map<String, Integer> resultHuTypeMap;

        public CalcPosEnd(Map<String, Integer> huTypeMap) {
            this.resultHuTypeMap = huTypeMap;
        }

    }

    /**
     * 添加胡类型
     *
     * @param opPoint
     * @param point
     */
    protected void addhuType(String opPoint, int point, MJEndType bEndType) {
        if (this.extHuTypeMap.containsKey(opPoint)) {
            // 累计							
            int calcPoint = point;
            if (MJEndType.PLUS.equals(bEndType)) {
                calcPoint = this.extHuTypeMap.get(opPoint) + point;
            } else if (MJEndType.MULTIPLY.equals(bEndType)) {
                calcPoint = this.extHuTypeMap.get(opPoint) * point;
            }
            this.extHuTypeMap.put(opPoint, calcPoint);
        } else {
            this.extHuTypeMap.put(opPoint, point);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCalcPosEnd() {
        return (T) new CalcPosEnd(this.extHuTypeMap);
    }

    public Map<String, Integer> getEXTHuTypeMap() {
        return extHuTypeMap;
    }
}
