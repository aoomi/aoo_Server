package business.global.mj.template.xueZhan;

import business.global.mj.AbsMJSetRoom;
import business.global.mj.template.MJTemplateRoomEnum;
import business.global.mj.template.MJTemplateSetPos;
import business.global.room.mj.MJRoomPos;
import cenum.RoomTypeEnum;
import cenum.mj.HuType;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommMath;
import jsproto.c2s.cclass.mj.BaseMJRoom_PosEnd;
import jsproto.c2s.cclass.mj.template.MJTemplateHuInfo;
import jsproto.c2s.cclass.mj.template.MJTemplateRoom_PosEnd;
import jsproto.c2s.cclass.mj.template.MJTemplateSet_Pos;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * 模板麻将 每一局每个位置信息
 *
 * @author Huaxing
 */
@Getter
@Setter
public abstract class MJTemplate_XueZhanSetPos extends MJTemplateSetPos {

    private List<MJTemplateHuInfo> huInfos = new ArrayList<>();//

    /**
     * 胡牌顺序
     */
    public MJTemplateRoomEnum.HuCardEndType huCardEndType = MJTemplateRoomEnum.HuCardEndType.NOT;
    /**
     * 杠分收取
     * value pos+1的list
     */
    public Map<Integer, List<Integer>> gangMap = new HashMap<>();
    /**
     * pos+1
     */
    public List<Integer> winList = new ArrayList<>();//胡的类型下面显示数字

    public MJTemplate_XueZhanSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set, Class<?> mActMrg) {
        super(posID, roomPos, set, mActMrg);
    }

    public MJTemplate_XueZhanSetPos(int posID, MJRoomPos roomPos, AbsMJSetRoom set) {
        super(posID, roomPos, set);
    }

    /**
     * 检测平胡
     */
    @Override
    public OpType checkPingHu(int curOpPos, int cardID) {
        //已经胡过牌了
        if (!getHuType().equals(HuType.NotHu) && !getHuType().equals(HuType.DianPao)) {
            return OpType.Not;
        }
        return super.checkPingHu(curOpPos, cardID);
    }


    @Override
    public MJTemplateSet_Pos getCommonNotify(MJTemplateSet_Pos setPos, boolean isSelf) {
        MJTemplateSet_Pos set_pos = super.getCommonNotify(setPos, isSelf);
        setPos.setHuType(getHuType());//本设置目前只有 血战才有
        setPos.setHuInfos(huInfos);
        setPos.setHuOpType(getHuType());
        if (this.getHandCard() != null) {
            // 首牌
            set_pos.setHandCard(isSelf ? this.getHandCard().getCardID() : (!getHuType().equals(HuType.NotHu) && !getHuType().equals(HuType.DianPao)) ? this.getHandCard().getCardID() : 5000);
        }
        return set_pos;
    }

    @Override
    public void setHuType(HuType huType) {
        if (huType.equals(HuType.DianPao)) {
            return;
        }
        preCalcHuType();
        if (getHuInfos().isEmpty()) {
            if (huType.name().equals("Hu")) {
                getHuInfos().add(new MJTemplateHuInfo(getHandCard().cardID, getHuType(), getSet().getLastOpInfo().getLastOpPos()));
            } else {
                getHuInfos().add(new MJTemplateHuInfo(getHandCard().cardID, getHuType(), -1));
            }
        }
        super.setHuType(huType);
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
        } else if (huType.name().contains("Hu")) {
            huInfos.add(new MJTemplateHuInfo(getHandCard().cardID, huType, getSet().getLastOpInfo().getLastOpPos()));
        } else {
            huInfos.add(new MJTemplateHuInfo(getHandCard().cardID, huType, -1));
        }
        super.setHuCardType(huType, huPos, roundId);
    }

    /**
     * 提前计算胡牌类型
     * 每个游戏都不一样 这里自己加
     */
    protected void preCalcHuType() {

    }

    /**
     * 统计本局分数
     *
     * @return
     */
    @Override
    public BaseMJRoom_PosEnd<?> calcPosEnd() {
        MJTemplateRoom_PosEnd ret = (MJTemplateRoom_PosEnd) super.calcPosEnd();
        ret.setGangMap(gangMap);
        ret.setWinList(winList);
        return ret;
    }

    @Override
    public void calcGangPoint1v3(int point) {
        for (int i = 0; i < this.getSet().getRoom().getPlayerNum(); i++) {

            MJTemplate_XueZhanSetPos setPos = (MJTemplate_XueZhanSetPos) getMJSetPos(i);
            if (Objects.isNull(setPos)) {
                return;
            }
            //不跟胡过得算分
            if (setPos.getHuCardEndType().ordinal() >= this.huCardEndType.ordinal() && !setPos.getHuCardEndType().equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
                return;
            }
        }
    }
}
