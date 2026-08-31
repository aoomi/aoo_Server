package jsproto.c2s.cclass;

import lombok.Data;

/**
 * 只查询Id
 */
@Data
public class QueryRoomIdItem {
    private long roomID;

    public static String getItemsName() {
        return "roomID";
    }

}
