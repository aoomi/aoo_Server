package business.global.mj.cdxzmj;

import business.cdxzmj.c2s.iclass.CCDXZMJ_CreateRoom;
import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import business.global.mj.template.MJTemplateRoomEnum;
import business.global.mj.template.xueZhan.MJTemplate_XueZhanCalcPosEnd;
import business.global.mj.template.xueZhan.MJTemplate_XueZhanSetPos;
import cenum.mj.HuType;
import cenum.mj.OpPointEnum;
import cenum.mj.OpType;
import com.ddm.server.common.CommLogD;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模板麻将
 */
public class CDXZMJCalcPosEnd extends MJTemplate_XueZhanCalcPosEnd {

    public CDXZMJCalcPosEnd(AbsMJSetPos mSetPos) {
        super(mSetPos);
    }


    @Override
    public void calcPosPoint(AbsMJSetPos mSetPos) {
        calcFanPei();
        calcChaJiao();
        if (getMSetPos().getSet().getMHuInfo().getHuPosList().isEmpty()) {
            return;
        }
        //如果不是胡牌的位置
        if (!getMSetPos().getSet().getMHuInfo().getHuPosList().contains(mSetPos.getPosID())) {
            return;
        }
        calcHu();
    }

    /**
     * 当两家或三家甚至四家摸完牌后仍然没有人胡牌，这时，每一家必须把牌亮出开始查叫（是否听牌），
     * 没有叫的一家必须按最大算法赔给有叫的一家，若每一家都有叫，则不用赔或这收入，此局荒牌。
     * 若查叫时某一家牌型中没有缺一门，无论有无叫，都要赔给有叫的一家总番数的两倍
     */
    private void calcChaJiao() {
        if (getMSetPos().getHuCardTypes().size() > 0 && (getMSetPos().getHuType().equals(HuType.NotHu) || getMSetPos().getHuType().equals(HuType.DianPao))) {
            List<AbsMJSetPos> notTingPosList = getMSetPos().getSet().getPosDict().values().stream().filter(k -> k.getHuCardTypes().isEmpty()).collect(Collectors.toList());
            List<AbsMJSetPos> notQueYiMenList = notTingPosList.stream().filter(k -> ((CDXZMJSetPos) k).checkExistQue(((CDXZMJSetPos) k).allCardIDs())).collect(Collectors.toList());
            notTingPosList.removeAll(notQueYiMenList);
            //没有缺一门
            int maxPoint = getMSetPos().getHuInfo().values().stream().mapToInt(Integer::intValue).max().getAsInt();
            maxPoint = (int) Math.pow(2, maxPoint);
            for (AbsMJSetPos notQymPos : notQueYiMenList) {
                calc1V1Op(notQymPos.getPosID(), OpPointEnum.ChaJiao, maxPoint * 2);
            }
            for (AbsMJSetPos notTingPos : notTingPosList) {
                calc1V1Op(notTingPos.getPosID(), OpPointEnum.ChaJiao, maxPoint);
            }
        }
    }

    public void calcFanPei() {
        if (getMSetPos().getRoom().RoomCfg(CDXZMJRoomEnum.KeXuanWanFa.FAN_PEI)) {
            if (getMSetPos().getHuCardTypes().isEmpty() && (getMSetPos().getHuType().equals(HuType.NotHu) || getMSetPos().getHuType().equals(HuType.DianPao))) {
                this.getMSetPos().getPublicCardList().forEach(k -> {
                    if (k.get(0) == OpType.AnGang.value()) {
                        getMSetPos().getGangMap().get(k.get(2)).forEach(pos -> calc1V1Op(pos, OpPointEnum.PeiZi, -2));
                    } else if (k.get(0) == OpType.Gang.value()) {
                        getMSetPos().getGangMap().get(k.get(2)).forEach(pos -> calc1V1Op(pos, OpPointEnum.PeiZi, -1));
                    } else if (k.get(0) == OpType.JieGang.value()) {
                        if (getMSetPos().getRoom().RoomCfg(CDXZMJRoomEnum.KeXuanWanFa.BA_DAO_TANG)) {
                            getMSetPos().getGangMap().get(k.get(2)).forEach(pos -> calc1V1Op(pos, OpPointEnum.PeiZi, -1));
                            calc1V1Op(k.get(1), OpPointEnum.PeiZi, -1);
                        } else {
                            calc1V1Op(k.get(1), OpPointEnum.PeiZi, -2);
                        }
                    }
                });
            } else {
                calcGang();
            }
        } else {
            calcGang();
        }
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
        MJTemplate_XueZhanSetPos fromPos = (MJTemplate_XueZhanSetPos) this.getMSetPos().getMJSetPos(lastOpPos);
        if (null == fromPos) {
            // 没找到
            CommLogD.error("calc1V1Op lastOpPos :{}", lastOpPos);
            return;
        }
        // 赢的分数计算。
        calcOpPointType(opType, calcPoint);
        this.getMSetPos().setDeductPoint(this.getMSetPos().getDeductPoint() + calcPoint);

        // 输的分数计算。
        fromPos.calcOpPointType(opType, (-calcPoint));
        fromPos.setDeductPoint(fromPos.getDeductPoint() - calcPoint);
    }

    /**
     * 计算胡牌类型
     */
    protected void calcHu() {
        MJCardInit mjCardInit = getMSetPos().mjCardInit(true);

        List<Object> opHuList = getMSetPos().getPosOpRecord().getOpHuList();
        MJFactory.getHuCard(getMSetPos().getRoom().getHuCardImpl()).checkHuCardReturn(getMSetPos(), mjCardInit);
        OpPointEnum pointEnum;
        int fan = 0;
        int curFan = 0;
        for (Object obj : opHuList) {
            pointEnum = (OpPointEnum) obj;
            curFan = point(pointEnum);
            fan += curFan;
            huTypeMap.put(pointEnum, curFan);
        }
        CCDXZMJ_CreateRoom cfg = getMSetPos().getRoom().getCfg();
        if (cfg.getFengDing() + 2 < fan) {
            fan = cfg.getFengDing() + 2;
        }

        int point = (int) Math.pow(2, fan);
        if (calc1v1()) {
            calc1V1OpHu(getMSetPos().getHuInfos().get(0).getPosID(), OpPointEnum.Not, point);
        } else {
            calc1V3OpHu(OpPointEnum.Not, point);
        }
    }

    /**
     * 是否是一家付
     *
     * @return
     */
    protected boolean calc1v1() {
        return getMSetPos().getHuType().name().contains("Hu");
    }

    /**
     * 计算杠
     */
    private void calcGang() {
        if (getMSetPos().getRoom().RoomCfg(CDXZMJRoomEnum.KeXuanWanFa.XIA_YU)) {
            this.getMSetPos().getPublicCardList().forEach(k -> {
                if (k.get(0) == OpType.AnGang.value()) {
                    getMSetPos().getGangMap().get(k.get(2)).forEach(pos -> calc1V1Op(pos - 1, OpPointEnum.AnGang, -2));
                } else if (k.get(0) == OpType.Gang.value()) {
                    getMSetPos().getGangMap().get(k.get(2)).forEach(pos -> calc1V1Op(pos - 1, OpPointEnum.Gang, 1));
                } else if (k.get(0) == OpType.JieGang.value()) {
                    if (getMSetPos().getRoom().RoomCfg(CDXZMJRoomEnum.KeXuanWanFa.BA_DAO_TANG)) {
                        getMSetPos().getGangMap().get(k.get(2)).forEach(pos -> calc1V1Op(pos - 1, OpPointEnum.JieGang, 1));
                        calc1V1Op(k.get(1), OpPointEnum.JieGang, 1);
                    } else {
                        calc1V1Op(k.get(1), OpPointEnum.JieGang, 2);
                    }
                }
            });
        }
    }

    /**
     * 动作分数 1V1扣分。
     *
     * @param opType    动作类型
     * @param lastOpPos 输分玩家的位置
     * @param calcPoint 分数
     */
    @Override
    public void calc1V1Op(int lastOpPos, Object opType, Object obj, int calcPoint) {
        // 输分玩家的位置信息
        AbsMJSetPos fromPos = this.getMSetPos().getMJSetPos(lastOpPos);
        if (null == fromPos) {
            // 没找到
            CommLogD.error("calc1V1Op lastOpPos :{}", lastOpPos);
            return;
        }

        // 赢的分数计算。
        calcOpPointType(opType, calcPoint);
        this.getMSetPos().setDeductPoint(this.getMSetPos().getDeductPoint() + calcPoint);

        // 输的分数计算。
        fromPos.calcOpPointType(opType, (-calcPoint));
        fromPos.setDeductPoint(fromPos.getDeductPoint() - calcPoint);
    }

    /**
     * 胡分对应的分数
     *
     * @param opPointEnum
     * @return
     */
    @Override
    protected int point(OpPointEnum opPointEnum) {
        int point = 0;
        switch (opPointEnum) {
            case PPH:
                if (getMSetPos().getRoom().RoomCfg(CDXZMJRoomEnum.KeXuanWanFa.DUI_DUI_HU_2FAN)) {
                    point = 2;
                } else {
                    point = 1;
                }
                break;
            case GangPao://带根
            case GSP:
                point = 1;
                break;
            case QDHu:
            case QYS:
            case GSKH:
            case HunYaoJiu:
            case BaoJiao:
                point = 2;
                break;
            case JYSPPH:
            case QYSPPH:
                point = 3;
                break;
            case HDDHu:
            case QYSQD:
            case QingYaoJiu:
                point = 4;
                break;
            case QYSHDDHu:
            case TianHu:
            case DiHu:
                point = 5;
                break;
            default:
                break;
        }
        return point;
    }

    public CDXZMJSetPos getMSetPos() {
        return (CDXZMJSetPos) super.getMSetPos();
    }


    public int preCalcHuCardPoint(int cardID) {
        MJCardInit mjCardInit = getMSetPos().mCardInit(cardID, false);
        return preCalcHuCardPoint(mjCardInit);
    }

    public int preCalcHuCardPoint(MJCardInit mjCardInit) {
        List<Object> opHuList = getMSetPos().getPosOpRecord().getOpHuList();
        MJFactory.getHuCard(CDXZMJNormalHuCardImpl.class).checkHuCardReturn(getMSetPos(), mjCardInit);
        OpPointEnum pointEnum;
        int fan = 0;
        int curFan = 0;
        for (Object obj : opHuList) {
            pointEnum = (OpPointEnum) obj;
            curFan = point(pointEnum);
            fan += curFan;
        }
        CCDXZMJ_CreateRoom cfg = getMSetPos().getRoom().getCfg();
        if (cfg.getFengDing() + 2 < fan) {
            fan = cfg.getFengDing() + 2;
        }
        return fan;
    }

    /**
     * 动作分数 1V1扣分。
     *
     * @param opType    动作类型
     * @param lastOpPos 输分玩家的位置
     * @param calcPoint 分数
     */
    public void calc1V1OpHu(int lastOpPos, Object opType, int calcPoint) {
        // 输分玩家的位置信息
        MJTemplate_XueZhanSetPos fromPos = (MJTemplate_XueZhanSetPos) this.getMSetPos().getMJSetPos(lastOpPos);
        if (null == fromPos) {
            // 没找到
            CommLogD.error("calc1V1Op lastOpPos :{}", lastOpPos);
            return;
        }
        //如果是后胡的
        if (!fromPos.getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT) && !getMSetPos().getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
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
        getMSetPos().getWinList().add(lastOpPos + 1);
    }

    /**
     * 动作分数 1V3扣分。 1人加分，3人扣分
     *
     * @param opType    动作类型
     * @param calcPoint 分数
     */
    public void calc1V3OpHu(Object opType, int calcPoint) {

        // 其他玩家信息
        MJTemplate_XueZhanSetPos mOSetPos = null;
        for (int i = 0; i < this.getMSetPos().getPlayerNum(); i++) {
            // 遍历玩家
            mOSetPos = (MJTemplate_XueZhanSetPos) this.getMSetPos().getMJSetPos(i);
            if (mOSetPos == null) {
                // 找不到玩家直接跳过。
                continue;
            }
            //如果是后胡的
            if (!mOSetPos.getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT) && !getMSetPos().getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
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
                getMSetPos().getWinList().add(mOSetPos.getPosID() + 1);

            }
        }
    }

}
