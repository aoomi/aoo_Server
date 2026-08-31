package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 只查询Id
 */
@Data
public class QueryUidOrPuidItem {
    private long id;
    /**
     * 成员Id
     */
    private long uid;
    /**
     * 上级Id
     */
    private long puid;
    /**
     * 预留值
     */
    private long reservedValue;

    public static String getItemsNameUid() {
        return "uid";
    }

    public static String getItemsNamePuid() {
        return "puid";
    }

    public static String getItemsNameId() {
        return "id,puid,uid";
    }

    public static String getItemsNameUidId() {
        return "id,uid";
    }

    public static String getItemsNameIdReversedValue() {
        return "id,puid,uid,reservedValue";
    }


}
