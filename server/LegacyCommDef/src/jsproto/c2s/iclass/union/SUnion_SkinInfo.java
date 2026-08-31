package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 亲友圈审核通知
 */
@Data
public class SUnion_SkinInfo extends BaseSendMsg {
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 皮肤类型
     */
    private int skinType;
    /**
     * 显示上级及所属亲友圈
     */
    private int showUplevelId;
    /**
     * 显示本圈标志
     */
    private int showClubSign;
    /**
     * 桌子类型
     */
    private int skinTable;
    /**
     * 背景类型
     */
    private int skinBackColor;

    public static SUnion_SkinInfo make(long unionId, int skinType, int showUplevelId, int showClubSign) {
        SUnion_SkinInfo ret = new SUnion_SkinInfo();
        ret.setUnionId(unionId);
        ret.setSkinType(skinType);
        ret.setShowClubSign(showClubSign);
        ret.setShowUplevelId(showUplevelId);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

    public static SUnion_SkinInfo make(long unionId, int skinType) {
        SUnion_SkinInfo ret = new SUnion_SkinInfo();
        ret.setUnionId(unionId);
        ret.setSkinType(skinType);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

    public static SUnion_SkinInfo make(long unionId, int skinType, int showUplevelId, int showClubSign, int skinTable, int skinBackColor) {
        SUnion_SkinInfo ret = new SUnion_SkinInfo();
        ret.setUnionId(unionId);
        ret.setSkinType(skinType);
        ret.setShowClubSign(showClubSign);
        ret.setShowUplevelId(showUplevelId);
        ret.setSkinBackColor(skinBackColor);
        ret.setSkinTable(skinTable);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
