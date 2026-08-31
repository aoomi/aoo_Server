package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 只查询Id
 */
@Data
public class ClubPlayerRemarkName {
    /**
     * 主键id
     */
    private long id;
    /**
     * 自己的ID
     */
    private long pid;
    /**
     * 备注的id
     */
    private long remarkID;
    /**
     * 备注的名称
     */
    private String remarkName;


    public static String getItemsNameId() {
        return "id,pid,remarkID,remarkName";
    }


}
