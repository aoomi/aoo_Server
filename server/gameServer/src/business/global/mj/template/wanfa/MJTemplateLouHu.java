package business.global.mj.template.wanfa;

import business.global.mj.AbsMJRoundPos;
import business.global.mj.template.MJTemplateRoomEnum;
import business.global.mj.template.MJTemplateSetPos;
import cenum.mj.OpType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 漏胡
 *
 * @author leo_wi
 */
@Data
public class MJTemplateLouHu {
    public MJTemplateSetPos mSetPos;
    /**
     * 漏胡枚举
     */
    public MJTemplateRoomEnum.LouHuEnum louHuEnum;
    /**
     * 漏壶分
     */
    public int louHuPoint;
    /**
     * 漏壶的牌
     */
    public List<Integer> louHuCardTypes = new ArrayList<>();
    /**
     * 起胡分 最低这个分才能胡
     */
    public int qiHuPoint = 0;

    /**
     * @param mSetPos
     * @param louHuEnum
     */
    public MJTemplateLouHu(MJTemplateSetPos mSetPos, MJTemplateRoomEnum.LouHuEnum louHuEnum) {
        this.mSetPos = mSetPos;
        this.louHuEnum = louHuEnum;
    }

    /**
     * @param mSetPos
     * @param louHuEnum
     */
    public MJTemplateLouHu(MJTemplateSetPos mSetPos, MJTemplateRoomEnum.LouHuEnum louHuEnum, int qiHuPoint) {
        this.mSetPos = mSetPos;
        this.louHuEnum = louHuEnum;
        this.qiHuPoint = qiHuPoint;
    }

    public boolean checkLou(int cardType, int huPoint) {
        if (qiHuPoint > huPoint) {
            return true;
        }
        if (cardType > 100) {
            cardType = cardType / 100;
        }
        if (louHuEnum.equals(MJTemplateRoomEnum.LouHuEnum.DA_PAI_HOU_KE_HU)) {
            return louHuCardTypes.size() > 0 || (huPoint <= louHuPoint && louHuPoint > 0);
        } else if (louHuEnum.equals(MJTemplateRoomEnum.LouHuEnum.NENG_HU_BU_HU_MO_PAI_KE_HU)) {
            return louHuCardTypes.size() > 0 || louHuPoint > 0;
        } else if (!louHuEnum.equals(MJTemplateRoomEnum.LouHuEnum.NOT)) {
            return louHuCardTypes.contains(cardType) || (huPoint <= louHuPoint && louHuPoint > 0);
        }
        return false;
    }

    public void updateLouHuEnum(MJTemplateRoomEnum.LouHuEnum louHuEnum) {
        if (!this.louHuEnum.equals(louHuEnum)) {
            this.louHuEnum = louHuEnum;
            louHuPoint = 0;
            louHuCardTypes.clear();
        }
    }

    /**
     * 设置漏胡
     *
     * @param louHuCard
     * @param huPoint
     */
    public void addPassHuCard(int louHuCard, int huPoint) {
        if (louHuCard > 100) {
            louHuCard = louHuCard / 100;
        }
        if (!louHuEnum.equals(MJTemplateRoomEnum.LouHuEnum.NOT)) {
            louHuCardTypes.add(louHuCard);
            louHuPoint = huPoint;
        }
    }

    /**
     * 打过的牌不能胡
     *
     * @param type
     */
    public void addOutCardType(int type) {
        if (louHuEnum.equals(MJTemplateRoomEnum.LouHuEnum.BU_HU_DA_GUO_DE_PAI)) {
            louHuCardTypes.add(type);
        } else if (louHuEnum.equals(MJTemplateRoomEnum.LouHuEnum.DA_PAI_HOU_KE_HU)) {
            louHuCardTypes.clear();
            louHuPoint = 0;
        } else if (louHuEnum.equals(MJTemplateRoomEnum.LouHuEnum.ZiMO_BUHU_SUAN_LOU_HU)) {
            //自摸算漏胡
            AbsMJRoundPos roundPos = mSetPos.getSet().getCurRound().getRoundPosDict().get(mSetPos.getPosID());
            if (roundPos.checkRecieveOpTypes(OpType.Hu)) {
                return;
            }
            louHuCardTypes.clear();
            louHuPoint = 0;

        }
    }

    public void reSetLouHu() {
        louHuCardTypes.clear();
        louHuPoint = 0;
    }

}
