package jsproto.c2s.cclass.union;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UnionMemberRankedInfo {
    /**
     * 表格数据
     */
    private List<UnionMemberItem> unionMemberItemList = new ArrayList<>();


    public UnionMemberRankedInfo(List<UnionMemberItem> unionMemberItemList) {
        this.unionMemberItemList = unionMemberItemList;

    }
}
