package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 联赛区间id
 */
@Data
public class UnionShareSectionItem {
    private long id;
    private long unionId;
    private long clubId;
    private int updateTime;
    private int createTime;
    /**
     * 报名费区间开始值
     */
    private double beginValue;
    /**
     * 报名费区间结束值
     */
    private double endValue;
    /**
     * 是否是最后一个区间
     */
    private int endFlag;


    public static String getItemsName() {
        return "id,unionId,clubId,updateTime,createTime,beginValue,endValue,endFlag";
    }


}
