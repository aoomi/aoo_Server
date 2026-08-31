package com.ddm.server.common;

public class GameConfig {

    /**
     * 绑定工会奖励
     *
     * @return
     */
    public static int BindingFamilyReward() {
        return Integer.parseInt(System.getProperty("BindingFamilyReward"));
    }

    /**
     * 抽奖消耗房卡数量
     *
     * @return
     */
    public static int LuckDrawCard() {
        return Integer.parseInt(System.getProperty("LuckDrawCard"));
    }

    /**
     * 分享房卡送金币
     *
     * @return
     */
    public static int ShareCard() {
        return Integer.parseInt(System.getProperty("ShareCard"));
    }

    /**
     * 新用户赠送房卡
     *
     * @return
     */
    public static int NewPlayerCard() {
        return Integer.parseInt(System.getProperty("NewPlayerCard"));
    }

    /**
     * 新用户乐豆
     *
     * @return
     */
    public static int NewPlayerGold() {
        return Integer.parseInt(System.getProperty("NewPlayerGold"));
    }

    /**
     * 新用户钻石(兑换卷)
     *
     * @return
     */
    public static int NewPlayerCrystal() {
        return Integer.parseInt(System.getProperty("NewPlayerCrystal"));
    }


    /**
     * 微信
     *
     * @return
     */
    public static String WxOrderNumberURL() {
        return System.getProperty("WxOrderNumberURL");
    }

    /**
     * 专属场手续费
     *
     * @return
     */
    public static double ExclusiveCost() {
        return Double.parseDouble(System.getProperty("ExclusiveCost"));
    }

    /**
     * 创建俱乐部
     *
     * @return
     */
    public static int CreateClub() {
        return Integer.parseInt(System.getProperty("CreateClub", "0"));
    }


    /**
     * 洗牌费用
     *
     * @return
     */
    public static int XiPaiCost() {
        return Integer.parseInt(System.getProperty("XiPaiCost"));
    }

    /**
     * 手机号绑定奖励
     *
     * @return
     */
    public static int Phone() {
        return Integer.parseInt(System.getProperty("Phone"));
    }


    /**
     * 亲友圈创建房间限制
     *
     * @return
     */
    public static int CreateClubRoomLimit() {
        return Integer.parseInt(System.getProperty("CreateClubRoomLimit"));
    }

    /**
     * 玩家加入亲友圈数量上限
     *
     * @return
     */
    public static int ClubPlayerJoinUpperLimit() {
        return Integer.parseInt(System.getProperty("ClubPlayerJoinUpperLimit"));
    }


    /**
     * 亲友圈成员数量上限
     *
     * @return
     */
    public static int ClubMemberUpperLimit() {
        return Integer.parseInt(System.getProperty("ClubMemberUpperLimit"));
    }

    /**
     * 亲友圈管理员数量上限
     *
     * @return
     */
    public static int ClubMinisterUpperLimit() {
        return Integer.parseInt(System.getProperty("ClubMinisterUpperLimit"));
    }

    /**
     * 加入亲友圈首次加入赠送
     *
     * @return
     */
    public static int ClubFirstJoinValue() {
        return Integer.parseInt(System.getProperty("ClubFirstJoinValue"));
    }
}
