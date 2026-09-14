package business.global.mj.cdxzmj;

import business.cdxzmj.c2s.cclass.CDXZMJResults;
import business.cdxzmj.c2s.cclass.CDXZMJRoom_PosEnd;
import business.cdxzmj.c2s.cclass.CDXZMJSet_Pos;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.MJCardInit;
import business.global.mj.template.MJTemplateRoomEnum;
import business.global.mj.template.wanfa.MJTemplateLouHu;
import business.global.mj.template.xueZhan.MJTemplate_XueZhanSetPos;
import business.global.room.mj.MJRoomPos;
import cenum.mj.HuType;
import cenum.mj.MJHuOpType;
import cenum.mj.OpPointEnum;
import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.cclass.room.AbsBaseResults;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 模板麻将 每一局每个位置信息
 *
 * @author Huaxing
 */
@Getter
@Setter
public class CDXZMJSetPos extends MJTemplate_XueZhanSetPos {


    public CDXZMJSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set) {
        super(posID, roomPos, set);
        this.setCalcPosEnd(new CDXZMJCalcPosEnd(this));
    }

    /**
     * 漏壶
     *
     * @return
     */
    protected MJTemplateLouHu newMJLouHu() {
        return new MJTemplateLouHu(this, MJTemplateRoomEnum.LouHuEnum.HU_FEN_DA_KE_HU);
    }

    /**
     * 检测自摸胡
     */
    @Override
    public List<OpType> recieveOpTypes() {
        this.clearOutCard();
        getPosOpRecord().clearHuCardType();
        //不能出的牌			
        this.addBuNengChuList();
        List<OpType> opTypes = new ArrayList<OpType>();
        if (checkOpType(0, OpType.Ting)) {
            opTypes.add(OpType.Ting);
            if (getOutCardIDs().size() == 0 && getPosID() == getSet().getDPos()) {
                opTypes.add(OpType.BaoTing);
            }
        }
        if (checkOpType(0, OpType.Hu)) {
            opTypes.add(OpType.Hu);
            getPosOpRecord().getHuCardTypeList().add(getHandCard().type);
            this.setmHuOpType(MJHuOpType.ZiMo);
            louHu.setLouHuPoint(preCalcPosPoint(mjCardInit(false)));
        } else {
            louHu.setLouHuPoint(0);

        }
        if (!((CDXZMJSetCard) this.getSet().getSetCard()).isPopCardNull()) {
            if (checkOpType(0, OpType.AnGang)) {
                opTypes.add(OpType.AnGang);
            }
            if (checkOpType(0, OpType.Gang)) {
                opTypes.add(OpType.Gang);
            }
        }
        opTypes.add(OpType.Out);
        return opTypes;
    }

    /**
     * 过手
     */
    @Override
    public void clearPass() {
        // 清空漏碰类型列表	
        this.getPosOpRecord().clearOpCardType();
    }

    /**
     * 新一局中各位置的信息
     *
     * @return
     */
    @Override
    protected BaseMJSet_Pos newMJSetPos() {
        return new CDXZMJSet_Pos();
    }


    /**
     * 新位置结算信息
     *
     * @return
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected CDXZMJRoom_PosEnd newMJSetPosEnd() {
        return new CDXZMJRoom_PosEnd();
    }

    @Override
    public CDXZMJRoom getRoom() {
        return (CDXZMJRoom) super.getRoom();
    }

    /**
     * 计算总结算信息
     */
    @Override
    public void calcResults() {
        // 获取总结算信息												
        CDXZMJResults results = (CDXZMJResults) this.mResultsInfo();
        Map<OpPointEnum, Integer> huTypeMap = getCalcPosEnd().getHuTypeMap();
        results.addAnGangPoint(huTypeMap.get(OpPointEnum.AnGang) == null ? 0 : huTypeMap.get(OpPointEnum.AnGang));
        results.addMingGangPoint(huTypeMap.get(OpPointEnum.JieGang) == null ? 0 : huTypeMap.get(OpPointEnum.JieGang));
        results.addMingGangPoint(huTypeMap.get(OpPointEnum.GangPao) == null ? 0 : huTypeMap.get(OpPointEnum.GangPao));
        results.addZhongMaPoint(zhongList.size());
        // 并且设置覆盖	
        this.setResults(results);
    }

    @Override
    protected AbsBaseResults newResults() {
        return new CDXZMJResults();
    }

    /**
     * 预先计算的牌型分 这个可以前 听牌对应的胡分
     * 如果有维护听牌分 可以直接拿听牌分来判断
     * 如果没有  需要重写
     *
     * @param cardID
     * @return
     */
    protected int preCalcHuCardPoint(int cardID) {
        if (getRoom().RoomCfg(CDXZMJRoomEnum.KeXuanWanFa.GUO_HU_JIA_FAN) && checkOpType(cardID, OpType.JiePao)) {
            return ((CDXZMJCalcPosEnd) getCalcPosEnd()).preCalcHuCardPoint(cardID);
        }
        return 0;
    }

    /**
     * 提前预算胡牌分
     *
     * @param init
     * @return
     */
    @Override
    public int preCalcPosPoint(MJCardInit init) {
        return ((CDXZMJCalcPosEnd) getCalcPosEnd()).preCalcHuCardPoint(init);
    }

    /**
     * 提前计算胡牌类型
     * 每个游戏都不一样 这里自己加
     */
    protected void preCalcHuType() {

    }

    /**
     * 默认算1分 有需要的自己重写此方法
     */
    public void actualTimeCalcGangPoint() {
        List<Integer> list = getPublicCardList().get(sizePublicCardList() - 1);
        int dianPos = list.get(1);
        if (dianPos != getPosID() && !getRoom().RoomCfg(CDXZMJRoomEnum.KeXuanWanFa.BA_DAO_TANG)) {
            getGangMap().put(list.get(2), new ArrayList<>(Arrays.asList((dianPos + 1) % getPlayerNum())));
        } else {
            List<Integer> posList = getSet().getPosDict().values().stream().filter(k -> (k.getHuType().equals(HuType.NotHu) ||
                    k.getHuType().equals(HuType.DianPao) && getPosID() != k.getPosID())).map(k -> k.getPosID()).collect(Collectors.toList());
            posList = posList.stream().map(k -> k++).collect(Collectors.toList());
            gangMap.put(list.get(2), posList);
        }

    }

}
