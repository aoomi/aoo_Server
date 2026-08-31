package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * @author zaf
 */
@Data
public class CUnion_SkinInfo extends CUnion_Base {
    /**
     * 皮肤类型
     */
    private int skinType = -1;
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
    private int skinTable = -1;
    /**
     * 背景类型
     */
    private int skinBackColor = -1;

    public CUnion_SkinInfo(long unionId, long clubId, int skinType, int showUplevelId, int showClubSign) {
        super(unionId, clubId);
        this.skinType = skinType;
        this.showUplevelId = showUplevelId;
        this.showClubSign = showClubSign;
    }

    public CUnion_SkinInfo() {
    }
}