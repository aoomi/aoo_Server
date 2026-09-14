package jsproto.c2s.cclass.mj.template;

import cenum.mj.HuType;
import lombok.Data;

@Data
public class MJTemplateHuInfo {
    /**
     * 胡的牌
     */
    private int cardID;
    /**
     * 胡的类型
     */
    private HuType type;
    /**
     * 点炮位置  -1：自摸
     */
    private int posID;

    public MJTemplateHuInfo(int cardID, HuType type, int posID) {
        this.cardID = cardID;
        this.type = type;
        this.posID = posID;
    }

    public MJTemplateHuInfo(int cardID, int posID) {
        this.cardID = cardID;
        this.posID = posID;
    }

}
