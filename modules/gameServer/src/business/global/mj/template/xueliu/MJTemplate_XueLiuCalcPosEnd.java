package business.global.mj.template.xueliu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.template.MJTemplateCalcPosEnd;
import business.global.mj.template.MJTemplateRoom;
import business.global.mj.template.MJTemplateRoomEnum;
import cenum.RoomTypeEnum;
import cenum.mj.MJEndType;
import cenum.mj.OpPointEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommMath;

import java.util.HashMap;
import java.util.Map;

/**
 * 模板麻将
 */
public abstract class MJTemplate_XueLiuCalcPosEnd extends MJTemplateCalcPosEnd {

    public MJTemplate_XueLiuCalcPosEnd(AbsMJSetPos mSetPos) {
        super(mSetPos);
    }

    /**
     * @param lastOpPos
     * @param opType
     * @param calcPoint
     * @param cardType
     */
    public void actualTimeCalc1V1OpPoint(int lastOpPos, Object opType, Object loseOpType, int calcPoint, int cardType) {
        // 输分玩家的位置信息
        MJTemplate_XueLiuSetPos setPos = (MJTemplate_XueLiuSetPos) this.getMSetPos().getMJSetPos(lastOpPos);
        if (null == setPos) {
            // 没找到
            CommLogD.error("calc1V1Op lastOpPos :{}", lastOpPos);
            return;
        }
        //如果是后胡的
        if (!setPos.getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT) && !getMSetPos().getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
            if (setPos.getHuCardEndType().ordinal() >= getMSetPos().getHuCardEndType().ordinal()) {
                return;
            }
        }

        MJTemplateRoom room = getMSetPos().getRoom();

        //输的分
        if (room.calcFenUseYiKao() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
            if (room.isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() <= 0) {
                return;
            }
            double beiShu = Math.max(0D, room.getRoomTyepImpl().getSportsDouble());
            double sPoint = CommMath.mul(calcPoint, beiShu);
            double sportPoint = sPoint;
            if (room.isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() - sPoint <= 0) {
                sportPoint = setPos.getRoomPos().getRoomSportsPoint();
            }
            setPos.setDeductEndPoint(-sportPoint);
            setPos.addActualSportsPoint(-sportPoint);
            getMSetPos().setDeductEndPoint(sportPoint);
            getMSetPos().addActualSportsPoint(sportPoint);
        }
        //输钱
        setPos.addActualPoint(-calcPoint);
        setPos.setPidSumPointEnd(-calcPoint);
        setPos.setDeductEndPoint(0);
        setPos.updateCurLiuShui(-calcPoint, cardType, getMSetPos().getPosID(), loseOpType);

        //赢钱
        getMSetPos().addActualPoint(calcPoint);
        getMSetPos().setPidSumPointEnd(calcPoint);
        getMSetPos().setDeductEndPoint(0);
        getMSetPos().updateCurLiuShui(calcPoint, cardType, lastOpPos, opType);
    }

    /**
     * @param opType
     * @param calcPoint
     * @param cardType
     */
    public void actualTimeCalc1V3OpPoint(Object opType, Object loseOpType, int calcPoint, int cardType) {

        // 其他玩家信息
        MJTemplate_XueLiuSetPos setPos = null;
        for (int i = 0; i < this.getMSetPos().getPlayerNum(); i++) {
            // 遍历玩家
            setPos = (MJTemplate_XueLiuSetPos) this.getMSetPos().getMJSetPos(i);
            if (setPos == null) {
                // 找不到玩家直接跳过。
                continue;
            }
            //如果是后胡的
            if (!setPos.getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT) && !getMSetPos().getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
                if (setPos.getHuCardEndType().ordinal() >= getMSetPos().getHuCardEndType().ordinal()) {
                    continue;
                }
            }
            if (setPos.getPid() == this.getMSetPos().getPid()) {
                // 胡牌玩家本身跳过。
                continue;
            } else {
                MJTemplateRoom room = getMSetPos().getRoom();

                //输的分
                if (room.calcFenUseYiKao() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
                    if (room.isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() <= 0) {
                        return;
                    }
                    double beiShu = Math.max(0D, room.getRoomTyepImpl().getSportsDouble());
                    double sPoint = CommMath.mul(calcPoint, beiShu);
                    double sportPoint = sPoint;
                    if (room.isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() - sPoint <= 0) {
                        sportPoint = setPos.getRoomPos().getRoomSportsPoint();
                    }
                    setPos.setDeductEndPoint(-sportPoint);
                    setPos.addActualSportsPoint(-sportPoint);
                    getMSetPos().setDeductEndPoint(sportPoint);
                    getMSetPos().addActualSportsPoint(sportPoint);
                }
                //输钱
                setPos.addActualPoint(-calcPoint);
                setPos.setPidSumPointEnd(-calcPoint);
                setPos.setDeductEndPoint(0);
                setPos.updateCurLiuShui(-calcPoint, cardType, getMSetPos().getPosID(), loseOpType);

                //赢钱
                getMSetPos().addActualPoint(calcPoint);
                getMSetPos().setPidSumPointEnd(calcPoint);
                getMSetPos().setDeductEndPoint(0);
                getMSetPos().updateCurLiuShui(calcPoint, cardType, setPos.getPosID(), opType);
            }
        }
    }

    /**
     * 动作分数 1V1扣分。
     *
     * @param winHuCardMap  牌型map
     * @param loseHuCardMap 牌型map
     * @param lastOpPos     输分玩家的位置
     * @param calcPoint     分数
     */
    public void actualTimeCalc1V1Op(int lastOpPos, Map<Object, Integer> winHuCardMap, Map<Object, Integer> loseHuCardMap, int calcPoint, int cardType) {
        // 输分玩家的位置信息
        MJTemplate_XueLiuSetPos setPos = (MJTemplate_XueLiuSetPos) this.getMSetPos().getMJSetPos(lastOpPos);
        if (null == setPos) {
            // 没找到
            CommLogD.error("calc1V1Op lastOpPos :{}", lastOpPos);
            return;
        }
        //如果是后胡的
        if (!setPos.getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT) && !getMSetPos().getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
            if (setPos.getHuCardEndType().ordinal() >= getMSetPos().getHuCardEndType().ordinal()) {
                return;
            }
        }

        MJTemplateRoom room = getMSetPos().getRoom();

        //输的分
        if (room.calcFenUseYiKao() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
            if (room.isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() <= 0) {
                return;
            }
            double beiShu = Math.max(0D, room.getRoomTyepImpl().getSportsDouble());
            double sPoint = CommMath.mul(calcPoint, beiShu);
            double sportPoint = sPoint;
            if (room.isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() - sPoint <= 0) {
                sportPoint = setPos.getRoomPos().getRoomSportsPoint();
            }
            setPos.setDeductEndPoint(-sportPoint);
            setPos.addActualSportsPoint(-sportPoint);
            getMSetPos().setDeductEndPoint(sportPoint);
            getMSetPos().addActualSportsPoint(sportPoint);
        }
        //输钱
        setPos.addActualPoint(-calcPoint);
        setPos.setPidSumPointEnd(-calcPoint);
        setPos.setDeductEndPoint(0);
        setPos.updateCurLiuShui(-calcPoint, cardType, getMSetPos().getPosID(), loseHuCardMap);

        //赢钱
        getMSetPos().addActualPoint(calcPoint);
        getMSetPos().setPidSumPointEnd(calcPoint);
        getMSetPos().setDeductEndPoint(0);
        getMSetPos().updateCurLiuShui(calcPoint, cardType, lastOpPos, winHuCardMap);


    }

    /**
     * @param huCardMap
     * @param calcPoint
     * @param cardType
     */
    public void actualTimeCalc1V3Op(Map<Object, Integer> huCardMap, Map<Object, Integer> loseHuCardMap, int calcPoint, int cardType) {

        // 其他玩家信息
        MJTemplate_XueLiuSetPos setPos = null;
        for (int i = 0; i < this.getMSetPos().getPlayerNum(); i++) {
            // 遍历玩家
            setPos = (MJTemplate_XueLiuSetPos) this.getMSetPos().getMJSetPos(i);
            if (setPos == null) {
                // 找不到玩家直接跳过。
                continue;
            }
            //如果是后胡的
            if (!setPos.getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT) && !getMSetPos().getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
                if (setPos.getHuCardEndType().ordinal() >= getMSetPos().getHuCardEndType().ordinal()) {
                    continue;
                }
            }
            if (setPos.getPid() == this.getMSetPos().getPid()) {
                // 胡牌玩家本身跳过。
                continue;
            } else {
                MJTemplateRoom room = getMSetPos().getRoom();

                //输的分
                if (room.calcFenUseYiKao() && RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
                    if (room.isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() <= 0) {
                        return;
                    }
                    double beiShu = Math.max(0D, room.getRoomTyepImpl().getSportsDouble());
                    double sPoint = CommMath.mul(calcPoint, beiShu);
                    double sportPoint = sPoint;
                    if (room.isRulesOfCanNotBelowZero() && setPos.getRoomPos().getRoomSportsPoint() - sPoint <= 0) {
                        sportPoint = setPos.getRoomPos().getRoomSportsPoint();
                    }
                    setPos.setDeductEndPoint(-sportPoint);
                    setPos.addActualSportsPoint(-sportPoint);
                    getMSetPos().setDeductEndPoint(sportPoint);
                    getMSetPos().addActualSportsPoint(sportPoint);
                }
                //输钱
                setPos.addActualPoint(-calcPoint);
                setPos.setPidSumPointEnd(-calcPoint);
                setPos.setDeductEndPoint(0);
                setPos.updateCurLiuShui(-calcPoint, cardType, getMSetPos().getPosID(), loseHuCardMap);

                //赢钱
                getMSetPos().addActualPoint(calcPoint);
                getMSetPos().setPidSumPointEnd(calcPoint);
                getMSetPos().setDeductEndPoint(0);
                getMSetPos().updateCurLiuShui(calcPoint, cardType, setPos.getPosID(), huCardMap);
            }
        }
    }

    /**
     * 添加胡类型
     *
     * @param opPoint
     * @param point
     */
    @Override
    protected void addhuType(OpPointEnum opPoint, int point, MJEndType bEndType) {
        Map<Object, Integer> huTypeMap = getMSetPos().getCurLiuShui().getHuTypeMap();
        if (huTypeMap.containsKey(opPoint)) {
            // 累计
            int calcPoint = point;
            if (MJEndType.PLUS.equals(bEndType)) {
                calcPoint = huTypeMap.get(opPoint) + point;
            } else if (MJEndType.MULTIPLY.equals(bEndType)) {
                calcPoint = huTypeMap.get(opPoint) * point;
            }
            huTypeMap.put(opPoint, calcPoint);
        } else {
            huTypeMap.put(opPoint, point);
        }
    }


    @Override
    public MJTemplate_XueLiuSetPos getMSetPos() {
        return (MJTemplate_XueLiuSetPos) super.getMSetPos();
    }
}
