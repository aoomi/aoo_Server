package jsproto.c2s.cclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.iclass.club.SClub_BanRoomConfigItem;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 赛事禁止房间配置数据表
 */
@Data
@NoArgsConstructor
public class ClubBanRoomConfigEach extends BaseSendMsg {
    public List<SClub_BanRoomConfigItem> items=new ArrayList<>();

    public ClubBanRoomConfigEach(List<SClub_BanRoomConfigItem> items) {
        this.items = items;
    }
}
