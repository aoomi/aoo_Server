package business.global.mj.template.xueliu;

import business.global.mj.AbsMJSetRoom;
import business.global.mj.template.MJTemplateRoomEnum;
import business.global.mj.template.MJTemplateSetPos;
import business.global.room.mj.MJRoomPos;
import cenum.mj.HuType;
import cenum.mj.OpPointEnum;
import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.BaseMJRoom_PosEnd;
import jsproto.c2s.cclass.mj.template.MJTemplateHuInfo;
import jsproto.c2s.cclass.mj.template.MJTemplateRoom_PosEnd;
import jsproto.c2s.cclass.mj.template.MJTemplateSet_Pos;
import jsproto.c2s.cclass.mj.template.MJTemplate_XueLiuPlayerLiuSui;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板麻将 每一局每个位置信息
 *
 * @author Huaxing
 */
@Getter
@Setter
public abstract class MJTemplate_XueLiuSetPos extends MJTemplateSetPos {

    /**
     * 胡牌信息
     */
    private List<MJTemplateHuInfo> huInfos = new ArrayList<>();//
    /**
     * 血流玩法的流水
     */
    private List<MJTemplate_XueLiuPlayerLiuSui> xueLiuPlayerLiuShuiINfos = new ArrayList<>();
    /**
     * 胡牌顺序
     */
    public MJTemplateRoomEnum.HuCardEndType huCardEndType = MJTemplateRoomEnum.HuCardEndType.NOT;
    /**
     * 当前流水
     */
    private MJTemplate_XueLiuPlayerLiuSui curLiuShui;

    public MJTemplate_XueLiuSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set, Class<?> mActMrg) {
        super(posID, roomPos, set, mActMrg);
    }

    @Override
    protected void initSetPosData() {
        curLiuShui = new MJTemplate_XueLiuPlayerLiuSui(getPosID());
        super.initSetPosData();
    }

    public MJTemplate_XueLiuSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set) {
        super(posID, roomPos, set);
    }

    /**
     * 流模式胡完锁住手牌
     * 找出不能出的手牌
     * 1.报听
     * 2.定缺的牌
     * 3.金牌
     */
    protected void addBuNengChuList() {
        //1.报听后 除了摸进来的牌 其他的都不能打出去
        if (getHuInfos().size() > 0) {
            Set<Integer> protectedCardTypes = getPrivateCards().stream().map(k -> k.cardID).collect(Collectors.toSet());
            protectedCardTypes.stream().forEach(k -> getPosOpNotice().addBuNengChuList(k));
            return;
        }
        super.addBuNengChuList();
    }

    /**
     * 设置胡牌类型
     *
     * @param huType 胡牌类型
     * @param huPos  胡Pos
     */
    @Override
    public void setHuCardType(HuType huType, int huPos, int roundId) {
        if (huType.equals(HuType.DianPao)) {
            return;
        } else if (HuType.JiePao.equals(huType)) {
            huInfos.add(new MJTemplateHuInfo(getSet().getLastOpInfo().getLastOutCard(), huType, getSet().getLastOpInfo().getLastOpPos()));
        } else if (HuType.QGH.equals(huType)) {
            huInfos.add(new MJTemplateHuInfo(getSet().getLastOpInfo().getLastOpCard(), huType, getSet().getLastOpInfo().getLastOpPos()));
        } else {
            huInfos.add(new MJTemplateHuInfo(getHandCard().cardID, huType, -1));
        }

        super.setHuCardType(huType, huPos, roundId);

    }

    @Override
    public MJTemplateSet_Pos getCommonNotify(MJTemplateSet_Pos setPos, boolean isSelf) {
        MJTemplateSet_Pos set_Pos = super.getCommonNotify(setPos, isSelf);
        setPos.setHuInfos(huInfos);
        set_Pos.setHuOpType(getHuType());
        return set_Pos;
    }

    /**
     * 添加当前流水
     */
    public void addLiuShui() {
        //若存在出分对象
        if (curLiuShui.getDuiXiangList().size() > 0) {
            this.xueLiuPlayerLiuShuiINfos.add(curLiuShui);
            //新建流水 并清空胡牌分
            this.curLiuShui = new MJTemplate_XueLiuPlayerLiuSui(getPosID());
            getCalcPosEnd().getHuTypeMap().clear();
        }
    }

    /**
     * 添加流水处分对象
     *
     * @param pos
     */
    public void addDuiXiang(int pos) {
        if (pos != getPosID()) {
            if (!curLiuShui.getDuiXiangList().contains(pos)) {
                curLiuShui.getDuiXiangList().add(pos);
            }
        }
    }

    /**
     * 默认算1分 有需要的自己重写此方法
     */
    public void actualTimeCalcGangPoint(OpType opType) {
        if (getRoom().wanFa_ActualTimeCalcPoint().equals(MJTemplateRoomEnum.ActualTimeCalcPoint.CALC_GANG_POINT)) {

            int dianPos = getPublicCardList().get(sizePublicCardList() - 1).get(1);
            int cardType = getSet().getCurRound().getOpCard() / 100;
            MJTemplate_XueLiuCalcPosEnd calcPosEnd = (MJTemplate_XueLiuCalcPosEnd) getCalcPosEnd();
            if (opType.equals(OpType.JieGang)) {
                //  JieGangNum, // 被明杠
                calcPosEnd.actualTimeCalc1V1OpPoint(dianPos, OpPointEnum.JieGang, OpPointEnum.JieGangNum, 1, cardType);
            } else if (opType.equals(OpType.AnGang)) {
                //    AnGangNum,//被暗杠
                calcPosEnd.actualTimeCalc1V3OpPoint(OpPointEnum.AnGang, OpPointEnum.AnGangNum, 1, cardType);
            } else if (opType.equals(OpType.Gang)) {
                //    GangNum,//被补杠
                calcPosEnd.actualTimeCalc1V1OpPoint(dianPos, OpPointEnum.Gang, OpPointEnum.GangNum, 1, cardType);
            } else if (opType.equals(OpType.QiangGangHu)) {
                calcPosEnd.actualTimeCalc1V1OpPoint(dianPos, OpPointEnum.Gang, OpPointEnum.GangNum, -1, cardType);
            }

            //这个要自己改成自己的游戏类 不是每个游戏必要的  自己添加
            getTemplateRoomSet().getRoomPlayBack().playBack2All(getTemplateRoomSet().noticePromptly(getPosID(), 1, dianPos));
            getSet().getPosDict().values().forEach(k -> ((MJTemplate_XueLiuSetPos) k).addLiuShui());
        }

    }

    public void updateCurLiuShui(int point, int cardType, int duiXiang, Map<Object, Integer> huCardMap) {
        addDuiXiang(duiXiang);
        curLiuShui.addPoint(point);
        curLiuShui.setCardType(cardType);
        Map<Object, Integer> curLiuShuiHuTypeMap = curLiuShui.getHuTypeMap();
        huCardMap.entrySet().forEach(k -> {
            if (curLiuShuiHuTypeMap.containsKey(k.getKey())) {
                int value = curLiuShuiHuTypeMap.get(k.getKey()) + k.getValue();
                curLiuShuiHuTypeMap.put(k.getKey(), value);
            } else {
                curLiuShuiHuTypeMap.put(k.getKey(), k.getValue());
            }
        });
        curLiuShui.getHuTypeMap().putAll(huCardMap);
    }

    public void updateCurLiuShui(int point, int cardType, int duiXiang, Object pointEnum) {
        addDuiXiang(duiXiang);
        curLiuShui.addPoint(point);
        curLiuShui.setCardType(cardType);
        if (curLiuShui.getHuTypeMap().containsKey(pointEnum)) {
            curLiuShui.getHuTypeMap().put(pointEnum, point + curLiuShui.getHuTypeMap().get(pointEnum));
        } else {
            curLiuShui.getHuTypeMap().put(pointEnum, point);
        }
    }

    /**
     * 统计本局分数
     *
     * @return
     */
    @Override
    public BaseMJRoom_PosEnd<?> calcPosEnd() {
        MJTemplateRoom_PosEnd ret = (MJTemplateRoom_PosEnd) super.calcPosEnd();
        ret.setXueLiuPlayerLiuShuiInfos(getXueLiuPlayerLiuShuiINfos());
        return ret;
    }

    @Override
    public void calcGangPoint1v3(int point) {
        for (int i = 0; i < this.getSet().getRoom().getPlayerNum(); i++) {
            MJTemplate_XueLiuSetPos setPos = (MJTemplate_XueLiuSetPos) getMJSetPos(i);
            if (Objects.isNull(setPos)) {
                return;
            }
            //不跟胡过得算分
            if (setPos.getHuCardEndType().ordinal() >= this.huCardEndType.ordinal() && !setPos.getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
                return;
            }
        }
    }

    /**
     * 是否已经胡牌了
     *
     * @return
     */
    public boolean isHuCard() {
        return getHuInfos().size() > 0;
    }

    public MJTemplate_XueLiuRoomSet getTemplateRoomSet() {
        return (MJTemplate_XueLiuRoomSet) getSet();
    }
}