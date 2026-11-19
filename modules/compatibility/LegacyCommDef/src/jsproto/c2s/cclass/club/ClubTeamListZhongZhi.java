package jsproto.c2s.cclass.club;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 俱乐部推广员项
 */
@Data
public class ClubTeamListZhongZhi {

    private List<ClubTeamListInfo> clubTeamListInfoList = new ArrayList<>();
    private int type;

    public ClubTeamListZhongZhi(List<ClubTeamListInfo> clubTeamListInfoList, int dateType) {
        this.clubTeamListInfoList = clubTeamListInfoList;
        this.type = dateType;
    }
}
