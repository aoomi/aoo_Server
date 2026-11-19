package jsproto.c2s.cclass;

import lombok.Data;

/**
 * 只查询Id
 */
@Data
public class QueryIdItem {
    private long id;

    public static String getItemsName() {
        return "id";
    }

    public static String getItemsNameMax() {
        return "max(`id`) as id";
    }

    public static String getItemsNameMin() {
        return "min(`id`) as id";
    }

}
