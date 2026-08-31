package jsproto.c2s.cclass.union;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 赛事房间玩法项
 */
@Data
public class UnionShouXianListInfo {
    /**
     * 房间信息
     */
    private List<UnionRoomCfgItem> roomCfgItems=new ArrayList<>();
    /**
     * 已选
     */
    private List<Long> configList=new ArrayList<>();

    public UnionShouXianListInfo(List<UnionRoomCfgItem> roomCfgItems, List<Long> configList) {
        this.roomCfgItems = roomCfgItems;
        this.configList = configList;
    }

    public List<UnionRoomCfgItem> getRoomCfgItems() {
        return roomCfgItems;
    }

    public void setRoomCfgItems(List<UnionRoomCfgItem> roomCfgItems) {
        this.roomCfgItems = roomCfgItems;
    }

    public List<Long> getConfigList() {
        return configList;
    }

    public void setConfigList(List<Long> configList) {
        this.configList = configList;
    }
}
