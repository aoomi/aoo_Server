package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 赛事禁止房间配置数据表
 */
@Data
@NoArgsConstructor
public class SUnion_BanClubItem extends BaseSendMsg {

    /**
     * 0:不禁止,1:禁止
     */
    private int isBan = 1;
    private String name = "";// 俱乐部名称
    private int clubSign;// 随机的俱乐部标识ID
    private long clubId;


    public static String getItemsName() {
        return "clubId,name,clubSign";
    }

}
