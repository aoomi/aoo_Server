package business.global.club;

import jsproto.c2s.cclass.club.Club_define;
import lombok.Data;

@Data
public class ClubMemberItem {
    private ClubMember toClubMember;
    private ClubMember doClubMember;
    private Club_define.Club_PROMOTION_LEVEL_POWER levelPower;

    public ClubMemberItem(ClubMember toClubMember, ClubMember doClubMember, Club_define.Club_PROMOTION_LEVEL_POWER levelPower) {
        this.toClubMember = toClubMember;
        this.doClubMember = doClubMember;
        this.levelPower = levelPower;
    }
}
