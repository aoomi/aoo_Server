package jsproto.c2s.cclass.club;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 总算信息
 *
 * @author Administrator
 */
@Data
@NoArgsConstructor
public class ClubTotalInfo implements Serializable {
    /**
     * 开房总次数
     */
    private int roomTotalCount;
    /**
     * 房卡总消耗
     */
    private int roomCardTotalCount;
    /**
     * 分页总数
     */
    private int pageNumTotal;


    public ClubTotalInfo(int roomTotalCount, int roomCardTotalCount) {
        super();
        this.roomTotalCount = roomTotalCount;
        this.roomCardTotalCount = roomCardTotalCount;
        this.pageNumTotal = 0;
    }

    public static String getItemsName() {
        return "roomID as roomTotalCount,`value` as roomCardTotalCount";
    }

    public static String getUnionItemsName() {
        return "id as roomTotalCount ,(consumeValue) as roomCardTotalCount";
    }

}
