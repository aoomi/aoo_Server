package jsproto.c2s.cclass.union;

import jsproto.c2s.iclass.union.SUnion_BanRoomConfigItem;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class UnionBanRoomConfigItem {
    /**
     * 配置列表
     */
    List<SUnion_BanRoomConfigItem> unionBanRoomConfigBOList = Collections.emptyList();
    /**
     * 1：全选
     */
    private int isAll;

    public UnionBanRoomConfigItem(List<SUnion_BanRoomConfigItem> unionBanRoomConfigBOList, int isAll) {
        this.unionBanRoomConfigBOList = unionBanRoomConfigBOList;
        this.isAll = isAll;
    }
}
