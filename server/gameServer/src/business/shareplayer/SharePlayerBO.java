package business.shareplayer;

import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/7 14:32
 * @description 共享玩家基础信息
 */
@Data
public class SharePlayerBO {
    private long id;
    private long accountID;
    private int sid;
    private String name = "";
    private int icon;
    private int sex;
    private String headImageUrl = "";
    private long createTime;
    private int lv;
    private int gmLevel;
    private int vipLevel;
    private int vipExp;
    private int totalRecharge;
    private int crystal;
    private int gold;
    private int roomCard;
    private int fastCard;
    private int cheatTimes;
    private int bannedChatExpiredTime;
    private int bannedLoginExpiredTime;
    private int bannedTimes;
    private int lastLogin;
    private int lastLogout;
    private int currentGameType = -1;
    private long familyID = 10001;
    private String realName = "";
    private String realNumber = "";
    private int realReferer;
    private String wx_unionid = "";
    private String gameList = "";
    private long sendClubReward;
    private int clubTotalRecharge;
    private int mjPoint;
    private int pkPoint;
    private long phone;
    private int os;
}
